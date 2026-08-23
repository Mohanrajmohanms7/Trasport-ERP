package com.transport.erp.repository;

import com.transport.erp.model.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    /** Prefer company-scoped lookup — key_name is unique per tenant, not globally. */
    Optional<AppSetting> findByKeyNameAndCompanyIdAndIsDeletedFalse(String keyName, Long companyId);

    List<AppSetting> findByCompanyIdAndIsDeletedFalse(Long companyId);

    List<AppSetting> findByKeyNameAndIsDeletedFalse(String keyName);
}
