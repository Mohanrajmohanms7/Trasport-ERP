package com.transport.erp.repository;

import com.transport.erp.model.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {
    List<DriverDocument> findByDriverIdAndIsDeletedFalse(Long driverId);
}
