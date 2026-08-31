package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CustomerAgingSummaryDTO {
    private BigDecimal currentAmount = BigDecimal.ZERO;
    private BigDecimal days1To30 = BigDecimal.ZERO;
    private BigDecimal days31To60 = BigDecimal.ZERO;
    private BigDecimal days61To90 = BigDecimal.ZERO;
    private BigDecimal days91To180 = BigDecimal.ZERO;
    private BigDecimal days181To365 = BigDecimal.ZERO;
    private BigDecimal above365 = BigDecimal.ZERO;
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
}
