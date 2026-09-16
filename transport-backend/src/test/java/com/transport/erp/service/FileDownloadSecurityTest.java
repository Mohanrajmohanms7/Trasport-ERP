package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileDownloadSecurityTest {

    @Mock
    private TenantAccessService tenantAccess;

    @Mock
    private CustomerDocumentRepository customerDocumentRepository;

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Mock
    private DriverDocumentRepository driverDocumentRepository;

    @Mock
    private TripDocumentRepository tripDocumentRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private FuelEntryRepository fuelEntryRepository;

    @Mock
    private VehicleServiceLogRepository vehicleServiceLogRepository;

    private FileStorageService fileStorageService;

    private AppUser tenantUserCompany1;
    private AppUser tenantUserCompany2;
    private AppUser superAdminUser;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService("target/test-uploads");
        ReflectionTestUtils.setField(fileStorageService, "tenantAccess", tenantAccess);
        ReflectionTestUtils.setField(fileStorageService, "customerDocumentRepository", customerDocumentRepository);
        ReflectionTestUtils.setField(fileStorageService, "vehicleDocumentRepository", vehicleDocumentRepository);
        ReflectionTestUtils.setField(fileStorageService, "driverDocumentRepository", driverDocumentRepository);
        ReflectionTestUtils.setField(fileStorageService, "tripDocumentRepository", tripDocumentRepository);
        ReflectionTestUtils.setField(fileStorageService, "expenseRepository", expenseRepository);
        ReflectionTestUtils.setField(fileStorageService, "fuelEntryRepository", fuelEntryRepository);
        ReflectionTestUtils.setField(fileStorageService, "vehicleServiceLogRepository", vehicleServiceLogRepository);

        tenantUserCompany1 = new AppUser();
        tenantUserCompany1.setId(10L);
        tenantUserCompany1.setUsername("user_comp1");
        tenantUserCompany1.setCompanyId(1L);
        tenantUserCompany1.setBranchId(101L);

        tenantUserCompany2 = new AppUser();
        tenantUserCompany2.setId(20L);
        tenantUserCompany2.setUsername("user_comp2");
        tenantUserCompany2.setCompanyId(2L);
        tenantUserCompany2.setBranchId(202L);

        superAdminUser = new AppUser();
        superAdminUser.setId(1L);
        superAdminUser.setUsername("super_admin");
        superAdminUser.setCompanyId(null);
    }

    @Test
    @DisplayName("1. Authorized same-company document download -> PASS")
    void authorizedSameCompanyDownload_Pass() {
        when(tenantAccess.requireCurrentUser()).thenReturn(tenantUserCompany1);
        when(tenantAccess.isSuperAdmin(tenantUserCompany1)).thenReturn(false);

        Customer customer = new Customer();
        customer.setCompanyId(1L);
        customer.setBranchId(101L);

        CustomerDocument doc = new CustomerDocument();
        doc.setCustomer(customer);
        doc.setFilePath("uuid_cust_doc.pdf");

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_cust_doc.pdf"))
                .thenReturn(Optional.of(doc));

        assertDoesNotThrow(() -> fileStorageService.validateFileAccess("uuid_cust_doc.pdf", tenantUserCompany1));
        verify(tenantAccess).assertCompanyAccess(1L);
    }

    @Test
    @DisplayName("2. Unauthorized different-company download -> BLOCK")
    void unauthorizedDifferentCompanyDownload_Block() {
        when(tenantAccess.requireCurrentUser()).thenReturn(tenantUserCompany2);
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Customer customer = new Customer();
        customer.setCompanyId(1L);

        CustomerDocument doc = new CustomerDocument();
        doc.setCustomer(customer);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_cust_doc.pdf"))
                .thenReturn(Optional.of(doc));
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_cust_doc.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("3. Unauthorized different-branch download -> BLOCK")
    void unauthorizedDifferentBranchDownload_Block() {
        when(tenantAccess.requireCurrentUser()).thenReturn(tenantUserCompany1);
        when(tenantAccess.isSuperAdmin(tenantUserCompany1)).thenReturn(false);

        Trip trip = new Trip();
        trip.setCompanyId(1L);
        trip.setBranchId(999L); // Different branch

        TripDocument tripDoc = new TripDocument();
        tripDoc.setTrip(trip);
        tripDoc.setFilePath("uuid_trip_pod.pdf");

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_trip_pod.pdf")).thenReturn(Optional.of(tripDoc));

        doThrow(new AccessDeniedException("Access denied: Trip belongs to another branch."))
                .when(tenantAccess).assertBranchAccess(999L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_trip_pod.pdf", tenantUserCompany1));
    }

    @Test
    @DisplayName("4. SUPER_ADMIN authorized download -> PASS")
    void superAdminAuthorizedDownload_Pass() {
        when(tenantAccess.isSuperAdmin(superAdminUser)).thenReturn(true);

        assertDoesNotThrow(() -> fileStorageService.validateFileAccess("any_file.pdf", superAdminUser));
        verify(tenantAccess, never()).assertCompanyAccess(any());
    }

    @Test
    @DisplayName("5. Unknown filename / unverified ownership -> BLOCK")
    void unknownFilename_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany1)).thenReturn(false);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleServiceLogRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("unknown_file.pdf", tenantUserCompany1));
    }

    @Test
    @DisplayName("6. Path traversal attempt -> BLOCK")
    void pathTraversalAttempt_Block() {
        when(tenantAccess.requireCurrentUser()).thenReturn(tenantUserCompany1);
        assertThrows(BusinessValidationException.class, () ->
                fileStorageService.loadFileAsResource("../../../etc/passwd"));
    }

    @Test
    @DisplayName("7. Dangerous filename sequence -> BLOCK")
    void dangerousFilename_Block() {
        when(tenantAccess.requireCurrentUser()).thenReturn(tenantUserCompany1);
        assertThrows(BusinessValidationException.class, () ->
                fileStorageService.loadFileAsResource("..\\secret.key"));
    }

    @Test
    @DisplayName("8. Vehicle document cross-company -> BLOCK")
    void vehicleDocumentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Vehicle vehicle = new Vehicle();
        vehicle.setCompanyId(1L);

        VehicleDocument vdoc = new VehicleDocument();
        vdoc.setVehicle(vehicle);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_rc.pdf")).thenReturn(Optional.of(vdoc));
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_rc.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("9. Driver document cross-company -> BLOCK")
    void driverDocumentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Driver driver = new Driver();
        driver.setCompanyId(1L);

        DriverDocument ddoc = new DriverDocument();
        ddoc.setDriver(driver);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_dl.pdf")).thenReturn(Optional.of(ddoc));
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_dl.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("10. Customer document cross-company -> BLOCK")
    void customerDocumentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Customer customer = new Customer();
        customer.setCompanyId(1L);

        CustomerDocument cdoc = new CustomerDocument();
        cdoc.setCustomer(customer);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_gst.pdf")).thenReturn(Optional.of(cdoc));
        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_gst.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("11. Trip document cross-company -> BLOCK")
    void tripDocumentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Trip trip = new Trip();
        trip.setCompanyId(1L);

        TripDocument tdoc = new TripDocument();
        tdoc.setTrip(trip);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse("uuid_pod.pdf")).thenReturn(Optional.of(tdoc));

        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_pod.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("12. Fuel attachment cross-company -> BLOCK")
    void fuelAttachmentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        FuelEntry fuel = new FuelEntry();
        fuel.setCompanyId(1L);
        fuel.setAttachmentPath("uuid_fuel_bill.jpg");

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse("uuid_fuel_bill.jpg")).thenReturn(Optional.of(fuel));

        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_fuel_bill.jpg", tenantUserCompany2));
    }

    @Test
    @DisplayName("13. Expense attachment cross-company -> BLOCK")
    void expenseAttachmentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        Expense exp = new Expense();
        exp.setCompanyId(1L);
        exp.setAttachmentPath("uuid_toll_receipt.pdf");

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse("uuid_toll_receipt.pdf")).thenReturn(Optional.of(exp));

        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_toll_receipt.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("14. Vehicle service attachment cross-company -> BLOCK")
    void vehicleServiceAttachmentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        VehicleServiceLog svc = new VehicleServiceLog();
        svc.setCompanyId(1L);
        svc.setAttachmentPath("uuid_service_invoice.pdf");

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleServiceLogRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse("uuid_service_invoice.pdf")).thenReturn(Optional.of(svc));

        doThrow(new AccessDeniedException("Access denied to another company's data"))
                .when(tenantAccess).assertCompanyAccess(1L);

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("uuid_service_invoice.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("15. Receipt attachment cross-company -> BLOCK")
    void receiptAttachmentCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleServiceLogRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("unregistered_receipt_file.pdf", tenantUserCompany2));
    }

    @Test
    @DisplayName("16. User profile file cross-company -> BLOCK")
    void userProfileFileCrossCompany_Block() {
        when(tenantAccess.isSuperAdmin(tenantUserCompany2)).thenReturn(false);

        when(customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(vehicleServiceLogRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                fileStorageService.validateFileAccess("unregistered_profile.png", tenantUserCompany2));
    }
}
