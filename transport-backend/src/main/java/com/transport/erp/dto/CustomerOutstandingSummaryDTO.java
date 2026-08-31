package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerOutstandingSummaryDTO {
    private Long customerId;
    private String customerName;
    private String customerCode;
    private BigDecimal totalInvoiceAmount = BigDecimal.ZERO;
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    private BigDecimal totalAdvance = BigDecimal.ZERO;
    private long invoiceCount;
    private long unpaidInvoiceCount;
    private long partiallyPaidInvoiceCount;
    private long paidInvoiceCount;
    private LocalDate lastPaymentDate;

    // Default constructor for mapping/reflection
    public CustomerOutstandingSummaryDTO() {}

    // Convenience constructor for manual projections
    public CustomerOutstandingSummaryDTO(Long customerId, String customerName, String customerCode,
                                         Object totalInvoiceAmount, Object totalPaidAmount,
                                         Object totalOutstanding, Object totalAdvance,
                                         Object invoiceCount, Object unpaidInvoiceCount,
                                         Object partiallyPaidInvoiceCount, Object paidInvoiceCount,
                                         Object lastPaymentDate) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.totalInvoiceAmount = toBigDecimal(totalInvoiceAmount);
        this.totalPaidAmount = toBigDecimal(totalPaidAmount);
        this.totalOutstanding = toBigDecimal(totalOutstanding);
        this.totalAdvance = toBigDecimal(totalAdvance);
        this.invoiceCount = toLong(invoiceCount);
        this.unpaidInvoiceCount = toLong(unpaidInvoiceCount);
        this.partiallyPaidInvoiceCount = toLong(partiallyPaidInvoiceCount);
        this.paidInvoiceCount = toLong(paidInvoiceCount);
        this.lastPaymentDate = (LocalDate) lastPaymentDate;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return BigDecimal.ZERO;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }
}
