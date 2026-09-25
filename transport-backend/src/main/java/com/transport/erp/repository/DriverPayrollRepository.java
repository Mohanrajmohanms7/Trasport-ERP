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

    /** The active (not deleted, not cancelled) payroll for a driver and month, if any. */
    @Query("""
            SELECT p FROM DriverPayroll p
            WHERE p.driver.id = :driverId AND p.payYear = :payYear AND p.payMonth = :payMonth
              AND p.isDeleted = false AND p.status <> 'CANCELLED'
            """)
    Optional<DriverPayroll> findActiveForPeriod(@Param("driverId") Long driverId, @Param("payYear") Integer payYear,
                                                @Param("payMonth") Integer payMonth);

    @Query("""
            SELECT p FROM DriverPayroll p
            WHERE p.companyId = :companyId AND p.isDeleted = false
              AND (:branchId IS NULL OR p.branchId = :branchId)
              AND (:driverId IS NULL OR p.driver.id = :driverId)
              AND (:payYear IS NULL OR p.payYear = :payYear)
              AND (:payMonth IS NULL OR p.payMonth = :payMonth)
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<DriverPayroll> search(@Param("companyId") Long companyId, @Param("branchId") Long branchId,
                               @Param("driverId") Long driverId, @Param("payYear") Integer payYear,
                               @Param("payMonth") Integer payMonth, @Param("status") String status, Pageable pageable);

    List<DriverPayroll> findByDriverIdAndStatusInAndIsDeletedFalseOrderByPayYearDescPayMonthDesc(Long driverId, List<String> statuses);

    Optional<DriverPayroll> findByPayrollNumberAndIsDeletedFalse(String payrollNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM DriverPayroll p WHERE p.id = :id AND p.isDeleted = false")
    Optional<DriverPayroll> findByIdForUpdate(@Param("id") Long id);
}
