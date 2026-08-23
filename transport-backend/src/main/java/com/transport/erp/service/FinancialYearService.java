package com.transport.erp.service;

import com.transport.erp.model.FinancialYear;
import com.transport.erp.repository.FinancialYearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialYearService {

    @Autowired
    private FinancialYearRepository fyRepository;

    public Page<FinancialYear> getFinancialYears(Long companyId, Pageable pageable) {
        return fyRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public FinancialYear getById(Long id) {
        return fyRepository.findById(id)
                .filter(fy -> !fy.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Financial Year not found: " + id));
    }

    @Transactional
    public FinancialYear create(FinancialYear fy) {
        if (fyRepository.findByCodeAndCompanyIdAndIsDeletedFalse(fy.getCode(), fy.getCompanyId()).isPresent()) {
            throw new IllegalArgumentException("Financial Year code already exists in this company: " + fy.getCode());
        }
        
        if (fy.getIsDefault()) {
            clearOtherDefaults(fy.getCompanyId());
        }

        fy.setIsDeleted(false);
        return fyRepository.save(fy);
    }

    @Transactional
    public FinancialYear update(Long id, FinancialYear details) {
        FinancialYear existing = getById(id);

        if (details.getIsDefault()) {
            clearOtherDefaults(existing.getCompanyId());
        }

        existing.setCode(details.getCode());
        existing.setName(details.getName());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setStatus(details.getStatus());
        existing.setIsDefault(details.getIsDefault());
        existing.setDescription(details.getDescription());

        return fyRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        FinancialYear fy = getById(id);
        fy.setIsDeleted(true);
        fyRepository.save(fy);
    }

    private void clearOtherDefaults(Long companyId) {
        fyRepository.findAll().stream()
                .filter(fy -> !fy.getIsDeleted() && fy.getIsDefault() && fy.getCompanyId().equals(companyId))
                .forEach(fy -> {
                    fy.setIsDefault(false);
                    fyRepository.save(fy);
                });
    }
}
