package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.DriverAdvanceRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cash advances to drivers.
 * Issue : Dr Driver Advances (1150, asset) / Cr Cash or Bank.
 * Recovery happens only when a payroll is POSTED (see DriverPayrollService).
 * Cancel: allowed only while nothing has been recovered; the issue JV is reversed.
 */
@Service
public class DriverAdvanceService {

    private static final List<String> PAYMENT_METHODS = List.of("CASH", "BANK_TRANSFER", "CHEQUE", "UPI");

    @Autowired private DriverAdvanceRepository advanceRepository;
    @Autowired private JournalVoucherRepository jvRepository;
    @Autowired private ChartOfAccountService coaService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private TenantParentAccess parentAccess;
    @Autowired private FinancialYearPeriodValidationService periodValidationService;
    @Autowired private DocumentNumberService documentNumberService;
    @Autowired private AuditService auditService;

    public Page<DriverAdvance> search(Long companyId, Long driverId, String status, Pageable pageable) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        return advanceRepository.search(cid, driverId, restrictedBranchId(), st, pageable);
    }

    public BigDecimal outstanding(Long driverId) {
        Driver driver = parentAccess.requireDriver(driverId);
        assertBranch(driver.getBranchId());
        BigDecimal v = advanceRepository.sumOutstanding(driverId);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional
    public DriverAdvance issue(DriverAdvance request, String username) {
        if (request.getDriver() == null || request.getDriver().getId() == null) {
            throw new IllegalArgumentException("Driver is required for an advance.");
        }
        Driver driver = parentAccess.requireDriver(request.getDriver().getId());
        assertBranch(driver.getBranchId());
        BigDecimal amount = DriverPayrollCalculator.money(request.getAmount());
        if (amount.signum() <= 0) {
            throw new BusinessValidationException("Invalid Advance", "ADVANCE_AMOUNT_INVALID",
                    "Advance amount must be greater than zero.", "Enter the amount given to the driver.");
        }
        String method = request.getPaymentMethod() == null ? "CASH" : request.getPaymentMethod().trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BusinessValidationException("Invalid Payment Method", "INVALID_PAYMENT_METHOD",
                    "Payment method must be CASH, BANK_TRANSFER, CHEQUE or UPI.", "Choose a valid payment method.");
        }
        LocalDate date = request.getAdvanceDate() != null ? request.getAdvanceDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Advance Date In Future", "ADVANCE_DATE_FUTURE",
                    "Advance date " + date + " is in the future.", "Use today's date or earlier.");
        }
        periodValidationService.validatePostingAllowed(driver.getCompanyId(), date);

        DriverAdvance adv = new DriverAdvance();
        adv.setDriver(driver);
        adv.setAmount(amount);
        adv.setRecoveredAmount(BigDecimal.ZERO);
        adv.setAdvanceDate(date);
        adv.setPaymentMethod(method);
        adv.setPaymentReference(request.getPaymentReference());
        adv.setRemarks(request.getRemarks());
        adv.setStatus("ISSUED");
        adv.setCompanyId(driver.getCompanyId());
        adv.setBranchId(driver.getBranchId());
        adv.setAdvanceNumber(documentNumberService.next(driver.getCompanyId(), "DRIVER_ADVANCE", "ADV-", date));
        adv.setCode(adv.getAdvanceNumber());
        adv.setName("Driver advance - " + driver.getName());
        adv.setIsDeleted(false);
        adv.setCreatedBy(username);
        adv.setUpdatedBy(username);
        adv = advanceRepository.save(adv);

        ChartOfAccount advances = coaService.getOrCreateAccount(adv.getCompanyId(), adv.getBranchId(), "1150", "Driver Advances", "ASSET");
        ChartOfAccount cashOrBank = "CASH".equals(method)
                ? coaService.getOrCreateAccount(adv.getCompanyId(), adv.getBranchId(), "1000", "Cash on Hand", "ASSET")
                : coaService.getOrCreateAccount(adv.getCompanyId(), adv.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        JournalVoucher jv = postJv(adv, "DADV-" + adv.getId(), "JV-DADV-" + adv.getId(), date, advances, cashOrBank,
                amount, "Driver Advance JV", "Advance " + adv.getAdvanceNumber() + " given to " + driver.getName(), username);
        adv.setJvNumber(jv.getVoucherNumber());
        DriverAdvance saved = advanceRepository.save(adv);
        auditService.log(username, "DRIVER_ADVANCE_ISSUED", "driver_advances", saved.getId(), null,
                "Issued advance " + saved.getAdvanceNumber() + " of " + amount + " to " + driver.getName());
        return saved;
    }

    @Transactional
    public DriverAdvance cancel(Long id, String username) {
        DriverAdvance adv = advanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance not found: " + id));
        tenantAccess.assertOwned(adv.getCompanyId());
        assertBranch(adv.getBranchId());
        if (!"ISSUED".equals(adv.getStatus())) {
            throw new BusinessValidationException("Advance Not Active", "ADVANCE_NOT_ISSUED",
                    "Advance " + adv.getAdvanceNumber() + " is " + adv.getStatus() + ".", "No action needed.");
        }
        if (adv.getRecoveredAmount() != null && adv.getRecoveredAmount().signum() > 0) {
            throw new BusinessValidationException("Advance Partly Recovered", "ADVANCE_ALREADY_RECOVERED",
                    "Advance " + adv.getAdvanceNumber() + " has ₹" + adv.getRecoveredAmount().toPlainString()
                            + " recovered through payroll and cannot be cancelled.",
                    "Cancel the payroll that recovered it first.");
        }
        LocalDate today = LocalDate.now();
        periodValidationService.validatePostingAllowed(adv.getCompanyId(), today);
        Optional<JournalVoucher> orig = jvRepository.findByReferenceNumberAndIsDeletedFalse("DADV-" + adv.getId()).stream().findFirst();
        if (orig.isPresent()) {
            JournalVoucher o = orig.get();
            JournalVoucher rev = postJv(adv, "REV-DADV-" + adv.getId(), "REV-" + o.getVoucherNumber(), today,
                    o.getCreditAccount(), o.getDebitAccount(), o.getAmount(), "Reversal Driver Advance JV",
                    "Reversal of " + o.getVoucherNumber() + " for cancelled advance " + adv.getAdvanceNumber(), username);
            adv.setCancellationJvNumber(rev.getVoucherNumber());
        }
        adv.setStatus("CANCELLED");
        adv.setUpdatedBy(username);
        DriverAdvance saved = advanceRepository.save(adv);
        auditService.log(username, "DRIVER_ADVANCE_CANCELLED", "driver_advances", saved.getId(), null,
                "Cancelled advance " + saved.getAdvanceNumber());
        return saved;
    }

    private JournalVoucher postJv(DriverAdvance a, String reference, String voucherNumber, LocalDate date,
                                  ChartOfAccount debit, ChartOfAccount credit, BigDecimal amount,
                                  String name, String description, String username) {
        Optional<JournalVoucher> existing = jvRepository.findByReferenceNumberAndIsDeletedFalse(reference).stream().findFirst();
        if (existing.isPresent()) return existing.get();
        JournalVoucher jv = new JournalVoucher();
        jv.setVoucherNumber(voucherNumber);
        jv.setVoucherDate(date);
        jv.setReferenceNumber(reference);
        jv.setDebitAccount(debit);
        jv.setCreditAccount(credit);
        jv.setAmount(amount);
        jv.setDescription(description);
        jv.setCompanyId(a.getCompanyId());
        jv.setBranchId(a.getBranchId());
        jv.setCode(reference);
        jv.setName(name);
        jv.setStatus("POSTED");
        jv.setIsDeleted(false);
        jv.setCreatedBy(username);
        jv.setUpdatedBy(username);
        return jvRepository.save(jv);
    }

    private void assertBranch(Long resourceBranchId) {
        Long mine = restrictedBranchId();
        if (mine != null && resourceBranchId != null && !mine.equals(resourceBranchId)) {
            throw new AccessDeniedException("Access denied: record belongs to another branch.");
        }
    }

    private Long restrictedBranchId() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (tenantAccess.isSuperAdmin(user)) return null;
        boolean companyWide = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()) || "ADMIN".equals(r.getCode()));
        return companyWide ? null : user.getBranchId();
    }
}
