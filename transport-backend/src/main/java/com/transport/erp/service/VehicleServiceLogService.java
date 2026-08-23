package com.transport.erp.service;

import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleServiceLog;
import com.transport.erp.repository.VehicleServiceLogRepository;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleServiceLogService {

    @Autowired
    private VehicleServiceLogRepository logRepository;

    @Autowired
    private TenantParentAccess parentAccess;

    public List<VehicleServiceLog> getLogsByVehicle(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return logRepository.findByVehicleIdAndIsDeletedFalse(vehicleId);
    }

    @Transactional
    public VehicleServiceLog addLog(Long vehicleId, VehicleServiceLog log) {
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);

        log.setVehicle(vehicle);
        log.setIsDeleted(false);
        log.setCode(log.getServiceType() + "_" + System.currentTimeMillis());
        log.setName(log.getServiceType() + " Maintenance");
        log.setCompanyId(vehicle.getCompanyId());
        log.setBranchId(vehicle.getBranchId());

        return logRepository.save(log);
    }
}
