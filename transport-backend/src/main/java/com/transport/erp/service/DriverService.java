package com.transport.erp.service;

import com.transport.erp.model.Driver;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public Page<Driver> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return driverRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return driverRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Driver> getById(Long id) {
        return driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .map(d -> {
                    tenantAccess.assertOwned(d.getCompanyId());
                    return d;
                });
    }

    @Transactional
    public Driver create(Driver driver) {
        Long companyId = tenantAccess.resolveCompanyId(driver.getCompanyId());
        driver.setCompanyId(companyId);
        if (driverRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, driver.getCode()).isPresent()) {
            throw new IllegalArgumentException("Driver code already exists in this company: " + driver.getCode());
        }
        if (driverRepository.findByCompanyIdAndLicenseNumberAndIsDeletedFalse(companyId, driver.getLicenseNumber()).isPresent()) {
            throw new IllegalArgumentException("Driver license already exists in this company: " + driver.getLicenseNumber());
        }
        driver.setIsDeleted(false);
        return driverRepository.save(driver);
    }

    @Transactional
    public Driver update(Long id, Driver driverDetails) {
        Driver driver = driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());

        Optional<Driver> existingCode = driverRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                driver.getCompanyId(), driverDetails.getCode());
        if (existingCode.isPresent() && !existingCode.get().getId().equals(id)) {
            throw new IllegalArgumentException("Driver code already exists: " + driverDetails.getCode());
        }

        Optional<Driver> existingLicense = driverRepository.findByCompanyIdAndLicenseNumberAndIsDeletedFalse(
                driver.getCompanyId(), driverDetails.getLicenseNumber());
        if (existingLicense.isPresent() && !existingLicense.get().getId().equals(id)) {
            throw new IllegalArgumentException("Driver license already exists in this company: " + driverDetails.getLicenseNumber());
        }

        driver.setCode(driverDetails.getCode());
        driver.setName(driverDetails.getName());
        driver.setDescription(driverDetails.getDescription());
        driver.setStatus(driverDetails.getStatus());
        driver.setLicenseNumber(driverDetails.getLicenseNumber());
        driver.setLicenseExpiryDate(driverDetails.getLicenseExpiryDate());
        driver.setPhoneNumber(driverDetails.getPhoneNumber());
        if (driverDetails.getBranchId() != null) {
            driver.setBranchId(driverDetails.getBranchId());
        }

        return driverRepository.save(driver);
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());
        driver.setIsDeleted(true);
        driverRepository.save(driver);
    }

    @Transactional
    public Driver toggleStatus(Long id) {
        Driver driver = driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        tenantAccess.assertOwned(driver.getCompanyId());
        driver.setStatus("ACTIVE".equals(driver.getStatus()) ? "INACTIVE" : "ACTIVE");
        return driverRepository.save(driver);
    }
}
