package com.transport.erp.repository;

import com.transport.erp.model.SaaSTenantSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SaaSTenantSubscriptionRepository extends JpaRepository<SaaSTenantSubscription, Long> {
    List<SaaSTenantSubscription> findByCompanyId(Long companyId);
    Page<SaaSTenantSubscription> findByCompanyId(Long companyId, Pageable pageable);
}
