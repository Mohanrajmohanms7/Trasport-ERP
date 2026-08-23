package com.transport.erp.repository;

import com.transport.erp.model.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    Optional<Driver> findByLicenseNumberAndIsDeletedFalse(String licenseNumber);

    Optional<Driver> findByCompanyIdAndLicenseNumberAndIsDeletedFalse(Long companyId, String licenseNumber);

    Page<Driver> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Driver> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
            Long companyId, String name, String code, Pageable pageable);

    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);
}
