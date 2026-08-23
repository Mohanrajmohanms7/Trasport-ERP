package com.transport.erp.repository;

import com.transport.erp.model.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {
    List<CustomerContact> findByCustomerIdAndIsDeletedFalse(Long customerId);
}
