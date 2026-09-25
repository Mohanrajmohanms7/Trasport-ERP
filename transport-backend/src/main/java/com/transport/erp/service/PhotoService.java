package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.DriverRepository;
import com.transport.erp.repository.SparePartRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/** Identification photos: vehicle (truck), driver (face for ID / verification), spare part (identify the part). */
@Service
public class PhotoService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private SparePartRepository sparePartRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private AuditService auditService;

    @Transactional
    public String setPhoto(String type, Long id, MultipartFile file, String username) {
        if (file == null || file.getContentType() == null || !IMAGE_TYPES.contains(file.getContentType().toLowerCase())) {
            throw new BusinessValidationException("Image Required", "PHOTO_NOT_IMAGE",
                    "The photo must be a JPG, PNG or WEBP image.", "Choose an image file.");
        }
        BaseEntity record = load(type, id);
        Map<String, Object> stored = fileStorageService.storeFile(file);
        String name = (String) stored.get("fileName");
        apply(record, name);
        record.setUpdatedBy(username);
        save(type, record);
        auditService.log(username, "PHOTO_UPDATED", type, id, null, "Photo set for " + type + " " + id);
        return name;
    }

    @Transactional
    public void removePhoto(String type, Long id, String username) {
        BaseEntity record = load(type, id);
        apply(record, null);
        record.setUpdatedBy(username);
        save(type, record);
        auditService.log(username, "PHOTO_REMOVED", type, id, null, "Photo removed for " + type + " " + id);
    }

    private BaseEntity load(String type, Long id) {
        BaseEntity r = switch (type) {
            case "vehicles" -> vehicleRepository.findById(id).orElse(null);
            case "drivers" -> driverRepository.findById(id).orElse(null);
            case "spare-parts" -> sparePartRepository.findById(id).orElse(null);
            default -> throw new BusinessValidationException("Unsupported", "PHOTO_TYPE_UNSUPPORTED",
                    "Photos are available for vehicles, drivers and spare parts.", "Use a supported record type.");
        };
        if (r == null || Boolean.TRUE.equals(r.getIsDeleted())) throw new IllegalArgumentException("Record not found: " + id);
        tenantAccess.assertOwned(r.getCompanyId());
        return r;
    }

    private static void apply(BaseEntity r, String name) {
        if (r instanceof Vehicle v) v.setPhotoFile(name);
        else if (r instanceof Driver d) d.setPhotoFile(name);
        else if (r instanceof SparePart p) p.setPhotoFile(name);
    }

    private void save(String type, BaseEntity r) {
        switch (type) {
            case "vehicles" -> vehicleRepository.save((Vehicle) r);
            case "drivers" -> driverRepository.save((Driver) r);
            default -> sparePartRepository.save((SparePart) r);
        }
    }
}
