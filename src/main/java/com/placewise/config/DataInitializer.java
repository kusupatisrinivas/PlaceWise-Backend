package com.placewise.config;
import com.placewise.entity.*;
import com.placewise.entity.Application.Status;
import com.placewise.repository.*;
import com.placewise.service.PortalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final StudentRepository students;
    private final CompanyRepository companies;
    private final DriveRepository drives;
    private final ApplicationRepository applications;
    private final PasswordEncoder passwords;
    private final PortalService portal;
    private final Clock clock;
    @Value("${app.admin-email}") private String adminEmail;
    @Value("${app.admin-password:}") private String adminPassword;
    @Value("${app.seed-demo:false}") private boolean seedDemo;
    public DataInitializer(UserRepository users, StudentRepository students, CompanyRepository companies, DriveRepository drives,
            ApplicationRepository applications, PasswordEncoder passwords, PortalService portal, Clock clock) {
        this.users=users; this.students=students; this.companies=companies; this.drives=drives;
        this.applications=applications; this.passwords=passwords; this.portal=portal; this.clock=clock;
    }
    @Override @Transactional
    public void run(String... args) {
        String email=adminEmail.strip().toLowerCase(Locale.ROOT);
        if (!adminPassword.isBlank() && !users.existsByEmail(email)) {
            if (adminPassword.length()<8 || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                throw new IllegalArgumentException("ADMIN_PASSWORD must be at least 8 characters and at most 72 UTF-8 bytes.");
            User admin=new User(); admin.email=email; admin.fullName="Placement Officer";
            admin.passwordHash=passwords.encode(adminPassword); admin.role=User.Role.ROLE_ADMIN; users.save(admin);
        }
        // Demo data is explicitly opt-in. It never runs against the default MySQL profile.
        if (!seedDemo || companies.count()>0 || users.existsByEmail("hemanth@placewise.edu")) return;
        List<Student> sampleStudents=new ArrayList<>();
        String[] names={"Hemanth Kumar","Ananya Rao","Arjun Mehta","Priya Sharma","Rahul Reddy","Sneha Patel","Karthik Nair","Ishita Das"};
        String[] branches={"CSE","IT","CSE","ECE","CSE","IT","ECE","CSE"};
        for (int i=0;i<names.length;i++) {
            User user=new User(); user.fullName=names[i]; user.email=i==0 ? "hemanth@placewise.edu" : "student"+i+"@placewise.edu";
            user.passwordHash=passwords.encode("Student@123"); user.role=User.Role.ROLE_STUDENT;
            Student student=new Student(); student.user=user; student.rollNumber="22"+branches[i]+String.format("%03d",i+1);
            student.branch=branches[i]; student.year=4; student.cgpa=new BigDecimal(i==0 ? "8.60" : "8.20");
            student.phone="98765432"+String.format("%02d",i);
            student.skills=portal.resolveSkills(Set.of("java","sql","react","javascript","python","communication"));
            sampleStudents.add(students.save(student));
        }
        String[] companyNames={"Google","Microsoft","Atlassian","Deloitte","Infosys","TCS","Amazon","Accenture"};
        String[] sites={"https://about.google","https://microsoft.com","https://atlassian.com","https://deloitte.com","https://infosys.com","https://tcs.com","https://amazon.com","https://accenture.com"};
        String[] locations={"Bengaluru","Hyderabad","Bengaluru","Hyderabad","Pune","Chennai","Bengaluru","Hyderabad"};
        String[] titles={"Software Engineer","Associate Software Engineer","Frontend Engineer","Technology Analyst","Systems Engineer","Java Developer","Software Development Engineer","Associate Engineer"};
        String[] packages={"24","18","16","8","6.5","7","22","6"};
        List<PlacementDrive> sampleDrives=new ArrayList<>();
        for (int i=0;i<companyNames.length;i++) {
            Company company=new Company(); company.name=companyNames[i]; company.website=sites[i];
            company.industry=i==3 || i==7 ? "Consulting" : "Technology";
            company.location=locations[i]; company.description="Build your career with "+companyNames[i]+". Join collaborative teams solving meaningful problems through technology, learning, and innovation.";
            companies.save(company);
            PlacementDrive drive=new PlacementDrive(); drive.company=company; drive.jobTitle=titles[i];
            drive.description="Join "+companyNames[i]+" as a "+titles[i]+" and work with experienced engineers on products that make a difference.\n\nWhat you will do\n• Build and maintain reliable, well-tested software.\n• Collaborate with designers and engineers to solve real customer problems.\n• Participate in code reviews and learn through hands-on mentoring.\n\nWhat we are looking for\nStrong fundamentals, curiosity, and a collaborative mindset. Bring your problem-solving skills and a willingness to learn.\n\nSelection process\nOnline assessment → Technical interview → HR conversation.";
            drive.salaryPackage=new BigDecimal(packages[i]); drive.location=locations[i];
            drive.deadline=LocalDate.now(clock).plusDays(3+i*2); drive.driveDate=drive.deadline.plusDays(4);
            drive.minCgpa=new BigDecimal(i==0 ? "9.0" : i==6 ? "8.0" : "7.0");
            drive.eligibleBranches=new LinkedHashSet<>(i==0 || i==2 ? Set.of("CSE","IT") : Set.of("CSE","IT","ECE"));
            drive.requiredSkills=portal.resolveSkills(i==2 ? Set.of("react","javascript") : Set.of("java","sql"));
            drive.jobType=i==4 ? PlacementDrive.JobType.INTERNSHIP : PlacementDrive.JobType.FULL_TIME;
            sampleDrives.add(drives.save(drive));
        }
        // Believable, small demo dataset. Every dashboard number comes from these rows.
        Status[] statuses={Status.SHORTLISTED,Status.INTERVIEW,Status.APPLIED,Status.SELECTED,Status.REJECTED};
        for (int s=0;s<sampleStudents.size();s++) {
            for (int n=0;n<(s==0 ? 3 : 2);n++) {
                Application app=new Application(); app.student=sampleStudents.get(s);
                app.drive=sampleDrives.get(1+(s+n)%7);
                app.status=s==0 ? statuses[n] : statuses[(s+n)%statuses.length];
                app.appliedAt=clock.instant().minusSeconds((s+n+1)*86400L);
                applications.save(app);
            }
        }
    }
}
