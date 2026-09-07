package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.FuelRequestRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FuelEntryService {

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private FuelRequestRepository requestRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private JournalVoucherService jvService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AppSettingService settingService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;



    public Page<FuelEntry> getFuelEntries(Long companyId, Pageable pageable) {
        Long resolvedCompanyId = tenantAccess.resolveCompanyId(companyId);
        return fuelEntryRepository.findByCompanyIdAndIsDeletedFalse(resolvedCompanyId, pageable);
    }

    public FuelEntry getFuelEntryById(Long id) {
        FuelEntry entry = fuelEntryRepository.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Entry Not Found",
                        "FUEL_ENTRY_NOT_FOUND",
                        "Fuel Entry not found with ID: " + id,
                        "Verify the fuel entry ID."
                ));
        tenantAccess.assertOwned(entry.getCompanyId());
        return entry;
    }

    @Transactional
    public FuelEntry createFuelEntry(FuelEntry entry, String username) {
        if (entry.getVehicle() == null || entry.getVehicle().getId() == null) {
            throw new BusinessValidationException(
                    "Missing Vehicle",
                    "INVALID_VEHICLE",
                    "Vehicle reference is required for fuel entry.",
                    "Select a valid vehicle."
            );
        }
        Vehicle vehicle = parentAccess.requireVehicle(entry.getVehicle().getId());

        if (entry.getDriver() == null || entry.getDriver().getId() == null) {
            throw new BusinessValidationException(
                    "Missing Driver",
                    "INVALID_DRIVER",
                    "Driver reference is required for fuel entry.",
                    "Select a valid driver."
            );
        }
        Driver driver = parentAccess.requireDriver(entry.getDriver().getId());

        FuelRequest req = null;
        if (entry.getFuelRequest() != null && entry.getFuelRequest().getId() != null) {
            Long reqId = entry.getFuelRequest().getId();
            req = requestRepository.findByIdForUpdate(reqId)
                    .orElseThrow(() -> new BusinessValidationException(
                            "Fuel Request Not Found",
                            "FUEL_REQUEST_NOT_FOUND",
                            "Referenced Fuel Request not found with ID: " + reqId,
                            "Verify the fuel request ID."
                    ));

            tenantAccess.assertOwned(req.getCompanyId());

            if ("FULFILLED".equalsIgnoreCase(req.getStatus())) {
                List<String> details = new ArrayList<>();
                details.add(String.format("Fuel Request '%s' has already been fulfilled.", req.getRequestNumber()));
                throw new BusinessValidationException(
                        "Request Already Fulfilled",
                        "FUEL_REQUEST_ALREADY_FULFILLED",
                        String.format("Fuel Request '%s' is already FULFILLED.", req.getRequestNumber()),
                        "Select an unfulfilled approved fuel request.",
                        details
                );
            }

            if ("CANCELLED".equalsIgnoreCase(req.getStatus()) || "REJECTED".equalsIgnoreCase(req.getStatus())) {
                List<String> details = new ArrayList<>();
                details.add(String.format("Fuel Request '%s' status is %s.", req.getRequestNumber(), req.getStatus()));
                throw new BusinessValidationException(
                        "Request Inactive",
                        "FUEL_REQUEST_INACTIVE",
                        String.format("Fuel Request '%s' is %s and cannot be fulfilled.", req.getRequestNumber(), req.getStatus()),
                        "Only APPROVED fuel requests can be fulfilled.",
                        details
                );
            }

            if (!"APPROVED".equalsIgnoreCase(req.getStatus())) {
                List<String> details = new ArrayList<>();
                details.add(String.format("Fuel Request '%s' status is %s.", req.getRequestNumber(), req.getStatus()));
                throw new BusinessValidationException(
                        "Request Not Approved",
                        "FUEL_REQUEST_NOT_APPROVED",
                        String.format("Fuel Request '%s' must be APPROVED before fulfillment.", req.getRequestNumber()),
                        "Approve the fuel request first.",
                        details
                );
            }

            Optional<FuelEntry> existingFulfillment = fuelEntryRepository.findByFuelRequestIdAndIsDeletedFalse(req.getId());
            if (existingFulfillment.isPresent()) {
                List<String> details = new ArrayList<>();
                details.add(String.format("Fuel Entry '%s' already fulfills request '%s'.", existingFulfillment.get().getFuelEntryNumber(), req.getRequestNumber()));
                throw new BusinessValidationException(
                        "Duplicate Fulfillment Blocked",
                        "FUEL_REQUEST_DUPLICATE_FULFILLMENT",
                        String.format("Fuel Request '%s' has already been fulfilled by another fuel entry.", req.getRequestNumber()),
                        "Each fuel request can be fulfilled at most once.",
                        details
                );
            }

            if (entry.getFuelQuantity() != null && req.getRequestedQuantity() != null &&
                    entry.getFuelQuantity().compareTo(req.getRequestedQuantity()) > 0) {
                List<String> details = new ArrayList<>();
                details.add(String.format("Fuel quantity (%.2f L) exceeds requested quantity (%.2f L).", entry.getFuelQuantity(), req.getRequestedQuantity()));
                throw new BusinessValidationException(
                        "Quantity Exceeds Request",
                        "FUEL_FULFILLMENT_EXCEEDS_REQUEST",
                        String.format("Fuel entry quantity %.2f L exceeds approved request limit of %.2f L.", entry.getFuelQuantity(), req.getRequestedQuantity()),
                        "Reduce the fuel entry quantity or request a new approval.",
                        details
                );
            }
        }

        String prefix = settingService.getByKey("PREFIX_FUEL").map(AppSetting::getValueData).orElse("FUEL-");
        entry.setFuelEntryNumber(prefix + System.currentTimeMillis());
        entry.setFuelDate(entry.getFuelDate() != null ? entry.getFuelDate() : LocalDate.now());
        entry.setStatus("DRAFT");
        entry.setIsDeleted(false);
        entry.setVehicle(vehicle);
        entry.setDriver(driver);
        entry.setCompanyId(vehicle.getCompanyId());
        Long entryBranchId = entry.getBranchId();
        if (entryBranchId == null && req != null && req.getBranchId() != null) {
            entryBranchId = req.getBranchId();
        }
        if (entryBranchId == null && vehicle.getBranchId() != null) {
            entryBranchId = vehicle.getBranchId();
        }
        if (entryBranchId == null) {
            AppUser currentUser = tenantAccess.requireCurrentUser();
            entryBranchId = currentUser.getBranchId();
        }
        if (entryBranchId == null) {
            entryBranchId = 1L;
        }
        entry.setBranchId(entryBranchId);
        entry.setCreatedBy(username);
        entry.setUpdatedBy(username);

        if (entry.getFuelQuantity() != null && entry.getRatePerLitre() != null) {
            entry.setTotalAmount(entry.getFuelQuantity().multiply(entry.getRatePerLitre()));
        }

        if (req != null) {
            entry.setFuelRequest(req);
            if (entry.getTrip() == null && req.getTrip() != null) {
                entry.setTrip(req.getTrip());
            }
        }

        FuelEntry saved = fuelEntryRepository.save(entry);

        if (req != null) {
            req.setStatus("FULFILLED");
            req.setFulfilledQuantity(saved.getFuelQuantity() != null ? saved.getFuelQuantity() : BigDecimal.ZERO);
            req.setFulfilledAmount(saved.getTotalAmount() != null ? saved.getTotalAmount() : BigDecimal.ZERO);
            req.setFuelEntry(saved);
            req.setUpdatedBy(username);
            requestRepository.save(req);
        }

        auditService.log(username, "FUEL_ENTRY_RECORDED", "fuel_entries", saved.getId(), null,
                "Recorded fuel entry: " + saved.getFuelEntryNumber() + (req != null ? " for request " + req.getRequestNumber() : ""));

        return saved;
    }

    @Transactional
    public FuelEntry approveFuelEntry(Long id, String username) {
        FuelEntry entry = fuelEntryRepository.findAndLockById(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Entry Not Found",
                        "FUEL_ENTRY_NOT_FOUND",
                        "Fuel Entry not found with ID: " + id,
                        "Verify the fuel entry ID."
                ));

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
                    "No further approval action can be taken.",
                    details
            );
        }

        if (!"DRAFT".equalsIgnoreCase(entry.getStatus()) && !"ACTIVE".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' cannot be approved because its status is %s.", entry.getFuelEntryNumber(), entry.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Approval Blocked",
                    "FUEL_ENTRY_STATUS_APPROVAL_BLOCKED",
                    String.format("Fuel Entry '%s' cannot be approved because its status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only DRAFT fuel entries can be approved.",
                    details
            );
        }

        // Validate Financial Year period status
        LocalDate fuelPostingDate = entry.getFuelDate() != null ? entry.getFuelDate() : LocalDate.now();
        periodValidationService.validatePostingAllowed(entry.getCompanyId(), fuelPostingDate);

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
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Entry Not Found",
                        "FUEL_ENTRY_NOT_FOUND",
                        "Fuel entry not found with ID: " + id,
                        "Verify the fuel entry ID."
                ));

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
                    "No further cancellation action permitted.",
                    details
            );
        }

        if (!"APPROVED".equalsIgnoreCase(entry.getStatus()) && !"ACTIVE".equalsIgnoreCase(entry.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Entry '%s' cannot be cancelled because its status is %s.", entry.getFuelEntryNumber(), entry.getStatus()));
            throw new BusinessValidationException(
                    "Fuel Entry Cancellation Blocked",
                    "FUEL_ENTRY_STATUS_CANCELLATION_BLOCKED",
                    String.format("Fuel Entry '%s' cannot be cancelled because its status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only APPROVED fuel entries can be cancelled.",
                    details
            );
        }

        // Validate Financial Year period status
        periodValidationService.validatePostingAllowed(entry.getCompanyId(), LocalDate.now());


        entry.setStatus("CANCELLED");
        entry.setUpdatedBy(username);

        // Revert linked FuelRequest if present
        if (entry.getFuelRequest() != null) {
            FuelRequest req = requestRepository.findByIdForUpdate(entry.getFuelRequest().getId()).orElse(null);
            if (req != null) {
                req.setStatus("APPROVED");
                req.setFulfilledQuantity(BigDecimal.ZERO);
                req.setFulfilledAmount(BigDecimal.ZERO);
                req.setFuelEntry(null);
                req.setUpdatedBy(username);
                requestRepository.save(req);
            }
        }

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
                    String.format("Fuel entry '%s' cannot be modified because its status is %s.", existing.getFuelEntryNumber(), existing.getStatus()),
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
                    String.format("Fuel entry '%s' cannot be deleted because its status is %s.", entry.getFuelEntryNumber(), entry.getStatus()),
                    "Only DRAFT fuel entries can be deleted.",
                    errorDetails
            );
        }

        if (entry.getFuelRequest() != null) {
            FuelRequest req = requestRepository.findByIdForUpdate(entry.getFuelRequest().getId()).orElse(null);
            if (req != null) {
                req.setStatus("APPROVED");
                req.setFulfilledQuantity(BigDecimal.ZERO);
                req.setFulfilledAmount(BigDecimal.ZERO);
                req.setFuelEntry(null);
                req.setUpdatedBy(username);
                requestRepository.save(req);
            }
        }

        entry.setIsDeleted(true);
        entry.setUpdatedBy(username);
        fuelEntryRepository.save(entry);

        auditService.log(username, "FUEL_ENTRY_DELETED", "fuel_entries", entry.getId(), null,
                "Soft deleted fuel entry: " + entry.getFuelEntryNumber());
    }
}
