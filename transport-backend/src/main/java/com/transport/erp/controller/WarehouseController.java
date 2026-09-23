package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.WarehouseRequest;
import com.transport.erp.dto.WarehouseResponse;
import com.transport.erp.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warehouses")
@CrossOrigin(origins = "*")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping
    public ApiResponse<Page<WarehouseResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String code,
            Pageable pageable) {
        return ApiResponse.success(
                warehouseService.list(companyId, branchId, status, code, pageable),
                "Warehouses fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseResponse> get(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.get(id), "Warehouse fetched successfully");
    }

    @PostMapping
    public ApiResponse<WarehouseResponse> create(@RequestBody WarehouseRequest request, Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.success(warehouseService.create(request, username), "Warehouse created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<WarehouseResponse> update(
            @PathVariable Long id,
            @RequestBody WarehouseRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.success(warehouseService.update(id, request, username), "Warehouse updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        warehouseService.delete(id, username);
        return ApiResponse.success(null, "Warehouse deleted successfully");
    }
}
