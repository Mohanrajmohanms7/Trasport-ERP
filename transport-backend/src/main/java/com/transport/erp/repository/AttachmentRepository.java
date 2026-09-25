package com.transport.erp.repository;

import com.transport.erp.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByCompanyIdAndEntityTypeAndEntityIdAndIsDeletedFalseOrderByIdDesc(Long companyId, String entityType, Long entityId);

    Optional<Attachment> findFirstByFileNameAndIsDeletedFalse(String fileName);
}
