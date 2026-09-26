package com.transport.erp.service;

import com.transport.erp.model.*;
import com.transport.erp.repository.InventoryTransactionRepository;
import com.transport.erp.repository.WarehouseStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Stock value and its accounting.
 *
 * Moving average cost per warehouse + part. Accounting (only when a cost is known):
 *   Receipt          Dr 1200 Spare Parts Inventory  / Cr 2000 Accounts Payable
 *   Opening balance  Dr 1200 Spare Parts Inventory  / Cr 3900 Opening Balance Equity
 *   Work order done  Dr 5400 Repair & Maintenance   / Cr 1200 Spare Parts Inventory   (net parts issued, at cost)
 *                    Dr 5400 Repair & Maintenance   / Cr 2000 Accounts Payable        (outside labour / workshop)
 */
@Service
public class InventoryValuationService {

    static final String INVENTORY_CODE = "1200";
    static final String INVENTORY_NAME = "Spare Parts Inventory";
    static final String OPENING_EQUITY_CODE = "3900";
    static final String OPENING_EQUITY_NAME = "Opening Balance Equity";

    @Autowired private WarehouseStockRepository stockRepository;
    @Autowired private InventoryTransactionRepository transactionRepository;
    @Autowired private ChartOfAccountService chartOfAccountService;
    @Autowired private JournalVoucherService journalVoucherService;
    @Autowired private FinancialYearPeriodValidationService periodValidationService;

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Call after the quantity was already added to {@code stock}. */
    public void blendCost(WarehouseStock stock, BigDecimal addedQty, BigDecimal unitCost) {
        if (unitCost == null || unitCost.signum() <= 0 || addedQty == null || addedQty.signum() <= 0) return;
        BigDecimal newQty = nz(stock.getAvailableQuantity());
        BigDecimal oldQty = newQty.subtract(addedQty).max(BigDecimal.ZERO);
        BigDecimal value = oldQty.multiply(nz(stock.getAverageCost())).add(addedQty.multiply(unitCost));
        stock.setAverageCost(newQty.signum() > 0 ? value.divide(newQty, 4, RoundingMode.HALF_UP) : unitCost);
        stockRepository.saveAndFlush(stock);
    }

    /** Weighted cost of what was issued to a work order line (used to value returns). */
    public BigDecimal issueCostForLine(Long workOrderPartId) {
        java.util.List<Object[]> rows = transactionRepository.valueAndQuantityForLine(workOrderPartId, InventoryTransaction.TYPE_ISSUE);
        return averageOf(rows == null || rows.isEmpty() ? null : rows.get(0));
    }

    /** Net cost of parts issued from stock to a work order (issues minus returns). */
    public BigDecimal netPartsCost(Long workOrderId) {
        BigDecimal issued = nz(transactionRepository.valueForWorkOrder(workOrderId, InventoryTransaction.TYPE_ISSUE));
        BigDecimal returned = nz(transactionRepository.valueForWorkOrder(workOrderId, InventoryTransaction.TYPE_RETURN));
        return issued.subtract(returned).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal averageOf(Object[] r) {
        if (r == null || r.length < 2 || r[0] == null || r[1] == null) return BigDecimal.ZERO;
        BigDecimal value = new BigDecimal(r[0].toString());
        BigDecimal qty = new BigDecimal(r[1].toString());
        return qty.signum() > 0 ? value.divide(qty, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public void postReceipt(InventoryTransaction t, String username) {
        String mode = t.getPaymentMode() == null ? "CREDIT" : t.getPaymentMode();
        String crCode = "CASH".equals(mode) ? "1000" : "BANK".equals(mode) ? "1010" : "2000";
        String crName = "CASH".equals(mode) ? "Cash on Hand" : "BANK".equals(mode) ? "Bank - Current A/c" : "Accounts Payable";
        String crType = "CREDIT".equals(mode) ? "LIABILITY" : "ASSET";
        post(t.getCompanyId(), t.getBranchId(), value(t), INVENTORY_CODE, INVENTORY_NAME, "ASSET",
                crCode, crName, crType, "STK-RCPT-" + t.getId(),
                "Spare parts received " + t.getCode() + (t.getSupplier() != null ? " from " + t.getSupplier().getName() : ""),
                "Stock Receipt Voucher", username);
    }

    public void postOpening(InventoryTransaction t, String username) {
        post(t.getCompanyId(), t.getBranchId(), value(t), INVENTORY_CODE, INVENTORY_NAME, "ASSET",
                OPENING_EQUITY_CODE, OPENING_EQUITY_NAME, "EQUITY", "STK-OPEN-" + t.getId(),
                "Opening stock " + t.getCode(), "Opening Stock Voucher", username);
    }

    public void postInitialCost(WarehouseStock stock, BigDecimal unitCost, String username) {
        BigDecimal amount = nz(stock.getAvailableQuantity()).multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        post(stock.getCompanyId(), stock.getBranchId(), amount, INVENTORY_CODE, INVENTORY_NAME, "ASSET",
                OPENING_EQUITY_CODE, OPENING_EQUITY_NAME, "EQUITY", "STK-VAL-" + stock.getId(),
                "Initial valuation of existing stock (" + (stock.getSparePart() != null ? stock.getSparePart().getName() : "part") + ")",
                "Stock Valuation Voucher", username);
    }

    /** Parts consumed by a completed work order leave inventory into repair expense. */
    public void postPartsConsumed(WorkOrder order, BigDecimal partsCost, String username) {
        post(order.getCompanyId(), order.getBranchId(), partsCost, "5400", "Vehicle Repair & Maintenance", "EXPENSE",
                INVENTORY_CODE, INVENTORY_NAME, "ASSET", "MAINT-PARTS-" + order.getWorkOrderNumber(),
                "Parts from stock used on work order " + order.getWorkOrderNumber(), "Parts Consumption Voucher", username);
    }

    private static BigDecimal value(InventoryTransaction t) {
        return nz(t.getQuantity()).multiply(nz(t.getUnitRate())).setScale(2, RoundingMode.HALF_UP);
    }

    private void post(Long companyId, Long branchId, BigDecimal amount, String drCode, String drName, String drType,
                      String crCode, String crName, String crType, String reference, String description,
                      String name, String username) {
        if (amount == null || amount.signum() <= 0) return;
        LocalDate date = LocalDate.now();
        periodValidationService.validatePostingAllowed(companyId, date);
        JournalVoucher v = new JournalVoucher();
        v.setVoucherDate(date);
        v.setDebitAccount(chartOfAccountService.getOrCreateAccount(companyId, branchId, drCode, drName, drType));
        v.setCreditAccount(chartOfAccountService.getOrCreateAccount(companyId, branchId, crCode, crName, crType));
        v.setAmount(amount);
        v.setReferenceNumber(reference);
        v.setDescription(description);
        v.setCompanyId(companyId);
        v.setBranchId(branchId);
        v.setCode(reference.length() <= 50 ? reference : reference.substring(0, 50));
        v.setName(name);
        v.setCreatedBy(username);
        v.setUpdatedBy(username);
        journalVoucherService.createVoucher(v, username);
    }
}
