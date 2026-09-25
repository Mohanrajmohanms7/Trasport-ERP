package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DriverPayrollPaymentDTO {
    private String paymentMethod; // CASH, BANK_TRANSFER, CHEQUE, UPI
    private LocalDate paymentDate;
    private String paymentReference;
    private String remarks;
}
