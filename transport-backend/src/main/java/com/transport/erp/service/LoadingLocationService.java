package com.transport.erp.service;

import com.transport.erp.model.LoadingLocation;
import com.transport.erp.repository.LoadingLocationRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoadingLocationService {

    @Autowired
    private LoadingLocationRepository locationRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public List<LoadingLocation> getLoadingLocations(Long companyId) {
        Long scoped = tenantAccess.resolveCompanyId(companyId);
        return locationRepository.findByCompanyIdAndIsDeletedFalse(scoped);
    }

    @Transactional
    public LoadingLocation create(LoadingLocation loc) {
        Long companyId = tenantAccess.resolveCompanyId(loc.getCompanyId());
        loc.setCompanyId(companyId);
        if (locationRepository.findByCompanyIdAndLocationCodeAndIsDeletedFalse(companyId, loc.getLocationCode()).isPresent()) {
            throw new IllegalArgumentException("Location code already exists in this company: " + loc.getLocationCode());
        }
        loc.setIsDeleted(false);
        if (loc.getBranchId() == null) loc.setBranchId(1L);
        return locationRepository.save(loc);
    }

    @Transactional
    public LoadingLocation update(Long id, LoadingLocation details) {
        LoadingLocation existing = locationRepository.findById(id)
                .filter(loc -> !Boolean.TRUE.equals(loc.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Loading Location not found: " + id));
        tenantAccess.assertCompanyAccess(existing.getCompanyId());

        if (details.getLocationCode() != null
                && !details.getLocationCode().equals(existing.getLocationCode())) {
            locationRepository.findByCompanyIdAndLocationCodeAndIsDeletedFalse(
                            existing.getCompanyId(), details.getLocationCode())
                    .ifPresent(other -> {
                        throw new IllegalArgumentException(
                                "Location code already exists in this company: " + details.getLocationCode());
                    });
        }

        existing.setLocationCode(details.getLocationCode());
        existing.setLoadingPoint(details.getLoadingPoint());
        existing.setLoadingCharges(details.getLoadingCharges());
        existing.setLatitude(details.getLatitude());
        existing.setLongitude(details.getLongitude());

        return locationRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        LoadingLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loading Location not found: " + id));
        tenantAccess.assertCompanyAccess(loc.getCompanyId());
        loc.setIsDeleted(true);
        locationRepository.save(loc);
    }
}
