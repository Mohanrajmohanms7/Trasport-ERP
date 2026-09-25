package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.repository.SupplierRepository;
import com.transport.erp.repository.VehicleServiceLogRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleServiceLogService {

    @Autowired
    private VehicleServiceLogRepository logRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private SupplierRepository supplierRepository;

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
    private BusinessDependencyValidationService dependencyValidationService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;



    public List<VehicleServiceLog> getLogsByVehicle(Long vehicleId) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        tenantAccess.assertOwned(vehicle.getCompanyId());
        return logRepository.findByVehicleIdAndIsDeletedFalse(vehicleId);
    }

    public VehicleServiceLog getLogById(Long vehicleId, Long logId) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        tenantAccess.assertOwned(vehicle.getCompanyId());

        VehicleServiceLog log = logRepository.findById(logId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle service log not found with ID: " + logId));

        if (!log.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service log does not belong to vehicle: " + vehicleId);
        }

        return log;
    }

    @Transactional
    public VehicleServiceLog addLog(Long vehicleId, VehicleServiceLog log, String username) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        tenantAccess.assertOwned(vehicle.getCompanyId());

        if (log.getCost() == null || log.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            List<String> details = new ArrayList<>();
            details.add("Repairs cost must be greater than 0.");
            throw new BusinessValidationException(
                    "Invalid Service Cost",
                    "INVALID_SERVICE_COST",
                    "Service cost must be greater than zero.",
                    "Provide a valid positive cost amount for the service log.",
                    details
            );
        }

        String payMethod = log.getPaymentMethod() != null ? log.getPaymentMethod().toUpperCase() : "CASH";
        if (!"CASH".equals(payMethod) && !"BANK".equals(payMethod) && !"CREDIT".equals(payMethod)) {
            List<String> details = new ArrayList<>();
            details.add("Invalid payment method: " + log.getPaymentMethod() + ". Allowed: CASH, BANK, CREDIT.");
            throw new BusinessValidationException(
                    "Invalid Payment Method",
                    "INVALID_PAYMENT_METHOD",
                    "Payment method '" + log.getPaymentMethod() + "' is not supported.",
                    "Select a valid payment method (CASH, BANK, or CREDIT).",
                    details
            );
        }

        if ("CREDIT".equals(payMethod)) {
            if (log.getSupplier() == null || log.getSupplier().getId() == null) {
                List<String> details = new ArrayList<>();
                details.add("Supplier/Workshop is required when payment method is CREDIT.");
                throw new BusinessValidationException(
                        "Supplier Required for Credit",
                        "SUPPLIER_REQUIRED_FOR_CREDIT",
                        "Supplier must be specified for credit service transactions.",
                        "Select a valid supplier/workshop for credit payment.",
                        details
                );
            }
            Supplier supplier = supplierRepository.findById(log.getSupplier().getId())
                    .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + log.getSupplier().getId()));
            tenantAccess.assertOwned(supplier.getCompanyId());
            log.setSupplier(supplier);
        }

        Long branchId = vehicle.getBranchId() != null ? vehicle.getBranchId() : vehicle.getCompanyId();
        log.setVehicle(vehicle);
        log.setStatus("DRAFT");
        log.setPaymentMethod(payMethod);
        log.setIsDeleted(false);
        if (log.getReferenceNumber() == null || log.getReferenceNumber().trim().isEmpty()) {
            log.setReferenceNumber("SRV-" + System.currentTimeMillis());
        }
        log.setCode(log.getReferenceNumber());
        log.setName(log.getServiceType() + " Maintenance");
        log.setCompanyId(vehicle.getCompanyId());
        log.setBranchId(branchId);
        log.setCreatedBy(username);
        log.setUpdatedBy(username);

        VehicleServiceLog saved = logRepository.save(log);

        auditService.log(username, "VEHICLE_SERVICE_LOGGED", "vehicle_services", saved.getId(), null,
                "Logged vehicle service: " + saved.getReferenceNumber() + " for vehicle: " + vehicle.getCode());

        return saved;
    }

    @Transactional
    public VehicleServiceLog approveLog(Long vehicleId, Long logId, String username) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        tenantAccess.assertOwned(vehicle.getCompanyId());

        VehicleServiceLog log = logRepository.findAndLockById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle service log not found with ID: " + logId));

        if (!log.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service log does not belong to vehicle: " + vehicleId);
        }

        if ("APPROVED".equalsIgnoreCase(log.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle Service Log '%s' is already APPROVED.", log.getReferenceNumber()));
            throw new BusinessValidationException(
                    "Service Log Already Approved",
                    "VEHICLE_SERVICE_ALREADY_APPROVED",
                    String.format("Vehicle Service Log '%s' is already APPROVED.", log.getReferenceNumber()),
                    "No further approval action can be taken on an approved service log.",
                    details
            );
        }

        if (!"DRAFT".equalsIgnoreCase(log.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle Service Log '%s' status is %s.", log.getReferenceNumber(), log.getStatus()));
            throw new BusinessValidationException(
                    "Service Log Approval Blocked",
                    "VEHICLE_SERVICE_STATUS_APPROVAL_BLOCKED",
                    String.format("Vehicle Service Log '%s' cannot be approved because its current status is %s.", log.getReferenceNumber(), log.getStatus()),
                    "Only DRAFT vehicle service logs can be approved.",
                    details
            );
        }

        if (log.getCost() == null || log.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            List<String> details = new ArrayList<>();
            details.add("Service cost must be greater than zero for accounting posting.");
            throw new BusinessValidationException(
                    "Invalid Service Amount",
                    "INVALID_SERVICE_AMOUNT",
                    "Service cost must be greater than zero.",
                    "Ensure repair cost is positive before approving.",
                    details
            );
        }

        String payMethod = log.getPaymentMethod() != null ? log.getPaymentMethod().toUpperCase() : "CASH";
        if ("CREDIT".equals(payMethod) && (log.getSupplier() == null || log.getSupplier().getId() == null)) {
            List<String> details = new ArrayList<>();
            details.add("Supplier is required for credit payment approval.");
            throw new BusinessValidationException(
                    "Supplier Required for Credit",
                    "SUPPLIER_REQUIRED_FOR_CREDIT",
                    "Supplier must be specified for credit service transactions.",
                    "Select a valid supplier/workshop before approving.",
                    details
            );
        }

        // Validate Financial Year period status
        LocalDate servicePostingDate = log.getServiceDate() != null ? log.getServiceDate() : LocalDate.now();
        periodValidationService.validatePostingAllowed(log.getCompanyId(), servicePostingDate);

        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(log.getReferenceNumber());

        if (existingJvs != null && !existingJvs.isEmpty()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Accounting vouchers already exist for vehicle service log '%s'.", log.getReferenceNumber()));
            throw new BusinessValidationException(
                    "Duplicate Accounting Blocked",
                    "VEHICLE_SERVICE_ACCOUNTING_EXISTS",
                    String.format("Accounting entry already exists for service log '%s'.", log.getReferenceNumber()),
                    "Vehicle service accounting has already been posted.",
                    details
            );
        }

        log.setStatus("APPROVED");
        log.setUpdatedBy(username);
        VehicleServiceLog saved = logRepository.save(log);

        // Double-entry GL posting
        // Debit: 5400 Vehicle Repair & Maintenance Expense
        ChartOfAccount repairExpenseAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "5400", "Vehicle Repair & Maintenance", "EXPENSE");
        ChartOfAccount creditAcc;

        if ("CASH".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1000", "Cash on Hand", "ASSET");
        } else if ("CREDIT".equals(payMethod)) {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "2000", "Accounts Payable", "LIABILITY");
        } else {
            creditAcc = coaService.getOrCreateAccount(saved.getCompanyId(), saved.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        }

        Long effectiveBranchId = saved.getBranchId() != null ? saved.getBranchId() : saved.getCompanyId();
        BigDecimal amount = saved.getCost();
        JournalVoucher jv = new JournalVoucher();
        jv.setVoucherNumber("JV-SRV-" + saved.getId());
        jv.setVoucherDate(servicePostingDate);
        jv.setDebitAccount(repairExpenseAcc);
        jv.setCreditAccount(creditAcc);
        jv.setAmount(amount);
        jv.setReferenceNumber(saved.getReferenceNumber());
        jv.setDescription("Auto-posted vehicle maintenance service JV for " + saved.getReferenceNumber());
        jv.setCompanyId(saved.getCompanyId());
        jv.setBranchId(effectiveBranchId);
        jv.setIsDeleted(false);
        jv.setCreatedBy(username);
        jv.setUpdatedBy(username);
        jv.setCode(jv.getVoucherNumber());
        jv.setName("Vehicle Service Voucher");
        jvService.createVoucher(jv, username);

        auditService.log(username, "VEHICLE_SERVICE_APPROVED", "vehicle_services", saved.getId(), null,
                "Approved vehicle service: " + saved.getReferenceNumber() + " with JV: " + jv.getVoucherNumber());

        return saved;
    }

    @Transactional
    public VehicleServiceLog cancelLog(Long vehicleId, Long logId, String username) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        tenantAccess.assertOwned(vehicle.getCompanyId());

        VehicleServiceLog log = logRepository.findAndLockById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle service log not found with ID: " + logId));

        if (!log.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service log does not belong to vehicle: " + vehicleId);
        }

        if ("CANCELLED".equalsIgnoreCase(log.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle Service Log '%s' is already CANCELLED.", log.getReferenceNumber()));
            throw new BusinessValidationException(
                    "Service Log Already Cancelled",
                    "VEHICLE_SERVICE_ALREADY_CANCELLED",
                    String.format("Vehicle Service Log '%s' is already CANCELLED.", log.getReferenceNumber()),
                    "No further cancellation action can be taken on a cancelled service log.",
                    details
            );
        }

        if (!"APPROVED".equalsIgnoreCase(log.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle Service Log '%s' status is %s.", log.getReferenceNumber(), log.getStatus()));
            throw new BusinessValidationException(
                    "Service Log Cancellation Blocked",
                    "VEHICLE_SERVICE_STATUS_CANCELLATION_BLOCKED",
                    String.format("Vehicle Service Log '%s' cannot be cancelled because its current status is %s.", log.getReferenceNumber(), log.getStatus()),
                    "Only APPROVED vehicle service logs can be cancelled.",
                    details
            );
        }

        // Validate Financial Year period status
        periodValidationService.validatePostingAllowed(log.getCompanyId(), LocalDate.now());


        // 3. locate original approval JV
        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(log.getReferenceNumber());

        // 4. verify reversal does not already exist
        List<JournalVoucher> existingReversals = jvRepository.findByReferenceNumberAndIsDeletedFalse("REV-" + log.getReferenceNumber());
        if (existingReversals != null && !existingReversals.isEmpty()) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Reversal JV already exists for vehicle service log '%s'.", log.getReferenceNumber()));
            throw new BusinessValidationException(
                    "Reversal Already Exists",
                    "VEHICLE_SERVICE_REVERSAL_EXISTS",
                    String.format("Reversal accounting entry already exists for service log '%s'.", log.getReferenceNumber()),
                    "Vehicle service cancellation reversal has already been posted.",
                    details
            );
        }

        // 5. create reversal JV
        Long effectiveBranchIdRev = log.getBranchId() != null ? log.getBranchId() : (log.getVehicle() != null && log.getVehicle().getBranchId() != null ? log.getVehicle().getBranchId() : log.getCompanyId());
        if (existingJvs != null) {
            for (JournalVoucher origJv : existingJvs) {
                JournalVoucher revJv = new JournalVoucher();
                revJv.setVoucherNumber("REV-JV-SRV-" + log.getId() + "-" + origJv.getId());
                revJv.setVoucherDate(LocalDate.now());
                revJv.setDebitAccount(origJv.getCreditAccount());
                revJv.setCreditAccount(origJv.getDebitAccount());
                revJv.setAmount(origJv.getAmount());
                revJv.setReferenceNumber("REV-" + origJv.getReferenceNumber());
                revJv.setDescription("Reversal of JV " + origJv.getVoucherNumber() + " for cancelled vehicle service " + log.getReferenceNumber());
                revJv.setCompanyId(log.getCompanyId());
                revJv.setBranchId(effectiveBranchIdRev);
                revJv.setIsDeleted(false);
                revJv.setCreatedBy(username);
                revJv.setUpdatedBy(username);
                revJv.setCode(revJv.getVoucherNumber());
                revJv.setName("Vehicle Service Reversal Voucher");
                jvService.createVoucher(revJv, username);
            }
        }

        // 6. only after successful reversal creation, set service log status to CANCELLED
        log.setStatus("CANCELLED");
        log.setUpdatedBy(username);
        VehicleServiceLog saved = logRepository.save(log);

        auditService.log(username, "VEHICLE_SERVICE_CANCELLED", "vehicle_services", saved.getId(), null,
                "Cancelled vehicle service: " + saved.getReferenceNumber());

        return saved;
    }

    @Transactional
    public VehicleServiceLog updateLog(Long vehicleId, Long logId, VehicleServiceLog details, String username) {
        VehicleServiceLog existing = getLogById(vehicleId, logId);

        if ("APPROVED".equalsIgnoreCase(existing.getStatus()) || "CANCELLED".equalsIgnoreCase(existing.getStatus())) {
            List<String> errorDetails = new ArrayList<>();
            errorDetails.add(String.format("Vehicle service log '%s' is in %s status.", existing.getReferenceNumber(), existing.getStatus()));
            throw new BusinessValidationException(
                    "Service Log Update Blocked",
                    "VEHICLE_SERVICE_STATUS_UPDATE_BLOCKED",
                    String.format("Vehicle service log '%s' cannot be modified because its current status is %s.", existing.getReferenceNumber(), existing.getStatus()),
                    "Only DRAFT vehicle service logs can be modified.",
                    errorDetails
            );
        }

        if (details.getCost() != null && details.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            List<String> errs = new ArrayList<>();
            errs.add("Repairs cost must be greater than 0.");
            throw new BusinessValidationException(
                    "Invalid Service Cost",
                    "INVALID_SERVICE_COST",
                    "Service cost must be greater than zero.",
                    "Provide a valid positive cost amount.",
                    errs
            );
        }

        if (details.getPaymentMethod() != null) {
            String payMethod = details.getPaymentMethod().toUpperCase();
            if (!"CASH".equals(payMethod) && !"BANK".equals(payMethod) && !"CREDIT".equals(payMethod)) {
                List<String> errs = new ArrayList<>();
                errs.add("Invalid payment method: " + details.getPaymentMethod());
                throw new BusinessValidationException(
                        "Invalid Payment Method",
                        "INVALID_PAYMENT_METHOD",
                        "Payment method '" + details.getPaymentMethod() + "' is not supported.",
                        "Select a valid payment method (CASH, BANK, or CREDIT).",
                        errs
                );
            }
            existing.setPaymentMethod(payMethod);
        }

        if ("CREDIT".equalsIgnoreCase(existing.getPaymentMethod())) {
            if (details.getSupplier() != null && details.getSupplier().getId() != null) {
                Supplier supplier = supplierRepository.findById(details.getSupplier().getId())
                        .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                        .orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + details.getSupplier().getId()));
                tenantAccess.assertOwned(supplier.getCompanyId());
                existing.setSupplier(supplier);
            }
        }

        existing.setServiceType(details.getServiceType());
        existing.setServiceDate(details.getServiceDate());
        existing.setNextServiceDate(details.getNextServiceDate());
        existing.setWorkshop(details.getWorkshop());
        if (details.getCost() != null) {
            existing.setCost(details.getCost());
        }
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        VehicleServiceLog saved = logRepository.save(existing);

        auditService.log(username, "VEHICLE_SERVICE_UPDATED", "vehicle_services", saved.getId(), null,
                "Updated vehicle service log: " + saved.getReferenceNumber());

        return saved;
    }

    @Transactional
    public void deleteLog(Long vehicleId, Long logId, String username) {
        VehicleServiceLog log = getLogById(vehicleId, logId);

        dependencyValidationService.validateVehicleServiceLogDelete(logId);

        log.setIsDeleted(true);
        log.setUpdatedBy(username);
        logRepository.save(log);

        auditService.log(username, "VEHICLE_SERVICE_DELETED", "vehicle_services", log.getId(), null,
                "Soft deleted vehicle service log: " + log.getReferenceNumber());
    }
}
