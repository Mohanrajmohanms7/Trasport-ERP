package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.FinancialYear;
import com.transport.erp.repository.FinancialYearRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class FinancialYearPeriodValidationService {

    @Autowired
    private FinancialYearRepository fyRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    /**
     * Validates that posting is permitted for the given company and transaction date.
     * Throws BusinessValidationException if:
     * 1. Transaction date is null.
     * 2. Tenant access/company assertion fails.
     * 3. No Financial Year exists for the company covering the transaction date.
     * 4. The matching Financial Year has status "CLOSED".
     */
    @Transactional(readOnly = true)
    public FinancialYear validatePostingAllowed(Long companyId, LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new BusinessValidationException(
                "Financial Period Validation Failed",
                "INVALID_TRANSACTION_DATE",
                "Transaction date cannot be null for accounting posting.",
                "Provide a valid transaction date."
            );
        }

        Long resolvedCompanyId = tenantAccess.resolveCompanyId(companyId);

        List<FinancialYear> companyFys = fyRepository.findByCompanyIdAndIsDeletedFalse(resolvedCompanyId);

        if (companyFys == null || companyFys.isEmpty()) {
            throw new BusinessValidationException(
                "Financial Period Not Found",
                "FINANCIAL_YEAR_NOT_FOUND",
                String.format("No Financial Year is configured for company ID %d.", resolvedCompanyId),
                "Configure a Financial Year for the company before posting financial transactions."
            );
        }

        // Find the Financial Year that covers the transactionDate (inclusive of startDate and endDate)
        Optional<FinancialYear> matchingFy = companyFys.stream()
            .filter(fy -> fy.getStartDate() != null && fy.getEndDate() != null)
            .filter(fy -> !transactionDate.isBefore(fy.getStartDate()) && !transactionDate.isAfter(fy.getEndDate()))
            .findFirst();

        if (matchingFy.isEmpty()) {
            String formattedDate = transactionDate.format(DATE_FORMATTER);
            throw new BusinessValidationException(
                "Transaction Date Outside Financial Year",
                "TRANSACTION_DATE_OUTSIDE_FINANCIAL_YEAR",
                String.format("Transaction dated %s does not fall within any configured Financial Year for company ID %d.", formattedDate, resolvedCompanyId),
                "Select a transaction date that falls within an active Financial Year, or create a new Financial Year for this period."
            );
        }

        FinancialYear fy = matchingFy.get();

        if ("CLOSED".equalsIgnoreCase(fy.getStatus())) {
            String formattedDate = transactionDate.format(DATE_FORMATTER);
            throw new BusinessValidationException(
                "Financial Period Closed",
                "FINANCIAL_YEAR_CLOSED",
                String.format("Transaction dated %s cannot be posted because Financial Year '%s' (%s to %s) is CLOSED.",
                    formattedDate, fy.getName() != null ? fy.getName() : fy.getCode(),
                    fy.getStartDate().format(DATE_FORMATTER), fy.getEndDate().format(DATE_FORMATTER)),
                "Use an OPEN accounting period or reopen the Financial Year through the authorized Financial Year workflow."
            );
        }

        return fy;
    }
}
