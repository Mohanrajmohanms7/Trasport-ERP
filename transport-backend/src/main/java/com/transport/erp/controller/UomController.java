package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.UomConversion;
import com.transport.erp.model.UomMaster;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.UomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uoms")
@CrossOrigin(origins = "*")
public class UomController {

    @Autowired
    private UomService uomService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<List<UomMaster>> getAllUoms(@RequestParam(required = false) Long companyId) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            List<UomMaster> uoms = uomService.getAllUoms(scopedCompanyId);
            return ApiResponse.success(uoms, "UOMs fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch UOMs");
        }
    }

    @GetMapping("/paged")
    public ApiResponse<Page<UomMaster>> getUomsPaged(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<UomMaster> uoms = uomService.getUomsPaged(scopedCompanyId, search, pageable);
            return ApiResponse.success(uoms, "Paged UOMs fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch paged UOMs");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<UomMaster> getUomById(@PathVariable Long id) {
        try {
            UomMaster uom = uomService.getUomById(id);
            return ApiResponse.success(uom, "UOM fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch UOM");
        }
    }

    @PostMapping
    public ApiResponse<UomMaster> createUom(@RequestBody UomMaster uom) {
        try {
            uom.setCompanyId(tenantAccess.resolveCompanyId(uom.getCompanyId()));
            UomMaster created = uomService.createUom(uom);
            return ApiResponse.success(created, "UOM created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create UOM");
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<UomMaster> updateUom(@PathVariable Long id, @RequestBody UomMaster uom) {
        try {
            UomMaster updated = uomService.updateUom(id, uom);
            return ApiResponse.success(updated, "UOM updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update UOM");
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUom(@PathVariable Long id) {
        try {
            uomService.deleteUom(id);
            return ApiResponse.success(null, "UOM deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete UOM");
        }
    }

    // --- UOM Conversion Endpoints ---

    @GetMapping("/conversions")
    public ApiResponse<List<UomConversion>> getAllConversions(@RequestParam(required = false) Long companyId) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            List<UomConversion> conversions = uomService.getAllConversions(scopedCompanyId);
            return ApiResponse.success(conversions, "UOM conversions fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch UOM conversions");
        }
    }

    @GetMapping("/conversions/paged")
    public ApiResponse<Page<UomConversion>> getConversionsPaged(
            @RequestParam(required = false) Long companyId,
            Pageable pageable) {
        try {
            Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
            Page<UomConversion> conversions = uomService.getConversionsPaged(scopedCompanyId, pageable);
            return ApiResponse.success(conversions, "Paged UOM conversions fetched successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to fetch paged UOM conversions");
        }
    }

    @PostMapping("/conversions")
    public ApiResponse<UomConversion> createConversion(@RequestBody UomConversion conversion) {
        try {
            conversion.setCompanyId(tenantAccess.resolveCompanyId(conversion.getCompanyId()));
            UomConversion created = uomService.createConversion(conversion);
            return ApiResponse.success(created, "UOM conversion created successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to create UOM conversion");
        }
    }

    @PutMapping("/conversions/{id}")
    public ApiResponse<UomConversion> updateConversion(@PathVariable Long id, @RequestBody UomConversion conversion) {
        try {
            UomConversion updated = uomService.updateConversion(id, conversion);
            return ApiResponse.success(updated, "UOM conversion updated successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to update UOM conversion");
        }
    }

    @DeleteMapping("/conversions/{id}")
    public ApiResponse<Void> deleteConversion(@PathVariable Long id) {
        try {
            uomService.deleteConversion(id);
            return ApiResponse.success(null, "UOM conversion deleted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to delete UOM conversion");
        }
    }

    // --- Dynamic UOM Calculation Endpoint ---

    @GetMapping("/convert")
    public ApiResponse<Map<String, Object>> convertQuantity(
            @RequestParam(required = false) Long materialId,
            @RequestParam Long fromUomId,
            @RequestParam Long toUomId,
            @RequestParam BigDecimal quantity) {
        try {
            BigDecimal converted = uomService.convertQuantity(materialId, fromUomId, toUomId, quantity);
            Map<String, Object> result = new HashMap<>();
            result.put("materialId", materialId);
            result.put("fromUomId", fromUomId);
            result.put("toUomId", toUomId);
            result.put("originalQuantity", quantity);
            result.put("convertedQuantity", converted);
            return ApiResponse.success(result, "Quantity converted successfully");
        } catch (Exception e) {
            return ApiResponse.error(Collections.singletonList(e.getMessage()), "Failed to convert quantity");
        }
    }
}
