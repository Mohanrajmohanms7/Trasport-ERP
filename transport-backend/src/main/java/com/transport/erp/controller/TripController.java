package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Trip;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
@CrossOrigin(origins = "*")
public class TripController {

    @Autowired
    private TripService tripService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Trip>> getTrips(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<Trip> data = tripService.getTrips(targetCompanyId, status, pageable);
        return ApiResponse.success(data, "Trips fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<Trip> getTripById(@PathVariable Long id) {
        Trip trip = tripService.getTripById(id);
        return ApiResponse.success(trip, "Trip details fetched successfully");
    }

    @PostMapping
    public ApiResponse<Trip> create(@RequestBody Trip trip) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Trip created = tripService.createTrip(trip, activeUser);
        return ApiResponse.success(created, "Trip planned successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<Trip> update(@PathVariable Long id, @RequestBody Trip trip) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Trip updated = tripService.updateTrip(id, trip, activeUser);
        return ApiResponse.success(updated, "Trip details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        tripService.deleteTrip(id, activeUser);
        return ApiResponse.success(null, "Trip deleted successfully");
    }

    @PostMapping("/{id}/dispatch")
    public ApiResponse<Trip> dispatch(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Trip dispatched = tripService.dispatchTrip(id, activeUser);
        return ApiResponse.success(dispatched, "Trip dispatched successfully");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Trip> complete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Trip completed = tripService.completeTrip(id, activeUser);
        return ApiResponse.success(completed, "Trip completed successfully");
    }
}
