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

import com.transport.erp.model.SalesInvoice;

import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.dto.CustomerLedgerPrintDTO;
import com.transport.erp.model.Company;
import com.transport.erp.model.Branch;
import java.util.ArrayList;

@Service
public class CustomerLedgerService {

    @Autowired
    private CustomerLedgerRepository ledgerRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BranchRepository branchRepository;

    public List<CustomerLedger> getLedgerByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());
        return ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(customerId);
    }

    @Transactional
    public void postToLedger(Long customerId, CustomerReceipt receipt, BigDecimal debit, BigDecimal credit, String username) {
        postToLedger(customerId, receipt, null, debit, credit, null, username, null);
    }

    @Transactional
    public void postToLedger(Long customerId, CustomerReceipt receipt, BigDecimal debit, BigDecimal credit, String remarks, String username) {
        postToLedger(customerId, receipt, null, debit, credit, remarks, username, null);
    }

    @Transactional
    public void postToLedger(Long customerId, CustomerReceipt receipt, BigDecimal debit, BigDecimal credit, String remarks, String username, Long explicitBranchId) {
        postToLedger(customerId, receipt, null, debit, credit, remarks, username, explicitBranchId);
    }

    @Transactional
    public void postToLedger(Long customerId, CustomerReceipt receipt, SalesInvoice invoice, BigDecimal debit, BigDecimal credit, String remarks, String username, Long explicitBranchId) {
        // Concurrency protection: Lock customer row before calculating running balance
        Customer customer = customerRepository.findAndLockById(customerId)
                .orElseGet(() -> customerRepository.findById(customerId)
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId)));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());

        // Get current running balance
        List<CustomerLedger> existing = ledgerRepository.findByCustomerIdAndIsDeletedFalseOrderByIdAsc(customerId);
        BigDecimal currentBal = existing.isEmpty() ? BigDecimal.ZERO : existing.get(existing.size() - 1).getRunningBalance();

        // New Running Balance = currentBal + debit - credit (receipts reduce customer outstanding credit)
        BigDecimal newBal = currentBal.add(debit).subtract(credit);

        CustomerLedger entry = new CustomerLedger();
        entry.setCustomer(customer);
        entry.setReceipt(receipt);
        entry.setInvoice(invoice);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setRunningBalance(newBal);
        entry.setRemarks(remarks != null ? remarks : (receipt != null ? "Payment received via receipt " + receipt.getReceiptNumber() : (invoice != null ? "Sales invoice entry " + invoice.getInvoiceNumber() : "Manual ledger adjustment")));
        entry.setIsDeleted(false);

        entry.setCreatedBy(username);
        entry.setUpdatedBy(username);
        entry.setCompanyId(tenantAccess.resolveCompanyId(customer.getCompanyId()));

        Long targetBranchId = explicitBranchId;
        if (targetBranchId == null && receipt != null) {
            targetBranchId = receipt.getBranchId();
        }
        if (targetBranchId == null && invoice != null) {
            targetBranchId = invoice.getBranchId();
        }
        if (targetBranchId == null && customer.getBranchId() != null) {
            targetBranchId = customer.getBranchId();
        }
        if (targetBranchId == null) {
            try {
                targetBranchId = tenantAccess.resolveBranchId(null);
            } catch (Exception e) {
                targetBranchId = customer.getCompanyId();
            }
        }
        entry.setBranchId(targetBranchId);

        entry.setCode("LEDG_" + customerId + "_" + System.currentTimeMillis());
        entry.setName("Customer Ledger Entry");

        ledgerRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public CustomerLedgerPrintDTO getLedgerPrintData(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        tenantAccess.assertCompanyAccess(customer.getCompanyId());

        com.transport.erp.model.AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null
                && customer.getBranchId() != null && !currentUser.getBranchId().equals(customer.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Customer belongs to another branch.");
        }

        List<CustomerLedger> entries = getLedgerByCustomer(customerId);

        CustomerLedgerPrintDTO dto = new CustomerLedgerPrintDTO();
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getName());
        dto.setCustomerCode(customer.getCode());
        dto.setCustomerAddress(customer.getAddress());
        dto.setCustomerPhone(customer.getPhone());
        dto.setCustomerEmail(customer.getEmail());
        dto.setCustomerGSTIN(customer.getGstNumber());

        if (customer.getCompanyId() != null) {
            companyRepository.findById(customer.getCompanyId()).ifPresent(comp -> {
                dto.setCompanyId(comp.getId());
                dto.setCompanyName(comp.getName());
                dto.setCompanyAddress(comp.getAddress());
                dto.setCompanyPhone(comp.getPhone());
                dto.setCompanyEmail(comp.getEmail());
                dto.setCompanyGSTIN(comp.getGstNumber());
                dto.setCompanyPAN(comp.getPanNumber());
            });
        }

        if (customer.getBranchId() != null) {
            branchRepository.findById(customer.getBranchId()).ifPresent(br -> {
                dto.setBranchId(br.getId());
                dto.setBranchName(br.getName());
                dto.setBranchAddress(br.getAddress());
                dto.setBranchPhone(br.getPhone());
            });
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<CustomerLedgerPrintDTO.CustomerLedgerItemDTO> items = new ArrayList<>();

        for (CustomerLedger entry : entries) {
            CustomerLedgerPrintDTO.CustomerLedgerItemDTO item = new CustomerLedgerPrintDTO.CustomerLedgerItemDTO();
            item.setLedgerId(entry.getId());
            item.setTransactionDate(entry.getCreatedDate());

            if (entry.getInvoice() != null) {
                item.setTransactionType("Sales Invoice");
                item.setReferenceNumber(entry.getInvoice().getInvoiceNumber());
            } else if (entry.getReceipt() != null) {
                item.setTransactionType("Customer Receipt");
                item.setReferenceNumber(entry.getReceipt().getReceiptNumber());
            } else {
                item.setTransactionType("Ledger Adjustment");
                item.setReferenceNumber(entry.getCode() != null ? entry.getCode() : "LEDG-" + entry.getId());
            }

            item.setRemarks(entry.getRemarks() != null ? entry.getRemarks() : "");
            item.setDebitAmount(entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO);
            item.setCreditAmount(entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO);
            item.setRunningBalance(entry.getRunningBalance() != null ? entry.getRunningBalance() : BigDecimal.ZERO);

            totalDebit = totalDebit.add(item.getDebitAmount());
            totalCredit = totalCredit.add(item.getCreditAmount());
            items.add(item);
        }

        dto.setOpeningBalance(BigDecimal.ZERO);
        dto.setTotalDebit(totalDebit);
        dto.setTotalCredit(totalCredit);
        dto.setClosingBalance(entries.isEmpty() ? BigDecimal.ZERO : entries.get(entries.size() - 1).getRunningBalance());
        dto.setItems(items);

        return dto;
    }

    @Transactional(readOnly = true)
    public void generateLedgerPdf(Long customerId, java.io.OutputStream os) throws Exception {
        CustomerLedgerPrintDTO data = getLedgerPrintData(customerId);
        com.transport.erp.util.CustomerLedgerPdfGenerator.generateCustomerLedgerPdf(data, os);
    }

    @Transactional(readOnly = true)
    public String generateLedgerCsv(Long customerId) {
        CustomerLedgerPrintDTO dto = getLedgerPrintData(customerId);
        StringBuilder sb = new StringBuilder();

        sb.append("Customer Ledger Statement for ").append(csv(dto.getCustomerName()))
                .append(" (Code: ").append(csv(dto.getCustomerCode())).append(")\n");
        sb.append("Company: ").append(csv(dto.getCompanyName())).append("\n");
        sb.append("Opening Balance: ").append(dto.getOpeningBalance()).append("\n");
        sb.append("Total Debit: ").append(dto.getTotalDebit()).append("\n");
        sb.append("Total Credit: ").append(dto.getTotalCredit()).append("\n");
        sb.append("Closing Balance: ").append(dto.getClosingBalance()).append("\n\n");

        sb.append("Date & Time,Transaction Type,Reference Number,Remarks / Description,Debit,Credit,Running Balance\n");

        for (CustomerLedgerPrintDTO.CustomerLedgerItemDTO item : dto.getItems()) {
            sb.append(csv(item.getTransactionDate() != null ? item.getTransactionDate().toString() : "")).append(',')
                    .append(csv(item.getTransactionType())).append(',')
                    .append(csv(item.getReferenceNumber())).append(',')
                    .append(csv(item.getRemarks())).append(',')
                    .append(item.getDebitAmount()).append(',')
                    .append(item.getCreditAmount()).append(',')
                    .append(item.getRunningBalance()).append('\n');
        }

        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null || "null".equals(value)) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
