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

    /**
     * Returns the branch id the caller is allowed to use for transaction creation.
     * Non–super-admins are always bound to their own branchId (client value is ignored).
     * If user has no branch context, creation is safely rejected instead of defaulting to 1L.
     */
    @Transactional(readOnly = true)
    public Long resolveBranchId(Long requestedBranchId) {
        AppUser user = requireCurrentUser();
        if (isSuperAdmin(user)) {
            if (requestedBranchId != null) return requestedBranchId;
            if (user.getBranchId() != null) return user.getBranchId();
            throw new AccessDeniedException("User branch context is required to create this transaction.");
        }
        if (user.getBranchId() == null) {
            throw new AccessDeniedException("User branch context is required to create this transaction.");
        }
        return user.getBranchId();
    }

    @Transactional(readOnly = true)
    public void assertBranchAccess(Long resourceBranchId) {
        AppUser user = requireCurrentUser();
        if (isSuperAdmin(user)) return;
        if (resourceBranchId == null || user.getBranchId() == null
                || !sameBranch(resourceBranchId, user.getBranchId())) {
            throw new AccessDeniedException("Access denied: Trip belongs to another branch.");
        }
    }

    private static boolean sameCompany(Long a, Long b) {
        return a != null && b != null && a.longValue() == b.longValue();
    }

    private static boolean sameBranch(Long a, Long b) {
        return a != null && b != null && a.longValue() == b.longValue();
    }
}

