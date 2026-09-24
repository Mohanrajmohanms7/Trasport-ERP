package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.Trip;
import com.transport.erp.model.TripDetail;
import com.transport.erp.model.AppSetting;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.AppUser;

import com.transport.erp.model.Booking;
import com.transport.erp.model.BookingDetail;
import com.transport.erp.model.Quarry;
import com.transport.erp.model.LoadingLocation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import com.transport.erp.repository.BookingRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.VehicleDriverAssignment;
import com.transport.erp.repository.VehicleDriverAssignmentRepository;
import com.transport.erp.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class TripService {


    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleDriverAssignmentRepository assignmentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleMaintenanceStateService maintenanceState;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private BusinessDependencyValidationService validationService;

    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private com.transport.erp.repository.QuarryRepository quarryRepository;

    @Autowired
    private com.transport.erp.repository.LoadingLocationRepository loadingLocationRepository;




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

        if (trip.getBooking() == null || trip.getBooking().getId() == null) {
            throw new IllegalArgumentException("A trip must be planned against a booking.");
        }
        // Lock the booking so two trips cannot both use its remaining quantity at the same time.
        Booking booking = bookingRepository.findAndLockById(trip.getBooking().getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + trip.getBooking().getId()));
        tenantAccess.assertOwned(booking.getCompanyId());
        tenantAccess.assertBranchAccess(booking.getBranchId());
        if (!"APPROVED".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessValidationException(
                    "Booking Not Approved",
                    "TRIP_BOOKING_NOT_APPROVED",
                    "Booking " + booking.getBookingNumber() + " is " + booking.getStatus() + ". Trips can only be planned for approved bookings.",
                    "Approve the booking first, then plan the trip.");
        }
        trip.setBooking(booking);
        trip.setCompanyId(booking.getCompanyId());
        trip.setBranchId(booking.getBranchId());

        trip.setTripDate(resolveTripDate(trip.getTripDate(), booking));
        trip.setTripNumber(documentNumberService.next(trip.getCompanyId(), DocumentNumberService.TRIP, prefix, trip.getTripDate()));
        trip.setStatus(defaultStatus);
        trip.setIsDeleted(false);
        trip.setCreatedBy(createdByUsername);
        trip.setUpdatedBy(createdByUsername);
        if (trip.getCode() == null) trip.setCode(trip.getTripNumber());
        if (trip.getName() == null) trip.setName("Trip " + trip.getTripNumber());

        resolveLoadingSource(trip, trip.getQuarry(), trip.getLoadingLocation());
        validateVehicleDriverAssignment(trip);
        assertVehicleAvailableForNewTrip(trip, null);

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
        applyBookingLines(trip, booking, null);

        Trip saved = tripRepository.save(trip);

        auditService.log(createdByUsername, "TRIP_PLANNED", "trips", saved.getId(), null,
                "Planned dispatch trip number: " + saved.getTripNumber());

        return saved;
    }

    @Transactional
    public Trip updateTrip(Long id, Trip details, String updatedByUsername) {
        Trip existing = tripRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        tenantAccess.assertOwned(existing.getCompanyId());
        if (existing.getBranchId() != null) {
            tenantAccess.assertBranchAccess(existing.getBranchId());
        }

        String status = existing.getStatus() != null ? existing.getStatus().toUpperCase() : "PLANNED";
        if ("CANCELLED".equals(status)) {
            throw new BusinessValidationException("Trip Cancelled", "TRIP_UPDATE_CANCELLED",
                    "Trip " + existing.getTripNumber() + " is cancelled and cannot be edited.",
                    "Plan a new trip instead.");
        }
        if (salesInvoiceRepository.countByTripIdAndIsDeletedFalse(existing.getId()) > 0) {
            throw new BusinessValidationException("Trip Already Invoiced", "TRIP_UPDATE_INVOICED",
                    "Trip " + existing.getTripNumber() + " is on an invoice, so its quantities and rates are locked.",
                    "Cancel the invoice first if the trip really needs correcting.");
        }
        boolean completed = "COMPLETED".equals(status);

        Booking booking = bookingRepository.findAndLockById(existing.getBooking().getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found for trip " + existing.getTripNumber()));

        Long previousVehicleId = existing.getVehicle() == null ? null : existing.getVehicle().getId();
        if (!completed) {
            // Vehicle/driver cannot change after the truck has delivered.
            existing.setVehicle(details.getVehicle());
            existing.setDriver(details.getDriver());
            validateVehicleDriverAssignment(existing);
            assertVehicleAvailableForNewTrip(existing, previousVehicleId);
        }
        if (details.getTripDate() != null) {
            existing.setTripDate(resolveTripDate(details.getTripDate(), booking));
        }
        resolveLoadingSource(existing, details.getQuarry(), details.getLoadingLocation());
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
        applyBookingLines(existing, booking, existing.getId());

        Trip saved = tripRepository.save(existing);

        auditService.log(updatedByUsername, "TRIP_UPDATED", "trips", saved.getId(), null,
                "Updated allocations for trip: " + saved.getTripNumber());

        return saved;
    }

    /** Defaults to today; no future dates and not before the booking date. */
    private LocalDate resolveTripDate(LocalDate requested, Booking booking) {
        LocalDate date = requested != null ? requested : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("Trip Date In Future", "TRIP_DATE_IN_FUTURE",
                    "Trip date " + date + " is in the future.", "Use today's date or an earlier date.");
        }
        if (booking != null && booking.getBookingDate() != null && date.isBefore(booking.getBookingDate())) {
            throw new BusinessValidationException("Trip Before Booking", "TRIP_DATE_BEFORE_BOOKING",
                    "Trip date " + date + " is before booking date " + booking.getBookingDate() + ".",
                    "Use a trip date on or after the booking date.");
        }
        return date;
    }

    /** Quarry and loading point are optional but must belong to the same company. */
    private void resolveLoadingSource(Trip trip, Quarry quarryRef, LoadingLocation locationRef) {
        Quarry quarry = null;
        if (quarryRef != null && quarryRef.getId() != null) {
            quarry = quarryRepository.findById(quarryRef.getId())
                    .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("Quarry not found: " + quarryRef.getId()));
            tenantAccess.assertOwned(quarry.getCompanyId());
        }
        LoadingLocation location = null;
        if (locationRef != null && locationRef.getId() != null) {
            location = loadingLocationRepository.findById(locationRef.getId())
                    .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()))
                    .orElseThrow(() -> new IllegalArgumentException("Loading location not found: " + locationRef.getId()));
            tenantAccess.assertOwned(location.getCompanyId());
        }
        trip.setQuarry(quarry);
        trip.setLoadingLocation(location);
    }

    /**
     * Each trip line must be a material on the booking. Missing rates are copied from the booking,
     * weighbridge values are checked, and the total moved per material may not exceed the booked
     * quantity plus the BOOKING_QTY_TOLERANCE_PERCENT setting (default 0).
     */
    private void applyBookingLines(Trip trip, Booking booking, Long excludeTripId) {
        if (trip.getDetails() == null || trip.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Add at least one material line to the trip.");
        }

        Map<Long, BigDecimal> bookedQty = new HashMap<>();
        Map<Long, BookingDetail> bookingLines = new HashMap<>();
        for (BookingDetail bd : booking.getDetails()) {
            if (bd.getMaterial() == null || Boolean.TRUE.equals(bd.getIsDeleted())) continue;
            bookedQty.merge(bd.getMaterial().getId(), nz(bd.getQuantity()), BigDecimal::add);
            bookingLines.putIfAbsent(bd.getMaterial().getId(), bd);
        }

        Map<Long, BigDecimal> thisTripQty = new HashMap<>();
        for (TripDetail d : trip.getDetails()) {
            if (d.getMaterial() == null || d.getMaterial().getId() == null) {
                throw new IllegalArgumentException("Select a material on every trip line.");
            }
            BookingDetail bd = bookingLines.get(d.getMaterial().getId());
            String materialName = bd != null && bd.getMaterial().getName() != null ? bd.getMaterial().getName() : "ID " + d.getMaterial().getId();
            if (bd == null) {
                throw new BusinessValidationException("Material Not On Booking", "TRIP_MATERIAL_NOT_ON_BOOKING",
                        "Material " + materialName + " is not on booking " + booking.getBookingNumber() + ".",
                        "Pick a material from the booking, or add it to the booking first.");
            }
            if (d.getQuantity() == null || d.getQuantity().signum() <= 0) {
                throw new BusinessValidationException("Invalid Quantity", "TRIP_QUANTITY_INVALID",
                        "Payload quantity for " + materialName + " must be greater than zero.", "Enter the planned payload.");
            }
            if ((d.getLoadedQuantity() != null && d.getLoadedQuantity().signum() < 0)
                    || (d.getDeliveredQuantity() != null && d.getDeliveredQuantity().signum() < 0)) {
                throw new BusinessValidationException("Invalid Weighbridge Quantity", "TRIP_WEIGHT_NEGATIVE",
                        "Loaded and delivered quantity cannot be negative.", "Correct the weighbridge values.");
            }
            if (d.getLoadedQuantity() != null && d.getDeliveredQuantity() != null
                    && d.getDeliveredQuantity().compareTo(d.getLoadedQuantity()) > 0) {
                throw new BusinessValidationException("Delivered More Than Loaded", "TRIP_DELIVERED_EXCEEDS_LOADED",
                        "Delivered quantity for " + materialName + " is more than the loaded quantity.",
                        "Check the weighbridge slips; delivered cannot exceed loaded.");
            }
            d.setMaterial(bd.getMaterial());
            if (d.getRate() == null || d.getRate().signum() <= 0) d.setRate(nz(bd.getRate()));
            if (d.getRoyalty() == null || d.getRoyalty().signum() <= 0) d.setRoyalty(nz(bd.getRoyaltyRate()));
            if (d.getLoadingCharges() == null || d.getLoadingCharges().signum() <= 0) d.setLoadingCharges(nz(bd.getLoadingCharge()));
            if (d.getCode() == null) d.setCode("TRIP-LINE");
            if (d.getName() == null) d.setName("Trip Line");
            thisTripQty.merge(bd.getMaterial().getId(), d.getBillableQuantity(), BigDecimal::add);
        }

        BigDecimal tolerancePct = settingService.getByKey("BOOKING_QTY_TOLERANCE_PERCENT", booking.getCompanyId())
                .map(s -> {
                    try { return new BigDecimal(s.getValueData().trim()); } catch (Exception e) { return BigDecimal.ZERO; }
                }).orElse(BigDecimal.ZERO);

        Map<Long, BigDecimal> alreadyMoved = new HashMap<>();
        for (Object[] row : tripRepository.sumQuantityByMaterialForBooking(booking.getId(), excludeTripId != null ? excludeTripId : -1L)) {
            alreadyMoved.put((Long) row[0], (BigDecimal) row[1]);
        }

        for (Map.Entry<Long, BigDecimal> e : thisTripQty.entrySet()) {
            BigDecimal booked = bookedQty.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal allowed = booked.add(booked.multiply(tolerancePct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            BigDecimal moved = alreadyMoved.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal total = moved.add(e.getValue());
            if (total.compareTo(allowed) > 0) {
                String name = bookingLines.get(e.getKey()).getMaterial().getName();
                BigDecimal remaining = allowed.subtract(moved).max(BigDecimal.ZERO);
                throw new BusinessValidationException("Booking Quantity Exceeded", "TRIP_EXCEEDS_BOOKING_QTY",
                        String.format("%s: booked %s, already moved %s, this trip %s. Only %s left on booking %s.",
                                name, booked.toPlainString(), moved.toPlainString(), e.getValue().toPlainString(),
                                remaining.toPlainString(), booking.getBookingNumber()),
                        "Reduce the trip quantity, increase the booking quantity, or set BOOKING_QTY_TOLERANCE_PERCENT in settings.");
            }
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void validateVehicleDriverAssignment(Trip trip) {
        if (trip != null && trip.getVehicle() != null && trip.getVehicle().getId() != null
                && trip.getDriver() != null && trip.getDriver().getId() != null) {
            Long vehId = trip.getVehicle().getId();
            Long drvId = trip.getDriver().getId();
            Optional<VehicleDriverAssignment> activeAssign = assignmentRepository
                    .findByVehicleIdAndDriverIdAndRemovalDateIsNullAndIsDeletedFalse(vehId, drvId);
            if (activeAssign.isEmpty()) {
                List<String> errorDetails = new ArrayList<>();
                errorDetails.add(String.format("Driver ID %d is not actively assigned to Vehicle ID %d.", drvId, vehId));
                throw new BusinessValidationException(
                        "Vehicle Driver Assignment Required",
                        "DRIVER_VEHICLE_ASSIGNMENT_REQUIRED",
                        "Trip cannot be dispatched/created without an active vehicle-driver pairing assignment.",
                        "Create an active vehicle-driver assignment before selecting this vehicle and driver for the trip.",
                        errorDetails
                );
            }
        }
    }

    /**
     * Blocks a new use of a vehicle that has an IN_PROGRESS work order.
     * A trip that already references the same vehicle is left unchanged.
     * The vehicle row is locked first so starting maintenance and creating a trip serialize.
     * There is no role bypass.
     */
    private void assertVehicleAvailableForNewTrip(Trip trip, Long alreadyAssignedVehicleId) {
        if (trip == null || trip.getVehicle() == null || trip.getVehicle().getId() == null) {
            return;
        }
        Long vehicleId = trip.getVehicle().getId();
        if (vehicleId.equals(alreadyAssignedVehicleId)) {
            return;
        }
        vehicleRepository.findByIdForUpdate(vehicleId).ifPresent(locked -> {
            if (maintenanceState.isVehicleUnderMaintenance(locked.getId())) {
                throw new BusinessValidationException(
                        "This vehicle is currently under maintenance and cannot be used for a new trip.",
                        "VEHICLE_UNDER_MAINTENANCE",
                        "VEHICLE_UNDER_MAINTENANCE: Vehicle is currently under maintenance and cannot be used for a new trip.",
                        "Complete or cancel the in-progress work order before planning a new trip for this vehicle."
                );
            }
        });
    }

    @Transactional
    public Trip dispatchTrip(Long id, String dispatchedByUsername) {
        Trip trip = getTripById(id);
        requireStatus(trip, "PLANNED", "dispatched");
        if (trip.getVehicle() == null || trip.getDriver() == null) {
            throw new BusinessValidationException("Vehicle And Driver Required", "TRIP_DISPATCH_UNASSIGNED",
                    "Trip " + trip.getTripNumber() + " needs a vehicle and a driver before dispatch.",
                    "Edit the trip and allocate a vehicle and driver.");
        }
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
        requireStatus(trip, "DISPATCHED", "completed");
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

    private void requireStatus(Trip trip, String expected, String action) {
        if (!expected.equalsIgnoreCase(trip.getStatus())) {
            throw new BusinessValidationException("Invalid Trip Status", "TRIP_STATUS_TRANSITION_BLOCKED",
                    "Trip " + trip.getTripNumber() + " is " + trip.getStatus() + " and cannot be " + action + ".",
                    "Trips move PLANNED -> DISPATCHED -> COMPLETED.");
        }
    }

    @Transactional
    public void deleteTrip(Long id, String deletedByUsername) {
        Trip trip = getTripById(id);

        validationService.validateTripDelete(trip);

        trip.setIsDeleted(true);
        trip.setUpdatedBy(deletedByUsername);
        tripRepository.save(trip);

        auditService.log(deletedByUsername, "TRIP_CANCELLED", "trips", trip.getId(), null,
                "Cancelled trip itinerary: " + trip.getTripNumber());
    }
}
