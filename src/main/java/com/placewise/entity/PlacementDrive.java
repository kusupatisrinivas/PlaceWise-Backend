package com.placewise.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity @Table(name = "placement_drives")
public class PlacementDrive {
    public enum JobType { FULL_TIME, INTERNSHIP }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Company company;
    @Column(nullable = false, length = 150) public String jobTitle;
    @Column(nullable = false, length = 6000) public String description;
    @Column(nullable = false, precision = 8, scale = 2) public BigDecimal salaryPackage;
    @Column(nullable = false, length = 100) public String location;
    @Column(nullable = false) public LocalDate driveDate;
    @Column(nullable = false) public LocalDate deadline;
    @Column(nullable = false, precision = 4, scale = 2) public BigDecimal minCgpa;
    @ElementCollection @CollectionTable(name = "drive_branches", joinColumns = @JoinColumn(name = "drive_id"))
    @Column(name = "branch", nullable = false, length = 20) public Set<String> eligibleBranches = new LinkedHashSet<>();
    @ManyToMany @JoinTable(name = "drive_skills", joinColumns = @JoinColumn(name = "drive_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    public Set<Skill> requiredSkills = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING) @Column(nullable = false) public JobType jobType;
    @Column(nullable = false) public boolean active = true;
    @OneToMany(mappedBy = "drive", cascade = CascadeType.REMOVE) public List<Application> applications = new ArrayList<>();
}
