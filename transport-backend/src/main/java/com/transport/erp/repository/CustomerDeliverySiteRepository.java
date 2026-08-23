package com.transport.erp.repository;

import com.transport.erp.model.CustomerDeliverySite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerDeliverySiteRepository extends JpaRepository<CustomerDeliverySite, Long> {
    List<CustomerDeliverySite> findByCustomerIdAndIsDeletedFalse(Long customerId);
}
