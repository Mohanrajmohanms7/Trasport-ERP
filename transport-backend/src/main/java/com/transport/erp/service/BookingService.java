package com.transport.erp.service;

import com.transport.erp.security.TenantAccessService;

import com.transport.erp.model.Booking;
import com.transport.erp.model.BookingDetail;
import com.transport.erp.model.AppSetting;
import com.transport.erp.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.transport.erp.security.TenantParentAccess;

@Service
public class BookingService {

    @Autowired
    private DocumentNumberService documentNumberService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private com.transport.erp.repository.TripRepository tripRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Autowired
    private BusinessDependencyValidationService validationService;

    @Autowired
    private TenantParentAccess tenantParentAccess;



    @Autowired
    private AuditService auditService;




    @Autowired
    private AppSettingService settingService;




    public Page<Booking> getBookings(Long companyId, String status, Pageable pageable) {
        if (status != null && !status.trim().isEmpty()) {
            return bookingRepository.findByCompanyIdAndIsDeletedFalseAndStatus(companyId, status, pageable);
        }
        return bookingRepository.findByCompanyIdAndIsDeletedFalse(companyId, pageable);
    }

    public Booking getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        tenantAccess.assertOwned(booking.getCompanyId());
        return booking;
    }

    @Transactional
    public Booking createBooking(Booking booking, String createdByUsername) {
        String prefix = settingService.getByKey("PREFIX_BOOKING").map(s -> s.getValueData()).orElse("BKG-");
        String defaultStatus = settingService.getByKey("DEFAULT_BOOKING_STATUS").map(s -> s.getValueData()).orElse("PENDING");
        
        booking.setBookingNumber(documentNumberService.next(tenantAccess.resolveCompanyId(booking.getCompanyId()),
                DocumentNumberService.BOOKING, prefix, LocalDate.now()));
        booking.setBookingDate(LocalDate.now());
        if (booking.getStatus() == null || booking.getStatus().trim().isEmpty()) {
            booking.setStatus(defaultStatus);
        }
        booking.setIsDeleted(false);
        booking.setCreatedBy(createdByUsername);
        booking.setUpdatedBy(createdByUsername);

        if (booking.getCustomer() != null && booking.getCustomer().getId() != null) {
            tenantParentAccess.requireCustomer(booking.getCustomer().getId());
        }

        booking.setCompanyId(tenantAccess.resolveCompanyId(booking.getCompanyId()));
        booking.setBranchId(tenantAccess.resolveBranchId(booking.getBranchId()));


        // Map parent links and calculate totals
        if (booking.getDetails() != null) {
            for (BookingDetail detail : booking.getDetails()) {
                detail.setBooking(booking);
                detail.setIsDeleted(false);
                detail.setCreatedBy(createdByUsername);
                detail.setUpdatedBy(createdByUsername);
                detail.setCompanyId(booking.getCompanyId());
                detail.setBranchId(booking.getBranchId());

                // Calculate Net Amount = quantity * (rate + transportRate + royaltyRate + loadingCharge) + GST
                detail.setNetAmount(lineAmount(detail));
            }
        }

        Booking saved = bookingRepository.save(booking);

        auditService.log(createdByUsername, "BOOKING_CREATED", "bookings", saved.getId(), null,
                "Registered customer booking number: " + saved.getBookingNumber());

        return saved;
    }

    @Transactional
    public Booking updateBooking(Long id, Booking details, String updatedByUsername) {
        Booking existing = bookingRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        tenantAccess.assertOwned(existing.getCompanyId());
        String st = existing.getStatus() == null ? "" : existing.getStatus().toUpperCase();
        if ("REJECTED".equals(st) || "CANCELLED".equals(st) || "COMPLETED".equals(st) || "CLOSED".equals(st)) {
            throw new com.transport.erp.exception.BusinessValidationException("Booking Locked", "BOOKING_UPDATE_BLOCKED",
                    "Booking " + existing.getBookingNumber() + " is " + existing.getStatus() + " and cannot be edited.",
                    "Create a new booking instead.");
        }
        // Quantity already moved per material by active trips: a material with trips must stay, and its
        // booked quantity cannot drop below what was already moved.
        java.util.Map<Long, BigDecimal> moved = new java.util.HashMap<>();
        for (Object[] row : tripRepository.sumQuantityByMaterialForBooking(existing.getId(), -1L)) {
            moved.put((Long) row[0], (BigDecimal) row[1]);
        }
        if (!moved.isEmpty()) {
            java.util.Map<Long, BigDecimal> newQty = new java.util.HashMap<>();
            if (details.getDetails() != null) {
                for (BookingDetail d : details.getDetails()) {
                    if (d.getMaterial() != null && d.getMaterial().getId() != null) {
                        newQty.merge(d.getMaterial().getId(), nz(d.getQuantity()), BigDecimal::add);
                    }
                }
            }
            for (java.util.Map.Entry<Long, BigDecimal> e : moved.entrySet()) {
                BigDecimal q = newQty.get(e.getKey());
                if (q == null || q.compareTo(e.getValue()) < 0) {
                    throw new com.transport.erp.exception.BusinessValidationException("Quantity Below Delivered", "BOOKING_QTY_BELOW_MOVED",
                            "Trips on booking " + existing.getBookingNumber() + " already moved " + e.getValue().toPlainString()
                                    + " of material ID " + e.getKey() + "; the booked quantity cannot be lower or removed.",
                            "Keep the material and set its quantity to at least what has been moved.");
                }
            }
        }

        existing.setPriority(details.getPriority());
        existing.setRemarks(details.getRemarks());
        existing.setUpdatedBy(updatedByUsername);

        // Replace details
        existing.getDetails().clear();
        if (details.getDetails() != null) {
            for (BookingDetail d : details.getDetails()) {
                d.setBooking(existing);
                d.setIsDeleted(false);
                d.setCreatedBy(updatedByUsername);
                d.setUpdatedBy(updatedByUsername);
                d.setCompanyId(existing.getCompanyId());
                d.setBranchId(existing.getBranchId());

                d.setNetAmount(lineAmount(d));
                existing.getDetails().add(d);
            }
        }

        Booking saved = bookingRepository.save(existing);

        auditService.log(updatedByUsername, "BOOKING_UPDATED", "bookings", saved.getId(), null,
                "Modified booking details for: " + saved.getBookingNumber());

        return saved;
    }

    @Transactional
    public Booking approveBooking(Long id, String approvedByUsername) {
        Booking booking = getBookingById(id);
        String st = booking.getStatus() == null ? "" : booking.getStatus().toUpperCase();
        if ("APPROVED".equals(st)) {
            throw new com.transport.erp.exception.BusinessValidationException("Already Approved", "BOOKING_ALREADY_APPROVED",
                    "Booking " + booking.getBookingNumber() + " is already approved.", "No action needed.");
        }
        if ("REJECTED".equals(st) || "CANCELLED".equals(st)) {
            throw new com.transport.erp.exception.BusinessValidationException("Booking Closed", "BOOKING_APPROVE_BLOCKED",
                    "Booking " + booking.getBookingNumber() + " is " + booking.getStatus() + " and cannot be approved.",
                    "Create a new booking.");
        }
        if (booking.getDetails() == null || booking.getDetails().isEmpty()) {
            throw new com.transport.erp.exception.BusinessValidationException("Empty Booking", "BOOKING_NO_LINES",
                    "Booking " + booking.getBookingNumber() + " has no material lines.", "Add at least one material before approving.");
        }
        booking.setStatus("APPROVED");
        booking.setUpdatedBy(approvedByUsername);
        
        Booking saved = bookingRepository.save(booking);

        auditService.log(approvedByUsername, "BOOKING_APPROVED", "bookings", saved.getId(), null,
                "Approved customer booking: " + saved.getBookingNumber());

        return saved;
    }

    /** Short-close: the customer needs no more loads. Only APPROVED bookings with no planned/dispatched trips. */
    @Transactional
    public Booking closeBooking(Long id, String username) {
        Booking booking = bookingRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        tenantAccess.assertOwned(booking.getCompanyId());
        if (!"APPROVED".equalsIgnoreCase(booking.getStatus())) {
            throw new com.transport.erp.exception.BusinessValidationException("Booking Not Open", "BOOKING_CLOSE_BLOCKED",
                    "Booking " + booking.getBookingNumber() + " is " + booking.getStatus() + "; only approved bookings can be closed.",
                    "No action needed.");
        }
        long open = tripRepository.countOpenTripsForBooking(booking.getId());
        if (open > 0) {
            throw new com.transport.erp.exception.BusinessValidationException("Trips Still Running", "BOOKING_CLOSE_OPEN_TRIPS",
                    "Booking " + booking.getBookingNumber() + " has " + open + " planned or dispatched trip(s).",
                    "Complete or cancel those trips first.");
        }
        booking.setStatus("COMPLETED");
        booking.setUpdatedBy(username);
        Booking saved = bookingRepository.save(booking);
        auditService.log(username, "BOOKING_CLOSED", "bookings", saved.getId(), null,
                "Closed booking " + saved.getBookingNumber() + " (no more loads)");
        return saved;
    }

    @Transactional
    public Booking rejectBooking(Long id, String rejectedByUsername) {
        Booking booking = getBookingById(id);
        long activeTrips = tripRepository.countByBookingIdAndIsDeletedFalse(booking.getId());
        if (activeTrips > 0) {
            throw new com.transport.erp.exception.BusinessValidationException("Booking Has Trips", "BOOKING_REJECT_HAS_TRIPS",
                    "Booking " + booking.getBookingNumber() + " already has " + activeTrips + " trip(s) and cannot be rejected.",
                    "Cancel the planned trips first.");
        }
        booking.setStatus("REJECTED");
        booking.setUpdatedBy(rejectedByUsername);

        Booking saved = bookingRepository.save(booking);

        auditService.log(rejectedByUsername, "BOOKING_REJECTED", "bookings", saved.getId(), null,
                "Rejected customer booking: " + saved.getBookingNumber());

        return saved;
    }

    @Transactional
    public void deleteBooking(Long id, String deletedByUsername) {
        Booking booking = getBookingById(id);

        validationService.validateBookingDelete(booking);

        booking.setIsDeleted(true);
        booking.setUpdatedBy(deletedByUsername);
        bookingRepository.save(booking);

        auditService.log(deletedByUsername, "BOOKING_DELETED", "bookings", booking.getId(), null,
                "Soft deleted booking: " + booking.getBookingNumber());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** quantity x (rate + transport + royalty + loading) x (1 + GST%), rounded to paise. */
    private static BigDecimal lineAmount(BookingDetail d) {
        if (d.getQuantity() == null || d.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Booking quantity must be greater than zero.");
        }
        BigDecimal base = nz(d.getRate()).add(nz(d.getTransportRate())).add(nz(d.getRoyaltyRate())).add(nz(d.getLoadingCharge()));
        if (base.signum() < 0) throw new IllegalArgumentException("Booking rates cannot be negative.");
        BigDecimal taxable = d.getQuantity().multiply(base).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal tax = taxable.multiply(nz(d.getGstPercentage())).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return taxable.add(tax);
    }
}
