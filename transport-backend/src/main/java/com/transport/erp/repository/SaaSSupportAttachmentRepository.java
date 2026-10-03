package com.transport.erp.repository;

import com.transport.erp.model.SaaSSupportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaaSSupportAttachmentRepository extends JpaRepository<SaaSSupportAttachment, Long> {

    /** File list without loading the file bytes. */
    @Query("SELECT new map(a.id as id, a.fileName as fileName, a.contentType as contentType, a.sizeBytes as sizeBytes, "
            + "a.isInternal as isInternal, a.uploadedBy as uploadedBy, a.fromSupport as fromSupport, a.createdDate as createdDate) "
            + "FROM SaaSSupportAttachment a WHERE a.ticketId = :ticketId ORDER BY a.createdDate")
    List<java.util.Map<String, Object>> listMeta(@Param("ticketId") Long ticketId);

    long countByTicketId(Long ticketId);
}
