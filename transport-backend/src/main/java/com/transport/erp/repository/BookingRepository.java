package com.transport.erp.repository;

import com.transport.erp.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNumberAndIsDeletedFalse(String bookingNumber);

    Page<Booking> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<Booking> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);

    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, String status);

    List<Booking> findTop5ByCompanyIdAndIsDeletedFalseOrderByIdDesc(Long companyId);

    long countByCustomerIdAndIsDeletedFalse(Long customerId);

    long countByDeliverySiteIdAndIsDeletedFalse(Long deliverySiteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.isDeleted = false")
    Optional<Booking> findAndLockById(@Param("id") Long id);
}
