package com.transport.erp.repository;

import com.transport.erp.model.CompanyUom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyUomRepository extends JpaRepository<CompanyUom, Long> {

    List<CompanyUom> findByCompanyIdAndIsDeletedFalse(Long companyId);
}
