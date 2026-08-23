package com.transport.erp.repository;

import com.transport.erp.model.MaterialPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialPriceRepository extends JpaRepository<MaterialPrice, Long> {
    List<MaterialPrice> findByMaterialIdAndIsDeletedFalse(Long materialId);
}
