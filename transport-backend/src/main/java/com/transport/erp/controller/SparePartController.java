package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.SparePartRequest;
import com.transport.erp.dto.SparePartResponse;
import com.transport.erp.model.UomMaster;
import com.transport.erp.service.SparePartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spare-parts")
@CrossOrigin(origins = "*")
public class SparePartController {

    @Autowired
    private SparePartService sparePartService;

    @GetMapping
    public ApiResponse<Page<SparePartResponse>> list(
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        return ApiResponse.success(sparePartService.list(companyId, pageable), "Spare parts fetched successfully");
    }

    @GetMapping("/uoms")
    public ApiResponse<List<UomMaster>> uoms(@RequestParam(required = false) Long companyId) {
        return ApiResponse.success(sparePartService.usableUoms(companyId), "Units fetched successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<SparePartResponse> update(@PathVariable Long id, @RequestBody SparePartRequest request, Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.success(sparePartService.update(id, request, username), "Spare part updated");
    }

    @PostMapping
    public ApiResponse<SparePartResponse> create(
            @RequestBody SparePartRequest request,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "SYSTEM";
        return ApiResponse.success(sparePartService.create(request, username), "Spare part created successfully");
    }
}
