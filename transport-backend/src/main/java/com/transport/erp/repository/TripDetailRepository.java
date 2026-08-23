package com.transport.erp.repository;

import com.transport.erp.model.TripDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TripDetailRepository extends JpaRepository<TripDetail, Long> {
    List<TripDetail> findByTripIdAndIsDeletedFalse(Long tripId);
}
