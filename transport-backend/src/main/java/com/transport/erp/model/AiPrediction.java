package com.transport.erp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "ai_predictions")
public class AiPrediction extends BaseEntity {

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType; // MAINTENANCE, FUEL, TRIP_DELAY

    @Column(name = "prediction_text", nullable = false, columnDefinition = "TEXT")
    private String predictionText;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probability = BigDecimal.ZERO;
}
