package com.transport.erp.repository;

import com.transport.erp.model.VehicleServiceLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleServiceLogRepository extends JpaRepository<VehicleServiceLog, Long> {
    List<VehicleServiceLog> findByVehicleIdAndIsDeletedFalse(Long vehicleId);
    Optional<VehicleServiceLog> findFirstByAttachmentPathContainingAndIsDeletedFalse(String attachmentPath);

    long countByVehicleIdAndIsDeletedFalse(Long vehicleId);

    long countBySupplierIdAndIsDeletedFalse(Long supplierId);

    Optional<VehicleServiceLog> findByCompanyIdAndReferenceNumberAndIsDeletedFalse(Long companyId, String referenceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM VehicleServiceLog s WHERE s.id = :id AND (s.isDeleted IS NULL OR s.isDeleted = false)")
    Optional<VehicleServiceLog> findAndLockById(@Param("id") Long id);
}

