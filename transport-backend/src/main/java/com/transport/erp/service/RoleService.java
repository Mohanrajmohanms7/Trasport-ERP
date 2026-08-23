package com.transport.erp.service;

import com.transport.erp.model.AppRole;
import com.transport.erp.repository.AppRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.transport.erp.security.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RoleService {

    @Autowired
    private AppRoleRepository roleRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    public List<AppRole> getRoles() {
        return roleRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted())
                .toList();
    }

    public List<AppRole> getRoles(Long companyId) {
        if (companyId == null) {
            return getRoles();
        }
        return roleRepository.findAll().stream()
                .filter(r -> !r.getIsDeleted())
                .filter(r -> companyId.equals(r.getCompanyId()))
                .toList();
    }

    public AppRole getRoleById(Long id) {
        AppRole role = roleRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        if (role.getCompanyId() != null) {
            tenantAccess.assertOwned(role.getCompanyId());
        } else if (!tenantAccess.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied to system role");
        }
        return role;
    }

    @Transactional
    public AppRole createRole(AppRole role, String createdByUsername) {
        boolean codeTaken = role.getCompanyId() == null
                ? roleRepository.findAllByCodeAndIsDeletedFalse(role.getCode()).stream()
                    .anyMatch(r -> r.getCompanyId() == null)
                : roleRepository.findByCodeAndCompanyIdAndIsDeletedFalse(role.getCode(), role.getCompanyId()).isPresent();
        if (codeTaken) {
            throw new IllegalArgumentException("Role code/name is already taken: " + role.getCode());
        }

        role.setIsDeleted(false);
        role.setCreatedBy(createdByUsername);
        role.setUpdatedBy(createdByUsername);

        AppRole savedRole = roleRepository.save(role);

        // Audit log
        auditService.log(createdByUsername, "ROLE_CREATED", "app_roles", savedRole.getId(), null,
                "Created role: " + savedRole.getCode());

        return savedRole;
    }

    @Transactional
    public AppRole updateRole(Long id, AppRole roleDetails, String updatedByUsername) {
        AppRole existingRole = getRoleById(id);

        existingRole.setName(roleDetails.getName());
        existingRole.setDescription(roleDetails.getDescription());
        existingRole.setStatus(roleDetails.getStatus());
        existingRole.setPermissions(roleDetails.getPermissions());
        existingRole.setUpdatedBy(updatedByUsername);

        AppRole updatedRole = roleRepository.save(existingRole);

        // Audit log
        auditService.log(updatedByUsername, "ROLE_UPDATED", "app_roles", updatedRole.getId(), null,
                "Updated role: " + updatedRole.getCode());

        return updatedRole;
    }

    @Transactional
    public void deleteRole(Long id, String deletedByUsername) {
        AppRole role = getRoleById(id);
        role.setIsDeleted(true);
        role.setUpdatedBy(deletedByUsername);
        roleRepository.save(role);

        // Audit log
        auditService.log(deletedByUsername, "ROLE_DELETED", "app_roles", role.getId(), null,
                "Soft deleted role: " + role.getCode());
    }
}
