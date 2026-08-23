package com.transport.erp.repository;

import com.transport.erp.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByCompanyIdAndCodeAndIsDeletedFalse(Long companyId, String code);

    Page<Vehicle> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Vehicle> findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
            Long companyId, String name, String code, Pageable pageable);

    long countByCompanyIdAndIsDeletedFalse(Long companyId);

    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);

    @Query("""
            SELECT COUNT(v) FROM Vehicle v
            WHERE v.companyId = :companyId AND v.isDeleted = false
              AND v.insuranceExpiryDate IS NOT NULL
              AND v.insuranceExpiryDate BETWEEN :from AND :to
            """)
    long countExpiringInsurance(@Param("companyId") Long companyId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(v) FROM Vehicle v
            WHERE v.companyId = :companyId AND v.isDeleted = false
              AND v.permitExpiryDate IS NOT NULL
              AND v.permitExpiryDate BETWEEN :from AND :to
            """)
    long countExpiringPermit(@Param("companyId") Long companyId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(v) FROM Vehicle v
            WHERE v.companyId = :companyId AND v.isDeleted = false
              AND v.fitnessExpiryDate IS NOT NULL
              AND v.fitnessExpiryDate BETWEEN :from AND :to
            """)
    long countExpiringFitness(@Param("companyId") Long companyId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
