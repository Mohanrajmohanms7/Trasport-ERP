package com.transport.erp.service;

import com.transport.erp.dto.SparePartRequest;
import com.transport.erp.dto.SparePartResponse;
import com.transport.erp.dto.WorkOrderLabourRequest;
import com.transport.erp.dto.WorkOrderPartRequest;
import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppRole;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.SparePart;
import com.transport.erp.model.UomMaster;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.model.WorkOrderLabour;
import com.transport.erp.model.WorkOrderPart;
import com.transport.erp.repository.AppUserRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.UomMasterRepository;
import com.transport.erp.repository.WorkOrderLabourRepository;
import com.transport.erp.repository.WorkOrderPartRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderPartsLabourServiceTest {

    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;

    @Mock private SparePartRepository sparePartRepository;
    @Mock private UomMasterRepository uomMasterRepository;
    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private WorkOrderPartRepository partRepository;
    @Mock private WorkOrderLabourRepository labourRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private WorkOrderService workOrderService;
    @Mock private TenantAccessService tenantAccess;
    @Mock private AuditService auditService;

    @InjectMocks private SparePartService sparePartService;
    @InjectMocks private WorkOrderLineService lineService;

    private AppUser companyAdmin;
    private WorkOrder order;
    private SparePart part;
    private UomMaster uom;

    @BeforeEach
    void setUp() {
        companyAdmin = user("admin", COMPANY, 1L, "COMPANY_ADMIN");
        stubTenant(companyAdmin, false);
        order = openOrder();
        uom = uom(7L, null);
        part = part(5L, COMPANY, "ACTIVE", false, new BigDecimal("10.00"));
        when(workOrderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(sparePartRepository.findById(5L)).thenReturn(Optional.of(part));
        when(sparePartRepository.saveAndFlush(any())).thenAnswer(inv -> {
            SparePart saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(3L);
            }
            return saved;
        });
        when(partRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WorkOrderPart line = inv.getArgument(0);
            if (line.getId() == null) {
                line.setId(100L);
            }
            return line;
        });
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(labourRepository.saveAndFlush(any())).thenAnswer(inv -> {
            WorkOrderLabour line = inv.getArgument(0);
            if (line.getId() == null) {
                line.setId(200L);
            }
            return line;
        });
        when(labourRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workOrderService.get(10L)).thenReturn(new WorkOrderResponse());
        when(uomMasterRepository.findById(7L)).thenReturn(Optional.of(uom));
    }

    @Test
    @DisplayName("1 valid spare part")
    void validSparePart() {
        SparePartRequest request = new SparePartRequest();
        request.setCode(" oil ");
        request.setName("Engine oil");
        request.setDefaultUomId(7L);
        request.setDefaultRate(new BigDecimal("12.50"));
        request.setCompanyId(OTHER_COMPANY);

        SparePartResponse response = sparePartService.create(request, "admin");

        assertEquals("OIL", response.getCode());
        assertEquals(COMPANY, response.getCompanyId());
        assertEquals(new BigDecimal("12.50"), response.getDefaultRate());
        assertEquals(7L, response.getDefaultUomId());
        verify(auditService).log(eq("admin"), eq("SPARE_PART_CREATED"), eq("spare_parts"), eq(3L), nullable(String.class), eq("OIL"));
    }

    @Test
    @DisplayName("2 inactive and deleted parts are rejected")
    void inactiveOrDeletedPartRejected() {
        part.setStatus("INACTIVE");
        BusinessValidationException inactive = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), null), "admin"));
        assertEquals("SPARE_PART_INVALID", inactive.getErrorCode());

        part.setStatus("ACTIVE");
        part.setIsDeleted(true);
        BusinessValidationException deleted = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), null), "admin"));
        assertEquals("SPARE_PART_INVALID", deleted.getErrorCode());
        verify(partRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("3 other-company part is rejected")
    void otherCompanyPartRejected() {
        part.setCompanyId(OTHER_COMPANY);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("10.00")), "admin"));
        assertEquals("SPARE_PART_INVALID", ex.getErrorCode());
        verify(partRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("4 valid part line copies UOM and default rate")
    void validPartLine() {
        WorkOrderPart line = addPart(partRequest(new BigDecimal("2"), null));
        assertEquals(part, line.getSparePart());
        assertEquals(uom, line.getUom());
        assertEquals(new BigDecimal("10.00"), line.getUnitRate());
        assertEquals(new BigDecimal("2.000"), line.getQuantity());
        assertEquals(new BigDecimal("20.00"), line.getLineTotal());
        assertEquals("WOP-000100", line.getCode());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_PART_ADDED"), eq("work_order_parts"), eq(100L), nullable(String.class), any());
    }

    @Test
    @DisplayName("5 invalid quantity")
    void invalidQuantity() {
        for (BigDecimal quantity : new BigDecimal[] { null, BigDecimal.ZERO, new BigDecimal("-1") }) {
            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> lineService.addPart(10L, partRequest(quantity, new BigDecimal("10.00")), "admin"));
            assertEquals("WORK_ORDER_PART_QUANTITY_INVALID", ex.getErrorCode());
        }
        verify(partRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("6 invalid rate")
    void invalidRate() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("-0.01")), "admin"));
        assertEquals("WORK_ORDER_PART_RATE_INVALID", ex.getErrorCode());
        verify(partRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("7 duplicate part lines are allowed")
    void duplicatePartLinesAllowed() {
        lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("10.00")), "admin");
        lineService.addPart(10L, partRequest(new BigDecimal("3"), new BigDecimal("10.00")), "admin");
        verify(partRepository, times(2)).saveAndFlush(any());
    }

    @Test
    @DisplayName("8 server-side part line total ignores any client figure")
    void serverSidePartLineTotal() {
        WorkOrderPart line = addPart(partRequest(new BigDecimal("1.005"), new BigDecimal("1.00")));
        assertEquals(new BigDecimal("1.01"), line.getLineTotal());
        assertEquals(0, WorkOrderLineService.lineTotal(new BigDecimal("1.5"), new BigDecimal("10.33"))
                .compareTo(new BigDecimal("15.50")));
    }

    @Test
    @DisplayName("9 valid labour without a user")
    void validLabour() {
        WorkOrderLabour line = addLabour(labourRequest(null, "Workshop fitting", new BigDecimal("1.50"), new BigDecimal("200.00")));
        assertNull(line.getAppUser());
        assertEquals("Workshop fitting", line.getDescription());
        assertEquals(new BigDecimal("300.00"), line.getLineTotal());
        assertEquals("WOL-000200", line.getCode());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_LABOUR_ADDED"), eq("work_order_labour"), eq(200L), nullable(String.class), any());
    }

    @Test
    @DisplayName("10 blank labour description")
    void blankLabourDescription() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(null, "  ", new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_LABOUR_DESCRIPTION_REQUIRED", ex.getErrorCode());
        verify(labourRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("11 invalid hours")
    void invalidHours() {
        for (BigDecimal hours : new BigDecimal[] { null, BigDecimal.ZERO, new BigDecimal("-2") }) {
            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> lineService.addLabour(10L, labourRequest(null, "Fit", hours, new BigDecimal("1.00")), "admin"));
            assertEquals("WORK_ORDER_LABOUR_HOURS_INVALID", ex.getErrorCode());
        }
    }

    @Test
    @DisplayName("12 invalid labour rate")
    void invalidLabourRate() {
        BusinessValidationException negative = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(null, "Fit", new BigDecimal("1"), new BigDecimal("-1")), "admin"));
        assertEquals("WORK_ORDER_LABOUR_RATE_INVALID", negative.getErrorCode());
        BusinessValidationException missing = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(null, "Fit", new BigDecimal("1"), null), "admin"));
        assertEquals("WORK_ORDER_LABOUR_RATE_INVALID", missing.getErrorCode());
    }

    @Test
    @DisplayName("13 other-company labour user is rejected")
    void otherCompanyLabourUserRejected() {
        AppUser foreign = user("foreign", OTHER_COMPANY, null, "DRIVER");
        foreign.setId(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(foreign));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(9L, "Fit", new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_LABOUR_USER_INVALID", ex.getErrorCode());

        AppUser deleted = user("gone", COMPANY, 1L, "DRIVER");
        deleted.setId(8L);
        deleted.setIsDeleted(true);
        when(userRepository.findById(8L)).thenReturn(Optional.of(deleted));
        BusinessValidationException deletedEx = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(8L, "Fit", new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_LABOUR_USER_INVALID", deletedEx.getErrorCode());
    }

    @Test
    @DisplayName("14 multiple labour lines")
    void multipleLabourLines() {
        lineService.addLabour(10L, labourRequest(null, "One", new BigDecimal("1"), new BigDecimal("10.00")), "admin");
        lineService.addLabour(10L, labourRequest(null, "Two", new BigDecimal("2"), new BigDecimal("10.00")), "admin");
        verify(labourRepository, times(2)).saveAndFlush(any());
    }

    @Test
    @DisplayName("15 update part line keeps the line rate when the master rate changes")
    void updatePartLine() {
        WorkOrderPart existing = storedPart();
        part.setDefaultRate(new BigDecimal("99.00"));
        WorkOrderPartRequest request = partRequest(new BigDecimal("4"), null);
        lineService.updatePart(10L, 100L, request, "admin");
        assertEquals(new BigDecimal("10.00"), existing.getUnitRate());
        assertEquals(new BigDecimal("40.00"), existing.getLineTotal());
        assertEquals(uom, existing.getUom());

        request.setUnitRate(new BigDecimal("12.50"));
        lineService.updatePart(10L, 100L, request, "admin");
        assertEquals(new BigDecimal("12.50"), existing.getUnitRate());
        assertEquals(new BigDecimal("50.00"), existing.getLineTotal());
        verify(auditService, times(2)).log(eq("admin"), eq("WORK_ORDER_PART_UPDATED"), eq("work_order_parts"), eq(100L), nullable(String.class), any());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("16 update labour line")
    void updateLabourLine() {
        WorkOrderLabour existing = storedLabour();
        WorkOrderLabourRequest request = labourRequest(null, "Adjusted", new BigDecimal("2.50"), new BigDecimal("8.00"));
        lineService.updateLabour(10L, 200L, request, "admin");
        assertEquals("Adjusted", existing.getDescription());
        assertEquals(new BigDecimal("20.00"), existing.getLineTotal());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_LABOUR_UPDATED"), eq("work_order_labour"), eq(200L), nullable(String.class), any());
    }

    @Test
    @DisplayName("17 soft-delete part line")
    void softDeletePartLine() {
        WorkOrderPart existing = storedPart();
        lineService.removePart(10L, 100L, "admin");
        assertTrue(existing.getIsDeleted());
        verify(partRepository).save(existing);
        verify(partRepository, never()).delete(any());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_PART_REMOVED"), eq("work_order_parts"), eq(100L), nullable(String.class), any());
    }

    @Test
    @DisplayName("18 soft-delete labour line")
    void softDeleteLabourLine() {
        WorkOrderLabour existing = storedLabour();
        lineService.removeLabour(10L, 200L, "admin");
        assertTrue(existing.getIsDeleted());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_LABOUR_REMOVED"), eq("work_order_labour"), eq(200L), nullable(String.class), any());
    }

    @Test
    @DisplayName("19 completed work order rejects part and labour changes")
    void completedMutationRejected() {
        order.setStatus("COMPLETED");
        order.setActualCost(new BigDecimal("80.00"));
        BusinessValidationException partEx = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        BusinessValidationException labourEx = assertThrows(BusinessValidationException.class,
                () -> lineService.addLabour(10L, labourRequest(null, "Fit", new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", partEx.getErrorCode());
        assertEquals("WORK_ORDER_NOT_EDITABLE", labourEx.getErrorCode());
        assertEquals(new BigDecimal("80.00"), order.getActualCost());
        verify(partRepository, never()).saveAndFlush(any());
        verify(labourRepository, never()).saveAndFlush(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("20 cancelled work order rejects part and labour changes")
    void cancelledMutationRejected() {
        order.setStatus("CANCELLED");
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.updatePart(10L, 100L, partRequest(new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", ex.getErrorCode());
        verify(partRepository, never()).findActiveLine(any(), any());
    }

    @Test
    @DisplayName("issued part cannot be removed or reduced below issued quantity")
    void issuedPartCannotBeRemovedOrReduced() {
        WorkOrderPart existing = storedPart();
        existing.setIssuedQuantity(new BigDecimal("2.000"));
        existing.setReturnedQuantity(BigDecimal.ZERO);
        BusinessValidationException removed = assertThrows(BusinessValidationException.class,
                () -> lineService.removePart(10L, 100L, "admin"));
        assertEquals("INVENTORY_WORK_ORDER_PART_INVALID", removed.getErrorCode());
        assertFalse(Boolean.TRUE.equals(existing.getIsDeleted()));

        BusinessValidationException reduced = assertThrows(BusinessValidationException.class,
                () -> lineService.updatePart(10L, 100L, partRequest(new BigDecimal("1"), new BigDecimal("10.00")), "admin"));
        assertEquals("INVENTORY_QUANTITY_INVALID", reduced.getErrorCode());
        assertEquals(new BigDecimal("1.000"), existing.getQuantity());
    }

    @Test
    @DisplayName("21 and 22 deleted lines stay out of the active line query used for totals")
    void deletedLinesExcludedFromTotalsQuery() throws Exception {
        Query parts = WorkOrderPartRepository.class.getMethod("findActiveByWorkOrderId", Long.class).getAnnotation(Query.class);
        Query labour = WorkOrderLabourRepository.class.getMethod("findActiveByWorkOrderId", Long.class).getAnnotation(Query.class);
        assertTrue(parts.value().contains("isDeleted = false"));
        assertTrue(parts.value().contains("JOIN FETCH p.sparePart"));
        assertTrue(parts.value().contains("JOIN FETCH p.uom"));
        assertTrue(labour.value().contains("isDeleted = false"));
        assertTrue(labour.value().contains("LEFT JOIN FETCH l.appUser"));
        assertFalse(parts.value().toLowerCase().contains("join fetch p.workorder.vehicle"));
    }

    @Test
    @DisplayName("23 actual cost is not rewritten when a line is added")
    void actualCostUnchanged() {
        order.setActualCost(new BigDecimal("80.00"));
        order.setEstimatedCost(new BigDecimal("50.00"));
        addPart(partRequest(new BigDecimal("2"), new BigDecimal("10.00")));
        assertEquals(new BigDecimal("80.00"), order.getActualCost());
        assertEquals(new BigDecimal("50.00"), order.getEstimatedCost());
        verify(workOrderRepository, never()).save(any());
        verify(workOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("24 work-order pessimistic lock is used before the line is saved")
    void pessimisticLockUsed() {
        lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("10.00")), "admin");
        InOrder sequence = inOrder(workOrderRepository, partRepository);
        sequence.verify(workOrderRepository).findByIdForUpdate(10L);
        sequence.verify(partRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("25 stale completed status is rejected after the lock")
    void staleLifecycleRejectedAfterLock() {
        when(workOrderRepository.findByIdForUpdate(10L)).thenAnswer(inv -> {
            order.setStatus("COMPLETED");
            return Optional.of(order);
        });
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertEquals("WORK_ORDER_NOT_EDITABLE", ex.getErrorCode());
        verify(workOrderRepository).findByIdForUpdate(10L);
        verify(partRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("26 company isolation")
    void companyIsolation() {
        order.setCompanyId(OTHER_COMPANY);
        assertThrows(AccessDeniedException.class,
                () -> lineService.addPart(10L, partRequest(new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        verify(partRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("27 branch isolation")
    void branchIsolation() {
        order.setBranchId(2L);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lineService.addLabour(10L, labourRequest(null, "Fit", new BigDecimal("1"), new BigDecimal("1.00")), "admin"));
        assertTrue(ex.getMessage().contains("another branch"));
        verify(labourRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("28 detail parts query is a single fetch")
    void detailPartsQueryIsSingleFetch() throws Exception {
        String jpql = WorkOrderPartRepository.class.getMethod("findActiveByWorkOrderId", Long.class)
                .getAnnotation(Query.class).value();
        assertTrue(jpql.startsWith("SELECT p FROM WorkOrderPart"));
        assertEquals(2, jpql.split("JOIN FETCH").length - 1);
    }

    @Test
    @DisplayName("29 detail labour query is a single fetch")
    void detailLabourQueryIsSingleFetch() throws Exception {
        String jpql = WorkOrderLabourRepository.class.getMethod("findActiveByWorkOrderId", Long.class)
                .getAnnotation(Query.class).value();
        assertTrue(jpql.startsWith("SELECT l FROM WorkOrderLabour"));
        assertEquals(1, jpql.split("JOIN FETCH").length - 1);
    }

    @Test
    @DisplayName("30 completion still does not update baseline, service log, expenses, or journals")
    void completionDoesNotTouchAccountingOrBaseline() {
        for (Class<?> type : List.of(WorkOrderLineService.class, SparePartService.class)) {
            for (Field field : type.getDeclaredFields()) {
                String name = field.getType().getName();
                assertFalse(name.contains("VehicleOdometer"), type.getSimpleName());
                assertFalse(name.contains("VehicleServiceLog"), type.getSimpleName());
                assertFalse(name.contains("Expense"), type.getSimpleName());
                assertFalse(name.contains("Journal"), type.getSimpleName());
                assertFalse(name.contains("VehicleMaintenanceBaseline"), type.getSimpleName());
                assertFalse(name.contains("DriverSalary"), type.getSimpleName());
            }
        }
    }

    @Test
    @DisplayName("Missing line is reported as not found")
    void missingLine() {
        when(partRepository.findActiveLine(404L, 10L)).thenReturn(Optional.empty());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> lineService.removePart(10L, 404L, "admin"));
        assertEquals("WORK_ORDER_LINE_NOT_FOUND", ex.getErrorCode());
    }

    private WorkOrderPart addPart(WorkOrderPartRequest request) {
        lineService.addPart(10L, request, "admin");
        ArgumentCaptor<WorkOrderPart> captor = ArgumentCaptor.forClass(WorkOrderPart.class);
        verify(partRepository, times(1)).save(captor.capture());
        org.mockito.Mockito.clearInvocations(partRepository);
        return captor.getValue();
    }

    private WorkOrderLabour addLabour(WorkOrderLabourRequest request) {
        lineService.addLabour(10L, request, "admin");
        ArgumentCaptor<WorkOrderLabour> captor = ArgumentCaptor.forClass(WorkOrderLabour.class);
        verify(labourRepository, times(1)).save(captor.capture());
        org.mockito.Mockito.clearInvocations(labourRepository);
        return captor.getValue();
    }

    private WorkOrderPart storedPart() {
        WorkOrderPart existing = new WorkOrderPart();
        existing.setId(100L);
        existing.setCode("WOP-000100");
        existing.setUnitRate(new BigDecimal("10.00"));
        existing.setQuantity(new BigDecimal("1.000"));
        existing.setLineTotal(new BigDecimal("10.00"));
        existing.setSparePart(part);
        existing.setUom(uom);
        existing.setIsDeleted(false);
        when(partRepository.findActiveLine(100L, 10L)).thenReturn(Optional.of(existing));
        return existing;
    }

    private WorkOrderLabour storedLabour() {
        WorkOrderLabour existing = new WorkOrderLabour();
        existing.setId(200L);
        existing.setCode("WOL-000200");
        existing.setDescription("Fit");
        existing.setHours(new BigDecimal("1.00"));
        existing.setRate(new BigDecimal("10.00"));
        existing.setLineTotal(new BigDecimal("10.00"));
        existing.setIsDeleted(false);
        when(labourRepository.findActiveLine(200L, 10L)).thenReturn(Optional.of(existing));
        return existing;
    }

    private WorkOrderPartRequest partRequest(BigDecimal quantity, BigDecimal rate) {
        WorkOrderPartRequest request = new WorkOrderPartRequest();
        request.setSparePartId(5L);
        request.setQuantity(quantity);
        request.setUnitRate(rate);
        request.setNotes("Filter");
        return request;
    }

    private WorkOrderLabourRequest labourRequest(Long userId, String description, BigDecimal hours, BigDecimal rate) {
        WorkOrderLabourRequest request = new WorkOrderLabourRequest();
        request.setAppUserId(userId);
        request.setDescription(description);
        request.setHours(hours);
        request.setRate(rate);
        return request;
    }

    private WorkOrder openOrder() {
        WorkOrder row = new WorkOrder();
        row.setId(10L);
        row.setWorkOrderNumber("WO-000010");
        row.setStatus("OPEN");
        row.setCompanyId(COMPANY);
        row.setBranchId(1L);
        row.setIsDeleted(false);
        return row;
    }

    private SparePart part(Long id, Long companyId, String status, boolean deleted, BigDecimal rate) {
        SparePart row = new SparePart();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setCode("OIL");
        row.setName("Engine oil");
        row.setStatus(status);
        row.setIsDeleted(deleted);
        row.setDefaultRate(rate);
        row.setDefaultUom(uom);
        return row;
    }

    private UomMaster uom(Long id, Long companyId) {
        UomMaster row = new UomMaster();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setCode("LITRE");
        row.setName("Litre");
        row.setStatus("ACTIVE");
        row.setIsDeleted(false);
        return row;
    }

    private void stubTenant(AppUser user, boolean superAdmin) {
        when(tenantAccess.requireCurrentUser()).thenReturn(user);
        when(tenantAccess.isSuperAdmin(user)).thenReturn(superAdmin);
        when(tenantAccess.resolveCompanyId(any())).thenReturn(user.getCompanyId());
        org.mockito.Mockito.doAnswer(inv -> {
            Long companyId = inv.getArgument(0);
            if (companyId == null || !companyId.equals(user.getCompanyId())) {
                throw new AccessDeniedException("Access denied to another company's data");
            }
            return null;
        }).when(tenantAccess).assertCompanyAccess(nullable(Long.class));
    }

    private AppUser user(String username, Long companyId, Long branchId, String roleCode) {
        AppUser row = new AppUser();
        row.setId(1L);
        row.setUsername(username);
        row.setName(username);
        row.setCompanyId(companyId);
        row.setBranchId(branchId);
        row.setIsDeleted(false);
        AppRole role = new AppRole();
        role.setCode(roleCode);
        row.setRoles(new HashSet<>(Set.of(role)));
        return row;
    }
}
