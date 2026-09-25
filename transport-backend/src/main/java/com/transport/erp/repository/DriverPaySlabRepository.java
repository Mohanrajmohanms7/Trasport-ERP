package com.transport.erp.repository;

import com.transport.erp.model.DriverPaySlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverPaySlabRepository extends JpaRepository<DriverPaySlab, Long> {
    List<DriverPaySlab> findByCompanyIdAndIsDeletedFalseOrderByTripsFromAsc(Long companyId);
}
