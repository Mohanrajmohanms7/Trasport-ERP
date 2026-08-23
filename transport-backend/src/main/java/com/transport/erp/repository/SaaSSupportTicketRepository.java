package com.transport.erp.repository;

import com.transport.erp.model.SaaSSupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaaSSupportTicketRepository extends JpaRepository<SaaSSupportTicket, Long> {
    Page<SaaSSupportTicket> findByCompanyId(Long companyId, Pageable pageable);
    Page<SaaSSupportTicket> findByStatus(String status, Pageable pageable);
}
