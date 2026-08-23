package com.transport.erp.repository;

import com.transport.erp.model.SaaSSupportReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SaaSSupportReplyRepository extends JpaRepository<SaaSSupportReply, Long> {
    List<SaaSSupportReply> findByTicketIdOrderByCreatedDateAsc(Long ticketId);
}
