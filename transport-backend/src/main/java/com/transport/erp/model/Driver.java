package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "drivers")
public class Driver extends BaseEntity {

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
