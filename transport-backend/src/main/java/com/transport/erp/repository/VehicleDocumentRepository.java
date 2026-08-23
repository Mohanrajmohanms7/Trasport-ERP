package com.transport.erp.repository;

import com.transport.erp.model.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    List<VehicleDocument> findByVehicleIdAndIsDeletedFalse(Long vehicleId);
}
