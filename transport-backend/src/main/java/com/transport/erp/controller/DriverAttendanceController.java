package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.DriverAttendance;
import com.transport.erp.service.DriverAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/attendance")
@CrossOrigin(origins = "*")
public class DriverAttendanceController {

    @Autowired
    private DriverAttendanceService attendanceService;

    @GetMapping
    public ApiResponse<List<DriverAttendance>> getAttendance(@PathVariable Long driverId) {
        List<DriverAttendance> logs = attendanceService.getAttendanceByDriver(driverId);
        return ApiResponse.success(logs, "Driver attendance history fetched successfully");
    }

    @PostMapping
    public ApiResponse<DriverAttendance> logAttendance(
            @PathVariable Long driverId,
            @RequestBody DriverAttendance log) {
        DriverAttendance created = attendanceService.logAttendance(driverId, log);
        return ApiResponse.success(created, "Driver attendance logged successfully");
    }
}
