package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.JournalVoucher;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.repository.ChartOfAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class JournalVoucherService {

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private JournalVoucherRepository voucherRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private ChartOfAccountRepository accountRepository;




    @Autowired
    private AuditService auditService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;

    public Page<JournalVoucher> getVouchers(Long companyId, Pageable pageable) {
        return voucherRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    @Transactional
    public JournalVoucher createVoucher(JournalVoucher voucher, String username) {
        if (voucher.getVoucherDate() == null) {
            voucher.setVoucherDate(LocalDate.now());
        }
        voucher.setVoucherNumber(documentNumberService.next(tenantAccess.resolveCompanyId(voucher.getCompanyId()),
                DocumentNumberService.JOURNAL_VOUCHER, "JV-", voucher.getVoucherDate()));
        voucher.setIsDeleted(false);
        voucher.setCreatedBy(username);
        voucher.setUpdatedBy(username);

        voucher.setCompanyId(tenantAccess.resolveCompanyId(voucher.getCompanyId()));
        voucher.setBranchId(tenantAccess.resolveBranchId(voucher.getBranchId()));

        // Validate Financial Year period status (must be OPEN and cover voucherDate)
        periodValidationService.validatePostingAllowed(voucher.getCompanyId(), voucher.getVoucherDate());


        if (voucher.getCode() == null) voucher.setCode(voucher.getVoucherNumber());
        if (voucher.getName() == null) voucher.setName("Journal Voucher Entry");

        if (voucher.getAmount() == null || voucher.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Journal voucher amount must be greater than zero.");
        }

        if (voucher.getDebitAccount() == null || voucher.getDebitAccount().getId() == null) {
            throw new IllegalArgumentException("Debit Account must be specified");
        }
        if (voucher.getCreditAccount() == null || voucher.getCreditAccount().getId() == null) {
            throw new IllegalArgumentException("Credit Account must be specified");
        }

        if (voucher.getDebitAccount().getId().equals(voucher.getCreditAccount().getId())) {
            throw new IllegalArgumentException("Debit account and credit account cannot be the same account.");
        }

        // Load debit and credit accounts — must belong to caller's company
        ChartOfAccount debitAcc = accountRepository.findById(voucher.getDebitAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Debit Account not found"));
        tenantAccess.assertOwned(debitAcc.getCompanyId());

        ChartOfAccount creditAcc = accountRepository.findById(voucher.getCreditAccount().getId())
                .orElseThrow(() -> new IllegalArgumentException("Credit Account not found"));
        tenantAccess.assertOwned(creditAcc.getCompanyId());

        // Update running balances (ASSET/EXPENSE increase on Debit, decrease on Credit; LIABILITY/EQUITY/INCOME increase on Credit, decrease on Debit)
        if ("ASSET".equalsIgnoreCase(debitAcc.getAccountType()) || "EXPENSE".equalsIgnoreCase(debitAcc.getAccountType())) {
            debitAcc.setRunningBalance(debitAcc.getRunningBalance().add(voucher.getAmount()));
        } else {
            debitAcc.setRunningBalance(debitAcc.getRunningBalance().subtract(voucher.getAmount()));
        }

        if ("ASSET".equalsIgnoreCase(creditAcc.getAccountType()) || "EXPENSE".equalsIgnoreCase(creditAcc.getAccountType())) {
            creditAcc.setRunningBalance(creditAcc.getRunningBalance().subtract(voucher.getAmount()));
        } else {
            creditAcc.setRunningBalance(creditAcc.getRunningBalance().add(voucher.getAmount()));
        }

        accountRepository.save(debitAcc);
        accountRepository.save(creditAcc);

        voucher.setDebitAccount(debitAcc);
        voucher.setCreditAccount(creditAcc);

        JournalVoucher saved = voucherRepository.save(voucher);

        auditService.log(username, "VOUCHER_CREATED", "journal_vouchers", saved.getId(), null,
                "Posted double-entry journal voucher number: " + saved.getVoucherNumber());

        return saved;
    }
}
