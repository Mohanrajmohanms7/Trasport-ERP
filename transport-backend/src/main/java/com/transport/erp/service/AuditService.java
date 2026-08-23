package com.transport.erp.service;

import com.transport.erp.model.AuditLog;
import com.transport.erp.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String username, String action, String entityName, Long entityId, String ipAddress, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setActionTime(LocalDateTime.now());
        auditLog.setIpAddress(ipAddress);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }
}
