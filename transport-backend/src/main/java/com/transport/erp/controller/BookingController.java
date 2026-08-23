package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.Booking;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<Booking>> getBookings(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<Booking> data = bookingService.getBookings(targetCompanyId, status, pageable);
        return ApiResponse.success(data, "Bookings fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<Booking> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ApiResponse.success(booking, "Booking details fetched successfully");
    }

    @PostMapping
    public ApiResponse<Booking> create(@RequestBody Booking booking) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking created = bookingService.createBooking(booking, activeUser);
        return ApiResponse.success(created, "Booking request registered successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<Booking> update(@PathVariable Long id, @RequestBody Booking booking) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking updated = bookingService.updateBooking(id, booking, activeUser);
        return ApiResponse.success(updated, "Booking modified successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        bookingService.deleteBooking(id, activeUser);
        return ApiResponse.success(null, "Booking cancelled successfully");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Booking> approve(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking approved = bookingService.approveBooking(id, activeUser);
        return ApiResponse.success(approved, "Booking approved successfully");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Booking> reject(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking rejected = bookingService.rejectBooking(id, activeUser);
        return ApiResponse.success(rejected, "Booking rejected successfully");
    }
}
