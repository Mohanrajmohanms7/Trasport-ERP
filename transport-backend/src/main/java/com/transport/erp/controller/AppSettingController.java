package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppSetting;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.AppSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin(origins = "*")
public class AppSettingController {

    @Autowired
    private AppSettingService settingService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Map<String, String>> getSettings(
            @RequestParam(required = false) Long companyId) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        List<AppSetting> settings = settingService.getSettings(scopedCompanyId);
        Map<String, String> data = new HashMap<>();
        for (AppSetting s : settings) {
            data.put(s.getKeyName(), s.getValueData());
        }
        return ApiResponse.success(data, "Application settings fetched successfully");
    }

    @PutMapping
    public ApiResponse<Map<String, String>> saveSettings(
            @RequestParam(required = false) Long companyId,
            @RequestBody Map<String, String> payload) {
        Long scopedCompanyId = tenantAccess.resolveCompanyId(companyId);
        Map<String, String> response = new HashMap<>();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            AppSetting updated = settingService.saveOrUpdate(entry.getKey(), entry.getValue(), scopedCompanyId);
            response.put(updated.getKeyName(), updated.getValueData());
        }
        return ApiResponse.success(response, "Application settings updated successfully");
    }
}
