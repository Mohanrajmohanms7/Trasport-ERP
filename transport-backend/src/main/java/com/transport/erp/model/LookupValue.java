package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lookup_values")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LookupValue extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LookupValue parent;
}
