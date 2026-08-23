package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicle extends BaseEntity {

    @Column(name = "chassis_number", length = 100)
    private String chassisNumber;

    @Column(name = "engine_number", length = 100)
    private String engineNumber;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private LookupValue type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private LookupValue category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capacity_id")
    private LookupValue capacity;

    @Column(name = "owner_name", length = 150)
    private String ownerName;

    @Column(name = "owner_type", length = 50)
    private String ownerType;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    @Column(name = "fitness_expiry_date")
    private LocalDate fitnessExpiryDate;

    @Column(name = "permit_expiry_date")
    private LocalDate permitExpiryDate;
}
