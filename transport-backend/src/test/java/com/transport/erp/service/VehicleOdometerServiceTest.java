package com.transport.erp.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VehicleOdometerServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleOdometerReadingRepository readingRepository;

    @Mock
    private TenantAccessService tenantAccess;

    @Mock
    private TenantParentAccess parentAccess;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VehicleOdometerService odometerService;

    private Vehicle vehicle;
    private AppUser manager;
    private final VehicleOdometerReading[] lastSavedReading = new VehicleOdometerReading[1];

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(10L);
        vehicle.setCode("TN-01-AB-1000");
        vehicle.setName("TATA Truck");
        vehicle.setCompanyId(3L);
        vehicle.setBranchId(1L);
        vehicle.setStatus("ACTIVE");
        vehicle.setIsDeleted(false);
        vehicle.setCurrentOdometerKm(null);
        vehicle.setOdometerUpdatedAt(null);

        manager = userWithRole("manager", "COMPANY_ADMIN");

        when(tenantAccess.requireCurrentUser()).thenReturn(manager);
        when(tenantAccess.isSuperAdmin(any(AppUser.class))).thenReturn(false);
        doNothing().when(tenantAccess).assertOwned(any());
        when(parentAccess.requireVehicle(10L)).thenReturn(vehicle);
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
        when(readingRepository.save(any(VehicleOdometerReading.class))).thenAnswer(i -> {
            VehicleOdometerReading reading = i.getArgument(0);
            reading.setId(500L);
            reading.setCreatedDate(LocalDateTime.now());
            lastSavedReading[0] = reading;
            return reading;
        });
        when(readingRepository.findTop20ByVehicle_IdAndIsDeletedFalseOrderByReadingAtDescIdDesc(10L))
                .thenAnswer(i -> lastSavedReading[0] == null ? List.of() : List.of(lastSavedReading[0]));
    }

    @Test
    @DisplayName("1. Opening KM initializes authoritative current KM and history")
    void openingKm_initializesCurrentKm() {
        VehicleOdometerResponse response = odometerService.recordOdometer(10L, request("OPENING", "48500", null), "manager");

        assertEquals(0, new BigDecimal("48500").compareTo(vehicle.getCurrentOdometerKm()));
        assertNotNull(vehicle.getOdometerUpdatedAt());
        assertEquals(0, new BigDecimal("48500").compareTo(response.getCurrentOdometerKm()));
        verify(readingRepository, times(1)).save(any(VehicleOdometerReading.class));
        assertEquals("OPENING", lastSavedReading[0].getSource());
        assertEquals(3L, lastSavedReading[0].getCompanyId());
        assertEquals(1L, lastSavedReading[0].getBranchId());
        verify(auditService).log(eq("manager"), eq("ODOMETER_RECORDED"), eq("vehicles"), eq(10L), isNull(), contains("source=OPENING"));
    }

    @Test
    @DisplayName("2. Opening KM is rejected after initialization")
    void openingKm_rejectedWhenAlreadyInitialized() {
        vehicle.setCurrentOdometerKm(new BigDecimal("48500"));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                odometerService.recordOdometer(10L, request("OPENING", "49000", null), "manager")
        );

        assertEquals("ODOMETER_ALREADY_INITIALIZED", ex.getErrorCode());
        verify(readingRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("3. Manual increase updates current KM")
    void manualIncrease_succeeds() {
        vehicle.setCurrentOdometerKm(new BigDecimal("48500"));

        VehicleOdometerResponse response = odometerService.recordOdometer(10L, request("MANUAL", "49200", null), "manager");

        assertEquals(0, new BigDecimal("49200").compareTo(vehicle.getCurrentOdometerKm()));
        assertEquals(0, new BigDecimal("49200").compareTo(response.getCurrentOdometerKm()));
        assertEquals("MANUAL", lastSavedReading[0].getSource());
        verify(auditService).log(eq("manager"), eq("ODOMETER_RECORDED"), eq("vehicles"), eq(10L), isNull(), contains("source=MANUAL"));
    }

    @Test
    @DisplayName("4. Manual decrease is rejected")
    void manualDecrease_rejected() {
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                odometerService.recordOdometer(10L, request("MANUAL", "48900", null), "manager")
        );

        assertEquals("ODOMETER_MOVED_BACKWARDS", ex.getErrorCode());
        assertEquals(0, new BigDecimal("49200").compareTo(vehicle.getCurrentOdometerKm()));
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Authorized correction decrease succeeds and is audited")
    void correctionDecrease_succeeds() {
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));
        String reason = "Dashboard entered incorrect opening value";

        VehicleOdometerResponse response = odometerService.recordOdometer(
                10L, request("CORRECTION", "48900", reason), "manager");

        assertEquals(0, new BigDecimal("48900").compareTo(vehicle.getCurrentOdometerKm()));
        assertEquals(0, new BigDecimal("48900").compareTo(response.getCurrentOdometerKm()));
        assertEquals("CORRECTION", lastSavedReading[0].getSource());
        assertEquals(reason, lastSavedReading[0].getReason());
        verify(auditService).log(eq("manager"), eq("ODOMETER_CORRECTED"), eq("vehicles"), eq(10L), isNull(),
                contains("source=CORRECTION"));
        verify(auditService).log(eq("manager"), eq("ODOMETER_CORRECTED"), eq("vehicles"), eq(10L), isNull(),
                contains("oldKm=49200"));
    }

    @Test
    @DisplayName("6. Correction without reason is rejected")
    void correctionWithoutReason_rejected() {
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                odometerService.recordOdometer(10L, request("CORRECTION", "48900", "  "), "manager")
        );

        assertEquals("ODOMETER_CORRECTION_REASON_REQUIRED", ex.getErrorCode());
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Operator cannot perform correction")
    void operatorCorrection_accessDenied() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("operator", "OPERATOR"));
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                odometerService.recordOdometer(10L, request("CORRECTION", "48900", "fix"), "operator")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("access denied"));
        verify(vehicleRepository, never()).findByIdForUpdate(any());
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("7b. Accountant cannot perform correction")
    void accountantCorrection_accessDenied() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("acct", "ACCOUNTANT"));
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));

        assertThrows(AccessDeniedException.class, () ->
                odometerService.recordOdometer(10L, request("CORRECTION", "48900", "fix"), "acct")
        );
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("7c. Viewer cannot perform correction")
    void viewerCorrection_accessDenied() {
        when(tenantAccess.requireCurrentUser()).thenReturn(userWithRole("viewer", "VIEWER"));
        vehicle.setCurrentOdometerKm(new BigDecimal("49200"));

        assertThrows(AccessDeniedException.class, () ->
                odometerService.recordOdometer(10L, request("CORRECTION", "48900", "fix"), "viewer")
        );
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Tenant isolation denies another company's vehicle")
    void tenantIsolation_accessDenied() {
        vehicle.setCompanyId(99L);
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertOwned(99L);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                odometerService.recordOdometer(10L, request("OPENING", "48500", null), "manager")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("access denied"));
        verify(readingRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("9. Pessimistic lock is taken before KM validation and writes")
    void concurrentUpdate_locksVehicleBeforeRead() {
        vehicle.setCurrentOdometerKm(new BigDecimal("50000"));

        odometerService.recordOdometer(10L, request("MANUAL", "50500", null), "manager");

        InOrder inOrder = inOrder(vehicleRepository, readingRepository, auditService);
        inOrder.verify(vehicleRepository).findByIdForUpdate(10L);
        inOrder.verify(readingRepository).save(any(VehicleOdometerReading.class));
        inOrder.verify(vehicleRepository).save(vehicle);
        inOrder.verify(auditService).log(eq("manager"), eq("ODOMETER_RECORDED"), eq("vehicles"), eq(10L), isNull(), anyString());
    }

    @Test
    @DisplayName("Negative KM is rejected")
    void negativeKm_rejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                odometerService.recordOdometer(10L, request("OPENING", "-1", null), "manager")
        );
        assertEquals("INVALID_ODOMETER", ex.getErrorCode());
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unknown source including FUEL is rejected")
    void invalidSource_rejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                odometerService.recordOdometer(10L, request("FUEL", "48500", null), "manager")
        );
        assertEquals("INVALID_ODOMETER_SOURCE", ex.getErrorCode());
        verify(readingRepository, never()).save(any());
    }

    @Test
    @DisplayName("GET odometer returns current KM and recent history")
    void getOdometer_returnsCurrentAndHistory() {
        vehicle.setCurrentOdometerKm(new BigDecimal("48500"));
        vehicle.setOdometerUpdatedAt(LocalDateTime.of(2026, 9, 19, 10, 0));
        VehicleOdometerReading reading = new VehicleOdometerReading();
        reading.setId(1L);
        reading.setReadingKm(new BigDecimal("48500"));
        reading.setSource("OPENING");
        reading.setReadingAt(vehicle.getOdometerUpdatedAt());
        when(readingRepository.findTop20ByVehicle_IdAndIsDeletedFalseOrderByReadingAtDescIdDesc(10L))
                .thenReturn(List.of(reading));

        VehicleOdometerResponse response = odometerService.getOdometer(10L);

        assertEquals(10L, response.getVehicleId());
        assertEquals(0, new BigDecimal("48500").compareTo(response.getCurrentOdometerKm()));
        assertNotNull(response.getLatestReading());
        assertEquals(1, response.getRecentReadings().size());
        verify(parentAccess).requireVehicle(10L);
    }

    @Test
    @DisplayName("Missing vehicle uses standard not-found behavior")
    void missingVehicle_notFound() {
        when(vehicleRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                odometerService.recordOdometer(10L, request("OPENING", "48500", null), "manager")
        );
        assertTrue(ex.getMessage().contains("Vehicle not found"));
    }

    private VehicleOdometerRequest request(String source, String km, String reason) {
        VehicleOdometerRequest req = new VehicleOdometerRequest();
        req.setSource(source);
        req.setReadingKm(new BigDecimal(km));
        req.setReason(reason);
        return req;
    }

    private AppUser userWithRole(String username, String roleCode) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername(username);
        user.setCompanyId(3L);
        user.setBranchId(1L);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        role.setName(roleCode);
        Set<AppRole> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }
}
