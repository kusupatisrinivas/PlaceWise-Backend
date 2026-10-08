package com.placewise;
import com.placewise.entity.*;
import com.placewise.service.EligibilityService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class EligibilityServiceTest {
    @Test void deadlineRemainsOpenUntilMidnightInCampusTimezone() {
        ZoneId zone=ZoneId.of("Asia/Kolkata");
        PlacementDrive drive=new PlacementDrive(); drive.deadline=LocalDate.of(2026,9,30); drive.active=true;
        Clock before=Clock.fixed(Instant.parse("2026-09-30T18:29:59Z"),zone);
        Clock after=Clock.fixed(Instant.parse("2026-09-30T18:30:00Z"),zone);
        assertThat(new EligibilityService(before).isOpen(drive)).isTrue();
        assertThat(new EligibilityService(after).isOpen(drive)).isFalse();
    }
    @Test void exactCgpaBoundaryAndAllSkillsAreRequiredCaseInsensitively() {
        EligibilityService service=new EligibilityService(Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"),ZoneId.of("Asia/Kolkata")));
        Student student=new Student(); student.cgpa=new BigDecimal("7.50"); student.branch="cse";
        student.skills=new LinkedHashSet<>(List.of(new Skill("JAVA"),new Skill("sql"),new Skill("React")));
        PlacementDrive drive=new PlacementDrive(); drive.minCgpa=new BigDecimal("7.5"); drive.deadline=LocalDate.of(2026,9,2);
        drive.eligibleBranches=Set.of("CSE","IT"); drive.requiredSkills=Set.of(new Skill("java"),new Skill("SQL"));
        assertThat(service.evaluate(student,drive).eligible()).isTrue();
        drive.requiredSkills=Set.of(new Skill("java"),new Skill("python"),new Skill("docker"));
        assertThat(service.evaluate(student,drive).reasons()).singleElement().asString().contains("docker", "python");
    }
}
