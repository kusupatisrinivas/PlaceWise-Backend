package com.placewise.entity;

import jakarta.persistence.*;

@Entity @Table(name = "skills")
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 50) public String name;
    public Skill() { }
    public Skill(String name) { this.name = name; }
}
