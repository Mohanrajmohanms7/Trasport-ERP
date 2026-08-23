package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_contacts")
public class CustomerContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;
}
