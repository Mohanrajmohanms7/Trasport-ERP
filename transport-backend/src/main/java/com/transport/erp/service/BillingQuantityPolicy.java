package com.transport.erp.service;

import com.transport.erp.model.TripDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Which quantity a client bills (set per client by Platform Admin; V85, SQL twin: billed_qty()).
 * DELIVERED (default): delivered when recorded, else planned — unchanged behaviour for every client.
 * ORDERED: the trip's planned (ordered) quantity, or the delivered quantity when more was delivered.
 * The delivered quantity is always kept and shown on the invoice.
 */
@Service
@RequiredArgsConstructor
public class BillingQuantityPolicy {
    public static final String DELIVERED = "DELIVERED";
    public static final String ORDERED = "ORDERED";

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditService auditService;

    public String basis(Long companyId) {
        if (companyId == null) return DELIVERED;
        List<String> v = jdbc.queryForList("SELECT billing_quantity_basis FROM companies WHERE id = :id",
                new MapSqlParameterSource("id", companyId), String.class);
        return !v.isEmpty() && ORDERED.equals(v.get(0)) ? ORDERED : DELIVERED;
    }

    public static BigDecimal billQty(TripDetail d, String basis) {
        if (!ORDERED.equals(basis)) return d.getBillableQuantity();
        BigDecimal planned = d.getQuantity() == null ? BigDecimal.ZERO : d.getQuantity();
        BigDecimal delivered = d.getDeliveredQuantity() == null ? BigDecimal.ZERO : d.getDeliveredQuantity();
        return delivered.compareTo(planned) > 0 ? delivered : planned;
    }

    /** Platform Admin only (controller is SUPER_ADMIN): switch a client's basis; audited. Existing invoices never change. */
    public String setBasis(Long companyId, String basis, String username) {
        String b = basis == null ? "" : basis.trim().toUpperCase();
        if (!DELIVERED.equals(b) && !ORDERED.equals(b)) {
            throw new com.transport.erp.exception.BusinessValidationException("Invalid Billing Basis", "BILLING_BASIS_INVALID",
                    "Choose Delivered quantity or Ordered quantity.", "Select one of the two options.");
        }
        String old = basis(companyId);
        int n = jdbc.update("UPDATE companies SET billing_quantity_basis = :b WHERE id = :id AND is_deleted = false",
                new MapSqlParameterSource("b", b).addValue("id", companyId));
        if (n == 0) throw new IllegalArgumentException("Client not found");
        auditService.log(username, "BILLING_BASIS_CHANGED", "companies", companyId, null, old + " -> " + b);
        return b;
    }
}
