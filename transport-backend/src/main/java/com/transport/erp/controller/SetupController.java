package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.SetupStatusResponse;
import com.transport.erp.service.SetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
@Tag(name = "Setup", description = "First-run setup wizard status")
public class SetupController {

    private final SetupService setupService;

    @GetMapping("/status")
    @Operation(summary = "Check whether the ERP still needs first-run setup")
    public ApiResponse<SetupStatusResponse> getStatus() {
        return ApiResponse.success(setupService.getStatus(), "Setup status retrieved");
    }

    @PostMapping("/complete")
    @Operation(summary = "Mark the setup wizard as finished")
    public ApiResponse<SetupStatusResponse> completeSetup() {
        return ApiResponse.success(setupService.completeSetup(), "Setup marked as completed");
    }

    @PostMapping("/seed-supporting")
    @Operation(summary = "Seed lookups/materials/quarry for current company (no customer/vehicle/driver)")
    public ApiResponse<Map<String, Object>> seedSupportingExampleData() {
        Map<String, Object> result = setupService.seedSupportingExampleData();
        return ApiResponse.success(result, String.valueOf(result.getOrDefault("message", "Supporting example data seeded")));
    }

    @PostMapping("/seed-demo")
    @Operation(summary = "Reset database and seed complete demo data")
    public ApiResponse<SetupStatusResponse> seedDemoData() {
        setupService.seedDemoData();
        return ApiResponse.success(setupService.getStatus(), "Database reset and demo data seeded successfully");
    }
}

