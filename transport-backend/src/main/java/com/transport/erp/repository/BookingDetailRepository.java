package com.transport.erp.repository;

import com.transport.erp.model.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    List<BookingDetail> findByBookingIdAndIsDeletedFalse(Long bookingId);

    @Query("SELECT COUNT(bd) FROM BookingDetail bd WHERE bd.material.id = :materialId AND bd.booking.isDeleted = false")
    long countByMaterialIdAndBookingIsDeletedFalse(@Param("materialId") Long materialId);
}
