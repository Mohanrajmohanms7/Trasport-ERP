package com.transport.erp.repository;

import com.transport.erp.model.WorkOrderLabour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderLabourRepository extends JpaRepository<WorkOrderLabour, Long> {

    @Query("SELECT l FROM WorkOrderLabour l LEFT JOIN FETCH l.appUser "
            + "WHERE l.workOrder.id = :workOrderId AND l.isDeleted = false ORDER BY l.id")
    List<WorkOrderLabour> findActiveByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Query("SELECT l FROM WorkOrderLabour l LEFT JOIN FETCH l.appUser "
            + "WHERE l.id = :id AND l.workOrder.id = :workOrderId AND l.isDeleted = false")
    Optional<WorkOrderLabour> findActiveLine(@Param("id") Long id, @Param("workOrderId") Long workOrderId);
}
