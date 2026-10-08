package com.placewise.dto;
import com.placewise.entity.Application.Status;
import com.placewise.entity.PlacementDrive.JobType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ApiDtos {
    private ApiDtos() {}
    public record Login(@NotBlank @Email @Size(max=180) String email, @NotBlank @Size(max=64) String password) { @Override public String toString() { return "Login[credentials=REDACTED]"; } }
    public record Register(
        @NotBlank @Size(max=100) String fullName,
        @NotBlank @Email @Size(max=180) String email,
        @NotBlank @Size(min=8, max=64) String password,
        @NotBlank @Pattern(regexp="[A-Za-z0-9-]{3,30}") String rollNumber,
        @NotBlank @Size(max=20) String branch,
        @Min(1) @Max(4) int year,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer=2, fraction=2) BigDecimal cgpa,
        @NotBlank @Pattern(regexp="[0-9]{10}") String phone) { @Override public String toString() { return "Register[credentials=REDACTED]"; } }
    public record ProfileInput(
        @NotBlank @Size(max=100) String fullName,
        @NotBlank @Pattern(regexp="[A-Za-z0-9-]{3,30}") String rollNumber,
        @NotBlank @Size(max=20) String branch,
        @Min(1) @Max(4) int year,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer=2, fraction=2) BigDecimal cgpa,
        @NotBlank @Pattern(regexp="[0-9]{10}") String phone,
        @NotNull @Size(max=30) Set<@NotBlank @Size(max=50) String> skills) {}
    public record CompanyInput(
        @NotBlank @Size(max=100) String name,
        @NotBlank @Size(max=3000) String description,
        @NotBlank @Size(max=300) @Pattern(regexp="https?://[^\\s]+", message="must be an http or https URL") String website,
        @NotBlank @Size(max=100) String industry,
        @NotBlank @Size(max=100) String location) {}
    public record DriveInput(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max=150) String jobTitle,
        @NotBlank @Size(max=6000) String description,
        @NotNull @DecimalMin("0") @DecimalMax("999999") @Digits(integer=6, fraction=2) BigDecimal salaryPackage,
        @NotBlank @Size(max=100) String location,
        @NotNull LocalDate driveDate, @NotNull LocalDate deadline,
        @NotNull @DecimalMin("0") @DecimalMax("10") @Digits(integer=2, fraction=2) BigDecimal minCgpa,
        @NotEmpty @Size(max=20) Set<@NotBlank @Size(max=20) String> eligibleBranches,
        @NotNull @Size(max=30) Set<@NotBlank @Size(max=50) String> requiredSkills,
        @NotNull JobType jobType, boolean active) {}
    public record ApplyInput(@NotNull @Positive Long driveId) {}
    public record StatusInput(@NotNull Status status) {}
    public record ActiveInput(boolean active) {}
    public record AuthResponse(String token, Instant expiresAt, String role, Long userId, String fullName, String email) { @Override public String toString() { return "AuthResponse[role=" + role + ", userId=" + userId + "]"; } }
    public record StudentView(Long id, Long userId, String fullName, String email, String rollNumber,
        String branch, int year, BigDecimal cgpa, String phone, Set<String> skills, boolean placed) {}
    public record CompanyView(Long id, String name, String description, String website, String industry, String location, int driveCount) {}
    public record Eligibility(boolean eligible, List<String> reasons) {}
    public record DriveView(Long id, CompanyView company, String jobTitle, String description,
        BigDecimal salaryPackage, String location, LocalDate driveDate, LocalDate deadline,
        BigDecimal minCgpa, Set<String> eligibleBranches, Set<String> requiredSkills,
        JobType jobType, boolean active, boolean enabled, boolean applied, int applicantCount, Eligibility eligibility) {}
    public record ApplicationView(Long id, Long driveId, String company, String jobTitle, String location,
        BigDecimal salaryPackage, Instant appliedAt, Status status, StudentView student) {}
    public record StudentDashboard(StudentView student, long availableDrives, long applications,
        long shortlisted, long selected, List<DriveView> upcomingDrives, List<ApplicationView> recentApplications) {}
    public record ChartItem(String label, long value) {}
    public record AdminDashboard(long totalStudents, long totalCompanies, long activeDrives,
        long totalApplications, long selectedStudents, List<ChartItem> applicationsByCompany,
        List<ChartItem> applicationsByBranch, List<ChartItem> applicationStatuses,
        List<ApplicationView> recentApplications, List<DriveView> upcomingDrives) {}
}
