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

    @org.springframework.beans.factory.annotation.Autowired
    private com.transport.erp.repository.SalesInvoiceRepository salesInvoiceRepository;

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

    @Autowired
    private OrderUomService orderUomService;

    @Autowired
    private com.transport.erp.repository.MaterialRepository materialRepository;




    public Page<Booking> getBookings(Long companyId, String status, Pageable pageable) {
        return bookingRepository.findForList(companyId, tenantAccess.listBranchScope(), (status == null || status.trim().isEmpty() ? null : status.trim()), pageable);
    }

    public Page<Booking> searchForPicker(Long companyId, Long customerId, String search, Pageable pageable) {
        return bookingRepository.searchForPicker(companyId, customerId, search == null ? "" : search.trim(), pageable);
    }

    public Booking getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        tenantAccess.assertOwned(booking.getCompanyId());
        tenantAccess.assertBranchVisible(booking.getBranchId());
        return booking;
    }

    @Transactional
    public Booking createBooking(Booking booking, String createdByUsername) {
        if (booking.getCustomer() == null || booking.getCustomer().getId() == null) {
            throw new com.transport.erp.exception.BusinessValidationException("Customer Required", "BOOKING_CUSTOMER_REQUIRED",
                    "Select the customer for this booking.", "Choose a customer, then add the materials.");
        }
        String prefix = settingService.getByKey("PREFIX_BOOKING").map(s -> s.getValueData()).orElse("BKG-");
        String defaultStatus = settingService.getByKey("DEFAULT_BOOKING_STATUS").map(s -> s.getValueData()).orElse("PENDING");
        
        booking.setBookingNumber(documentNumberService.next(tenantAccess.resolveCompanyId(booking.getCompanyId()),
                DocumentNumberService.BOOKING, prefix, LocalDate.now()));
        // Orders taken earlier (phone / WhatsApp) can be entered afterwards; never a future date.
        LocalDate bookingDate = booking.getBookingDate() != null ? booking.getBookingDate() : LocalDate.now();
        if (bookingDate.isAfter(LocalDate.now())) {
            throw new com.transport.erp.exception.BusinessValidationException("Booking Date In Future", "BOOKING_DATE_IN_FUTURE",
                    "Booking date " + bookingDate + " is in the future.", "Use today's date or earlier.");
        }
        booking.setBookingDate(bookingDate);
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


        // Unit of every line (Unit / Ton / …): requested, else material default, else company default. Never converted.
        applyLineUnits(booking.getDetails(), booking.getCompanyId(), java.util.Map.of(), java.util.Map.of());

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

        // Units already on the booking stay valid even if the company later switches them off; a material that
        // already has trips keeps its unit (trip and invoice lines were recorded in it).
        java.util.Map<Long, com.transport.erp.model.UomMaster> previousUnits = new java.util.HashMap<>();
        for (BookingDetail d : existing.getDetails()) {
            if (d.getMaterial() != null && d.getUom() != null && !Boolean.TRUE.equals(d.getIsDeleted())) {
                previousUnits.putIfAbsent(d.getMaterial().getId(), d.getUom());
            }
        }
        applyLineUnits(details.getDetails(), existing.getCompanyId(), previousUnits, moved);

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
        // Credit control: do not commit more loads to a customer who would go over the agreed credit limit.
        com.transport.erp.model.Customer cust = booking.getCustomer();
        if (cust != null && cust.getCreditLimit() != null && cust.getCreditLimit().signum() > 0) {
            java.math.BigDecimal due = salesInvoiceRepository.sumOutstandingByCustomer(cust.getId(), booking.getCompanyId(), null);
            java.math.BigDecimal value = booking.getDetails().stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                    .map(d -> d.getNetAmount() == null ? java.math.BigDecimal.ZERO : d.getNetAmount())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal exposure = (due == null ? java.math.BigDecimal.ZERO : due).add(value);
            if (exposure.compareTo(cust.getCreditLimit()) > 0) {
                throw new com.transport.erp.exception.BusinessValidationException("Credit Limit Exceeded", "BOOKING_CREDIT_LIMIT",
                        cust.getName() + " owes ₹" + due.setScale(2, java.math.RoundingMode.HALF_UP) + "; with this booking (₹"
                                + value.setScale(2, java.math.RoundingMode.HALF_UP) + ") that is more than the credit limit of ₹"
                                + cust.getCreditLimit().setScale(2, java.math.RoundingMode.HALF_UP) + ".",
                        "Collect payment first, or raise the customer's credit limit in Customer Master.");
            }
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

    /**
     * Sets the unit of measure on each booking line (see {@link OrderUomService.OrderUnits#resolve}).
     * One material is booked in one unit per booking, so trips and invoices of that material share it.
     */
    private void applyLineUnits(java.util.List<BookingDetail> lines, Long companyId,
                                java.util.Map<Long, com.transport.erp.model.UomMaster> previousUnits,
                                java.util.Map<Long, BigDecimal> movedByMaterial) {
        if (lines == null || lines.isEmpty()) return;
        OrderUomService.OrderUnits units = orderUomService.forCompany(companyId);
        java.util.Set<Long> materialIds = new java.util.HashSet<>();
        for (BookingDetail d : lines) {
            if (d.getMaterial() != null && d.getMaterial().getId() != null) materialIds.add(d.getMaterial().getId());
        }
        java.util.Map<Long, com.transport.erp.model.Material> materials = new java.util.HashMap<>();
        for (com.transport.erp.model.Material m : materialRepository.findAllById(materialIds)) materials.put(m.getId(), m);

        java.util.Map<Long, com.transport.erp.model.UomMaster> unitByMaterial = new java.util.HashMap<>();
        for (BookingDetail d : lines) {
            if (d.getMaterial() == null || d.getMaterial().getId() == null) continue;
            Long matId = d.getMaterial().getId();
            com.transport.erp.model.Material m = materials.get(matId);
            String matName = m != null && m.getName() != null ? m.getName() : "material ID " + matId;
            com.transport.erp.model.UomMaster previous = previousUnits.get(matId);
            com.transport.erp.model.UomMaster uom = units.resolve(d.getUom(), m, previous, matName);

            com.transport.erp.model.UomMaster seen = unitByMaterial.putIfAbsent(matId, uom);
            if (seen != null && !seen.getId().equals(uom.getId())) {
                throw new com.transport.erp.exception.BusinessValidationException("One Unit Per Material", "BOOKING_MIXED_UOM",
                        matName + " is on this booking in both " + OrderUomService.label(seen) + " and "
                                + OrderUomService.label(uom) + ".",
                        "Use one unit for " + matName + " on this booking (add the quantities together), or make a separate booking.");
            }
            if (previous != null && movedByMaterial.containsKey(matId) && !previous.getId().equals(uom.getId())) {
                throw new com.transport.erp.exception.BusinessValidationException("Unit Locked", "BOOKING_UOM_LOCKED",
                        "Trips for " + matName + " were already recorded in " + OrderUomService.label(previous)
                                + ", so its unit cannot change to " + OrderUomService.label(uom) + ".",
                        "Keep " + OrderUomService.label(previous) + " for " + matName + " on this booking.");
            }
            d.setUom(uom);
        }
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
