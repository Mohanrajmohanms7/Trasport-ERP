package com.transport.erp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "app_settings",
    uniqueConstraints = @UniqueConstraint(name = "uq_app_settings_company_key", columnNames = {"company_id", "key_name"})
)
public class AppSetting extends BaseEntity {

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @Column(name = "value_data", columnDefinition = "TEXT")
    private String valueData;
}
