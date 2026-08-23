package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TenantAccessService tenantAccess;

    @GetMapping
    public ApiResponse<Page<AppUser>> getUsers(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort sorting = Sort.by(sort[0].equals("id") && sort[1].equals("desc") 
                ? Sort.Direction.DESC : Sort.Direction.ASC, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        // Default to companyId 1 if not provided for dev stages
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Page<AppUser> data = userService.getUsers(targetCompanyId, search, pageable);
        
        return ApiResponse.success(data, "Users fetched successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<AppUser> getUserById(@PathVariable Long id) {
        AppUser user = userService.getUserById(id);
        return ApiResponse.success(user, "User details fetched successfully");
    }

    @PostMapping
    public ApiResponse<AppUser> createUser(@Valid @RequestBody AppUser user) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        // Default company and branch ID if not set for dev stages
        user.setCompanyId(tenantAccess.resolveCompanyId(user.getCompanyId()));
        if (user.getBranchId() == null) user.setBranchId(1L);
        
        AppUser created = userService.createUser(user, activeUser);
        return ApiResponse.success(created, "User account created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<AppUser> updateUser(@PathVariable Long id, @Valid @RequestBody AppUser user) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser updated = userService.updateUser(id, user, activeUser);
        return ApiResponse.success(updated, "User details updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        String activeUser = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.deleteUser(id, activeUser);
        return ApiResponse.success(null, "User soft deleted successfully");
    }
}
