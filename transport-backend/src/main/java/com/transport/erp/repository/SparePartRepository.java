package com.transport.erp.repository;

import com.transport.erp.model.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    @Query(
            value = "SELECT p FROM SparePart p JOIN FETCH p.defaultUom "
                    + "WHERE p.companyId = :companyId AND p.isDeleted = false",
            countQuery = "SELECT COUNT(p) FROM SparePart p WHERE p.companyId = :companyId AND p.isDeleted = false")
    Page<SparePart> findActiveByCompany(@Param("companyId") Long companyId, Pageable pageable);
}
