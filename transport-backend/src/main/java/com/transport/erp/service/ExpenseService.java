package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.Expense;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




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
        
        expense.setExpenseNumber(prefix + System.currentTimeMillis());
        expense.setExpenseDate(LocalDate.now());
        expense.setStatus(defaultStatus);
        expense.setIsDeleted(false);
        expense.setCreatedBy(username);
        expense.setUpdatedBy(username);

        expense.setCompanyId(tenantAccess.resolveCompanyId(expense.getCompanyId()));
        if (expense.getBranchId() == null) expense.setBranchId(1L);

        // Calc totalAmount
        expense.setTotalAmount(expense.getAmount().add(expense.getGstAmount()));

        Expense saved = expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_CREATED", "expenses", saved.getId(), null,
                "Registered expense voucher number: " + saved.getExpenseNumber());

        return saved;
    }

    @Transactional
    public Expense updateExpense(Long id, Expense details, String username) {
        Expense existing = getExpenseById(id);

        existing.setCategory(details.getCategory());
        existing.setVehicle(details.getVehicle());
        existing.setDriver(details.getDriver());
        existing.setTrip(details.getTrip());
        existing.setDescription(details.getDescription());
        existing.setAmount(details.getAmount());
        existing.setGstAmount(details.getGstAmount());
        existing.setTotalAmount(details.getAmount().add(details.getGstAmount()));
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
        Expense expense = getExpenseById(id);
        expense.setStatus("APPROVED");
        expense.setUpdatedBy(username);

        Expense saved = expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_APPROVED", "expenses", saved.getId(), null,
                "Approved expense voucher: " + saved.getExpenseNumber());

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
        expense.setIsDeleted(true);
        expense.setUpdatedBy(username);
        expenseRepository.save(expense);

        auditService.log(username, "EXPENSE_DELETED", "expenses", expense.getId(), null,
                "Soft deleted expense entry: " + expense.getExpenseNumber());
    }
}
