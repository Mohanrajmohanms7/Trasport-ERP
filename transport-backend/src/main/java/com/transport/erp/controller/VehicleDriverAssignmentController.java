package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.service.VehicleDriverAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/driver")
@CrossOrigin(origins = "*")
public class VehicleDriverAssignmentController {

    @Autowired
    private VehicleDriverAssignmentService assignmentService;

    @GetMapping
    public ApiResponse<List<VehicleDriverAssignment>> getAssignments(@PathVariable Long vehicleId) {
        List<VehicleDriverAssignment> history = assignmentService.getAssignmentsByVehicle(vehicleId);
        return ApiResponse.success(history, "Driver assignments history fetched successfully");
    }

    @PostMapping("/{driverId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'FLEET_MANAGER')")
    public ApiResponse<VehicleDriverAssignment> assignDriver(
            @PathVariable Long vehicleId,
            @PathVariable Long driverId) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        VehicleDriverAssignment assignment = assignmentService.assignDriver(vehicleId, driverId, activeUser);
        return ApiResponse.success(assignment, "Driver assigned to vehicle successfully");
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'BRANCH_MANAGER', 'FLEET_MANAGER')")
    public ApiResponse<VehicleDriverAssignment> unassignDriver(@PathVariable Long vehicleId) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        VehicleDriverAssignment unassigned = assignmentService.unassignDriver(vehicleId, activeUser);
        return ApiResponse.success(unassigned, "Driver unassigned from vehicle successfully");
    }
}
