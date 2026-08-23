package com.transport.erp.service;

import com.transport.erp.model.Company;
import com.transport.erp.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public Page<Company> getAll(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return companyRepository.findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    search, search, pageable);
        }
        return companyRepository.findByIsDeletedFalse(pageable);
    }

    public Optional<Company> getById(Long id) {
        return companyRepository.findById(id)
                .filter(c -> !c.getIsDeleted());
    }

    @Transactional
    public Company create(Company company) {
        if (companyRepository.findByCodeAndIsDeletedFalse(company.getCode()).isPresent()) {
            throw new IllegalArgumentException("Company code already exists: " + company.getCode());
        }
        company.setIsDeleted(false);
        return companyRepository.save(company);
    }

    @Transactional
    public Company update(Long id, Company companyDetails) {
        Company company = companyRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));

        Optional<Company> existing = companyRepository.findByCodeAndIsDeletedFalse(companyDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Company code already exists: " + companyDetails.getCode());
        }

        company.setCode(companyDetails.getCode());
        company.setName(companyDetails.getName());
        company.setDescription(companyDetails.getDescription());
        company.setStatus(companyDetails.getStatus());
        company.setGstNumber(companyDetails.getGstNumber());
        company.setPanNumber(companyDetails.getPanNumber());
        company.setCinNumber(companyDetails.getCinNumber());
        company.setPhone(companyDetails.getPhone());
        company.setEmail(companyDetails.getEmail());
        company.setWebsite(companyDetails.getWebsite());
        company.setAddress(companyDetails.getAddress());
        company.setCity(companyDetails.getCity());
        company.setState(companyDetails.getState());
        company.setCountry(companyDetails.getCountry());
        company.setPincode(companyDetails.getPincode());
        company.setLogo(companyDetails.getLogo());
        company.setDigitalSignature(companyDetails.getDigitalSignature());

        return companyRepository.save(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
        company.setIsDeleted(true);
        companyRepository.save(company);
    }

    @Transactional
    public Company toggleStatus(Long id) {
        Company company = companyRepository.findById(id)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
        company.setStatus("ACTIVE".equals(company.getStatus()) ? "INACTIVE" : "ACTIVE");
        return companyRepository.save(company);
    }
}
