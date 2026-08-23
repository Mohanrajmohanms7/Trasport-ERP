package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.SalesInvoiceDetail;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.SalesInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SalesInvoiceService {

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private CustomerLedgerService ledgerService;




    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




    public Page<SalesInvoice> getInvoices(Long companyId, String status, Pageable pageable) {
        if (status != null && !status.trim().isEmpty()) {
            return invoiceRepository.findByCompanyIdAndIsDeletedFalseAndStatus(companyId, status, pageable);
        }
        return invoiceRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public SalesInvoice getInvoiceById(Long id) {
        SalesInvoice invoice = invoiceRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        tenantAccess.assertOwned(invoice.getCompanyId());
        return invoice;
    }

    @Transactional
    public SalesInvoice createInvoice(SalesInvoice invoice, String username) {
        String prefix = settingService.getByKey("PREFIX_INVOICE").map(s -> s.getValueData()).orElse("INV-");
        String defaultStatus = settingService.getByKey("DEFAULT_INVOICE_STATUS").map(s -> s.getValueData()).orElse("DRAFT");
        
        invoice.setInvoiceNumber(prefix + System.currentTimeMillis());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(defaultStatus);
        invoice.setIsDeleted(false);
        invoice.setCreatedBy(username);
        invoice.setUpdatedBy(username);

        invoice.setCompanyId(tenantAccess.resolveCompanyId(invoice.getCompanyId()));
        if (invoice.getBranchId() == null) invoice.setBranchId(1L);

        BigDecimal subtotal = BigDecimal.ZERO;

        if (invoice.getDetails() != null) {
            for (SalesInvoiceDetail detail : invoice.getDetails()) {
                detail.setInvoice(invoice);
                detail.setIsDeleted(false);
                detail.setCreatedBy(username);
                detail.setUpdatedBy(username);
                detail.setCompanyId(invoice.getCompanyId());
                detail.setBranchId(invoice.getBranchId());

                // Calculate base amount = quantity * (rate + freight + loading + royalty)
                BigDecimal basePrice = detail.getRate()
                        .add(detail.getFreightCharges())
                        .add(detail.getLoadingCharges())
                        .add(detail.getRoyalty());
                BigDecimal lineSub = detail.getQuantity().multiply(basePrice);

                // Calculate GST components (default CGST = 9%, SGST = 9%)
                BigDecimal totalTaxRate = detail.getGstPercentage().divide(BigDecimal.valueOf(100));
                BigDecimal taxVal = lineSub.multiply(totalTaxRate);

                detail.setCgst(taxVal.divide(BigDecimal.valueOf(2)));
                detail.setSgst(taxVal.divide(BigDecimal.valueOf(2)));
                detail.setIgst(BigDecimal.ZERO);
                detail.setNetAmount(lineSub.add(taxVal));

                subtotal = subtotal.add(detail.getNetAmount());
            }
        }

        invoice.setSubtotal(subtotal);
        invoice.setNetAmount(subtotal.subtract(invoice.getDiscount()));

        SalesInvoice saved = invoiceRepository.save(invoice);

        auditService.log(username, "INVOICE_CREATED", "sales_invoices", saved.getId(), null,
                "Created invoice voucher: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public SalesInvoice updateInvoice(Long id, SalesInvoice details, String username) {
        SalesInvoice existing = getInvoiceById(id);

        existing.setDiscount(details.getDiscount());
        existing.setPaymentTerms(details.getPaymentTerms());
        existing.setUpdatedBy(username);

        existing.getDetails().clear();
        BigDecimal subtotal = BigDecimal.ZERO;

        if (details.getDetails() != null) {
            for (SalesInvoiceDetail d : details.getDetails()) {
                d.setInvoice(existing);
                d.setIsDeleted(false);
                d.setCreatedBy(username);
                d.setUpdatedBy(username);
                d.setCompanyId(existing.getCompanyId());
                d.setBranchId(existing.getBranchId());

                BigDecimal basePrice = d.getRate()
                        .add(d.getFreightCharges())
                        .add(d.getLoadingCharges())
                        .add(d.getRoyalty());
                BigDecimal lineSub = d.getQuantity().multiply(basePrice);

                BigDecimal totalTaxRate = d.getGstPercentage().divide(BigDecimal.valueOf(100));
                BigDecimal taxVal = lineSub.multiply(totalTaxRate);

                d.setCgst(taxVal.divide(BigDecimal.valueOf(2)));
                d.setSgst(taxVal.divide(BigDecimal.valueOf(2)));
                d.setIgst(BigDecimal.ZERO);
                d.setNetAmount(lineSub.add(taxVal));

                subtotal = subtotal.add(d.getNetAmount());
                existing.getDetails().add(d);
            }
        }

        existing.setSubtotal(subtotal);
        existing.setNetAmount(subtotal.subtract(existing.getDiscount()));

        SalesInvoice saved = invoiceRepository.save(existing);

        auditService.log(username, "INVOICE_UPDATED", "sales_invoices", saved.getId(), null,
                "Modified details for invoice voucher: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public SalesInvoice approveInvoice(Long id, String username) {
        SalesInvoice invoice = getInvoiceById(id);
        invoice.setStatus("APPROVED");
        invoice.setUpdatedBy(username);

        SalesInvoice saved = invoiceRepository.save(invoice);

        // Automatically post debit update to customer ledger (Invoice increases outstanding customer owed balance)
        ledgerService.postToLedger(saved.getCustomer().getId(), null, saved.getNetAmount(), BigDecimal.ZERO, username);

        auditService.log(username, "INVOICE_APPROVED", "sales_invoices", saved.getId(), null,
                "Approved invoice voucher & posted to ledger: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public SalesInvoice cancelInvoice(Long id, String username) {
        SalesInvoice invoice = getInvoiceById(id);
        invoice.setStatus("CANCELLED");
        invoice.setUpdatedBy(username);

        SalesInvoice saved = invoiceRepository.save(invoice);

        auditService.log(username, "INVOICE_CANCELLED", "sales_invoices", saved.getId(), null,
                "Cancelled invoice voucher: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public void deleteInvoice(Long id, String username) {
        SalesInvoice invoice = getInvoiceById(id);
        invoice.setIsDeleted(true);
        invoice.setUpdatedBy(username);
        invoiceRepository.save(invoice);

        auditService.log(username, "INVOICE_DELETED", "sales_invoices", invoice.getId(), null,
                "Soft deleted invoice voucher: " + invoice.getInvoiceNumber());
    }
}
