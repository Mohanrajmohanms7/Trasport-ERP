package com.transport.erp.repository;

import com.transport.erp.model.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findFirstByStoredNameAndIsDeletedFalse(String storedName);
}
