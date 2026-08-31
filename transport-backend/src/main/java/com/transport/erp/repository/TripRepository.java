package com.transport.erp.repository;

import com.transport.erp.model.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;


import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTripNumberAndIsDeletedFalse(String tripNumber);

    Page<Trip> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Trip> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);

    long countByCompanyIdAndTripDateAndIsDeletedFalse(Long companyId, LocalDate tripDate);

    long countByCompanyIdAndStatusInAndIsDeletedFalse(Long companyId, Collection<String> statuses);

    long countByCompanyIdAndTripDateAndStatusAndIsDeletedFalse(Long companyId, LocalDate tripDate, String status);

    long countByCompanyIdAndTripDateAndStatusInAndIsDeletedFalse(Long companyId, LocalDate tripDate, Collection<String> statuses);

    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);

    @Query("""
            SELECT COUNT(DISTINCT t.vehicle.id) FROM Trip t
            WHERE t.companyId = :companyId AND t.isDeleted = false
              AND t.status IN :statuses AND t.vehicle IS NOT NULL
            """)
    long countDistinctVehiclesOnTrips(@Param("companyId") Long companyId,
                                      @Param("statuses") Collection<String> statuses);

    List<Trip> findTop5ByCompanyIdAndIsDeletedFalseOrderByIdDesc(Long companyId);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.companyId = :companyId AND t.isDeleted = false AND t.status = 'COMPLETED'
              AND (:branchId IS NULL OR t.branchId = :branchId)
              AND NOT EXISTS (
                SELECT 1 FROM SalesInvoice i JOIN i.details d
                WHERE d.trip.id = t.id AND i.isDeleted = false AND i.status != 'CANCELLED'
              )
            """)
    Page<Trip> findCompletedTripsReadyForBilling(@Param("companyId") Long companyId, @Param("branchId") Long branchId, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id AND t.isDeleted = false")
    Optional<Trip> findAndLockById(@Param("id") Long id);
}

