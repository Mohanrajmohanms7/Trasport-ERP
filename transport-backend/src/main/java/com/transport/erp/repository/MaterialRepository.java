package com.transport.erp.repository;

import com.transport.erp.model.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Material> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    /** Parenthesised on purpose: the derived-name version OR-ed the code match outside the company filter. */
    @Query("SELECT e FROM Material e WHERE e.companyId = :companyId AND e.isDeleted = false AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<Material> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("companyId") Long companyId, @Param("name") String name, @Param("code") String code, Pageable pageable);
}
