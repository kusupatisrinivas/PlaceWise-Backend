package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.service.PortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/students")
public class StudentController {
    private final PortalService service;
    public StudentController(PortalService service) { this.service=service; }
    @GetMapping("/profile") @PreAuthorize("hasRole('STUDENT')")
    public StudentView profile() { return service.profile(); }
    @PutMapping("/profile") @PreAuthorize("hasRole('STUDENT')")
    public StudentView update(@Valid @RequestBody ProfileInput input) { return service.updateProfile(input); }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<StudentView> list(@RequestParam(required=false) String search) { return service.listStudents(search); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public StudentView edit(@PathVariable Long id, @Valid @RequestBody ProfileInput input) { return service.updateStudent(id,input); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deleteStudent(id); }
}
