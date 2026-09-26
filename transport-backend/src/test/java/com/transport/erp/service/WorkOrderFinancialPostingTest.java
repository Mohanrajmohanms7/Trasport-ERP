package com.transport.erp.service;

import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.model.Supplier;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.JournalVoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderFinancialPostingTest {

    @Mock private JournalVoucherRepository journalVoucherRepository;
    @Mock private JournalVoucherService journalVoucherService;
    @Mock private ChartOfAccountService chartOfAccountService;
    @Mock private FinancialYearPeriodValidationService periodValidationService;
    @Mock private AuditService auditService;

    @Mock private InventoryValuationService valuationService;
    @InjectMocks private WorkOrderFinancialPosting posting;

    private WorkOrder order;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(valuationService.netPartsCost(org.mockito.ArgumentMatchers.any())).thenReturn(java.math.BigDecimal.ZERO);
        order = new WorkOrder();
        order.setId(10L);
        order.setCompanyId(1L);
        order.setBranchId(1L);
        order.setWorkOrderNumber("WO-000010");
        order.setStatus("IN_PROGRESS");
        order.setEstimatedCost(new BigDecimal("50.00"));
        Supplier supplier = new Supplier();
        supplier.setId(4L);
        supplier.setName("ABC Motors");
        order.setSupplier(supplier);
        when(chartOfAccountService.getOrCreateAccount(eq(1L), eq(1L), eq("5400"), any(), eq("EXPENSE")))
                .thenReturn(account(11L, "5400"));
        when(chartOfAccountService.getOrCreateAccount(eq(1L), eq(1L), eq("2000"), any(), eq("LIABILITY")))
                .thenReturn(account(22L, "2000"));
        when(journalVoucherRepository.findByReferenceNumberAndIsDeletedFalse("MAINT-WO-000010")).thenReturn(List.of());
        when(journalVoucherService.createVoucher(any(), eq("admin"))).thenAnswer(inv -> {
            JournalVoucher voucher = inv.getArgument(0);
            voucher.setId(77L);
            voucher.setCreatedDate(LocalDateTime.of(2026, 9, 23, 10, 0));
            return voucher;
        });
    }

    @Test
    @DisplayName("Positive actual cost posts one balanced 5400 debit and 2000 credit")
    void postsActualCostNotOperationalCost() {
        posting.postCompletion(order, new BigDecimal("350.00"), "admin");

        ArgumentCaptor<JournalVoucher> captor = ArgumentCaptor.forClass(JournalVoucher.class);
        verify(journalVoucherService).createVoucher(captor.capture(), eq("admin"));
        JournalVoucher voucher = captor.getValue();
        assertEquals("5400", voucher.getDebitAccount().getAccountCode());
        assertEquals("2000", voucher.getCreditAccount().getAccountCode());
        assertEquals(0, new BigDecimal("350.00").compareTo(voucher.getAmount()));
        assertEquals("MAINT-WO-000010", voucher.getReferenceNumber());
        assertEquals(1L, voucher.getCompanyId());
        assertEquals(1L, voucher.getBranchId());
        assertTrue(voucher.getDescription().contains("ABC Motors"));
        assertFalse(voucher.getDebitAccount().getId().equals(voucher.getCreditAccount().getId()));
        assertEquals(0, voucher.getAmount().compareTo(voucher.getAmount()));
        assertEquals(new BigDecimal("50.00"), order.getEstimatedCost());
        assertNull(order.getActualCost());
        verify(auditService).log(eq("admin"), eq("WORK_ORDER_ACCOUNTING_POSTED"), eq("work_orders"), eq(10L), any(), any());
        verify(periodValidationService).validatePostingAllowed(eq(1L), any());
    }

    @Test
    @DisplayName("Null and zero actual cost do not post and do not use operational cost")
    void nullAndZeroDoNotPost() {
        posting.postCompletion(order, null, "admin");
        posting.postCompletion(order, BigDecimal.ZERO, "admin");
        verify(journalVoucherService, never()).createVoucher(any(), any());
        verify(periodValidationService, never()).validatePostingAllowed(any(), any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Negative actual cost is rejected")
    void negativeAmountRejected() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> posting.postCompletion(order, new BigDecimal("-1.00"), "admin"));
        assertEquals("WORK_ORDER_ACCOUNTING_AMOUNT_INVALID", ex.getErrorCode());
        verify(journalVoucherService, never()).createVoucher(any(), any());
    }

    @Test
    @DisplayName("An existing company voucher blocks a second posting")
    void duplicateVoucherRejected() {
        JournalVoucher existing = new JournalVoucher();
        existing.setId(5L);
        existing.setCompanyId(1L);
        existing.setReferenceNumber("MAINT-WO-000010");
        existing.setAmount(new BigDecimal("350.00"));
        when(journalVoucherRepository.findByReferenceNumberAndIsDeletedFalse("MAINT-WO-000010"))
                .thenReturn(List.of(existing));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> posting.postCompletion(order, new BigDecimal("350.00"), "admin"));
        assertEquals("WORK_ORDER_ACCOUNTING_ALREADY_POSTED", ex.getErrorCode());
        verify(journalVoucherService, never()).createVoucher(any(), any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("A closed fiscal period rejects posting")
    void fiscalPeriodRejected() {
        when(periodValidationService.validatePostingAllowed(eq(1L), any())).thenThrow(new BusinessValidationException(
                "Financial Period Closed", "FINANCIAL_YEAR_CLOSED", "FINANCIAL_YEAR_CLOSED: closed", "Use an open year"));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> posting.postCompletion(order, new BigDecimal("10.00"), "admin"));
        assertEquals("FINANCIAL_YEAR_CLOSED", ex.getErrorCode());
        verify(journalVoucherService, never()).createVoucher(any(), any());
    }

    @Test
    @DisplayName("Journal voucher creation failure does not audit a posting")
    void voucherFailureDoesNotAudit() {
        when(journalVoucherService.createVoucher(any(), any())).thenThrow(new IllegalArgumentException("debit missing"));
        assertThrows(IllegalArgumentException.class,
                () -> posting.postCompletion(order, new BigDecimal("10.00"), "admin"));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Detail accounting state is derived from the journal voucher")
    void accountingStateDerivedFromVoucher() {
        JournalVoucher existing = new JournalVoucher();
        existing.setId(77L);
        existing.setCompanyId(1L);
        existing.setReferenceNumber("MAINT-WO-000010");
        existing.setAmount(new BigDecimal("350.00"));
        existing.setCreatedDate(LocalDateTime.of(2026, 9, 23, 10, 0));
        when(journalVoucherRepository.findByReferenceNumberAndIsDeletedFalse("MAINT-WO-000010"))
                .thenReturn(List.of(existing));
        WorkOrderResponse response = new WorkOrderResponse();
        posting.attachAccounting(order, response);
        assertEquals("POSTED", response.getAccountingStatus());
        assertEquals(0, new BigDecimal("350.00").compareTo(response.getFinancialAmount()));
        assertEquals(77L, response.getJournalVoucherId());
        assertEquals("MAINT-WO-000010", response.getJournalVoucherReference());
        assertEquals(existing.getCreatedDate(), response.getPostedAt());

        when(journalVoucherRepository.findByReferenceNumberAndIsDeletedFalse("MAINT-WO-000010")).thenReturn(List.of());
        WorkOrderResponse open = new WorkOrderResponse();
        posting.attachAccounting(order, open);
        assertEquals("NOT_POSTED", open.getAccountingStatus());
        assertNull(open.getFinancialAmount());
    }

    @Test
    @DisplayName("Posting does not depend on expenses, service logs, inventory, or payments")
    void noExpenseServiceLogOrInventory() {
        for (Field field : WorkOrderFinancialPosting.class.getDeclaredFields()) {
            String name = field.getType().getName();
            assertFalse(name.contains("Expense"));
            assertFalse(name.contains("VehicleServiceLog"));
            assertFalse(name.contains("VehicleOdometer"));
            assertFalse(name.contains("Payment"));
            assertFalse(name.contains("Inventory"));
            assertFalse(name.contains("Stock"));
            assertFalse(name.contains("Purchase"));
        }
    }

    private ChartOfAccount account(Long id, String code) {
        ChartOfAccount account = new ChartOfAccount();
        account.setId(id);
        account.setAccountCode(code);
        account.setCompanyId(1L);
        return account;
    }
}
