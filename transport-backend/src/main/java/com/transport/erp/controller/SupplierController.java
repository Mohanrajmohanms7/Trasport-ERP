package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Supplier;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Supplier>> getAll(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<Supplier> suppliers = supplierService.getAll(scopedCompanyId, search, pageable);
            return ApiResponse.success(suppliers, "Suppliers fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch suppliers");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Supplier> getById(@PathVariable Long id) {
        try {
            return supplierService.getById(id)
                    .map(s -> ApiResponse.success(s, "Supplier fetched successfully"))
                    .orElse(ApiResponse.error(Collections.singletonList("Supplier not found"), "Supplier not found"));
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch supplier");
        }
    }

    @PostMapping
    public ApiResponse<Supplier> create(@RequestBody Supplier supplier) {
        try {
            supplier.setCompanyId(tenantAccess.resolveCompanyId(supplier.getCompanyId()));
            Supplier created = supplierService.create(supplier);
            return ApiResponse.success(created, "Supplier created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create supplier");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Supplier> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        try {
            Supplier updated = supplierService.update(id, supplier);
            return ApiResponse.success(updated, "Supplier updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update supplier");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            supplierService.delete(id);
            return ApiResponse.success(null, "Supplier deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete supplier");
        }
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Supplier> toggleStatus(@PathVariable Long id) {
        try {
            Supplier toggled = supplierService.toggleStatus(id);
            return ApiResponse.success(toggled, "Supplier status toggled successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to toggle status");
        }
    }
}
