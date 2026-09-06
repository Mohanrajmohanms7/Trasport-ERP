package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverPayrollPaymentDTO {
    private String paymentMethod; // CASH, BANK_TRANSFER, CHEQUE, UPI
    private String remarks;
}
