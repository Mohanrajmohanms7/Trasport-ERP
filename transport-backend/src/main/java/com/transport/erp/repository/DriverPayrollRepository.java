package com.transport.erp.repository;

import com.transport.erp.model.DriverPayroll;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverPayrollRepository extends JpaRepository<DriverPayroll, Long> {

    Page<DriverPayroll> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<DriverPayroll> findByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status, Pageable pageable);

    List<DriverPayroll> findByDriverIdAndIsDeletedFalse(Long driverId);

    Optional<DriverPayroll> findByDriverIdAndPayYearAndPayMonthAndIsDeletedFalse(Long driverId, Integer payYear, Integer payMonth);

    Optional<DriverPayroll> findByPayrollNumberAndIsDeletedFalse(String payrollNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM DriverPayroll p WHERE p.id = :id AND p.isDeleted = false")
    Optional<DriverPayroll> findByIdForUpdate(@Param("id") Long id);
}
