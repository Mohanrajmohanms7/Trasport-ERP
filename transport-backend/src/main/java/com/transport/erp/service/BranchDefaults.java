package com.transport.erp.service;

import com.transport.erp.model.AppUser;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Picks the branch for a new branch-based master (vehicle, driver). */
@Service
public class BranchDefaults {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TenantAccessService tenantAccess;

    public Long resolve(Long companyId, Long requested) {
        if (requested != null) {
            List<Long> ok = jdbc.queryForList("SELECT id FROM branches WHERE id = ? AND company_id = ? AND is_deleted = false",
                    Long.class, requested, companyId);
            if (ok.isEmpty()) throw new AccessDeniedException("Branch does not belong to this company.");
            return requested;
        }
        AppUser user = tenantAccess.requireCurrentUser();
        if (user.getBranchId() != null && companyId.equals(user.getCompanyId())) return user.getBranchId();
        List<Long> ho = jdbc.queryForList("SELECT MIN(id) FROM branches WHERE company_id = ? AND is_deleted = false", Long.class, companyId);
        return ho.isEmpty() ? null : ho.get(0);
    }
}
