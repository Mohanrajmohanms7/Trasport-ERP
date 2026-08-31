package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.GpsTracking;
import com.transport.erp.repository.GpsTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GpsTrackingService {

    @Autowired
    private GpsTrackingRepository gpsRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private com.transport.erp.security.TenantParentAccess parentAccess;

    @Autowired
    private AuditService auditService;

    public List<GpsTracking> getVehicleRoute(Long vehicleId) {
        parentAccess.requireVehicle(vehicleId);
        return gpsRepository.findByVehicleIdAndIsDeletedFalseOrderByPingTimeDesc(vehicleId);
    }

    @Transactional
    public GpsTracking createPing(GpsTracking ping, String username) {
        if (ping.getVehicle() == null || ping.getVehicle().getId() == null) {
            throw new IllegalArgumentException("Vehicle is required for GPS ping");
        }
        var vehicle = parentAccess.requireVehicle(ping.getVehicle().getId());
        ping.setVehicle(vehicle);

        ping.setPingTime(LocalDateTime.now());
        ping.setIsDeleted(false);
        ping.setCreatedBy(username);
        ping.setUpdatedBy(username);

        ping.setCompanyId(tenantAccess.resolveCompanyId(vehicle.getCompanyId()));
        if (ping.getBranchId() == null) {
            ping.setBranchId(vehicle.getBranchId() != null ? vehicle.getBranchId() : tenantAccess.resolveBranchId(null));
        }

        if (ping.getCode() == null) ping.setCode("GPS-" + System.currentTimeMillis());
        if (ping.getName() == null) ping.setName("GPS Ping Update");

        GpsTracking saved = gpsRepository.save(ping);

        auditService.log(username, "GPS_PING_RECEIVED", "gps_trackings", saved.getId(), null,
                "Received GPS telemetry update for vehicle ID: " + saved.getVehicle().getId() +
                " Lat: " + saved.getLatitude() + " Lng: " + saved.getLongitude());

        return saved;
    }
}
