package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_delivery_sites")
public class CustomerDeliverySite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "site_code", nullable = false, length = 50)
    private String siteCode;

    @Column(name = "site_name", nullable = false, length = 150)
    private String siteName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "manager_name", length = 100)
    private String managerName;
}
