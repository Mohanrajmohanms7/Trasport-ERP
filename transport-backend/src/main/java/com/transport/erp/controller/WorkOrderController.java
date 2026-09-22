package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.WorkOrderCancelRequest;
import com.transport.erp.dto.WorkOrderCompleteRequest;
import com.transport.erp.dto.WorkOrderCreateRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.dto.WorkOrderUpdateRequest;
import com.transport.erp.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/work-orders")
@CrossOrigin(origins = "*")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping
    public ApiResponse<Page<WorkOrderResponse>> list(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String maintenanceType,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        Page<WorkOrderResponse> data = workOrderService.list(
                vehicleId, status, source, maintenanceType, branchId, companyId, pageable);
        return ApiResponse.success(data, "Work orders fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success(workOrderService.get(id), "Work order fetched successfully");
    }

    @PostMapping
    public ApiResponse<WorkOrderResponse> create(
            @RequestBody WorkOrderCreateRequest request,
            Authentication auth) {
        WorkOrderResponse data = workOrderService.create(request, username(auth));
        return ApiResponse.success(data, "Work order created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkOrderResponse> update(
            @PathVariable Long id,
            @RequestBody WorkOrderUpdateRequest request,
            Authentication auth) {
        WorkOrderResponse data = workOrderService.update(id, request, username(auth));
        return ApiResponse.success(data, "Work order updated successfully");
    }

    @PostMapping("/{id}/start")
    public ApiResponse<WorkOrderResponse> start(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success(workOrderService.start(id, username(auth)), "Work order started successfully");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<WorkOrderResponse> complete(
            @PathVariable Long id,
            @RequestBody WorkOrderCompleteRequest request,
            Authentication auth) {
        return ApiResponse.success(
                workOrderService.complete(id, request, username(auth)),
                "Work order completed successfully");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<WorkOrderResponse> cancel(
            @PathVariable Long id,
            @RequestBody WorkOrderCancelRequest request,
            Authentication auth) {
        return ApiResponse.success(
                workOrderService.cancel(id, request, username(auth)),
                "Work order cancelled successfully");
    }

    private String username(Authentication auth) {
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
