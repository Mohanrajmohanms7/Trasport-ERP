package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.FuelRequestRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelRequestFulfillmentTest {

    @Mock
    private FuelRequestRepository requestRepository;

    @Mock
    private FuelEntryRepository fuelEntryRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private JournalVoucherRepository jvRepository;

    @Mock
    private FinancialYearPeriodValidationService periodValidationService;

    @Mock
    private TenantAccessService tenantAccess;

    @Mock
    private TenantParentAccess parentAccess;

    @Mock
    private ChartOfAccountService coaService;

    @Mock
    private JournalVoucherService jvService;

    @Mock
    private AuditService auditService;

    @Mock
    private AppSettingService settingService;

    @Mock
    private DocumentNumberService documentNumberService;

    @InjectMocks
    private FuelRequestService requestService;

    @InjectMocks
    private FuelEntryService fuelEntryService;

    private Trip mockTrip;
    private Vehicle mockVehicle;
    private Driver mockDriver;
    private FuelRequest mockRequest;
    private FuelEntry mockEntry;

    @BeforeEach
    void setUp() {
        lenient().when(documentNumberService.next(any(), any(), any(), any())).thenReturn("DOC-2627/00001");
        mockTrip = new Trip();
        mockTrip.setId(10L);
        mockTrip.setTripNumber("TRIP-100");
        mockTrip.setCompanyId(3L);
        mockTrip.setBranchId(3L);

        mockVehicle = new Vehicle();
        mockVehicle.setId(5L);
        mockVehicle.setName("TN-01-AB-1234");
        mockVehicle.setCompanyId(3L);
        mockVehicle.setBranchId(3L);

        mockDriver = new Driver();
        mockDriver.setId(8L);
        mockDriver.setName("Kumar Selvam");
        mockDriver.setCompanyId(3L);
        mockDriver.setBranchId(3L);

        mockRequest = new FuelRequest();
        mockRequest.setId(1L);
        mockRequest.setRequestNumber("FREQ-10001");
        mockRequest.setTrip(mockTrip);
        mockRequest.setRequestedQuantity(new BigDecimal("100.00"));
        mockRequest.setRequestedAmount(new BigDecimal("9000.00"));
        mockRequest.setStatus("PENDING");
        mockRequest.setCompanyId(3L);
        mockRequest.setBranchId(3L);

        mockEntry = new FuelEntry();
        mockEntry.setId(50L);
        mockEntry.setFuelEntryNumber("FUEL-50001");
        mockEntry.setVehicle(mockVehicle);
        mockEntry.setDriver(mockDriver);
        mockEntry.setTrip(mockTrip);
        mockEntry.setFuelStation("IOCL Station");
        mockEntry.setFuelQuantity(new BigDecimal("100.00"));
        mockEntry.setRatePerLitre(new BigDecimal("90.00"));
        mockEntry.setTotalAmount(new BigDecimal("9000.00"));
        mockEntry.setStatus("DRAFT");
        mockEntry.setCompanyId(3L);
        mockEntry.setBranchId(3L);
    }

    @Test
    @DisplayName("1. Create Fuel Request - Success")
    void testCreateFuelRequest_Success() {
        FuelRequest req = new FuelRequest();
        req.setTrip(mockTrip);
        req.setRequestedQuantity(new BigDecimal("100.00"));
        req.setRequestedAmount(new BigDecimal("9000.00"));

        when(tripRepository.findById(10L)).thenReturn(Optional.of(mockTrip));
        when(requestRepository.save(any(FuelRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        FuelRequest created = requestService.createRequest(req, "testuser");

        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());
        assertEquals(new BigDecimal("100.00"), created.getRequestedQuantity());
        verify(requestRepository).save(any(FuelRequest.class));
    }

    @Test
    @DisplayName("2. Approve Fuel Request - Success")
    void testApproveFuelRequest_Success() {
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));
        when(requestRepository.save(any(FuelRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        FuelRequest approved = requestService.approveRequest(1L, "testuser");

        assertEquals("APPROVED", approved.getStatus());
        verify(requestRepository).save(any(FuelRequest.class));
    }

    @Test
    @DisplayName("3. Double Approval - Blocked")
    void testApproveFuelRequest_AlreadyApproved() {
        mockRequest.setStatus("APPROVED");
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                requestService.approveRequest(1L, "testuser")
        );
        assertEquals("FUEL_REQUEST_ALREADY_APPROVED", ex.getErrorCode());
    }

    @Test
    @DisplayName("4. Create Fuel Entry linked to Approved Request - Success and Marks Request FULFILLED")
    void testCreateFuelEntry_LinkedToApprovedRequest_Success() {
        mockRequest.setStatus("APPROVED");
        mockEntry.setFuelRequest(mockRequest);

        when(parentAccess.requireVehicle(5L)).thenReturn(mockVehicle);
        when(parentAccess.requireDriver(8L)).thenReturn(mockDriver);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));
        when(fuelEntryRepository.findByFuelRequestIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());
        when(fuelEntryRepository.save(any(FuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(FuelRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        FuelEntry created = fuelEntryService.createFuelEntry(mockEntry, "testuser");

        assertNotNull(created);
        assertEquals("DRAFT", created.getStatus());
        assertEquals("FULFILLED", mockRequest.getStatus());
        assertEquals(new BigDecimal("100.00"), mockRequest.getFulfilledQuantity());
        verify(requestRepository).save(mockRequest);
        verify(fuelEntryRepository).save(any(FuelEntry.class));
    }

    @Test
    @DisplayName("5. Duplicate Fulfillment - Blocked")
    void testCreateFuelEntry_DuplicateFulfillment_Blocked() {
        mockRequest.setStatus("APPROVED");
        mockEntry.setFuelRequest(mockRequest);

        FuelEntry existingEntry = new FuelEntry();
        existingEntry.setId(49L);
        existingEntry.setFuelEntryNumber("FUEL-49000");

        when(parentAccess.requireVehicle(5L)).thenReturn(mockVehicle);
        when(parentAccess.requireDriver(8L)).thenReturn(mockDriver);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));
        when(fuelEntryRepository.findByFuelRequestIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(existingEntry));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                fuelEntryService.createFuelEntry(mockEntry, "testuser")
        );
        assertEquals("FUEL_REQUEST_DUPLICATE_FULFILLMENT", ex.getErrorCode());
    }

    @Test
    @DisplayName("6. Fulfillment Exceeding Approved Quantity - Blocked")
    void testCreateFuelEntry_ExceedsRequestedQuantity_Blocked() {
        mockRequest.setStatus("APPROVED");
        mockRequest.setRequestedQuantity(new BigDecimal("50.00")); // Request is for 50L
        mockEntry.setFuelRequest(mockRequest);
        mockEntry.setFuelQuantity(new BigDecimal("100.00")); // Entry attempts 100L

        when(parentAccess.requireVehicle(5L)).thenReturn(mockVehicle);
        when(parentAccess.requireDriver(8L)).thenReturn(mockDriver);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                fuelEntryService.createFuelEntry(mockEntry, "testuser")
        );
        assertEquals("FUEL_FULFILLMENT_EXCEEDS_REQUEST", ex.getErrorCode());
    }

    @Test
    @DisplayName("7. Cancel Fulfilled Fuel Entry - Reverts Request to APPROVED and Posts Reversal JV")
    void testCancelFuelEntry_RevertsRequestStatus() {
        mockRequest.setStatus("FULFILLED");
        mockRequest.setFulfilledQuantity(new BigDecimal("100.00"));
        mockRequest.setFulfilledAmount(new BigDecimal("9000.00"));
        mockRequest.setFuelEntry(mockEntry);

        mockEntry.setStatus("APPROVED");
        mockEntry.setFuelRequest(mockRequest);

        JournalVoucher origJv = new JournalVoucher();
        origJv.setId(99L);
        origJv.setVoucherNumber("JV-FUEL-50");
        origJv.setReferenceNumber("FUEL-50001");
        origJv.setAmount(new BigDecimal("9000.00"));

        AppUser mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setCompanyId(3L);

        when(fuelEntryRepository.findAndLockById(50L)).thenReturn(Optional.of(mockEntry));
        when(tenantAccess.requireCurrentUser()).thenReturn(mockUser);
        when(tenantAccess.isSuperAdmin(mockUser)).thenReturn(true);
        when(requestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mockRequest));
        when(fuelEntryRepository.save(any(FuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requestRepository.save(any(FuelRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse("FUEL-50001")).thenReturn(Collections.singletonList(origJv));

        FuelEntry cancelled = fuelEntryService.cancelFuelEntry(50L, "testuser");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("APPROVED", mockRequest.getStatus());
        assertEquals(BigDecimal.ZERO, mockRequest.getFulfilledQuantity());
        verify(requestRepository).save(mockRequest);
        verify(jvService).createVoucher(any(JournalVoucher.class), eq("testuser"));
    }
}
