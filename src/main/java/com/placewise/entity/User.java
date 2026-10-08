package com.placewise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    public enum Role { ROLE_STUDENT, ROLE_ADMIN }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 180) public String email;
    @Column(nullable = false) public String passwordHash;
    @Column(nullable = false, length = 100) public String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Role role;
}
