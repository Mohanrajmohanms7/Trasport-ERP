package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.FuelEntry;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.FuelEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fuel")
@CrossOrigin(origins = "*")
public class FuelEntryController {

    @Autowired
    private FuelEntryService fuelEntryService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<FuelEntry>> getFuelEntries(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<FuelEntry> data = fuelEntryService.getFuelEntries(targetCompanyId, pageable);
        return ApiResponse.success(data, "Fuel Entries fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<FuelEntry> getFuelEntryById(@PathVariable Long id) {
        FuelEntry entry = fuelEntryService.getFuelEntryById(id);
        return ApiResponse.success(entry, "Fuel Entry details fetched successfully");
    }

    @PostMapping
    public ApiResponse<FuelEntry> create(@RequestBody FuelEntry entry) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelEntry created = fuelEntryService.createFuelEntry(entry, activeUser);
        return ApiResponse.success(created, "Fuel Entry recorded successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<FuelEntry> update(@PathVariable Long id, @RequestBody FuelEntry entry) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        FuelEntry updated = fuelEntryService.updateFuelEntry(id, entry, activeUser);
        return ApiResponse.success(updated, "Fuel Entry updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        fuelEntryService.deleteFuelEntry(id, activeUser);
        return ApiResponse.success(null, "Fuel Entry deleted successfully");
    }
}
