package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.Trip;
import com.transport.erp.model.TripDetail;
import com.transport.erp.model.AppSetting;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.AppUser;

import com.transport.erp.model.Booking;
import com.transport.erp.repository.BookingRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class TripService {


    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TenantAccessService tenantAccess;



    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




    public Page<Trip> getTrips(Long companyId, String status, Pageable pageable) {
        Page<Trip> trips;
        if (status != null && !status.trim().isEmpty()) {
            trips = tripRepository.findByCompanyIdAndIsDeletedFalseAndStatus(companyId, status, pageable);
        } else {
            trips = tripRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
        }
        trips.forEach(this::populateBillingStatus);
        return trips;
    }

    public Trip getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        tenantAccess.assertOwned(trip.getCompanyId());
        populateBillingStatus(trip);
        return trip;
    }

    public Page<Trip> getTripsReadyForBilling(Long companyId, Pageable pageable) {
        AppUser currentUser = tenantAccess.requireCurrentUser();
        Long targetBranchId = null;
        if (!tenantAccess.isSuperAdmin(currentUser)) {
            targetBranchId = currentUser.getBranchId();
        }
        Page<Trip> trips = tripRepository.findCompletedTripsReadyForBilling(companyId, targetBranchId, pageable);
        trips.forEach(this::populateBillingStatus);
        return trips;
    }


    public void populateBillingStatus(Trip trip) {
        if (trip == null) return;
        List<SalesInvoice> invoices = salesInvoiceRepository.findInvoicesByTripId(trip.getId());
        if (invoices != null && !invoices.isEmpty()) {
            SalesInvoice inv = invoices.get(0);
            if ("DRAFT".equals(inv.getStatus())) {
                trip.setBillingStatus("INVOICE_DRAFT");
            } else {
                trip.setBillingStatus("INVOICED");
            }
            trip.setAssociatedInvoiceNumber(inv.getInvoiceNumber());
            trip.setAssociatedInvoiceId(inv.getId());
        } else {
            if ("COMPLETED".equals(trip.getStatus())) {
                trip.setBillingStatus("READY_FOR_BILLING");
            } else {
                trip.setBillingStatus("NOT_READY");
            }
        }
    }


    @Transactional
    public Trip createTrip(Trip trip, String createdByUsername) {
        String prefix = settingService.getByKey("PREFIX_TRIP").map(s -> s.getValueData()).orElse("TRIP-");
        String defaultStatus = settingService.getByKey("DEFAULT_TRIP_STATUS").map(s -> s.getValueData()).orElse("PLANNED");
        
        trip.setTripNumber(prefix + System.currentTimeMillis());
        trip.setTripDate(LocalDate.now());
        trip.setStatus(defaultStatus);
        trip.setIsDeleted(false);
        trip.setCreatedBy(createdByUsername);
        trip.setUpdatedBy(createdByUsername);

        if (trip.getBooking() != null && trip.getBooking().getId() != null) {
            Booking booking = bookingRepository.findById(trip.getBooking().getId())
                    .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + trip.getBooking().getId()));
            tenantAccess.assertOwned(booking.getCompanyId());
            tenantAccess.assertBranchAccess(booking.getBranchId());
            trip.setBooking(booking);
            trip.setCompanyId(booking.getCompanyId());
            trip.setBranchId(booking.getBranchId());
        } else {
            trip.setCompanyId(tenantAccess.resolveCompanyId(trip.getCompanyId()));
            trip.setBranchId(tenantAccess.resolveBranchId(trip.getBranchId()));
        }


        if (trip.getDetails() != null) {
            for (TripDetail detail : trip.getDetails()) {
                detail.setTrip(trip);
                detail.setIsDeleted(false);
                detail.setCreatedBy(createdByUsername);
                detail.setUpdatedBy(createdByUsername);
                detail.setCompanyId(trip.getCompanyId());
                detail.setBranchId(trip.getBranchId());
            }
        }

        Trip saved = tripRepository.save(trip);

        auditService.log(createdByUsername, "TRIP_PLANNED", "trips", saved.getId(), null,
                "Planned dispatch trip number: " + saved.getTripNumber());

        return saved;
    }

    @Transactional
    public Trip updateTrip(Long id, Trip details, String updatedByUsername) {
        Trip existing = getTripById(id);

        existing.setVehicle(details.getVehicle());
        existing.setDriver(details.getDriver());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(updatedByUsername);

        // Replace details
        existing.getDetails().clear();
        if (details.getDetails() != null) {
            for (TripDetail d : details.getDetails()) {
                d.setTrip(existing);
                d.setIsDeleted(false);
                d.setCreatedBy(updatedByUsername);
                d.setUpdatedBy(updatedByUsername);
                d.setCompanyId(existing.getCompanyId());
                d.setBranchId(existing.getBranchId());
                existing.getDetails().add(d);
            }
        }

        Trip saved = tripRepository.save(existing);

        auditService.log(updatedByUsername, "TRIP_UPDATED", "trips", saved.getId(), null,
                "Updated allocations for trip: " + saved.getTripNumber());

        return saved;
    }

    @Transactional
    public Trip dispatchTrip(Long id, String dispatchedByUsername) {
        Trip trip = getTripById(id);
        trip.setStatus("DISPATCHED");
        trip.setUpdatedBy(dispatchedByUsername);

        if (trip.getDetails() != null) {
            for (TripDetail detail : trip.getDetails()) {
                detail.setDispatchTime(LocalDateTime.now());
            }
        }

        Trip saved = tripRepository.save(trip);

        auditService.log(dispatchedByUsername, "TRIP_DISPATCHED", "trips", saved.getId(), null,
                "Dispatched vehicle transit for trip: " + saved.getTripNumber());

        return saved;
    }

    @Transactional
    public Trip completeTrip(Long id, String completedByUsername) {
        Trip trip = getTripById(id);
        trip.setStatus("COMPLETED");
        trip.setUpdatedBy(completedByUsername);

        if (trip.getDetails() != null) {
            for (TripDetail detail : trip.getDetails()) {
                detail.setArrivalTime(LocalDateTime.now());
            }
        }

        Trip saved = tripRepository.save(trip);

        auditService.log(completedByUsername, "TRIP_COMPLETED", "trips", saved.getId(), null,
                "Completed customer delivery for trip: " + saved.getTripNumber());

        return saved;
    }

    @Transactional
    public void deleteTrip(Long id, String deletedByUsername) {
        Trip trip = getTripById(id);
        trip.setIsDeleted(true);
        trip.setUpdatedBy(deletedByUsername);
        tripRepository.save(trip);

        auditService.log(deletedByUsername, "TRIP_CANCELLED", "trips", trip.getId(), null,
                "Cancelled trip itinerary: " + trip.getTripNumber());
    }
}
