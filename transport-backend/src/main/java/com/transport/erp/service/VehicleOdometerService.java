package com.transport.erp.service;

import com.transport.erp.dto.VehicleOdometerReadingResponse;
import com.transport.erp.dto.VehicleOdometerRequest;
import com.transport.erp.dto.VehicleOdometerResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleOdometerReading;
import com.transport.erp.repository.VehicleOdometerReadingRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleOdometerService {

    static final String SOURCE_OPENING = "OPENING";
    static final String SOURCE_MANUAL = "MANUAL";
    static final String SOURCE_CORRECTION = "CORRECTION";

    private static final Set<String> ALLOWED_SOURCES = Set.of(SOURCE_OPENING, SOURCE_MANUAL, SOURCE_CORRECTION);
    private static final Set<String> WRITE_ROLES = Set.of("COMPANY_ADMIN", "BRANCH_MANAGER");

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleOdometerReadingRepository readingRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    public VehicleOdometerResponse getOdometer(Long vehicleId) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        assertVehicleBranchAccess(vehicle);
        return toResponse(vehicle, loadRecentReadings(vehicleId));
    }

    @Transactional
    public VehicleOdometerResponse recordOdometer(Long vehicleId, VehicleOdometerRequest request, String username) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        assertOdometerWriteAccess(currentUser);

        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));
        if (Boolean.TRUE.equals(vehicle.getIsDeleted())) {
            throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
        }

        tenantAccess.assertOwned(vehicle.getCompanyId());
        assertVehicleBranchAccess(vehicle);

        String source = normalizeSource(request != null ? request.getSource() : null);
        BigDecimal readingKm = request != null ? request.getReadingKm() : null;
        String reason = request != null ? trimToNull(request.getReason()) : null;
        LocalDateTime readingAt = request != null && request.getReadingAt() != null
                ? request.getReadingAt()
                : LocalDateTime.now();

        validateReading(vehicle, source, readingKm, reason);

        BigDecimal oldKm = vehicle.getCurrentOdometerKm();

        VehicleOdometerReading reading = new VehicleOdometerReading();
        reading.setCode("ODO-" + vehicle.getId() + "-" + System.currentTimeMillis());
        reading.setName(source + " odometer reading");
        reading.setDescription(reason);
        reading.setStatus("ACTIVE");
        reading.setCompanyId(vehicle.getCompanyId());
        reading.setBranchId(vehicle.getBranchId());
        reading.setVehicle(vehicle);
        reading.setReadingKm(readingKm);
        reading.setReadingAt(readingAt);
        reading.setSource(source);
        reading.setReason(reason);
        reading.setCreatedBy(username);
        reading.setUpdatedBy(username);
        reading.setIsDeleted(false);
        readingRepository.save(reading);

        vehicle.setCurrentOdometerKm(readingKm);
        vehicle.setOdometerUpdatedAt(readingAt);
        vehicle.setUpdatedBy(username);
        vehicleRepository.save(vehicle);

        String auditAction = SOURCE_CORRECTION.equals(source) ? "ODOMETER_CORRECTED" : "ODOMETER_RECORDED";
        auditService.log(username, auditAction, "vehicles", vehicle.getId(), null,
                String.format("vehicleId=%d, oldKm=%s, newKm=%s, source=%s, reason=%s",
                        vehicle.getId(),
                        oldKm,
                        readingKm,
                        source,
                        reason != null ? reason : ""));

        return toResponse(vehicle, loadRecentReadings(vehicleId));
    }

    private void validateReading(Vehicle vehicle, String source, BigDecimal readingKm, String reason) {
        if (readingKm == null || readingKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(
                    "Invalid Odometer",
                    "INVALID_ODOMETER",
                    "Odometer reading KM must be zero or greater.",
                    "Enter a valid non-negative odometer reading."
            );
        }

        if (!ALLOWED_SOURCES.contains(source)) {
            throw new BusinessValidationException(
                    "Invalid Odometer Source",
                    "INVALID_ODOMETER_SOURCE",
                    "Odometer source must be OPENING, MANUAL, or CORRECTION.",
                    "Use an allowed Phase 1 odometer source."
            );
        }

        BigDecimal currentKm = vehicle.getCurrentOdometerKm();

        if (SOURCE_OPENING.equals(source)) {
            if (currentKm != null) {
                throw new BusinessValidationException(
                        "Odometer Already Initialized",
                        "ODOMETER_ALREADY_INITIALIZED",
                        "Opening KM can be recorded only when the vehicle has no authoritative current KM.",
                        "Use MANUAL to increase KM or CORRECTION to adjust an existing value."
                );
            }
            return;
        }

        if (currentKm == null) {
            throw new BusinessValidationException(
                    "Odometer Not Initialized",
                    "ODOMETER_NOT_INITIALIZED",
                    "Record an OPENING reading before MANUAL or CORRECTION readings.",
                    "Submit source OPENING with the vehicle's starting KM."
            );
        }

        if (SOURCE_MANUAL.equals(source) && readingKm.compareTo(currentKm) < 0) {
            throw new BusinessValidationException(
                    "Odometer Moved Backwards",
                    "ODOMETER_MOVED_BACKWARDS",
                    "Manual odometer readings cannot be lower than the current authoritative KM.",
                    "Enter a reading greater than or equal to the current KM, or use CORRECTION with a reason."
            );
        }

        if (SOURCE_CORRECTION.equals(source) && reason == null) {
            throw new BusinessValidationException(
                    "Correction Reason Required",
                    "ODOMETER_CORRECTION_REASON_REQUIRED",
                    "A reason is required when correcting vehicle odometer KM.",
                    "Provide a reason describing why the odometer is being corrected."
            );
        }
    }

    private void assertOdometerWriteAccess(AppUser user) {
        if (tenantAccess.isSuperAdmin(user)) {
            return;
        }
        if (user.getRoles() == null || user.getRoles().stream().map(AppRole::getCode).noneMatch(WRITE_ROLES::contains)) {
            throw new AccessDeniedException("Access denied: odometer updates require COMPANY_ADMIN or BRANCH_MANAGER.");
        }
    }

    private void assertVehicleBranchAccess(Vehicle vehicle) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (tenantAccess.isSuperAdmin(currentUser)) {
            return;
        }
        if (currentUser.getBranchId() != null && vehicle.getBranchId() != null
                && !currentUser.getBranchId().equals(vehicle.getBranchId())) {
            throw new AccessDeniedException("Access denied: Vehicle belongs to another branch.");
        }
    }

    private List<VehicleOdometerReading> loadRecentReadings(Long vehicleId) {
        return readingRepository.findTop20ByVehicle_IdAndIsDeletedFalseOrderByReadingAtDescIdDesc(vehicleId);
    }

    private VehicleOdometerResponse toResponse(Vehicle vehicle, List<VehicleOdometerReading> readings) {
        VehicleOdometerResponse response = new VehicleOdometerResponse();
        response.setVehicleId(vehicle.getId());
        response.setCurrentOdometerKm(vehicle.getCurrentOdometerKm());
        response.setOdometerUpdatedAt(vehicle.getOdometerUpdatedAt());
        List<VehicleOdometerReadingResponse> recent = readings.stream()
                .map(this::toReadingResponse)
                .collect(Collectors.toList());
        response.setRecentReadings(recent);
        response.setLatestReading(recent.isEmpty() ? null : recent.get(0));
        return response;
    }

    private VehicleOdometerReadingResponse toReadingResponse(VehicleOdometerReading reading) {
        VehicleOdometerReadingResponse dto = new VehicleOdometerReadingResponse();
        dto.setId(reading.getId());
        dto.setReadingKm(reading.getReadingKm());
        dto.setReadingAt(reading.getReadingAt());
        dto.setSource(reading.getSource());
        dto.setReason(reading.getReason());
        dto.setCreatedBy(reading.getCreatedBy());
        dto.setCreatedDate(reading.getCreatedDate());
        return dto;
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        return source.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
