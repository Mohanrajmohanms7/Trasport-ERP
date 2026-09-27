package com.transport.erp.repository;

import com.transport.erp.model.LookupValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {
    Optional<LookupValue> findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(Long companyId, String type, String code);
    List<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalse(Long companyId, String type);
    Page<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalse(Long companyId, String type, Pageable pageable);
    /** Parenthesised on purpose: the derived-name version OR-ed the code match outside the company filter. */
    @Query("SELECT e FROM LookupValue e WHERE e.companyId = :companyId AND e.isDeleted = false AND e.type = :type AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("companyId") Long companyId, @Param("type") String type, @Param("name") String name, @Param("code") String code, Pageable pageable);
}
