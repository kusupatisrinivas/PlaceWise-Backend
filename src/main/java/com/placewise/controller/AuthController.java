package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service=service; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody Register input) { return service.register(input); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody Login input) { return service.login(input); }
}
