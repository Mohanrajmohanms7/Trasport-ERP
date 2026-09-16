package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportPrintDTO<T> {

    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String companyGSTIN;
    private String companyPAN;
    private String branchName;
    private T reportData;
}
