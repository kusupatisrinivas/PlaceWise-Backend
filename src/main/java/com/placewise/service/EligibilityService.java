package com.placewise.service;
import com.placewise.dto.ApiDtos.Eligibility;
import com.placewise.entity.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityService {
    private final Clock clock;
    public EligibilityService(Clock clock) { this.clock = clock; }
    public boolean isOpen(PlacementDrive drive) { return drive.active && !drive.deadline.isBefore(LocalDate.now(clock)); }
    public Eligibility evaluate(Student student, PlacementDrive drive) {
        List<String> reasons = new ArrayList<>();
        if (!drive.active) reasons.add("This drive is currently paused.");
        if (drive.deadline.isBefore(LocalDate.now(clock))) reasons.add("The application deadline has passed.");
        if (student.cgpa.compareTo(drive.minCgpa) < 0)
            reasons.add("Minimum CGPA required: " + drive.minCgpa + ". Your CGPA: " + student.cgpa + ".");
        if (drive.eligibleBranches.stream().noneMatch(b -> b.equalsIgnoreCase(student.branch)))
            reasons.add("Eligible branches: " + String.join(", ", drive.eligibleBranches) + ". Your branch: " + student.branch + ".");
        Set<String> skills = student.skills.stream().map(s -> s.name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        List<String> missing = drive.requiredSkills.stream().map(s -> s.name).filter(s -> !skills.contains(s.toLowerCase(Locale.ROOT))).sorted().toList();
        if (!missing.isEmpty()) reasons.add("Missing required skills: " + String.join(", ", missing) + ".");
        return new Eligibility(reasons.isEmpty(), reasons);
    }
}
