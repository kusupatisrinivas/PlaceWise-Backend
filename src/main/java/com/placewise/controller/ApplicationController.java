package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.service.PortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/applications")
public class ApplicationController {
    private final PortalService service;
    public ApplicationController(PortalService service) { this.service=service; }
    @PostMapping @PreAuthorize("hasRole('STUDENT')") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView apply(@Valid @RequestBody ApplyInput input) { return service.apply(input.driveId()); }
    @GetMapping("/my") @PreAuthorize("hasRole('STUDENT')")
    public List<ApplicationView> mine() { return service.myApplications(); }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<ApplicationView> all() { return service.listApplications(null); }
    @GetMapping("/drive/{driveId}") @PreAuthorize("hasRole('ADMIN')")
    public List<ApplicationView> drive(@PathVariable Long driveId) { return service.listApplications(driveId); }
    @PutMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public ApplicationView status(@PathVariable Long id, @Valid @RequestBody StatusInput input) { return service.updateStatus(id,input.status()); }
}
