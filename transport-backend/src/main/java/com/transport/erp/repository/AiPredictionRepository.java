package com.transport.erp.repository;

import com.transport.erp.model.AiPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {
    Page<AiPrediction> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
}
