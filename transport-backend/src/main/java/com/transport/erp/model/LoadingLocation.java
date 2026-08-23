package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "loading_locations")
public class LoadingLocation extends BaseEntity {

    @Column(name = "location_code", nullable = false, length = 50)
    private String locationCode;

    @Column(name = "loading_point", nullable = false, length = 150)
    private String loadingPoint;

    @Column(name = "loading_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal loadingCharges = BigDecimal.ZERO;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
}
