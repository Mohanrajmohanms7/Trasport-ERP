package com.transport.erp.repository;

import com.transport.erp.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);
    Page<Supplier> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);
    /** Parenthesised on purpose: the derived-name version OR-ed the code match outside the company filter. */
    @Query("SELECT e FROM Supplier e WHERE e.companyId = :companyId AND e.isDeleted = false AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<Supplier> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("companyId") Long companyId, @Param("name") String name, @Param("code") String code, Pageable pageable);

    /** Dropdown search: one status (e.g. ACTIVE) and an optional name/code text; empty text = all. */
    @Query("SELECT e FROM Supplier e WHERE e.companyId = :companyId AND e.isDeleted = false AND COALESCE(e.status, 'ACTIVE') = :status "
            + "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Supplier> searchByStatus(@Param("companyId") Long companyId, @Param("q") String q, @Param("status") String status, Pageable pageable);
}
