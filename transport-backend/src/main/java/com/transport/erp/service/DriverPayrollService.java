package com.transport.erp.service;

import com.transport.erp.dto.DriverPayrollCreateDTO;
import com.transport.erp.dto.DriverPayrollPaymentDTO;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Driver Daily Slab Payroll.
 *
 * DRAFT -> APPROVED -> POSTED -> PAID  (CANCELLED from APPROVED / POSTED / PAID; DRAFT is deleted instead)
 *
 * - Daily earnings: completed trips per driver per trip_date, one slab amount per day (never per trip).
 * - Gross = trip earnings + basic salary (driver salary config, may be 0) + allowance.
 * - Net   = gross - deductions (fine/damage/other) - advance recovery.
 * - POST  : Dr Driver Salary Expense / Cr Driver Salary Payable (gross)
 *           Dr Driver Salary Payable / Cr Driver Advances (advance recovery, FIFO over open advances)
 *           Dr Driver Salary Payable / Cr Driver Recoveries (deductions)
 * - PAY   : Dr Driver Salary Payable / Cr Cash or Bank (net). No expense is posted again.
 * Every JV has a fixed reference per payroll, so retries and double clicks never duplicate it.
 */
@Service
public class DriverPayrollService {

    @org.springframework.beans.factory.annotation.Autowired
    private ApprovalPolicyService approvalPolicy;

    /** Trip statuses that earn pay. Only fully delivered trips; PLANNED/DISPATCHED/CANCELLED do not. */
    public static final List<String> ELIGIBLE_TRIP_STATUSES = List.of("COMPLETED");
    private static final Set<String> DEDUCTION_TYPES = Set.of("FINE", "DAMAGE", "OTHER");
    private static final List<String> PAYMENT_METHODS = List.of("CASH", "BANK_TRANSFER", "CHEQUE", "UPI");

    @Autowired private DriverPayrollRepository payrollRepository;
    @Autowired private JournalVoucherRepository jvRepository;
    @Autowired private ChartOfAccountService coaService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private TenantParentAccess parentAccess;
    @Autowired private AuditService auditService;
    @Autowired private DriverSalaryService driverSalaryService;
    @Autowired private FinancialYearPeriodValidationService periodValidationService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private DriverPaySlabRepository slabRepository;
    @Autowired private DriverAdvanceRepository advanceRepository;
    @Autowired private DriverAdvanceRecoveryRepository recoveryRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private DocumentNumberService documentNumberService;
    @Autowired private ExpenseRepository expenseRepository;

    // ------------------------------------------------------------------ queries

    public Page<DriverPayroll> getPayrolls(Long companyId, String status, Pageable pageable) {
        return searchPayrolls(companyId, null, null, null, status, pageable);
    }

    public Page<DriverPayroll> searchPayrolls(Long companyId, Long driverId, Integer payYear, Integer payMonth,
                                              String status, Pageable pageable) {
        Long resolvedCompanyId = tenantAccess.resolveCompanyId(companyId);
        String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        return payrollRepository.search(resolvedCompanyId, restrictedBranchId(), driverId, payYear, payMonth, st, pageable);
    }

    public DriverPayroll getPayrollById(Long id) {
        DriverPayroll payroll = payrollRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> notFound(id));
        assertScope(payroll);
        return payroll;
    }

    public List<DriverPayroll> getPayrollsByDriver(Long driverId) {
        Driver driver = parentAccess.requireDriver(driverId);
        assertBranch(driver.getBranchId());
        return payrollRepository.findByDriverIdAndIsDeletedFalse(driverId);
    }

    /** Driver self-service: the logged-in driver's POSTED / PAID payrolls. */
    public List<DriverPayroll> getMyPayrolls() {
        Driver me = currentDriver();
        return payrollRepository.findByDriverIdAndStatusInAndIsDeletedFalseOrderByPayYearDescPayMonthDesc(
                me.getId(), List.of("POSTED", "PAID"));
    }

    // ------------------------------------------------------------------ generate / edit

    @Transactional
    public DriverPayroll createPayroll(DriverPayrollCreateDTO dto, String username) {
        if (dto == null || dto.getDriverId() == null) {
            throw new BusinessValidationException("Missing Driver", "INVALID_DRIVER",
                    "Driver ID is required for payroll processing.", "Select a valid driver.");
        }
        YearMonth period = validatePeriod(dto.getPayYear(), dto.getPayMonth());
        Driver driver = parentAccess.requireDriver(dto.getDriverId());
        assertBranch(driver.getBranchId());

        payrollRepository.findActiveForPeriod(driver.getId(), dto.getPayYear(), dto.getPayMonth()).ifPresent(p -> {
            throw duplicatePeriod(driver, period);
        });

        DriverPayroll payroll = new DriverPayroll();
        payroll.setDriver(driver);
        payroll.setPayYear(dto.getPayYear());
        payroll.setPayMonth(dto.getPayMonth());
        payroll.setStatus("DRAFT");
        payroll.setIsDeleted(false);
        payroll.setCompanyId(driver.getCompanyId());
        payroll.setBranchId(driver.getBranchId());
        payroll.setPayrollNumber(documentNumberService.next(driver.getCompanyId(), "DRIVER_PAYROLL", "PAY-", LocalDate.now()));
        payroll.setCode(payroll.getPayrollNumber());
        payroll.setName("Driver Payroll - " + driver.getName() + " (" + period + ")");
        payroll.setDescription(dto.getDescription() != null ? dto.getDescription() : "Driver monthly payroll");
        payroll.setCreatedBy(username);
        payroll.setUpdatedBy(username);

        applyInputs(payroll, dto, username);
        recalculate(payroll, username);

        DriverPayroll saved;
        try {
            saved = payrollRepository.saveAndFlush(payroll);
        } catch (DataIntegrityViolationException e) {
            // Another request created the same driver/month a moment earlier (unique active-period index).
            throw duplicatePeriod(driver, period);
        }
        auditService.log(username, "DRIVER_PAYROLL_CREATED", "driver_payrolls", saved.getId(), null,
                "Generated DRAFT payroll " + saved.getPayrollNumber() + ": " + saved.getTotalTrips() + " trips on "
                        + saved.getTripDays() + " days, net " + saved.getNetSalaryPayable());
        return saved;
    }

    @Transactional
    public DriverPayroll updatePayroll(Long id, DriverPayrollCreateDTO dto, String username) {
        DriverPayroll payroll = lockPayroll(id);
        requireStatus(payroll, "DRAFT", "edited");
        applyInputs(payroll, dto, username);
        if (dto.getDescription() != null) payroll.setDescription(dto.getDescription());
        recalculate(payroll, username);
        payroll.setUpdatedBy(username);
        DriverPayroll saved = payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_UPDATED", "driver_payrolls", saved.getId(), null,
                "Recalculated DRAFT payroll " + saved.getPayrollNumber() + ", net " + saved.getNetSalaryPayable());
        return saved;
    }

    /** Re-reads trips and slabs for a DRAFT payroll without changing the accountant's inputs. */
    @Transactional
    public DriverPayroll recalculatePayroll(Long id, String username) {
        DriverPayroll payroll = lockPayroll(id);
        requireStatus(payroll, "DRAFT", "recalculated");
        recalculate(payroll, username);
        payroll.setUpdatedBy(username);
        return payrollRepository.save(payroll);
    }

    @Transactional
    public void deletePayroll(Long id, String username) {
        DriverPayroll payroll = lockPayroll(id);
        if (!"DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException("Payroll Deletion Blocked", "PAYROLL_STATUS_UPDATE_BLOCKED",
                    String.format("Payroll %s is in %s status and cannot be deleted.", payroll.getPayrollNumber(), payroll.getStatus()),
                    "Only DRAFT payrolls can be deleted. Cancel an approved or posted payroll instead.");
        }
        payroll.setIsDeleted(true);
        payroll.setUpdatedBy(username);
        payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_DELETED", "driver_payrolls", id, null,
                "Deleted DRAFT payroll " + payroll.getPayrollNumber());
    }

    /** Copies accountant-entered values (allowance, advance recovery, deductions). Never trip counts or pay. */
    private void applyInputs(DriverPayroll payroll, DriverPayrollCreateDTO dto, String username) {
        BigDecimal allowance = DriverPayrollCalculator.money(dto.getAllowanceAmount() != null ? dto.getAllowanceAmount() : payroll.getAllowanceAmount());
        BigDecimal advance = DriverPayrollCalculator.money(dto.getAdvanceAdjustment() != null ? dto.getAdvanceAdjustment() : payroll.getAdvanceAdjustment());
        if (allowance.signum() < 0 || advance.signum() < 0) {
            throw new BusinessValidationException("Invalid Amount", "PAYROLL_NEGATIVE_AMOUNT",
                    "Allowance and advance recovery cannot be negative.", "Enter 0 or a positive amount.");
        }
        payroll.setAllowanceAmount(allowance);
        payroll.setAdvanceAdjustment(advance);

        List<DriverPayrollCreateDTO.Deduction> lines = dto.getDeductions();
        boolean sentLines = lines != null && !lines.isEmpty();
        if (!sentLines && dto.getDeductionAmount() != null && dto.getDeductionAmount().signum() > 0) {
            DriverPayrollCreateDTO.Deduction legacy = new DriverPayrollCreateDTO.Deduction();
            legacy.setDeductionType("OTHER");
            legacy.setAmount(dto.getDeductionAmount());
            lines = List.of(legacy);
            sentLines = true;
        }
        if (!sentLines && payroll.getId() != null && dto.getDeductions() == null) {
            return; // edit without a deductions list keeps the existing ones
        }
        payroll.getDeductions().clear();
        YearMonth period = YearMonth.of(payroll.getPayYear(), payroll.getPayMonth());
        for (DriverPayrollCreateDTO.Deduction in : lines == null ? List.<DriverPayrollCreateDTO.Deduction>of() : lines) {
            String type = in.getDeductionType() == null ? "OTHER" : in.getDeductionType().trim().toUpperCase();
            if (!DEDUCTION_TYPES.contains(type)) {
                throw new BusinessValidationException("Invalid Deduction Type", "PAYROLL_DEDUCTION_TYPE",
                        "Deduction type must be FINE, DAMAGE or OTHER.", "Pick a valid deduction type.");
            }
            BigDecimal amt = DriverPayrollCalculator.money(in.getAmount());
            if (amt.signum() <= 0) {
                throw new BusinessValidationException("Invalid Deduction", "PAYROLL_DEDUCTION_AMOUNT",
                        "Each deduction amount must be greater than zero.", "Enter the deduction amount or remove the row.");
            }
            DriverPayrollDeduction d = new DriverPayrollDeduction();
            d.setPayroll(payroll);
            d.setDeductionType(type);
            d.setAmount(amt);
            d.setDeductionDate(in.getDeductionDate() != null ? in.getDeductionDate() : period.atEndOfMonth().isAfter(LocalDate.now()) ? LocalDate.now() : period.atEndOfMonth());
            d.setRemarks(in.getRemarks());
            d.setCode("DED-" + type);
            d.setName("Payroll deduction " + type);
            d.setCompanyId(payroll.getCompanyId());
            d.setBranchId(payroll.getBranchId());
            d.setIsDeleted(false);
            d.setCreatedBy(username);
            d.setUpdatedBy(username);
            payroll.getDeductions().add(d);
        }
    }

    /** Server-side recalculation of days, trips, earnings, gross and net. */
    private void recalculate(DriverPayroll payroll, String username) {
        YearMonth period = YearMonth.of(payroll.getPayYear(), payroll.getPayMonth());
        List<DriverPaySlab> slabs = DriverPayrollCalculator.validateSlabs(
                slabRepository.findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(payroll.getCompanyId()));

        Map<LocalDate, Long> tripsByDate = new HashMap<>();
        for (Object[] row : tripRepository.countTripsByDate(payroll.getDriver().getId(), payroll.getCompanyId(),
                ELIGIBLE_TRIP_STATUSES, period.atDay(1), period.atEndOfMonth())) {
            tripsByDate.put((LocalDate) row[0], ((Number) row[1]).longValue());
        }
        DriverPayrollCalculator.Result result = DriverPayrollCalculator.calculate(tripsByDate, slabs);
        if (result.totalTrips() > 0 && slabs.isEmpty()) {
            throw new BusinessValidationException("Pay Slabs Not Configured", "PAY_SLABS_MISSING",
                    "The driver has " + result.totalTrips() + " completed trips in " + period + " but no daily pay slabs are set up.",
                    "Set up the daily pay slabs (e.g. 1 trip = 500, 2+ trips = 1000) before generating payroll.");
        }

        payroll.getDays().clear();
        for (DriverPayrollCalculator.Day day : result.days()) {
            DriverPayrollDay d = new DriverPayrollDay();
            d.setPayroll(payroll);
            d.setWorkDate(day.date());
            d.setTripCount(day.tripCount());
            d.setSlabTripsFrom(day.slabFrom());
            d.setSlabTripsTo(day.slabTo());
            d.setDailyAmount(day.amount());
            d.setCode("DAY-" + day.date());
            d.setName("Payroll day " + day.date());
            d.setCompanyId(payroll.getCompanyId());
            d.setBranchId(payroll.getBranchId());
            d.setIsDeleted(false);
            d.setCreatedBy(username);
            d.setUpdatedBy(username);
            payroll.getDays().add(d);
        }

        BigDecimal basic = DriverPayrollCalculator.money(driverSalaryService.getSalaryByDriver(payroll.getDriver().getId())
                .map(DriverSalary::getBasicSalary).orElse(BigDecimal.ZERO));
        if (basic.signum() < 0) basic = BigDecimal.ZERO.setScale(2);
        BigDecimal deductions = payroll.getDeductions().stream().map(DriverPayrollDeduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = result.tripEarnings().add(basic).add(DriverPayrollCalculator.money(payroll.getAllowanceAmount()));

        BigDecimal advance = DriverPayrollCalculator.money(payroll.getAdvanceAdjustment());
        if (advance.signum() > 0) {
            BigDecimal outstanding = DriverPayrollCalculator.money(advanceRepository.sumOutstanding(payroll.getDriver().getId()));
            if (advance.compareTo(outstanding) > 0) {
                throw advanceExceeds(advance, outstanding);
            }
        }

        BigDecimal bata = expenseRepository.sumDriverBata(payroll.getDriver().getId(), period.atDay(1), period.atEndOfMonth());
        payroll.setBataPaid(DriverPayrollCalculator.money(bata));
        payroll.setTotalTrips(result.totalTrips());
        payroll.setTripDays(result.tripDays());
        payroll.setTripEarnings(result.tripEarnings());
        payroll.setBasicSalary(basic);
        payroll.setGrossAmount(DriverPayrollCalculator.money(gross));
        payroll.setDeductionAmount(DriverPayrollCalculator.money(deductions));
        payroll.setNetSalaryPayable(DriverPayrollCalculator.net(gross, deductions, advance));
        if (payroll.getGrossAmount().signum() <= 0) {
            throw new BusinessValidationException("Nothing To Pay", "PAYROLL_ZERO_GROSS",
                    "No completed trips, basic salary or allowance for " + payroll.getDriver().getName() + " in " + period + ".",
                    "Check the driver's completed trips for the month or the salary configuration.");
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Transactional
    public DriverPayroll approvePayroll(Long id, String username) {
        DriverPayroll payroll = lockPayroll(id);
        requireStatus(payroll, "DRAFT", "approved");
        approvalPolicy.assertDifferentApprover(payroll.getCompanyId(), payroll.getCreatedBy(), username, "Payroll " + payroll.getPayrollNumber());
        recalculate(payroll, username); // approve what the trips say right now
        payroll.setStatus("APPROVED");
        payroll.setApprovedBy(username);
        payroll.setApprovedAt(LocalDateTime.now());
        payroll.setUpdatedBy(username);
        DriverPayroll saved = payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_APPROVED", "driver_payrolls", saved.getId(), null,
                "Approved payroll " + saved.getPayrollNumber() + " gross " + saved.getGrossAmount() + " net " + saved.getNetSalaryPayable());
        return saved;
    }

    @Transactional
    public DriverPayroll postPayroll(Long id, String username) {
        DriverPayroll payroll = lockPayroll(id);
        requireStatus(payroll, "APPROVED", "posted");

        YearMonth period = YearMonth.of(payroll.getPayYear(), payroll.getPayMonth());
        LocalDate postingDate = period.atEndOfMonth().isAfter(LocalDate.now()) ? LocalDate.now() : period.atEndOfMonth();
        periodValidationService.validatePostingAllowed(payroll.getCompanyId(), postingDate);

        Long cid = payroll.getCompanyId(), bid = payroll.getBranchId();
        ChartOfAccount expense = coaService.getOrCreateAccount(cid, bid, "5150", "Driver Salary Expense", "EXPENSE");
        ChartOfAccount payable = coaService.getOrCreateAccount(cid, bid, "2050", "Driver Salary Payable", "LIABILITY");

        JournalVoucher accrual = postJv(payroll, "SAL-ACC-PAY-" + payroll.getId(), "JV-SAL-ACC-" + payroll.getId(), postingDate,
                expense, payable, payroll.getGrossAmount(), "Salary Accrual JV",
                "Driver salary expense for payroll " + payroll.getPayrollNumber() + " (" + period + ")", username);
        payroll.setAccrualJvNumber(accrual.getVoucherNumber());

        // Advance recovery, FIFO over the driver's open advances (rows locked).
        BigDecimal toRecover = DriverPayrollCalculator.money(payroll.getAdvanceAdjustment());
        if (toRecover.signum() > 0) {
            List<DriverAdvance> open = advanceRepository.lockOutstandingForDriver(payroll.getDriver().getId());
            BigDecimal outstanding = open.stream().map(DriverAdvance::getOutstandingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (toRecover.compareTo(outstanding) > 0) throw advanceExceeds(toRecover, outstanding);
            BigDecimal left = toRecover;
            for (DriverAdvance adv : open) {
                if (left.signum() <= 0) break;
                BigDecimal take = left.min(adv.getOutstandingAmount());
                adv.setRecoveredAmount(adv.getRecoveredAmount().add(take));
                adv.setUpdatedBy(username);
                advanceRepository.save(adv);
                DriverAdvanceRecovery r = new DriverAdvanceRecovery();
                r.setPayrollId(payroll.getId());
                r.setAdvance(adv);
                r.setAmount(take);
                r.setStatus("ACTIVE");
                r.setCode("REC-" + payroll.getId() + "-" + adv.getId());
                r.setName("Advance recovery " + adv.getAdvanceNumber());
                r.setCompanyId(cid);
                r.setBranchId(bid);
                r.setIsDeleted(false);
                r.setCreatedBy(username);
                r.setUpdatedBy(username);
                recoveryRepository.save(r);
                left = left.subtract(take);
            }
            ChartOfAccount advances = coaService.getOrCreateAccount(cid, bid, "1150", "Driver Advances", "ASSET");
            JournalVoucher rec = postJv(payroll, "SAL-ADV-REC-" + payroll.getId(), "JV-SAL-ADV-" + payroll.getId(), postingDate,
                    payable, advances, toRecover, "Salary Advance Recovery JV",
                    "Advance recovered through payroll " + payroll.getPayrollNumber(), username);
            payroll.setRecoveryJvNumber(rec.getVoucherNumber());
        }

        BigDecimal deductions = DriverPayrollCalculator.money(payroll.getDeductionAmount());
        if (deductions.signum() > 0) {
            ChartOfAccount recoveries = coaService.getOrCreateAccount(cid, bid, "4900", "Driver Recoveries", "INCOME");
            JournalVoucher ded = postJv(payroll, "SAL-DED-" + payroll.getId(), "JV-SAL-DED-" + payroll.getId(), postingDate,
                    payable, recoveries, deductions, "Salary Deduction JV",
                    "Fines / damage / other deductions on payroll " + payroll.getPayrollNumber(), username);
            payroll.setDeductionJvNumber(ded.getVoucherNumber());
        }

        payroll.setStatus("POSTED");
        payroll.setPostingDate(postingDate);
        payroll.setPostedBy(username);
        payroll.setPostedAt(LocalDateTime.now());
        payroll.setUpdatedBy(username);
        DriverPayroll saved = payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_POSTED", "driver_payrolls", saved.getId(), null,
                "Posted payroll " + saved.getPayrollNumber() + ": expense " + saved.getGrossAmount()
                        + ", advance " + saved.getAdvanceAdjustment() + ", deductions " + saved.getDeductionAmount());
        return saved;
    }

    @Transactional
    public DriverPayroll payPayroll(Long id, DriverPayrollPaymentDTO paymentDto, String username) {
        DriverPayroll payroll = lockPayroll(id);
        requireStatus(payroll, "POSTED", "paid");

        String method = paymentDto != null && paymentDto.getPaymentMethod() != null ? paymentDto.getPaymentMethod().trim().toUpperCase() : "CASH";
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BusinessValidationException("Invalid Payment Method", "INVALID_PAYMENT_METHOD",
                    "Payment method must be CASH, BANK_TRANSFER, CHEQUE or UPI.", "Choose a valid payment method.");
        }
        LocalDate payDate = paymentDto != null && paymentDto.getPaymentDate() != null ? paymentDto.getPaymentDate() : LocalDate.now();
        if (payDate.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Payment Date In Future", "PAYROLL_PAY_DATE_FUTURE",
                    "Payment date " + payDate + " is in the future.", "Use today's date or earlier.");
        }
        if (payroll.getPostingDate() != null && payDate.isBefore(payroll.getPostingDate())) {
            throw new BusinessValidationException("Payment Before Posting", "PAYROLL_PAY_BEFORE_POSTING",
                    "Payment date " + payDate + " is before the payroll posting date " + payroll.getPostingDate() + ".",
                    "Use a payment date on or after the posting date.");
        }
        periodValidationService.validatePostingAllowed(payroll.getCompanyId(), payDate);

        if (payroll.getNetSalaryPayable().signum() > 0) {
            ChartOfAccount payable = coaService.getOrCreateAccount(payroll.getCompanyId(), payroll.getBranchId(), "2050", "Driver Salary Payable", "LIABILITY");
            ChartOfAccount cashOrBank = "CASH".equals(method)
                    ? coaService.getOrCreateAccount(payroll.getCompanyId(), payroll.getBranchId(), "1000", "Cash on Hand", "ASSET")
                    : coaService.getOrCreateAccount(payroll.getCompanyId(), payroll.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
            JournalVoucher jv = postJv(payroll, "SAL-PAY-PAY-" + payroll.getId(), "JV-SAL-PAY-" + payroll.getId(), payDate,
                    payable, cashOrBank, payroll.getNetSalaryPayable(), "Salary Payment JV",
                    "Driver salary paid for payroll " + payroll.getPayrollNumber(), username);
            payroll.setPaymentJvNumber(jv.getVoucherNumber());
        }
        payroll.setStatus("PAID");
        payroll.setPaymentMethod(method);
        payroll.setPaidDate(payDate);
        payroll.setPaidBy(username);
        payroll.setPaymentReference(paymentDto != null ? paymentDto.getPaymentReference() : null);
        payroll.setUpdatedBy(username);
        DriverPayroll saved = payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_PAID", "driver_payrolls", saved.getId(), null,
                "Paid payroll " + saved.getPayrollNumber() + " net " + saved.getNetSalaryPayable() + " via " + method);
        return saved;
    }

    /** APPROVED: just cancelled. POSTED / PAID: every payroll JV reversed and advance recoveries returned. */
    @Transactional
    public DriverPayroll cancelPayroll(Long id, String username) {
        DriverPayroll payroll = lockPayroll(id);
        String status = payroll.getStatus() == null ? "" : payroll.getStatus().toUpperCase();
        if ("CANCELLED".equals(status)) {
            throw new BusinessValidationException("Payroll Already Cancelled", "PAYROLL_ALREADY_CANCELLED",
                    "Payroll " + payroll.getPayrollNumber() + " is already cancelled.", "No action needed.");
        }
        if ("DRAFT".equals(status)) {
            throw new BusinessValidationException("Payroll Cancellation Blocked", "PAYROLL_DRAFT_CANCEL_BLOCKED",
                    "Payroll " + payroll.getPayrollNumber() + " is a DRAFT. Delete the draft instead of cancelling.",
                    "Use Delete for draft payrolls.");
        }
        List<String> reversals = new ArrayList<>();
        if ("POSTED".equals(status) || "PAID".equals(status)) {
            LocalDate today = LocalDate.now();
            periodValidationService.validatePostingAllowed(payroll.getCompanyId(), today);
            reverse(payroll, "SAL-PAY-PAY-", today, username, reversals);
            reverse(payroll, "SAL-DED-", today, username, reversals);
            reverse(payroll, "SAL-ADV-REC-", today, username, reversals);
            reverse(payroll, "SAL-ACC-PAY-", today, username, reversals);
            for (DriverAdvanceRecovery r : recoveryRepository.findByPayrollIdAndStatusAndIsDeletedFalse(payroll.getId(), "ACTIVE")) {
                DriverAdvance adv = advanceRepository.findByIdForUpdate(r.getAdvance().getId()).orElseThrow();
                adv.setRecoveredAmount(adv.getRecoveredAmount().subtract(r.getAmount()).max(BigDecimal.ZERO));
                adv.setUpdatedBy(username);
                advanceRepository.save(adv);
                r.setStatus("REVERSED");
                r.setUpdatedBy(username);
                recoveryRepository.save(r);
            }
        }
        payroll.setStatus("CANCELLED");
        payroll.setCancellationJvNumber(reversals.isEmpty() ? null : String.join(",", reversals));
        payroll.setUpdatedBy(username);
        DriverPayroll saved = payrollRepository.save(payroll);
        auditService.log(username, "DRIVER_PAYROLL_CANCELLED", "driver_payrolls", saved.getId(), null,
                "Cancelled payroll " + saved.getPayrollNumber() + (reversals.isEmpty() ? "" : " with reversals " + reversals));
        return saved;
    }

    // ------------------------------------------------------------------ helpers

    private JournalVoucher postJv(DriverPayroll p, String reference, String voucherNumber, LocalDate date,
                                  ChartOfAccount debit, ChartOfAccount credit, BigDecimal amount,
                                  String name, String description, String username) {
        Optional<JournalVoucher> existing = jvRepository.findByReferenceNumberAndIsDeletedFalse(reference).stream().findFirst();
        if (existing.isPresent()) return existing.get(); // idempotent: never a second JV for the same step
        JournalVoucher jv = new JournalVoucher();
        jv.setVoucherNumber(voucherNumber);
        jv.setVoucherDate(date);
        jv.setReferenceNumber(reference);
        jv.setDebitAccount(debit);
        jv.setCreditAccount(credit);
        jv.setAmount(amount);
        jv.setDescription(description);
        jv.setCompanyId(p.getCompanyId());
        jv.setBranchId(p.getBranchId());
        jv.setCode(reference);
        jv.setName(name);
        jv.setStatus("POSTED");
        jv.setIsDeleted(false);
        jv.setCreatedBy(username);
        jv.setUpdatedBy(username);
        return jvRepository.save(jv);
    }

    private void reverse(DriverPayroll p, String refPrefix, LocalDate date, String username, List<String> out) {
        String ref = refPrefix + p.getId();
        Optional<JournalVoucher> orig = jvRepository.findByReferenceNumberAndIsDeletedFalse(ref).stream().findFirst();
        if (orig.isEmpty()) return;
        JournalVoucher o = orig.get();
        JournalVoucher rev = postJv(p, "REV-" + ref, "REV-" + o.getVoucherNumber(), date, o.getCreditAccount(), o.getDebitAccount(),
                o.getAmount(), "Reversal " + o.getName(), "Reversal of " + o.getVoucherNumber() + " for cancelled payroll " + p.getPayrollNumber(), username);
        out.add(rev.getVoucherNumber());
    }

    private DriverPayroll lockPayroll(Long id) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        assertScope(payroll);
        return payroll;
    }

    private void requireStatus(DriverPayroll p, String expected, String action) {
        if (!expected.equalsIgnoreCase(p.getStatus())) {
            throw new BusinessValidationException("Invalid Payroll Status", "PAYROLL_STATUS_" + expected + "_REQUIRED",
                    String.format("Payroll %s is %s and cannot be %s.", p.getPayrollNumber(), p.getStatus(), action),
                    "Payroll moves DRAFT -> APPROVED -> POSTED -> PAID.");
        }
    }

    private YearMonth validatePeriod(Integer year, Integer month) {
        if (year == null || year < 2000 || month == null || month < 1 || month > 12) {
            throw new BusinessValidationException("Invalid Pay Period", "INVALID_PAY_PERIOD",
                    "Pay year must be 2000 or later and pay month between 1 and 12.", "Choose a valid month.");
        }
        YearMonth period = YearMonth.of(year, month);
        if (period.isAfter(YearMonth.now())) {
            throw new BusinessValidationException("Future Pay Period", "PAYROLL_FUTURE_PERIOD",
                    "Payroll cannot be generated for " + period + " before that month starts.", "Choose the current or a past month.");
        }
        return period;
    }

    private void assertScope(DriverPayroll p) {
        tenantAccess.assertOwned(p.getCompanyId());
        assertBranch(p.getBranchId());
    }

    /** Branch-bound users (not company-wide admins) only see their own branch. */
    private void assertBranch(Long resourceBranchId) {
        Long mine = restrictedBranchId();
        if (mine != null && resourceBranchId != null && !mine.equals(resourceBranchId)) {
            throw new AccessDeniedException("Access denied: payroll belongs to another branch.");
        }
    }

    private Long restrictedBranchId() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (tenantAccess.isSuperAdmin(user)) return null;
        boolean companyWide = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()) || "ADMIN".equals(r.getCode()));
        return companyWide ? null : user.getBranchId();
    }

    private Driver currentDriver() {
        AppUser user = tenantAccess.requireCurrentUser();
        return driverRepository.findByAppUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Your login is not linked to a driver."));
    }

    private DriverPayroll requireOwnPayroll(Long id) {
        Driver me = currentDriver();
        DriverPayroll p = payrollRepository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> notFound(id));
        if (p.getDriver() == null || !me.getId().equals(p.getDriver().getId())
                || !("POSTED".equals(p.getStatus()) || "PAID".equals(p.getStatus()))) {
            throw new AccessDeniedException("You can only view your own posted salary slips.");
        }
        return p;
    }

    private static BusinessValidationException notFound(Long id) {
        return new BusinessValidationException("Payroll Not Found", "PAYROLL_NOT_FOUND",
                "Driver payroll not found with ID: " + id, "Verify the payroll ID.");
    }

    private static BusinessValidationException duplicatePeriod(Driver d, YearMonth period) {
        return new BusinessValidationException("Duplicate Pay Period", "PAYROLL_DUPLICATE_PAY_PERIOD",
                String.format("Payroll already exists for Driver '%s' for %s.", d.getName(), period),
                "Open the existing payroll, or cancel it first to generate again.");
    }

    private static BusinessValidationException advanceExceeds(BigDecimal wanted, BigDecimal outstanding) {
        return new BusinessValidationException("Advance Recovery Too High", "PAYROLL_ADVANCE_EXCEEDS_OUTSTANDING",
                String.format("Advance recovery ₹%s is more than the driver's outstanding advance ₹%s.",
                        wanted.toPlainString(), outstanding.toPlainString()),
                "Reduce the advance recovery to the outstanding amount or less.");
    }

    @Transactional(readOnly = true)
    public com.transport.erp.dto.DriverPayrollPrintDTO getSalarySlipPrintData(Long id) {
        return buildPrintData(getPayrollById(id));
    }

    /** Driver self-service: own POSTED / PAID payroll only. */
    @Transactional(readOnly = true)
    public com.transport.erp.dto.DriverPayrollPrintDTO getMySalarySlip(Long id) {
        return buildPrintData(requireOwnPayroll(id));
    }

    private com.transport.erp.dto.DriverPayrollPrintDTO buildPrintData(DriverPayroll payroll) {

        com.transport.erp.dto.DriverPayrollPrintDTO dto = new com.transport.erp.dto.DriverPayrollPrintDTO();
        dto.setPayrollId(payroll.getId());
        dto.setPayrollNumber(payroll.getPayrollNumber());
        dto.setPayYear(payroll.getPayYear());
        dto.setPayMonth(payroll.getPayMonth());

        if (payroll.getPayMonth() != null && payroll.getPayMonth() >= 1 && payroll.getPayMonth() <= 12) {
            String monthName = java.time.Month.of(payroll.getPayMonth()).name();
            monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).toLowerCase();
            dto.setPayPeriod(monthName + " " + payroll.getPayYear());
        } else {
            dto.setPayPeriod(payroll.getPayMonth() + "/" + payroll.getPayYear());
        }

        dto.setStatus(payroll.getStatus());
        dto.setPaymentMethod(payroll.getPaymentMethod());
        dto.setCreatedDate(payroll.getCreatedDate());

        if (payroll.getDriver() != null) {
            Driver d = payroll.getDriver();
            dto.setDriverId(d.getId());
            dto.setDriverName(d.getName());
            dto.setDriverCode(d.getCode());
            dto.setDriverPhone(d.getPhoneNumber());
            dto.setLicenseNumber(d.getLicenseNumber());
        }

        BigDecimal basic = payroll.getBasicSalary() != null ? payroll.getBasicSalary() : BigDecimal.ZERO;
        BigDecimal allowance = payroll.getAllowanceAmount() != null ? payroll.getAllowanceAmount() : BigDecimal.ZERO;
        BigDecimal deduction = payroll.getDeductionAmount() != null ? payroll.getDeductionAmount() : BigDecimal.ZERO;
        BigDecimal advance = payroll.getAdvanceAdjustment() != null ? payroll.getAdvanceAdjustment() : BigDecimal.ZERO;

        BigDecimal tripEarnings = payroll.getTripEarnings() != null ? payroll.getTripEarnings() : BigDecimal.ZERO;
        dto.setBasicSalary(basic);
        dto.setAllowanceAmount(allowance);
        dto.setTripEarnings(tripEarnings);
        dto.setTotalTrips(payroll.getTotalTrips());
        dto.setTripDays(payroll.getTripDays());
        dto.setGrossEarnings(payroll.getGrossAmount() != null && payroll.getGrossAmount().signum() > 0
                ? payroll.getGrossAmount() : basic.add(allowance).add(tripEarnings));
        dto.setBataPaid(payroll.getBataPaid() != null ? payroll.getBataPaid() : BigDecimal.ZERO);
        dto.setPostingDate(payroll.getPostingDate());
        dto.setPaidDate(payroll.getPaidDate());
        dto.setPaymentReference(payroll.getPaymentReference());
        dto.setRecoveryJvNumber(payroll.getRecoveryJvNumber());
        dto.setDeductionJvNumber(payroll.getDeductionJvNumber());
        for (DriverPayrollDay d : payroll.getDays()) {
            com.transport.erp.dto.DriverPayrollPrintDTO.DayLine line = new com.transport.erp.dto.DriverPayrollPrintDTO.DayLine();
            line.setWorkDate(d.getWorkDate());
            line.setTripCount(d.getTripCount());
            line.setSlab(slabLabel(d.getSlabTripsFrom(), d.getSlabTripsTo()));
            line.setDailyAmount(d.getDailyAmount());
            dto.getDays().add(line);
        }
        for (DriverPayrollDeduction d : payroll.getDeductions()) {
            com.transport.erp.dto.DriverPayrollPrintDTO.DeductionLine line = new com.transport.erp.dto.DriverPayrollPrintDTO.DeductionLine();
            line.setDeductionType(d.getDeductionType());
            line.setDeductionDate(d.getDeductionDate());
            line.setAmount(d.getAmount());
            line.setRemarks(d.getRemarks());
            dto.getDeductions().add(line);
        }

        dto.setDeductionAmount(deduction);
        dto.setAdvanceAdjustment(advance);
        dto.setTotalDeductions(deduction.add(advance));

        dto.setNetSalaryPayable(payroll.getNetSalaryPayable() != null ? payroll.getNetSalaryPayable() : BigDecimal.ZERO);

        dto.setAccrualJvNumber(payroll.getAccrualJvNumber());
        dto.setPaymentJvNumber(payroll.getPaymentJvNumber());
        dto.setCancellationJvNumber(payroll.getCancellationJvNumber());

        if (payroll.getCompanyId() != null) {
            dto.setCompanyId(payroll.getCompanyId());
            companyRepository.findById(payroll.getCompanyId()).ifPresent(c -> {
                dto.setCompanyName(c.getName());
                dto.setCompanyAddress(c.getAddress());
                dto.setCompanyPhone(c.getPhone());
                dto.setCompanyEmail(c.getEmail());
                dto.setCompanyGSTIN(c.getGstNumber());
            });
        }

        if (payroll.getBranchId() != null) {
            dto.setBranchId(payroll.getBranchId());
            branchRepository.findById(payroll.getBranchId()).ifPresent(b -> {
                dto.setBranchName(b.getName());
                dto.setBranchAddress(b.getAddress());
            });
        }

        return dto;
    }

    private static String slabLabel(Integer from, Integer to) {
        if (from == null) return "No slab";
        if (to == null) return from + "+ trips";
        return from.equals(to) ? from + (from == 1 ? " trip" : " trips") : from + "-" + to + " trips";
    }

    @Transactional(readOnly = true)
    public void generateMySalarySlipPdf(Long id, java.io.OutputStream os) throws Exception {
        com.transport.erp.util.DriverSalarySlipPdfGenerator.generateSalarySlipPdf(getMySalarySlip(id), os);
    }

    @Transactional(readOnly = true)
    public void generateSalarySlipPdf(Long id, java.io.OutputStream os) throws Exception {
        com.transport.erp.dto.DriverPayrollPrintDTO data = getSalarySlipPrintData(id);
        com.transport.erp.util.DriverSalarySlipPdfGenerator.generateSalarySlipPdf(data, os);
    }
}
