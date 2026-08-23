package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.GpsTracking;
import com.transport.erp.service.GpsTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gps")
@CrossOrigin(origins = "*")
public class GpsTrackingController {

    @Autowired
    private GpsTrackingService gpsService;

    @GetMapping("/live")
    public ApiResponse<List<GpsTracking>> getLiveRoute(@RequestParam Long vehicleId) {
        List<GpsTracking> route = gpsService.getVehicleRoute(vehicleId);
        return ApiResponse.success(route, "Vehicle route pings history fetched successfully");
    }

    @PostMapping("/location")
    public ApiResponse<GpsTracking> recordPing(@RequestBody GpsTracking ping) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        GpsTracking saved = gpsService.createPing(ping, activeUser);
        return ApiResponse.success(saved, "GPS ping registered successfully");
    }
}
