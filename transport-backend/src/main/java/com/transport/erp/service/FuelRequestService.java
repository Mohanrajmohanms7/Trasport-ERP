package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.AppUser;
import com.transport.erp.model.FuelRequest;
import com.transport.erp.model.Trip;
import com.transport.erp.repository.FuelRequestRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class FuelRequestService {

    @Autowired
    private FuelRequestRepository requestRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private AuditService auditService;

    public Page<FuelRequest> getRequests(Long companyId, String status, Pageable pageable) {
        Long resolvedCompanyId = tenantAccess.resolveCompanyId(companyId);
        if (status != null && !status.trim().isEmpty()) {
            return requestRepository.findByCompanyIdAndIsDeletedFalseAndStatus(resolvedCompanyId, status, pageable);
        }
        return requestRepository.findByCompanyIdAndIsDeletedFalse(resolvedCompanyId, pageable);
    }

    public FuelRequest getRequestById(Long id) {
        FuelRequest req = requestRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Request Not Found",
                        "FUEL_REQUEST_NOT_FOUND",
                        "Fuel Request not found with ID: " + id,
                        "Verify the fuel request ID."
                ));
        tenantAccess.assertOwned(req.getCompanyId());
        return req;
    }

    @Transactional
    public FuelRequest createRequest(FuelRequest req, String username) {
        if (req.getTrip() == null || req.getTrip().getId() == null) {
            throw new BusinessValidationException(
                    "Missing Trip",
                    "INVALID_TRIP",
                    "Trip reference is required for fuel request creation.",
                    "Select a valid active trip."
            );
        }

        Long tripId = req.getTrip().getId();
        Trip trip = tripRepository.findById(tripId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new BusinessValidationException(
                        "Trip Not Found",
                        "TRIP_NOT_FOUND",
                        "Trip not found with ID: " + tripId,
                        "Verify the trip ID."
                ));

        tenantAccess.assertOwned(trip.getCompanyId());

        if (req.getRequestedQuantity() == null || req.getRequestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Requested Quantity",
                    "INVALID_FUEL_QUANTITY",
                    "Requested fuel quantity must be greater than 0.",
                    "Enter a positive fuel quantity."
            );
        }

        if (req.getRequestedAmount() == null || req.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Requested Amount",
                    "INVALID_FUEL_AMOUNT",
                    "Requested fuel amount must be greater than 0.",
                    "Enter a positive fuel amount."
            );
        }

        req.setTrip(trip);
        req.setRequestNumber("FREQ-" + System.currentTimeMillis());
        req.setStatus("PENDING");
        req.setIsDeleted(false);
        req.setFulfilledQuantity(BigDecimal.ZERO);
        req.setFulfilledAmount(BigDecimal.ZERO);
        req.setRequestedBy(username);
        req.setCreatedBy(username);
        req.setUpdatedBy(username);
        req.setCompanyId(trip.getCompanyId());
        Long reqBranchId = trip.getBranchId();
        if (reqBranchId == null) {
            AppUser currentUser = tenantAccess.requireCurrentUser();
            reqBranchId = currentUser.getBranchId();
        }
        if (reqBranchId == null) {
            reqBranchId = 1L;
        }
        req.setBranchId(reqBranchId);
        req.setCode("REQ_" + trip.getId() + "_" + System.currentTimeMillis() % 10000);
        req.setName("Fuel Request - Trip #" + trip.getTripNumber());

        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_CREATED", "fuel_requests", saved.getId(), null,
                "Created fuel request: " + saved.getRequestNumber());

        return saved;
    }

    @Transactional
    public FuelRequest approveRequest(Long id, String username) {
        FuelRequest req = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Request Not Found",
                        "FUEL_REQUEST_NOT_FOUND",
                        "Fuel Request not found with ID: " + id,
                        "Verify the fuel request ID."
                ));

        tenantAccess.assertOwned(req.getCompanyId());

        if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' is already APPROVED.", req.getRequestNumber()));
            throw new BusinessValidationException(
                    "Fuel Request Already Approved",
                    "FUEL_REQUEST_ALREADY_APPROVED",
                    String.format("Fuel Request '%s' is already APPROVED.", req.getRequestNumber()),
                    "Proceed to fuel entry fulfillment.",
                    details
            );
        }

        if ("FULFILLED".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' is already FULFILLED.", req.getRequestNumber()));
            throw new BusinessValidationException(
                    "Fuel Request Already Fulfilled",
                    "FUEL_REQUEST_ALREADY_FULFILLED",
                    String.format("Fuel Request '%s' is already FULFILLED.", req.getRequestNumber()),
                    "No further approval permitted.",
                    details
            );
        }

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' status is %s.", req.getRequestNumber(), req.getStatus()));
            throw new BusinessValidationException(
                    "Approval Blocked",
                    "FUEL_REQUEST_STATUS_UPDATE_BLOCKED",
                    String.format("Fuel Request '%s' status %s cannot be approved.", req.getRequestNumber(), req.getStatus()),
                    "Only PENDING fuel requests can be approved.",
                    details
            );
        }

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
        FuelRequest req = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Request Not Found",
                        "FUEL_REQUEST_NOT_FOUND",
                        "Fuel Request not found with ID: " + id,
                        "Verify the fuel request ID."
                ));

        tenantAccess.assertOwned(req.getCompanyId());

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' status is %s.", req.getRequestNumber(), req.getStatus()));
            throw new BusinessValidationException(
                    "Rejection Blocked",
                    "FUEL_REQUEST_STATUS_UPDATE_BLOCKED",
                    String.format("Fuel Request '%s' status %s cannot be rejected.", req.getRequestNumber(), req.getStatus()),
                    "Only PENDING fuel requests can be rejected.",
                    details
            );
        }

        req.setStatus("REJECTED");
        req.setApprovedBy(username);
        req.setUpdatedBy(username);

        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_REJECTED", "fuel_requests", saved.getId(), null,
                "Rejected fuel request: " + saved.getRequestNumber());

        return saved;
    }

    @Transactional
    public FuelRequest cancelRequest(Long id, String username) {
        FuelRequest req = requestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessValidationException(
                        "Fuel Request Not Found",
                        "FUEL_REQUEST_NOT_FOUND",
                        "Fuel Request not found with ID: " + id,
                        "Verify the fuel request ID."
                ));

        tenantAccess.assertOwned(req.getCompanyId());

        if ("FULFILLED".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' is FULFILLED by Fuel Entry.", req.getRequestNumber()));
            throw new BusinessValidationException(
                    "Cancellation Blocked",
                    "FUEL_REQUEST_HAS_FULFILLMENT",
                    String.format("Fuel Request '%s' is already FULFILLED. Cancel the linked Fuel Entry instead.", req.getRequestNumber()),
                    "Cancel the fuel entry to reverse fulfillment and accounting.",
                    details
            );
        }

        if ("CANCELLED".equalsIgnoreCase(req.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Fuel Request '%s' is already CANCELLED.", req.getRequestNumber()));
            throw new BusinessValidationException(
                    "Request Already Cancelled",
                    "FUEL_REQUEST_ALREADY_CANCELLED",
                    String.format("Fuel Request '%s' is already CANCELLED.", req.getRequestNumber()),
                    "No further action permitted.",
                    details
            );
        }

        req.setStatus("CANCELLED");
        req.setUpdatedBy(username);

        FuelRequest saved = requestRepository.save(req);

        auditService.log(username, "FUEL_REQUEST_CANCELLED", "fuel_requests", saved.getId(), null,
                "Cancelled fuel request: " + saved.getRequestNumber());

        return saved;
    }
}
