package com.transport.erp.service;

import com.transport.erp.dto.ServiceHistoryResponse;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.Vehicle;
import com.transport.erp.model.VehicleServiceLog;
import com.transport.erp.model.WorkOrder;
import com.transport.erp.repository.VehicleServiceLogRepository;
import com.transport.erp.repository.WorkOrderRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.security.TenantParentAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VehicleServiceHistoryService {

    static final String SOURCE_WORK_ORDER = "WORK_ORDER";
    static final String SOURCE_SERVICE_LOG = "SERVICE_LOG";

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private VehicleServiceLogRepository serviceLogRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private TenantParentAccess parentAccess;

    @Transactional(readOnly = true)
    public Page<ServiceHistoryResponse> history(Long vehicleId, Pageable pageable) {
        AppUser user = tenantAccess.requireCurrentUser();
        Vehicle vehicle = parentAccess.requireVehicle(vehicleId);
        if (!tenantAccess.isSuperAdmin(user)
                && user.getBranchId() != null
                && vehicle.getBranchId() != null
                && !user.getBranchId().equals(vehicle.getBranchId())) {
            throw new AccessDeniedException("Access denied: Vehicle belongs to another branch.");
        }

        List<ServiceHistoryResponse> rows = new ArrayList<>();
        for (WorkOrder order : workOrderRepository.findCompletedForVehicle(vehicle.getCompanyId(), vehicleId)) {
            rows.add(fromWorkOrder(order));
        }
        for (VehicleServiceLog log : serviceLogRepository.findActiveHistory(vehicle.getCompanyId(), vehicleId)) {
            rows.add(fromServiceLog(log));
        }
        rows.sort(Comparator
                .comparing(ServiceHistoryResponse::getServiceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ServiceHistoryResponse::getSourceId, Comparator.nullsLast(Comparator.reverseOrder())));

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new PageImpl<>(rows.subList(from, to), pageable, rows.size());
    }

    private ServiceHistoryResponse fromWorkOrder(WorkOrder order) {
        ServiceHistoryResponse dto = new ServiceHistoryResponse();
        dto.setId(order.getId());
        dto.setSourceType(SOURCE_WORK_ORDER);
        dto.setSourceId(order.getId());
        dto.setWorkOrderNumber(order.getWorkOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setServiceType(order.getMaintenanceType());
        dto.setDescription(order.getName());
        dto.setServiceDate(order.getCompletedAt() != null ? order.getCompletedAt().toLocalDate() : null);
        dto.setOdometerKm(order.getOdometerAtComplete());
        dto.setEstimatedCost(order.getEstimatedCost());
        dto.setActualCost(order.getActualCost());
        dto.setFinancialAmount(WorkOrderFinancialPosting.financialAmount(order.getActualCost()));
        dto.setAttachmentPath(order.getAttachmentPath());
        dto.setCreatedAt(order.getCompletedAt());
        applyVehicle(dto, order.getVehicle());
        if (order.getSupplier() != null) {
            dto.setSupplierName(order.getSupplier().getName());
        }
        return dto;
    }

    private ServiceHistoryResponse fromServiceLog(VehicleServiceLog log) {
        ServiceHistoryResponse dto = new ServiceHistoryResponse();
        dto.setId(log.getId());
        dto.setSourceType(SOURCE_SERVICE_LOG);
        dto.setSourceId(log.getId());
        dto.setStatus(log.getStatus());
        dto.setServiceType(log.getServiceType());
        dto.setDescription(log.getName() != null ? log.getName() : log.getRemarks());
        dto.setServiceDate(log.getServiceDate());
        dto.setActualCost(log.getCost());
        dto.setFinancialAmount(log.getCost());
        dto.setAttachmentPath(log.getAttachmentPath());
        dto.setCreatedAt(log.getCreatedDate());
        applyVehicle(dto, log.getVehicle());
        if (log.getSupplier() != null && log.getSupplier().getName() != null) {
            dto.setSupplierName(log.getSupplier().getName());
        } else {
            dto.setSupplierName(log.getWorkshop());
        }
        return dto;
    }

    private void applyVehicle(ServiceHistoryResponse dto, Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        dto.setVehicleId(vehicle.getId());
        dto.setVehicleRegistrationNumber(vehicle.getCode());
    }
}
