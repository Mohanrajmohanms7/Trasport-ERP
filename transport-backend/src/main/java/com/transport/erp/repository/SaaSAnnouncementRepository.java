package com.transport.erp.repository;

import com.transport.erp.model.SaaSAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaaSAnnouncementRepository extends JpaRepository<SaaSAnnouncement, Long> {
    List<SaaSAnnouncement> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String status, LocalDate currentDate1, LocalDate currentDate2);
    Page<SaaSAnnouncement> findByStatus(String status, Pageable pageable);
}
