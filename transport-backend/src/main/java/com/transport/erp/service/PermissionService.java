package com.transport.erp.service;

import com.transport.erp.model.AppPermission;
import com.transport.erp.repository.AppPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PermissionService {

    @Autowired
    private AppPermissionRepository permissionRepository;

    public List<AppPermission> getPermissions() {
        return permissionRepository.findAll().stream()
                .filter(p -> !p.getIsDeleted())
                .toList();
    }
}
