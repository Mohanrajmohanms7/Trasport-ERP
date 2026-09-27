package com.transport.erp.repository;

import com.transport.erp.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCodeAndIsDeletedFalse(String code);
    Page<Company> findByIsDeletedFalse(Pageable pageable);
    /** Parenthesised on purpose: the derived-name version OR-ed the code match outside the company filter. */
    @Query("SELECT e FROM Company e WHERE e.isDeleted = false AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(e.code) LIKE LOWER(CONCAT('%', :code, '%')))")
    Page<Company> findByIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("name") String name, @Param("code") String code, Pageable pageable);
}
