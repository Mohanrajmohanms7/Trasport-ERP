package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.CompanyUom;
import com.transport.erp.model.Material;
import com.transport.erp.model.UomMaster;
import com.transport.erp.repository.CompanyUomRepository;
import com.transport.erp.repository.UomMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Units of measure for material orders (booking → trip → invoice). See docs/UNITS_OF_MEASURE.md.
 *
 * <ul>
 *   <li>The unit catalogue is {@code uom_master} (global rows + a company's own rows).</li>
 *   <li>{@code company_uoms} switches units on/off for one company's orders and marks the default.
 *       A company that was never configured uses Unit only (same as the V76 seed).</li>
 *   <li>Every order line stores its unit; quantities are never converted between units.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrderUomService {

    /** Unit used when a company has no order-unit settings yet. */
    public static final String FALLBACK_CODE = "UNIT";

    private final CompanyUomRepository companyUomRepository;
    private final UomMasterRepository uomMasterRepository;

    /** Display text for a unit: its symbol ("Unit", "Ton", "kg"), else its code. */
    public static String label(UomMaster uom) {
        if (uom == null) return "";
        if (uom.getSymbol() != null && !uom.getSymbol().isBlank()) return uom.getSymbol().trim();
        return uom.getCode() == null ? "" : uom.getCode();
    }

    // ------------------------------------------------------------------ what a company may use

    /** The units a company may use on new order lines, default first. Loaded once per request and reused for every line. */
    @Transactional(readOnly = true)
    public OrderUnits forCompany(Long companyId) {
        List<CompanyUom> rows = companyUomRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        LinkedHashMap<Long, UomMaster> enabled = new LinkedHashMap<>();
        UomMaster def = null;
        if (rows.isEmpty()) {
            UomMaster fallback = fallbackUnit();
            if (fallback != null) {
                enabled.put(fallback.getId(), fallback);
                def = fallback;
            }
        } else {
            rows.sort(Comparator.comparing((CompanyUom r) -> !Boolean.TRUE.equals(r.getIsDefault()))
                    .thenComparing(r -> r.getUom().getCode()));
            for (CompanyUom r : rows) {
                UomMaster u = r.getUom();
                if (!isActive(r.getStatus()) || u == null || Boolean.TRUE.equals(u.getIsDeleted()) || !isActive(u.getStatus())) continue;
                enabled.put(u.getId(), u);
                if (Boolean.TRUE.equals(r.getIsDefault())) def = u;
            }
            if (def == null && !enabled.isEmpty()) def = enabled.values().iterator().next();
        }
        return new OrderUnits(enabled, def);
    }

    /** Units for the order screens (booking / invoice line dropdowns). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> enabledUnits(Long companyId) {
        OrderUnits units = forCompany(companyId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (UomMaster u : units.enabled().values()) {
            Map<String, Object> m = describe(u);
            m.put("isDefault", units.defaultUnit() != null && units.defaultUnit().getId().equals(u.getId()));
            out.add(m);
        }
        return out;
    }

    // ------------------------------------------------------------------ admin settings

    /** Every unit the company could switch on (global + its own), with whether it is on and which is the default. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> settings(Long companyId) {
        OrderUnits units = forCompany(companyId);
        List<UomMaster> all = uomMasterRepository.findAllForCompanyOrGlobal(companyId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (UomMaster u : all) {
            Map<String, Object> m = describe(u);
            m.put("masterStatus", u.getStatus());
            m.put("enabled", units.enabled().containsKey(u.getId()));
            m.put("isDefault", units.defaultUnit() != null && units.defaultUnit().getId().equals(u.getId()));
            out.add(m);
        }
        out.sort(Comparator.comparing((Map<String, Object> m) -> !Boolean.TRUE.equals(m.get("enabled")))
                .thenComparing(m -> String.valueOf(m.get("code"))));
        return out;
    }

    /**
     * Switch a unit on/off for a company's orders, or make it the default.
     * The default unit cannot be switched off, and at least one unit always stays on.
     * Lines already saved keep their unit either way.
     */
    @Transactional
    public List<Map<String, Object>> updateSetting(Long companyId, Long uomId, Boolean enabled, Boolean makeDefault, String username) {
        UomMaster uom = uomMasterRepository.findById(uomId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException("Unit Not Found", "ORDER_UOM_NOT_FOUND",
                        "This unit is not in the UOM master.", "Refresh the page and pick a unit from the list."));
        if (uom.getCompanyId() != null && !uom.getCompanyId().equals(companyId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to another company's unit");
        }
        boolean wantDefault = Boolean.TRUE.equals(makeDefault);
        boolean wantOn = wantDefault || (enabled != null ? enabled : true);
        if (wantOn && !isActive(uom.getStatus())) {
            throw new BusinessValidationException("Unit Inactive In Master", "ORDER_UOM_MASTER_INACTIVE",
                    label(uom) + " is inactive in the UOM master.", "Activate it in the UOM master first.");
        }

        List<CompanyUom> rows = materialiseRows(companyId, username);
        CompanyUom row = rows.stream().filter(r -> r.getUom().getId().equals(uomId)).findFirst().orElse(null);

        if (!wantOn) {
            if (row == null || !isActive(row.getStatus())) return settings(companyId);
            if (Boolean.TRUE.equals(row.getIsDefault())) {
                throw new BusinessValidationException("Default Unit", "ORDER_UOM_DEFAULT_OFF",
                        label(uom) + " is the default order unit and cannot be switched off.",
                        "Make another unit the default first, then switch " + label(uom) + " off.");
            }
            long onCount = rows.stream().filter(r -> isActive(r.getStatus())).count();
            if (onCount <= 1) {
                throw new BusinessValidationException("Last Order Unit", "ORDER_UOM_LAST_ONE",
                        "At least one unit must stay switched on for orders.", "Switch another unit on first.");
            }
            row.setStatus("INACTIVE");
            row.setUpdatedBy(username);
            companyUomRepository.save(row);
            return settings(companyId);
        }

        if (row == null) {
            row = new CompanyUom();
            row.setCompanyId(companyId);
            row.setUom(uom);
            row.setCode(uom.getCode());
            row.setName(uom.getName());
            row.setCreatedBy(username);
        }
        row.setStatus("ACTIVE");
        row.setUpdatedBy(username);
        if (wantDefault && !Boolean.TRUE.equals(row.getIsDefault())) {
            // Clear the old default first so the one-default-per-company index is never violated mid-flush.
            for (CompanyUom r : rows) {
                if (Boolean.TRUE.equals(r.getIsDefault())) {
                    r.setIsDefault(false);
                    r.setUpdatedBy(username);
                    companyUomRepository.saveAndFlush(r);
                }
            }
            row.setIsDefault(true);
        }
        companyUomRepository.save(row);
        return settings(companyId);
    }

    /** New companies start with Unit as their only, default order unit (idempotent). */
    @Transactional
    public void ensureConfigured(Long companyId, String username) {
        if (companyId == null) return;
        if (!companyUomRepository.findByCompanyIdAndIsDeletedFalse(companyId).isEmpty()) return;
        materialiseRows(companyId, username == null ? "SYSTEM" : username);
    }

    // ------------------------------------------------------------------ helpers

    /** Rows for a company; an unconfigured company first gets its implicit Unit default stored. */
    private List<CompanyUom> materialiseRows(Long companyId, String username) {
        List<CompanyUom> rows = companyUomRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        if (!rows.isEmpty()) return new ArrayList<>(rows);
        UomMaster fallback = fallbackUnit();
        List<CompanyUom> out = new ArrayList<>();
        if (fallback != null) {
            CompanyUom r = new CompanyUom();
            r.setCompanyId(companyId);
            r.setUom(fallback);
            r.setCode(fallback.getCode());
            r.setName(fallback.getName());
            r.setStatus("ACTIVE");
            r.setIsDefault(true);
            r.setCreatedBy(username);
            r.setUpdatedBy(username);
            out.add(companyUomRepository.saveAndFlush(r));
        }
        return out;
    }

    private UomMaster fallbackUnit() {
        return uomMasterRepository.findByCodeAndIsDeletedFalse(FALLBACK_CODE).orElse(null);
    }

    private static boolean isActive(String status) {
        return status == null || "ACTIVE".equalsIgnoreCase(status);
    }

    private static Map<String, Object> describe(UomMaster u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("code", u.getCode());
        m.put("name", u.getName());
        m.put("symbol", u.getSymbol());
        m.put("label", label(u));
        m.put("category", u.getCategory());
        m.put("global", u.getCompanyId() == null);
        return m;
    }

    // ------------------------------------------------------------------ line resolution

    /**
     * The units a company may use, prepared once per request. {@link #resolve} decides the unit of one order line:
     * the requested unit (must be switched on, or be the unit the line already had), else the line's previous unit,
     * else the material's default unit when it is switched on, else the company default.
     */
    public record OrderUnits(LinkedHashMap<Long, UomMaster> enabled, UomMaster defaultUnit) {

        public UomMaster resolve(UomMaster requested, Material material, UomMaster previous, String what) {
            Long reqId = requested != null ? requested.getId() : null;
            if (reqId != null) {
                if (previous != null && reqId.equals(previous.getId())) return previous;
                UomMaster u = enabled.get(reqId);
                if (u != null) return u;
                throw new BusinessValidationException("Unit Not Switched On", "ORDER_UOM_NOT_ENABLED",
                        "The unit chosen for " + what + " is not switched on for orders in this company.",
                        "Use " + names() + ". A company admin can switch other units on in Material & Quarry → UOM Master.");
            }
            if (previous != null) return previous;
            if (material != null && material.getDefaultUom() != null) {
                UomMaster u = enabled.get(material.getDefaultUom().getId());
                if (u != null) return u;
            }
            if (defaultUnit != null) return defaultUnit;
            throw new BusinessValidationException("No Order Unit", "ORDER_UOM_NONE",
                    "No unit of measure is switched on for orders in this company.",
                    "Ask a company admin to switch a unit on in Material & Quarry → UOM Master.");
        }

        private String names() {
            return enabled.values().stream().map(OrderUomService::label).collect(Collectors.joining(", "));
        }
    }
}
