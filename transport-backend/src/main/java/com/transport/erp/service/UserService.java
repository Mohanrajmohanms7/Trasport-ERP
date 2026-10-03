package com.transport.erp.service;

import com.transport.erp.model.AppUser;
import com.transport.erp.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.transport.erp.security.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @org.springframework.beans.factory.annotation.Autowired
    private PlanLimitService planLimits;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TenantAccessService tenantAccess;
    
    @Autowired
    private com.transport.erp.repository.BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.transport.erp.repository.AppRoleRepository roleRepository;

    private static final java.util.Set<String> ADMIN_ROLE_CODES = java.util.Set.of("SUPER_ADMIN", "COMPANY_ADMIN", "ADMIN");

    /**
     * Roles are re-read from the database by id: a role must belong to the user's company (or be a global role),
     * SUPER_ADMIN can only be granted by a super admin, and admin roles only by an admin.
     */
    private java.util.Set<com.transport.erp.model.AppRole> resolveRoles(java.util.Set<com.transport.erp.model.AppRole> requested, Long companyId) {
        java.util.Set<com.transport.erp.model.AppRole> out = new java.util.HashSet<>();
        if (requested == null) return out;
        boolean callerSuper = tenantAccess.isSuperAdmin();
        for (com.transport.erp.model.AppRole ref : requested) {
            if (ref == null || ref.getId() == null) continue;
            com.transport.erp.model.AppRole role = roleRepository.findById(ref.getId())
                    .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + ref.getId()));
            if (role.getCompanyId() != null && !role.getCompanyId().equals(companyId)) {
                throw new org.springframework.security.access.AccessDeniedException("Role " + role.getCode() + " belongs to another company.");
            }
            if ("SUPER_ADMIN".equals(role.getCode()) && !callerSuper) {
                throw new org.springframework.security.access.AccessDeniedException("Only a platform super admin can grant SUPER_ADMIN.");
            }
            out.add(role);
        }
        return out;
    }

    public Page<AppUser> getUsers(Long companyId, String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return userRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
        }
        return userRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                companyId, search, search, pageable);
    }

    public AppUser getUserById(Long id) {
        AppUser user = userRepository.findById(id)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        tenantAccess.assertOwned(user.getCompanyId());
        return user;
    }

    @Transactional
    public AppUser createUser(AppUser user, String createdByUsername) {
        // Validation checks
        if (userRepository.findByUsernameAndIsDeletedFalse(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken: " + user.getUsername());
        }
        if (user.getCode() != null
                && userRepository.findByCompanyIdAndCodeAndIsDeletedFalse(user.getCompanyId(), user.getCode()).isPresent()) {
            throw new IllegalArgumentException("Employee Code is already taken in this company: " + user.getCode());
        }

        // Dynamically resolve branch ID if null
        if (user.getBranchId() == null) {
            org.springframework.data.domain.Page<com.transport.erp.model.Branch> branches = 
                branchRepository.findByCompanyIdAndIsDeletedFalse(user.getCompanyId(), org.springframework.data.domain.PageRequest.of(0, 1));
            if (branches.hasContent()) {
                user.setBranchId(branches.getContent().get(0).getId());
            } else {
                user.setBranchId(null);
            }

        }

        // Company is always the caller's own (a super admin may pick one); branch must belong to it.
        user.setCompanyId(tenantAccess.resolveCompanyId(user.getCompanyId()));
        if (user.getBranchId() != null) {
            com.transport.erp.model.Branch branch = branchRepository.findById(user.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + user.getBranchId()));
            if (!user.getCompanyId().equals(branch.getCompanyId())) {
                throw new org.springframework.security.access.AccessDeniedException("Branch belongs to another company.");
            }
        }
        user.setRoles(resolveRoles(user.getRoles(), user.getCompanyId()));
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsDeleted(false);
        user.setCreatedBy(createdByUsername);
        user.setUpdatedBy(createdByUsername);

        if (!driverOnly(user) && (user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()))) {
            planLimits.assertCanAdd(user.getCompanyId(), PlanLimitService.Kind.USERS);
        }
        AppUser savedUser = userRepository.save(user);

        // Audit log
        auditService.log(createdByUsername, "USER_CREATED", "app_users", savedUser.getId(), null,
                "Created user account: " + savedUser.getUsername());

        return savedUser;
    }

    @Transactional
    public AppUser updateUser(Long id, AppUser userDetails, String updatedByUsername) {
        AppUser existingUser = getUserById(id);

        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setPhone(userDetails.getPhone());
        if (userDetails.getStatus() != null && !"ACTIVE".equalsIgnoreCase(userDetails.getStatus())) {
            assertNotLockingOut(existingUser, "deactivate");
        }
        if ("ACTIVE".equalsIgnoreCase(userDetails.getStatus()) && !"ACTIVE".equalsIgnoreCase(existingUser.getStatus()) && !driverOnly(existingUser)) {
            planLimits.assertCanAdd(existingUser.getCompanyId(), PlanLimitService.Kind.USERS);
        }
        existingUser.setStatus(userDetails.getStatus());
        if (userDetails.getRoles() != null) {
            String me = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                    ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : null;
            java.util.Set<com.transport.erp.model.AppRole> newRoles = resolveRoles(userDetails.getRoles(), existingUser.getCompanyId());
            java.util.Set<Long> oldIds = existingUser.getRoles().stream().map(com.transport.erp.model.AppRole::getId).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> newIds = newRoles.stream().map(com.transport.erp.model.AppRole::getId).collect(java.util.stream.Collectors.toSet());
            if (me != null && me.equals(existingUser.getUsername()) && !oldIds.equals(newIds)) {
                throw new org.springframework.security.access.AccessDeniedException("You cannot change your own roles.");
            }
            boolean wasAdmin = existingUser.getRoles().stream().anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()));
            boolean staysAdmin = newRoles.stream().anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()));
            if (wasAdmin && !staysAdmin && otherActiveCompanyAdmins(existingUser) == 0) {
                throw new com.transport.erp.exception.BusinessValidationException("Last Company Admin", "LAST_COMPANY_ADMIN",
                        existingUser.getUsername() + " is the only Company Admin of this company.",
                        "Give another user the Company Admin role first.");
            }
            existingUser.setRoles(newRoles);
        }
        existingUser.setDescription(userDetails.getDescription());
        existingUser.setUpdatedBy(updatedByUsername);

        AppUser updatedUser = userRepository.save(existingUser);

        // Audit log
        auditService.log(updatedByUsername, "USER_UPDATED", "app_users", updatedUser.getId(), null,
                "Updated user details for: " + updatedUser.getUsername());

        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long id, String deletedByUsername) {
        AppUser user = getUserById(id);
        assertNotLockingOut(user, "delete");
        user.setIsDeleted(true);
        user.setUpdatedBy(deletedByUsername);
        userRepository.save(user);

        // Audit log
        auditService.log(deletedByUsername, "USER_DELETED", "app_users", user.getId(), null,
                "Soft deleted user account: " + user.getUsername());
    }

    /** A user cannot delete / deactivate themselves, and a company always keeps one active Company Admin. */
    private void assertNotLockingOut(AppUser target, String action) {
        String me = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : null;
        if (me != null && me.equals(target.getUsername())) {
            throw new com.transport.erp.exception.BusinessValidationException("Own Account", "USER_SELF_" + action.toUpperCase(),
                    "You cannot " + action + " your own login.", "Ask another admin, or keep your account active.");
        }
        boolean isAdmin = target.getRoles() != null && target.getRoles().stream().anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()));
        if (isAdmin && otherActiveCompanyAdmins(target) == 0) {
            throw new com.transport.erp.exception.BusinessValidationException("Last Company Admin", "LAST_COMPANY_ADMIN",
                    target.getUsername() + " is the only Company Admin of this company.",
                    "Give another user the Company Admin role before you " + action + " this login.");
        }
    }

    private long otherActiveCompanyAdmins(AppUser target) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(target.getId()))
                .filter(u -> target.getCompanyId() != null && target.getCompanyId().equals(u.getCompanyId()))
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode())))
                .count();
    }

    /** Driver app logins are not counted against the plan's staff-login limit. */
    private static boolean driverOnly(AppUser u) {
        return u.getRoles() != null && !u.getRoles().isEmpty() && u.getRoles().stream().allMatch(r -> "DRIVER".equals(r.getCode()));
    }
}
