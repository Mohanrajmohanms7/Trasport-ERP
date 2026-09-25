package com.transport.erp.repository;

import com.transport.erp.model.DriverAdvanceRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverAdvanceRecoveryRepository extends JpaRepository<DriverAdvanceRecovery, Long> {
    List<DriverAdvanceRecovery> findByPayrollIdAndStatusAndIsDeletedFalse(Long payrollId, String status);

    List<DriverAdvanceRecovery> findByAdvanceIdAndIsDeletedFalse(Long advanceId);
}
