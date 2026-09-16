package com.transport.erp.service;

import com.transport.erp.model.UomConversion;
import com.transport.erp.model.UomMaster;
import com.transport.erp.repository.UomConversionRepository;
import com.transport.erp.repository.UomMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UomService {

    private final UomMasterRepository uomMasterRepository;
    private final UomConversionRepository uomConversionRepository;

    // --- UOM Master Operations ---

    public List<UomMaster> getAllUoms(Long companyId) {
        if (companyId != null) {
            return uomMasterRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        }
        return uomMasterRepository.findByIsDeletedFalse();
    }

    public Page<UomMaster> getUomsPaged(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return uomMasterRepository.searchUoms(companyId, search.trim(), pageable);
        }
        if (companyId != null) {
            return uomMasterRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
        }
        return uomMasterRepository.findByIsDeletedFalse(pageable);
    }

    public UomMaster getUomById(Long id) {
        return uomMasterRepository.findById(id)
                .filter(uom -> !uom.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("UOM not found with ID: " + id));
    }

    public UomMaster getUomByCode(String code) {
        return uomMasterRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new IllegalArgumentException("UOM not found with code: " + code));
    }

    @Transactional
    public UomMaster createUom(UomMaster uom) {
        if (uom.getCode() == null || uom.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("UOM code is required");
        }
        uom.setCode(uom.getCode().trim().toUpperCase());
        if (uomMasterRepository.findByCodeAndIsDeletedFalse(uom.getCode()).isPresent()) {
            throw new IllegalArgumentException("UOM code already exists: " + uom.getCode());
        }
        return uomMasterRepository.save(uom);
    }

    @Transactional
    public UomMaster updateUom(Long id, UomMaster updatedUom) {
        UomMaster existing = getUomById(id);
        existing.setName(updatedUom.getName());
        existing.setSymbol(updatedUom.getSymbol());
        existing.setCategory(updatedUom.getCategory());
        existing.setIsBaseUnit(updatedUom.getIsBaseUnit());
        existing.setDescription(updatedUom.getDescription());
        if (updatedUom.getStatus() != null) {
            existing.setStatus(updatedUom.getStatus());
        }
        return uomMasterRepository.save(existing);
    }

    @Transactional
    public void deleteUom(Long id) {
        UomMaster uom = getUomById(id);
        uom.setIsDeleted(true);
        uomMasterRepository.save(uom);
    }

    // --- UOM Conversion Operations ---

    public List<UomConversion> getAllConversions(Long companyId) {
        if (companyId != null) {
            return uomConversionRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        }
        return uomConversionRepository.findByIsDeletedFalse();
    }

    public Page<UomConversion> getConversionsPaged(Long companyId, Pageable pageable) {
        if (companyId != null) {
            return uomConversionRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
        }
        return uomConversionRepository.findByIsDeletedFalse(pageable);
    }

    public UomConversion getConversionById(Long id) {
        return uomConversionRepository.findById(id)
                .filter(conv -> !conv.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("UOM Conversion not found with ID: " + id));
    }

    @Transactional
    public UomConversion createConversion(UomConversion conversion) {
        if (conversion.getFromUom() == null || conversion.getToUom() == null) {
            throw new IllegalArgumentException("From UOM and To UOM are required");
        }
        if (conversion.getConversionFactor() == null || conversion.getConversionFactor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Conversion factor must be greater than zero");
        }
        if (conversion.getCode() == null || conversion.getCode().trim().isEmpty()) {
            String fromCode = uomMasterRepository.findById(conversion.getFromUom().getId())
                    .map(UomMaster::getCode).orElse("FROM");
            String toCode = uomMasterRepository.findById(conversion.getToUom().getId())
                    .map(UomMaster::getCode).orElse("TO");
            conversion.setCode("CONV_" + fromCode + "_" + toCode);
        }
        if (conversion.getName() == null || conversion.getName().trim().isEmpty()) {
            conversion.setName("1 " + conversion.getCode());
        }
        return uomConversionRepository.save(conversion);
    }

    @Transactional
    public UomConversion updateConversion(Long id, UomConversion updated) {
        UomConversion existing = getConversionById(id);
        existing.setConversionFactor(updated.getConversionFactor());
        existing.setDescription(updated.getDescription());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        return uomConversionRepository.save(existing);
    }

    @Transactional
    public void deleteConversion(Long id) {
        UomConversion conversion = getConversionById(id);
        conversion.setIsDeleted(true);
        uomConversionRepository.save(conversion);
    }

    // --- Dynamic UOM Conversion Engine ---

    public BigDecimal convertQuantity(Long materialId, Long fromUomId, Long toUomId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (fromUomId == null || toUomId == null || fromUomId.equals(toUomId)) {
            return quantity;
        }

        // 1. Material-Specific Direct Conversion
        if (materialId != null) {
            Optional<UomConversion> matDirect = uomConversionRepository.findMaterialSpecificConversion(materialId, fromUomId, toUomId);
            if (matDirect.isPresent()) {
                return quantity.multiply(matDirect.get().getConversionFactor()).setScale(4, RoundingMode.HALF_UP);
            }
        }

        // 2. Global Direct Conversion
        Optional<UomConversion> globalDirect = uomConversionRepository.findGlobalConversion(fromUomId, toUomId);
        if (globalDirect.isPresent()) {
            return quantity.multiply(globalDirect.get().getConversionFactor()).setScale(4, RoundingMode.HALF_UP);
        }

        // 3. Material-Specific Inverse Conversion
        if (materialId != null) {
            Optional<UomConversion> matInverse = uomConversionRepository.findMaterialSpecificConversion(materialId, toUomId, fromUomId);
            if (matInverse.isPresent()) {
                return quantity.divide(matInverse.get().getConversionFactor(), 6, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
            }
        }

        // 4. Global Inverse Conversion
        Optional<UomConversion> globalInverse = uomConversionRepository.findGlobalConversion(toUomId, fromUomId);
        if (globalInverse.isPresent()) {
            return quantity.divide(globalInverse.get().getConversionFactor(), 6, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
        }

        log.warn("No direct or inverse conversion rule found for materialId={}, fromUomId={}, toUomId={}", materialId, fromUomId, toUomId);
        throw new IllegalArgumentException("No UOM conversion rule found for conversion between UOM " + fromUomId + " and UOM " + toUomId);
    }
}
