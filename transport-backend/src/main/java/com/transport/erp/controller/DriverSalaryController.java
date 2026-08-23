package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.DriverSalary;
import com.transport.erp.service.DriverSalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/salary")
@CrossOrigin(origins = "*")
public class DriverSalaryController {

    @Autowired
    private DriverSalaryService salaryService;

    @GetMapping
    public ApiResponse<DriverSalary> getSalary(@PathVariable Long driverId) {
        DriverSalary salary = salaryService.getSalaryByDriver(driverId)
                .orElse(new DriverSalary());
        return ApiResponse.success(salary, "Driver salary details fetched successfully");
    }

    @PostMapping
    public ApiResponse<DriverSalary> saveSalary(
            @PathVariable Long driverId,
            @RequestBody DriverSalary salary) {
        DriverSalary saved = salaryService.saveSalary(driverId, salary);
        return ApiResponse.success(saved, "Driver salary details configured successfully");
    }
}
