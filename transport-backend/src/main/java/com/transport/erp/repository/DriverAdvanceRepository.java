package com.transport.erp.repository;

import com.transport.erp.model.DriverAdvance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverAdvanceRepository extends JpaRepository<DriverAdvance, Long> {

    @Query("""
            SELECT a FROM DriverAdvance a
            WHERE a.companyId = :companyId AND a.isDeleted = false
              AND (:driverId IS NULL OR a.driver.id = :driverId)
              AND (:branchId IS NULL OR a.branchId = :branchId)
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.advanceDate DESC, a.id DESC
            """)
    Page<DriverAdvance> search(@Param("companyId") Long companyId, @Param("driverId") Long driverId,
                               @Param("branchId") Long branchId, @Param("status") String status, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(a.amount - a.recoveredAmount), 0) FROM DriverAdvance a
            WHERE a.driver.id = :driverId AND a.isDeleted = false AND a.status = 'ISSUED'
            """)
    BigDecimal sumOutstanding(@Param("driverId") Long driverId);

    /** Oldest first, locked: recovery is applied FIFO and must not race another posting. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM DriverAdvance a
            WHERE a.driver.id = :driverId AND a.isDeleted = false AND a.status = 'ISSUED'
              AND a.recoveredAmount < a.amount
            ORDER BY a.advanceDate ASC, a.id ASC
            """)
    List<DriverAdvance> lockOutstandingForDriver(@Param("driverId") Long driverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DriverAdvance a WHERE a.id = :id AND a.isDeleted = false")
    Optional<DriverAdvance> findByIdForUpdate(@Param("id") Long id);
}
