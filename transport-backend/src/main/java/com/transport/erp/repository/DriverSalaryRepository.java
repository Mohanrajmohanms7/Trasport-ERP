package com.transport.erp.repository;

import com.transport.erp.model.DriverSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DriverSalaryRepository extends JpaRepository<DriverSalary, Long> {
    Optional<DriverSalary> findByDriverIdAndIsDeletedFalse(Long driverId);
}
