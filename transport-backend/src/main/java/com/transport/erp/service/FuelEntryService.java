package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.FuelEntry;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FuelEntryService {

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private JournalVoucherService jvService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AppSettingService settingService;

    public Page<FuelEntry> getFuelEntries(Long companyId, Pageable pageable) {
        return fuelEntryRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public FuelEntry getFuelEntryById(Long id) {
        FuelEntry entry = fuelEntryRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Fuel Entry not found: " + id));
        tenantAccess.assertOwned(entry.getCompanyId());
        return entry;
    }

    @Transactional
    public FuelEntry createFuelEntry(FuelEntry entry, String username) {
        String prefix = settingService.getByKey("PREFIX_FUEL").map(s -> s.getValueData()).orElse("FUEL-");
        entry.setFuelEntryNumber(prefix + System.currentTimeMillis());
        entry.setFuelDate(LocalDate.now());
        entry.setStatus("DRAFT");
        entry.setIsDeleted(false);
        entry.setCreatedBy(username);
        entry.setUpdatedBy(username);

        entry.setCompanyId(tenantAccess.resolveCompanyId(entry.getCompanyId()));
        entry.setBranchId(tenantAccess.resolveBranchId(entry.getBranchId()));

        // Calc totalAmount
        if (entry.getFuelQuantity() != null && entry.getRatePerLitre() != null) {
            entry.setTotalAmount(entry.getFuelQuantity().multiply(entry.getRatePerLitre()));
        }

        FuelEntry saved = fuelEntryRepository.save(entry);

        auditService.log(username, "FUEL_ENTRY_RECORDED", "fuel_entries", saved.getId(), null,
                "Recorded fuel entry number: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public FuelEntry approveFuelEntry(Long id, String username) {
        FuelEntry entry = fuelEntryRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fuel entry not found with ID: " + id));

        tenantAccess.assertOwned(entry.getCompanyId());
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && entry.getBranchId() != null && !currentUser.getBranchId().equals(entry.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Fuel entry belongs to another branch.");
            }
        }

        if ("APPROVED".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' is already APPROVED.", entry.getFuelEntryNumber()));
            throw new BusinessValidationException(
                    "Fuel Entry Already Approved",
                    "FUEL_ENTRY_ALREADY_APPROVED",
                    String.format("Fuel Entry '%s' is already APPROVED.", entry.getFuelEntryNumber()),
                    "No further approval action can be taken on an approved fuel entry.",
                    details
            );
        }

        if (!"DRAFT".equalsIgnoreCase(entry.getStatus()) && !"ACTIVE".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' cannot be approved because its current status is %s.", entry.getFuelEntryNumber(), entry.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Approval Blocked",
                    "FUEL_ENTRY_STATUS_APPROVAL_BLOCKED",
                    String.format("Fuel Entry '%s' cannot be approved because its current status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only DRAFT fuel entries can be approved.",
                    details
            );
        }

        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(entry.getFuelEntryNumber());
        if (existingJvs != null && !existingJvs.isEmpty()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Accounting vouchers already exist for fuel entry '%s'.", entry.getFuelEntryNumber()));
            throw new BusinessValidationException(
                    "Duplicate Accounting Blocked",
                    "FUEL_ENTRY_ACCOUNTING_EXISTS",
                    String.format("Accounting entry already exists for fuel entry '%s'.", entry.getFuelEntryNumber()),
                    "Fuel entry accounting has already been posted.",
                    details
            );
        }

        entry.setStatus("APPROVED");
        entry.setUpdatedBy(username);
        FuelEntry saved = fuelEntryRepository.save(entry);

        // Double-entry GL posting
        ChartOfAccount fuelExpenseAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5100", "Fuel Expense", "EXPENSE");
        ChartOfAccount creditAcc;
        String payMethod = saved.getPaymentMethod() != null ? saved.getPaymentMethod().toUpperCase() : "CASH";
        if ("CASH".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1000", "Cash on Hand", "ASSET");
        } else if ("CREDIT".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "2000", "Accounts Payable", "LIABILITY");
        } else {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        }

        BigDecimal amount = saved.getTotalAmount() != null ? saved.getTotalAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            JournalVoucher jv = new JournalVoucher();
            jv.setVoucherNumber("JV-FUEL-" + saved.getId());
            jv.setVoucherDate(LocalDate.now());
            jv.setDebitAccount(fuelExpenseAcc);
            jv.setCreditAccount(creditAcc);
            jv.setAmount(amount);
            jv.setReferenceNumber(saved.getFuelEntryNumber());
            jv.setDescription("Auto-posted fuel expense JV for " + saved.getFuelEntryNumber());
            jv.setCompanyId(saved.getCompanyId());
            jv.setBranchId(saved.getBranchId());
            jv.setIsDeleted(false);
            jv.setCreatedBy(username);
            jv.setUpdatedBy(username);
            jv.setCode(jv.getVoucherNumber());
            jv.setName("Fuel Expense Voucher");
            jvService.createVoucher(jv, username);
        }

        auditService.log(username, "FUEL_ENTRY_APPROVED", "fuel_entries", saved.getId(), null,
                "Approved fuel entry: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public FuelEntry cancelFuelEntry(Long id, String username) {
        FuelEntry entry = fuelEntryRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fuel entry not found with ID: " + id));

        tenantAccess.assertOwned(entry.getCompanyId());
        AppUser currentUser = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null && entry.getBranchId() != null && !currentUser.getBranchId().equals(entry.getBranchId())) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: Fuel entry belongs to another branch.");
            }
        }

        if ("CANCELLED".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' is already CANCELLED.", entry.getFuelEntryNumber()));
            throw new BusinessValidationException(
                    "Fuel Entry Already Cancelled",
                    "FUEL_ENTRY_ALREADY_CANCELLED",
                    String.format("Fuel Entry '%s' is already CANCELLED.", entry.getFuelEntryNumber()),
                    "No further cancellation action can be taken on a cancelled fuel entry.",
                    details
            );
        }

        if (!"APPROVED".equalsIgnoreCase(entry.getStatus()) && !"ACTIVE".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' cannot be cancelled because its current status is %s.", entry.getFuelEntryNumber(), entry.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Cancellation Blocked",
                    "FUEL_ENTRY_STATUS_CANCELLATION_BLOCKED",
                    String.format("Fuel Entry '%s' cannot be cancelled because its current status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only APPROVED fuel entries can be cancelled.",
                    details
            );
        }

        entry.setStatus("CANCELLED");
        entry.setUpdatedBy(username);
        FuelEntry saved = fuelEntryRepository.save(entry);

        // Reversal of JVs
        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(entry.getFuelEntryNumber());
        if (existingJvs != null) {
            for (JournalVoucher origJv : existingJvs) {
                JournalVoucher revJv = new JournalVoucher();
                revJv.setVoucherNumber("REV-JV-FUEL-" + saved.getId() + "-" + origJv.getId());
                revJv.setVoucherDate(LocalDate.now());
                revJv.setDebitAccount(origJv.getCreditAccount());
                revJv.setCreditAccount(origJv.getDebitAccount());
                revJv.setAmount(origJv.getAmount());
                revJv.setReferenceNumber("REV-" + origJv.getReferenceNumber());
                revJv.setDescription("Reversal of JV " + origJv.getVoucherNumber() + " for cancelled fuel entry " + saved.getFuelEntryNumber());
                revJv.setCompanyId(saved.getCompanyId());
                revJv.setBranchId(saved.getBranchId());
                revJv.setIsDeleted(false);
                revJv.setCreatedBy(username);
                revJv.setUpdatedBy(username);
                revJv.setCode(revJv.getVoucherNumber());
                revJv.setName("Fuel Expense Reversal Voucher");
                jvService.createVoucher(revJv, username);
            }
        }

        auditService.log(username, "FUEL_ENTRY_CANCELLED", "fuel_entries", saved.getId(), null,
                "Cancelled fuel entry: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public FuelEntry updateFuelEntry(Long id, FuelEntry details, String username) {
        FuelEntry existing = getFuelEntryById(id);

        if ("APPROVED".equalsIgnoreCase(existing.getStatus()) || "CANCELLED".equalsIgnoreCase(existing.getStatus())) {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add(String.format("Fuel entry '%s' is in %s status.", existing.getFuelEntryNumber(), existing.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Update Blocked",
                    "FUEL_ENTRY_STATUS_UPDATE_BLOCKED",
                    String.format("Fuel entry '%s' cannot be modified because its current status is %s.", existing.getFuelEntryNumber(), existing.getStatus()),
                    "Only DRAFT fuel entries can be modified.",
                    errorDetails
            );
        }

        existing.setFuelStation(details.getFuelStation());
        existing.setFuelQuantity(details.getFuelQuantity());
        existing.setRatePerLitre(details.getRatePerLitre());
        if (details.getFuelQuantity() != null && details.getRatePerLitre() != null) {
            existing.setTotalAmount(details.getFuelQuantity().multiply(details.getRatePerLitre()));
        }
        existing.setPaymentMethod(details.getPaymentMethod());
        existing.setInvoiceNumber(details.getInvoiceNumber());
        existing.setCurrentOdometer(details.getCurrentOdometer());
        existing.setPreviousOdometer(details.getPreviousOdometer());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        FuelEntry saved = fuelEntryRepository.save(existing);

        auditService.log(username, "FUEL_ENTRY_UPDATED", "fuel_entries", saved.getId(), null,
                "Updated details for fuel entry: " + saved.getFuelEntryNumber());

        return saved;
    }

    @Transactional
    public void deleteFuelEntry(Long id, String username) {
        FuelEntry entry = getFuelEntryById(id);

        if ("APPROVED".equalsIgnoreCase(entry.getStatus()) || "CANCELLED".equalsIgnoreCase(entry.getStatus())) {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add(String.format("Fuel entry '%s' is in %s status.", entry.getFuelEntryNumber(), entry.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Delete Blocked",
                    "FUEL_ENTRY_STATUS_DELETE_BLOCKED",
                    String.format("Fuel entry '%s' cannot be deleted because its current status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only DRAFT fuel entries can be deleted.",
                    errorDetails
            );
        }

        entry.setIsDeleted(true);
        entry.setUpdatedBy(username);
        fuelEntryRepository.save(entry);

        auditService.log(username, "FUEL_ENTRY_DELETED", "fuel_entries", entry.getId(), null,
                "Soft deleted fuel entry: " + entry.getFuelEntryNumber());
    }
}
