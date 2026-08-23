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
@Table(name = "quarries")
public class Quarry extends BaseEntity {

    @Column(name = "location_address", columnDefinition = "TEXT")
    private String locationAddress;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "working_hours", length = 100)
    private String workingHours;
}
