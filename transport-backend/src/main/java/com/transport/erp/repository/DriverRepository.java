package com.transport.erp.repository;

import com.transport.erp.model.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d WHERE d.id = :id AND d.isDeleted = false")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    Optional<Driver> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    Optional<Driver> findByLicenseNumberAndIsDeletedFalse(String licenseNumber);

    Optional<Driver> findByCompanyIdAndLicenseNumberAndIsDeletedFalse(Long companyId, String licenseNumber);

    Optional<Driver> findByAppUserIdAndIsDeletedFalse(Long appUserId);

    Optional<Driver> findByAppUserIdAndIdNotAndIsDeletedFalse(Long appUserId, Long id);

    Page<Driver> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Driver> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
            Long companyId, String name, String code, Pageable pageable);

    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);
}
