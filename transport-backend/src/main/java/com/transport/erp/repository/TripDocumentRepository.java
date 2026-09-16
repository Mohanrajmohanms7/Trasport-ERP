package com.transport.erp.repository;

import com.transport.erp.model.TripDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripDocumentRepository extends JpaRepository<TripDocument, Long> {
    List<TripDocument> findByTripIdAndIsDeletedFalse(Long tripId);
    java.util.Optional<TripDocument> findFirstByFilePathContainingAndIsDeletedFalse(String filePath);
    java.util.Optional<TripDocument> findFirstByFileNameAndIsDeletedFalse(String fileName);
}
