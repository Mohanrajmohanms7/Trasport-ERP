package com.transport.erp.service;

import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public Page<Vehicle> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return vehicleRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return vehicleRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Vehicle> getById(Long id) {
        return vehicleRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .map(v -> {
                    tenantAccess.assertOwned(v.getCompanyId());
                    return v;
                });
    }

    @Transactional
    public Vehicle create(Vehicle vehicle) {
        Long companyId = tenantAccess.resolveCompanyId(vehicle.getCompanyId());
        vehicle.setCompanyId(companyId);
        if (vehicleRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, vehicle.getCode()).isPresent()) {
            throw new IllegalArgumentException("Vehicle plate/reg code already exists: " + vehicle.getCode());
        }
        vehicle.setIsDeleted(false);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle update(Long id, Vehicle vehicleDetails) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id));
        tenantAccess.assertOwned(vehicle.getCompanyId());

        Optional<Vehicle> existing = vehicleRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                vehicle.getCompanyId(), vehicleDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Vehicle plate/reg code already exists: " + vehicleDetails.getCode());
        }

        vehicle.setCode(vehicleDetails.getCode());
        vehicle.setName(vehicleDetails.getName());
        vehicle.setDescription(vehicleDetails.getDescription());
        vehicle.setStatus(vehicleDetails.getStatus());
        vehicle.setChassisNumber(vehicleDetails.getChassisNumber());
        vehicle.setEngineNumber(vehicleDetails.getEngineNumber());
        vehicle.setModel(vehicleDetails.getModel());
        vehicle.setBrand(vehicleDetails.getBrand());
        vehicle.setType(vehicleDetails.getType());
        vehicle.setCategory(vehicleDetails.getCategory());
        vehicle.setCapacity(vehicleDetails.getCapacity());
        vehicle.setOwnerName(vehicleDetails.getOwnerName());
        vehicle.setOwnerType(vehicleDetails.getOwnerType());
        vehicle.setPurchaseDate(vehicleDetails.getPurchaseDate());
        vehicle.setInsuranceExpiryDate(vehicleDetails.getInsuranceExpiryDate());
        vehicle.setFitnessExpiryDate(vehicleDetails.getFitnessExpiryDate());
        vehicle.setPermitExpiryDate(vehicleDetails.getPermitExpiryDate());
        // Never allow moving a record to another company via update body
        if (vehicleDetails.getBranchId() != null) {
            vehicle.setBranchId(vehicleDetails.getBranchId());
        }

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id));
        tenantAccess.assertOwned(vehicle.getCompanyId());
        vehicle.setIsDeleted(true);
        vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle toggleStatus(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id));
        tenantAccess.assertOwned(vehicle.getCompanyId());
        vehicle.setStatus("ACTIVE".equals(vehicle.getStatus()) ? "INACTIVE" : "ACTIVE");
        return vehicleRepository.save(vehicle);
    }
}
