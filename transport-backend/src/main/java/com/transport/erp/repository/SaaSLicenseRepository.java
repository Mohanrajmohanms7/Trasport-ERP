package com.transport.erp.repository;

import com.transport.erp.model.SaaSLicense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SaaSLicenseRepository extends JpaRepository<SaaSLicense, Long> {
    Optional<SaaSLicense> findByLicenseKey(String licenseKey);
    Page<SaaSLicense> findByCompanyId(Long companyId, Pageable pageable);
}
