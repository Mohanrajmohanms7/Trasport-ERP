package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.FinancialYear;
import com.transport.erp.repository.FinancialYearRepository;
import com.transport.erp.security.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FinancialYearPeriodValidationTest {

    @Mock
    private FinancialYearRepository fyRepository;

    @Mock
    private TenantAccessService tenantAccess;

    @InjectMocks
    private FinancialYearPeriodValidationService periodValidationService;

    private FinancialYear openFy;
    private FinancialYear closedFy;
    private final Long companyId = 1L;

    @BeforeEach
    void setUp() {
        when(tenantAccess.resolveCompanyId(anyLong())).thenAnswer(invocation -> invocation.getArgument(0));

        openFy = new FinancialYear();
        openFy.setId(100L);
        openFy.setCode("FY2025-26");
        openFy.setName("Financial Year 2025-2026");
        openFy.setStartDate(LocalDate.of(2025, 4, 1));
        openFy.setEndDate(LocalDate.of(2026, 3, 31));
        openFy.setStatus("ACTIVE");
        openFy.setCompanyId(companyId);
        openFy.setIsDeleted(false);

        closedFy = new FinancialYear();
        closedFy.setId(101L);
        closedFy.setCode("FY2024-25");
        closedFy.setName("Financial Year 2024-2025");
        closedFy.setStartDate(LocalDate.of(2024, 4, 1));
        closedFy.setEndDate(LocalDate.of(2025, 3, 31));
        closedFy.setStatus("CLOSED");
        closedFy.setCompanyId(companyId);
        closedFy.setIsDeleted(false);
    }

    @Test
    @DisplayName("Scenario 1: OPEN FY + valid date -> PASS")
    void testOpenFyValidDate_Pass() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(openFy));
        LocalDate validDate = LocalDate.of(2025, 10, 15);

        assertDoesNotThrow(() -> {
            FinancialYear result = periodValidationService.validatePostingAllowed(companyId, validDate);
            assertEquals("FY2025-26", result.getCode());
        });
    }

    @Test
    @DisplayName("Scenario 2: CLOSED FY + valid date -> FAIL (FINANCIAL_YEAR_CLOSED)")
    void testClosedFyValidDate_Fail() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(closedFy));
        LocalDate validDate = LocalDate.of(2024, 12, 1);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
            periodValidationService.validatePostingAllowed(companyId, validDate)
        );

        assertEquals("FINANCIAL_YEAR_CLOSED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("CLOSED"));
    }

    @Test
    @DisplayName("Scenario 3: No FY -> FAIL (FINANCIAL_YEAR_NOT_FOUND)")
    void testNoFy_Fail() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(Collections.emptyList());
        LocalDate anyDate = LocalDate.of(2025, 6, 1);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
            periodValidationService.validatePostingAllowed(companyId, anyDate)
        );

        assertEquals("FINANCIAL_YEAR_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("Scenario 4: Date before FY -> FAIL (TRANSACTION_DATE_OUTSIDE_FINANCIAL_YEAR)")
    void testDateBeforeFy_Fail() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(openFy));
        LocalDate dateBefore = LocalDate.of(2025, 3, 30);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
            periodValidationService.validatePostingAllowed(companyId, dateBefore)
        );

        assertEquals("TRANSACTION_DATE_OUTSIDE_FINANCIAL_YEAR", ex.getErrorCode());
    }

    @Test
    @DisplayName("Scenario 5: Date after FY -> FAIL (TRANSACTION_DATE_OUTSIDE_FINANCIAL_YEAR)")
    void testDateAfterFy_Fail() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(openFy));
        LocalDate dateAfter = LocalDate.of(2026, 4, 1);

        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
            periodValidationService.validatePostingAllowed(companyId, dateAfter)
        );

        assertEquals("TRANSACTION_DATE_OUTSIDE_FINANCIAL_YEAR", ex.getErrorCode());
    }

    @Test
    @DisplayName("Scenario 6: Exact FY start date -> PASS")
    void testExactStartFYDate_Pass() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(openFy));
        LocalDate startDate = LocalDate.of(2025, 4, 1);

        assertDoesNotThrow(() -> periodValidationService.validatePostingAllowed(companyId, startDate));
    }

    @Test
    @DisplayName("Scenario 7: Exact FY end date -> PASS")
    void testExactEndFYDate_Pass() {
        when(fyRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(openFy));
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        assertDoesNotThrow(() -> periodValidationService.validatePostingAllowed(companyId, endDate));
    }

    @Test
    @DisplayName("Scenario 8: Company mismatch -> FAIL")
    void testCompanyMismatch_Fail() {
        Long wrongCompanyId = 999L;
        when(tenantAccess.resolveCompanyId(wrongCompanyId)).thenThrow(new IllegalArgumentException("Company access denied"));

        assertThrows(IllegalArgumentException.class, () ->
            periodValidationService.validatePostingAllowed(wrongCompanyId, LocalDate.of(2025, 5, 1))
        );
    }

    @Test
    @DisplayName("Scenario 9: Null transaction date -> FAIL")
    void testNullTransactionDate_Fail() {
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
            periodValidationService.validatePostingAllowed(companyId, null)
        );

        assertEquals("INVALID_TRANSACTION_DATE", ex.getErrorCode());
    }
}
