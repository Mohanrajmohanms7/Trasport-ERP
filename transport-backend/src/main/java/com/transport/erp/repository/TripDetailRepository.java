package com.transport.erp.repository;

import com.transport.erp.model.TripDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TripDetailRepository extends JpaRepository<TripDetail, Long> {
    List<TripDetail> findByTripIdAndIsDeletedFalse(Long tripId);

    @Query("SELECT COUNT(td) FROM TripDetail td WHERE td.material.id = :materialId AND td.trip.isDeleted = false")
    long countByMaterialIdAndTripIsDeletedFalse(@Param("materialId") Long materialId);
}
