package com.transport.erp.service;

import com.transport.erp.repository.CustomerReceiptAuditRepository;
import com.transport.erp.repository.CustomerReceiptAllocationRepository;
import com.transport.erp.model.CustomerReceiptAudit;
import com.transport.erp.model.CustomerReceiptAllocation;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.Customer;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.dto.CustomerReceiptDTO;
import com.transport.erp.dto.CustomerReceiptResponseDTO;
import com.transport.erp.dto.CustomerReceiptListDTO;
import com.transport.erp.dto.CustomerReceiptDetailDTO;
import com.transport.erp.dto.CustomerReceiptAllocationDTO;
import com.transport.erp.dto.CustomerPaymentHistoryResponseDTO;
import com.transport.erp.dto.CustomerPaymentHistoryDTO;
import com.transport.erp.dto.CustomerReceiptAuditDTO;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.repository.ChartOfAccountRepository;
import com.transport.erp.repository.CustomerRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.service.JournalVoucherService;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.transport.erp.repository.CustomerReceiptPrintAuditRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.model.CustomerReceiptPrintAudit;
import com.transport.erp.model.Company;
import com.transport.erp.model.Branch;
import com.transport.erp.dto.CustomerReceiptPrintDTO;
import com.transport.erp.dto.CustomerReceiptPrintAuditDTO;
import com.transport.erp.dto.CustomerReceiptPrintHistoryDTO;
import com.transport.erp.dto.CustomerReceiptSettlementSummaryDTO;
import com.transport.erp.dto.CustomerOutstandingSummaryDTO;
import com.transport.erp.dto.CustomerInvoiceAgingDTO;
import com.transport.erp.dto.CustomerAgingSummaryDTO;
import com.transport.erp.dto.ReceiptSettlementReconciliationDTO;
import com.transport.erp.dto.CustomerPaymentPerformanceDTO;
import com.transport.erp.dto.ReceiptSettlementDashboardDTO;
import org.springframework.data.domain.PageImpl;
import java.util.Map;
import java.util.HashMap;
import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.CustomerReceipt;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.CustomerReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CustomerReceiptService {

    @Autowired
    private CustomerReceiptAuditRepository auditTrailRepository;

    @Autowired
    private CustomerReceiptAllocationRepository allocationRepository;

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private ChartOfAccountRepository coaRepository;

    @Autowired
    private ChartOfAccountService coaService;

    @Autowired
    private JournalVoucherService jvService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JournalVoucherRepository jvRepository;


    @Autowired
    private CustomerReceiptRepository receiptRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private CustomerLedgerService ledgerService;




    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;

    @Autowired
    private CustomerReceiptPrintAuditRepository printAuditRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BranchRepository branchRepository;




    public Page<CustomerReceipt> getReceipts(Long companyId, Pageable pageable) {
        return receiptRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public CustomerReceipt getReceiptById(Long id) {
        CustomerReceipt receipt = receiptRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + id));
        tenantAccess.assertOwned(receipt.getCompanyId());
        return receipt;
    }

    @Transactional
    public CustomerReceipt createReceipt(CustomerReceipt receipt, String username) {
        String prefix = settingService.getByKey("PREFIX_RECEIPT").map(s -> s.getValueData()).orElse("RCT-");
        receipt.setReceiptNumber(prefix + System.currentTimeMillis());
        receipt.setReceiptDate(LocalDate.now());
        receipt.setIsDeleted(false);
        receipt.setCreatedBy(username);
        receipt.setUpdatedBy(username);

        receipt.setCompanyId(tenantAccess.resolveCompanyId(receipt.getCompanyId()));
        receipt.setBranchId(tenantAccess.resolveBranchId(receipt.getBranchId()));


        CustomerReceipt saved = receiptRepository.save(receipt);

        // Automatically post credit update to customer ledger (receipt reduces outstanding customer liability)
        ledgerService.postToLedger(saved.getCustomer().getId(), saved, BigDecimal.ZERO, saved.getAmountReceived(), username);

        auditService.log(username, "RECEIPT_CREATED", "customer_receipts", saved.getId(), null,
                "Registered customer receipt voucher: " + saved.getReceiptNumber());

        return saved;
    }

    @Transactional
    public CustomerReceipt updateReceipt(Long id, CustomerReceipt details, String username) {
        CustomerReceipt existing = getReceiptById(id);

        if (!"DRAFT".equals(existing.getStatus())) {
            throw new IllegalArgumentException("Only DRAFT receipts can be modified.");
        }

        existing.setAmountReceived(details.getAmountReceived());
        existing.setAdvanceAmount(details.getAdvanceAmount());
        existing.setPaymentMethod(details.getPaymentMethod());
        existing.setReferenceNumber(details.getReferenceNumber());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(username);

        CustomerReceipt saved = receiptRepository.save(existing);

        auditService.log(username, "RECEIPT_UPDATED", "customer_receipts", saved.getId(), null,
                "Updated details for receipt voucher: " + saved.getReceiptNumber());

        return saved;
    }

    @Transactional
    public void deleteReceipt(Long id, String username) {
        CustomerReceipt receipt = getReceiptById(id);
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Only DRAFT receipts can be deleted.");
        }
        receipt.setIsDeleted(true);
        receipt.setUpdatedBy(username);
        receiptRepository.save(receipt);

        auditService.log(username, "RECEIPT_DELETED", "customer_receipts", receipt.getId(), null,
                "Soft deleted receipt voucher: " + receipt.getReceiptNumber());
    }


    @Transactional
    public CustomerReceiptResponseDTO createReceiptWithAllocations(CustomerReceiptDTO dto, String username) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.resolveBranchId(null);


        Customer customer = customerRepository.findById(dto.getCustomerId())
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + dto.getCustomerId()));
        tenantAccess.assertOwned(customer.getCompanyId());
        if (currentUser.getBranchId() != null && customer.getBranchId() != null && !currentUser.getBranchId().equals(customer.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Customer belongs to another branch.");
        }

        if (dto.getAmountReceived() == null || dto.getAmountReceived().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount received must be greater than zero.");
        }

        CustomerReceipt receipt = new CustomerReceipt();
        String prefix = settingService.getByKey("PREFIX_RECEIPT").map(s -> s.getValueData()).orElse("RCT-");
        receipt.setReceiptNumber(prefix + System.currentTimeMillis());
        receipt.setReceiptDate(dto.getReceiptDate() != null ? dto.getReceiptDate() : LocalDate.now());
        receipt.setCustomer(customer);
        receipt.setAmountReceived(dto.getAmountReceived());
        receipt.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "CASH");
        receipt.setReferenceNumber(dto.getReferenceNumber());
        receipt.setRemarks(dto.getRemarks());
        receipt.setIsDeleted(false);
        receipt.setCreatedBy(username);
        receipt.setUpdatedBy(username);
        receipt.setCompanyId(companyId);
        receipt.setBranchId(branchId);
        receipt.setCode(receipt.getReceiptNumber());
        receipt.setName("Customer Receipt Entry");
        receipt.setStatus("DRAFT");

        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<CustomerReceiptAllocation> allocationEntities = new ArrayList<>();

        if (dto.getAllocations() != null && !dto.getAllocations().isEmpty()) {
            java.util.Set<Long> uniqueInvoiceIds = new java.util.HashSet<>();
            for (CustomerReceiptDTO.AllocationDTO alloc : dto.getAllocations()) {
                if (alloc.getInvoiceId() == null) {
                    throw new IllegalArgumentException("Invoice ID must be specified in allocations.");
                }
                if (!uniqueInvoiceIds.add(alloc.getInvoiceId())) {
                    throw new IllegalArgumentException("Duplicate allocation of the same invoice ID: " + alloc.getInvoiceId());
                }

                if (alloc.getAmount() == null || alloc.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Allocation amount must be greater than zero.");
                }

                // In DRAFT mode, we read invoice without locking it
                SalesInvoice invoice = salesInvoiceRepository.findById(alloc.getInvoiceId())
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found or deleted with ID: " + alloc.getInvoiceId()));

                tenantAccess.assertOwned(invoice.getCompanyId());
                if (currentUser.getBranchId() != null && invoice.getBranchId() != null && !currentUser.getBranchId().equals(invoice.getBranchId())) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied: Invoice belongs to another branch.");
                }

                if (!"APPROVED".equals(invoice.getStatus())) {
                    throw new IllegalArgumentException("Only APPROVED invoices can be allocated. Invoice status: " + invoice.getStatus());
                }

                BigDecimal outstanding = invoice.getNetAmount().subtract(invoice.getPaidAmount());
                if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid.");
                }

                if (alloc.getAmount().compareTo(outstanding) > 0) {
                    throw new IllegalArgumentException("Allocation amount ₹" + alloc.getAmount() + " exceeds invoice outstanding balance ₹" + outstanding);
                }

                CustomerReceiptAllocation entity = new CustomerReceiptAllocation();
                entity.setReceipt(receipt);
                entity.setInvoice(invoice);
                entity.setAllocatedAmount(alloc.getAmount());
                entity.setIsDeleted(false);
                entity.setCreatedBy(username);
                entity.setUpdatedBy(username);
                entity.setCompanyId(companyId);
                entity.setBranchId(branchId);
                entity.setCode("ALLOC_" + invoice.getId() + "_" + System.currentTimeMillis());
                entity.setName("Invoice Payment Allocation");

                allocationEntities.add(entity);
                totalAllocated = totalAllocated.add(alloc.getAmount());
            }
        }

        if (totalAllocated.compareTo(receipt.getAmountReceived()) > 0) {
            throw new IllegalArgumentException("Total allocations exceed the receipt amount received.");
        }

        BigDecimal calculatedAdvance = receipt.getAmountReceived().subtract(totalAllocated);
        receipt.setAdvanceAmount(calculatedAdvance);
        receipt.setAllocations(allocationEntities);

        CustomerReceipt savedReceipt = receiptRepository.save(receipt);

        logAudit(savedReceipt, "CREATED", "Registered customer receipt voucher as draft with allocations.", username);

        auditService.log(username, "RECEIPT_CREATED", "customer_receipts", savedReceipt.getId(), null,
                "Registered customer receipt voucher as draft: " + savedReceipt.getReceiptNumber() + " with " + allocationEntities.size() + " allocations");

        CustomerReceiptResponseDTO response = new CustomerReceiptResponseDTO();
        response.setReceiptId(savedReceipt.getId());
        response.setReceiptNumber(savedReceipt.getReceiptNumber());
        response.setCustomerId(savedReceipt.getCustomer().getId());
        response.setCustomerName(savedReceipt.getCustomer().getName());
        response.setReceiptDate(savedReceipt.getReceiptDate());
        response.setPaymentAmount(savedReceipt.getAmountReceived());
        response.setPaymentMethod(savedReceipt.getPaymentMethod());
        response.setAdvanceAmount(savedReceipt.getAdvanceAmount());
        response.setTotalAllocated(totalAllocated);
        response.setStatus(savedReceipt.getStatus());
        response.setReferenceNumber(savedReceipt.getReferenceNumber());
        response.setRemarks(savedReceipt.getRemarks());
        response.setApprovedBy(savedReceipt.getApprovedBy());
        response.setApprovedAt(savedReceipt.getApprovedAt());

        List<CustomerReceiptResponseDTO.AllocationResponseDTO> allocResponseDTOs = savedReceipt.getAllocations().stream().map(a -> {
            CustomerReceiptResponseDTO.AllocationResponseDTO ar = new CustomerReceiptResponseDTO.AllocationResponseDTO();
            ar.setInvoiceId(a.getInvoice().getId());
            ar.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
            ar.setAllocatedAmount(a.getAllocatedAmount());
            ar.setInvoiceTotal(a.getInvoice().getNetAmount());
            ar.setPaidAmount(a.getInvoice().getPaidAmount());
            ar.setOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
            ar.setPaymentStatus(a.getInvoice().getPaymentStatus());
            return ar;
        }).collect(Collectors.toList());

        response.setAllocations(allocResponseDTOs);
        return response;
    }

    @Transactional
    public CustomerReceiptResponseDTO cancelReceipt(Long receiptId, String username) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        
        CustomerReceipt receipt = receiptRepository.findAndLockById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));
                
        tenantAccess.assertOwned(receipt.getCompanyId());
        if (currentUser.getBranchId() != null && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        if ("CANCELLED".equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Receipt is already cancelled.");
        }

        if ("DRAFT".equals(receipt.getStatus())) {
            // Simply transition to CANCELLED without any accounting/invoice reversals
            receipt.setStatus("CANCELLED");
            receipt.setUpdatedBy(username);
            CustomerReceipt savedReceipt = receiptRepository.save(receipt);

            logAudit(savedReceipt, "CANCELLED", "Cancelled customer receipt draft: " + savedReceipt.getReceiptNumber(), username);

            auditService.log(username, "RECEIPT_CANCELLED", "customer_receipts", savedReceipt.getId(), null,
                    "Cancelled customer receipt draft: " + savedReceipt.getReceiptNumber());

            // Map response DTO
            CustomerReceiptResponseDTO response = new CustomerReceiptResponseDTO();
            response.setReceiptId(savedReceipt.getId());
            response.setReceiptNumber(savedReceipt.getReceiptNumber());
            response.setCustomerId(savedReceipt.getCustomer().getId());
            response.setCustomerName(savedReceipt.getCustomer().getName());
            response.setReceiptDate(savedReceipt.getReceiptDate());
            response.setPaymentAmount(savedReceipt.getAmountReceived());
            response.setPaymentMethod(savedReceipt.getPaymentMethod());
            response.setAdvanceAmount(savedReceipt.getAdvanceAmount());
            response.setTotalAllocated(savedReceipt.getAmountReceived().subtract(savedReceipt.getAdvanceAmount()));
            response.setStatus(savedReceipt.getStatus());
            response.setReferenceNumber(savedReceipt.getReferenceNumber());
            response.setRemarks(savedReceipt.getRemarks());
            response.setApprovedBy(savedReceipt.getApprovedBy());
            response.setApprovedAt(savedReceipt.getApprovedAt());

            List<CustomerReceiptResponseDTO.AllocationResponseDTO> allocResponseDTOs = savedReceipt.getAllocations().stream().map(a -> {
                CustomerReceiptResponseDTO.AllocationResponseDTO ar = new CustomerReceiptResponseDTO.AllocationResponseDTO();
                ar.setInvoiceId(a.getInvoice().getId());
                ar.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
                ar.setAllocatedAmount(a.getAllocatedAmount());
                ar.setInvoiceTotal(a.getInvoice().getNetAmount());
                ar.setPaidAmount(a.getInvoice().getPaidAmount());
                ar.setOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
                ar.setPaymentStatus(a.getInvoice().getPaymentStatus());
                return ar;
            }).collect(Collectors.toList());

            response.setAllocations(allocResponseDTOs);
            return response;
        }

        // Process invoice paidAmount & status reversals for APPROVED receipts
        if (receipt.getAllocations() != null && !receipt.getAllocations().isEmpty()) {
            for (CustomerReceiptAllocation allocation : receipt.getAllocations()) {
                SalesInvoice invoice = salesInvoiceRepository.findAndLockById(allocation.getInvoice().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found or deleted: " + allocation.getInvoice().getId()));
                        
                tenantAccess.assertOwned(invoice.getCompanyId());
                if (currentUser.getBranchId() != null && invoice.getBranchId() != null && !currentUser.getBranchId().equals(invoice.getBranchId())) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied: Invoice belongs to another branch.");
                }
                
                if (!invoice.getCompanyId().equals(receipt.getCompanyId())) {
                    throw new IllegalArgumentException("Company mismatch between receipt and invoice.");
                }

                BigDecimal newPaidAmount = invoice.getPaidAmount().subtract(allocation.getAllocatedAmount());
                if (newPaidAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Reversal would result in negative paid amount for invoice " + invoice.getInvoiceNumber());
                }
                
                invoice.setPaidAmount(newPaidAmount);
                if (newPaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    invoice.setPaymentStatus("UNPAID");
                } else if (newPaidAmount.compareTo(invoice.getNetAmount()) < 0) {
                    invoice.setPaymentStatus("PARTIALLY_PAID");
                } else {
                    invoice.setPaymentStatus("PAID");
                }
                
                salesInvoiceRepository.save(invoice);
            }
        }

        // Update receipt status
        receipt.setStatus("CANCELLED");
        receipt.setUpdatedBy(username);
        CustomerReceipt savedReceipt = receiptRepository.save(receipt);

        logAudit(savedReceipt, "CANCELLED", "Cancelled customer receipt voucher: " + savedReceipt.getReceiptNumber(), username);

        // Reverse Customer Ledger (debit equal to original credit)
        ledgerService.postToLedger(
                savedReceipt.getCustomer().getId(),
                savedReceipt,
                savedReceipt.getAmountReceived(),
                BigDecimal.ZERO,
                "Reversal of receipt " + savedReceipt.getReceiptNumber(),
                username
        );

        // Reverse Journal Voucher
        List<JournalVoucher> originalJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(savedReceipt.getReceiptNumber());
        if (!originalJvs.isEmpty()) {
            JournalVoucher originalJv = originalJvs.get(0);
            
            JournalVoucher reversalJv = new JournalVoucher();
            reversalJv.setVoucherNumber("JV-" + System.currentTimeMillis());
            reversalJv.setVoucherDate(LocalDate.now());
            reversalJv.setDebitAccount(originalJv.getCreditAccount());
            reversalJv.setCreditAccount(originalJv.getDebitAccount());
            reversalJv.setAmount(savedReceipt.getAmountReceived());
            reversalJv.setReferenceNumber(savedReceipt.getReceiptNumber());
            reversalJv.setDescription("Auto-posted reversal JV for " + savedReceipt.getReceiptNumber());
            reversalJv.setCompanyId(savedReceipt.getCompanyId());
            reversalJv.setBranchId(savedReceipt.getBranchId());
            reversalJv.setIsDeleted(false);
            reversalJv.setCreatedBy(username);
            reversalJv.setUpdatedBy(username);
            reversalJv.setCode(reversalJv.getVoucherNumber());
            reversalJv.setName("Journal Voucher Reversal Entry");
            
            jvService.createVoucher(reversalJv, username);
        }

        auditService.log(username, "RECEIPT_CANCELLED", "customer_receipts", savedReceipt.getId(), null,
                "Cancelled customer receipt voucher: " + savedReceipt.getReceiptNumber());


        // Map response DTO
        CustomerReceiptResponseDTO response = new CustomerReceiptResponseDTO();
        response.setReceiptId(savedReceipt.getId());
        response.setReceiptNumber(savedReceipt.getReceiptNumber());
        response.setCustomerId(savedReceipt.getCustomer().getId());
        response.setCustomerName(savedReceipt.getCustomer().getName());
        response.setReceiptDate(savedReceipt.getReceiptDate());
        response.setPaymentAmount(savedReceipt.getAmountReceived());
        response.setPaymentMethod(savedReceipt.getPaymentMethod());
        response.setAdvanceAmount(savedReceipt.getAdvanceAmount());
        response.setTotalAllocated(savedReceipt.getAmountReceived().subtract(savedReceipt.getAdvanceAmount()));
        response.setStatus(savedReceipt.getStatus());

        List<CustomerReceiptResponseDTO.AllocationResponseDTO> allocResponseDTOs = savedReceipt.getAllocations().stream().map(a -> {
            CustomerReceiptResponseDTO.AllocationResponseDTO ar = new CustomerReceiptResponseDTO.AllocationResponseDTO();
            ar.setInvoiceId(a.getInvoice().getId());
            ar.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
            ar.setAllocatedAmount(a.getAllocatedAmount());
            ar.setInvoiceTotal(a.getInvoice().getNetAmount());
            ar.setOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
            ar.setPaymentStatus(a.getInvoice().getPaymentStatus());
            return ar;
        }).collect(Collectors.toList());

        response.setAllocations(allocResponseDTOs);
        return response;
    }

    @Transactional
    public CustomerReceiptResponseDTO approveReceipt(Long receiptId, String username) {
        AppUser currentUser = tenantAccess.requireCurrentUser();

        // 1. Acquire pessimistic write lock on the receipt
        CustomerReceipt receipt = receiptRepository.findAndLockById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        // 2. Validate tenant & branch security
        tenantAccess.assertOwned(receipt.getCompanyId());
        if (currentUser.getBranchId() != null && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        // 3. Validate status transitions
        if ("APPROVED".equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Receipt is already approved");
        }
        if ("CANCELLED".equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Cancelled receipt cannot be approved");
        }
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new IllegalArgumentException("Only draft receipts can be approved. Current status: " + receipt.getStatus());
        }

        // 4. Validate amount
        if (receipt.getAmountReceived() == null || receipt.getAmountReceived().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount received must be greater than zero.");
        }

        BigDecimal totalAllocated = BigDecimal.ZERO;

        // 5. Lock Invoices in deterministic ascending ID order to prevent deadlocks
        if (receipt.getAllocations() != null && !receipt.getAllocations().isEmpty()) {
            List<CustomerReceiptAllocation> sortedAllocations = receipt.getAllocations().stream()
                    .sorted(java.util.Comparator.comparing(a -> a.getInvoice().getId()))
                    .collect(Collectors.toList());

            for (CustomerReceiptAllocation allocation : sortedAllocations) {
                // Lock invoice
                SalesInvoice invoice = salesInvoiceRepository.findAndLockById(allocation.getInvoice().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found or deleted: " + allocation.getInvoice().getId()));

                // Validate invoice tenant and branch ownership
                tenantAccess.assertOwned(invoice.getCompanyId());
                if (currentUser.getBranchId() != null && invoice.getBranchId() != null && !currentUser.getBranchId().equals(invoice.getBranchId())) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied: Invoice belongs to another branch.");
                }

                if (!invoice.getCompanyId().equals(receipt.getCompanyId())) {
                    throw new IllegalArgumentException("Company mismatch between receipt and invoice.");
                }

                // Validate Customer match
                if (!invoice.getCustomer().getId().equals(receipt.getCustomer().getId())) {
                    throw new IllegalArgumentException("Customer mismatch: Invoice belongs to another customer.");
                }

                // Validate Invoice Status
                if (!"APPROVED".equals(invoice.getStatus())) {
                    throw new IllegalArgumentException("Only APPROVED invoices can receive payment. Invoice status: " + invoice.getStatus());
                }

                // Recalculate outstanding from DB
                BigDecimal outstanding = invoice.getNetAmount().subtract(invoice.getPaidAmount());
                if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid.");
                }

                // Validate allocation bounds
                BigDecimal allocatedAmount = allocation.getAllocatedAmount();
                if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Allocation amount must be greater than zero.");
                }

                if (allocatedAmount.compareTo(outstanding) > 0) {
                    throw new IllegalArgumentException("Allocation amount ₹" + allocatedAmount + " exceeds invoice outstanding balance ₹" + outstanding);
                }

                // Update invoice paidAmount & status
                BigDecimal newPaidAmount = invoice.getPaidAmount().add(allocatedAmount);
                if (newPaidAmount.compareTo(invoice.getNetAmount()) > 0) {
                    throw new IllegalArgumentException("Paid amount cannot exceed net amount for invoice " + invoice.getInvoiceNumber());
                }

                invoice.setPaidAmount(newPaidAmount);
                if (newPaidAmount.compareTo(BigDecimal.ZERO) == 0) {
                    invoice.setPaymentStatus("UNPAID");
                } else if (newPaidAmount.compareTo(invoice.getNetAmount()) < 0) {
                    invoice.setPaymentStatus("PARTIALLY_PAID");
                } else {
                    invoice.setPaymentStatus("PAID");
                }

                salesInvoiceRepository.save(invoice);
                totalAllocated = totalAllocated.add(allocatedAmount);
            }
        }

        // Validate total allocations do not exceed amount received
        if (totalAllocated.compareTo(receipt.getAmountReceived()) > 0) {
            throw new IllegalArgumentException("Total allocations exceed the receipt amount received.");
        }

        // Compute and set advance
        BigDecimal advance = receipt.getAmountReceived().subtract(totalAllocated);
        receipt.setAdvanceAmount(advance);

        // Update receipt approval audit fields
        receipt.setStatus("APPROVED");
        receipt.setApprovedBy(username);
        receipt.setApprovedAt(java.time.LocalDateTime.now());
        receipt.setUpdatedBy(username);

        CustomerReceipt savedReceipt = receiptRepository.save(receipt);

        // 6. Post to Ledger exactly once
        ledgerService.postToLedger(
                savedReceipt.getCustomer().getId(),
                savedReceipt,
                BigDecimal.ZERO,
                savedReceipt.getAmountReceived(),
                username
        );

        // 7. Post Journal Voucher exactly once (checking for duplicates)
        List<JournalVoucher> existingJvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(savedReceipt.getReceiptNumber());
        if (existingJvs.isEmpty()) {
            String debitAccountCode = "CASH".equalsIgnoreCase(savedReceipt.getPaymentMethod()) ? "1000" : "1010";
            String debitAccountName = "CASH".equalsIgnoreCase(savedReceipt.getPaymentMethod()) ? "Cash on Hand" : "Bank - Current A/c";
            ChartOfAccount debitAcc = coaService.getOrCreateAccount(savedReceipt.getCompanyId(), savedReceipt.getBranchId(), debitAccountCode, debitAccountName, "ASSET");
                    
            ChartOfAccount creditAcc = coaService.getOrCreateAccount(savedReceipt.getCompanyId(), savedReceipt.getBranchId(), "1100", "Customer Receivables", "ASSET");


            JournalVoucher jv = new JournalVoucher();
            jv.setVoucherNumber("JV-" + System.currentTimeMillis());
            jv.setVoucherDate(LocalDate.now());
            jv.setDebitAccount(debitAcc);
            jv.setCreditAccount(creditAcc);
            jv.setAmount(savedReceipt.getAmountReceived());
            jv.setReferenceNumber(savedReceipt.getReceiptNumber());
            jv.setDescription("Auto-posted receipt JV for " + savedReceipt.getReceiptNumber());
            jv.setCompanyId(savedReceipt.getCompanyId());
            jv.setBranchId(savedReceipt.getBranchId());
            jv.setIsDeleted(false);
            jv.setCreatedBy(username);
            jv.setUpdatedBy(username);
            jv.setCode(jv.getVoucherNumber());
            jv.setName("Journal Voucher Entry");
            
            jvService.createVoucher(jv, username);
        }

        auditService.log(username, "RECEIPT_APPROVED", "customer_receipts", savedReceipt.getId(), null,
                "Approved and posted customer receipt voucher: " + savedReceipt.getReceiptNumber());

        logAudit(savedReceipt, "APPROVED", "Approved customer receipt and posted accounting entries.", username);

        // Map response DTO
        CustomerReceiptResponseDTO response = new CustomerReceiptResponseDTO();
        response.setReceiptId(savedReceipt.getId());
        response.setReceiptNumber(savedReceipt.getReceiptNumber());
        response.setCustomerId(savedReceipt.getCustomer().getId());
        response.setCustomerName(savedReceipt.getCustomer().getName());
        response.setReceiptDate(savedReceipt.getReceiptDate());
        response.setPaymentAmount(savedReceipt.getAmountReceived());
        response.setPaymentMethod(savedReceipt.getPaymentMethod());
        response.setAdvanceAmount(savedReceipt.getAdvanceAmount());
        response.setTotalAllocated(totalAllocated);
        response.setStatus(savedReceipt.getStatus());
        response.setReferenceNumber(savedReceipt.getReferenceNumber());
        response.setRemarks(savedReceipt.getRemarks());
        response.setApprovedBy(savedReceipt.getApprovedBy());
        response.setApprovedAt(savedReceipt.getApprovedAt());

        List<CustomerReceiptResponseDTO.AllocationResponseDTO> allocResponseDTOs = savedReceipt.getAllocations().stream().map(a -> {
            CustomerReceiptResponseDTO.AllocationResponseDTO ar = new CustomerReceiptResponseDTO.AllocationResponseDTO();
            ar.setInvoiceId(a.getInvoice().getId());
            ar.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
            ar.setAllocatedAmount(a.getAllocatedAmount());
            ar.setInvoiceTotal(a.getInvoice().getNetAmount());
            ar.setPaidAmount(a.getInvoice().getPaidAmount());
            ar.setOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
            ar.setPaymentStatus(a.getInvoice().getPaymentStatus());
            return ar;
        }).collect(Collectors.toList());

        response.setAllocations(allocResponseDTOs);
        return response;
    }

    private void logAudit(CustomerReceipt receipt, String eventType, String remarks, String username) {
        CustomerReceiptAudit audit = new CustomerReceiptAudit();
        audit.setReceiptId(receipt.getId());
        audit.setEventType(eventType);
        audit.setEventTime(java.time.LocalDateTime.now());
        audit.setPerformedBy(username);
        audit.setRemarks(remarks);
        audit.setCompanyId(receipt.getCompanyId());
        audit.setBranchId(receipt.getBranchId());
        auditTrailRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public Page<CustomerReceiptListDTO> getReceipts(String search, String status, String paymentMethod, Long customerId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(currentUser) ? null : currentUser.getBranchId();

        Page<CustomerReceipt> receipts = receiptRepository.findReceiptsWithFilters(
                companyId, branchId, status, paymentMethod, customerId, fromDate, toDate, search, pageable);

        return receipts.map(r -> {
            CustomerReceiptListDTO dto = new CustomerReceiptListDTO();
            dto.setId(r.getId());
            dto.setReceiptNumber(r.getReceiptNumber());
            dto.setReferenceNumber(r.getReferenceNumber());
            dto.setCustomerId(r.getCustomer().getId());
            dto.setCustomerName(r.getCustomer().getName());
            dto.setReceiptDate(r.getReceiptDate());
            dto.setAmountReceived(r.getAmountReceived());
            dto.setAdvanceAmount(r.getAdvanceAmount());
            dto.setTotalAllocated(r.getAmountReceived().subtract(r.getAdvanceAmount()));
            dto.setPaymentMethod(r.getPaymentMethod());
            dto.setStatus(r.getStatus());
            dto.setApprovedBy(r.getApprovedBy());
            dto.setApprovedAt(r.getApprovedAt());
            dto.setBranchId(r.getBranchId());
            dto.setCompanyId(r.getCompanyId());
            dto.setCreatedDate(r.getCreatedDate());
            dto.setCreatedBy(r.getCreatedBy());
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public CustomerReceiptDetailDTO getReceiptDetails(Long receiptId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        tenantAccess.assertOwned(receipt.getCompanyId());
        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null 
            && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        CustomerReceiptDetailDTO dto = new CustomerReceiptDetailDTO();
        dto.setId(receipt.getId());
        dto.setReceiptNumber(receipt.getReceiptNumber());
        dto.setReferenceNumber(receipt.getReferenceNumber());
        dto.setReceiptDate(receipt.getReceiptDate());
        dto.setPaymentMethod(receipt.getPaymentMethod());
        dto.setAmountReceived(receipt.getAmountReceived());
        dto.setAdvanceAmount(receipt.getAdvanceAmount());
        dto.setRemarks(receipt.getRemarks());
        dto.setStatus(receipt.getStatus());

        dto.setCustomerId(receipt.getCustomer().getId());
        dto.setCustomerName(receipt.getCustomer().getName());

        dto.setCreatedBy(receipt.getCreatedBy());
        dto.setCreatedDate(receipt.getCreatedDate());
        dto.setApprovedBy(receipt.getApprovedBy());
        dto.setApprovedAt(receipt.getApprovedAt());

        dto.setCompanyId(receipt.getCompanyId());
        dto.setBranchId(receipt.getBranchId());

        BigDecimal totalAllocated = receipt.getAmountReceived().subtract(receipt.getAdvanceAmount());
        dto.setTotalAllocated(totalAllocated);
        dto.setAllocationCount(receipt.getAllocations() == null ? 0 : receipt.getAllocations().size());

        BigDecimal totalOutstandingAfter = BigDecimal.ZERO;
        if (receipt.getAllocations() != null) {
            List<CustomerReceiptAllocationDTO> allocs = receipt.getAllocations().stream().map(a -> {
                CustomerReceiptAllocationDTO adto = new CustomerReceiptAllocationDTO();
                adto.setAllocationId(a.getId());
                adto.setReceiptId(receipt.getId());
                adto.setInvoiceId(a.getInvoice().getId());
                adto.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
                adto.setInvoiceDate(a.getInvoice().getInvoiceDate());
                adto.setInvoiceTotal(a.getInvoice().getNetAmount());
                adto.setAllocatedAmount(a.getAllocatedAmount());
                adto.setInvoicePaidAmount(a.getInvoice().getPaidAmount());
                adto.setInvoiceOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
                adto.setPaymentStatus(a.getInvoice().getPaymentStatus());
                return adto;
            }).collect(Collectors.toList());
            dto.setAllocations(allocs);
            
            totalOutstandingAfter = allocs.stream()
                .map(CustomerReceiptAllocationDTO::getInvoiceOutstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        dto.setTotalInvoiceOutstandingAfterAllocation(totalOutstandingAfter);

        // Accounting references mapping
        if ("APPROVED".equals(receipt.getStatus()) || "CANCELLED".equals(receipt.getStatus())) {
            dto.setCustomerLedgerReference("LEDG_" + receipt.getCustomer().getId() + "_" + receipt.getReceiptNumber());
            dto.setJournalVoucherReference("JV_REC_" + receipt.getReceiptNumber());
            if ("CANCELLED".equals(receipt.getStatus())) {
                dto.setReversalJournalVoucherReference("JV_REV_" + receipt.getReceiptNumber());
            }
        }

        // Audit timeline mapping (load chronological events)
        List<CustomerReceiptAudit> auditLogs = auditTrailRepository.findAuditTrail(receiptId, receipt.getCompanyId(), null);
        List<CustomerReceiptAuditDTO> timeline = new ArrayList<>();
        if (auditLogs.isEmpty()) {
            CustomerReceiptAuditDTO c = new CustomerReceiptAuditDTO();
            c.setEventType("CREATED");
            c.setEventTime(receipt.getCreatedDate());
            c.setPerformedBy(receipt.getCreatedBy());
            c.setRemarks("Receipt created as draft.");
            timeline.add(c);

            if (receipt.getApprovedBy() != null) {
                CustomerReceiptAuditDTO a = new CustomerReceiptAuditDTO();
                a.setEventType("APPROVED");
                a.setEventTime(receipt.getApprovedAt() != null ? receipt.getApprovedAt() : receipt.getUpdatedDate());
                a.setPerformedBy(receipt.getApprovedBy());
                a.setRemarks("Receipt approved and financially posted.");
                timeline.add(a);
            }

            if ("CANCELLED".equals(receipt.getStatus())) {
                CustomerReceiptAuditDTO can = new CustomerReceiptAuditDTO();
                can.setEventType("CANCELLED");
                can.setEventTime(receipt.getUpdatedDate());
                can.setPerformedBy(receipt.getUpdatedBy());
                can.setRemarks("Receipt cancelled.");
                timeline.add(can);
            }
        } else {
            for (CustomerReceiptAudit log : auditLogs) {
                CustomerReceiptAuditDTO timelineDto = new CustomerReceiptAuditDTO();
                timelineDto.setEventType(log.getEventType());
                timelineDto.setEventTime(log.getEventTime());
                timelineDto.setPerformedBy(log.getPerformedBy());
                timelineDto.setRemarks(log.getRemarks());
                timeline.add(timelineDto);
            }
        }
        dto.setAuditTimeline(timeline);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<CustomerReceiptAllocationDTO> getReceiptAllocations(Long receiptId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        tenantAccess.assertOwned(receipt.getCompanyId());
        Long branchId = tenantAccess.isSuperAdmin(currentUser) ? null : currentUser.getBranchId();

        List<CustomerReceiptAllocation> allocs = allocationRepository.findByReceiptIdAndCompanyIdAndBranchId(receiptId, receipt.getCompanyId(), branchId);
        return allocs.stream().map(a -> {
            CustomerReceiptAllocationDTO dto = new CustomerReceiptAllocationDTO();
            dto.setAllocationId(a.getId());
            dto.setReceiptId(receiptId);
            dto.setInvoiceId(a.getInvoice().getId());
            dto.setInvoiceNumber(a.getInvoice().getInvoiceNumber());
            dto.setInvoiceDate(a.getInvoice().getInvoiceDate());
            dto.setInvoiceTotal(a.getInvoice().getNetAmount());
            dto.setAllocatedAmount(a.getAllocatedAmount());
            dto.setInvoicePaidAmount(a.getInvoice().getPaidAmount());
            dto.setInvoiceOutstandingAmount(a.getInvoice().getNetAmount().subtract(a.getInvoice().getPaidAmount()));
            dto.setPaymentStatus(a.getInvoice().getPaymentStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerPaymentHistoryResponseDTO getCustomerPaymentHistory(Long customerId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        tenantAccess.assertOwned(customer.getCompanyId());
        Long branchId = tenantAccess.isSuperAdmin(currentUser) ? null : currentUser.getBranchId();

        List<CustomerReceipt> receipts = receiptRepository.findCustomerHistory(customerId, customer.getCompanyId(), branchId);

        BigDecimal totalReceived = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalAdvance = BigDecimal.ZERO;
        BigDecimal totalCancelled = BigDecimal.ZERO;
        long totalApproved = 0;
        long totalCancelledCount = 0;
        long totalDraft = 0;

        List<CustomerPaymentHistoryDTO> historyList = new ArrayList<>();
        for (CustomerReceipt r : receipts) {
            CustomerPaymentHistoryDTO hdto = new CustomerPaymentHistoryDTO();
            hdto.setReceiptId(r.getId());
            hdto.setReceiptNumber(r.getReceiptNumber());
            hdto.setReceiptDate(r.getReceiptDate());
            hdto.setAmountReceived(r.getAmountReceived());
            hdto.setAdvanceAmount(r.getAdvanceAmount());
            
            BigDecimal allocated = r.getAmountReceived().subtract(r.getAdvanceAmount());
            hdto.setAllocatedAmount(allocated);
            hdto.setPaymentMethod(r.getPaymentMethod());
            hdto.setStatus(r.getStatus());
            hdto.setReferenceNumber(r.getReferenceNumber());
            hdto.setRemarks(r.getRemarks());
            hdto.setApprovedBy(r.getApprovedBy());
            hdto.setApprovedAt(r.getApprovedAt());
            hdto.setCustomerId(customerId);
            hdto.setCustomerName(customer.getName());

            historyList.add(hdto);

            if ("APPROVED".equals(r.getStatus())) {
                totalReceived = totalReceived.add(r.getAmountReceived());
                totalAllocated = totalAllocated.add(allocated);
                totalAdvance = totalAdvance.add(r.getAdvanceAmount());
                totalApproved++;
            } else if ("CANCELLED".equals(r.getStatus())) {
                totalCancelled = totalCancelled.add(r.getAmountReceived());
                totalCancelledCount++;
            } else if ("DRAFT".equals(r.getStatus())) {
                totalDraft++;
            }
        }

        BigDecimal outstandingInvoices = salesInvoiceRepository.sumOutstandingByCustomer(customerId, customer.getCompanyId(), branchId);

        CustomerPaymentHistoryResponseDTO response = new CustomerPaymentHistoryResponseDTO();
        response.setCustomerId(customerId);
        response.setCustomerName(customer.getName());
        response.setTotalReceived(totalReceived);
        response.setTotalAllocated(totalAllocated);
        response.setTotalAdvance(totalAdvance);
        response.setTotalCancelled(totalCancelled);
        response.setTotalApprovedReceipts(totalApproved);
        response.setTotalCancelledReceipts(totalCancelledCount);
        response.setTotalDraftReceipts(totalDraft);
        response.setTotalOutstandingInvoices(outstandingInvoices);
        response.setHistory(historyList);

        return response;
    }

    @Transactional(readOnly = true)
    public List<CustomerReceiptAuditDTO> getReceiptAuditTrail(Long receiptId) {
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        tenantAccess.assertOwned(receipt.getCompanyId());
        List<CustomerReceiptAudit> logs = auditTrailRepository.findAuditTrail(receiptId, receipt.getCompanyId(), null);

        List<CustomerReceiptAuditDTO> list = new ArrayList<>();
        if (logs.isEmpty()) {
            CustomerReceiptAuditDTO c = new CustomerReceiptAuditDTO();
            c.setEventType("CREATED");
            c.setEventTime(receipt.getCreatedDate());
            c.setPerformedBy(receipt.getCreatedBy());
            c.setRemarks("Receipt created as draft.");
            list.add(c);

            if (receipt.getApprovedBy() != null) {
                CustomerReceiptAuditDTO a = new CustomerReceiptAuditDTO();
                a.setEventType("APPROVED");
                a.setEventTime(receipt.getApprovedAt() != null ? receipt.getApprovedAt() : receipt.getUpdatedDate());
                a.setPerformedBy(receipt.getApprovedBy());
                a.setRemarks("Receipt approved and financially posted.");
                list.add(a);
            }

            if ("CANCELLED".equals(receipt.getStatus())) {
                CustomerReceiptAuditDTO can = new CustomerReceiptAuditDTO();
                can.setEventType("CANCELLED");
                can.setEventTime(receipt.getUpdatedDate());
                can.setPerformedBy(receipt.getUpdatedBy());
                can.setRemarks("Receipt cancelled.");
                list.add(can);
            }
        } else {
            for (CustomerReceiptAudit log : logs) {
                CustomerReceiptAuditDTO timelineDto = new CustomerReceiptAuditDTO();
                timelineDto.setEventType(log.getEventType());
                timelineDto.setEventTime(log.getEventTime());
                timelineDto.setPerformedBy(log.getPerformedBy());
                timelineDto.setRemarks(log.getRemarks());
                list.add(timelineDto);
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    public CustomerReceiptPrintDTO getReceiptPrintData(Long receiptId) {
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(receipt.getCompanyId());

        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null 
            && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        CustomerReceiptPrintDTO dto = new CustomerReceiptPrintDTO();
        
        // Receipt info
        dto.setReceiptId(receipt.getId());
        dto.setReceiptNumber(receipt.getReceiptNumber());
        dto.setReferenceNumber(receipt.getReferenceNumber());
        dto.setReceiptDate(receipt.getReceiptDate());
        dto.setStatus(receipt.getStatus());
        dto.setPaymentMethod(receipt.getPaymentMethod());
        dto.setAmountReceived(receipt.getAmountReceived());
        dto.setRemarks(receipt.getRemarks());

        // Customer info
        Customer customer = receipt.getCustomer();
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getName());
        dto.setCustomerCode(customer.getCode());
        dto.setCustomerAddress(customer.getAddress());
        dto.setCustomerPhone(customer.getPhone());
        dto.setCustomerEmail(customer.getEmail());
        dto.setCustomerGSTIN(customer.getGstNumber());

        // Company info
        Company company = companyRepository.findById(receipt.getCompanyId()).orElse(null);
        if (company != null) {
            dto.setCompanyId(company.getId());
            dto.setCompanyName(company.getName());
            dto.setCompanyAddress(company.getAddress());
            dto.setCompanyPhone(company.getPhone());
            dto.setCompanyEmail(company.getEmail());
            dto.setCompanyGSTIN(company.getGstNumber());
            dto.setCompanyPAN(company.getPanNumber());
        }

        // Branch info
        Branch branch = branchRepository.findById(receipt.getBranchId()).orElse(null);
        if (branch != null) {
            dto.setBranchId(branch.getId());
            dto.setBranchName(branch.getName());
            dto.setBranchAddress(branch.getAddress());
            dto.setBranchPhone(branch.getPhone());
        }

        // Approval info
        dto.setApprovedBy(receipt.getApprovedBy());
        dto.setApprovedAt(receipt.getApprovedAt());

        // Cancellation info
        if ("CANCELLED".equals(receipt.getStatus())) {
            dto.setIsCancelled(true);
            dto.setCancelledBy(receipt.getUpdatedBy());
            dto.setCancelledAt(receipt.getUpdatedDate());
        } else {
            dto.setIsCancelled(false);
        }

        // Allocations & calculations
        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<CustomerReceiptPrintDTO.AllocationDetailDTO> allocationDTOs = new ArrayList<>();
        
        List<CustomerReceiptAllocation> allocs = allocationRepository.findByReceiptIdAndCompanyIdAndBranchId(
                receiptId, receipt.getCompanyId(), receipt.getBranchId()
        );
        
        for (CustomerReceiptAllocation alloc : allocs) {
            CustomerReceiptPrintDTO.AllocationDetailDTO detail = new CustomerReceiptPrintDTO.AllocationDetailDTO();
            SalesInvoice inv = alloc.getInvoice();
            detail.setInvoiceId(inv.getId());
            detail.setInvoiceNumber(inv.getInvoiceNumber());
            detail.setInvoiceDate(inv.getInvoiceDate());
            detail.setInvoiceTotal(inv.getNetAmount());
            detail.setAllocatedAmount(alloc.getAllocatedAmount());
            
            BigDecimal prevPaid;
            if ("APPROVED".equals(receipt.getStatus())) {
                prevPaid = inv.getPaidAmount().subtract(alloc.getAllocatedAmount());
            } else {
                prevPaid = inv.getPaidAmount();
            }
            detail.setInvoicePaidAmount(prevPaid);
            detail.setPaidAfterReceipt(prevPaid.add(alloc.getAllocatedAmount()));
            detail.setInvoiceOutstanding(inv.getNetAmount().subtract(prevPaid));
            detail.setOutstandingAfterReceipt(inv.getNetAmount().subtract(prevPaid.add(alloc.getAllocatedAmount())));
            detail.setPaymentStatus(inv.getPaymentStatus());
            
            allocationDTOs.add(detail);
            totalAllocated = totalAllocated.add(alloc.getAllocatedAmount());
        }

        dto.setAllocations(allocationDTOs);
        dto.setTotalAllocated(totalAllocated);
        dto.setAllocationCount(allocationDTOs.size());
        
        // Calculate advance
        dto.setTotalAdvance(receipt.getAmountReceived().subtract(totalAllocated));

        // Accounting JV reference
        if ("APPROVED".equals(receipt.getStatus()) || "CANCELLED".equals(receipt.getStatus())) {
            List<JournalVoucher> jvs = jvRepository.findByReferenceNumberAndIsDeletedFalse(receipt.getReceiptNumber());
            if (!jvs.isEmpty()) {
                JournalVoucher targetJv = jvs.stream()
                        .filter(jv -> !jv.getDescription().contains("reversal"))
                        .findFirst()
                        .orElse(jvs.get(0));
                dto.setJournalVoucherReference(targetJv.getVoucherNumber());
                if (targetJv.getDebitAccount() != null) {
                    dto.setDebitAccount(targetJv.getDebitAccount().getName());
                }
                if (targetJv.getCreditAccount() != null) {
                    dto.setCreditAccount(targetJv.getCreditAccount().getName());
                }
            }
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public CustomerReceiptPrintHistoryDTO getReceiptPrintHistory(Long receiptId) {
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(receipt.getCompanyId());
        
        Long companyId = tenantAccess.resolveCompanyId(null);
        Long branchId = tenantAccess.isSuperAdmin(currentUser) ? null : currentUser.getBranchId();

        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null 
            && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        List<CustomerReceiptPrintAudit> audits = printAuditRepository.findByReceiptIdAndCompanyIdAndBranchIdOrderByPrintedAtAsc(receiptId, companyId, branchId);

        CustomerReceiptPrintHistoryDTO history = new CustomerReceiptPrintHistoryDTO();
        history.setReceiptId(receiptId);
        history.setTotalPrints(audits.stream().filter(a -> "PRINT".equals(a.getEventType())).count());
        history.setTotalPdfExports(audits.stream().filter(a -> "PDF_EXPORT".equals(a.getEventType())).count());
        history.setTotalReprints(audits.stream().filter(a -> "REPRINT".equals(a.getEventType())).count());
        
        if (!audits.isEmpty()) {
            history.setLastPrintedAt(audits.get(audits.size() - 1).getPrintedAt());
        }

        List<CustomerReceiptPrintAuditDTO> eventDtos = audits.stream().map(a -> {
            CustomerReceiptPrintAuditDTO dto = new CustomerReceiptPrintAuditDTO();
            dto.setEventType(a.getEventType());
            dto.setPrintedBy(a.getPrintedBy());
            dto.setPrintedAt(a.getPrintedAt());
            dto.setFormat(a.getFormat());
            return dto;
        }).collect(Collectors.toList());

        history.setEvents(eventDtos);
        return history;
    }

    @Transactional
    public void recordReceiptPrintEvent(Long receiptId, String eventType, String format, String username) {
        CustomerReceipt receipt = receiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found: " + receiptId));

        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(receipt.getCompanyId());
        
        if (!tenantAccess.isSuperAdmin(currentUser) && currentUser.getBranchId() != null 
            && receipt.getBranchId() != null && !currentUser.getBranchId().equals(receipt.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Receipt belongs to another branch.");
        }

        // Validate eventType and format
        if (!List.of("PRINT", "PDF_EXPORT", "REPRINT").contains(eventType)) {
            throw new IllegalArgumentException("Invalid print event type: " + eventType);
        }
        if (!List.of("PRINT", "PDF").contains(format)) {
            throw new IllegalArgumentException("Invalid format: " + format);
        }

        CustomerReceiptPrintAudit audit = new CustomerReceiptPrintAudit();
        audit.setReceiptId(receiptId);
        audit.setEventType(eventType);
        audit.setPrintedBy(username);
        audit.setPrintedAt(java.time.LocalDateTime.now());
        audit.setFormat(format);
        audit.setCompanyId(receipt.getCompanyId());
        audit.setBranchId(receipt.getBranchId());
        
        printAuditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public CustomerReceiptSettlementSummaryDTO getSettlementSummary(Long companyId, Long branchId, LocalDate fromDate, LocalDate toDate) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        CustomerReceiptSettlementSummaryDTO summary = new CustomerReceiptSettlementSummaryDTO();

        // 1. Receipt summaries (APPROVED, DRAFT, CANCELLED)
        List<Object[]> receiptStats = receiptRepository.getReceiptSummaryStats(companyId, branchId, fromDate, toDate);
        for (Object[] stat : receiptStats) {
            String status = (String) stat[0];
            long count = ((Number) stat[1]).longValue();
            BigDecimal amount = (BigDecimal) stat[2];
            
            if ("APPROVED".equals(status)) {
                summary.setTotalApprovedReceipts(count);
                summary.setTotalReceived(amount);
            } else if ("DRAFT".equals(status)) {
                summary.setTotalDraft(count);
            } else if ("CANCELLED".equals(status)) {
                summary.setTotalCancelled(amount);
            }
        }

        // 2. Active allocations
        BigDecimal totalAllocated = allocationRepository.sumAllocatedAmount(companyId, branchId, fromDate, toDate);
        summary.setTotalAllocated(totalAllocated);

        // 3. Active advances (Approved amountReceived - totalAllocated)
        BigDecimal advance = summary.getTotalReceived().subtract(totalAllocated);
        if (advance.compareTo(BigDecimal.ZERO) < 0) {
            advance = BigDecimal.ZERO;
        }
        summary.setTotalAdvance(advance);

        // 4. Invoice summaries (APPROVED status counts and outstanding amounts)
        List<Object[]> invoiceStats = salesInvoiceRepository.getInvoiceSummaryStats(companyId, branchId);
        BigDecimal totalInvoiceAmount = BigDecimal.ZERO;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        
        for (Object[] stat : invoiceStats) {
            String paymentStatus = (String) stat[1];
            long count = ((Number) stat[2]).longValue();
            BigDecimal netAmount = (BigDecimal) stat[3];
            BigDecimal paidAmount = (BigDecimal) stat[4];
            
            totalInvoiceAmount = totalInvoiceAmount.add(netAmount);
            totalPaidAmount = totalPaidAmount.add(paidAmount);
            
            if ("PAID".equals(paymentStatus)) {
                summary.setTotalPaidInvoices(count);
            } else if ("PARTIALLY_PAID".equals(paymentStatus)) {
                summary.setTotalPartiallyPaidInvoices(count);
                summary.setTotalOutstandingInvoices(summary.getTotalOutstandingInvoices() + count);
            } else if ("UNPAID".equals(paymentStatus)) {
                summary.setTotalUnpaidInvoices(count);
                summary.setTotalOutstandingInvoices(summary.getTotalOutstandingInvoices() + count);
            }
        }

        BigDecimal outstanding = totalInvoiceAmount.subtract(totalPaidAmount);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }
        summary.setTotalOutstanding(outstanding);

        // 5. Total customers count
        summary.setTotalCustomers(salesInvoiceRepository.countCustomersWithInvoices(companyId, branchId));

        return summary;
    }

    @Transactional(readOnly = true)
    public Page<CustomerOutstandingSummaryDTO> getCustomerOutstandingSummary(Long companyId, Long branchId, String search, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        String searchPattern = null;
        if (search != null && !search.trim().isEmpty()) {
            searchPattern = "%" + search.trim().toLowerCase() + "%";
        }

        Page<CustomerOutstandingSummaryDTO> page = salesInvoiceRepository.getCustomerOutstandingSummaries(companyId, branchId, searchPattern, pageable);
        if (page.isEmpty()) {
            return page;
        }

        List<Long> customerIds = page.getContent().stream()
                .map(CustomerOutstandingSummaryDTO::getCustomerId)
                .collect(Collectors.toList());

        List<Object[]> advanceAndDates = receiptRepository.getCustomerAdvanceAndLastPayment(customerIds, companyId, branchId);
        
        Map<Long, BigDecimal> advancesMap = new HashMap<>();
        Map<Long, LocalDate> datesMap = new HashMap<>();
        for (Object[] row : advanceAndDates) {
            Long custId = (Long) row[0];
            BigDecimal adv = (BigDecimal) row[1];
            LocalDate date = (LocalDate) row[2];
            advancesMap.put(custId, adv);
            datesMap.put(custId, date);
        }

        for (CustomerOutstandingSummaryDTO dto : page.getContent()) {
            dto.setTotalAdvance(advancesMap.getOrDefault(dto.getCustomerId(), BigDecimal.ZERO));
            dto.setLastPaymentDate(datesMap.get(dto.getCustomerId()));
        }

        return page;
    }

    public static LocalDate calculateDueDate(LocalDate invoiceDate, String paymentTerms) {
        if (invoiceDate == null) {
            return null;
        }
        if (paymentTerms == null) {
            return invoiceDate;
        }
        switch (paymentTerms.toUpperCase()) {
            case "NET_15":
                return invoiceDate.plusDays(15);
            case "NET_30":
                return invoiceDate.plusDays(30);
            case "NET_45":
                return invoiceDate.plusDays(45);
            case "NET_60":
                return invoiceDate.plusDays(60);
            case "IMMEDIATE":
            default:
                return invoiceDate;
        }
    }

    public static String getAgingBucketName(long days) {
        if (days <= 0) return "CURRENT";
        if (days <= 30) return "1_30";
        if (days <= 60) return "31_60";
        if (days <= 90) return "61_90";
        if (days <= 180) return "91_180";
        if (days <= 365) return "181_365";
        return "ABOVE_365";
    }

    @Transactional(readOnly = true)
    public Page<CustomerInvoiceAgingDTO> getCustomerInvoiceAging(Long companyId, Long branchId, Long customerId, String bucket, String search, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        String searchPattern = null;
        if (search != null && !search.trim().isEmpty()) {
            searchPattern = "%" + search.trim().toLowerCase() + "%";
        }

        List<SalesInvoice> invoices = salesInvoiceRepository.findOutstandingInvoicesForAging(companyId, branchId, customerId, searchPattern);
        List<CustomerInvoiceAgingDTO> dtos = new ArrayList<>();

        for (SalesInvoice inv : invoices) {
            CustomerInvoiceAgingDTO dto = new CustomerInvoiceAgingDTO();
            dto.setInvoiceId(inv.getId());
            dto.setInvoiceNumber(inv.getInvoiceNumber());
            dto.setInvoiceDate(inv.getInvoiceDate());
            
            LocalDate dueDate = calculateDueDate(inv.getInvoiceDate(), inv.getPaymentTerms());
            dto.setDueDate(dueDate);
            dto.setCustomerId(inv.getCustomer().getId());
            dto.setCustomerName(inv.getCustomer().getName());
            dto.setInvoiceAmount(inv.getNetAmount());
            dto.setPaidAmount(inv.getPaidAmount());
            
            BigDecimal outstanding = inv.getNetAmount().subtract(inv.getPaidAmount());
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }
            dto.setOutstandingAmount(outstanding);
            dto.setPaymentStatus(inv.getPaymentStatus());
            
            long days = 0;
            if (dueDate != null) {
                days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                if (days < 0) days = 0;
            }
            dto.setAgingDays(days);
            
            String bucketName = getAgingBucketName(days);
            dto.setAgingBucket(bucketName);
            
            boolean matchesBucket = true;
            if (bucket != null && !bucket.trim().isEmpty()) {
                String cleanParam = bucket.replace("_", "").toLowerCase();
                String cleanBucket = bucketName.replace("_", "").toLowerCase();
                matchesBucket = cleanParam.equals(cleanBucket);
            }
            
            if (matchesBucket) {
                dtos.add(dto);
            }
        }

        dtos.sort((d1, d2) -> d2.getInvoiceDate().compareTo(d1.getInvoiceDate()));

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<CustomerInvoiceAgingDTO> sublist = new ArrayList<>();
        if (start < dtos.size()) {
            sublist = dtos.subList(start, end);
        }
        
        return new PageImpl<>(sublist, pageable, dtos.size());
    }

    @Transactional(readOnly = true)
    public CustomerAgingSummaryDTO getAgingSummary(Long companyId, Long branchId, Long customerId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        List<SalesInvoice> invoices = salesInvoiceRepository.findOutstandingInvoicesForAging(companyId, branchId, customerId, null);
        CustomerAgingSummaryDTO summary = new CustomerAgingSummaryDTO();

        for (SalesInvoice inv : invoices) {
            BigDecimal outstanding = inv.getNetAmount().subtract(inv.getPaidAmount());
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }
            
            LocalDate dueDate = calculateDueDate(inv.getInvoiceDate(), inv.getPaymentTerms());
            long days = 0;
            if (dueDate != null) {
                days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                if (days < 0) days = 0;
            }
            
            String bucketName = getAgingBucketName(days);
            switch (bucketName) {
                case "CURRENT":
                    summary.setCurrentAmount(summary.getCurrentAmount().add(outstanding));
                    break;
                case "1_30":
                    summary.setDays1To30(summary.getDays1To30().add(outstanding));
                    break;
                case "31_60":
                    summary.setDays31To60(summary.getDays31To60().add(outstanding));
                    break;
                case "61_90":
                    summary.setDays61To90(summary.getDays61To90().add(outstanding));
                    break;
                case "91_180":
                    summary.setDays91To180(summary.getDays91To180().add(outstanding));
                    break;
                case "181_365":
                    summary.setDays181To365(summary.getDays181To365().add(outstanding));
                    break;
                case "ABOVE_365":
                    summary.setAbove365(summary.getAbove365().add(outstanding));
                    break;
            }
            summary.setTotalOutstanding(summary.getTotalOutstanding().add(outstanding));
        }

        return summary;
    }

    @Transactional(readOnly = true)
    public Page<ReceiptSettlementReconciliationDTO> getReceiptSettlementReconciliation(Long companyId, Long branchId, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        Page<CustomerReceipt> receiptsPage = receiptRepository.findReceiptsForReconciliation(companyId, branchId, pageable);
        if (receiptsPage.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        List<Long> receiptIds = receiptsPage.getContent().stream()
                .map(CustomerReceipt::getId)
                .collect(Collectors.toList());

        List<Object[]> allocSums = allocationRepository.sumAllocationsAndCountForReceipts(receiptIds);
        Map<Long, BigDecimal> sumsMap = new HashMap<>();
        Map<Long, Long> countsMap = new HashMap<>();
        for (Object[] row : allocSums) {
            Long rId = (Long) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            Long cnt = (Long) row[2];
            sumsMap.put(rId, sum);
            countsMap.put(rId, cnt);
        }

        List<ReceiptSettlementReconciliationDTO> dtos = new ArrayList<>();
        for (CustomerReceipt r : receiptsPage.getContent()) {
            ReceiptSettlementReconciliationDTO dto = new ReceiptSettlementReconciliationDTO();
            dto.setReceiptId(r.getId());
            dto.setReceiptNumber(r.getReceiptNumber());
            dto.setReceiptDate(r.getReceiptDate());
            dto.setCustomerId(r.getCustomer().getId());
            dto.setCustomerName(r.getCustomer().getName());
            dto.setStatus(r.getStatus());
            dto.setAmountReceived(r.getAmountReceived());
            
            BigDecimal allocated = sumsMap.getOrDefault(r.getId(), BigDecimal.ZERO);
            dto.setTotalAllocated(allocated);
            
            BigDecimal adv = r.getAdvanceAmount() != null ? r.getAdvanceAmount() : BigDecimal.ZERO;
            dto.setAdvanceAmount(adv);

            long allocCount = countsMap.getOrDefault(r.getId(), 0L);
            dto.setAllocationCount(allocCount);

            BigDecimal diff = r.getAmountReceived().subtract(allocated).subtract(adv).setScale(2, java.math.RoundingMode.HALF_UP);
            dto.setReconciliationDifference(diff);
            
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                dto.setReconciliationStatus("BALANCED");
            } else {
                dto.setReconciliationStatus("MISMATCH");
            }

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, receiptsPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<CustomerPaymentPerformanceDTO> getCustomerPaymentPerformance(Long companyId, Long branchId, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        Page<Object[]> performancePage = receiptRepository.getCustomerPaymentStatsPaginated(companyId, branchId, pageable);
        if (performancePage.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        List<Long> customerIds = performancePage.getContent().stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        List<Object[]> cancelledStats = receiptRepository.getCustomerCancelledStats(companyId, branchId);
        Map<Long, BigDecimal> cancelledMap = new HashMap<>();
        for (Object[] row : cancelledStats) {
            cancelledMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<Object[]> outstandingStats = salesInvoiceRepository.getCustomerOutstandingBalances(companyId, branchId);
        Map<Long, BigDecimal> outstandingMap = new HashMap<>();
        for (Object[] row : outstandingStats) {
            outstandingMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<CustomerPaymentPerformanceDTO> dtos = new ArrayList<>();
        for (Object[] row : performancePage.getContent()) {
            Long custId = (Long) row[0];
            String name = (String) row[1];
            long paymentCount = ((Number) row[2]).longValue();
            BigDecimal received = (BigDecimal) row[3];
            BigDecimal advance = (BigDecimal) row[4];
            LocalDate lastPayment = (LocalDate) row[5];

            CustomerPaymentPerformanceDTO dto = new CustomerPaymentPerformanceDTO();
            dto.setCustomerId(custId);
            dto.setCustomerName(name);
            dto.setPaymentCount(paymentCount);
            dto.setTotalReceipts(paymentCount);
            dto.setTotalReceived(received);
            
            BigDecimal allocated = received.subtract(advance);
            if (allocated.compareTo(BigDecimal.ZERO) < 0) {
                allocated = BigDecimal.ZERO;
            }
            dto.setTotalAllocated(allocated);
            dto.setTotalAdvance(advance);
            
            dto.setTotalCancelled(cancelledMap.getOrDefault(custId, BigDecimal.ZERO));
            dto.setLastPaymentDate(lastPayment);
            dto.setOutstandingAmount(outstandingMap.getOrDefault(custId, BigDecimal.ZERO));

            if (paymentCount > 0) {
                dto.setAverageReceiptAmount(received.divide(BigDecimal.valueOf(paymentCount), 2, java.math.RoundingMode.HALF_UP));
            } else {
                dto.setAverageReceiptAmount(BigDecimal.ZERO);
            }

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, performancePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ReceiptSettlementDashboardDTO getSettlementDashboard(Long companyId, Long branchId) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        tenantAccess.assertOwned(companyId);
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            if (currentUser.getBranchId() != null) {
                branchId = currentUser.getBranchId();
            }
        }

        ReceiptSettlementDashboardDTO dashboard = new ReceiptSettlementDashboardDTO();

        // 1. Summary
        dashboard.setSummary(getSettlementSummary(companyId, branchId, null, null));

        // 2. Aging Summary
        dashboard.setAgingSummary(getAgingSummary(companyId, branchId, null));

        // 3. Top Outstanding Customers (page size 5)
        Pageable top5 = org.springframework.data.domain.PageRequest.of(0, 5);
        dashboard.setTopOutstandingCustomers(getCustomerOutstandingSummary(companyId, branchId, null, top5).getContent());

        // 4. Aging Invoices (first 5 invoices)
        dashboard.setAgingInvoices(getCustomerInvoiceAging(companyId, branchId, null, null, null, top5).getContent());

        // 5. Recent Approved Receipts
        List<CustomerReceipt> recentReceipts = receiptRepository.findRecentApprovedReceipts(companyId, branchId, top5);
        List<CustomerReceiptResponseDTO> recentDTOs = recentReceipts.stream().map(r -> {
            CustomerReceiptResponseDTO dto = new CustomerReceiptResponseDTO();
            dto.setReceiptId(r.getId());
            dto.setReceiptNumber(r.getReceiptNumber());
            dto.setCustomerId(r.getCustomer().getId());
            dto.setCustomerName(r.getCustomer().getName());
            dto.setReceiptDate(r.getReceiptDate());
            dto.setPaymentAmount(r.getAmountReceived());
            dto.setPaymentMethod(r.getPaymentMethod());
            dto.setAdvanceAmount(r.getAdvanceAmount());
            dto.setStatus(r.getStatus());
            dto.setReferenceNumber(r.getReferenceNumber());
            dto.setRemarks(r.getRemarks());
            
            BigDecimal allocated = BigDecimal.ZERO;
            if (r.getAllocations() != null) {
                for (CustomerReceiptAllocation a : r.getAllocations()) {
                    allocated = allocated.add(a.getAllocatedAmount());
                }
            }
            dto.setTotalAllocated(allocated);
            dto.setApprovedBy(r.getApprovedBy());
            dto.setApprovedAt(r.getApprovedAt());
            return dto;
        }).collect(Collectors.toList());
        dashboard.setRecentApprovedReceipts(recentDTOs);

        // 6. Reconciliation Summary (first 5)
        dashboard.setReconciliationSummary(getReceiptSettlementReconciliation(companyId, branchId, top5).getContent());

        // 7. Payment Performance (first 5)
        dashboard.setPaymentPerformance(getCustomerPaymentPerformance(companyId, branchId, top5).getContent());

        return dashboard;
    }
}
