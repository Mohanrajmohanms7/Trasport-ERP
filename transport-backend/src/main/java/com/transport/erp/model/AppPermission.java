package com.transport.erp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_permissions")
public class AppPermission extends BaseEntity {
    // Inherits core attributes
}
