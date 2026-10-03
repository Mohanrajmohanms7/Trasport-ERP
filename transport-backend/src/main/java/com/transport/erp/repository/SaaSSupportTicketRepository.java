package com.transport.erp.repository;

import com.transport.erp.model.SaaSSupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SaaSSupportTicketRepository extends JpaRepository<SaaSSupportTicket, Long> {
    Page<SaaSSupportTicket> findByCompanyId(Long companyId, Pageable pageable);
    Page<SaaSSupportTicket> findByStatus(String status, Pageable pageable);

    /** Client list: always one company; username = only that user's tickets (null for a company admin). */
    @Query("""
            SELECT t FROM SaaSSupportTicket t
            WHERE t.companyId = :companyId
              AND (:username IS NULL OR t.username = :username)
              AND (:status IS NULL OR t.status = :status)
              AND (:q = '' OR LOWER(t.ticketNumber) LIKE CONCAT('%', :q, '%') OR LOWER(t.subject) LIKE CONCAT('%', :q, '%')
                   OR LOWER(COALESCE(t.module, '')) LIKE CONCAT('%', :q, '%'))
            """)
    Page<SaaSSupportTicket> searchForClient(@Param("companyId") Long companyId, @Param("username") String username,
                                            @Param("status") String status, @Param("q") String q, Pageable pageable);

    /** Platform admin list across all clients. */
    @Query("""
            SELECT t FROM SaaSSupportTicket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:companyId IS NULL OR t.companyId = :companyId)
              AND (:module IS NULL OR t.module = :module)
              AND (:assignedTo IS NULL OR t.assignedTo = :assignedTo)
              AND (:overdue = false OR (t.status IN ('OPEN', 'IN_PROGRESS') AND t.resolveDue < :now))
              AND (:q = '' OR LOWER(t.ticketNumber) LIKE CONCAT('%', :q, '%') OR LOWER(t.subject) LIKE CONCAT('%', :q, '%')
                   OR LOWER(t.username) LIKE CONCAT('%', :q, '%')
                   OR t.companyId IN (SELECT c.id FROM Company c WHERE LOWER(c.name) LIKE CONCAT('%', :q, '%')))
            """)
    Page<SaaSSupportTicket> searchForAdmin(@Param("status") String status, @Param("priority") String priority,
                                           @Param("companyId") Long companyId, @Param("module") String module,
                                           @Param("assignedTo") String assignedTo, @Param("q") String q,
                                           @Param("overdue") boolean overdue, @Param("now") LocalDateTime now, Pageable pageable);
}
