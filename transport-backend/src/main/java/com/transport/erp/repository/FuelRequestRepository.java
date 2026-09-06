package com.transport.erp.repository;

import com.transport.erp.model.FuelRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelRequestRepository extends JpaRepository<FuelRequest, Long> {

    Page<FuelRequest> findByCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    Page<FuelRequest> findByCompanyIdAndIsDeletedFalseAndStatus(Long companyId, String status, Pageable pageable);

    Optional<FuelRequest> findByRequestNumberAndIsDeletedFalse(String requestNumber);

    List<FuelRequest> findByTripIdAndIsDeletedFalse(Long tripId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FuelRequest r WHERE r.id = :id AND r.isDeleted = false")
    Optional<FuelRequest> findByIdForUpdate(@Param("id") Long id);
}
