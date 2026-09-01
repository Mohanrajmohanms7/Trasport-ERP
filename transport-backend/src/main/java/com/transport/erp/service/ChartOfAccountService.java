package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.repository.ChartOfAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChartOfAccountService {

    @Autowired
    private ChartOfAccountRepository accountRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    @Transactional
    public Page<ChartOfAccount> getAccounts(Long companyId, Pageable pageable) {
        return accountRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    @Transactional
    public ChartOfAccount getOrCreateAccount(Long companyId, Long branchId, String accountCode, String accountName, String accountType) {
        return accountRepository.findByCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, accountCode)
                .orElseGet(() -> {
                    ChartOfAccount coa = new ChartOfAccount();
                    coa.setAccountCode(accountCode);
                    coa.setAccountName(accountName);
                    coa.setAccountType(accountType);
                    coa.setOpeningBalance(java.math.BigDecimal.ZERO);
                    coa.setRunningBalance(java.math.BigDecimal.ZERO);
                    coa.setCode(accountCode);
                    coa.setName(accountName);
                    coa.setDescription(accountName);
                    coa.setStatus("ACTIVE");
                    coa.setCompanyId(companyId);
                    coa.setBranchId(branchId != null ? branchId : companyId);
                    coa.setIsDeleted(false);
                    coa.setCreatedBy("SYSTEM");
                    return accountRepository.save(coa);
                });
    }


    public ChartOfAccount getAccountById(Long id) {
        ChartOfAccount account = accountRepository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        tenantAccess.assertOwned(account.getCompanyId());
        return account;
    }

    @Transactional
    public ChartOfAccount createAccount(ChartOfAccount account, String username) {
        account.setRunningBalance(account.getOpeningBalance());
        account.setIsDeleted(false);
        account.setCreatedBy(username);
        account.setUpdatedBy(username);

        Long companyId = tenantAccess.resolveCompanyId(account.getCompanyId());
        account.setCompanyId(companyId);
        account.setBranchId(tenantAccess.resolveBranchId(account.getBranchId()));

        if (account.getCode() == null) account.setCode("ACC-" + System.currentTimeMillis());
        if (account.getName() == null) account.setName(account.getAccountName());
        if (account.getAccountCode() != null
                && accountRepository.findByCompanyIdAndAccountCodeAndIsDeletedFalse(companyId, account.getAccountCode()).isPresent()) {
            throw new IllegalArgumentException("Account code already exists in this company: " + account.getAccountCode());
        }

        ChartOfAccount saved = accountRepository.save(account);

        auditService.log(username, "ACCOUNT_CREATED", "chart_of_accounts", saved.getId(), null,
                "Registered Chart of Account: " + saved.getAccountName() + " (" + saved.getAccountCode() + ")");

        return saved;
    }

    @Transactional
    public ChartOfAccount updateAccount(Long id, ChartOfAccount details, String username) {
        ChartOfAccount existing = getAccountById(id);

        existing.setAccountName(details.getAccountName());
        existing.setAccountType(details.getAccountType());
        existing.setUpdatedBy(username);

        ChartOfAccount saved = accountRepository.save(existing);

        auditService.log(username, "ACCOUNT_UPDATED", "chart_of_accounts", saved.getId(), null,
                "Updated Chart of Account details: " + saved.getAccountCode());

        return saved;
    }

    @Transactional
    public void deleteAccount(Long id, String username) {
        ChartOfAccount account = getAccountById(id);
        account.setIsDeleted(true);
        account.setUpdatedBy(username);
        accountRepository.save(account);

        auditService.log(username, "ACCOUNT_DELETED", "chart_of_accounts", account.getId(), null,
                "Soft deleted account: " + account.getAccountCode());
    }
}
