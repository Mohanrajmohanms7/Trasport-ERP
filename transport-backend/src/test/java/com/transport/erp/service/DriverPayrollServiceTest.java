package com.transport.erp.service;

import com.transport.erp.dto.DriverPayrollCreateDTO;
import com.transport.erp.dto.DriverPayrollPaymentDTO;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DriverPayrollServiceTest {

    @Mock private DriverPayrollRepository payrollRepository;
    @Mock private JournalVoucherRepository jvRepository;
    @Mock private FinancialYearPeriodValidationService periodValidationService;
    @Mock private ChartOfAccountService coaService;
    @Mock private TenantAccessService tenantAccess;
    @Mock private TenantParentAccess parentAccess;
    @Mock private AuditService auditService;
    @Mock private DriverSalaryService driverSalaryService;
    @Mock private CompanyRepository companyRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private TripRepository tripRepository;
    @Mock private DriverPaySlabRepository slabRepository;
    @Mock private DriverAdvanceRepository advanceRepository;
    @Mock private DriverAdvanceRecoveryRepository recoveryRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private DocumentNumberService documentNumberService;
    @Mock private ExpenseRepository expenseRepository;

    @Mock
    private ApprovalPolicyService approvalPolicy;

    @InjectMocks
    private DriverPayrollService payrollService;

    private Driver driver;
    private AppUser accountant;
    private final YearMonth period = YearMonth.now().minusMonths(1);
    private final Map<String, JournalVoucher> jvByRef = new HashMap<>();
    private final List<JournalVoucher> savedJvs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        driver = new Driver();
        driver.setId(10L);
        driver.setName("Kumar");
        driver.setCode("D001");
        driver.setCompanyId(3L);
        driver.setBranchId(3L);

        accountant = new AppUser();
        accountant.setId(50L);
        accountant.setCompanyId(3L);
        accountant.setBranchId(3L);
        AppRole role = new AppRole();
        role.setCode("ACCOUNTANT");
        accountant.setRoles(new HashSet<>(Set.of(role)));

        when(tenantAccess.requireCurrentUser()).thenReturn(accountant);
        when(tenantAccess.resolveCompanyId(any())).thenReturn(3L);
        when(parentAccess.requireDriver(10L)).thenReturn(driver);
        when(documentNumberService.next(any(), any(), any(), any())).thenReturn("PAY-2627/00001");
        when(slabRepository.findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(3L)).thenReturn(slabs());
        when(driverSalaryService.getSalaryByDriver(10L)).thenReturn(Optional.empty());
        when(payrollRepository.findActiveForPeriod(any(), any(), any())).thenReturn(Optional.empty());
        when(payrollRepository.saveAndFlush(any())).thenAnswer(i -> withId(i.getArgument(0)));
        when(payrollRepository.save(any())).thenAnswer(i -> withId(i.getArgument(0)));
        when(advanceRepository.sumOutstanding(10L)).thenReturn(BigDecimal.ZERO);
        when(coaService.getOrCreateAccount(any(), any(), anyString(), anyString(), anyString())).thenAnswer(i -> account(i.getArgument(2)));
        when(jvRepository.findByReferenceNumberAndIsDeletedFalse(anyString())).thenAnswer(i -> {
            JournalVoucher jv = jvByRef.get((String) i.getArgument(0));
            return jv == null ? List.of() : List.of(jv);
        });
        when(jvRepository.save(any())).thenAnswer(i -> {
            JournalVoucher jv = i.getArgument(0);
            jvByRef.put(jv.getReferenceNumber(), jv);
            savedJvs.add(jv);
            return jv;
        });
    }

    private static ChartOfAccount account(String code) {
        ChartOfAccount a = new ChartOfAccount();
        a.setAccountCode(code);
        a.setId(Long.valueOf(code));
        return a;
    }

    private static DriverPayroll withId(DriverPayroll p) {
        if (p.getId() == null) p.setId(100L);
        return p;
    }

    /** 1 trip = 500, 2+ trips = 1000. */
    private static List<DriverPaySlab> slabs() {
        DriverPaySlab one = new DriverPaySlab();
        one.setTripsFrom(1);
        one.setTripsTo(1);
        one.setDailyAmount(new BigDecimal("500"));
        DriverPaySlab more = new DriverPaySlab();
        more.setTripsFrom(2);
        more.setTripsTo(null);
        more.setDailyAmount(new BigDecimal("1000"));
        return new ArrayList<>(List.of(one, more));
    }

    private void tripsPerDay(long... counts) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) rows.add(new Object[]{period.atDay(i + 1), counts[i]});
        }
        when(tripRepository.countTripsByDate(eq(10L), eq(3L), anyCollection(), any(), any())).thenReturn(rows);
    }

    private DriverPayrollCreateDTO request() {
        DriverPayrollCreateDTO dto = new DriverPayrollCreateDTO();
        dto.setDriverId(10L);
        dto.setPayYear(period.getYear());
        dto.setPayMonth(period.getMonthValue());
        return dto;
    }

    private BigDecimal jvTotal(String debitCode, String creditCode) {
        return savedJvs.stream()
                .filter(j -> debitCode.equals(j.getDebitAccount().getAccountCode()) && creditCode.equals(j.getCreditAccount().getAccountCode()))
                .map(JournalVoucher::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long jvCount(String debitCode, String creditCode) {
        return savedJvs.stream()
                .filter(j -> debitCode.equals(j.getDebitAccount().getAccountCode()) && creditCode.equals(j.getCreditAccount().getAccountCode()))
                .count();
    }

    @Test
    @DisplayName("Slab: 0 -> 0, 1 -> 500, 2/3/4 -> 1000")
    void dailySlabAmounts() {
        List<DriverPaySlab> s = DriverPayrollCalculator.validateSlabs(slabs());
        assertNull(DriverPayrollCalculator.slabFor(0, s));
        assertEquals(0, new BigDecimal("500.00").compareTo(DriverPayrollCalculator.slabFor(1, s).getDailyAmount()));
        for (int trips : new int[]{2, 3, 4}) {
            assertEquals(0, new BigDecimal("1000.00").compareTo(DriverPayrollCalculator.slabFor(trips, s).getDailyAmount()));
        }
    }

    @Test
    @DisplayName("Overlapping slabs are rejected")
    void invalidSlabsRejected() {
        List<DriverPaySlab> bad = slabs();
        bad.get(1).setTripsFrom(1);
        assertThrows(BusinessValidationException.class, () -> DriverPayrollCalculator.validateSlabs(bad));
    }

    @Test
    @DisplayName("Spec scenario: 1, 2, 4, 0 trips -> 500 + 1000 + 1000 + 0 = 2500")
    void specScenarioGross() {
        tripsPerDay(1, 2, 4, 0);
        DriverPayroll p = payrollService.createPayroll(request(), "acc");

        assertEquals("DRAFT", p.getStatus());
        assertEquals(3, p.getDays().size());
        assertEquals(7, p.getTotalTrips());
        assertEquals(3, p.getTripDays());
        assertEquals(0, new BigDecimal("2500").compareTo(p.getTripEarnings()));
        assertEquals(0, new BigDecimal("2500").compareTo(p.getGrossAmount()));
        assertEquals(0, new BigDecimal("1000").compareTo(p.getDays().get(2).getDailyAmount())); // 4 trips -> 1000, not 2000
        assertEquals(0, new BigDecimal("2500").compareTo(p.getNetSalaryPayable()));
    }

    @Test
    @DisplayName("Three trips on one day make ONE daily earning of 1000, not 1500")
    void sameDayTripsOneEarning() {
        tripsPerDay(3);
        DriverPayroll p = payrollService.createPayroll(request(), "acc");
        assertEquals(1, p.getDays().size());
        assertEquals(0, new BigDecimal("1000").compareTo(p.getTripEarnings()));
    }

    @Test
    @DisplayName("Each date calculated independently; month total is the sum of days")
    void multipleDates() {
        tripsPerDay(1, 1, 3, 0, 2, 1);
        DriverPayroll p = payrollService.createPayroll(request(), "acc");
        assertEquals(0, new BigDecimal("3500").compareTo(p.getTripEarnings()));
        assertEquals(5, p.getTripDays());
        assertEquals(8, p.getTotalTrips());
    }

    @Test
    @DisplayName("Gross - deductions - advance = net")
    void deductionsAndAdvance() {
        tripsPerDay(1, 2, 4);
        when(advanceRepository.sumOutstanding(10L)).thenReturn(new BigDecimal("800"));
        DriverPayrollCreateDTO dto = request();
        dto.setAdvanceAdjustment(new BigDecimal("500"));
        DriverPayrollCreateDTO.Deduction fine = new DriverPayrollCreateDTO.Deduction();
        fine.setDeductionType("FINE");
        fine.setAmount(new BigDecimal("200"));
        dto.setDeductions(List.of(fine));

        DriverPayroll p = payrollService.createPayroll(dto, "acc");
        assertEquals(0, new BigDecimal("2500").compareTo(p.getGrossAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(p.getDeductionAmount()));
        assertEquals(0, new BigDecimal("1800").compareTo(p.getNetSalaryPayable()));
    }

    @Test
    @DisplayName("Advance recovery above the outstanding balance is rejected")
    void advanceAboveOutstandingRejected() {
        tripsPerDay(2);
        when(advanceRepository.sumOutstanding(10L)).thenReturn(new BigDecimal("100"));
        DriverPayrollCreateDTO dto = request();
        dto.setAdvanceAdjustment(new BigDecimal("500"));
        assertThrows(BusinessValidationException.class, () -> payrollService.createPayroll(dto, "acc"));
    }

    @Test
    @DisplayName("Client-sent salary figures are ignored")
    void clientFiguresIgnored() {
        tripsPerDay(1);
        DriverPayrollCreateDTO dto = request();
        dto.setBasicSalary(new BigDecimal("99999"));
        DriverPayroll p = payrollService.createPayroll(dto, "acc");
        assertEquals(0, new BigDecimal("500").compareTo(p.getGrossAmount()));
    }

    @Test
    @DisplayName("Duplicate driver + month is rejected")
    void duplicatePeriodRejected() {
        tripsPerDay(1);
        when(payrollRepository.findActiveForPeriod(10L, period.getYear(), period.getMonthValue()))
                .thenReturn(Optional.of(new DriverPayroll()));
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> payrollService.createPayroll(request(), "acc"));
        assertEquals("PAYROLL_DUPLICATE_PAY_PERIOD", ex.getErrorCode());
        verify(payrollRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Concurrent generate: unique index violation becomes a clean duplicate error")
    void concurrentGenerateRejected() {
        tripsPerDay(1);
        doThrow(new DataIntegrityViolationException("uq_driver_payroll_active_period")).when(payrollRepository).saveAndFlush(any());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> payrollService.createPayroll(request(), "acc"));
        assertEquals("PAYROLL_DUPLICATE_PAY_PERIOD", ex.getErrorCode());
    }

    @Test
    @DisplayName("Future months cannot be generated")
    void futureMonthRejected() {
        DriverPayrollCreateDTO dto = request();
        YearMonth next = YearMonth.now().plusMonths(1);
        dto.setPayYear(next.getYear());
        dto.setPayMonth(next.getMonthValue());
        assertThrows(BusinessValidationException.class, () -> payrollService.createPayroll(dto, "acc"));
    }

    private DriverPayroll stored(String status, String gross, String advance, String deductions) {
        DriverPayroll p = new DriverPayroll();
        p.setId(100L);
        p.setPayrollNumber("PAY-2627/00001");
        p.setDriver(driver);
        p.setPayYear(period.getYear());
        p.setPayMonth(period.getMonthValue());
        p.setCompanyId(3L);
        p.setBranchId(3L);
        p.setStatus(status);
        p.setGrossAmount(new BigDecimal(gross));
        p.setAdvanceAdjustment(new BigDecimal(advance));
        p.setDeductionAmount(new BigDecimal(deductions));
        p.setNetSalaryPayable(new BigDecimal(gross).subtract(new BigDecimal(advance)).subtract(new BigDecimal(deductions)));
        when(payrollRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(p));
        when(payrollRepository.findById(100L)).thenReturn(Optional.of(p));
        return p;
    }

    private DriverAdvance advance(long id, String amount, String recovered, int daysAgo) {
        DriverAdvance a = new DriverAdvance();
        a.setId(id);
        a.setAdvanceNumber("ADV-" + id);
        a.setDriver(driver);
        a.setAmount(new BigDecimal(amount));
        a.setRecoveredAmount(new BigDecimal(recovered));
        a.setAdvanceDate(LocalDate.now().minusDays(daysAgo));
        a.setStatus("ISSUED");
        return a;
    }

    @Test
    @DisplayName("Post: one expense JV (gross) + one advance recovery JV; advance recovered once, FIFO")
    void postCreatesExpectedAccounting() {
        stored("APPROVED", "2500", "500", "0");
        DriverAdvance older = advance(1, "300", "0", 20);
        DriverAdvance newer = advance(2, "400", "0", 5);
        when(advanceRepository.lockOutstandingForDriver(10L)).thenReturn(List.of(older, newer));

        DriverPayroll posted = payrollService.postPayroll(100L, "acc");

        assertEquals("POSTED", posted.getStatus());
        assertEquals(1, jvCount("5150", "2050"));
        assertEquals(0, new BigDecimal("2500").compareTo(jvTotal("5150", "2050")));
        assertEquals(1, jvCount("2050", "1150"));
        assertEquals(0, new BigDecimal("500").compareTo(jvTotal("2050", "1150")));
        assertEquals(0, jvCount("2050", "4900"));
        assertEquals(0, new BigDecimal("300").compareTo(older.getRecoveredAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(newer.getRecoveredAmount()));
        verify(recoveryRepository, times(2)).save(any());
        assertEquals(period.atEndOfMonth(), posted.getPostingDate());
    }

    @Test
    @DisplayName("Deductions post Dr Payable / Cr Driver Recoveries")
    void deductionPosting() {
        stored("APPROVED", "3000", "0", "250");
        payrollService.postPayroll(100L, "acc");
        assertEquals(0, new BigDecimal("250").compareTo(jvTotal("2050", "4900")));
    }

    @Test
    @DisplayName("Double post: second call rejected, no extra JV")
    void doublePostRejected() {
        DriverPayroll p = stored("APPROVED", "2500", "0", "0");
        payrollService.postPayroll(100L, "acc");
        int before = savedJvs.size();
        assertEquals("POSTED", p.getStatus());
        assertThrows(BusinessValidationException.class, () -> payrollService.postPayroll(100L, "acc"));
        assertEquals(before, savedJvs.size());
    }

    @Test
    @DisplayName("Retry reaching the posting code reuses the JV with the same reference")
    void jvIdempotentByReference() {
        DriverPayroll p = stored("APPROVED", "1000", "0", "0");
        payrollService.postPayroll(100L, "acc");
        p.setStatus("APPROVED");
        payrollService.postPayroll(100L, "acc");
        assertEquals(1, jvCount("5150", "2050"));
    }

    @Test
    @DisplayName("Pay: one Dr Payable / Cr Cash JV for net; no expense again; double pay rejected")
    void payCreatesSettlementOnly() {
        stored("POSTED", "2500", "500", "0");
        DriverPayrollPaymentDTO pay = new DriverPayrollPaymentDTO();
        pay.setPaymentMethod("CASH");
        DriverPayroll paid = payrollService.payPayroll(100L, pay, "acc");

        assertEquals("PAID", paid.getStatus());
        assertEquals(1, jvCount("2050", "1000"));
        assertEquals(0, new BigDecimal("2000").compareTo(jvTotal("2050", "1000")));
        assertEquals(0, jvCount("5150", "2050"));
        assertThrows(BusinessValidationException.class, () -> payrollService.payPayroll(100L, pay, "acc"));
        assertEquals(1, jvCount("2050", "1000"));
    }

    @Test
    @DisplayName("Cancel posted payroll: JVs reversed and advance recovery returned")
    void cancelPostedReverses() {
        stored("POSTED", "2500", "500", "0");
        DriverAdvance adv = advance(1, "500", "500", 10);
        JournalVoucher acc = new JournalVoucher();
        acc.setVoucherNumber("JV-SAL-ACC-100");
        acc.setReferenceNumber("SAL-ACC-PAY-100");
        acc.setDebitAccount(account("5150"));
        acc.setCreditAccount(account("2050"));
        acc.setAmount(new BigDecimal("2500"));
        acc.setName("Salary Accrual JV");
        jvByRef.put(acc.getReferenceNumber(), acc);
        DriverAdvanceRecovery r = new DriverAdvanceRecovery();
        r.setAdvance(adv);
        r.setAmount(new BigDecimal("500"));
        r.setStatus("ACTIVE");
        when(recoveryRepository.findByPayrollIdAndStatusAndIsDeletedFalse(100L, "ACTIVE")).thenReturn(List.of(r));
        when(advanceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(adv));

        DriverPayroll cancelled = payrollService.cancelPayroll(100L, "acc");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(0, new BigDecimal("2500").compareTo(jvTotal("2050", "5150")));
        assertEquals(0, adv.getRecoveredAmount().signum());
        assertEquals("REVERSED", r.getStatus());
    }

    @Test
    @DisplayName("Tenant isolation: another company's payroll is refused")
    void tenantIsolation() {
        stored("DRAFT", "100", "0", "0").setCompanyId(99L);
        doThrow(new AccessDeniedException("other company")).when(tenantAccess).assertOwned(99L);
        assertThrows(AccessDeniedException.class, () -> payrollService.getPayrollById(100L));
        assertThrows(AccessDeniedException.class, () -> payrollService.approvePayroll(100L, "acc"));
    }

    @Test
    @DisplayName("Branch isolation: branch-bound user cannot open another branch's payroll")
    void branchIsolation() {
        stored("DRAFT", "100", "0", "0").setBranchId(7L);
        assertThrows(AccessDeniedException.class, () -> payrollService.getPayrollById(100L));
    }

    @Test
    @DisplayName("Driver sees only own POSTED/PAID slips")
    void driverOwnSlipsOnly() {
        Driver me = new Driver();
        me.setId(10L);
        when(driverRepository.findByAppUserIdAndIsDeletedFalse(50L)).thenReturn(Optional.of(me));
        DriverPayroll p = stored("POSTED", "100", "0", "0");
        assertEquals(100L, payrollService.getMySalarySlip(100L).getPayrollId());

        p.setStatus("DRAFT");
        assertThrows(AccessDeniedException.class, () -> payrollService.getMySalarySlip(100L));

        p.setStatus("PAID");
        Driver other = new Driver();
        other.setId(11L);
        p.setDriver(other);
        assertThrows(AccessDeniedException.class, () -> payrollService.getMySalarySlip(100L));
    }

    @Test
    @DisplayName("Draft cannot be posted or paid directly")
    void lifecycleEnforced() {
        stored("DRAFT", "100", "0", "0");
        assertThrows(BusinessValidationException.class, () -> payrollService.postPayroll(100L, "acc"));
        assertThrows(BusinessValidationException.class, () -> payrollService.payPayroll(100L, null, "acc"));
        assertTrue(savedJvs.isEmpty());
    }

    @Test
    @DisplayName("Net cannot go negative")
    void netNegativeRejected() {
        assertThrows(BusinessValidationException.class,
                () -> DriverPayrollCalculator.net(new BigDecimal("500"), new BigDecimal("300"), new BigDecimal("300")));
    }

    @Test
    @DisplayName("Trips but no slabs configured -> clear error")
    void slabsMissing() {
        when(slabRepository.findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(3L)).thenReturn(new ArrayList<>());
        tripsPerDay(2);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                () -> payrollService.createPayroll(request(), "acc"));
        assertEquals("PAY_SLABS_MISSING", ex.getErrorCode());
    }

    @Test
    @DisplayName("Only COMPLETED trips are eligible")
    @SuppressWarnings("unchecked")
    void eligibleStatuses() {
        assertEquals(List.of("COMPLETED"), DriverPayrollService.ELIGIBLE_TRIP_STATUSES);
        tripsPerDay(1);
        payrollService.createPayroll(request(), "acc");
        ArgumentCaptor<Collection<String>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(tripRepository).countTripsByDate(eq(10L), eq(3L), statuses.capture(), eq(period.atDay(1)), eq(period.atEndOfMonth()));
        assertFalse(statuses.getValue().contains("CANCELLED"));
    }
}
