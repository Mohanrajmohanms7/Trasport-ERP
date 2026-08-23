package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class AppSettingService {

    @Autowired
    private AppSettingRepository settingRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<AppSetting> getSettings() {
        Long companyId = tenantAccess.resolveCompanyId(null);
        return getSettings(companyId);
    }

    public List<AppSetting> getSettings(Long companyId) {
        Long scoped = companyId != null ? companyId : tenantAccess.resolveCompanyId(null);
        return settingRepository.findByCompanyIdAndIsDeletedFalse(scoped);
    }

    /** Resolves setting for the current tenant (never global key-only lookup). */
    public Optional<AppSetting> getByKey(String keyName) {
        return getByKey(keyName, tenantAccess.resolveCompanyId(null));
    }

    public Optional<AppSetting> getByKey(String keyName, Long companyId) {
        Long scoped = companyId != null ? companyId : tenantAccess.resolveCompanyId(null);
        return settingRepository.findByKeyNameAndCompanyIdAndIsDeletedFalse(keyName, scoped);
    }

    @Transactional
    public AppSetting saveOrUpdate(String keyName, String valueData, Long companyId) {
        Long cid = tenantAccess.resolveCompanyId(companyId);
        Optional<AppSetting> existing = settingRepository.findByKeyNameAndCompanyIdAndIsDeletedFalse(keyName, cid);
        AppSetting setting;

        if (existing.isPresent()) {
            setting = existing.get();
            setting.setValueData(valueData);
        } else {
            setting = new AppSetting();
            setting.setKeyName(keyName);
            setting.setValueData(valueData);
            setting.setCode(keyName);
            setting.setName(keyName);
            setting.setCompanyId(cid);
            setting.setBranchId(1L);
            setting.setIsDeleted(false);
        }

        return settingRepository.save(setting);
    }
}
