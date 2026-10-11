package com.transport.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "sales_invoice_details")
public class SalesInvoiceDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonBackReference
    private SalesInvoice invoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Unit the quantity is in (UNIT, TON, KG …). Rates on this line are per this unit. No conversion is applied. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uom_id")
    private UomMaster uom;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Delivered quantity of the trip at billing time (V85) — shown on the invoice when it differs from the billed quantity. */
    @Column(name = "delivered_quantity", precision = 12, scale = 3)
    private BigDecimal deliveredQuantity;

    /** DELIVERED or ORDERED: the client's billing basis used for this line (V85). */
    @Column(name = "quantity_basis", length = 20)
    private String quantityBasis;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "freight_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightCharges = BigDecimal.ZERO;

    @Column(name = "loading_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal loadingCharges = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal royalty = BigDecimal.ZERO;

    @Column(name = "gst_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.valueOf(18.00);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igst = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** Share of the invoice discount applied to this line (before GST). */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** quantity x (rate + freight + loading + royalty) - discountAmount. */
    @Column(name = "taxable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;
}
