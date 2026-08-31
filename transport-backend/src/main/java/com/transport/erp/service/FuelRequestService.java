package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.FuelRequest;
import com.transport.erp.repository.FuelRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuelRequestService {

    @Autowired
    private FuelRequestRepository requestRepository;

    @Autowired
    private TenantAccessService tenantAccess;


    @Autowired
    private AuditService auditService;




    public Page<FuelRequest> getRequests(Long companyId, String status, Pageable pageable) {
        if (status != null && !status.trim().isEmpty()) {
            return requestRepository.findByCompanyIdAndIsDeletedFalseAndStatus(companyId, status, pageable);
        }
        return requestRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public FuelRequest getRequestById(Long id) {
        FuelRequest req = requestRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Fuel Request not found: " + id));
        tenantAccess.assertOwned(req.getCompanyId());
        return req;
    }

    @Transactional
    public FuelRequest createRequest(FuelRequest req, String username) {
        req.setRequestNumber("FREQ-" + System.currentTimeMillis());
        req.setStatus("PENDING");
        req.setIsDeleted(false);
        req.setRequestedBy(username);
        req.setCreatedBy(username);
        req.setUpdatedBy(username);

        req.setCompanyId(tenantAccess.resolveCompanyId(req.getCompanyId()));
        req.setBranchId(tenantAccess.resolveBranchId(req.getBranchId()));


        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_CREATED", "fuel_requests", saved.getId(), null,
                "Created fuel request: " + saved.getRequestNumber());

        return saved;
    }

    @Transactional
    public FuelRequest approveRequest(Long id, String username) {
        FuelRequest req = getRequestById(id);
        req.setStatus("APPROVED");
        req.setApprovedBy(username);
        req.setUpdatedBy(username);

        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_APPROVED", "fuel_requests", saved.getId(), null,
                "Approved fuel request: " + saved.getRequestNumber());

        return saved;
    }

    @Transactional
    public FuelRequest rejectRequest(Long id, String username) {
        FuelRequest req = getRequestById(id);
        req.setStatus("REJECTED");
        req.setApprovedBy(username);
        req.setUpdatedBy(username);

        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_REJECTED", "fuel_requests", saved.getId(), null,
                "Rejected fuel request: " + saved.getRequestNumber());

        return saved;
    }
}
