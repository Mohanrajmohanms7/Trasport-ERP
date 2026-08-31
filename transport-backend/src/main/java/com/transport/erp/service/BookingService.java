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
    private BookingRepository bookingRepository;

    @Autowired
    private TenantAccessService tenantAccess;

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
        
        booking.setBookingNumber(prefix + System.currentTimeMillis());
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(defaultStatus);
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
                BigDecimal base = detail.getRate()
                        .add(detail.getTransportRate())
                        .add(detail.getRoyaltyRate())
                        .add(detail.getLoadingCharge());
                BigDecimal subTotal = detail.getQuantity().multiply(base);
                BigDecimal taxFactor = BigDecimal.ONE.add(detail.getGstPercentage().divide(BigDecimal.valueOf(100)));
                detail.setNetAmount(subTotal.multiply(taxFactor));
            }
        }

        Booking saved = bookingRepository.save(booking);

        auditService.log(createdByUsername, "BOOKING_CREATED", "bookings", saved.getId(), null,
                "Registered customer booking number: " + saved.getBookingNumber());

        return saved;
    }

    @Transactional
    public Booking updateBooking(Long id, Booking details, String updatedByUsername) {
        Booking existing = getBookingById(id);

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

                BigDecimal base = d.getRate()
                        .add(d.getTransportRate())
                        .add(d.getRoyaltyRate())
                        .add(d.getLoadingCharge());
                BigDecimal subTotal = d.getQuantity().multiply(base);
                BigDecimal taxFactor = BigDecimal.ONE.add(d.getGstPercentage().divide(BigDecimal.valueOf(100)));
                d.setNetAmount(subTotal.multiply(taxFactor));
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
        booking.setStatus("APPROVED");
        booking.setUpdatedBy(approvedByUsername);
        
        Booking saved = bookingRepository.save(booking);

        auditService.log(approvedByUsername, "BOOKING_APPROVED", "bookings", saved.getId(), null,
                "Approved customer booking: " + saved.getBookingNumber());

        return saved;
    }

    @Transactional
    public Booking rejectBooking(Long id, String rejectedByUsername) {
        Booking booking = getBookingById(id);
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
        booking.setIsDeleted(true);
        booking.setUpdatedBy(deletedByUsername);
        bookingRepository.save(booking);

        auditService.log(deletedByUsername, "BOOKING_DELETED", "bookings", booking.getId(), null,
                "Soft deleted booking: " + booking.getBookingNumber());
    }
}
