package com.transport.erp.security;

import com.transport.erp.model.AppUser;
import com.transport.erp.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the authenticated user's tenant scope.
 * SUPER_ADMIN may access any company; all other roles are locked to their own companyId.
 */
@Component
public class TenantAccessService {

    @Autowired
    private AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public AppUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsernameAndIsDeletedFalse(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }

    public boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                String code = authority.getAuthority();
                if ("SUPER_ADMIN".equals(code) || "ROLE_SUPER_ADMIN".equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** DB-backed check used when SecurityContext authorities are incomplete. */
    public boolean isSuperAdmin(AppUser user) {
        if (isSuperAdmin()) return true;
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getCode()));
    }

    /**
     * Assert the given company-scoped entity belongs to the caller.
     * Call after loading any record by id for read/update/delete.
     */
    public void assertOwned(Long resourceCompanyId) {
        assertCompanyAccess(resourceCompanyId);
    }

    /**
     * Returns the company id the caller is allowed to use.
     * Non–super-admins are always bound to their own companyId (client value is ignored).
     */
    @Transactional(readOnly = true)
    public Long resolveCompanyId(Long requestedCompanyId) {
        AppUser user = requireCurrentUser();
        if (isSuperAdmin(user)) {
            if (requestedCompanyId != null) return requestedCompanyId;
            return user.getCompanyId();
        }
        if (user.getCompanyId() == null) {
            throw new AccessDeniedException("User is not assigned to a company");
        }
        // Never trust client-supplied companyId for tenant users — stale localStorage
        // was causing false "Access denied to another company's data" on create.
        return user.getCompanyId();
    }

    @Transactional(readOnly = true)
    public void assertCompanyAccess(Long resourceCompanyId) {
        AppUser user = requireCurrentUser();
        if (isSuperAdmin(user)) return;
        if (resourceCompanyId == null || user.getCompanyId() == null
                || !sameCompany(resourceCompanyId, user.getCompanyId())) {
            throw new AccessDeniedException("Access denied to another company's data");
        }
    }

    private static boolean sameCompany(Long a, Long b) {
        return a != null && b != null && a.longValue() == b.longValue();
    }
}
