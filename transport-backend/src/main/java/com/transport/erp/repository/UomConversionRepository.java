package com.transport.erp.repository;

import com.transport.erp.model.UomConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UomConversionRepository extends JpaRepository<UomConversion, Long> {

    List<UomConversion> findByIsDeletedFalse();

    Page<UomConversion> findByIsDeletedFalse(Pageable pageable);

    List<UomConversion> findByCompanyIdAndIsDeletedFalse(Long companyId);

    Page<UomConversion> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    @Query("SELECT c FROM UomConversion c WHERE c.isDeleted = false " +
           "AND c.material.id = :materialId " +
           "AND c.fromUom.id = :fromUomId " +
           "AND c.toUom.id = :toUomId")
    Optional<UomConversion> findMaterialSpecificConversion(
            @Param("materialId") Long materialId,
            @Param("fromUomId") Long fromUomId,
            @Param("toUomId") Long toUomId);

    @Query("SELECT c FROM UomConversion c WHERE c.isDeleted = false " +
           "AND c.material IS NULL " +
           "AND c.fromUom.id = :fromUomId " +
           "AND c.toUom.id = :toUomId")
    Optional<UomConversion> findGlobalConversion(
            @Param("fromUomId") Long fromUomId,
            @Param("toUomId") Long toUomId);

    List<UomConversion> findByMaterialIdAndIsDeletedFalse(Long materialId);
}
