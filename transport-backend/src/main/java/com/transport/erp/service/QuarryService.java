package com.transport.erp.service;

import com.transport.erp.model.Quarry;
import com.transport.erp.repository.QuarryRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QuarryService {

    @Autowired
    private QuarryRepository quarryRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public Page<Quarry> getAll(Long companyId, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return quarryRepository.findByCompanyIdAndIsDeletedFalseAndNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    companyId, search, search, pageable);
        }
        return quarryRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Optional<Quarry> getById(Long id) {
        return quarryRepository.findById(id)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .map(q -> {
                    tenantAccess.assertOwned(q.getCompanyId());
                    return q;
                });
    }

    @Transactional
    public Quarry create(Quarry quarry) {
        Long companyId = tenantAccess.resolveCompanyId(quarry.getCompanyId());
        quarry.setCompanyId(companyId);
        if (quarryRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, quarry.getCode()).isPresent()) {
            throw new IllegalArgumentException("Quarry code already exists: " + quarry.getCode());
        }
        quarry.setIsDeleted(false);
        return quarryRepository.save(quarry);
    }

    @Transactional
    public Quarry update(Long id, Quarry quarryDetails) {
        Quarry quarry = quarryRepository.findById(id)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Quarry not found: " + id));
        tenantAccess.assertOwned(quarry.getCompanyId());

        Optional<Quarry> existing = quarryRepository.findByCompanyIdAndCodeAndIsDeletedFalse(
                quarry.getCompanyId(), quarryDetails.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Quarry code already exists: " + quarryDetails.getCode());
        }

        quarry.setCode(quarryDetails.getCode());
        quarry.setName(quarryDetails.getName());
        quarry.setDescription(quarryDetails.getDescription());
        quarry.setStatus(quarryDetails.getStatus());
        quarry.setLocationAddress(quarryDetails.getLocationAddress());
        quarry.setOwnerName(quarryDetails.getOwnerName());
        quarry.setContactNumber(quarryDetails.getContactNumber());
        quarry.setGstNumber(quarryDetails.getGstNumber());
        quarry.setLicenseNumber(quarryDetails.getLicenseNumber());
        quarry.setLatitude(quarryDetails.getLatitude());
        quarry.setLongitude(quarryDetails.getLongitude());
        quarry.setWorkingHours(quarryDetails.getWorkingHours());

        return quarryRepository.save(quarry);
    }

    @Transactional
    public void delete(Long id) {
        Quarry quarry = quarryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quarry not found: " + id));
        tenantAccess.assertOwned(quarry.getCompanyId());
        quarry.setIsDeleted(true);
        quarryRepository.save(quarry);
    }

    @Transactional
    public Quarry toggleStatus(Long id) {
        Quarry quarry = quarryRepository.findById(id)
                .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Quarry not found: " + id));
        tenantAccess.assertOwned(quarry.getCompanyId());
        quarry.setStatus("ACTIVE".equals(quarry.getStatus()) ? "INACTIVE" : "ACTIVE");
        return quarryRepository.save(quarry);
    }
}
