package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.entity.PlacementDrive.JobType;
import com.placewise.service.PortalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
@RestController @RequestMapping("/api/drives") @Validated
public class DriveController {
    private final PortalService service;
    public DriveController(PortalService service) { this.service=service; }
    @GetMapping public List<DriveView> list(@RequestParam(required=false) @Size(max=150) String search,
        @RequestParam(required=false) String branch, @RequestParam(required=false) @DecimalMin("0") @DecimalMax("10") BigDecimal maxCgpa,
        @RequestParam(required=false) @DecimalMin("0") BigDecimal minPackage, @RequestParam(required=false) JobType jobType,
        @RequestParam(required=false) Long companyId, @RequestParam(required=false) Boolean active) {
        return service.listDrives(search,branch,maxCgpa,minPackage,jobType,companyId,active);
    }
    @GetMapping("/{id}") public DriveView get(@PathVariable Long id) { return service.getDrive(id); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    public DriveView create(@Valid @RequestBody DriveInput input) { return service.saveDrive(null,input); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public DriveView update(@PathVariable Long id, @Valid @RequestBody DriveInput input) { return service.saveDrive(id,input); }
    @PatchMapping("/{id}/active") @PreAuthorize("hasRole('ADMIN')")
    public DriveView toggle(@PathVariable Long id, @RequestBody ActiveInput input) { return service.setActive(id,input.active()); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deleteDrive(id); }
}
