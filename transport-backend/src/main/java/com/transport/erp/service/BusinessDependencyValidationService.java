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
            List<String> depParts = new ArrayList<>();
            if (bookingCount > 0) depParts.add(bookingCount + " booking(s)");
            if (invoiceCount > 0) depParts.add(invoiceCount + " invoice(s)");
            if (receiptCount > 0) depParts.add(receiptCount + " payment receipt(s)");

            String depSummary = String.join(", ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Customer '%s' cannot be deleted because it is referenced by %s.", customer.getName(), depSummary));
            details.add("Historical transaction records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Customer Cannot Be Deleted",
                    "CUSTOMER_HAS_DEPENDENCIES",
                    String.format("Customer '%s' has active transaction dependencies.", customer.getName()),
                    "Deactivate the customer instead of deleting it.",
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
            details.add(String.format("Delivery site '%s' cannot be deleted because it is referenced by %d booking transaction(s).",
                    site.getSiteName(), bookingCount));
            details.add("Historical delivery site logistics records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Delivery Site Cannot Be Deleted",
                    "DELIVERY_SITE_HAS_DEPENDENCIES",
                    String.format("Delivery site '%s' is in use.", site.getSiteName()),
                    "Deactivate the delivery site instead of deleting it.",
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
            List<String> depParts = new ArrayList<>();
            if (tripCount > 0) depParts.add(tripCount + " trip(s)");
            if (fuelCount > 0) depParts.add(fuelCount + " fuel entry/entries");
            if (expenseCount > 0) depParts.add(expenseCount + " expense(s)");

            String depSummary = String.join(", ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Vehicle '%s' cannot be deleted because it is referenced by %s.", vehicleIdentifier, depSummary));
            details.add("Historical vehicle operation records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Vehicle Cannot Be Deleted",
                    "VEHICLE_HAS_DEPENDENCIES",
                    String.format("Vehicle '%s' has historical dispatch and operational records.", vehicleIdentifier),
                    "Deactivate the vehicle instead of deleting it.",
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
            List<String> depParts = new ArrayList<>();
            if (tripCount > 0) depParts.add(tripCount + " trip(s)");
            if (fuelCount > 0) depParts.add(fuelCount + " fuel entry/entries");
            if (expenseCount > 0) depParts.add(expenseCount + " expense(s)");

            String depSummary = String.join(", ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Driver '%s' cannot be deleted because this driver is referenced by %s.", driver.getName(), depSummary));
            details.add("Historical driver assignment records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Driver Cannot Be Deleted",
                    "DRIVER_HAS_DEPENDENCIES",
                    String.format("Driver '%s' has historical dispatch records.", driver.getName()),
                    "Deactivate the driver instead of deleting it.",
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
            List<String> depParts = new ArrayList<>();
            if (bookingDetailCount > 0) depParts.add(bookingDetailCount + " booking item(s)");
            if (tripDetailCount > 0) depParts.add(tripDetailCount + " trip item(s)");
            if (invoiceDetailCount > 0) depParts.add(invoiceDetailCount + " invoice item(s)");

            String depSummary = String.join(", ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Material '%s' cannot be deleted because it is referenced by %s.", material.getName(), depSummary));
            details.add("Historical material transaction records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Material Cannot Be Deleted",
                    "MATERIAL_HAS_DEPENDENCIES",
                    String.format("Material '%s' is referenced in existing business transactions.", material.getName()),
                    "Deactivate the material instead of deleting it.",
                    details
            );
        }
    }

    public void validateBookingDelete(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        tenantAccess.assertOwned(booking.getCompanyId());

        if (!"DRAFT".equalsIgnoreCase(booking.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Booking '%s' cannot be deleted because its current status is %s.",
                    booking.getBookingNumber(), booking.getStatus()));
            details.add("Only bookings in DRAFT status can be deleted.");

            throw new BusinessValidationException(
                    "Booking Cannot Be Deleted",
                    "BOOKING_STATUS_DELETE_BLOCKED",
                    String.format("Booking '%s' cannot be deleted because its current status is %s.", booking.getBookingNumber(), booking.getStatus()),
                    "Cancel the booking using the supported cancellation workflow instead of deleting it.",
                    details
            );
        }

        long tripCount = tripRepository.countByBookingIdAndIsDeletedFalse(bookingId);
        long invoiceCount = salesInvoiceRepository.countByBookingIdAndIsDeletedFalse(bookingId);

        if (tripCount > 0 || invoiceCount > 0) {
            List<String> depParts = new ArrayList<>();
            if (tripCount > 0) depParts.add(tripCount + " trip(s)");
            if (invoiceCount > 0) depParts.add(invoiceCount + " sales invoice(s)");

            String depSummary = String.join(" and ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Booking '%s' cannot be deleted because it is referenced by %s.",
                    booking.getBookingNumber(), depSummary));
            details.add("Historical transaction records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Booking Cannot Be Deleted",
                    "BOOKING_HAS_DEPENDENCIES",
                    String.format("Booking '%s' has active transaction dependencies.", booking.getBookingNumber()),
                    "Cancel or resolve the dependent transaction using its supported business workflow before attempting deletion.",
                    details
            );
        }
    }

    public void validateBookingDelete(Booking booking) {
        if (booking == null) return;
        tenantAccess.assertOwned(booking.getCompanyId());

        if (!"DRAFT".equalsIgnoreCase(booking.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Booking '%s' cannot be deleted because its current status is %s.",
                    booking.getBookingNumber(), booking.getStatus()));
            details.add("Only bookings in DRAFT status can be deleted.");

            throw new BusinessValidationException(
                    "Booking Cannot Be Deleted",
                    "BOOKING_STATUS_DELETE_BLOCKED",
                    String.format("Booking '%s' cannot be deleted because its current status is %s.", booking.getBookingNumber(), booking.getStatus()),
                    "Cancel the booking using the supported cancellation workflow instead of deleting it.",
                    details
            );
        }

        long tripCount = tripRepository.countByBookingIdAndIsDeletedFalse(booking.getId());
        long invoiceCount = salesInvoiceRepository.countByBookingIdAndIsDeletedFalse(booking.getId());

        if (tripCount > 0 || invoiceCount > 0) {
            List<String> depParts = new ArrayList<>();
            if (tripCount > 0) depParts.add(tripCount + " trip(s)");
            if (invoiceCount > 0) depParts.add(invoiceCount + " sales invoice(s)");

            String depSummary = String.join(" and ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Booking '%s' cannot be deleted because it is referenced by %s.",
                    booking.getBookingNumber(), depSummary));
            details.add("Historical transaction records must not be automatically deleted or altered.");

            throw new BusinessValidationException(
                    "Booking Cannot Be Deleted",
                    "BOOKING_HAS_DEPENDENCIES",
                    String.format("Booking '%s' has active transaction dependencies.", booking.getBookingNumber()),
                    "Cancel or resolve the dependent transaction using its supported business workflow before attempting deletion.",
                    details
            );
        }
    }

    public void validateTripDelete(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + tripId));

        validateTripDelete(trip);
    }

    public void validateTripDelete(Trip trip) {
        if (trip == null) return;
        tenantAccess.assertOwned(trip.getCompanyId());
        if (trip.getBranchId() != null) {
            tenantAccess.assertBranchAccess(trip.getBranchId());
        }

        String status = trip.getStatus();
        if (!"PLANNED".equalsIgnoreCase(status) && !"SCHEDULED".equalsIgnoreCase(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Trip '%s' cannot be deleted because its current status is %s.",
                    trip.getTripNumber(), status));
            details.add("Completed or dispatched trips cannot be deleted.");

            throw new BusinessValidationException(
                    "Trip Cannot Be Deleted",
                    "TRIP_STATUS_DELETE_BLOCKED",
                    String.format("Trip '%s' cannot be deleted because its current status is %s.", trip.getTripNumber(), status),
                    "Use the supported Trip cancellation/correction workflow instead of deleting the completed Trip.",
                    details
            );
        }

        long fuelCount = fuelEntryRepository.countByTripIdAndIsDeletedFalse(trip.getId());
        long expenseCount = expenseRepository.countByTripIdAndIsDeletedFalse(trip.getId());
        long invoiceCount = salesInvoiceRepository.countByTripIdAndIsDeletedFalse(trip.getId());

        if (fuelCount > 0 || expenseCount > 0 || invoiceCount > 0) {
            List<String> depParts = new ArrayList<>();
            if (fuelCount > 0) depParts.add(fuelCount + " fuel entry/entries");
            if (expenseCount > 0) depParts.add(expenseCount + " expense(s)");
            if (invoiceCount > 0) depParts.add(invoiceCount + " sales invoice(s)");

            String depSummary = String.join(" and ", depParts);
            List<String> details = new ArrayList<>();
            details.add(String.format("Trip '%s' cannot be deleted because it is referenced by %s.",
                    trip.getTripNumber(), depSummary));
            details.add("Operational and historical trip records must not be automatically deleted or altered.");

            String userAction;
            if (fuelCount > 0 && expenseCount == 0 && invoiceCount == 0) {
                userAction = "Resolve or cancel the related fuel transaction before attempting to delete the trip.";
            } else if (expenseCount > 0 && fuelCount == 0 && invoiceCount == 0) {
                userAction = "Resolve or cancel the related expense transaction before attempting to delete the trip.";
            } else if (invoiceCount > 0 && fuelCount == 0 && expenseCount == 0) {
                userAction = "Resolve the related sales invoice using its supported business workflow before attempting to delete the trip.";
            } else {
                userAction = "Resolve the related operational and financial transactions using their supported workflows before attempting to delete the trip.";
            }

            throw new BusinessValidationException(
                    "Trip Cannot Be Deleted",
                    "TRIP_HAS_DEPENDENCIES",
                    String.format("Trip '%s' has active operational or financial dependencies.", trip.getTripNumber()),
                    userAction,
                    details
            );
        }
    }

    // =========================================================================
    // SALES INVOICE LIFECYCLE & DEPENDENCY VALIDATION
    // =========================================================================

    public void validateInvoiceDelete(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + invoiceId));

        validateInvoiceDelete(invoice);
    }

    public void validateInvoiceDelete(SalesInvoice invoice) {
        if (invoice == null) return;

        tenantAccess.assertOwned(invoice.getCompanyId());
        if (invoice.getBranchId() != null) {
            tenantAccess.assertBranchAccess(invoice.getBranchId());
        }

        String status = invoice.getStatus() != null ? invoice.getStatus().toUpperCase() : "DRAFT";

        if ("APPROVED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be deleted because its current status is APPROVED.", invoice.getInvoiceNumber()));
            details.add("Historical financial documents cannot be deleted.");

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_ALREADY_APPROVED",
                    String.format("Invoice '%s' cannot be deleted because its current status is APPROVED.", invoice.getInvoiceNumber()),
                    "Use the supported cancellation/reversal workflow instead of deleting historical financial data.",
                    details
            );
        }

        if ("GENERATED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be deleted because its current status is GENERATED.", invoice.getInvoiceNumber()));

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_ALREADY_GENERATED",
                    String.format("Invoice '%s' cannot be deleted because its current status is GENERATED.", invoice.getInvoiceNumber()),
                    "Use the supported cancellation/reversal workflow instead of deleting historical financial data.",
                    details
            );
        }

        if ("CANCELLED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be deleted because it is already CANCELLED.", invoice.getInvoiceNumber()));

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_ALREADY_CANCELLED",
                    String.format("Invoice '%s' is already CANCELLED.", invoice.getInvoiceNumber()),
                    "No further action can be taken on a cancelled invoice.",
                    details
            );
        }

        if (!"DRAFT".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be deleted because its current status is %s.", invoice.getInvoiceNumber(), status));

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_STATUS_DELETE_BLOCKED",
                    String.format("Invoice '%s' cannot be deleted because its current status is %s.", invoice.getInvoiceNumber(), status),
                    "Only invoices in DRAFT status can be deleted.",
                    details
            );
        }

        long allocationCount = allocationRepository.countByInvoiceIdAndIsDeletedFalse(invoice.getId());
        BigDecimal allocatedTotal = allocationRepository.sumAllocatedAmountByInvoiceId(invoice.getId());
        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;

        if (allocationCount > 0 || allocatedTotal.compareTo(BigDecimal.ZERO) > 0 || paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal activePayment = allocatedTotal.compareTo(paidAmount) > 0 ? allocatedTotal : paidAmount;
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' has ₹%.2f in active customer payments/receipts allocated to it.", invoice.getInvoiceNumber(), activePayment));
            details.add("Automatic payment reversal is unsafe and disabled.");

            throw new BusinessValidationException(
                    "Invoice Cannot Be Deleted",
                    "INVOICE_HAS_PAYMENT_DEPENDENCY",
                    String.format("Invoice '%s' has active payment transactions allocated to it.", invoice.getInvoiceNumber()),
                    "Explicitly reverse or cancel the related customer receipt/payment using the receipt workflow before attempting invoice cancellation.",
                    details
            );
        }
    }

    public void validateInvoiceUpdate(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + invoiceId));
        validateInvoiceUpdate(invoice);
    }

    public void validateInvoiceUpdate(SalesInvoice invoice) {
        if (invoice == null) return;

        tenantAccess.assertOwned(invoice.getCompanyId());
        if (invoice.getBranchId() != null) {
            tenantAccess.assertBranchAccess(invoice.getBranchId());
        }

        String status = invoice.getStatus() != null ? invoice.getStatus().toUpperCase() : "DRAFT";

        if (!"DRAFT".equals(status)) {
            String errorCode;
            if ("APPROVED".equals(status)) errorCode = "INVOICE_ALREADY_APPROVED";
            else if ("GENERATED".equals(status)) errorCode = "INVOICE_ALREADY_GENERATED";
            else if ("CANCELLED".equals(status)) errorCode = "INVOICE_ALREADY_CANCELLED";
            else errorCode = "INVOICE_STATUS_UPDATE_BLOCKED";

            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be modified because its current status is %s.", invoice.getInvoiceNumber(), status));

            throw new BusinessValidationException(
                    "Invoice Cannot Be Modified",
                    errorCode,
                    String.format("Invoice '%s' cannot be modified because its current status is %s.", invoice.getInvoiceNumber(), status),
                    "Only DRAFT invoices can be modified.",
                    details
            );
        }
    }

    public void validateInvoiceCancel(Long invoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Sales Invoice not found with ID: " + invoiceId));
        validateInvoiceCancel(invoice);
    }

    public void validateInvoiceCancel(SalesInvoice invoice) {
        if (invoice == null) return;

        tenantAccess.assertOwned(invoice.getCompanyId());
        if (invoice.getBranchId() != null) {
            tenantAccess.assertBranchAccess(invoice.getBranchId());
        }

        String status = invoice.getStatus() != null ? invoice.getStatus().toUpperCase() : "DRAFT";

        if ("CANCELLED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' is already CANCELLED.", invoice.getInvoiceNumber()));

            throw new BusinessValidationException(
                    "Invoice Already Cancelled",
                    "INVOICE_ALREADY_CANCELLED",
                    String.format("Invoice '%s' is already CANCELLED.", invoice.getInvoiceNumber()),
                    "No further action can be taken on a cancelled invoice.",
                    details
            );
        }

        long allocationCount = allocationRepository.countByInvoiceIdAndIsDeletedFalse(invoice.getId());
        BigDecimal allocatedTotal = allocationRepository.sumAllocatedAmountByInvoiceId(invoice.getId());
        BigDecimal paidAmount = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;

        if (allocationCount > 0 || allocatedTotal.compareTo(BigDecimal.ZERO) > 0 || paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal activePayment = allocatedTotal.compareTo(paidAmount) > 0 ? allocatedTotal : paidAmount;
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' cannot be cancelled because ₹%.2f in customer payments has already been allocated to this invoice.", invoice.getInvoiceNumber(), activePayment));
            details.add("Automatic payment reversal is unsafe and disabled.");

            throw new BusinessValidationException(
                    "Invoice Cannot Be Cancelled",
                    "INVOICE_HAS_PAYMENT_DEPENDENCY",
                    String.format("Invoice '%s' has active payment transactions allocated to it.", invoice.getInvoiceNumber()),
                    "Explicitly reverse or cancel the related customer receipt/payment using the receipt workflow before attempting invoice cancellation.",
                    details
            );
        }
    }

    // =========================================================================
    // CUSTOMER RECEIPT LIFECYCLE & DEPENDENCY VALIDATION
    // =========================================================================

    public void validateReceiptDelete(Long receiptId) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer receipt not found with ID: " + receiptId));
        validateReceiptDelete(receipt);
    }

    public void validateReceiptDelete(CustomerReceipt receipt) {
        if (receipt == null) return;

        tenantAccess.assertOwned(receipt.getCompanyId());
        if (receipt.getBranchId() != null) {
            tenantAccess.assertBranchAccess(receipt.getBranchId());
        }

        String status = receipt.getStatus() != null ? receipt.getStatus().toUpperCase() : "DRAFT";

        if ("APPROVED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' is currently APPROVED and posted to accounting history.", receipt.getReceiptNumber()));

            throw new BusinessValidationException(
                    "Receipt Cannot Be Deleted",
                    "RECEIPT_ALREADY_APPROVED",
                    String.format("Customer Receipt '%s' cannot be deleted because it is already APPROVED.", receipt.getReceiptNumber()),
                    "Approved receipts cannot be deleted or directly edited. Use the explicit receipt cancellation/reversal workflow.",
                    details
            );
        }

        if ("CANCELLED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' is already CANCELLED.", receipt.getReceiptNumber()));

            throw new BusinessValidationException(
                    "Receipt Already Cancelled",
                    "RECEIPT_ALREADY_CANCELLED",
                    String.format("Customer Receipt '%s' is already CANCELLED.", receipt.getReceiptNumber()),
                    "No further action can be taken on a cancelled receipt.",
                    details
            );
        }
    }

    public void validateReceiptUpdate(Long receiptId) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer receipt not found with ID: " + receiptId));
        validateReceiptUpdate(receipt);
    }

    public void validateReceiptUpdate(CustomerReceipt receipt) {
        if (receipt == null) return;

        tenantAccess.assertOwned(receipt.getCompanyId());
        if (receipt.getBranchId() != null) {
            tenantAccess.assertBranchAccess(receipt.getBranchId());
        }

        String status = receipt.getStatus() != null ? receipt.getStatus().toUpperCase() : "DRAFT";

        if (!"DRAFT".equals(status)) {
            String errorCode = "APPROVED".equals(status) ? "RECEIPT_ALREADY_APPROVED" :
                    ("CANCELLED".equals(status) ? "RECEIPT_ALREADY_CANCELLED" : "RECEIPT_STATUS_UPDATE_BLOCKED");

            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' cannot be modified because its current status is %s.", receipt.getReceiptNumber(), status));

            throw new BusinessValidationException(
                    "Receipt Cannot Be Modified",
                    errorCode,
                    String.format("Customer Receipt '%s' cannot be modified because its current status is %s.", receipt.getReceiptNumber(), status),
                    "Only DRAFT receipts can be modified.",
                    details
            );
        }
    }

    public void validateReceiptCancel(Long receiptId) {
        CustomerReceipt receipt = customerReceiptRepository.findById(receiptId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Customer receipt not found with ID: " + receiptId));
        validateReceiptCancel(receipt);
    }

    public void validateReceiptCancel(CustomerReceipt receipt) {
        if (receipt == null) return;

        tenantAccess.assertOwned(receipt.getCompanyId());
        if (receipt.getBranchId() != null) {
            tenantAccess.assertBranchAccess(receipt.getBranchId());
        }

        String status = receipt.getStatus() != null ? receipt.getStatus().toUpperCase() : "DRAFT";

        if ("CANCELLED".equals(status)) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' is already CANCELLED.", receipt.getReceiptNumber()));

            throw new BusinessValidationException(
                    "Receipt Already Cancelled",
                    "RECEIPT_ALREADY_CANCELLED",
                    String.format("Customer Receipt '%s' is already CANCELLED.", receipt.getReceiptNumber()),
                    "No further action can be taken on a cancelled receipt.",
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

        if (receipt.getCustomer() != null && invoice.getCustomer() != null && !receipt.getCustomer().getId().equals(invoice.getCustomer().getId())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Receipt '%s' belongs to customer '%s', but invoice '%s' belongs to customer '%s'.",
                    receipt.getReceiptNumber(), receipt.getCustomer().getName(),
                    invoice.getInvoiceNumber(), invoice.getCustomer().getName()));

            throw new BusinessValidationException(
                    "Customer Mismatch",
                    "ALLOCATION_CUSTOMER_MISMATCH",
                    String.format("Cannot allocate receipt '%s' to invoice '%s' because invoice belongs to a different customer.", receipt.getReceiptNumber(), invoice.getInvoiceNumber()),
                    "Select an invoice belonging to the same customer.",
                    details
            );
        }

        if (receipt.getCompanyId() != null && invoice.getCompanyId() != null && !receipt.getCompanyId().equals(invoice.getCompanyId())) {
            List<String> details = new ArrayList<>();
            details.add("Receipt and invoice belong to different tenant companies.");

            throw new BusinessValidationException(
                    "Company Mismatch",
                    "ALLOCATION_COMPANY_MISMATCH",
                    String.format("Cannot allocate receipt '%s' to invoice '%s' because invoice belongs to a different company.", receipt.getReceiptNumber(), invoice.getInvoiceNumber()),
                    "Select an invoice belonging to the same company.",
                    details
            );
        }

        if (Boolean.TRUE.equals(invoice.getIsDeleted()) || "CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' status is %s.", invoice.getInvoiceNumber(), invoice.getStatus()));

            throw new BusinessValidationException(
                    "Invoice Cancelled Or Deleted",
                    "ALLOCATION_INVOICE_CANCELLED",
                    String.format("Cannot allocate payment to invoice '%s' because the invoice is %s.", invoice.getInvoiceNumber(), invoice.getStatus()),
                    "Payment allocation is only allowed for active APPROVED invoices.",
                    details
            );
        }

        if (!"APPROVED".equalsIgnoreCase(invoice.getStatus())) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Invoice '%s' status is %s.", invoice.getInvoiceNumber(), invoice.getStatus()));

            throw new BusinessValidationException(
                    "Invoice Not Approved",
                    "ALLOCATION_INVOICE_NOT_APPROVED",
                    String.format("Cannot allocate payment to invoice '%s' because its status is %s.", invoice.getInvoiceNumber(), invoice.getStatus()),
                    "Approve the invoice before attempting payment allocation.",
                    details
            );
        }

        BigDecimal outstanding = invoice.getNetAmount().subtract(invoice.getPaidAmount());
        if (allocationAmount.compareTo(outstanding) > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Cannot allocate ₹%.2f to invoice %s because the invoice outstanding amount is ₹%.2f.",
                    allocationAmount, invoice.getInvoiceNumber(), outstanding));

            throw new BusinessValidationException(
                    "Allocation Exceeds Outstanding Balance",
                    "ALLOCATION_EXCEEDS_OUTSTANDING",
                    String.format("Cannot allocate ₹%.2f to invoice %s because the invoice outstanding amount is ₹%.2f.",
                            allocationAmount, invoice.getInvoiceNumber(), outstanding),
                    String.format("Reduce allocation amount to be less than or equal to the invoice outstanding balance of ₹%.2f.", outstanding),
                    details
            );
        }
    }

    public void validateTotalAllocationVsReceived(BigDecimal totalAllocated, BigDecimal amountReceived) {
        if (totalAllocated != null && amountReceived != null && totalAllocated.compareTo(amountReceived) > 0) {
            List<String> details = new ArrayList<>();
            details.add(String.format("Total allocated amount ₹%.2f exceeds receipt amount received ₹%.2f.", totalAllocated, amountReceived));

            throw new BusinessValidationException(
                    "Total Allocation Exceeds Receipt Amount",
                    "ALLOCATION_EXCEEDS_RECEIVED",
                    String.format("Total allocation amount ₹%.2f exceeds the receipt amount received of ₹%.2f.", totalAllocated, amountReceived),
                    String.format("Adjust allocation amounts so total allocations do not exceed receipt amount of ₹%.2f.", amountReceived),
                    details
            );
        }
    }

}
