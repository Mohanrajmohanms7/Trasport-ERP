package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.FuelRequest;
import com.transport.erp.service.DriverAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Driver App: a DRIVER login's own profile, trips, bata and fuel requests (see DriverAppService). */
@RestController
@RequestMapping("/api/v1/driver-app")
public class DriverAppController {

    @Autowired private DriverAppService service;

    public record FuelRequestBody(Long tripId, BigDecimal litres, BigDecimal amount) {}

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() { return ApiResponse.success(service.profile(), "Your details"); }

    @GetMapping("/trips")
    public ApiResponse<List<Map<String, Object>>> trips(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(service.trips(from, to), "Your trips");
    }

    @GetMapping("/bata")
    public ApiResponse<Map<String, Object>> bata(@RequestParam(required = false) String month) {
        return ApiResponse.success(service.bata(month == null || month.isBlank() ? null : YearMonth.parse(month)), "Your bata");
    }

    @GetMapping("/fuel-requests")
    public ApiResponse<List<Map<String, Object>>> fuelRequests() { return ApiResponse.success(service.fuelRequests(), "Your fuel requests"); }

    @PostMapping("/fuel-requests")
    public ApiResponse<Map<String, Object>> requestFuel(@RequestBody FuelRequestBody body) {
        FuelRequest r = service.requestFuel(body.tripId(), body.litres(), body.amount());
        return ApiResponse.success(Map.of("id", r.getId(), "requestNumber", String.valueOf(r.getRequestNumber()), "status", String.valueOf(r.getStatus())),
                "Fuel request sent for approval");
    }
}
