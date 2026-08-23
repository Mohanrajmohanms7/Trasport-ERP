package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.service.VehicleDriverAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ApiResponse<VehicleDriverAssignment> assignDriver(
            @PathVariable Long vehicleId,
            @PathVariable Long driverId) {
        VehicleDriverAssignment assignment = assignmentService.assignDriver(vehicleId, driverId);
        return ApiResponse.success(assignment, "Driver assigned to vehicle successfully");
    }

    @DeleteMapping
    public ApiResponse<Void> unassignDriver(@PathVariable Long vehicleId) {
        assignmentService.unassignDriver(vehicleId);
        return ApiResponse.success(null, "Driver unassigned from vehicle successfully");
    }
}
