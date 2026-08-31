package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReceiptSettlementDashboardDTO {
    private CustomerReceiptSettlementSummaryDTO summary;
    private CustomerAgingSummaryDTO agingSummary;
    private List<CustomerOutstandingSummaryDTO> topOutstandingCustomers;
    private List<CustomerInvoiceAgingDTO> agingInvoices;
    private List<CustomerReceiptResponseDTO> recentApprovedReceipts;
    private List<ReceiptSettlementReconciliationDTO> reconciliationSummary;
    private List<CustomerPaymentPerformanceDTO> paymentPerformance;
}
