package com.transport.erp.repository;

import com.transport.erp.model.SaaSSupportTicketEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaaSSupportTicketEventRepository extends JpaRepository<SaaSSupportTicketEvent, Long> {
    List<SaaSSupportTicketEvent> findByTicketIdOrderByCreatedDateAsc(Long ticketId);
}
