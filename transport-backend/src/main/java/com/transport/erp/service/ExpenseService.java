package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.Expense;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.repository.ExpenseRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    @org.springframework.beans.factory.annotation.Autowired
    private ApprovalPolicyService approvalPolicy;

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private JournalVoucherService jvService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AppSettingService settingService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;



    public Page<Expense> getExpenses(Long companyId, String status, Pageable pageable) {
        if (status != null && !status.trim().isEmpty()) {
            return expenseRepository.findByCompanyIdAndIsDeletedFalseAndStatus(companyId, status, pageable);
        }
        return expenseRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Expense getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
        tenantAccess.assertOwned(expense.getCompanyId());
        return expense;
    }

    @Transactional
    public Expense createExpense(Expense expense, String username) {
        String prefix = settingService.getByKey("PREFIX_EXPENSE").map(s -> s.getValueData()).orElse("EXP-");
        String defaultStatus = settingService.getByKey("DEFAULT_EXPENSE_STATUS").map(s -> s.getValueData()).orElse("SUBMITTED");
        
        // Bills can be entered after the day they were paid; never in the future.
        LocalDate expenseDate = expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now();
        if (expenseDate.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Expense Date In Future", "EXPENSE_DATE_IN_FUTURE",
                    "Expense date " + expenseDate + " is in the future.", "Use the date the money was spent (today or earlier).");
        }
        expense.setExpenseDate(expenseDate);
        expense.setExpenseNumber(documentNumberService.next(tenantAccess.resolveCompanyId(expense.getCompanyId()),
                DocumentNumberService.EXPENSE, prefix, expenseDate));
        expense.setStatus(defaultStatus);
        expense.setIsDeleted(false);
        expense.setCreatedBy(username);
        expense.setUpdatedBy(username);

        expense.setCompanyId(tenantAccess.resolveCompanyId(expense.getCompanyId()));
        expense.setBranchId(tenantAccess.resolveBranchId(expense.getBranchId()));

        // Calc totalAmount
        BigDecimal amt = expense.getAmount() != null ? expense.getAmount() : BigDecimal.ZERO;
        BigDecimal gst = expense.getGstAmount() != null ? expense.getGstAmount() : BigDecimal.ZERO;
        expense.setTotalAmount(amt.add(gst));

        Expense saved = expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_CREATED", "expenses", saved.getId(), null,
                "Registered expense voucher number: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public Expense updateExpense(Long id, Expense details, String username) {
        Expense existing = getExpenseById(id);

        if ("APPROVED".equalsIgnoreCase(existing.getStatus()) || "PAID".equalsIgnoreCase(existing.getStatus()) || "CANCELLED".equalsIgnoreCase(existing.getStatus())) {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add(String.format("Expense '%s' is in %s status.", existing.getExpenseNumber(), existing.getStatus()));
            throw new BusinessValidationException(
                    "Expense Update Blocked",
                    "EXPENSE_STATUS_UPDATE_BLOCKED",
                    String.format("Expense '%s' cannot be modified because its current status is %s.", existing.getExpenseNumber(), existing.getStatus()),
                    "Only DRAFT or SUBMITTED expenses can be modified.",
                    errorDetails
            );
        }

        if (details.getExpenseDate() != null) {
            if (details.getExpenseDate().isAfter(LocalDate.now())) {
                throw new BusinessValidationException("Expense Date In Future", "EXPENSE_DATE_IN_FUTURE",
                        "Expense date " + details.getExpenseDate() + " is in the future.", "Use today's date or earlier.");
            }
            existing.setExpenseDate(details.getExpenseDate());
        }
        existing.setCategory(details.getCategory());
        existing.setVehicle(details.getVehicle());
        existing.setDriver(details.getDriver());
        existing.setTrip(details.getTrip());
        existing.setDescription(details.getDescription());
        existing.setAmount(details.getAmount());
        existing.setGstAmount(details.getGstAmount());
        BigDecimal amt = details.getAmount() != null ? details.getAmount() : BigDecimal.ZERO;
        BigDecimal gst = details.getGstAmount() != null ? details.getGstAmount() : BigDecimal.ZERO;
        existing.setTotalAmount(amt.add(gst));
        existing.setPaymentMethod(details.getPaymentMethod());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        Expense saved = expenseRepository.save(existing);

        auditService.log(username, "EXPENSE_UPDATED", "expenses", saved.getId(), null,
                "Modified details for expense: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public Expense approveExpense(Long id, String username) {
        Expense expense = expenseRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));

        tenantAccess.assertOwned(expense.getCompanyId());
        approvalPolicy.assertDifferentApprover(expense.getCompanyId(), expense.getCreatedBy(), username, "Expense " + expense.getExpenseNumber());
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && expense.getBranchId() != null && !currentUser.getBranchId().equals(expense.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Expense belongs to another branch.");
            }
        }

        if ("APPROVED".equalsIgnoreCase(expense.getStatus()) || "PAID".equalsIgnoreCase(expense.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Expense '%s' is already %s.", expense.getExpenseNumber(), expense.getStatus()));
            throw new BusinessValidationException(
                    "Expense Already Approved",
                    "EXPENSE_ALREADY_APPROVED",
                    String.format("Expense '%s' is already %s.", expense.getExpenseNumber(), expense.getStatus()),
                    "No further approval action can be taken on an approved expense.",
                    details
            );
        }

        if (!"SUBMITTED".equalsIgnoreCase(expense.getStatus()) && !"DRAFT".equalsIgnoreCase(expense.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Expense '%s' cannot be approved because its current status is %s.", expense.getExpenseNumber(), expense.getStatus()));
            throw new BusinessValidationException(
                    "Expense Approval Blocked",
                    "EXPENSE_STATUS_APPROVAL_BLOCKED",
                    String.format("Expense '%s' cannot be approved because its current status is %s.", expense.getExpenseNumber(), expense.getStatus()),
                    "Only DRAFT or SUBMITTED expenses can be approved.",
                    details
            );
        }

        // Validate Financial Year period status
        LocalDate expensePostingDate = expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now();
        periodValidationService.validatePostingAllowed(expense.getCompanyId(), expensePostingDate);

        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(expense.getExpenseNumber());

        if (existingJvs != null && !existingJvs.isEmpty()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Accounting vouchers already exist for expense '%s'.", expense.getExpenseNumber()));
            throw new BusinessValidationException(
                    "Duplicate Accounting Blocked",
                    "EXPENSE_ACCOUNTING_EXISTS",
                    String.format("Accounting entry already exists for expense '%s'.", expense.getExpenseNumber()),
                    "Expense accounting has already been posted.",
                    details
            );
        }

        expense.setStatus("APPROVED");
        expense.setUpdatedBy(username);
        Expense saved = expenseRepository.save(expense);

        // Expense Category Debit Account mapping
        String cat = saved.getCategory() != null ? saved.getCategory().toUpperCase() : "GENERAL";
        ChartOfAccount expenseCategoryAcc;
        if ("TOLL".equals(cat) || "PARKING".equals(cat)) {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5200", "Toll & Parking Charges", "EXPENSE");
        } else if ("DRIVER_BATA".equals(cat)) {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5300", "Driver Bata & Wages", "EXPENSE");
        } else if ("VEHICLE_REPAIR".equals(cat) || "REPAIR".equals(cat) || "MAINTENANCE".equals(cat)) {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5400", "Vehicle Maintenance & Repair", "EXPENSE");
        } else if ("INSURANCE".equals(cat)) {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5500", "Vehicle & General Insurance", "EXPENSE");
        } else if ("OFFICE".equals(cat)) {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5600", "Office & Administrative Expenses", "EXPENSE");
        } else {
            expenseCategoryAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5000", "General Operating Expenses", "EXPENSE");
        }

        // Credit Account determined by paymentMethod
        ChartOfAccount creditAcc;
        String payMethod = saved.getPaymentMethod() != null ? saved.getPaymentMethod().toUpperCase() : "CASH";
        if ("CASH".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1000", "Cash on Hand", "ASSET");
        } else if ("CREDIT".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "2000", "Accounts Payable", "LIABILITY");
        } else {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        }

        BigDecimal amount = saved.getTotalAmount() != null ? saved.getTotalAmount() : (saved.getAmount() != null ? saved.getAmount() : BigDecimal.ZERO);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            JournalVoucher jv = new JournalVoucher();
            jv.setVoucherNumber("JV-EXP-" + saved.getId());
            jv.setVoucherDate(expensePostingDate);
            jv.setDebitAccount(expenseCategoryAcc);
            jv.setCreditAccount(creditAcc);
            jv.setAmount(amount);
            jv.setReferenceNumber(saved.getExpenseNumber());
            jv.setDescription("Auto-posted expense JV for " + saved.getExpenseNumber());
            jv.setCompanyId(saved.getCompanyId());
            jv.setBranchId(saved.getBranchId());
            jv.setIsDeleted(false);
            jv.setCreatedBy(username);
            jv.setUpdatedBy(username);
            jv.setCode(jv.getVoucherNumber());
            jv.setName("Operational Expense Voucher");
            jvService.createVoucher(jv, username);
        }

        auditService.log(username, "EXPENSE_APPROVED", "expenses", saved.getId(), null,
                "Approved expense voucher: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public Expense cancelExpense(Long id, String username) {
        Expense expense = expenseRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));

        tenantAccess.assertOwned(expense.getCompanyId());
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && expense.getBranchId() != null && !currentUser.getBranchId().equals(expense.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Expense belongs to another branch.");
            }
        }

        if ("CANCELLED".equalsIgnoreCase(expense.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Expense '%s' is already CANCELLED.", expense.getExpenseNumber()));
            throw new BusinessValidationException(
                    "Expense Already Cancelled",
                    "EXPENSE_ALREADY_CANCELLED",
                    String.format("Expense '%s' is already CANCELLED.", expense.getExpenseNumber()),
                    "No further cancellation action can be taken on a cancelled expense.",
                    details
            );
        }

        if (!"APPROVED".equalsIgnoreCase(expense.getStatus()) && !"PAID".equalsIgnoreCase(expense.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Expense '%s' cannot be cancelled because its current status is %s.", expense.getExpenseNumber(), expense.getStatus()));
            throw new BusinessValidationException(
                    "Expense Cancellation Blocked",
                    "EXPENSE_STATUS_CANCELLATION_BLOCKED",
                    String.format("Expense '%s' cannot be cancelled because its current status is %s.", expense.getExpenseNumber(), expense.getStatus()),
                    "Only APPROVED or PAID expenses can be cancelled.",
                    details
            );
        }

        // Validate Financial Year period status
        periodValidationService.validatePostingAllowed(expense.getCompanyId(), LocalDate.now());


        expense.setStatus("CANCELLED");
        expense.setUpdatedBy(username);
        Expense saved = expenseRepository.save(expense);

        // Reversal of JVs
        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(expense.getExpenseNumber());
        if (existingJvs != null) {
            for (JournalVoucher origJv : existingJvs) {
                JournalVoucher revJv = new JournalVoucher();
                revJv.setVoucherNumber("REV-JV-EXP-" + saved.getId() + "-" + origJv.getId());
                revJv.setVoucherDate(LocalDate.now());
                revJv.setDebitAccount(origJv.getCreditAccount());
                revJv.setCreditAccount(origJv.getDebitAccount());
                revJv.setAmount(origJv.getAmount());
                revJv.setReferenceNumber("REV-" + origJv.getReferenceNumber());
                revJv.setDescription("Reversal of JV " + origJv.getVoucherNumber() + " for cancelled expense " + saved.getExpenseNumber());
                revJv.setCompanyId(saved.getCompanyId());
                revJv.setBranchId(saved.getBranchId());
                revJv.setIsDeleted(false);
                revJv.setCreatedBy(username);
                revJv.setUpdatedBy(username);
                revJv.setCode(revJv.getVoucherNumber());
                revJv.setName("Expense Reversal Voucher");
                jvService.createVoucher(revJv, username);
            }
        }

        auditService.log(username, "EXPENSE_CANCELLED", "expenses", saved.getId(), null,
                "Cancelled expense voucher: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public Expense rejectExpense(Long id, String username) {
        Expense expense = getExpenseById(id);
        expense.setStatus("REJECTED");
        expense.setUpdatedBy(username);

        Expense saved = expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_REJECTED", "expenses", saved.getId(), null,
                "Rejected expense voucher: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public void deleteExpense(Long id, String username) {
        Expense expense = getExpenseById(id);

        if ("APPROVED".equalsIgnoreCase(expense.getStatus()) || "PAID".equalsIgnoreCase(expense.getStatus()) || "CANCELLED".equalsIgnoreCase(expense.getStatus())) {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add(String.format("Expense '%s' is in %s status.", expense.getExpenseNumber(), expense.getStatus()));
            throw new BusinessValidationException(
                    "Expense Delete Blocked",
                    "EXPENSE_STATUS_DELETE_BLOCKED",
                    String.format("Expense '%s' cannot be deleted because its current status is %s.", expense.getExpenseNumber(), expense.getStatus()),
                    "Only DRAFT or SUBMITTED expenses can be deleted.",
                    errorDetails
            );
        }

        expense.setIsDeleted(true);
        expense.setUpdatedBy(username);
        expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_DELETED", "expenses", expense.getId(), null,
                "Soft deleted expense entry: " + expense.getExpenseNumber());
    }
}
