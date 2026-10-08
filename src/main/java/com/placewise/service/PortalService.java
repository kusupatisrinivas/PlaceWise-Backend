package com.placewise.service;

import com.placewise.dto.ApiDtos.*;
import com.placewise.entity.*;
import com.placewise.entity.Application.Status;
import com.placewise.exception.ApiException;
import com.placewise.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @Transactional
public class PortalService {
    private final UserRepository users;
    private final StudentRepository students;
    private final CompanyRepository companies;
    private final DriveRepository drives;
    private final ApplicationRepository applications;
    private final SkillRepository skills;
    private final EligibilityService eligibility;
    private final Clock clock;

    public PortalService(UserRepository users, StudentRepository students, CompanyRepository companies,
            DriveRepository drives, ApplicationRepository applications, SkillRepository skills, EligibilityService eligibility, Clock clock) {
        this.users=users; this.students=students; this.companies=companies; this.drives=drives;
        this.applications=applications; this.skills=skills; this.eligibility=eligibility; this.clock=clock;
    }
    public User currentUser() {
        String subject=SecurityContextHolder.getContext().getAuthentication().getName();
        try { return users.findById(Long.valueOf(subject)).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your account is no longer available.")); }
        catch (NumberFormatException e) { throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid session."); }
    }
    private Student currentStudent() { return students.findByUserId(currentUser().id).orElseThrow(() -> ApiException.missing("Student profile")); }
    private PlacementDrive drive(Long id) { return drives.findById(id).orElseThrow(() -> ApiException.missing("Placement drive")); }
    private Company company(Long id) { return companies.findById(id).orElseThrow(() -> ApiException.missing("Company")); }
    private Student student(Long id) { return students.findById(id).orElseThrow(() -> ApiException.missing("Student")); }

    // Skills are normalized once and reused across the two many-to-many relationships.
    public Set<Skill> resolveSkills(Set<String> names) {
        Set<Skill> result=new LinkedHashSet<>();
        names.stream().map(s -> s.strip().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).distinct().sorted()
            .forEach(name -> result.add(skills.findByName(name).orElseGet(() -> skills.save(new Skill(name)))));
        return result;
    }
    private Set<String> skillNames(Set<Skill> values) {
        return values.stream().map(s -> s.name).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    }
    public StudentView studentView(Student s) {
        return new StudentView(s.id, s.user.id, s.user.fullName, s.user.email, s.rollNumber, s.branch, s.year,
            s.cgpa, s.phone, skillNames(s.skills), s.applications.stream().anyMatch(a -> a.status==Status.SELECTED));
    }
    public CompanyView companyView(Company c) {
        return new CompanyView(c.id, c.name, c.description, c.website, c.industry, c.location, c.drives.size());
    }
    public DriveView driveView(PlacementDrive d, Student student, Set<Long> applied) {
        return new DriveView(d.id, companyView(d.company), d.jobTitle, d.description, d.salaryPackage, d.location,
            d.driveDate, d.deadline, d.minCgpa, new TreeSet<>(d.eligibleBranches), skillNames(d.requiredSkills),
            d.jobType, eligibility.isOpen(d), d.active, applied.contains(d.id), d.applications.size(),
            student == null ? null : eligibility.evaluate(student, d));
    }
    public ApplicationView applicationView(Application a) {
        return new ApplicationView(a.id, a.drive.id, a.drive.company.name, a.drive.jobTitle, a.drive.location,
            a.drive.salaryPackage, a.appliedAt, a.status, studentView(a.student));
    }
    public StudentView profile() { return studentView(currentStudent()); }
    public StudentView updateProfile(ProfileInput input) { return updateStudent(currentStudent().id, input); }
    public List<StudentView> listStudents(String search) {
        String q=search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        return students.findAll(Sort.by("user.fullName")).stream()
            .filter(s -> (s.user.fullName+" "+s.user.email+" "+s.rollNumber+" "+s.branch).toLowerCase(Locale.ROOT).contains(q))
            .map(this::studentView).toList();
    }
    public StudentView updateStudent(Long id, ProfileInput input) {
        Student s=student(id);
        String roll=input.rollNumber().strip().toUpperCase(Locale.ROOT);
        if (!s.rollNumber.equals(roll) && students.existsByRollNumber(roll)) throw ApiException.conflict("This roll number is already registered.");
        s.user.fullName=input.fullName().strip(); s.rollNumber=roll; s.branch=input.branch().strip().toUpperCase(Locale.ROOT);
        s.year=input.year(); s.cgpa=input.cgpa(); s.phone=input.phone(); s.skills=resolveSkills(input.skills());
        students.saveAndFlush(s);
        return studentView(s);
    }
    public void deleteStudent(Long id) { students.delete(student(id)); }
    public List<CompanyView> listCompanies() { return companies.findAll(Sort.by("name")).stream().map(this::companyView).toList(); }
    public CompanyView getCompany(Long id) { return companyView(company(id)); }
    public CompanyView saveCompany(Long id, CompanyInput input) {
        Company c=id == null ? new Company() : company(id);
        c.name=input.name().strip(); c.description=input.description().strip(); c.website=input.website().strip();
        c.industry=input.industry().strip(); c.location=input.location().strip();
        return companyView(companies.save(c));
    }
    public void deleteCompany(Long id) {
        Company c=company(id);
        if (c.drives.stream().anyMatch(eligibility::isOpen))
            throw ApiException.conflict("Pause this company's active drives before deleting it.");
        companies.delete(c); // Cascades through archived drives and their applications.
    }

    public List<DriveView> listDrives(String search, String branch, BigDecimal maxCgpa, BigDecimal minPackage,
            PlacementDrive.JobType jobType, Long companyId, Boolean active) {
        User user=currentUser();
        Student student=user.role==User.Role.ROLE_STUDENT ? currentStudent() : null;
        Set<Long> applied=student==null ? Set.of() : applications.findByStudentIdOrderByAppliedAtDesc(student.id)
            .stream().map(a -> a.drive.id).collect(Collectors.toSet());
        Specification<PlacementDrive> spec=(root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates=new ArrayList<>();
            if (search!=null && !search.isBlank()) {
                String pattern="%"+search.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("jobTitle")), pattern, '\\'),
                    cb.like(cb.lower(root.get("location")), pattern, '\\'), cb.like(cb.lower(root.join("company").get("name")), pattern, '\\')));
            }
            if (branch!=null && !branch.isBlank()) predicates.add(cb.equal(root.join("eligibleBranches"), branch.strip().toUpperCase(Locale.ROOT)));
            if (maxCgpa!=null) predicates.add(cb.lessThanOrEqualTo(root.get("minCgpa"), maxCgpa));
            if (minPackage!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("salaryPackage"), minPackage));
            if (jobType!=null) predicates.add(cb.equal(root.get("jobType"), jobType));
            if (companyId!=null) predicates.add(cb.equal(root.get("company").get("id"), companyId));
            Predicate open=cb.and(cb.isTrue(root.get("active")), cb.greaterThanOrEqualTo(root.get("deadline"), LocalDate.now(clock)));
            if (active!=null) predicates.add(active ? open : cb.not(open));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return drives.findAll(spec, Sort.by("deadline").ascending().and(Sort.by("id").descending()))
            .stream().map(d -> driveView(d, student, applied)).toList();
    }
    public DriveView getDrive(Long id) {
        User u=currentUser(); Student student=u.role==User.Role.ROLE_STUDENT ? currentStudent() : null;
        Set<Long> applied=student!=null && applications.existsByStudentIdAndDriveId(student.id,id) ? Set.of(id) : Set.of();
        return driveView(drive(id), student, applied);
    }
    public DriveView saveDrive(Long id, DriveInput input) {
        if (input.deadline().isAfter(input.driveDate())) throw ApiException.bad("The application deadline must be on or before the drive date.");
        if (id==null && input.deadline().isBefore(LocalDate.now(clock))) throw ApiException.bad("A new drive cannot have a past deadline.");
        PlacementDrive d=id==null ? new PlacementDrive() : drive(id);
        d.company=company(input.companyId()); d.jobTitle=input.jobTitle().strip(); d.description=input.description().strip();
        d.salaryPackage=input.salaryPackage(); d.location=input.location().strip(); d.driveDate=input.driveDate();
        d.deadline=input.deadline(); d.minCgpa=input.minCgpa(); d.jobType=input.jobType(); d.active=input.active();
        d.eligibleBranches=input.eligibleBranches().stream().map(s -> s.strip().toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(LinkedHashSet::new));
        d.requiredSkills=resolveSkills(input.requiredSkills());
        return driveView(drives.save(d), null, Set.of());
    }
    public DriveView setActive(Long id, boolean active) {
        PlacementDrive d=drive(id); d.active=active;
        return driveView(d, null, Set.of());
    }
    public void deleteDrive(Long id) { drives.delete(drive(id)); }
    public ApplicationView apply(Long driveId) {
        Student student=currentStudent(); PlacementDrive drive=drive(driveId);
        if (applications.existsByStudentIdAndDriveId(student.id, driveId)) throw ApiException.conflict("You have already applied for this drive.");
        Eligibility result=eligibility.evaluate(student, drive);
        if (!result.eligible()) throw ApiException.bad(String.join(" ", result.reasons()));
        Application application=new Application();
        application.student=student; application.drive=drive; application.appliedAt=clock.instant();
        return applicationView(applications.saveAndFlush(application));
    }
    public List<ApplicationView> myApplications() { return applications.findByStudentIdOrderByAppliedAtDesc(currentStudent().id).stream().map(this::applicationView).toList(); }
    public List<ApplicationView> listApplications(Long driveId) {
        if (driveId!=null) drive(driveId);
        return (driveId==null ? applications.findAllByOrderByAppliedAtDesc() : applications.findByDriveIdOrderByAppliedAtDesc(driveId))
            .stream().map(this::applicationView).toList();
    }
    public ApplicationView updateStatus(Long id, Status status) {
        Application a=applications.findById(id).orElseThrow(() -> ApiException.missing("Application"));
        a.status=status; return applicationView(a);
    }
    public StudentDashboard studentDashboard() {
        Student student=currentStudent();
        List<ApplicationView> apps=myApplications();
        List<DriveView> open=listDrives(null,null,null,null,null,null,true);
        return new StudentDashboard(studentView(student), open.size(), apps.size(),
            apps.stream().filter(a -> a.status()==Status.SHORTLISTED).count(),
            apps.stream().filter(a -> a.status()==Status.SELECTED).count(),
            open.stream().filter(d -> !d.applied()).limit(3).toList(), apps.stream().limit(4).toList());
    }
    private List<ChartItem> group(List<Application> all, Function<Application,String> key) {
        return all.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting())).entrySet().stream()
            .map(e -> new ChartItem(e.getKey(),e.getValue())).toList();
    }
    public AdminDashboard adminDashboard() {
        List<Application> all=applications.findAllByOrderByAppliedAtDesc();
        List<DriveView> open=listDrives(null,null,null,null,null,null,true);
        return new AdminDashboard(students.count(),companies.count(),open.size(),all.size(),
            all.stream().filter(a -> a.status==Status.SELECTED).map(a -> a.student.id).distinct().count(),
            group(all,a -> a.drive.company.name),group(all,a -> a.student.branch),group(all,a -> a.status.name()),
            all.stream().limit(5).map(this::applicationView).toList(),open.stream().limit(3).toList());
    }
}
