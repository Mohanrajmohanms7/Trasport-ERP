package com.transport.erp.service;

import com.transport.erp.dto.DriverPayrollCreateDTO;
import com.transport.erp.dto.DriverPayrollPaymentDTO;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverPayroll;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.repository.DriverPayrollRepository;
import com.transport.erp.repository.JournalVoucherRepository;
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
class DriverPayrollServiceTest {

    @Mock
    private DriverPayrollRepository payrollRepository;

    @Mock
    private JournalVoucherRepository jvRepository;

    @Mock private FinancialYearPeriodValidationService periodValidationService;

    @Mock
    private ChartOfAccountService coaService;

    @Mock
    private TenantAccessService tenantAccess;

    @Mock
    private TenantParentAccess parentAccess;

    @Mock
    private AuditService auditService;

    @Mock
    private DriverSalaryService driverSalaryService;

    @InjectMocks
    private DriverPayrollService payrollService;

    private Driver mockDriver;
    private DriverPayroll mockPayroll;
    private ChartOfAccount mockExpenseAccount;
    private ChartOfAccount mockPayableAccount;
    private ChartOfAccount mockCashAccount;

    @BeforeEach
    void setUp() {
        mockDriver = new Driver();
        mockDriver.setId(10L);
        mockDriver.setName("John Driver");
        mockDriver.setCompanyId(3L);
        mockDriver.setBranchId(3L);

        mockPayroll = new DriverPayroll();
        mockPayroll.setId(100L);
        mockPayroll.setPayrollNumber("PAY-2026-08-DR10-12345");
        mockPayroll.setDriver(mockDriver);
        mockPayroll.setPayYear(2026);
        mockPayroll.setPayMonth(8);
        mockPayroll.setBasicSalary(new BigDecimal("25000.00"));
        mockPayroll.setNetSalaryPayable(new BigDecimal("25000.00"));
        mockPayroll.setStatus("DRAFT");
        mockPayroll.setCompanyId(3L);
        mockPayroll.setBranchId(3L);

        mockExpenseAccount = new ChartOfAccount();
        mockExpenseAccount.setId(5150L);
        mockExpenseAccount.setAccountCode("5150");

        mockPayableAccount = new ChartOfAccount();
        mockPayableAccount.setId(2050L);
        mockPayableAccount.setAccountCode("2050");

        mockCashAccount = new ChartOfAccount();
        mockCashAccount.setId(1000L);
        mockCashAccount.setAccountCode("1000");
    }

    @Test
    @DisplayName("Create Draft Payroll - Success")
    void testCreatePayroll_Success() {
        DriverPayrollCreateDTO dto = new DriverPayrollCreateDTO();
        dto.setDriverId(10L);
        dto.setPayYear(2026);
        dto.setPayMonth(8);
        dto.setBasicSalary(new BigDecimal("25000.00"));

        when(parentAccess.requireDriver(10L)).thenReturn(mockDriver);
        when(payrollRepository.findByDriverIdAndPayYearAndPayMonthAndIsDeletedFalse(10L, 2026, 8))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(DriverPayroll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverPayroll result = payrollService.createPayroll(dto, "testuser");

        assertNotNull(result);
        assertEquals("DRAFT", result.getStatus());
        assertEquals(new BigDecimal("25000.00"), result.getNetSalaryPayable());
        verify(payrollRepository).save(any(DriverPayroll.class));
    }

    @Test
    @DisplayName("Create Draft Payroll - Duplicate Period Blocked")
    void testCreatePayroll_DuplicatePeriod() {
        DriverPayrollCreateDTO dto = new DriverPayrollCreateDTO();
        dto.setDriverId(10L);
        dto.setPayYear(2026);
        dto.setPayMonth(8);

        when(parentAccess.requireDriver(10L)).thenReturn(mockDriver);
        when(payrollRepository.findByDriverIdAndPayYearAndPayMonthAndIsDeletedFalse(10L, 2026, 8))
                .thenReturn(Optional.of(mockPayroll));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                payrollService.createPayroll(dto, "testuser")
        );
        assertEquals("PAYROLL_DUPLICATE_PAY_PERIOD", ex.getErrorCode());
    }

    @Test
    @DisplayName("Approve Payroll - Success and Posts Accrual JV")
    void testApprovePayroll_Success() {
        when(payrollRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockPayroll));
        when(coaService.getOrCreateAccount(3L, 3L, "5150", "Driver Salary Expense", "EXPENSE")).thenReturn(mockExpenseAccount);
        when(coaService.getOrCreateAccount(3L, 3L, "2050", "Driver Salary Payable", "LIABILITY")).thenReturn(mockPayableAccount);
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse(anyString())).thenReturn(Collections.emptyList());
        when(jvRepository.save(any(JournalVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollRepository.save(any(DriverPayroll.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverPayroll approved = payrollService.approvePayroll(100L, "testuser");

        assertEquals("APPROVED", approved.getStatus());
        assertNotNull(approved.getAccrualJvNumber());
        verify(jvRepository).save(any(JournalVoucher.class));
    }

    @Test
    @DisplayName("Approve Payroll - Re-approval Blocked")
    void testApprovePayroll_AlreadyApproved() {
        mockPayroll.setStatus("APPROVED");
        when(payrollRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockPayroll));

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                payrollService.approvePayroll(100L, "testuser")
        );
        assertEquals("PAYROLL_ALREADY_APPROVED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Pay Payroll - Success and Posts Payment JV")
    void testPayPayroll_Success() {
        mockPayroll.setStatus("APPROVED");
        DriverPayrollPaymentDTO payDto = new DriverPayrollPaymentDTO();
        payDto.setPaymentMethod("CASH");

        when(payrollRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockPayroll));
        when(coaService.getOrCreateAccount(3L, 3L, "2050", "Driver Salary Payable", "LIABILITY")).thenReturn(mockPayableAccount);
        when(coaService.getOrCreateAccount(3L, 3L, "1000", "Cash on Hand", "ASSET")).thenReturn(mockCashAccount);
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse(anyString())).thenReturn(Collections.emptyList());
        when(jvRepository.save(any(JournalVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollRepository.save(any(DriverPayroll.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverPayroll paid = payrollService.payPayroll(100L, payDto, "testuser");

        assertEquals("PAID", paid.getStatus());
        assertEquals("CASH", paid.getPaymentMethod());
        assertNotNull(paid.getPaymentJvNumber());
        verify(jvRepository).save(any(JournalVoucher.class));
    }

    @Test
    @DisplayName("Cancel Paid Payroll - Posts Payment and Accrual Reversals")
    void testCancelPayroll_PaidStatus() {
        mockPayroll.setStatus("PAID");

        JournalVoucher origAccJv = new JournalVoucher();
        origAccJv.setVoucherNumber("JV-SAL-ACC-100");
        origAccJv.setDebitAccount(mockExpenseAccount);
        origAccJv.setCreditAccount(mockPayableAccount);
        origAccJv.setAmount(new BigDecimal("25000.00"));

        JournalVoucher origPayJv = new JournalVoucher();
        origPayJv.setVoucherNumber("JV-SAL-PAY-100");
        origPayJv.setDebitAccount(mockPayableAccount);
        origPayJv.setCreditAccount(mockCashAccount);
        origPayJv.setAmount(new BigDecimal("25000.00"));

        when(payrollRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(mockPayroll));
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse("SAL-PAY-PAY-100")).thenReturn(Collections.singletonList(origPayJv));
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse("SAL-ACC-PAY-100")).thenReturn(Collections.singletonList(origAccJv));
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse("REV-SAL-PAY-PAY-100")).thenReturn(Collections.emptyList());
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse("REV-SAL-ACC-PAY-100")).thenReturn(Collections.emptyList());
        when(jvRepository.save(any(JournalVoucher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payrollRepository.save(any(DriverPayroll.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverPayroll cancelled = payrollService.cancelPayroll(100L, "testuser");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getCancellationJvNumber());
        verify(jvRepository, times(2)).save(any(JournalVoucher.class));
    }
}
