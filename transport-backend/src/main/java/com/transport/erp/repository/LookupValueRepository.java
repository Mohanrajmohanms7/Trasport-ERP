package com.transport.erp.repository;

import com.transport.erp.model.LookupValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {
    Optional<LookupValue> findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(Long companyId, String type, String code);
    List<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalse(Long companyId, String type);
    Page<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalse(Long companyId, String type, Pageable pageable);
    Page<LookupValue> findByCompanyIdAndTypeAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(Long companyId, String type, String name, String code, Pageable pageable);
}
