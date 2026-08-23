package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "driver_salaries")
public class DriverSalary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    @JsonIgnore
    private Driver driver;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "overtime_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal overtimeRate = BigDecimal.ZERO;

    @Column(name = "advance_taken", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceTaken = BigDecimal.ZERO;
}
