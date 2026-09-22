package com.transport.erp.service;

import com.transport.erp.dto.MaintenanceDueDashboardResponse;
import com.transport.erp.repository.BookingRepository;
import com.transport.erp.repository.CustomerReceiptRepository;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.ExpenseRepository;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceMaintenanceDueTest {

    @Mock private TenantAccessService tenantAccess;
    @Mock private TripRepository tripRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private SalesInvoiceRepository salesInvoiceRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private FuelEntryRepository fuelEntryRepository;
    @Mock private CustomerReceiptRepository customerReceiptRepository;
    @Mock private MaintenanceDueDashboardService maintenanceDueDashboardService;

    @InjectMocks
    private DashboardService dashboardService;

    private final MaintenanceDueDashboardResponse projection = MaintenanceDueDashboardResponse.empty();

    @BeforeEach
    void setUp() {
        when(tenantAccess.resolveCompanyId(null)).thenReturn(3L);
        when(maintenanceDueDashboardService.build(eq(3L), any(LocalDate.class))).thenReturn(projection);
        when(vehicleRepository.countExpiringFitness(eq(3L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(7L);
        when(vehicleRepository.countExpiringInsurance(eq(3L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(2L);
        when(vehicleRepository.countExpiringPermit(eq(3L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4L);
    }

    @Test
    @DisplayName("Vehicle dashboard keeps maintenanceDueCount as fitness expiry")
    void vehicleDashboardFitnessCountUnchanged() {
        Map<String, Object> data = dashboardService.getVehicleDashboard();
        assertEquals(7L, data.get("maintenanceDueCount"));
        verify(vehicleRepository).countExpiringFitness(eq(3L), any(LocalDate.class), any(LocalDate.class));
        assertSame(projection, data.get("maintenanceDueDashboard"));
    }

    @Test
    @DisplayName("Admin dashboard exposes preventive maintenance as a separate field")
    void adminDashboardIncludesPreventiveProjection() {
        Map<String, Object> data = dashboardService.getAdminDashboard();
        assertSame(projection, data.get("maintenanceDueDashboard"));
    }
}
