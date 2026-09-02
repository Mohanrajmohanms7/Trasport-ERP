package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BusinessDependencyValidationService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerDeliverySiteRepository siteRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripDetailRepository tripDetailRepository;

    @Autowired
    private SalesInvoiceRepository salesInvoiceRepository;

    @Autowired
    private SalesInvoiceDetailRepository salesInvoiceDetailRepository;

    @Autowired
    private CustomerReceiptRepository customerReceiptRepository;

    @Autowired
    private CustomerReceiptAllocationRepository allocationRepository;

    @Autowired
    private FuelEntryRepository fuelEntryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    public void validateCompanyDelete(Long companyId) {
        tenantAccess.assertCompanyAccess(companyId);
        long customerCount = customerRepository.countByCompanyIdAndIsDeletedFalse(companyId);
        if (customerCount > 0) {
            throw new BusinessValidationException(
                    "Company Cannot Be Deleted",
                    "COMPANY_HAS_DEPENDENCIES",
                    "Company cannot be deleted while active tenant records exist.",
                    "Contact System Administrator to deactivate the company."
            );
        }
    }

    public void validateBranchDelete(Long branchId) {
        // Reserved for branch-level validation
    }

    public void validateCustomerDelete(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        tenantAccess.assertOwned(customer.getCompanyId());

        long bookingCount = bookingRepository.countByCustomerIdAndIsDeletedFalse(customerId);
        long invoiceCount = salesInvoiceRepository.countByCustomerIdAndIsDeletedFalse(customerId);
        long receiptCount = customerReceiptRepository.countByCustomerIdAndIsDeletedFalse(customerId);

        if (bookingCount > 0 || invoiceCount > 0 || receiptCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Customer '%s' cannot be deleted because it is already used by %d bookings, %d invoices, and %d payment receipts.",
                    customer.getName(), bookingCount, invoiceCount, receiptCount));
            details.add("Deleting this customer would break historical transaction records.");

            throw new BusinessValidationException(
                    "Customer Cannot Be Deleted",
                    "CUSTOMER_HAS_DEPENDENCIES",
                    String.format("Customer '%s' has active transaction dependencies.", customer.getName()),
                    "Deactivate the customer instead if it should no longer be available for new transactions.",
                    details
            );
        }
    }

    public void validateDeliverySiteDelete(Long siteId) {
        CustomerDeliverySite site = siteRepository.findById(siteId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Delivery site not found with ID: " + siteId));

        tenantAccess.assertCompanyAccess(site.getCompanyId());

        long bookingCount = bookingRepository.countByDeliverySiteIdAndIsDeletedFalse(siteId);
        if (bookingCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Delivery site '%s' cannot be deleted because it is referenced by %d booking transactions.",
                    site.getSiteName(), bookingCount));
            details.add("Deleting this site would corrupt historical logistics records.");

            throw new BusinessValidationException(
                    "Delivery Site Cannot Be Deleted",
                    "DELIVERY_SITE_HAS_DEPENDENCIES",
                    String.format("Delivery site '%s' is in use.", site.getSiteName()),
                    "Deactivate the site instead if it should no longer be available for new bookings.",
                    details
            );
        }
    }

    public void validateVehicleDelete(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + vehicleId));

        tenantAccess.assertOwned(vehicle.getCompanyId());

        long tripCount = tripRepository.countByVehicleIdAndIsDeletedFalse(vehicleId);
        long fuelCount = fuelEntryRepository.countByVehicleIdAndIsDeletedFalse(vehicleId);
        long expenseCount = expenseRepository.countByVehicleIdAndIsDeletedFalse(vehicleId);

        String vehicleIdentifier = vehicle.getName() != null && !vehicle.getName().trim().isEmpty() ? vehicle.getName() : vehicle.getCode();

        if (tripCount > 0 || fuelCount > 0 || expenseCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle '%s' cannot be deleted because it is used in %d trips, %d fuel entries, and %d expenses.",
                    vehicleIdentifier, tripCount, fuelCount, expenseCount));

            throw new BusinessValidationException(
                    "Vehicle Cannot Be Deleted",
                    "VEHICLE_HAS_DEPENDENCIES",
                    String.format("Vehicle '%s' has historical dispatch and operational records.", vehicleIdentifier),
                    "Deactivate the vehicle instead if it should no longer be available for future trips.",
                    details
            );
        }
    }

    public void validateDriverDelete(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        tenantAccess.assertOwned(driver.getCompanyId());

        long tripCount = tripRepository.countByDriverIdAndIsDeletedFalse(driverId);
        long fuelCount = fuelEntryRepository.countByDriverIdAndIsDeletedFalse(driverId);
        long expenseCount = expenseRepository.countByDriverIdAndIsDeletedFalse(driverId);

        if (tripCount > 0 || fuelCount > 0 || expenseCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Driver '%s' cannot be deleted because this driver is used in %d trips, %d fuel entries, and %d expenses.",
                    driver.getName(), tripCount, fuelCount, expenseCount));

            throw new BusinessValidationException(
                    "Driver Cannot Be Deleted",
                    "DRIVER_HAS_DEPENDENCIES",
                    String.format("Driver '%s' has historical dispatch records.", driver.getName()),
                    "Deactivate the driver instead if the driver should no longer be available for future trips.",
                    details
            );
        }
    }

    public void validateMaterialDelete(Long materialId) {
        Material material = materialRepository.findById(materialId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Material not found with ID: " + materialId));

        tenantAccess.assertOwned(material.getCompanyId());

        long bookingDetailCount = bookingDetailRepository.countByMaterialIdAndBookingIsDeletedFalse(materialId);
        long tripDetailCount = tripDetailRepository.countByMaterialIdAndTripIsDeletedFalse(materialId);
        long invoiceDetailCount = salesInvoiceDetailRepository.countByMaterialIdAndInvoiceIsDeletedFalse(materialId);

        if (bookingDetailCount > 0 || tripDetailCount > 0 || invoiceDetailCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Material '%s' cannot be deleted because it is used in %d booking items, %d trip items, and %d invoice items.",
                    material.getName(), bookingDetailCount, tripDetailCount, invoiceDetailCount));

            throw new BusinessValidationException(
                    "Material Cannot Be Deleted",
                    "MATERIAL_HAS_DEPENDENCIES",
                    String.format("Material '%s' is referenced in existing business transactions.", material.getName()),
                    "Deactivate the material if it should no longer be available for new transactions.",
                    details
            );
        }
    }

    public void validateBookingDelete(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        tenantAccess.assertOwned(booking.getCompanyId());

        long tripCount = tripRepository.countByBookingIdAndIsDeletedFalse(bookingId);
        long invoiceCount = salesInvoiceRepository.countByBookingIdAndIsDeletedFalse(bookingId);

        if (!"DRAFT".equalsIgnoreCase(booking.getStatus()) || tripCount > 0 || invoiceCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Booking '%s' cannot be deleted because a trip or invoice (%s) has already been generated.",
                    booking.getBookingNumber(), booking.getStatus()));

            throw new BusinessValidationException(
                    "Booking Cannot Be Deleted",
                    "BOOKING_HAS_DEPENDENCIES",
                    String.format("Booking '%s' is in %s state with linked operational records.", booking.getBookingNumber(), booking.getStatus()),
                    "Use the booking cancellation workflow instead of deleting.",
                    details
            );
        }
    }

    public void validateTripDelete(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + tripId));

        tenantAccess.assertOwned(trip.getCompanyId());

        long invoiceCount = salesInvoiceRepository.countByTripIdAndIsDeletedFalse(tripId);

        if ("COMPLETED".equalsIgnoreCase(trip.getStatus()) || invoiceCount > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Trip '%s' cannot be deleted because a sales invoice has already been generated or trip is completed.",
                    trip.getTripNumber()));

            throw new BusinessValidationException(
                    "Trip Cannot Be Deleted",
                    "TRIP_HAS_DEPENDENCIES",
                    String.format("Trip '%s' is in %s state.", trip.getTripNumber(), trip.getStatus()),
                    "Cancel or reverse the trip itinerary through controlled workflow.",
                    details
            );
        }
    }

    public void validateInvoiceDelete(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + invoiceId));

        tenantAccess.assertOwned(invoice.getCompanyId());

        long allocationCount = allocationRepository.countByInvoiceIdAndIsDeletedFalse(invoiceId);
        BigDecimal allocatedTotal = allocationRepository.sumAllocatedAmountByInvoiceId(invoiceId);

        if (!"DRAFT".equalsIgnoreCase(invoice.getStatus()) || allocationCount > 0 || allocatedTotal.compareTo(BigDecimal.ZERO) > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be deleted because ₹%.2f in customer payments has already been allocated to this invoice.",
                    invoice.getInvoiceNumber(), allocatedTotal));
            details.add("Financial accounting history cannot be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_HAS_PAYMENT_DEPENDENCY",
                    String.format("Invoice '%s' has active payment transactions or is approved.", invoice.getInvoiceNumber()),
                    "Cancel or reverse the related payment transaction before attempting invoice cancellation.",
                    details
            );
        }
    }

    public void validateReceiptDelete(Long receiptId) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer receipt not found with ID: " + receiptId));

        tenantAccess.assertOwned(receipt.getCompanyId());

        if (!"DRAFT".equalsIgnoreCase(receipt.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' is currently in '%s' status and posted to General Ledger.",
                    receipt.getReceiptNumber(), receipt.getStatus()));

            throw new BusinessValidationException(
                    "Receipt Cannot Be Deleted",
                    "RECEIPT_ALREADY_APPROVED",
                    String.format("Receipt '%s' is already approved/posted.", receipt.getReceiptNumber()),
                    "Use the customer receipt cancellation/reversal workflow.",
                    details
            );
        }
    }

    public void validatePaymentAllocation(CustomerReceipt receipt, SalesInvoice invoice, BigDecimal allocationAmount) {
        if (allocationAmount == null || allocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException(
                    "Invalid Allocation Amount",
                    "INVALID_ALLOCATION_AMOUNT",
                    "Allocation amount must be greater than zero.",
                    "Enter a positive allocation amount."
            );
        }

        BigDecimal outstanding = invoice.getNetAmount().subtract(invoice.getPaidAmount());
        if (allocationAmount.compareTo(outstanding) > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("You entered ₹%.2f, but the invoice outstanding balance is only ₹%.2f.",
                    allocationAmount, outstanding));

            throw new BusinessValidationException(
                    "Payment Allocation Exceeded",
                    "ALLOCATION_EXCEEDS_OUTSTANDING",
                    String.format("Allocation ₹%.2f exceeds invoice outstanding balance ₹%.2f.", allocationAmount, outstanding),
                    String.format("Enter an allocation amount up to ₹%.2f.", outstanding),
                    details
            );
        }

        if (!receipt.getCustomer().getId().equals(invoice.getCustomer().getId())) {
            throw new BusinessValidationException(
                    "Customer Mismatch",
                    "ALLOCATION_CUSTOMER_MISMATCH",
                    "Receipt customer and invoice customer do not match.",
                    "Select an invoice belonging to the same corporate customer."
            );
        }
    }
}
