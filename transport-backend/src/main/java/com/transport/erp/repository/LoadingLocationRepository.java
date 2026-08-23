package com.transport.erp.repository;

import com.transport.erp.model.LoadingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoadingLocationRepository extends JpaRepository<LoadingLocation, Long> {
    Optional<LoadingLocation> findByLocationCodeAndIsDeletedFalse(String locationCode);

    Optional<LoadingLocation> findByCompanyIdAndLocationCodeAndIsDeletedFalse(Long companyId, String locationCode);

    List<LoadingLocation> findByCompanyIdAndIsDeletedFalse(Long companyId);
}
