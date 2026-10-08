package com.placewise.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 100) public String name;
    @Column(nullable = false, length = 3000) public String description;
    @Column(nullable = false, length = 300) public String website;
    @Column(nullable = false, length = 100) public String industry;
    @Column(nullable = false, length = 100) public String location;
    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE) public List<PlacementDrive> drives = new ArrayList<>();
}
