package com.transport.erp.repository;

import com.transport.erp.model.UomMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UomMasterRepository extends JpaRepository<UomMaster, Long> {

    Optional<UomMaster> findByCodeAndIsDeletedFalse(String code);

    Optional<UomMaster> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    List<UomMaster> findByIsDeletedFalse();

    Page<UomMaster> findByIsDeletedFalse(Pageable pageable);

    List<UomMaster> findByCompanyIdAndIsDeletedFalse(Long companyId);

    Page<UomMaster> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    @Query("SELECT u FROM UomMaster u WHERE u.isDeleted = false AND " +
           "(:companyId IS NULL OR u.companyId IS NULL OR u.companyId = :companyId) AND " +
           "(LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UomMaster> searchUoms(@Param("companyId") Long companyId, @Param("search") String search, Pageable pageable);
}
