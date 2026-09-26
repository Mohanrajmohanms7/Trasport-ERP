package com.transport.erp.service;

import com.transport.erp.dto.WorkOrderResponse;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.JournalVoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Posts one maintenance journal voucher when a work order is completed with a positive actual cost.
 *
 * financialAmount = actualCost, scaled HALF_UP to 2 decimal places, when actualCost is greater than zero.
 * Phase 4 and Phase 5 allow completion with a null or zero actual cost. Operational cost is not a
 * financial amount and is never substituted. A null or zero actual cost completes with no journal voucher.
 *
 * The voucher debits maintenance expense 5400 and credits accounts payable 2000, matching the
 * workshop-credit path of vehicle service logs. It is not a cash or bank payment.
 */
@Service
public class WorkOrderFinancialPosting {

    static final String EXPENSE_CODE = "5400";
    static final String EXPENSE_NAME = "Vehicle Repair & Maintenance";
    static final String PAYABLE_CODE = "2000";
    static final String PAYABLE_NAME = "Accounts Payable";
    static final String NOT_POSTED = "NOT_POSTED";
    static final String POSTED = "POSTED";

    @Autowired
    private JournalVoucherRepository journalVoucherRepository;

    @Autowired
    private JournalVoucherService journalVoucherService;

    @Autowired
    private ChartOfAccountService chartOfAccountService;

    @Autowired
    private FinancialYearPeriodValidationService periodValidationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private InventoryValuationService valuationService;

    /**
     * Actual cost = parts taken from our own stock (at cost) + outside cost (labour, workshop, bought-out parts).
     * Parts from stock move from inventory to expense; only the outside part becomes a payable.
     */
    public void postCompletion(WorkOrder order, BigDecimal actualCost, String username) {
        if (actualCost != null && actualCost.compareTo(BigDecimal.ZERO) < 0) {
            throw amountInvalid();
        }
        BigDecimal partsCost = valuationService.netPartsCost(order.getId());
        if (actualCost != null && actualCost.setScale(2, RoundingMode.HALF_UP).compareTo(partsCost) < 0) {
            throw new BusinessValidationException(
                    "Actual Cost Too Low",
                    "WORK_ORDER_COST_BELOW_PARTS",
                    "Actual cost ₹" + actualCost.setScale(2, RoundingMode.HALF_UP).toPlainString()
                            + " is less than the parts issued from stock (₹" + partsCost.toPlainString() + ").",
                    "Enter the full job cost: parts from stock plus labour and outside charges.");
        }
        if (partsCost.signum() > 0) {
            if (findPosted(order, "MAINT-PARTS-" + order.getWorkOrderNumber()) != null) {
                throw alreadyPosted("MAINT-PARTS-" + order.getWorkOrderNumber());
            }
            valuationService.postPartsConsumed(order, partsCost, username);
        }
        BigDecimal financialAmount = financialAmount(actualCost == null ? null : actualCost.subtract(partsCost));
        if (financialAmount == null) {
            return;
        }
        String reference = referenceFor(order);
        if (findPosted(order, reference) != null) {
            throw alreadyPosted(reference);
        }
        periodValidationService.validatePostingAllowed(order.getCompanyId(), LocalDate.now());

        ChartOfAccount expense = chartOfAccountService.getOrCreateAccount(
                order.getCompanyId(), order.getBranchId(), EXPENSE_CODE, EXPENSE_NAME, "EXPENSE");
        ChartOfAccount payable = chartOfAccountService.getOrCreateAccount(
                order.getCompanyId(), order.getBranchId(), PAYABLE_CODE, PAYABLE_NAME, "LIABILITY");

        JournalVoucher voucher = new JournalVoucher();
        voucher.setVoucherDate(LocalDate.now());
        voucher.setDebitAccount(expense);
        voucher.setCreditAccount(payable);
        voucher.setAmount(financialAmount);
        voucher.setReferenceNumber(reference);
        voucher.setDescription(description(order, reference));
        voucher.setCompanyId(order.getCompanyId());
        voucher.setBranchId(order.getBranchId());
        voucher.setCode(reference.length() <= 50 ? reference : reference.substring(0, 50));
        voucher.setName("Maintenance Work Order Voucher");
        voucher.setCreatedBy(username);
        voucher.setUpdatedBy(username);
        JournalVoucher saved = journalVoucherService.createVoucher(voucher, username);
        auditService.log(username, "WORK_ORDER_ACCOUNTING_POSTED", "work_orders", order.getId(), null,
                "reference=" + reference + ", voucherId=" + saved.getId() + ", amount=" + financialAmount);
    }

    public void attachAccounting(WorkOrder order, WorkOrderResponse dto) {
        JournalVoucher posted = findPosted(order, referenceFor(order));
        JournalVoucher parts = findPosted(order, "MAINT-PARTS-" + order.getWorkOrderNumber());
        if (posted == null && parts != null) {
            posted = parts;
            parts = null;
        }
        if (posted != null && parts != null) {
            dto.setAccountingStatus(POSTED);
            dto.setFinancialAmount(posted.getAmount().add(parts.getAmount()));
            dto.setJournalVoucherId(posted.getId());
            dto.setJournalVoucherReference(posted.getReferenceNumber() + ", " + parts.getReferenceNumber());
            dto.setPostedAt(posted.getCreatedDate());
            return;
        }
        if (posted == null) {
            dto.setAccountingStatus(NOT_POSTED);
            dto.setFinancialAmount(null);
            return;
        }
        dto.setAccountingStatus(POSTED);
        dto.setFinancialAmount(posted.getAmount());
        dto.setJournalVoucherId(posted.getId());
        dto.setJournalVoucherReference(posted.getReferenceNumber());
        dto.setPostedAt(posted.getCreatedDate());
    }

    static BigDecimal financialAmount(BigDecimal actualCost) {
        if (actualCost == null || actualCost.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return actualCost.setScale(2, RoundingMode.HALF_UP);
    }

    static String referenceFor(WorkOrder order) {
        return "MAINT-" + order.getWorkOrderNumber();
    }

    private JournalVoucher findPosted(WorkOrder order, String reference) {
        List<JournalVoucher> found = journalVoucherRepository.findByReferenceNumberAndIsDeletedFalse(reference);
        if (found == null) {
            return null;
        }
        return found.stream()
                .filter(voucher -> order.getCompanyId().equals(voucher.getCompanyId()))
                .findFirst()
                .orElse(null);
    }

    private String description(WorkOrder order, String reference) {
        String workshop = order.getSupplier() != null && order.getSupplier().getName() != null
                ? " workshop " + order.getSupplier().getName()
                : "";
        return "Maintenance work order " + reference + workshop;
    }

    private BusinessValidationException amountInvalid() {
        return new BusinessValidationException(
                "Invalid Work Order Amount",
                "WORK_ORDER_ACCOUNTING_AMOUNT_INVALID",
                "WORK_ORDER_ACCOUNTING_AMOUNT_INVALID: Actual cost cannot be negative.",
                "Enter a positive actual cost, or leave it blank to complete without posting.");
    }

    private BusinessValidationException alreadyPosted(String reference) {
        return new BusinessValidationException(
                "Work Order Already Posted",
                "WORK_ORDER_ACCOUNTING_ALREADY_POSTED",
                "WORK_ORDER_ACCOUNTING_ALREADY_POSTED: A journal voucher already exists for " + reference + ".",
                "Refresh the work order. Do not post it again.");
    }
}
