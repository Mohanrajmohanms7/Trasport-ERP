package com.transport.erp.repository;

import com.transport.erp.model.SaaSSystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SaaSSystemSettingRepository extends JpaRepository<SaaSSystemSetting, Long> {
    Optional<SaaSSystemSetting> findByKeyName(String keyName);
}
