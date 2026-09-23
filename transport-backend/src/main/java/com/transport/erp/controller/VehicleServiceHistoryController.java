package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.ServiceHistoryResponse;
import com.transport.erp.service.VehicleServiceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/service-history")
@CrossOrigin(origins = "*")
public class VehicleServiceHistoryController {

    @Autowired
    private VehicleServiceHistoryService vehicleServiceHistoryService;

    @GetMapping
    public ApiResponse<Page<ServiceHistoryResponse>> history(
            @PathVariable Long vehicleId,
            Pageable pageable) {
        return ApiResponse.success(
                vehicleServiceHistoryService.history(vehicleId, pageable),
                "Service history fetched successfully");
    }
}
