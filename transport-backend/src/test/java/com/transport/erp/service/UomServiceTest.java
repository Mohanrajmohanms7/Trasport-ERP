package com.transport.erp.service;

import com.transport.erp.model.Material;
import com.transport.erp.model.UomConversion;
import com.transport.erp.model.UomMaster;
import com.transport.erp.repository.UomConversionRepository;
import com.transport.erp.repository.UomMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UomServiceTest {

    @Mock
    private UomMasterRepository uomMasterRepository;

    @Mock
    private UomConversionRepository uomConversionRepository;

    private UomService uomService;

    private UomMaster unitUom;
    private UomMaster cftUom;
    private UomMaster tonUom;
    private UomMaster kgUom;

    @BeforeEach
    void setUp() {
        uomService = new UomService(uomMasterRepository, uomConversionRepository);

        unitUom = new UomMaster();
        unitUom.setId(1L);
        unitUom.setCode("UNIT");
        unitUom.setName("Unit (100 CFT)");
        unitUom.setSymbol("UNIT");
        unitUom.setCategory("VOLUME");

        cftUom = new UomMaster();
        cftUom.setId(2L);
        cftUom.setCode("CFT");
        cftUom.setName("Cubic Feet");
        cftUom.setSymbol("cu ft");
        cftUom.setCategory("VOLUME");

        tonUom = new UomMaster();
        tonUom.setId(3L);
        tonUom.setCode("TON");
        tonUom.setName("Metric Tonne");
        tonUom.setSymbol("MT");
        tonUom.setCategory("WEIGHT");

        kgUom = new UomMaster();
        kgUom.setId(4L);
        kgUom.setCode("KG");
        kgUom.setName("Kilogram");
        kgUom.setSymbol("kg");
        kgUom.setCategory("WEIGHT");
    }

    @Test
    @DisplayName("Create UOM converts code to uppercase and saves entity")
    void testCreateUomSuccess() {
        UomMaster uom = new UomMaster();
        uom.setCode("bag");
        uom.setName("Bag");

        when(uomMasterRepository.findByCodeAndIsDeletedFalse("BAG")).thenReturn(Optional.empty());
        when(uomMasterRepository.save(any(UomMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        UomMaster created = uomService.createUom(uom);
        assertNotNull(created);
        assertEquals("BAG", created.getCode());
        verify(uomMasterRepository, times(1)).save(uom);
    }

    @Test
    @DisplayName("Global Conversion: 1 UNIT = 100 CFT")
    void testConvertUnitToCft() {
        UomConversion conv = new UomConversion();
        conv.setFromUom(unitUom);
        conv.setToUom(cftUom);
        conv.setConversionFactor(new BigDecimal("100.000000"));

        when(uomConversionRepository.findGlobalConversion(1L, 2L)).thenReturn(Optional.of(conv));

        BigDecimal result = uomService.convertQuantity(null, 1L, 2L, new BigDecimal("2.5"));
        assertEquals(new BigDecimal("250.0000"), result);
    }

    @Test
    @DisplayName("Global Conversion: 1 UNIT = 4.53 Metric Tonnes")
    void testConvertUnitToTon() {
        UomConversion conv = new UomConversion();
        conv.setFromUom(unitUom);
        conv.setToUom(tonUom);
        conv.setConversionFactor(new BigDecimal("4.530000"));

        when(uomConversionRepository.findGlobalConversion(1L, 3L)).thenReturn(Optional.of(conv));

        BigDecimal result = uomService.convertQuantity(null, 1L, 3L, new BigDecimal("10.0"));
        assertEquals(new BigDecimal("45.3000"), result);
    }

    @Test
    @DisplayName("Global Conversion: 1 UNIT = 4,530 KG")
    void testConvertUnitToKg() {
        UomConversion conv = new UomConversion();
        conv.setFromUom(unitUom);
        conv.setToUom(kgUom);
        conv.setConversionFactor(new BigDecimal("4530.000000"));

        when(uomConversionRepository.findGlobalConversion(1L, 4L)).thenReturn(Optional.of(conv));

        BigDecimal result = uomService.convertQuantity(null, 1L, 4L, new BigDecimal("2.0"));
        assertEquals(new BigDecimal("9060.0000"), result);
    }

    @Test
    @DisplayName("Inverse Conversion: CFT to UNIT when only UNIT to CFT exists")
    void testConvertCftToUnitInverse() {
        UomConversion conv = new UomConversion();
        conv.setFromUom(unitUom);
        conv.setToUom(cftUom);
        conv.setConversionFactor(new BigDecimal("100.000000"));

        when(uomConversionRepository.findGlobalConversion(2L, 1L)).thenReturn(Optional.empty());
        when(uomConversionRepository.findGlobalConversion(1L, 2L)).thenReturn(Optional.of(conv));

        BigDecimal result = uomService.convertQuantity(null, 2L, 1L, new BigDecimal("150.0"));
        assertEquals(new BigDecimal("1.5000"), result);
    }

    @Test
    @DisplayName("Material-Specific Conversion overrides global conversion")
    void testMaterialSpecificConversionOverride() {
        Long materialId = 99L;
        UomConversion matConv = new UomConversion();
        matConv.setFromUom(unitUom);
        matConv.setToUom(tonUom);
        matConv.setConversionFactor(new BigDecimal("4.800000")); // Material-specific density override

        when(uomConversionRepository.findMaterialSpecificConversion(materialId, 1L, 3L)).thenReturn(Optional.of(matConv));

        BigDecimal result = uomService.convertQuantity(materialId, 1L, 3L, new BigDecimal("10.0"));
        assertEquals(new BigDecimal("48.0000"), result);
        verify(uomConversionRepository, times(1)).findMaterialSpecificConversion(materialId, 1L, 3L);
        verify(uomConversionRepository, never()).findGlobalConversion(1L, 3L);
    }

    @Test
    @DisplayName("Same UOM Identity returns unchanged quantity")
    void testSameUomIdentity() {
        BigDecimal result = uomService.convertQuantity(null, 3L, 3L, new BigDecimal("5.5"));
        assertEquals(new BigDecimal("5.5"), result);
        verifyNoInteractions(uomConversionRepository);
    }

    @Test
    @DisplayName("Missing UOM conversion rule throws IllegalArgumentException")
    void testMissingConversionThrowsException() {
        when(uomConversionRepository.findGlobalConversion(1L, 99L)).thenReturn(Optional.empty());
        when(uomConversionRepository.findGlobalConversion(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            uomService.convertQuantity(null, 1L, 99L, new BigDecimal("10.0"))
        );
    }
}
