package com.transport.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reports whether the ERP still needs first-run configuration.
 * Consumed by the Angular Setup Wizard to decide whether to show itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupStatusResponse {

    /** True once the user has explicitly finished the Setup Wizard. */
    private boolean setupCompleted;

    /** True when at least one vehicle, driver, customer or material exists. */
    private boolean hasBusinessData;

    private long companyCount;
    private long branchCount;
    private long vehicleCount;
    private long driverCount;
    private long customerCount;
    private long materialCount;
}
