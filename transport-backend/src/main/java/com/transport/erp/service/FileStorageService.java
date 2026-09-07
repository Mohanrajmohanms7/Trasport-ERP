package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
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

    public FileStorageService(@Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Map<String, Object> storeFile(MultipartFile file) {
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

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
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

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }
}
