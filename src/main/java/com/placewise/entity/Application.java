package com.placewise.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "applications", uniqueConstraints = @UniqueConstraint(name = "uk_student_drive", columnNames = {"student_id", "drive_id"}))
public class Application {
    public enum Status { APPLIED, SHORTLISTED, INTERVIEW, SELECTED, REJECTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") public Student student;
    @ManyToOne(optional = false) @JoinColumn(name = "drive_id") public PlacementDrive drive;
    @Column(nullable = false) public Instant appliedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.APPLIED;
}
