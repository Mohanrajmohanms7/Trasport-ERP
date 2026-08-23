package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.CustomerLedger;
import com.transport.erp.model.CustomerReceipt;
import com.transport.erp.model.Customer;
import com.transport.erp.repository.CustomerLedgerRepository;
import com.transport.erp.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomerLedgerService {

    @Autowired
    private CustomerLedgerRepository ledgerRepository;

    @Autowired
    private TenantAccessService tenantAccess;



    @Autowired
    private CustomerRepository customerRepository;



    public List<CustomerLedger> getLedgerByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());
        return ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(customerId);
    }

    @Transactional
    public void postToLedger(Long customerId, CustomerReceipt receipt, BigDecimal debit, BigDecimal credit, String username) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());

        // Get current running balance
        List<CustomerLedger> existing = ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(customerId);
        BigDecimal currentBal = existing.isEmpty() ? BigDecimal.ZERO : existing.get(existing.size() - 1).getRunningBalance();

        // New Running Balance = currentBal + debit - credit (receipts reduce customer outstanding credit)
        BigDecimal newBal = currentBal.add(debit).subtract(credit);

        CustomerLedger entry = new CustomerLedger();
        entry.setCustomer(customer);
        entry.setReceipt(receipt);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setRunningBalance(newBal);
        entry.setRemarks(receipt != null ? "Payment received via receipt " + receipt.getReceiptNumber() : "Manual ledger adjustment");
        entry.setIsDeleted(false);
        entry.setCreatedBy(username);
        entry.setUpdatedBy(username);
        entry.setCompanyId(tenantAccess.resolveCompanyId(customer.getCompanyId()));
        entry.setBranchId(customer.getBranchId() != null ? customer.getBranchId() : 1L);
        entry.setCode("LEDG_" + customerId + "_" + System.currentTimeMillis());
        entry.setName("Customer Ledger Entry");

        ledgerRepository.save(entry);
    }
}
