package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.AuthorizedVehicleResponse;
import com.transport.erp.dto.MaintenanceRequestCancelRequest;
import com.transport.erp.dto.MaintenanceRequestCreateRequest;
import com.transport.erp.dto.MaintenanceRequestResponse;
import com.transport.erp.dto.MaintenanceRequestReviewRequest;
import com.transport.erp.dto.MaintenanceRequestUpdateRequest;
import com.transport.erp.service.MaintenanceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance-requests")
@CrossOrigin(origins = "*")
public class MaintenanceRequestController {

    @Autowired
    private MaintenanceRequestService maintenanceRequestService;

    @GetMapping
    public ApiResponse<Page<MaintenanceRequestResponse>> list(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long requestedById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        return ApiResponse.success(
                maintenanceRequestService.list(
                        vehicleId, status, priority, requestedById, fromDate, toDate, branchId, companyId, pageable),
                "Maintenance requests fetched successfully");
    }

    @GetMapping("/authorized-vehicles")
    public ApiResponse<List<AuthorizedVehicleResponse>> authorizedVehicles() {
        return ApiResponse.success(
                maintenanceRequestService.authorizedVehicles(),
                "Authorized vehicles fetched successfully");
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<MaintenanceRequestResponse> get(@PathVariable Long id) {
        return ApiResponse.success(maintenanceRequestService.get(id), "Maintenance request fetched successfully");
    }

    @PostMapping
    public ApiResponse<MaintenanceRequestResponse> create(
            @RequestBody MaintenanceRequestCreateRequest request,
            Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.create(request, username(auth)),
                "Maintenance request created successfully");
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<MaintenanceRequestResponse> update(
            @PathVariable Long id,
            @RequestBody MaintenanceRequestUpdateRequest request,
            Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.update(id, request, username(auth)),
                "Maintenance request updated successfully");
    }

    @PostMapping("/{id:\\d+}/review")
    public ApiResponse<MaintenanceRequestResponse> review(
            @PathVariable Long id,
            @RequestBody(required = false) MaintenanceRequestReviewRequest request,
            Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.review(id, request, username(auth)),
                "Maintenance request reviewed successfully");
    }

    @PostMapping("/{id:\\d+}/approve")
    public ApiResponse<MaintenanceRequestResponse> approve(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.approve(id, username(auth)),
                "Maintenance request approved successfully");
    }

    @PostMapping("/{id:\\d+}/cancel")
    public ApiResponse<MaintenanceRequestResponse> cancel(
            @PathVariable Long id,
            @RequestBody MaintenanceRequestCancelRequest request,
            Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.cancel(id, request, username(auth)),
                "Maintenance request cancelled successfully");
    }

    @PostMapping("/{id:\\d+}/convert")
    public ApiResponse<MaintenanceRequestResponse> convert(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(
                maintenanceRequestService.convert(id, username(auth)),
                "Maintenance request converted successfully");
    }

    private String username(Authentication auth) {
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
