package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.CustomerReceipt;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.CustomerReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CustomerReceiptService {

    @Autowired
    private CustomerReceiptRepository receiptRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private CustomerLedgerService ledgerService;




    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




    public Page<CustomerReceipt> getReceipts(Long companyId, Pageable pageable) {
        return receiptRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public CustomerReceipt getReceiptById(Long id) {
        CustomerReceipt receipt = receiptRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + id));
        tenantAccess.assertOwned(receipt.getCompanyId());
        return receipt;
    }

    @Transactional
    public CustomerReceipt createReceipt(CustomerReceipt receipt, String username) {
        String prefix = settingService.getByKey("PREFIX_RECEIPT").map(s -> s.getValueData()).orElse("RCT-");
        receipt.setReceiptNumber(prefix + System.currentTimeMillis());
        receipt.setReceiptDate(LocalDate.now());
        receipt.setIsDeleted(false);
        receipt.setCreatedBy(username);
        receipt.setUpdatedBy(username);

        receipt.setCompanyId(tenantAccess.resolveCompanyId(receipt.getCompanyId()));
        if (receipt.getBranchId() == null) receipt.setBranchId(1L);

        CustomerReceipt saved = receiptRepository.save(receipt);

        // Automatically post credit update to customer ledger (receipt reduces outstanding customer liability)
        ledgerService.postToLedger(saved.getCustomer().getId(), saved, BigDecimal.ZERO, saved.getAmountReceived(), username);

        auditService.log(username, "RECEIPT_CREATED", "customer_receipts", saved.getId(), null,
                "Registered customer receipt voucher: " + saved.getReceiptNumber());

        return saved;
    }

    @Transactional
    public CustomerReceipt updateReceipt(Long id, CustomerReceipt details, String username) {
        CustomerReceipt existing = getReceiptById(id);

        existing.setAmountReceived(details.getAmountReceived());
        existing.setAdvanceAmount(details.getAdvanceAmount());
        existing.setPaymentMethod(details.getPaymentMethod());
        existing.setReferenceNumber(details.getReferenceNumber());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        CustomerReceipt saved = receiptRepository.save(existing);

        auditService.log(username, "RECEIPT_UPDATED", "customer_receipts", saved.getId(), null,
                "Updated details for receipt voucher: " + saved.getReceiptNumber());

        return saved;
    }

    @Transactional
    public void deleteReceipt(Long id, String username) {
        CustomerReceipt receipt = getReceiptById(id);
        receipt.setIsDeleted(true);
        receipt.setUpdatedBy(username);
        receiptRepository.save(receipt);

        auditService.log(username, "RECEIPT_DELETED", "customer_receipts", receipt.getId(), null,
                "Soft deleted receipt voucher: " + receipt.getReceiptNumber());
    }
}
