package com.transport.erp.service;

import com.transport.erp.dto.DriverPayrollCreateDTO;
import com.transport.erp.dto.DriverPayrollPaymentDTO;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.Driver;
import com.transport.erp.model.DriverPayroll;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.repository.DriverPayrollRepository;
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
public class DriverPayrollService {

    @Autowired
    private DriverPayrollRepository payrollRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    @Autowired
    private DriverSalaryService driverSalaryService;

    public Page<DriverPayroll> getPayrolls(Long companyId, String status, Pageable pageable) {
        Long resolvedCompanyId = tenantAccess.resolveCompanyId(companyId);
        if (status != null && !status.trim().isEmpty()) {
            return payrollRepository.findByCompanyIdAndStatusAndIsDeletedFalse(resolvedCompanyId, status, pageable);
        }
        return payrollRepository.findByCompanyIdAndIsDeletedFalse(resolvedCompanyId, pageable);
    }

    public DriverPayroll getPayrollById(Long id) {
        DriverPayroll payroll = payrollRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());
        return payroll;
    }

    public List<DriverPayroll> getPayrollsByDriver(Long driverId) {
        parentAccess.requireDriver(driverId);
        return payrollRepository.findByDriverIdAndIsDeletedFalse(driverId);
    }

    @Transactional
    public DriverPayroll createPayroll(DriverPayrollCreateDTO dto, String username) {
        if (dto.getDriverId() == null) {
            throw new BusinessValidationException(
                    "Missing Driver",
                    "INVALID_DRIVER",
                    "Driver ID is required for payroll processing.",
                    "Select a valid driver."
            );
        }

        Driver driver = parentAccess.requireDriver(dto.getDriverId());

        if (dto.getPayYear() == null || dto.getPayYear() < 2000) {
            throw new BusinessValidationException(
                    "Invalid Pay Year",
                    "INVALID_PAY_YEAR",
                    "Pay year must be a valid year (>= 2000).",
                    "Enter a valid pay year."
            );
        }

        if (dto.getPayMonth() == null || dto.getPayMonth() < 1 || dto.getPayMonth() > 12) {
            throw new BusinessValidationException(
                    "Invalid Pay Month",
                    "INVALID_PAY_MONTH",
                    "Pay month must be between 1 and 12.",
                    "Enter a valid pay month."
            );
        }

        Optional<DriverPayroll> existing = payrollRepository.findByDriverIdAndPayYearAndPayMonthAndIsDeletedFalse(
                dto.getDriverId(), dto.getPayYear(), dto.getPayMonth());
        if (existing.isPresent()) {
            throw new BusinessValidationException(
                    "Duplicate Pay Period",
                    "PAYROLL_DUPLICATE_PAY_PERIOD",
                    String.format("Payroll already exists for Driver '%s' for %d-%02d.",
                            driver.getName(), dto.getPayYear(), dto.getPayMonth()),
                    "Use the existing payroll transaction or process a correction workflow."
            );
        }

        BigDecimal basicSalary = dto.getBasicSalary();
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) == 0) {
            basicSalary = driverSalaryService.getSalaryByDriver(dto.getDriverId())
                    .map(s -> s.getBasicSalary())
                    .orElse(BigDecimal.ZERO);
        }

        BigDecimal allowance = dto.getAllowanceAmount() != null ? dto.getAllowanceAmount() : BigDecimal.ZERO;
        BigDecimal deduction = dto.getDeductionAmount() != null ? dto.getDeductionAmount() : BigDecimal.ZERO;
        BigDecimal advanceAdj = dto.getAdvanceAdjustment() != null ? dto.getAdvanceAdjustment() : BigDecimal.ZERO;

        BigDecimal netSalary = basicSalary.add(allowance).subtract(deduction).subtract(advanceAdj);

        if (netSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Payroll Amount",
                    "INVALID_PAYROLL_AMOUNT",
                    String.format("Payroll net salary payable must be greater than 0, calculated amount: ₹%s", netSalary.toPlainString()),
                    "Adjust basic salary, allowances, or deductions to yield a positive net salary."
            );
        }

        Long resolvedBranchId = driver.getBranchId();

        DriverPayroll payroll = new DriverPayroll();
        payroll.setDriver(driver);
        payroll.setPayYear(dto.getPayYear());
        payroll.setPayMonth(dto.getPayMonth());
        payroll.setBasicSalary(basicSalary);
        payroll.setAllowanceAmount(allowance);
        payroll.setDeductionAmount(deduction);
        payroll.setAdvanceAdjustment(advanceAdj);
        payroll.setNetSalaryPayable(netSalary);
        payroll.setStatus("DRAFT");
        payroll.setIsDeleted(false);
        payroll.setCompanyId(driver.getCompanyId());
        payroll.setBranchId(resolvedBranchId);
        payroll.setCode("PAY_" + driver.getId() + "_" + dto.getPayYear() + "_" + dto.getPayMonth());
        payroll.setName("Driver Payroll - " + driver.getName() + " (" + dto.getPayYear() + "-" + String.format("%02d", dto.getPayMonth()) + ")");
        payroll.setDescription(dto.getDescription() != null ? dto.getDescription() : "Driver monthly salary payroll");
        payroll.setCreatedBy(username);
        payroll.setUpdatedBy(username);

        String payrollNum = String.format("PAY-%d-%02d-DR%d-%d", dto.getPayYear(), dto.getPayMonth(), driver.getId(), System.currentTimeMillis() % 100000);
        payroll.setPayrollNumber(payrollNum);

        DriverPayroll saved = payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_CREATED", "driver_payrolls", saved.getId(), null,
                "Created DRAFT driver payroll: " + saved.getPayrollNumber());

        return saved;
    }

    @Transactional
    public DriverPayroll updatePayroll(Long id, DriverPayrollCreateDTO dto, String username) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());

        if (!"DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Editing Blocked",
                    "PAYROLL_STATUS_UPDATE_BLOCKED",
                    String.format("Payroll %s is in %s status and cannot be edited.", payroll.getPayrollNumber(), payroll.getStatus()),
                    "Only DRAFT payrolls can be modified."
            );
        }

        BigDecimal basicSalary = dto.getBasicSalary() != null ? dto.getBasicSalary() : payroll.getBasicSalary();
        BigDecimal allowance = dto.getAllowanceAmount() != null ? dto.getAllowanceAmount() : payroll.getAllowanceAmount();
        BigDecimal deduction = dto.getDeductionAmount() != null ? dto.getDeductionAmount() : payroll.getDeductionAmount();
        BigDecimal advanceAdj = dto.getAdvanceAdjustment() != null ? dto.getAdvanceAdjustment() : payroll.getAdvanceAdjustment();

        BigDecimal netSalary = basicSalary.add(allowance).subtract(deduction).subtract(advanceAdj);

        if (netSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Payroll Amount",
                    "INVALID_PAYROLL_AMOUNT",
                    String.format("Payroll net salary payable must be greater than 0, calculated amount: ₹%s", netSalary.toPlainString()),
                    "Adjust basic salary, allowances, or deductions to yield a positive net salary."
            );
        }

        payroll.setBasicSalary(basicSalary);
        payroll.setAllowanceAmount(allowance);
        payroll.setDeductionAmount(deduction);
        payroll.setAdvanceAdjustment(advanceAdj);
        payroll.setNetSalaryPayable(netSalary);

        if (dto.getDescription() != null) {
            payroll.setDescription(dto.getDescription());
        }
        payroll.setUpdatedBy(username);

        DriverPayroll saved = payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_UPDATED", "driver_payrolls", saved.getId(), null,
                "Updated DRAFT driver payroll: " + saved.getPayrollNumber());

        return saved;
    }

    @Transactional
    public void deletePayroll(Long id, String username) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());

        if (!"DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Deletion Blocked",
                    "PAYROLL_STATUS_UPDATE_BLOCKED",
                    String.format("Payroll %s is in %s status and cannot be deleted.", payroll.getPayrollNumber(), payroll.getStatus()),
                    "Only DRAFT payrolls can be deleted."
            );
        }

        payroll.setIsDeleted(true);
        payroll.setUpdatedBy(username);
        payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_DELETED", "driver_payrolls", id, null,
                "Deleted DRAFT driver payroll: " + payroll.getPayrollNumber());
    }

    @Transactional
    public DriverPayroll approvePayroll(Long id, String username) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());

        if ("APPROVED".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Already Approved",
                    "PAYROLL_ALREADY_APPROVED",
                    String.format("Payroll %s is already approved.", payroll.getPayrollNumber()),
                    "Use payment or cancellation workflow."
            );
        }
        if ("PAID".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Already Paid",
                    "PAYROLL_ALREADY_PAID",
                    String.format("Payroll %s is already paid.", payroll.getPayrollNumber()),
                    "Do not modify financial records; use cancellation if required."
            );
        }
        if ("CANCELLED".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Cancelled",
                    "PAYROLL_ALREADY_CANCELLED",
                    String.format("Payroll %s is cancelled.", payroll.getPayrollNumber()),
                    "Create a new payroll transaction if required."
            );
        }

        if (!"DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Approval Blocked",
                    "PAYROLL_STATUS_UPDATE_BLOCKED",
                    String.format("Payroll %s status %s cannot be approved.", payroll.getPayrollNumber(), payroll.getStatus()),
                    "Only DRAFT payrolls can be approved."
            );
        }

        if (payroll.getNetSalaryPayable().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Payroll Amount",
                    "INVALID_PAYROLL_AMOUNT",
                    "Cannot approve payroll with net salary <= 0.",
                    "Ensure net salary is positive."
            );
        }

        ChartOfAccount salaryExpenseAcc = coaService.getOrCreateAccount(
                payroll.getCompanyId(), payroll.getBranchId(), "5150", "Driver Salary Expense", "EXPENSE");
        ChartOfAccount salaryPayableAcc = coaService.getOrCreateAccount(
                payroll.getCompanyId(), payroll.getBranchId(), "2050", "Driver Salary Payable", "LIABILITY");

        String accrualRef = "SAL-ACC-PAY-" + payroll.getId();
        Optional<JournalVoucher> existingJv = jvRepository.findByReferenceNumberAndIsDeletedFalse(accrualRef).stream().findFirst();

        JournalVoucher jv;
        if (existingJv.isPresent()) {
            jv = existingJv.get();
        } else {
            jv = new JournalVoucher();
            jv.setVoucherNumber("JV-SAL-ACC-" + payroll.getId());
            jv.setVoucherDate(LocalDate.now());
            jv.setReferenceNumber(accrualRef);
            jv.setDebitAccount(salaryExpenseAcc);
            jv.setCreditAccount(salaryPayableAcc);
            jv.setAmount(payroll.getNetSalaryPayable());
            jv.setDescription("Auto-posted driver salary accrual JV for payroll " + payroll.getPayrollNumber());
            jv.setCompanyId(payroll.getCompanyId());
            jv.setBranchId(payroll.getBranchId());
            jv.setCode(accrualRef);
            jv.setName("Salary Accrual JV");
            jv.setStatus("POSTED");
            jv.setIsDeleted(false);
            jv.setCreatedBy(username);
            jv.setUpdatedBy(username);

            jv = jvRepository.save(jv);
        }

        payroll.setStatus("APPROVED");
        payroll.setAccrualJvNumber(jv.getVoucherNumber());
        payroll.setUpdatedBy(username);

        DriverPayroll saved = payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_APPROVED", "driver_payrolls", saved.getId(), null,
                "Approved driver payroll " + saved.getPayrollNumber() + " with JV " + jv.getVoucherNumber());

        return saved;
    }

    @Transactional
    public DriverPayroll payPayroll(Long id, DriverPayrollPaymentDTO paymentDto, String username) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());

        if ("DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payment Blocked",
                    "PAYROLL_PAYMENT_STATUS_BLOCKED",
                    String.format("Payroll %s is in DRAFT status and must be APPROVED before payment.", payroll.getPayrollNumber()),
                    "Approve the payroll before recording payment."
            );
        }
        if ("PAID".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Already Paid",
                    "PAYROLL_ALREADY_PAID",
                    String.format("Payroll %s is already paid.", payroll.getPayrollNumber()),
                    "No action required."
            );
        }
        if ("CANCELLED".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Cancelled",
                    "PAYROLL_ALREADY_CANCELLED",
                    String.format("Payroll %s is cancelled.", payroll.getPayrollNumber()),
                    "Cannot pay a cancelled payroll."
            );
        }
        if (!"APPROVED".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payment Blocked",
                    "PAYROLL_PAYMENT_STATUS_BLOCKED",
                    String.format("Payroll %s status %s cannot be paid.", payroll.getPayrollNumber(), payroll.getStatus()),
                    "Only APPROVED payrolls can be paid."
            );
        }

        String pMethod = paymentDto != null && paymentDto.getPaymentMethod() != null ? paymentDto.getPaymentMethod().toUpperCase() : "CASH";
        if (!List.of("CASH", "BANK_TRANSFER", "CHEQUE", "UPI").contains(pMethod)) {
            throw new BusinessValidationException(
                    "Invalid Payment Method",
                    "INVALID_PAYMENT_METHOD",
                    "Payment method '" + pMethod + "' is not supported for driver payroll.",
                    "Use CASH, BANK_TRANSFER, CHEQUE, or UPI."
            );
        }

        ChartOfAccount salaryPayableAcc = coaService.getOrCreateAccount(
                payroll.getCompanyId(), payroll.getBranchId(), "2050", "Driver Salary Payable", "LIABILITY");

        ChartOfAccount creditAcc;
        if ("CASH".equalsIgnoreCase(pMethod)) {
            creditAcc = coaService.getOrCreateAccount(payroll.getCompanyId(), payroll.getBranchId(), "1000", "Cash on Hand", "ASSET");
        } else {
            creditAcc = coaService.getOrCreateAccount(payroll.getCompanyId(), payroll.getBranchId(), "1010", "Bank - Current A/c", "ASSET");
        }

        String paymentRef = "SAL-PAY-PAY-" + payroll.getId();
        Optional<JournalVoucher> existingJv = jvRepository.findByReferenceNumberAndIsDeletedFalse(paymentRef).stream().findFirst();

        JournalVoucher jv;
        if (existingJv.isPresent()) {
            jv = existingJv.get();
        } else {
            jv = new JournalVoucher();
            jv.setVoucherNumber("JV-SAL-PAY-" + payroll.getId());
            jv.setVoucherDate(LocalDate.now());
            jv.setReferenceNumber(paymentRef);
            jv.setDebitAccount(salaryPayableAcc);
            jv.setCreditAccount(creditAcc);
            jv.setAmount(payroll.getNetSalaryPayable());
            jv.setDescription("Auto-posted driver salary payment JV for payroll " + payroll.getPayrollNumber());
            jv.setCompanyId(payroll.getCompanyId());
            jv.setBranchId(payroll.getBranchId());
            jv.setCode(paymentRef);
            jv.setName("Salary Payment JV");
            jv.setStatus("POSTED");
            jv.setIsDeleted(false);
            jv.setCreatedBy(username);
            jv.setUpdatedBy(username);

            jv = jvRepository.save(jv);
        }

        payroll.setStatus("PAID");
        payroll.setPaymentMethod(pMethod);
        payroll.setPaymentJvNumber(jv.getVoucherNumber());
        payroll.setUpdatedBy(username);

        DriverPayroll saved = payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_PAID", "driver_payrolls", saved.getId(), null,
                "Paid driver payroll " + saved.getPayrollNumber() + " with JV " + jv.getVoucherNumber());

        return saved;
    }

    @Transactional
    public DriverPayroll cancelPayroll(Long id, String username) {
        DriverPayroll payroll = payrollRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Payroll Not Found",
                        "PAYROLL_NOT_FOUND",
                        "Driver payroll not found with ID: " + id,
                        "Verify the payroll ID."
                ));
        tenantAccess.assertOwned(payroll.getCompanyId());

        if ("CANCELLED".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Payroll Already Cancelled",
                    "PAYROLL_ALREADY_CANCELLED",
                    String.format("Payroll %s is already cancelled.", payroll.getPayrollNumber()),
                    "No further action permitted."
            );
        }

        if ("DRAFT".equalsIgnoreCase(payroll.getStatus())) {
            throw new BusinessValidationException(
                    "Cancellation Blocked",
                    "PAYROLL_STATUS_UPDATE_BLOCKED",
                    String.format("Payroll %s is in DRAFT status. Delete the draft payroll instead of cancelling.", payroll.getPayrollNumber()),
                    "Use delete endpoint for DRAFT payrolls."
            );
        }

        List<String> cancellationJvNumbers = new ArrayList<>();

        if ("PAID".equalsIgnoreCase(payroll.getStatus())) {
            // Reverse Payment JV
            String origPayRef = "SAL-PAY-PAY-" + payroll.getId();
            String revPayRef = "REV-SAL-PAY-PAY-" + payroll.getId();

            JournalVoucher origPayJv = jvRepository.findByReferenceNumberAndIsDeletedFalse(origPayRef).stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessValidationException(
                            "Original Payment JV Missing",
                            "PAYROLL_HAS_ACCOUNTING",
                            "Original payment JV not found for payroll " + payroll.getPayrollNumber(),
                            "Contact administrator."
                    ));

            Optional<JournalVoucher> existingRevPay = jvRepository.findByReferenceNumberAndIsDeletedFalse(revPayRef).stream().findFirst();
            JournalVoucher revPayJv;
            if (existingRevPay.isPresent()) {
                revPayJv = existingRevPay.get();
            } else {
                revPayJv = new JournalVoucher();
                revPayJv.setVoucherNumber("REV-JV-SAL-PAY-" + payroll.getId());
                revPayJv.setVoucherDate(LocalDate.now());
                revPayJv.setReferenceNumber(revPayRef);
                revPayJv.setDebitAccount(origPayJv.getCreditAccount()); // Cash/Bank
                revPayJv.setCreditAccount(origPayJv.getDebitAccount()); // Payable
                revPayJv.setAmount(origPayJv.getAmount());
                revPayJv.setDescription("Reversal of payment JV " + origPayJv.getVoucherNumber() + " for cancelled driver payroll " + payroll.getPayrollNumber());
                revPayJv.setCompanyId(payroll.getCompanyId());
                revPayJv.setBranchId(payroll.getBranchId());
                revPayJv.setCode(revPayRef);
                revPayJv.setName("Reversal Salary Payment JV");
                revPayJv.setStatus("POSTED");
                revPayJv.setIsDeleted(false);
                revPayJv.setCreatedBy(username);
                revPayJv.setUpdatedBy(username);

                revPayJv = jvRepository.save(revPayJv);
            }
            cancellationJvNumbers.add(revPayJv.getVoucherNumber());
        }

        // Reverse Accrual JV (for both APPROVED and PAID cancellations)
        String origAccRef = "SAL-ACC-PAY-" + payroll.getId();
        String revAccRef = "REV-SAL-ACC-PAY-" + payroll.getId();

        JournalVoucher origAccJv = jvRepository.findByReferenceNumberAndIsDeletedFalse(origAccRef).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessValidationException(
                        "Original Accrual JV Missing",
                        "PAYROLL_HAS_ACCOUNTING",
                        "Original accrual JV not found for payroll " + payroll.getPayrollNumber(),
                        "Contact administrator."
                ));

        Optional<JournalVoucher> existingRevAcc = jvRepository.findByReferenceNumberAndIsDeletedFalse(revAccRef).stream().findFirst();
        JournalVoucher revAccJv;
        if (existingRevAcc.isPresent()) {
            revAccJv = existingRevAcc.get();
        } else {
            revAccJv = new JournalVoucher();
            revAccJv.setVoucherNumber("REV-JV-SAL-ACC-" + payroll.getId());
            revAccJv.setVoucherDate(LocalDate.now());
            revAccJv.setReferenceNumber(revAccRef);
            revAccJv.setDebitAccount(origAccJv.getCreditAccount()); // Payable
            revAccJv.setCreditAccount(origAccJv.getDebitAccount()); // Expense
            revAccJv.setAmount(origAccJv.getAmount());
            revAccJv.setDescription("Reversal of accrual JV " + origAccJv.getVoucherNumber() + " for cancelled driver payroll " + payroll.getPayrollNumber());
            revAccJv.setCompanyId(payroll.getCompanyId());
            revAccJv.setBranchId(payroll.getBranchId());
            revAccJv.setCode(revAccRef);
            revAccJv.setName("Reversal Salary Accrual JV");
            revAccJv.setStatus("POSTED");
            revAccJv.setIsDeleted(false);
            revAccJv.setCreatedBy(username);
            revAccJv.setUpdatedBy(username);

            revAccJv = jvRepository.save(revAccJv);
        }
        cancellationJvNumbers.add(revAccJv.getVoucherNumber());

        payroll.setStatus("CANCELLED");
        payroll.setCancellationJvNumber(String.join(",", cancellationJvNumbers));
        payroll.setUpdatedBy(username);

        DriverPayroll saved = payrollRepository.save(payroll);

        auditService.log(username, "DRIVER_PAYROLL_CANCELLED", "driver_payrolls", saved.getId(), null,
                "Cancelled driver payroll " + saved.getPayrollNumber() + " with reversal JVs: " + saved.getCancellationJvNumber());

        return saved;
    }
}
