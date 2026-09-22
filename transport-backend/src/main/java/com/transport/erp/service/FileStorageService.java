package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private CustomerDocumentRepository customerDocumentRepository;

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private DriverDocumentRepository driverDocumentRepository;

    @Autowired
    private TripDocumentRepository tripDocumentRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private VehicleServiceLogRepository vehicleServiceLogRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    public FileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Map<String, Object> storeFile(MultipartFile file) {
        tenantAccess.requireCurrentUser();

        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("FILE_EMPTY", "File Upload Failed", "Uploaded file is empty.", "Please select a valid file to upload.");
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFileName.contains("..")) {
            throw new BusinessValidationException("PATH_TRAVERSAL", "Invalid File", "Filename contains invalid path sequence: " + originalFileName, "Rename file and try again.");
        }

        String ext = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessValidationException("INVALID_FILE_TYPE", "Unsupported File Type", "File extension '." + ext + "' is not supported.", "Allowed file types: JPG, JPEG, PNG, WEBP, PDF.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessValidationException("INVALID_MIME_TYPE", "Unsupported File Content", "MIME type '" + contentType + "' is not allowed.", "Allowed formats: JPEG, PNG, WEBP images and PDF documents.");
        }

        long maxSize = ext.equals("pdf") ? 10 * 1024 * 1024 : 5 * 1024 * 1024; // 10MB for PDF, 5MB for images
        if (file.getSize() > maxSize) {
            long maxMb = maxSize / (1024 * 1024);
            throw new BusinessValidationException("FILE_SIZE_EXCEEDED", "File Too Large", "File size (" + (file.getSize() / 1024) + " KB) exceeds maximum limit of " + maxMb + " MB.", "Please compress the file or choose a smaller attachment.");
        }

        String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = "/api/v1/files/download/" + storedFileName;

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", storedFileName);
            result.put("originalName", originalFileName);
            result.put("fileUrl", fileUrl);
            result.put("mimeType", contentType != null ? contentType : "application/octet-stream");
            result.put("fileSize", file.getSize());

            return result;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    @Transactional(readOnly = true)
    public Resource loadFileAsResource(String fileName) {
        if (fileName == null || fileName.contains("..")) {
            throw new BusinessValidationException("PATH_TRAVERSAL", "Invalid File", "Filename contains invalid path sequence: " + fileName, "Rename file and try again.");
        }

        String cleanName = StringUtils.cleanPath(fileName);
        if (cleanName.contains("..")) {
            throw new BusinessValidationException("PATH_TRAVERSAL", "Invalid File", "Filename contains invalid path sequence: " + cleanName, "Rename file and try again.");
        }

        AppUser currentUser = tenantAccess.requireCurrentUser();
        validateFileAccess(cleanName, currentUser);

        try {
            Path filePath = this.fileStorageLocation.resolve(cleanName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new BusinessValidationException("PATH_TRAVERSAL", "Invalid File", "File path outside upload directory", "Access denied.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessValidationException("FILE_NOT_FOUND", "File Not Found", "The requested document file could not be located.", "Verify document record or re-upload attachment.");
            }
        } catch (MalformedURLException ex) {
            throw new BusinessValidationException("FILE_NOT_FOUND", "File Not Found", "Malformed file path URL.", "Verify document identification.");
        }
    }

    public void validateFileAccess(String cleanName, AppUser currentUser) {
        if (tenantAccess.isSuperAdmin(currentUser)) {
            return;
        }

        Long owningCompanyId = null;
        Long owningBranchId = null;

        Optional<CustomerDocument> customerDoc = customerDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(cleanName);
        if (customerDoc.isPresent()) {
            Customer c = customerDoc.get().getCustomer();
            if (c != null) {
                owningCompanyId = c.getCompanyId();
                owningBranchId = c.getBranchId();
            }
        }

        if (owningCompanyId == null) {
            Optional<VehicleDocument> vehicleDoc = vehicleDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(cleanName);
            if (vehicleDoc.isPresent()) {
                Vehicle v = vehicleDoc.get().getVehicle();
                if (v != null) {
                    owningCompanyId = v.getCompanyId();
                    owningBranchId = v.getBranchId();
                }
            }
        }

        if (owningCompanyId == null) {
            Optional<DriverDocument> driverDoc = driverDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(cleanName);
            if (driverDoc.isPresent()) {
                Driver d = driverDoc.get().getDriver();
                if (d != null) {
                    owningCompanyId = d.getCompanyId();
                    owningBranchId = d.getBranchId();
                }
            }
        }

        if (owningCompanyId == null) {
            Optional<TripDocument> tripDoc = tripDocumentRepository.findFirstByFilePathContainingAndIsDeletedFalse(cleanName);
            if (tripDoc.isEmpty()) {
                tripDoc = tripDocumentRepository.findFirstByFileNameAndIsDeletedFalse(cleanName);
            }
            if (tripDoc.isPresent()) {
                Trip t = tripDoc.get().getTrip();
                if (t != null) {
                    owningCompanyId = t.getCompanyId();
                    owningBranchId = t.getBranchId();
                }
            }
        }

        if (owningCompanyId == null) {
            Optional<Expense> expense = expenseRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(cleanName);
            if (expense.isPresent()) {
                owningCompanyId = expense.get().getCompanyId();
                owningBranchId = expense.get().getBranchId();
            }
        }

        if (owningCompanyId == null) {
            Optional<FuelEntry> fuelEntry = fuelEntryRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(cleanName);
            if (fuelEntry.isPresent()) {
                owningCompanyId = fuelEntry.get().getCompanyId();
                owningBranchId = fuelEntry.get().getBranchId();
            }
        }

        if (owningCompanyId == null) {
            Optional<VehicleServiceLog> serviceLog = vehicleServiceLogRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(cleanName);
            if (serviceLog.isPresent()) {
                owningCompanyId = serviceLog.get().getCompanyId();
                owningBranchId = serviceLog.get().getBranchId();
            }
        }

        if (owningCompanyId == null) {
            Optional<WorkOrder> workOrder = workOrderRepository.findFirstByAttachmentPathContainingAndIsDeletedFalse(cleanName);
            if (workOrder.isPresent()) {
                owningCompanyId = workOrder.get().getCompanyId();
                owningBranchId = workOrder.get().getBranchId();
            }
        }

        if (owningCompanyId == null) {
            throw new AccessDeniedException("Access denied: File ownership could not be verified.");
        }

        tenantAccess.assertCompanyAccess(owningCompanyId);

        if (owningBranchId != null && currentUser.getBranchId() != null) {
            tenantAccess.assertBranchAccess(owningBranchId);
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}
