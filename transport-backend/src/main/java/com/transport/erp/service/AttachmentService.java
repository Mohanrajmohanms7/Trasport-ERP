package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Documents attached to business records. Supported record types and what they are for:
 * BOOKING (customer PO / order copy), INVOICE (signed copy, e-way bill, POD), RECEIPT (cheque / UPI proof),
 * EXPENSE and FUEL_ENTRY (bills), WORK_ORDER (garage bills, parts invoices), MAINTENANCE_REQUEST (damage photos),
 * DRIVER_ADVANCE and DRIVER_PAYROLL (signed vouchers), SUPPLIER (GST certificate, agreements),
 * JOURNAL_VOUCHER (supporting papers).
 */
@Service
public class AttachmentService {

    @Autowired private AttachmentRepository attachmentRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private TenantAccessService tenantAccess;
    @Autowired private AuditService auditService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private CustomerReceiptRepository customerReceiptRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequestRepository;
    @Autowired private DriverAdvanceRepository driverAdvanceRepository;
    @Autowired private DriverPayrollRepository driverPayrollRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private JournalVoucherRepository journalVoucherRepository;

    public static final int MAX_PER_RECORD = 20;

    private JpaRepository<? extends BaseEntity, Long> repoFor(String type) {
        return switch (type) {
            case "BOOKING" -> bookingRepository;
            case "INVOICE" -> salesInvoiceRepository;
            case "RECEIPT" -> customerReceiptRepository;
            case "EXPENSE" -> expenseRepository;
            case "FUEL_ENTRY" -> fuelEntryRepository;
            case "WORK_ORDER" -> workOrderRepository;
            case "MAINTENANCE_REQUEST" -> maintenanceRequestRepository;
            case "DRIVER_ADVANCE" -> driverAdvanceRepository;
            case "DRIVER_PAYROLL" -> driverPayrollRepository;
            case "SUPPLIER" -> supplierRepository;
            case "JOURNAL_VOUCHER" -> journalVoucherRepository;
            default -> throw new BusinessValidationException("Unsupported Record Type", "ATTACHMENT_TYPE_UNSUPPORTED",
                    "Attachments are not available for " + type + ".", "Use one of the supported record types.");
        };
    }

    /** Loads the owning record and checks company, branch and (for driver logins) ownership. */
    private BaseEntity requireRecord(String type, Long id, boolean write) {
        if (id == null) throw new IllegalArgumentException("Record id is required.");
        BaseEntity record = repoFor(type).findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + type + " " + id));
        AppUser user = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(user)) {
            tenantAccess.assertCompanyAccess(record.getCompanyId());
        }
        if (isDriverOnly(user)) {
            boolean own = record instanceof MaintenanceRequest mr && mr.getRequestedBy() != null
                    && user.getId().equals(mr.getRequestedBy().getId());
            if (!own) throw new AccessDeniedException("Drivers can only attach files to their own maintenance requests.");
        }
        return record;
    }

    private static boolean isDriverOnly(AppUser user) {
        return user.getRoles() != null && !user.getRoles().isEmpty()
                && user.getRoles().stream().allMatch(r -> "DRIVER".equals(r.getCode()));
    }

    private static String normType(String type) {
        return type == null ? "" : type.trim().toUpperCase().replace('-', '_');
    }

    @Transactional(readOnly = true)
    public List<Attachment> list(String entityType, Long entityId) {
        String type = normType(entityType);
        BaseEntity record = requireRecord(type, entityId, false);
        return attachmentRepository.findByCompanyIdAndEntityTypeAndEntityIdAndIsDeletedFalseOrderByIdDesc(record.getCompanyId(), type, entityId);
    }

    @Transactional
    public Attachment add(String entityType, Long entityId, MultipartFile file, String category, String remarks, String username) {
        String type = normType(entityType);
        BaseEntity record = requireRecord(type, entityId, true);
        List<Attachment> existing = attachmentRepository
                .findByCompanyIdAndEntityTypeAndEntityIdAndIsDeletedFalseOrderByIdDesc(record.getCompanyId(), type, entityId);
        if (existing.size() >= MAX_PER_RECORD) {
            throw new BusinessValidationException("Too Many Attachments", "ATTACHMENT_LIMIT",
                    "This record already has " + existing.size() + " attachments.", "Remove an old attachment before adding another.");
        }
        Map<String, Object> stored = fileStorageService.storeFile(file);
        Attachment a = new Attachment();
        a.setEntityType(type);
        a.setEntityId(entityId);
        a.setFileName((String) stored.get("fileName"));
        a.setOriginalName((String) stored.get("originalName"));
        a.setMimeType((String) stored.get("mimeType"));
        a.setFileSize(((Number) stored.get("fileSize")).longValue());
        a.setCategory(category != null && !category.isBlank() ? category.trim().toUpperCase() : "OTHER");
        a.setRemarks(remarks);
        a.setCompanyId(record.getCompanyId());
        a.setBranchId(record.getBranchId());
        a.setCode("ATT-" + type);
        a.setName(a.getOriginalName().length() > 150 ? a.getOriginalName().substring(0, 150) : a.getOriginalName());
        a.setStatus("ACTIVE");
        a.setIsDeleted(false);
        a.setCreatedBy(username);
        a.setUpdatedBy(username);
        Attachment saved = attachmentRepository.save(a);
        auditService.log(username, "ATTACHMENT_ADDED", "attachments", saved.getId(), null,
                "Attached " + saved.getOriginalName() + " to " + type + " " + entityId);
        return saved;
    }

    /** Uploader or an admin may remove an attachment. */
    @Transactional
    public void remove(Long id, String username) {
        Attachment a = attachmentRepository.findById(id).filter(x -> !Boolean.TRUE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + id));
        AppUser user = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(user)) tenantAccess.assertCompanyAccess(a.getCompanyId());
        boolean admin = tenantAccess.isSuperAdmin(user) || (user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "COMPANY_ADMIN".equals(r.getCode()) || "ADMIN".equals(r.getCode())));
        if (!admin && (a.getCreatedBy() == null || !a.getCreatedBy().equals(username))) {
            throw new AccessDeniedException("Only the person who uploaded it or an admin can remove this attachment.");
        }
        a.setIsDeleted(true);
        a.setUpdatedBy(username);
        attachmentRepository.save(a);
        auditService.log(username, "ATTACHMENT_REMOVED", "attachments", a.getId(), null,
                "Removed " + a.getOriginalName() + " from " + a.getEntityType() + " " + a.getEntityId());
    }
}
