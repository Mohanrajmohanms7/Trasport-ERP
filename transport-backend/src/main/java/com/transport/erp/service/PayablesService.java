package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Accounts payable per supplier.
 *
 * Manual bill   DRAFT -> APPROVED: Dr expense account (by category) + Dr 1210 GST Input Credit / Cr 2000 Accounts Payable.
 * Auto bill     Credit stock receipt / workshop job already posted Cr 2000: the bill only records whom we owe (no new JV).
 * Payment       Dr 2000 Accounts Payable / Cr Cash or Bank; allocated to open bills (chosen, else oldest due first).
 * Cancel        Bill: only with nothing paid (manual bill JV reversed). Payment: JV reversed, bills re-opened.
 */
@Service
public class PayablesService {

    public static final Map<String, String[]> CATEGORY_ACCOUNT = Map.of(
            "REPAIR", new String[]{"5400", "Vehicle Repair & Maintenance", "EXPENSE"},
            "TYRES", new String[]{"5410", "Tyres & Tubes", "EXPENSE"},
            "FUEL", new String[]{"5100", "Fuel Expense", "EXPENSE"},
            "OFFICE", new String[]{"5600", "Office & Administrative Expenses", "EXPENSE"},
            "OTHER", new String[]{"5000", "General Operating Expenses", "EXPENSE"});
    private static final List<String> METHODS = List.of("CASH", "BANK_TRANSFER", "CHEQUE", "UPI");

    @Autowired private SupplierBillRepository billRepository;
    @Autowired private SupplierPaymentRepository paymentRepository;
    @Autowired private SupplierPaymentAllocationRepository allocationRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private JournalVoucherRepository jvRepository;
    @Autowired private JournalVoucherService journalVoucherService;
    @Autowired private ChartOfAccountService coaService;
    @Autowired private FinancialYearPeriodValidationService periodValidationService;
    @Autowired private DocumentNumberService documentNumberService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private ApprovalPolicyService approvalPolicy;
    @Autowired private AuditService auditService;

    static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ queries

    public Page<SupplierBill> bills(Long companyId, Long supplierId, String status, String paymentStatus, LocalDate from, LocalDate to, int size) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        return billRepository.search(cid, restrictedBranchId(), supplierId, blankToNull(status), blankToNull(paymentStatus),
                from != null ? from : LocalDate.of(2000, 1, 1), to != null ? to : LocalDate.of(2999, 12, 31), PageRequest.of(0, Math.min(size, 1000)));
    }

    public List<SupplierBill> openBills(Long supplierId) {
        Supplier s = requireSupplier(supplierId);
        return billRepository.search(s.getCompanyId(), restrictedBranchId(), supplierId, "APPROVED", null,
                LocalDate.of(2000, 1, 1), LocalDate.of(2999, 12, 31), PageRequest.of(0, 1000)).getContent().stream()
                .filter(b -> b.getBalance().signum() > 0).sorted(Comparator.comparing(SupplierBill::getDueDate)).toList();
    }

    public Page<SupplierPayment> payments(Long companyId, Long supplierId, LocalDate from, LocalDate to, int size) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        return paymentRepository.search(cid, restrictedBranchId(), supplierId,
                from != null ? from : LocalDate.of(2000, 1, 1), to != null ? to : LocalDate.of(2999, 12, 31), PageRequest.of(0, Math.min(size, 1000)));
    }

    public List<SupplierPaymentAllocation> paymentAllocations(Long paymentId) {
        SupplierPayment p = paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        tenantAccess.assertOwned(p.getCompanyId());
        return allocationRepository.findByPaymentIdAndStatusAndIsDeletedFalse(paymentId, "ACTIVE");
    }

    // ------------------------------------------------------------------ bills

    @Transactional
    public SupplierBill createBill(SupplierBill in, String username) {
        Supplier supplier = requireSupplier(in.getSupplier() == null ? null : in.getSupplier().getId());
        SupplierBill b = new SupplierBill();
        b.setSupplier(supplier);
        b.setCompanyId(supplier.getCompanyId());
        b.setBranchId(in.getBranchId() != null ? in.getBranchId() : tenantAccess.requireCurrentUser().getBranchId());
        b.setSourceType("MANUAL");
        b.setStatus("DRAFT");
        b.setIsDeleted(false);
        b.setCreatedBy(username);
        applyBillInputs(b, in);
        b.setBillNumber(documentNumberService.next(b.getCompanyId(), "SUPPLIER_BILL", "SB-", b.getBillDate()));
        b.setCode(b.getBillNumber());
        b.setName("Bill " + supplier.getName());
        b.setUpdatedBy(username);
        SupplierBill saved = billRepository.save(b);
        auditService.log(username, "SUPPLIER_BILL_CREATED", "supplier_bills", saved.getId(), null, saved.getBillNumber() + " " + saved.getTotalAmount());
        return saved;
    }

    @Transactional
    public SupplierBill updateBill(Long id, SupplierBill in, String username) {
        SupplierBill b = lockBill(id);
        if (!"DRAFT".equals(b.getStatus()) || !"MANUAL".equals(b.getSourceType())) {
            throw invalid("Bill Locked", "SUPPLIER_BILL_LOCKED", "Only draft manual bills can be edited.", "Cancel it and enter a new bill.");
        }
        if (in.getSupplier() != null && in.getSupplier().getId() != null) b.setSupplier(requireSupplier(in.getSupplier().getId()));
        applyBillInputs(b, in);
        b.setUpdatedBy(username);
        return billRepository.save(b);
    }

    private void applyBillInputs(SupplierBill b, SupplierBill in) {
        String category = in.getCategory() == null ? "OTHER" : in.getCategory().trim().toUpperCase();
        if (!CATEGORY_ACCOUNT.containsKey(category)) {
            throw invalid("Invalid Category", "SUPPLIER_BILL_CATEGORY",
                    "Category must be REPAIR, TYRES, FUEL, OFFICE or OTHER. Spare parts for stock are billed through Stock → Receive stock (credit).",
                    "Pick a category.");
        }
        LocalDate billDate = in.getBillDate() != null ? in.getBillDate() : LocalDate.now();
        if (billDate.isAfter(LocalDate.now())) {
            throw invalid("Bill Date In Future", "SUPPLIER_BILL_DATE_FUTURE", "Bill date " + billDate + " is in the future.", "Use the date on the supplier's bill.");
        }
        BigDecimal taxable = money(in.getTaxableAmount());
        BigDecimal gst = money(in.getGstAmount());
        if (taxable.signum() <= 0 || gst.signum() < 0) {
            throw invalid("Invalid Amount", "SUPPLIER_BILL_AMOUNT", "Bill amount must be greater than zero and GST cannot be negative.", "Enter the bill amounts.");
        }
        int creditDays = b.getSupplier().getCreditDays() == null ? 0 : b.getSupplier().getCreditDays();
        LocalDate due = in.getDueDate() != null ? in.getDueDate() : billDate.plusDays(creditDays);
        if (due.isBefore(billDate)) {
            throw invalid("Invalid Due Date", "SUPPLIER_BILL_DUE", "Due date cannot be before the bill date.", "Correct the due date.");
        }
        b.setCategory(category);
        b.setBillDate(billDate);
        b.setDueDate(due);
        b.setSupplierBillNo(in.getSupplierBillNo() == null ? null : in.getSupplierBillNo().trim());
        b.setTaxableAmount(taxable);
        b.setGstAmount(gst);
        b.setTotalAmount(taxable.add(gst));
        b.setRemarks(in.getRemarks());
        if (in.getVehicle() != null && in.getVehicle().getId() != null) {
            Vehicle v = vehicleRepository.findById(in.getVehicle().getId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            tenantAccess.assertOwned(v.getCompanyId());
            b.setVehicle(v);
        } else {
            b.setVehicle(null);
        }
    }

    @Transactional
    public SupplierBill approveBill(Long id, String username) {
        SupplierBill b = lockBill(id);
        if (!"DRAFT".equals(b.getStatus())) {
            throw invalid("Invalid Bill Status", "SUPPLIER_BILL_STATUS", "Bill " + b.getBillNumber() + " is " + b.getStatus() + ".", "Only draft bills can be approved.");
        }
        approvalPolicy.assertDifferentApprover(b.getCompanyId(), b.getCreatedBy(), username, "Supplier bill " + b.getBillNumber());
        periodValidationService.validatePostingAllowed(b.getCompanyId(), b.getBillDate());
        String[] acc = CATEGORY_ACCOUNT.get(b.getCategory());
        String ref = "SB-POST-" + b.getId();
        ChartOfAccount payable = coaService.getOrCreateAccount(b.getCompanyId(), b.getBranchId(), "2000", "Accounts Payable", "LIABILITY");
        post(b.getCompanyId(), b.getBranchId(), b.getBillDate(), coaService.getOrCreateAccount(b.getCompanyId(), b.getBranchId(), acc[0], acc[1], acc[2]),
                payable, b.getTaxableAmount(), ref, "Supplier bill " + b.getBillNumber() + " " + b.getSupplier().getName()
                        + (b.getSupplierBillNo() != null ? " (bill " + b.getSupplierBillNo() + ")" : ""), username);
        if (b.getGstAmount().signum() > 0) {
            post(b.getCompanyId(), b.getBranchId(), b.getBillDate(),
                    coaService.getOrCreateAccount(b.getCompanyId(), b.getBranchId(), "1210", "GST Input Credit", "ASSET"),
                    payable, b.getGstAmount(), ref + "-GST", "GST on supplier bill " + b.getBillNumber(), username);
        }
        b.setJvReference(ref);
        b.setStatus("APPROVED");
        b.setUpdatedBy(username);
        SupplierBill saved = billRepository.save(b);
        auditService.log(username, "SUPPLIER_BILL_APPROVED", "supplier_bills", saved.getId(), null, saved.getBillNumber());
        return saved;
    }

    @Transactional
    public SupplierBill cancelBill(Long id, String username) {
        SupplierBill b = lockBill(id);
        if ("CANCELLED".equals(b.getStatus())) {
            throw invalid("Already Cancelled", "SUPPLIER_BILL_CANCELLED", "Bill is already cancelled.", "No action needed.");
        }
        if (b.getPaidAmount().signum() > 0) {
            throw invalid("Bill Has Payments", "SUPPLIER_BILL_PAID", "₹" + b.getPaidAmount().toPlainString() + " is already paid against this bill.",
                    "Cancel the payment first.");
        }
        if (!"MANUAL".equals(b.getSourceType()) && "APPROVED".equals(b.getStatus())) {
            throw invalid("Created By " + b.getSourceType(), "SUPPLIER_BILL_AUTO", "This bill was created by " + b.getSourceReference() + ".",
                    "Correct it at the source (stock receipt / work order).");
        }
        if ("APPROVED".equals(b.getStatus())) {
            reverse(b.getCompanyId(), "SB-POST-" + b.getId(), username);
            reverse(b.getCompanyId(), "SB-POST-" + b.getId() + "-GST", username);
        }
        b.setStatus("CANCELLED");
        b.setUpdatedBy(username);
        return billRepository.save(b);
    }

    /**
     * Bill for a payable already posted elsewhere (credit stock receipt, workshop part of a work order).
     * Idempotent per source.
     */
    @Transactional
    public SupplierBill recordPostedPayable(Supplier supplier, Long branchId, String sourceType, Long sourceId, String sourceRef,
                                           String supplierBillNo, LocalDate date, BigDecimal amount, Vehicle vehicle, String jvRef, String username) {
        if (supplier == null || amount == null || amount.signum() <= 0) return null;
        Optional<SupplierBill> existing = billRepository.findActiveBySource(supplier.getCompanyId(), sourceType, sourceId);
        if (existing.isPresent()) return existing.get();
        SupplierBill b = new SupplierBill();
        b.setSupplier(supplier);
        b.setCompanyId(supplier.getCompanyId());
        b.setBranchId(branchId);
        b.setSourceType(sourceType);
        b.setSourceId(sourceId);
        b.setSourceReference(sourceRef);
        b.setSupplierBillNo(supplierBillNo);
        b.setCategory("STOCK_RECEIPT".equals(sourceType) ? "PARTS_STOCK" : "WORKSHOP");
        b.setBillDate(date);
        b.setDueDate(date.plusDays(supplier.getCreditDays() == null ? 0 : supplier.getCreditDays()));
        b.setTaxableAmount(money(amount));
        b.setGstAmount(BigDecimal.ZERO.setScale(2));
        b.setTotalAmount(money(amount));
        b.setVehicle(vehicle);
        b.setJvReference(jvRef);
        b.setStatus("APPROVED");
        b.setIsDeleted(false);
        b.setBillNumber(documentNumberService.next(b.getCompanyId(), "SUPPLIER_BILL", "SB-", date));
        b.setCode(b.getBillNumber());
        b.setName("Bill " + supplier.getName());
        b.setCreatedBy(username);
        b.setUpdatedBy(username);
        return billRepository.save(b);
    }

    // ------------------------------------------------------------------ payments

    public record Allocation(Long billId, BigDecimal amount) { }

    @Transactional
    public SupplierPayment pay(Long supplierId, LocalDate date, BigDecimal amount, String method, String reference, String remarks,
                               List<Allocation> allocations, String username) {
        Supplier supplier = requireSupplier(supplierId);
        BigDecimal amt = money(amount);
        if (amt.signum() <= 0) throw invalid("Invalid Amount", "SUPPLIER_PAYMENT_AMOUNT", "Payment amount must be greater than zero.", "Enter the amount paid.");
        String m = method == null ? "BANK_TRANSFER" : method.trim().toUpperCase();
        if (!METHODS.contains(m)) throw invalid("Invalid Method", "SUPPLIER_PAYMENT_METHOD", "Method must be CASH, BANK_TRANSFER, CHEQUE or UPI.", "Pick a method.");
        LocalDate d = date != null ? date : LocalDate.now();
        periodValidationService.validatePostingAllowed(supplier.getCompanyId(), d);

        List<SupplierBill> open = billRepository.lockOpenBills(supplier.getCompanyId(), supplier.getId());
        BigDecimal outstanding = open.stream().map(SupplierBill::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amt.compareTo(outstanding) > 0) {
            throw invalid("More Than Due", "SUPPLIER_PAYMENT_EXCEEDS", String.format("₹%s is more than the ₹%s due to %s.",
                    amt.toPlainString(), money(outstanding).toPlainString(), supplier.getName()), "Pay at most the amount due, or enter the missing bill first.");
        }
        Map<Long, SupplierBill> byId = new LinkedHashMap<>();
        open.forEach(b -> byId.put(b.getId(), b));
        List<Allocation> plan = new ArrayList<>();
        if (allocations != null && !allocations.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Allocation a : allocations) {
                if (a.amount() == null || a.amount().signum() <= 0) continue;
                SupplierBill b = byId.get(a.billId());
                if (b == null) throw invalid("Bill Not Open", "SUPPLIER_PAYMENT_BILL", "Bill " + a.billId() + " is not an open bill of this supplier.", "Pick from the open bills.");
                if (money(a.amount()).compareTo(b.getBalance()) > 0) {
                    throw invalid("More Than Bill Balance", "SUPPLIER_PAYMENT_BILL_EXCEEDS", "₹" + money(a.amount()) + " is more than bill " + b.getBillNumber() + " balance ₹" + b.getBalance() + ".", "Reduce the amount.");
                }
                plan.add(new Allocation(b.getId(), money(a.amount())));
                sum = sum.add(money(a.amount()));
            }
            if (sum.compareTo(amt) != 0) {
                throw invalid("Allocation Mismatch", "SUPPLIER_PAYMENT_ALLOC_SUM", "Allocations total ₹" + sum + " but the payment is ₹" + amt + ".", "Make them equal.");
            }
        } else {
            BigDecimal left = amt;
            for (SupplierBill b : open) {
                if (left.signum() <= 0) break;
                BigDecimal take = left.min(b.getBalance());
                plan.add(new Allocation(b.getId(), take));
                left = left.subtract(take);
            }
        }

        SupplierPayment p = new SupplierPayment();
        p.setSupplier(supplier);
        p.setCompanyId(supplier.getCompanyId());
        p.setBranchId(tenantAccess.requireCurrentUser().getBranchId());
        p.setPaymentDate(d);
        p.setAmount(amt);
        p.setPaymentMethod(m);
        p.setReferenceNumber(reference);
        p.setRemarks(remarks);
        p.setStatus("POSTED");
        p.setIsDeleted(false);
        p.setPaymentNumber(documentNumberService.next(p.getCompanyId(), "SUPPLIER_PAYMENT", "SP-", d));
        p.setCode(p.getPaymentNumber());
        p.setName("Payment " + supplier.getName());
        p.setCreatedBy(username);
        p.setUpdatedBy(username);
        p = paymentRepository.save(p);

        for (Allocation a : plan) {
            SupplierBill b = byId.get(a.billId());
            b.setPaidAmount(b.getPaidAmount().add(a.amount()));
            b.setPaymentStatus(b.getPaidAmount().compareTo(b.getTotalAmount()) >= 0 ? "PAID" : "PARTIALLY_PAID");
            b.setUpdatedBy(username);
            billRepository.save(b);
            SupplierPaymentAllocation al = new SupplierPaymentAllocation();
            al.setPaymentId(p.getId());
            al.setBill(b);
            al.setAmount(a.amount());
            al.setStatus("ACTIVE");
            al.setCode("SPA-" + p.getId());
            al.setName("Allocation " + b.getBillNumber());
            al.setCompanyId(p.getCompanyId());
            al.setBranchId(p.getBranchId());
            al.setIsDeleted(false);
            al.setCreatedBy(username);
            al.setUpdatedBy(username);
            allocationRepository.save(al);
        }
        ChartOfAccount payable = coaService.getOrCreateAccount(p.getCompanyId(), p.getBranchId(), "2000", "Accounts Payable", "LIABILITY");
        ChartOfAccount cashOrBank = "CASH".equals(m)
                ? coaService.getOrCreateAccount(p.getCompanyId(), p.getBranchId(), "1000", "Cash on Hand", "ASSET")
                : coaService.getOrCreateAccount(p.getCompanyId(), p.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        String ref = "SP-POST-" + p.getId();
        post(p.getCompanyId(), p.getBranchId(), d, payable, cashOrBank, amt, ref,
                "Paid " + supplier.getName() + " " + p.getPaymentNumber() + (reference != null ? " ref " + reference : ""), username);
        p.setJvReference(ref);
        SupplierPayment saved = paymentRepository.save(p);
        auditService.log(username, "SUPPLIER_PAYMENT_POSTED", "supplier_payments", saved.getId(), null, saved.getPaymentNumber() + " " + amt);
        return saved;
    }

    @Transactional
    public SupplierPayment cancelPayment(Long id, String username) {
        SupplierPayment p = paymentRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        tenantAccess.assertOwned(p.getCompanyId());
        if (!"POSTED".equals(p.getStatus())) throw invalid("Not Active", "SUPPLIER_PAYMENT_STATUS", "Payment is " + p.getStatus() + ".", "No action needed.");
        periodValidationService.validatePostingAllowed(p.getCompanyId(), LocalDate.now());
        for (SupplierPaymentAllocation a : allocationRepository.findByPaymentIdAndStatusAndIsDeletedFalse(p.getId(), "ACTIVE")) {
            SupplierBill b = billRepository.findByIdForUpdate(a.getBill().getId()).orElseThrow();
            b.setPaidAmount(b.getPaidAmount().subtract(a.getAmount()).max(BigDecimal.ZERO));
            b.setPaymentStatus(b.getPaidAmount().signum() == 0 ? "UNPAID" : "PARTIALLY_PAID");
            b.setUpdatedBy(username);
            billRepository.save(b);
            a.setStatus("REVERSED");
            a.setUpdatedBy(username);
            allocationRepository.save(a);
        }
        reverse(p.getCompanyId(), "SP-POST-" + p.getId(), username);
        p.setStatus("CANCELLED");
        p.setUpdatedBy(username);
        return paymentRepository.save(p);
    }

    // ------------------------------------------------------------------ helpers

    private void post(Long companyId, Long branchId, LocalDate date, ChartOfAccount dr, ChartOfAccount cr, BigDecimal amount,
                      String reference, String description, String username) {
        if (amount == null || amount.signum() <= 0) return;
        if (!jvRepository.findByReferenceNumberAndIsDeletedFalse(reference).isEmpty()) return;
        JournalVoucher v = new JournalVoucher();
        v.setVoucherDate(date);
        v.setDebitAccount(dr);
        v.setCreditAccount(cr);
        v.setAmount(amount);
        v.setReferenceNumber(reference);
        v.setDescription(description);
        v.setCompanyId(companyId);
        v.setBranchId(branchId);
        v.setCode(reference.length() <= 50 ? reference : reference.substring(0, 50));
        v.setName("Payables Voucher");
        v.setCreatedBy(username);
        v.setUpdatedBy(username);
        journalVoucherService.createVoucher(v, username);
    }

    private void reverse(Long companyId, String reference, String username) {
        List<JournalVoucher> found = jvRepository.findByReferenceNumberAndIsDeletedFalse(reference);
        if (found.isEmpty()) return;
        JournalVoucher o = found.get(0);
        post(companyId, o.getBranchId(), LocalDate.now(), o.getCreditAccount(), o.getDebitAccount(), o.getAmount(),
                "REV-" + reference, "Reversal of " + o.getVoucherNumber(), username);
    }

    private SupplierBill lockBill(Long id) {
        SupplierBill b = billRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
        tenantAccess.assertOwned(b.getCompanyId());
        return b;
    }

    private Supplier requireSupplier(Long id) {
        if (id == null) throw new IllegalArgumentException("Supplier is required.");
        Supplier s = supplierRepository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        tenantAccess.assertOwned(s.getCompanyId());
        return s;
    }

    private Long restrictedBranchId() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (tenantAccess.isSuperAdmin(user)) return null;
        boolean companyWide = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()) || "ADMIN".equals(r.getCode()) || "ACCOUNTANT".equals(r.getCode()));
        return companyWide ? null : user.getBranchId();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }

    private static BusinessValidationException invalid(String title, String code, String msg, String action) {
        return new BusinessValidationException(title, code, msg, action);
    }
}
