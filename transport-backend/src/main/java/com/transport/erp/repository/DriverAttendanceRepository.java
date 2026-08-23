package com.transport.erp.repository;

import com.transport.erp.model.DriverAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverAttendanceRepository extends JpaRepository<DriverAttendance, Long> {
    List<DriverAttendance> findByDriverIdAndIsDeletedFalse(Long driverId);
}
