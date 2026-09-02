package com.transport.erp.repository;

import com.transport.erp.model.FuelEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {
    Optional<FuelEntry> findByFuelEntryNumberAndIsDeletedFalse(String fuelEntryNumber);

    Page<FuelEntry> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    long countByCompanyIdAndFuelDateAndIsDeletedFalse(Long companyId, LocalDate fuelDate);

    @Query("""
            SELECT COALESCE(SUM(f.totalAmount), 0) FROM FuelEntry f
            WHERE f.companyId = :companyId AND f.isDeleted = false
              AND f.fuelDate BETWEEN :from AND :to
            """)
    BigDecimal sumTotalAmountByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    long countByVehicleIdAndIsDeletedFalse(Long vehicleId);

    long countByDriverIdAndIsDeletedFalse(Long driverId);
}
