package com.placewise.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @OneToOne(optional = false, cascade = CascadeType.ALL) @JoinColumn(name = "user_id", unique = true) public User user;
    @Column(nullable = false, unique = true, length = 30) public String rollNumber;
    @Column(nullable = false, length = 20) public String branch;
    @Column(name = "study_year", nullable = false) public int year;
    @Column(nullable = false, precision = 4, scale = 2) public BigDecimal cgpa;
    @Column(nullable = false, length = 10) public String phone;
    @ManyToMany @JoinTable(name = "student_skills", joinColumns = @JoinColumn(name = "student_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    public Set<Skill> skills = new LinkedHashSet<>();
    @OneToMany(mappedBy = "student", cascade = CascadeType.REMOVE) public List<Application> applications = new ArrayList<>();
}
