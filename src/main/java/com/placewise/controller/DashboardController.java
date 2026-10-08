package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.service.PortalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/dashboard")
public class DashboardController {
    private final PortalService service;
    public DashboardController(PortalService service) { this.service=service; }
    @GetMapping("/student") @PreAuthorize("hasRole('STUDENT')")
    public StudentDashboard student() { return service.studentDashboard(); }
    @GetMapping("/admin") @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboard admin() { return service.adminDashboard(); }
}
