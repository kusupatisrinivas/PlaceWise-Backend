package com.placewise.controller;
import com.placewise.dto.ApiDtos.*;
import com.placewise.service.PortalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/companies")
public class CompanyController {
    private final PortalService service;
    public CompanyController(PortalService service) { this.service=service; }
    @GetMapping public List<CompanyView> list() { return service.listCompanies(); }
    @GetMapping("/{id}") public CompanyView get(@PathVariable Long id) { return service.getCompany(id); }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    public CompanyView create(@Valid @RequestBody CompanyInput input) { return service.saveCompany(null,input); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CompanyView update(@PathVariable Long id, @Valid @RequestBody CompanyInput input) { return service.saveCompany(id,input); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deleteCompany(id); }
}
