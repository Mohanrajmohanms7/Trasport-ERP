package com.transport.erp.repository;

import com.transport.erp.model.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {
    List<CustomerDocument> findByCustomerIdAndIsDeletedFalse(Long customerId);
}
