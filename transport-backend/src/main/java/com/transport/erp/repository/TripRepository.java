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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id IN :ids AND t.isDeleted = false")
    List<Trip> findAndLockAllByIds(@Param("ids") java.util.Collection<Long> ids);

    /** Completed trips per business date for one driver in a date range (one grouped query per payroll). */
    @Query("""
            SELECT t.tripDate, COUNT(t) FROM Trip t
            WHERE t.driver.id = :driverId AND t.companyId = :companyId
              AND t.isDeleted = false AND t.status IN :statuses
              AND t.tripDate BETWEEN :fromDate AND :toDate
            GROUP BY t.tripDate
            ORDER BY t.tripDate
            """)
    List<Object[]> countTripsByDate(@Param("driverId") Long driverId, @Param("companyId") Long companyId,
                                    @Param("statuses") Collection<String> statuses,
                                    @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /** Quantity already moved per material for a booking (delivered if recorded, else planned), excluding one trip. */
    @Query("""
            SELECT d.material.id,
                   COALESCE(SUM(CASE WHEN d.deliveredQuantity IS NOT NULL AND d.deliveredQuantity > 0
                                     THEN d.deliveredQuantity ELSE d.quantity END), 0)
            FROM TripDetail d
            WHERE d.trip.booking.id = :bookingId
              AND d.trip.isDeleted = false AND d.isDeleted = false
              AND d.trip.status <> 'CANCELLED'
              AND d.trip.id <> :excludeTripId
            GROUP BY d.material.id
            """)
    List<Object[]> sumQuantityByMaterialForBooking(@Param("bookingId") Long bookingId, @Param("excludeTripId") Long excludeTripId);

    /** Quantity delivered per material by COMPLETED trips of a booking. */
    @Query("""
            SELECT d.material.id,
                   COALESCE(SUM(CASE WHEN d.deliveredQuantity IS NOT NULL AND d.deliveredQuantity > 0
                                     THEN d.deliveredQuantity ELSE d.quantity END), 0)
            FROM TripDetail d
            WHERE d.trip.booking.id = :bookingId
              AND d.trip.isDeleted = false AND d.isDeleted = false
              AND d.trip.status = 'COMPLETED'
            GROUP BY d.material.id
            """)
    List<Object[]> sumDeliveredByMaterialForBooking(@Param("bookingId") Long bookingId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.booking.id = :bookingId AND t.isDeleted = false AND t.status IN ('PLANNED', 'DISPATCHED')")
    long countOpenTripsForBooking(@Param("bookingId") Long bookingId);

    long countByVehicleIdAndIsDeletedFalse(Long vehicleId);

    long countByDriverIdAndIsDeletedFalse(Long driverId);

    long countByBookingIdAndIsDeletedFalse(Long bookingId);
}

