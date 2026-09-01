package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.SalesInvoiceDetail;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.model.Trip;
import com.transport.erp.model.TripDetail;
import com.transport.erp.model.BookingDetail;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.model.AppUser;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;


import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.service.ChartOfAccountService;
import com.transport.erp.service.JournalVoucherService;

@Service
public class SalesInvoiceService {

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private CustomerLedgerService ledgerService;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private JournalVoucherService jvService;

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
        invoice.setBranchId(tenantAccess.resolveBranchId(invoice.getBranchId()));


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

        // Automatically post double-entry General Ledger posting
        // Dr: 1100 Customer Receivables = netAmount (e.g. ₹11,800)
        // Cr: 4000 Transport Freight Income = subtotal (e.g. ₹10,000)
        // Cr: 2200 GST Liability = tax amount (e.g. ₹1,800)
        try {
            ChartOfAccount arAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1100", "Customer Receivables", "ASSET");
            ChartOfAccount incomeAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "4000", "Transport Freight Income", "INCOME");
            
            BigDecimal subtotal = saved.getSubtotal() != null ? saved.getSubtotal() : saved.getNetAmount();
            if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
                JournalVoucher jvIncome = new JournalVoucher();
                jvIncome.setVoucherNumber("JV-INV-" + saved.getId() + "-INC");
                jvIncome.setVoucherDate(LocalDate.now());
                jvIncome.setDebitAccount(arAcc);
                jvIncome.setCreditAccount(incomeAcc);
                jvIncome.setAmount(subtotal);
                jvIncome.setReferenceNumber(saved.getInvoiceNumber());
                jvIncome.setDescription("Auto-posted sales invoice revenue JV for " + saved.getInvoiceNumber());
                jvIncome.setCompanyId(saved.getCompanyId());
                jvIncome.setBranchId(saved.getBranchId());
                jvIncome.setIsDeleted(false);
                jvIncome.setCreatedBy(username);
                jvIncome.setUpdatedBy(username);
                jvIncome.setCode(jvIncome.getVoucherNumber());
                jvIncome.setName("Sales Invoice Revenue Voucher");
                jvService.createVoucher(jvIncome, username);
            }

            BigDecimal gstAmount = saved.getNetAmount().subtract(subtotal);
            if (gstAmount.compareTo(BigDecimal.ZERO) > 0) {
                ChartOfAccount gstAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "2200", "GST Liability", "LIABILITY");
                JournalVoucher jvGst = new JournalVoucher();
                jvGst.setVoucherNumber("JV-INV-" + saved.getId() + "-GST");
                jvGst.setVoucherDate(LocalDate.now());
                jvGst.setDebitAccount(arAcc);
                jvGst.setCreditAccount(gstAcc);
                jvGst.setAmount(gstAmount);
                jvGst.setReferenceNumber(saved.getInvoiceNumber());
                jvGst.setDescription("Auto-posted sales invoice GST liability JV for " + saved.getInvoiceNumber());
                jvGst.setCompanyId(saved.getCompanyId());
                jvGst.setBranchId(saved.getBranchId());
                jvGst.setIsDeleted(false);
                jvGst.setCreatedBy(username);
                jvGst.setUpdatedBy(username);
                jvGst.setCode(jvGst.getVoucherNumber());
                jvGst.setName("Sales Invoice GST Liability Voucher");
                jvService.createVoucher(jvGst, username);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(SalesInvoiceService.class).warn("Failed to create automatic GL posting for invoice " + saved.getInvoiceNumber() + ": " + e.getMessage(), e);
        }

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

    @Transactional
    public SalesInvoice createInvoiceFromTrip(Long tripId, String username) {
        // Concurrency duplicate prevention: lock the Trip record.
        Trip trip = tripRepository.findAndLockById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found or deleted with ID: " + tripId));

        tenantAccess.assertOwned(trip.getCompanyId());

        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && trip.getBranchId() != null && !currentUser.getBranchId().equals(trip.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Trip belongs to another branch.");
            }
        }

        if (!"COMPLETED".equals(trip.getStatus())) {

            throw new IllegalArgumentException("Trip is not completed. Current status: " + trip.getStatus());
        }

        // Duplicate billing validation
        List<SalesInvoice> existingInvoices = invoiceRepository.findInvoicesByTripId(tripId);
        if (existingInvoices != null && !existingInvoices.isEmpty()) {
            throw new IllegalArgumentException("This trip has already been invoiced under invoice number: " 
                    + existingInvoices.get(0).getInvoiceNumber());
        }

        if (trip.getBooking() == null) {
            throw new IllegalArgumentException("Booking is missing for this trip.");
        }
        if (trip.getBooking().getCustomer() == null) {
            throw new IllegalArgumentException("Customer is missing from trip.");
        }
        if (trip.getDetails() == null || trip.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Billing information is incomplete. Trip has no payload details.");
        }

        SalesInvoice invoice = new SalesInvoice();
        invoice.setCustomer(trip.getBooking().getCustomer());
        invoice.setCompanyId(trip.getCompanyId());
        invoice.setBranchId(tenantAccess.resolveBranchId(trip.getBranchId()));

        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setStatus("DRAFT");
        invoice.setPaymentTerms("NET_30");
        invoice.setDetails(new ArrayList<>());

        for (TripDetail td : trip.getDetails()) {
            if (td.getMaterial() == null) {
                throw new IllegalArgumentException("Material information is missing from trip details.");
            }
            if (td.getQuantity() == null || td.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity is invalid for material: " + td.getMaterial().getName());
            }
            if (td.getRate() == null || td.getRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Rate/freight amount is invalid for material: " + td.getMaterial().getName());
            }

            // Find matching BookingDetail to fetch transport/freight rate and tax settings
            BookingDetail bookingDetail = trip.getBooking().getDetails().stream()
                    .filter(bd -> bd.getMaterial() != null && bd.getMaterial().getId().equals(td.getMaterial().getId()))
                    .findFirst()
                    .orElse(null);

            if (bookingDetail == null) {
                throw new IllegalArgumentException("Booking detail is missing for material: " + td.getMaterial().getName());
            }
            if (bookingDetail.getGstPercentage() == null) {
                throw new IllegalArgumentException("GST configuration is missing for this booking.");
            }
            if (bookingDetail.getTransportRate() == null) {
                throw new IllegalArgumentException("Transport rate is missing for this booking detail.");
            }

            SalesInvoiceDetail detail = new SalesInvoiceDetail();
            detail.setTrip(trip);
            detail.setMaterial(td.getMaterial());
            detail.setQuantity(td.getQuantity());
            detail.setRate(td.getRate()); // Base rate
            detail.setLoadingCharges(td.getLoadingCharges() != null ? td.getLoadingCharges() : BigDecimal.ZERO);
            detail.setRoyalty(td.getRoyalty() != null ? td.getRoyalty() : BigDecimal.ZERO);
            detail.setFreightCharges(bookingDetail.getTransportRate());
            detail.setGstPercentage(bookingDetail.getGstPercentage());

            invoice.getDetails().add(detail);
        }

        // Delegate to existing createInvoice to reuse calculation logic, save record, and write logs.
        return createInvoice(invoice, username);
    }

    public List<SalesInvoice> getOutstandingInvoices(Long customerId, Long companyId, Long branchId) {
        return invoiceRepository.findOutstandingInvoices(customerId, companyId, branchId);
    }
}


