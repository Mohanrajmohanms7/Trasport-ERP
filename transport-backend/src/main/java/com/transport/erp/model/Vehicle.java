package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(name = "current_odometer_km", precision = 12, scale = 2)
    private BigDecimal currentOdometerKm;

    @Column(name = "odometer_updated_at")
    private LocalDateTime odometerUpdatedAt;

    /**
     * Derived read model of {@code VehicleMaintenanceStateService}.
     * True only when a non-deleted work order for this vehicle is IN_PROGRESS.
     * Not a column, and not a substitute for the trip-creation check.
     */
    @Formula("""
            (SELECT CASE WHEN COUNT(wo.id) > 0 THEN true ELSE false END
               FROM work_orders wo
              WHERE wo.vehicle_id = id
                AND wo.status = 'IN_PROGRESS'
                AND wo.is_deleted = false)
            """)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean underMaintenance;

    @JsonProperty(value = "underMaintenance", access = JsonProperty.Access.READ_ONLY)
    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    /** Stored file name of the photo (download via /api/v1/files/download/{photoFile}). */
    @Column(name = "photo_file", length = 255)
    private String photoFile;
}
