package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.model.AppPermission;
import com.transport.erp.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<AppPermission>> getPermissions() {
        List<AppPermission> permissions = permissionService.getPermissions();
        return ApiResponse.success(permissions, "Permissions fetched successfully");
    }
}
