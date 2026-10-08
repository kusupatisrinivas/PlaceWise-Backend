package com.placewise.service;
import com.placewise.dto.ApiDtos.*;
import com.placewise.entity.*;
import com.placewise.exception.ApiException;
import com.placewise.repository.*;
import com.placewise.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service @Transactional
public class AuthService {
    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final String dummyHash;
    public AuthService(UserRepository users, StudentRepository students, PasswordEncoder passwords, JwtService jwt) {
        this.users=users; this.students=students; this.passwords=passwords; this.jwt=jwt;
        this.dummyHash=passwords.encode("constant-time-login-dummy");
    }
    public AuthResponse register(Register input) {
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        String roll = input.rollNumber().strip().toUpperCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw ApiException.conflict("This email address is already registered.");
        if (students.existsByRollNumber(roll)) throw ApiException.conflict("This roll number is already registered.");
        if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw ApiException.bad("Password must be at most 72 UTF-8 bytes.");
        User user = new User();
        user.fullName=input.fullName().strip(); user.email=email;
        user.passwordHash=passwords.encode(input.password()); user.role=User.Role.ROLE_STUDENT;
        Student student = new Student();
        student.user=user; student.rollNumber=roll; student.branch=input.branch().strip().toUpperCase(Locale.ROOT);
        student.year=input.year(); student.cgpa=input.cgpa(); student.phone=input.phone();
        students.saveAndFlush(student);
        return jwt.issue(user);
    }
    public AuthResponse login(Login input) {
        User user = users.findByEmail(input.email().strip().toLowerCase(Locale.ROOT)).orElse(null);
        boolean valid = passwords.matches(input.password(), user == null ? dummyHash : user.passwordHash);
        if (user == null || !valid) throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        return jwt.issue(user);
    }
}
