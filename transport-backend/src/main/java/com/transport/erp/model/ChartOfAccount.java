package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "chart_of_accounts")
public class ChartOfAccount extends BaseEntity {

    @Column(name = "account_code", nullable = false, length = 100)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 100)
    private String accountType; // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE

    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal runningBalance = BigDecimal.ZERO;
}
