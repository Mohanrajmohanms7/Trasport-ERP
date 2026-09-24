package com.transport.erp.repository;

import com.transport.erp.model.WorkOrderPart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderPartRepository extends JpaRepository<WorkOrderPart, Long> {

    @Query("SELECT p FROM WorkOrderPart p JOIN FETCH p.sparePart JOIN FETCH p.uom "
            + "WHERE p.workOrder.id = :workOrderId AND p.isDeleted = false ORDER BY p.id")
    List<WorkOrderPart> findActiveByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Query("SELECT p FROM WorkOrderPart p JOIN FETCH p.sparePart JOIN FETCH p.uom "
            + "WHERE p.id = :id AND p.workOrder.id = :workOrderId AND p.isDeleted = false")
    Optional<WorkOrderPart> findActiveLine(@Param("id") Long id, @Param("workOrderId") Long workOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM WorkOrderPart p JOIN FETCH p.sparePart "
            + "WHERE p.id = :id AND p.workOrder.id = :workOrderId AND p.isDeleted = false")
    Optional<WorkOrderPart> findActiveLineForUpdate(@Param("id") Long id, @Param("workOrderId") Long workOrderId);
}
