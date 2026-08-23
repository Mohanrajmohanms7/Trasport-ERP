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

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

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

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsDeleted(false);
        user.setCreatedBy(createdByUsername);
        user.setUpdatedBy(createdByUsername);

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
        existingUser.setStatus(userDetails.getStatus());
        existingUser.setRoles(userDetails.getRoles());
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
        user.setIsDeleted(true);
        user.setUpdatedBy(deletedByUsername);
        userRepository.save(user);

        // Audit log
        auditService.log(deletedByUsername, "USER_DELETED", "app_users", user.getId(), null,
                "Soft deleted user account: " + user.getUsername());
    }
}
