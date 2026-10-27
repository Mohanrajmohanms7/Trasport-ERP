package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;
import com.transport.erp.exception.BusinessValidationException;

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
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.service.ChartOfAccountService;
import com.transport.erp.service.JournalVoucherService;

import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.dto.SalesInvoicePrintDTO;
import com.transport.erp.model.Customer;
import com.transport.erp.model.Company;
import com.transport.erp.model.Branch;

@Service
public class SalesInvoiceService {

    /** Client billing basis (Platform Admin). May be absent in plain unit tests. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BillingQuantityPolicy billingQuantityPolicy;

    @org.springframework.beans.factory.annotation.Autowired
    private ApprovalPolicyService approvalPolicy;

    @Autowired
    private OrderUomService orderUomService;

    @Autowired
    private com.transport.erp.repository.MaterialRepository materialRepository;

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private com.transport.erp.security.TenantParentAccess tenantParentAccess;

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

    @Autowired
    private BusinessDependencyValidationService validationService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;






    public Page<SalesInvoice> getInvoices(Long companyId, String status, Pageable pageable) {
        return invoiceRepository.findForList(companyId, tenantAccess.listBranchScope(), (status == null || status.trim().isEmpty() ? null : status.trim()), pageable);
    }

    public SalesInvoice getInvoiceById(Long id) {
        SalesInvoice invoice = invoiceRepository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        tenantAccess.assertOwned(invoice.getCompanyId());
        tenantAccess.assertBranchVisible(invoice.getBranchId());
        return invoice;
    }

    @Transactional
    public SalesInvoice createInvoice(SalesInvoice invoice, String username) {
        String prefix = settingService.getByKey("PREFIX_INVOICE").map(s -> s.getValueData()).orElse("INV-");
        String defaultStatus = settingService.getByKey("DEFAULT_INVOICE_STATUS").map(s -> s.getValueData()).orElse("DRAFT");

        invoice.setCompanyId(tenantAccess.resolveCompanyId(invoice.getCompanyId()));
        invoice.setBranchId(tenantAccess.resolveBranchId(invoice.getBranchId()));

        if (invoice.getCustomer() == null || invoice.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required for an invoice.");
        }
        invoice.setCustomer(tenantParentAccess.requireCustomer(invoice.getCustomer().getId()));

        invoice.setInvoiceDate(resolveDocumentDate(invoice.getInvoiceDate(), "Invoice"));
        invoice.setInvoiceNumber(documentNumberService.next(invoice.getCompanyId(), DocumentNumberService.INVOICE,
                prefix, invoice.getInvoiceDate()));
        invoice.setStatus(defaultStatus);
        invoice.setIsDeleted(false);
        invoice.setCreatedBy(username);
        invoice.setUpdatedBy(username);
        if (invoice.getCode() == null) invoice.setCode(invoice.getInvoiceNumber());
        if (invoice.getName() == null) invoice.setName("Sales Invoice");

        if (invoice.getDetails() == null || invoice.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Add at least one billing line to the invoice.");
        }
        for (SalesInvoiceDetail detail : invoice.getDetails()) {
            detail.setInvoice(invoice);
            detail.setIsDeleted(false);
            detail.setCreatedBy(username);
            detail.setUpdatedBy(username);
            detail.setCompanyId(invoice.getCompanyId());
            detail.setBranchId(invoice.getBranchId());
            fillLineIdentity(detail);
        }

        validateTripLines(invoice, null);
        applyLineUnits(invoice, java.util.Map.of());
        recalculate(invoice);

        SalesInvoice saved = invoiceRepository.save(invoice);

        auditService.log(username, "INVOICE_CREATED", "sales_invoices", saved.getId(), null,
                "Created invoice voucher: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public SalesInvoice updateInvoice(Long id, SalesInvoice details, String username) {
        SalesInvoice existing = getInvoiceById(id);
        validationService.validateInvoiceUpdate(existing);

        LocalDate newDate = resolveDocumentDate(
                details.getInvoiceDate() != null ? details.getInvoiceDate() : existing.getInvoiceDate(), "Invoice");
        if (existing.getInvoiceDate() != null && !DocumentNumberService.financialYearLabel(newDate)
                .equals(DocumentNumberService.financialYearLabel(existing.getInvoiceDate()))) {
            throw new BusinessValidationException(
                    "Invoice Date Outside Financial Year",
                    "INVOICE_DATE_FY_CHANGE",
                    "Invoice number " + existing.getInvoiceNumber() + " belongs to another financial year than " + newDate + ".",
                    "Keep the date inside the same financial year, or delete this draft and create a new invoice.");
        }
        existing.setInvoiceDate(newDate);
        existing.setDiscount(details.getDiscount());
        existing.setPaymentTerms(details.getPaymentTerms());
        existing.setPlaceOfSupply(details.getPlaceOfSupply());
        existing.setUpdatedBy(username);

        java.util.Map<Long, com.transport.erp.model.UomMaster> previousUnits = new java.util.HashMap<>();
        for (SalesInvoiceDetail d : existing.getDetails()) {
            if (d.getMaterial() != null && d.getUom() != null) previousUnits.putIfAbsent(d.getMaterial().getId(), d.getUom());
        }
        existing.getDetails().clear();
        if (details.getDetails() == null || details.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Add at least one billing line to the invoice.");
        }
        for (SalesInvoiceDetail d : details.getDetails()) {
            d.setId(null);              // lines are replaced; ids sent by API clients must not be re-attached
            d.setVersion(null);
            d.setInvoice(existing);
            d.setIsDeleted(false);
            d.setCreatedBy(username);
            d.setUpdatedBy(username);
            d.setCompanyId(existing.getCompanyId());
            d.setBranchId(existing.getBranchId());
            fillLineIdentity(d);
            existing.getDetails().add(d);
        }

        validateTripLines(existing, existing.getId());
        applyLineUnits(existing, previousUnits);
        recalculate(existing);

        SalesInvoice saved = invoiceRepository.save(existing);

        auditService.log(username, "INVOICE_UPDATED", "sales_invoices", saved.getId(), null,
                "Modified details for invoice voucher: " + saved.getInvoiceNumber());

        return saved;
    }

    /** Recomputes GST, discount split and totals from the lines. */
    private void recalculate(SalesInvoice invoice) {
        String customerGstin = invoice.getCustomer() != null ? invoice.getCustomer().getGstNumber() : null;
        InvoiceTaxCalculator.apply(invoice, resolveSupplierStateCode(invoice), invoice.getPlaceOfSupply(), customerGstin);
    }

    /** State code of the billing branch GSTIN, else the company GSTIN. */
    private String resolveSupplierStateCode(SalesInvoice invoice) {
        if (invoice.getBranchId() != null) {
            String fromBranch = branchRepository.findById(invoice.getBranchId())
                    .map(b -> InvoiceTaxCalculator.stateCodeFromGstin(b.getGstNumber())).orElse(null);
            if (fromBranch != null) return fromBranch;
        }
        if (invoice.getCompanyId() != null) {
            return companyRepository.findById(invoice.getCompanyId())
                    .map(c -> InvoiceTaxCalculator.stateCodeFromGstin(c.getGstNumber())).orElse(null);
        }
        return null;
    }

    /** Defaults to today; future dates are rejected. */
    private LocalDate resolveDocumentDate(LocalDate requested, String label) {
        LocalDate date = requested != null ? requested : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessValidationException(
                    label + " Date In Future",
                    "DOCUMENT_DATE_IN_FUTURE",
                    label + " date " + date + " is in the future.",
                    "Use today's date or an earlier date.");
        }
        return date;
    }

    /**
     * Unit of each invoice line. A trip line is billed in the unit of that trip's line for the material; a manual line
     * uses the requested unit (switched on for the company, or the one it already had), else the material/company default.
     */
    private void applyLineUnits(SalesInvoice invoice, java.util.Map<Long, com.transport.erp.model.UomMaster> previousUnits) {
        OrderUomService.OrderUnits units = null;
        java.util.Map<Long, com.transport.erp.model.Material> materials = null;
        for (SalesInvoiceDetail d : invoice.getDetails()) {
            if (d.getMaterial() == null || d.getMaterial().getId() == null) continue;
            Long matId = d.getMaterial().getId();
            if (d.getTrip() != null && d.getTrip().getDetails() != null) {
                com.transport.erp.model.UomMaster tripUnit = d.getTrip().getDetails().stream()
                        .filter(td -> td.getMaterial() != null && matId.equals(td.getMaterial().getId()) && td.getUom() != null)
                        .map(TripDetail::getUom).findFirst().orElse(null);
                if (tripUnit != null) {
                    d.setUom(tripUnit);
                    continue;
                }
            }
            if (units == null) {
                units = orderUomService.forCompany(invoice.getCompanyId());
                java.util.Set<Long> ids = new java.util.HashSet<>();
                for (SalesInvoiceDetail x : invoice.getDetails()) {
                    if (x.getMaterial() != null && x.getMaterial().getId() != null) ids.add(x.getMaterial().getId());
                }
                materials = new java.util.HashMap<>();
                for (com.transport.erp.model.Material m : materialRepository.findAllById(ids)) materials.put(m.getId(), m);
            }
            com.transport.erp.model.Material m = materials.get(matId);
            String name = m != null && m.getName() != null ? m.getName() : "material ID " + matId;
            d.setUom(units.resolve(d.getUom(), m, previousUnits.get(matId), name));
        }
    }

    private void fillLineIdentity(SalesInvoiceDetail d) {
        if (d.getCode() == null) d.setCode("INV-LINE");
        if (d.getName() == null) d.setName("Invoice Line");
        if (d.getTrip() != null && d.getTrip().getId() == null) d.setTrip(null);
    }

    /**
     * Trip lines must point to completed trips of the same customer and company,
     * dated on or before the invoice, and not billed on another active invoice.
     * The trips are row-locked so two invoices cannot bill the same trip at once.
     */
    private void validateTripLines(SalesInvoice invoice, Long excludeInvoiceId) {
        java.util.Set<Long> tripIds = new java.util.LinkedHashSet<>();
        for (SalesInvoiceDetail d : invoice.getDetails()) {
            if (d.getTrip() != null && d.getTrip().getId() != null) tripIds.add(d.getTrip().getId());
        }
        if (tripIds.isEmpty()) return;

        java.util.Map<Long, Trip> trips = new java.util.HashMap<>();
        for (Trip t : tripRepository.findAndLockAllByIds(tripIds)) trips.put(t.getId(), t);
        // A trip is invoiced once: all of its materials must be on this invoice (otherwise the rest could never be billed).
        java.util.Map<Long, java.util.Set<Long>> onInvoice = new java.util.HashMap<>();
        for (SalesInvoiceDetail d : invoice.getDetails()) {
            if (d.getTrip() != null && d.getTrip().getId() != null && d.getMaterial() != null && d.getMaterial().getId() != null) {
                onInvoice.computeIfAbsent(d.getTrip().getId(), k -> new java.util.HashSet<>()).add(d.getMaterial().getId());
            }
        }
        for (Trip t : trips.values()) {
            if (t.getDetails() == null) continue;
            java.util.List<String> missing = t.getDetails().stream()
                    .filter(td -> td.getMaterial() != null && !onInvoice.getOrDefault(t.getId(), java.util.Set.of()).contains(td.getMaterial().getId()))
                    .map(td -> td.getMaterial().getName()).distinct().toList();
            if (!missing.isEmpty()) {
                throw new BusinessValidationException("Trip Not Complete On Invoice", "INVOICE_TRIP_PARTIAL",
                        "Trip " + t.getTripNumber() + " also carried " + String.join(", ", missing) + ", which is not on this invoice.",
                        "A trip is invoiced once — add all of its materials, or remove the trip from this invoice.");
            }
        }
        // The delivered quantity on each trip line is taken from the trip itself (never from the request), so an
        // invoice billed "as ordered" always shows the real delivered quantity (V85).
        String basisNow = billingQuantityPolicy == null ? BillingQuantityPolicy.DELIVERED : billingQuantityPolicy.basis(invoice.getCompanyId());
        for (SalesInvoiceDetail d : invoice.getDetails()) {
            if (d.getTrip() == null || d.getTrip().getId() == null || d.getMaterial() == null) continue;
            Trip tr = trips.get(d.getTrip().getId());
            if (tr == null || tr.getDetails() == null) continue;
            tr.getDetails().stream().filter(td -> td.getMaterial() != null && td.getMaterial().getId().equals(d.getMaterial().getId()))
                    .findFirst().ifPresent(td -> d.setDeliveredQuantity(td.getDeliveredQuantity()));
            if (d.getQuantityBasis() == null) d.setQuantityBasis(basisNow);
        }

        List<Long> alreadyBilled = invoiceRepository.findTripIdsOnActiveInvoices(tripIds,
                excludeInvoiceId != null ? excludeInvoiceId : -1L);

        for (Long tripId : tripIds) {
            Trip trip = trips.get(tripId);
            if (trip == null) {
                throw new IllegalArgumentException("Trip not found or deleted with ID: " + tripId);
            }
            tenantAccess.assertOwned(trip.getCompanyId());
            if (!"COMPLETED".equals(trip.getStatus())) {
                throw new BusinessValidationException("Trip Not Completed", "INVOICE_TRIP_NOT_COMPLETED",
                        "Trip " + trip.getTripNumber() + " is " + trip.getStatus() + " and cannot be billed yet.",
                        "Complete the trip before adding it to an invoice.");
            }
            if (alreadyBilled.contains(tripId)) {
                throw new BusinessValidationException("Trip Already Invoiced", "INVOICE_TRIP_ALREADY_BILLED",
                        "Trip " + trip.getTripNumber() + " is already on another active invoice.",
                        "Remove the trip from this invoice, or cancel the other invoice first.");
            }
            Long tripCustomerId = trip.getBooking() != null && trip.getBooking().getCustomer() != null
                    ? trip.getBooking().getCustomer().getId() : null;
            if (tripCustomerId != null && invoice.getCustomer() != null
                    && !tripCustomerId.equals(invoice.getCustomer().getId())) {
                throw new BusinessValidationException("Trip Customer Mismatch", "INVOICE_TRIP_CUSTOMER_MISMATCH",
                        "Trip " + trip.getTripNumber() + " belongs to a different customer.",
                        "Bill each trip to the customer on its booking.");
            }
            if (trip.getTripDate() != null && invoice.getInvoiceDate() != null
                    && invoice.getInvoiceDate().isBefore(trip.getTripDate())) {
                throw new BusinessValidationException("Invoice Before Trip", "INVOICE_DATE_BEFORE_TRIP",
                        "Invoice date " + invoice.getInvoiceDate() + " is before trip " + trip.getTripNumber()
                                + " date " + trip.getTripDate() + ".",
                        "Use an invoice date on or after the trip date.");
            }
        }
        for (SalesInvoiceDetail d : invoice.getDetails()) {
            if (d.getTrip() != null && d.getTrip().getId() != null) d.setTrip(trips.get(d.getTrip().getId()));
        }
    }

    @Transactional
    public SalesInvoice approveInvoice(Long id, String username) {
        SalesInvoice invoice = invoiceRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + id));

        tenantAccess.assertOwned(invoice.getCompanyId());
        approvalPolicy.assertDifferentApprover(invoice.getCompanyId(), invoice.getCreatedBy(), username, "Invoice " + invoice.getInvoiceNumber());
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && !tenantAccess.isCompanyWideAdmin(currentUser) && invoice.getBranchId() != null && !currentUser.getBranchId().equals(invoice.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Invoice belongs to another branch.");
            }
        }

        if ("APPROVED".equalsIgnoreCase(invoice.getStatus())) {
            List<String> details = new java.util.ArrayList<>();
            details.add(String.format("Invoice '%s' is already APPROVED.", invoice.getInvoiceNumber()));
            throw new com.transport.erp.exception.BusinessValidationException(
                    "Invoice Already Approved",
                    "INVOICE_ALREADY_APPROVED",
                    String.format("Invoice '%s' is already APPROVED.", invoice.getInvoiceNumber()),
                    "No further approval action can be taken on an approved invoice.",
                    details
            );
        }
        if (!"DRAFT".equalsIgnoreCase(invoice.getStatus())) {
            List<String> details = new java.util.ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be approved because its current status is %s.", invoice.getInvoiceNumber(), invoice.getStatus()));
            throw new com.transport.erp.exception.BusinessValidationException(
                    "Invoice Approval Blocked",
                    "INVOICE_STATUS_APPROVAL_BLOCKED",
                    String.format("Invoice '%s' cannot be approved because its current status is %s.", invoice.getInvoiceNumber(), invoice.getStatus()),
                    "Only DRAFT invoices can be approved.",
                    details
            );
        }

        // Validate Financial Year period status
        LocalDate invoicePostingDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        periodValidationService.validatePostingAllowed(invoice.getCompanyId(), invoicePostingDate);


        List<JournalVoucher> existingJvs = jvRepository.findByCompanyIdAndReferenceNumberAndIsDeletedFalse(invoice.getCompanyId(), invoice.getInvoiceNumber());
        if (existingJvs != null && !existingJvs.isEmpty()) {
            List<String> details = new java.util.ArrayList<>();
            details.add(String.format("Accounting vouchers already exist for invoice '%s'.", invoice.getInvoiceNumber()));
            throw new com.transport.erp.exception.BusinessValidationException(
                    "Duplicate Accounting Blocked",
                    "INVOICE_ACCOUNTING_EXISTS",
                    String.format("Accounting entry already exists for invoice '%s'.", invoice.getInvoiceNumber()),
                    "Invoice accounting has already been posted.",
                    details
            );
        }

        // Recompute with the current GST rules so drafts saved before a rule change post correctly.
        recalculate(invoice);

        invoice.setStatus("APPROVED");
        invoice.setUpdatedBy(username);

        SalesInvoice saved = invoiceRepository.save(invoice);

        // Automatically post debit update to customer ledger (Invoice increases outstanding customer owed balance)
        ledgerService.postToLedger(
                saved.getCustomer().getId(),
                null,
                saved,
                saved.getNetAmount(),
                BigDecimal.ZERO,
                "Sales invoice approval for " + saved.getInvoiceNumber(),
                username,
                saved.getBranchId()
        );

        // Automatically post double-entry General Ledger posting (Exceptions propagate without try-catch to ensure atomicity!)
        ChartOfAccount arAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1100", "Customer Receivables", "ASSET");
        ChartOfAccount incomeAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "4000", "Transport Freight Income", "INCOME");

        // Revenue = taxable value (after discount, before GST). GST goes to the liability account.
        BigDecimal taxableValue = saved.getTaxableAmount() != null ? saved.getTaxableAmount() : BigDecimal.ZERO;
        BigDecimal gstAmount = saved.getTaxAmount() != null ? saved.getTaxAmount() : BigDecimal.ZERO;
        if (taxableValue.compareTo(BigDecimal.ZERO) > 0) {
            JournalVoucher jvIncome = new JournalVoucher();
            jvIncome.setVoucherNumber("JV-INV-" + saved.getId() + "-INC");
            jvIncome.setVoucherDate(invoicePostingDate);
            jvIncome.setDebitAccount(arAcc);
            jvIncome.setCreditAccount(incomeAcc);
            jvIncome.setAmount(taxableValue);
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

        if (gstAmount.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount gstAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "2200", "GST Liability", "LIABILITY");
            JournalVoucher jvGst = new JournalVoucher();
            jvGst.setVoucherNumber("JV-INV-" + saved.getId() + "-GST");
            jvGst.setVoucherDate(invoicePostingDate);
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

        auditService.log(username, "INVOICE_APPROVED", "sales_invoices", saved.getId(), null,
                "Approved invoice voucher & posted to ledger: " + saved.getInvoiceNumber());

        return saved;
    }

    @Transactional
    public SalesInvoice cancelInvoice(Long id, String username) {
        SalesInvoice invoice = invoiceRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + id));

        validationService.validateInvoiceCancel(invoice);

        String previousStatus = invoice.getStatus();

        // Perform accounting reversals for APPROVED invoices
        if ("APPROVED".equalsIgnoreCase(previousStatus)) {
            periodValidationService.validatePostingAllowed(invoice.getCompanyId(), LocalDate.now());
            String reversalRef = "REV-" + invoice.getInvoiceNumber();

            List<JournalVoucher> existingReversals = jvRepository.findByCompanyIdAndReferenceNumberAndIsDeletedFalse(invoice.getCompanyId(), reversalRef);

            if (existingReversals.isEmpty()) {
                // 1. Post Customer Ledger reversal (Credit entry = netAmount)
                ledgerService.postToLedger(
                        invoice.getCustomer().getId(),
                        null,
                        invoice,
                        BigDecimal.ZERO,
                        invoice.getNetAmount(),
                        "Reversal of cancelled sales invoice " + invoice.getInvoiceNumber(),
                        username,
                        invoice.getBranchId()
                );

                // 2. Post Journal Voucher reversals
                List<JournalVoucher> originalJvs = jvRepository.findByCompanyIdAndReferenceNumberAndIsDeletedFalse(invoice.getCompanyId(), invoice.getInvoiceNumber());
                for (JournalVoucher origJv : originalJvs) {
                    JournalVoucher revJv = new JournalVoucher();
                    revJv.setVoucherNumber("JV-REV-" + origJv.getId() + "-" + System.currentTimeMillis());
                    revJv.setVoucherDate(LocalDate.now());
                    revJv.setDebitAccount(origJv.getCreditAccount());
                    revJv.setCreditAccount(origJv.getDebitAccount());
                    revJv.setAmount(origJv.getAmount());
                    revJv.setReferenceNumber(reversalRef);
                    revJv.setDescription("Auto-posted reversal JV for cancelled invoice " + invoice.getInvoiceNumber());
                    revJv.setCompanyId(invoice.getCompanyId());
                    revJv.setBranchId(invoice.getBranchId());
                    revJv.setIsDeleted(false);
                    revJv.setCreatedBy(username);
                    revJv.setUpdatedBy(username);
                    revJv.setCode(revJv.getVoucherNumber());
                    revJv.setName("Journal Voucher Reversal Entry");
                    jvService.createVoucher(revJv, username);
                }
            }
        }

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
        validationService.validateInvoiceDelete(invoice);

        invoice.setIsDeleted(true);
        invoice.setUpdatedBy(username);
        invoiceRepository.save(invoice);

        auditService.log(username, "INVOICE_DELETED", "sales_invoices", invoice.getId(), null,
                "Soft deleted invoice voucher: " + invoice.getInvoiceNumber());
    }

    @Transactional
    public SalesInvoice createInvoiceFromTrip(Long tripId, String username) {
        return createInvoiceFromTrips(List.of(tripId), username);
    }

    /**
     * One draft invoice for several completed, unbilled trips of the same customer (e.g. a week's loads to one site).
     * Every trip is row-locked and checked exactly like a single-trip invoice.
     */
    @Transactional
    public SalesInvoice createInvoiceFromTrips(List<Long> tripIds, String username) {
        if (tripIds == null || tripIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one completed trip to invoice.");
        }
        if (tripIds.size() > 200) {
            throw new IllegalArgumentException("Invoice at most 200 trips at a time.");
        }
        SalesInvoice invoice = new SalesInvoice();
        invoice.setDiscount(BigDecimal.ZERO);
        invoice.setStatus("DRAFT");
        invoice.setPaymentTerms("NET_30");
        invoice.setDetails(new ArrayList<>());
        LocalDate latestTrip = null;
        for (Long id : new java.util.LinkedHashSet<>(tripIds)) {
            Trip t = addTripLines(invoice, id);
            if (t.getTripDate() != null && (latestTrip == null || t.getTripDate().isAfter(latestTrip))) latestTrip = t.getTripDate();
        }
        // Delegate to createInvoice to reuse calculation logic, numbering, validation and logs.
        return createInvoice(invoice, username);
    }

    private Trip addTripLines(SalesInvoice invoice, Long tripId) {
        // Concurrency duplicate prevention: lock the Trip record.
        Trip trip = tripRepository.findAndLockById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found or deleted with ID: " + tripId));

        tenantAccess.assertOwned(trip.getCompanyId());

        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && !tenantAccess.isCompanyWideAdmin(currentUser) && trip.getBranchId() != null && !currentUser.getBranchId().equals(trip.getBranchId())) {
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

        if (invoice.getCustomer() == null) {
            invoice.setCustomer(trip.getBooking().getCustomer());
            invoice.setCompanyId(trip.getCompanyId());
            invoice.setBranchId(tenantAccess.resolveBranchId(trip.getBranchId()));
        } else if (!invoice.getCustomer().getId().equals(trip.getBooking().getCustomer().getId())) {
            throw new BusinessValidationException("Different Customers", "INVOICE_TRIPS_MIXED_CUSTOMERS",
                    "Trip " + trip.getTripNumber() + " belongs to " + trip.getBooking().getCustomer().getName()
                            + ", not " + invoice.getCustomer().getName() + ".",
                    "Select trips of one customer for one invoice.");
        }

        String billingBasis = billingQuantityPolicy == null ? BillingQuantityPolicy.DELIVERED : billingQuantityPolicy.basis(trip.getCompanyId());
        for (TripDetail td : trip.getDetails()) {
            if (td.getMaterial() == null) {
                throw new IllegalArgumentException("Material information is missing from trip details.");
            }
            BigDecimal billQty = BillingQuantityPolicy.billQty(td, billingBasis);
            if (billQty == null || billQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity is invalid for material: " + td.getMaterial().getName());
            }
            if (td.getRate() != null && td.getRate().compareTo(BigDecimal.ZERO) < 0) {
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
            detail.setUom(td.getUom());   // billed in the unit the trip moved (booking unit); never converted
            // Bill the weighbridge delivered quantity when recorded; trip-level rates win, booking rates fill gaps.
            detail.setQuantity(billQty);
            detail.setDeliveredQuantity(td.getDeliveredQuantity());   // always kept and shown, whatever the basis
            detail.setQuantityBasis(billingBasis);
            detail.setRate(positiveOr(td.getRate(), bookingDetail.getRate()));
            detail.setLoadingCharges(positiveOr(td.getLoadingCharges(), bookingDetail.getLoadingCharge()));
            detail.setRoyalty(positiveOr(td.getRoyalty(), bookingDetail.getRoyaltyRate()));
            detail.setFreightCharges(bookingDetail.getTransportRate());
            detail.setGstPercentage(bookingDetail.getGstPercentage());

            invoice.getDetails().add(detail);
        }

        return trip;
    }

    /**
     * New-invoice helper: every material line of the customer's completed, not-yet-invoiced trips, built exactly like
     * "invoice from trip" (billing basis, trip / booking rates, GST). Nothing is saved; trips the user cannot bill are
     * reported in "skipped". Same company / branch scope as Ready for billing.
     */
    @Transactional
    public java.util.Map<String, Object> billableLinesForCustomer(Long customerId, java.time.LocalDate upTo) {
        Long companyId = tenantAccess.resolveCompanyId(null);
        org.springframework.data.domain.Page<Trip> ready = tripRepository.findCompletedTripsReadyForBilling(
                companyId, tenantAccess.listBranchScope(), org.springframework.data.domain.PageRequest.of(0, 500));
        SalesInvoice preview = new SalesInvoice();
        java.util.List<String> skipped = new java.util.ArrayList<>();
        int trips = 0, later = 0;
        // Period billing: only trips on or before the invoice date (an invoice cannot be dated before its trips); oldest first.
        java.util.List<Trip> mine = ready.getContent().stream()
                .filter(t -> t.getBooking() != null && t.getBooking().getCustomer() != null && customerId.equals(t.getBooking().getCustomer().getId()))
                .sorted(java.util.Comparator.comparing(Trip::getTripDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Trip::getTripNumber, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        for (Trip t : mine) {
            if (upTo != null && t.getTripDate() != null && t.getTripDate().isAfter(upTo)) { later++; continue; }
            int before = preview.getDetails().size();
            try {
                addTripLines(preview, t.getId());
                trips++;
            } catch (RuntimeException ex) {
                while (preview.getDetails().size() > before) preview.getDetails().remove(preview.getDetails().size() - 1);
                skipped.add(t.getTripNumber() + ": " + ex.getMessage());
            }
        }
        java.util.List<java.util.Map<String, Object>> lines = new java.util.ArrayList<>();
        for (SalesInvoiceDetail d : preview.getDetails()) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("tripId", d.getTrip().getId());
            m.put("tripNumber", d.getTrip().getTripNumber());
            m.put("tripDate", d.getTrip().getTripDate());
            if (d.getTrip().getBooking() != null && d.getTrip().getBooking().getDeliverySite() != null) {
                m.put("siteName", d.getTrip().getBooking().getDeliverySite().getSiteName());
            }
            m.put("materialId", d.getMaterial().getId());
            m.put("materialName", d.getMaterial().getName());
            if (d.getUom() != null) {
                java.util.Map<String, Object> u = new java.util.LinkedHashMap<>();
                u.put("id", d.getUom().getId());
                u.put("code", d.getUom().getCode());
                u.put("symbol", d.getUom().getSymbol());
                u.put("label", OrderUomService.label(d.getUom()));
                m.put("uom", u);
            }
            m.put("quantity", d.getQuantity());
            m.put("deliveredQuantity", d.getDeliveredQuantity());
            m.put("quantityBasis", d.getQuantityBasis());
            m.put("rate", d.getRate());
            m.put("freightCharges", d.getFreightCharges());
            m.put("loadingCharges", d.getLoadingCharges());
            m.put("royalty", d.getRoyalty());
            m.put("gstPercentage", d.getGstPercentage());
            lines.add(m);
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("tripCount", trips);
        out.put("laterTrips", later);   // completed, uninvoiced, but dated after the invoice date (next invoice)
        out.put("lines", lines);
        out.put("skipped", skipped);
        return out;
    }

    private static BigDecimal positiveOr(BigDecimal preferred, BigDecimal fallback) {
        if (preferred != null && preferred.signum() > 0) return preferred;
        return fallback != null ? fallback : BigDecimal.ZERO;
    }

    public List<SalesInvoice> getOutstandingInvoices(Long customerId, Long companyId, Long branchId) {
        return invoiceRepository.findOutstandingInvoices(customerId, companyId, branchId);
    }

    @Transactional(readOnly = true)
    public SalesInvoicePrintDTO getInvoicePrintData(Long id) {
        SalesInvoice invoice = getInvoiceById(id);

        com.transport.erp.model.AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null && !tenantAccess.isCompanyWideAdmin(currentUser)
                && invoice.getBranchId() != null && !currentUser.getBranchId().equals(invoice.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Invoice belongs to another branch.");
        }

        SalesInvoicePrintDTO dto = new SalesInvoicePrintDTO();
        dto.setInvoiceId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setStatus(invoice.getStatus());
        dto.setPaymentTerms(invoice.getPaymentTerms());
        dto.setPaymentStatus(invoice.getPaymentStatus());
        dto.setSupplyType(invoice.getSupplyType());
        dto.setPlaceOfSupply(invoice.getPlaceOfSupply());

        // Customer details
        Customer customer = invoice.getCustomer();
        if (customer != null) {
            dto.setCustomerId(customer.getId());
            dto.setCustomerName(customer.getName());
            dto.setCustomerCode(customer.getCode());
            dto.setCustomerAddress(customer.getAddress());
            dto.setCustomerPhone(customer.getPhone());
            dto.setCustomerEmail(customer.getEmail());
            dto.setCustomerGSTIN(customer.getGstNumber());
        }

        // Company details
        if (invoice.getCompanyId() != null) {
            companyRepository.findById(invoice.getCompanyId()).ifPresent(c -> {
                dto.setCompanyId(c.getId());
                dto.setCompanyName(c.getName());
                dto.setCompanyAddress(c.getAddress());
                dto.setCompanyPhone(c.getPhone());
                dto.setCompanyEmail(c.getEmail());
                dto.setCompanyGSTIN(c.getGstNumber());
                dto.setCompanyPAN(c.getPanNumber());
                dto.setCompanyLogo(c.getLogo());
                dto.setCompanyState(c.getState());
                dto.setCompanyCity(c.getCity());
                dto.setCompanyPincode(c.getPincode());
                dto.setBankName(c.getBankName());
                dto.setBankBranch(c.getBankBranch());
                dto.setBankAccountNumber(c.getBankAccountNumber());
                dto.setBankIfsc(c.getBankIfsc());
                dto.setUpiId(c.getUpiId());
                dto.setInvoiceTerms(c.getInvoiceTerms());
            });
        }

        // Branch details
        if (invoice.getBranchId() != null) {
            branchRepository.findById(invoice.getBranchId()).ifPresent(b -> {
                dto.setBranchId(b.getId());
                dto.setBranchName(b.getName());
                dto.setBranchAddress(b.getAddress());
                dto.setBranchPhone(b.getPhone());
                dto.setBranchGSTIN(b.getGstNumber());
            });
        }
        // Due date from the payment terms (same day counts as receipts' ageing); booking + ship-to from the first trip
        if (invoice.getInvoiceDate() != null) dto.setDueDate(invoice.getInvoiceDate().plusDays(termDays(invoice.getPaymentTerms())));
        if (invoice.getDetails() != null) {
            invoice.getDetails().stream().map(SalesInvoiceDetail::getTrip).filter(java.util.Objects::nonNull)
                    .map(com.transport.erp.model.Trip::getBooking).filter(java.util.Objects::nonNull).findFirst().ifPresent(bk -> {
                dto.setBookingNumber(bk.getBookingNumber());
                if (bk.getDeliverySite() != null) {
                    dto.setShipToName(bk.getDeliverySite().getSiteName());
                    dto.setShipToAddress(bk.getDeliverySite().getAddress());
                }
            });
        }
        if (dto.getShipToAddress() == null && customer != null) {
            dto.setShipToName(customer.getName());
            dto.setShipToAddress(customer.getAddress());
        }

        // Financial calculations
        BigDecimal subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
        BigDecimal netAmount = invoice.getNetAmount() != null ? invoice.getNetAmount() : BigDecimal.ZERO;
        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal balanceDue = netAmount.subtract(paidAmount);
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) {
            balanceDue = BigDecimal.ZERO;
        }

        dto.setSubtotal(subtotal);
        dto.setDiscount(discount);
        dto.setNetAmount(netAmount);
        dto.setPaidAmount(paidAmount);
        dto.setBalanceDue(balanceDue);

        BigDecimal totalCGST = BigDecimal.ZERO;
        BigDecimal totalSGST = BigDecimal.ZERO;
        BigDecimal totalIGST = BigDecimal.ZERO;
        List<SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO> itemDTOs = new ArrayList<>();

        if (invoice.getDetails() != null) {
            for (SalesInvoiceDetail detail : invoice.getDetails()) {
                SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO idto = new SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO();
                idto.setDetailId(detail.getId());
                if (detail.getTrip() != null) {
                    idto.setTripId(detail.getTrip().getId());
                    idto.setTripNumber(detail.getTrip().getTripNumber());
                    idto.setTripDate(detail.getTrip().getTripDate());
                    if (detail.getTrip().getVehicle() != null) idto.setVehicleNumber(detail.getTrip().getVehicle().getCode());
                }
                if (detail.getMaterial() != null) {
                    idto.setMaterialId(detail.getMaterial().getId());
                    idto.setMaterialName(detail.getMaterial().getName());
                    idto.setHsnCode(detail.getMaterial().getHsnCode());
                }
                idto.setQuantity(detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO);
                idto.setDeliveredQuantity(detail.getDeliveredQuantity());
                idto.setQuantityBasis(detail.getQuantityBasis());
                if (detail.getUom() != null) {
                    idto.setUomCode(detail.getUom().getCode());
                    idto.setUomLabel(OrderUomService.label(detail.getUom()));
                }
                idto.setRate(detail.getRate() != null ? detail.getRate() : BigDecimal.ZERO);
                idto.setFreightCharges(detail.getFreightCharges() != null ? detail.getFreightCharges() : BigDecimal.ZERO);
                idto.setLoadingCharges(detail.getLoadingCharges() != null ? detail.getLoadingCharges() : BigDecimal.ZERO);
                idto.setRoyalty(detail.getRoyalty() != null ? detail.getRoyalty() : BigDecimal.ZERO);
                idto.setGstPercentage(detail.getGstPercentage() != null ? detail.getGstPercentage() : BigDecimal.ZERO);
                idto.setCgst(detail.getCgst() != null ? detail.getCgst() : BigDecimal.ZERO);
                idto.setSgst(detail.getSgst() != null ? detail.getSgst() : BigDecimal.ZERO);
                idto.setIgst(detail.getIgst() != null ? detail.getIgst() : BigDecimal.ZERO);

                BigDecimal lineTax = idto.getCgst().add(idto.getSgst()).add(idto.getIgst());
                BigDecimal lineDiscount = detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal lineTaxable = detail.getTaxableAmount() != null ? detail.getTaxableAmount() : BigDecimal.ZERO;

                idto.setLineSubtotal(lineTaxable.add(lineDiscount));
                idto.setLineDiscount(lineDiscount);
                idto.setLineTaxable(lineTaxable);
                idto.setLineTax(lineTax);
                idto.setNetAmount(detail.getNetAmount() != null ? detail.getNetAmount() : lineTaxable.add(lineTax));

                totalCGST = totalCGST.add(idto.getCgst());
                totalSGST = totalSGST.add(idto.getSgst());
                totalIGST = totalIGST.add(idto.getIgst());

                itemDTOs.add(idto);
            }
        }

        dto.setItems(itemDTOs);
        dto.setTotalCGST(totalCGST);
        dto.setTotalSGST(totalSGST);
        dto.setTotalIGST(totalIGST);
        BigDecimal totalTax = totalCGST.add(totalSGST).add(totalIGST);
        dto.setTaxableAmount(invoice.getTaxableAmount() != null ? invoice.getTaxableAmount() : netAmount.subtract(totalTax));

        return dto;
    }

    @Transactional(readOnly = true)
    public void generateInvoicePdf(Long id, java.io.OutputStream os) throws Exception {
        SalesInvoicePrintDTO data = getInvoicePrintData(id);
        data.setLogoImage(loadLogo(data.getCompanyLogo()));
        com.transport.erp.util.SalesInvoicePdfGenerator.generateSalesInvoicePdf(data, os);
    }

    /**
     * PROFORMA in the Normal PDF layout (title "PROFORMA INVOICE — NOT A TAX INVOICE", PROFORMA watermark, official total
     * noted) with only Qty / Rate / Taxable / GST % changed per line. Tax and totals are recalculated here with the invoice's
     * rule (IGST, or CGST + SGST half each, 2 decimals). READ ONLY: works on a copy of the print data; nothing is saved.
     */
    @Transactional(readOnly = true)
    public void generateProformaPdf(Long id, List<com.transport.erp.service.XlsxExportService.CustomLineAmounts> edits,
                                    java.io.OutputStream os) throws Exception {
        SalesInvoicePrintDTO data = getInvoicePrintData(id);   // tenant / branch checks as for the Normal PDF
        java.util.Map<Long, com.transport.erp.service.XlsxExportService.CustomLineAmounts> byLine = new java.util.HashMap<>();
        for (var e : edits == null ? List.<com.transport.erp.service.XlsxExportService.CustomLineAmounts>of() : edits) {
            if (e == null || e.detailId() == null || byLine.put(e.detailId(), e) != null) throw proformaErr("Each invoice line can be listed once.");
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (data.getItems() != null) data.getItems().forEach(it -> ids.add(it.getDetailId()));
        if (!ids.containsAll(byLine.keySet())) throw proformaErr("A line in the proforma does not belong to this invoice.");
        boolean igst = data.getSupplyType() != null ? data.getSupplyType().toUpperCase().contains("INTER") : nzP(data.getTotalIGST()).signum() > 0;
        BigDecimal officialNet = nzP(data.getNetAmount());
        BigDecimal tTaxable = BigDecimal.ZERO, tC = BigDecimal.ZERO, tS = BigDecimal.ZERO, tI = BigDecimal.ZERO;
        int n = 0;
        for (var it : data.getItems() == null ? List.<SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO>of() : data.getItems()) {
            n++;
            var e = byLine.get(it.getDetailId());
            String where = data.getInvoiceNumber() + " line " + n;
            BigDecimal qty = e == null || e.quantity() == null ? nzP(it.getQuantity()) : proformaQty(e.quantity(), where);
            BigDecimal gst = e == null || e.gstPercentage() == null ? nzP(it.getGstPercentage()) : proformaGst(e.gstPercentage(), where);
            BigDecimal taxable = e == null ? nzP(it.getLineTaxable())
                    : e.amount() != null ? proformaMoney(e.amount(), where)
                    : e.rate() != null ? qty.multiply(proformaMoney(e.rate(), where)).setScale(2, java.math.RoundingMode.HALF_UP)
                    : nzP(it.getLineTaxable());
            BigDecimal rate = e != null && e.rate() != null ? proformaMoney(e.rate(), where)
                    : qty.signum() == 0 ? BigDecimal.ZERO : taxable.divide(qty, 2, java.math.RoundingMode.HALF_UP);
            BigDecimal c = BigDecimal.ZERO, s2 = BigDecimal.ZERO, i2 = BigDecimal.ZERO;
            if (igst) i2 = taxable.multiply(gst).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            else { c = taxable.multiply(gst).divide(BigDecimal.valueOf(200), 2, java.math.RoundingMode.HALF_UP); s2 = c; }
            it.setQuantity(qty); it.setRate(rate);
            it.setFreightCharges(BigDecimal.ZERO); it.setLoadingCharges(BigDecimal.ZERO); it.setRoyalty(BigDecimal.ZERO);
            it.setLineSubtotal(taxable); it.setLineDiscount(BigDecimal.ZERO); it.setLineTaxable(taxable);
            it.setGstPercentage(gst); it.setCgst(c); it.setSgst(s2); it.setIgst(i2);
            it.setLineTax(c.add(s2).add(i2)); it.setNetAmount(taxable.add(c).add(s2).add(i2));
            tTaxable = tTaxable.add(taxable); tC = tC.add(c); tS = tS.add(s2); tI = tI.add(i2);
        }
        data.setSubtotal(tTaxable); data.setDiscount(BigDecimal.ZERO); data.setTaxableAmount(tTaxable);
        data.setTotalCGST(tC); data.setTotalSGST(tS); data.setTotalIGST(tI);
        data.setNetAmount(tTaxable.add(tC).add(tS).add(tI));
        data.setProforma(true);
        data.setOfficialNetAmount(officialNet);
        data.setLogoImage(loadLogo(data.getCompanyLogo()));
        com.transport.erp.util.SalesInvoicePdfGenerator.generateSalesInvoicePdf(data, os);
    }

    private static BigDecimal nzP(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal proformaQty(BigDecimal v, String w) {
        if (v.signum() < 0) throw proformaErr("Quantity cannot be negative (" + w + ").");
        if (v.stripTrailingZeros().scale() > 3) throw proformaErr("Quantity can have at most 3 decimals (" + w + ").");
        return v;
    }
    private static BigDecimal proformaMoney(BigDecimal v, String w) {
        if (v.signum() < 0) throw proformaErr("Amount / rate cannot be negative (" + w + ").");
        if (v.stripTrailingZeros().scale() > 2) throw proformaErr("Amount / rate can have at most 2 decimals (" + w + ").");
        return v.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
    private static BigDecimal proformaGst(BigDecimal v, String w) {
        if (v.signum() < 0 || v.compareTo(BigDecimal.valueOf(28)) > 0) throw proformaErr("GST % must be between 0 and 28 (" + w + ").");
        return v;
    }
    private static BusinessValidationException proformaErr(String m) {
        return new BusinessValidationException("Proforma Not Valid", "PROFORMA_INVALID", m, "Correct the values and try again.");
    }

    /** Days for a payment-terms code (IMMEDIATE = 0, NET_7 / NET_15 / NET_30 / NET_45 / NET_60). */
    static long termDays(String terms) {
        if (terms == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(terms);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.transport.erp.repository.StoredFileRepository storedFileRepository;

    /** Company logo for the PDF: a data URL or a file stored in TransaFlow. Web links are never fetched. Never fails. */
    private byte[] loadLogo(String logo) {
        try {
            if (logo == null || logo.isBlank()) return null;
            String v = logo.trim();
            if (v.startsWith("data:image")) {
                int comma = v.indexOf(',');
                return comma > 0 ? java.util.Base64.getDecoder().decode(v.substring(comma + 1).trim()) : null;
            }
            if (v.contains("://") || storedFileRepository == null) return null;
            String name = v.substring(v.lastIndexOf('/') + 1);
            return storedFileRepository.findFirstByStoredNameAndIsDeletedFalse(name).map(f -> f.getContent()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}


