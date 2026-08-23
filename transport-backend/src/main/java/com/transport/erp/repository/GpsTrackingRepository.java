package com.transport.erp.repository;

import com.transport.erp.model.GpsTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GpsTrackingRepository extends JpaRepository<GpsTracking, Long> {
    List<GpsTracking> findByVehicleIdAndIsDeletedFalseOrderByPingTimeDesc(Long vehicleId);
    Page<GpsTracking> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
