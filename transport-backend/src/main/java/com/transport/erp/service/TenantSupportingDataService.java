package com.transport.erp.service;

import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds supporting masters for a tenant so ops screens work,
 * without creating customers / vehicles / drivers (user enters those manually).
 */
@Service
@RequiredArgsConstructor
public class TenantSupportingDataService {

    private final LookupValueRepository lookupValueRepository;
    private final MaterialRepository materialRepository;
    private final QuarryRepository quarryRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final FuelEntryRepository fuelEntryRepository;
    private final ExpenseRepository expenseRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public Map<String, Object> seedForCompany(Long companyId, Long branchId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }
        Long branch = branchId != null ? branchId : 1L;

        int lookups = seedAllLookups(companyId, branch);
        int materials = seedSampleMaterials(companyId, branch);
        int quarries = seedSampleQuarry(companyId, branch);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyId", companyId);
        result.put("lookupsAdded", lookups);
        result.put("materialsAdded", materials);
        result.put("quarriesAdded", quarries);
        result.put("message", "Supporting example data ready. Create 1 Customer, 1 Vehicle, 1 Driver manually, then run Booking → Trip → Invoice.");
        return result;
    }

    private int seedAllLookups(Long companyId, Long branchId) {
        int added = 0;
        added += seedType(companyId, branchId, "STATUS", pairs("ACTIVE", "Active", "INACTIVE", "Inactive"));
        added += seedType(companyId, branchId, "BOOKING_STATUS", pairs(
                "DRAFT", "Draft", "PENDING", "Pending", "APPROVED", "Approved",
                "IN_PROGRESS", "In Progress", "COMPLETED", "Completed", "CANCELLED", "Cancelled",
                "ON_HOLD", "On Hold", "REJECTED", "Rejected"));
        added += seedType(companyId, branchId, "TRIP_STATUS", pairs(
                "PLANNED", "Planned", "ALLOCATED", "Allocated", "LOADING", "Loading",
                "LOADED", "Loaded", "DISPATCHED", "Dispatched", "IN_TRANSIT", "In Transit",
                "ARRIVED", "Arrived", "UNLOADING", "Unloading", "DELIVERED", "Delivered",
                "COMPLETED", "Completed", "CANCELLED", "Cancelled"));
        added += seedType(companyId, branchId, "INVOICE_STATUS", pairs(
                "DRAFT", "Draft", "PENDING", "Pending", "APPROVED", "Approved",
                "GENERATED", "Generated", "PAID", "Paid", "CANCELLED", "Cancelled"));
        added += seedType(companyId, branchId, "EXPENSE_STATUS", pairs(
                "DRAFT", "Draft", "SUBMITTED", "Submitted", "APPROVED", "Approved",
                "REJECTED", "Rejected", "PAID", "Paid", "CANCELLED", "Cancelled"));
        added += seedType(companyId, branchId, "FUEL_REQUEST_STATUS", pairs(
                "PENDING", "Pending", "APPROVED", "Approved", "REJECTED", "Rejected"));
        added += seedType(companyId, branchId, "VEHICLE_STATUS", pairs(
                "AVAILABLE", "Available", "ON_TRIP", "On Trip", "UNDER_MAINTENANCE", "Under Maintenance",
                "IDLE", "Idle", "INACTIVE", "Inactive"));
        added += seedType(companyId, branchId, "DRIVER_STATUS", pairs(
                "AVAILABLE", "Available", "ON_TRIP", "On Trip", "ON_LEAVE", "On Leave", "INACTIVE", "Inactive"));
        added += seedType(companyId, branchId, "VEHICLE_TYPE", pairs("TIPPER", "Tipper Lorry", "JCB", "JCB / Excavator"));
        added += seedType(companyId, branchId, "VEHICLE_CATEGORY", pairs("HEAVY", "Heavy Commercial Vehicle", "EQUIPMENT", "Construction Equipment"));
        added += seedType(companyId, branchId, "VEHICLE_CAPACITY", pairs(
                "16_TON", "16 Ton", "18_TON", "18 Ton", "20_TON", "20 Ton", "JCB_STD", "JCB Standard"));
        added += seedType(companyId, branchId, "MATERIAL_CATEGORY", pairs(
                "SAND", "Sand", "AGGREGATE", "Aggregate / Jalli", "DUST", "Crusher Dust", "WMM", "Wet Mix / Gravel"));
        added += seedType(companyId, branchId, "MATERIAL_UNIT", pairs(
                "TON", "Ton", "LOAD", "Load", "CUBIC_METER", "Cubic Meter", "NOS", "Numbers"));
        added += seedType(companyId, branchId, "FUEL_TYPE", pairs(
                "DIESEL", "Diesel", "PETROL", "Petrol", "CNG", "CNG", "ELECTRIC", "Electric"));
        added += seedType(companyId, branchId, "EXPENSE_TYPE", pairs(
                "FUEL", "Fuel", "DRIVER_BATA", "Driver Bata", "TOLL", "Toll", "PARKING", "Parking",
                "VEHICLE_SERVICE", "Vehicle Service", "VEHICLE_REPAIR", "Vehicle Repair", "TYRE", "Tyre",
                "LOADING", "Loading Charges", "INSURANCE", "Insurance", "PERMIT", "Permit",
                "ROAD_TAX", "Road Tax", "OFFICE", "Office Expense", "MISCELLANEOUS", "Miscellaneous"));
        added += seedType(companyId, branchId, "PAYMENT_METHOD", pairs(
                "CASH", "Cash", "UPI", "UPI", "NEFT", "NEFT", "RTGS", "RTGS", "IMPS", "IMPS",
                "BANK_TRANSFER", "Bank Transfer", "CHEQUE", "Cheque", "CREDIT", "Credit"));
        added += seedType(companyId, branchId, "PRIORITY", pairs("HIGH", "High", "MEDIUM", "Medium", "LOW", "Low"));
        added += seedType(companyId, branchId, "PAYMENT_TERMS", pairs(
                "IMMEDIATE", "Due on Receipt", "NET_7", "Net 7 Days", "NET_15", "Net 15 Days", "NET_30", "Net 30 Days"));
        added += seedType(companyId, branchId, "CURRENCY", pairs("INR", "Indian Rupee"));
        added += seedType(companyId, branchId, "GST_RATE", pairs(
                "GST_0", "0%", "GST_5", "5%", "GST_12", "12%", "GST_18", "18%"));
        added += seedType(companyId, branchId, "OWNER_TYPE", pairs(
                "SELF", "Self Owned", "HIRED", "Hired", "CLIENT", "Client Owned"));
        added += seedType(companyId, branchId, "VEHICLE_DOCUMENT_TYPE", pairs(
                "INSURANCE", "Insurance", "PERMIT", "Permit", "FITNESS", "Fitness", "PUC", "PUC", "RC", "RC Book"));
        added += seedType(companyId, branchId, "DRIVER_DOCUMENT_TYPE", pairs(
                "LICENSE_SCAN", "Driving License", "AADHAAR", "Aadhaar", "PAN", "PAN"));
        added += seedType(companyId, branchId, "CUSTOMER_DOCUMENT_TYPE", pairs(
                "GST_CERT", "GST Certificate", "PAN_CARD", "PAN Card"));
        added += seedType(companyId, branchId, "MAINTENANCE_TYPE", pairs(
                "OIL_CHANGE", "Oil Change", "ENGINE_SERVICE", "Engine Service",
                "TYRE_CHANGE", "Tyre Change", "BRAKE_SERVICE", "Brake Service", "GENERAL_SERVICE", "General Service"));
        added += seedType(companyId, branchId, "ATTENDANCE_STATUS", pairs(
                "PRESENT", "Present", "ABSENT", "Absent", "LEAVE", "Leave", "HALF_DAY", "Half Day"));
        added += seedType(companyId, branchId, "ACCOUNT_TYPE", pairs(
                "ASSET", "Asset", "LIABILITY", "Liability", "EQUITY", "Equity", "INCOME", "Income", "EXPENSE", "Expense"));
        added += seedType(companyId, branchId, "DESTINATION_CITY", pairs(
                "PERAMBALUR", "Perambalur", "ARIYALUR", "Ariyalur", "TRICHY", "Trichy"));
        return added;
    }

    private int seedSampleMaterials(Long companyId, Long branchId) {
        LookupValue sand = requireLookup(companyId, "MATERIAL_CATEGORY", "SAND");
        LookupValue aggregate = requireLookup(companyId, "MATERIAL_CATEGORY", "AGGREGATE");
        LookupValue ton = requireLookup(companyId, "MATERIAL_UNIT", "TON");

        int added = 0;
        added += upsertMaterial(companyId, branchId, "MAT000001", "M Sand", "Construction sand — example rate", sand, ton, "850.00");
        added += upsertMaterial(companyId, branchId, "MAT000002", "20 MM Jalli", "Aggregate — example rate", aggregate, ton, "920.00");
        added += upsertMaterial(companyId, branchId, "MAT000003", "P Sand", "Plastering sand — example rate", sand, ton, "780.00");
        return added;
    }

    private int seedSampleQuarry(Long companyId, Long branchId) {
        if (quarryRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, "QRY000001").isPresent()) {
            return 0;
        }
        Quarry q = new Quarry();
        q.setCode("QRY000001");
        q.setName("Example Quarry");
        q.setDescription("Sample quarry for booking / trip flow testing");
        q.setLocationAddress("Thannirpandhal, Perambalur, Tamil Nadu");
        q.setOwnerName("Example Quarry Owner");
        q.setContactNumber("9876500001");
        q.setStatus("ACTIVE");
        q.setCompanyId(companyId);
        q.setBranchId(branchId);
        q.setIsDeleted(false);
        q.setCreatedBy("SYSTEM");
        quarryRepository.save(q);
        return 1;
    }

    private int upsertMaterial(Long companyId, Long branchId, String code, String name, String description,
                               LookupValue category, LookupValue unit, String rate) {
        if (materialRepository.findByCompanyIdAndCodeAndIsDeletedFalse(companyId, code).isPresent()) {
            return 0;
        }
        Material m = new Material();
        m.setCode(code);
        m.setName(name);
        m.setDescription(description);
        m.setCategory(category);
        m.setUnit(unit);
        m.setDefaultRate(new BigDecimal(rate));
        m.setDensity(new BigDecimal("1.500"));
        m.setStatus("ACTIVE");
        m.setCompanyId(companyId);
        m.setBranchId(branchId);
        m.setIsDeleted(false);
        m.setCreatedBy("SYSTEM");
        materialRepository.save(m);
        return 1;
    }

    private LookupValue requireLookup(Long companyId, String type, String code) {
        return lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(companyId, type, code)
                .orElseThrow(() -> new IllegalStateException("Missing lookup " + type + "/" + code + " for company " + companyId));
    }

    private int seedType(Long companyId, Long branchId, String type, String[][] values) {
        int added = 0;
        for (String[] value : values) {
            String code = value[0];
            if (lookupValueRepository.findByCompanyIdAndTypeAndCodeAndIsDeletedFalse(companyId, type, code).isPresent()) {
                continue;
            }
            LookupValue lookup = new LookupValue();
            lookup.setType(type);
            lookup.setCode(code);
            lookup.setName(value[1]);
            lookup.setDescription(value[1]);
            lookup.setStatus("ACTIVE");
            lookup.setCompanyId(companyId);
            lookup.setBranchId(branchId);
            lookup.setIsDeleted(false);
            lookup.setCreatedBy("SYSTEM");
            lookupValueRepository.save(lookup);
            added++;
        }
        return added;
    }

    private static String[][] pairs(String... items) {
        if (items.length % 2 != 0) {
            throw new IllegalArgumentException("pairs requires even number of args");
        }
        String[][] out = new String[items.length / 2][2];
        for (int i = 0; i < items.length; i += 2) {
            out[i / 2][0] = items[i];
            out[i / 2][1] = items[i + 1];
        }
        return out;
    }

    @Transactional
    public Map<String, Object> seedFullDemoData(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }

        Branch branch = branchRepository.findAll().stream()
                .filter(b -> b.getCompanyId().equals(companyId) && "ACTIVE".equals(b.getStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Default branch not found for company " + companyId));
        
        Long branchId = branch.getId();
        LocalDate today = LocalDate.now();

        long existingDrivers = driverRepository.findAll().stream().filter(d -> d.getCompanyId().equals(companyId)).count();
        if (existingDrivers > 0) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("success", true);
            map.put("message", "Demo data already exists for company " + companyId);
            return map;
        }

        List<Driver> drivers = new ArrayList<>();
        String[] driverNames = {"Ramesh Kumar", "Suresh Kumar", "Anand Raj", "Karthik Raja", "Vijay Singh"};
        for (int i = 0; i < driverNames.length; i++) {
            Driver d = new Driver();
            d.setCode("DRV" + String.format("%05d", i + 1));
            d.setName(driverNames[i]);
            d.setLicenseNumber("DL-45" + String.format("%010d", 1000 + i));
            d.setLicenseExpiryDate(today.plusYears(3));
            d.setPhoneNumber("987654321" + i);
            d.setStatus("AVAILABLE");
            d.setCompanyId(companyId);
            d.setBranchId(branchId);
            drivers.add(driverRepository.save(d));
        }

        List<Vehicle> vehicles = new ArrayList<>();
        String[] plates = {"TN-45-AT-1234", "TN-45-AT-5678", "TN-45-AT-9012", "TN-45-AT-3456", "TN-45-AT-7890"};
        LookupValue tipperType = requireLookup(companyId, "VEHICLE_TYPE", "TIPPER");
        LookupValue heavyCat = requireLookup(companyId, "VEHICLE_CATEGORY", "HEAVY");
        LookupValue tonCapacity = requireLookup(companyId, "VEHICLE_CAPACITY", "16_TON");
        for (int i = 0; i < plates.length; i++) {
            Vehicle v = new Vehicle();
            v.setCode("VEH" + String.format("%05d", i + 1));
            v.setName(plates[i]);
            v.setChassisNumber("CHA-TN45-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            v.setEngineNumber("ENG-TN45-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            v.setModel("1613 Tipper");
            v.setBrand("Tata Motors");
            v.setType(tipperType);
            v.setCategory(heavyCat);
            v.setCapacity(tonCapacity);
            v.setOwnerName("Self owned");
            v.setOwnerType("SELF");
            v.setPurchaseDate(today.minusYears(1));
            v.setInsuranceExpiryDate(today.plusMonths(6));
            v.setFitnessExpiryDate(today.plusMonths(9));
            v.setPermitExpiryDate(today.plusMonths(12));
            v.setStatus("AVAILABLE");
            v.setCompanyId(companyId);
            v.setBranchId(branchId);
            vehicles.add(vehicleRepository.save(v));
        }

        List<Customer> customers = new ArrayList<>();
        String[] customerNames = {"Adani Infrastructure Ltd", "L&T Construction Div", "TATA Projects Ltd"};
        for (int i = 0; i < customerNames.length; i++) {
            Customer c = new Customer();
            c.setCode("CUST" + String.format("%04d", i + 1));
            c.setName(customerNames[i]);
            c.setEmail("contact@corp" + i + ".com");
            c.setPhone("998877665" + i);
            c.setAddress("Industrial Zone, Sector " + (i + 1) + ", Chennai");
            c.setGstNumber("33AAAAA1111A1Z" + i);
            c.setCreditLimit(new BigDecimal("1000000.00"));
            c.setStatus("ACTIVE");
            c.setCompanyId(companyId);
            c.setBranchId(branchId);
            customers.add(customerRepository.save(c));
        }

        Material mSand = materialRepository.findAll().stream()
                .filter(m -> m.getCompanyId().equals(companyId) && "MAT000001".equals(m.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Material MAT000001 not found"));

        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < customers.size(); i++) {
            Booking b = new Booking();
            b.setCode("BKG-" + today.getYear() + "-" + String.format("%04d", i + 1));
            b.setName("Order Booking for " + customers.get(i).getName());
            b.setBookingNumber("BKG-" + today.getYear() + "-" + String.format("%04d", i + 1));
            b.setBookingDate(today.minusDays(5 + i));
            b.setCustomer(customers.get(i));
            b.setStatus("APPROVED");
            b.setPriority("MEDIUM");
            b.setRemarks("Bulk delivery order seeded automatically");
            b.setCompanyId(companyId);
            b.setBranchId(branchId);

            BookingDetail detail1 = new BookingDetail();
            detail1.setCode("BKG-DTL-" + i + "-1");
            detail1.setName("Booking Detail 1");
            detail1.setBooking(b);
            detail1.setMaterial(mSand);
            detail1.setQuantity(new BigDecimal("150.00"));
            detail1.setRate(new BigDecimal("850.00"));
            detail1.setTransportRate(new BigDecimal("150.00"));
            detail1.setRoyaltyRate(new BigDecimal("50.00"));
            detail1.setLoadingCharge(new BigDecimal("20.00"));
            detail1.setGstPercentage(new BigDecimal("18.00"));
            detail1.setNetAmount(new BigDecimal("150.00").multiply(new BigDecimal("1070.00")));
            detail1.setCompanyId(companyId);
            detail1.setBranchId(branchId);

            b.getDetails().add(detail1);
            bookings.add(bookingRepository.save(b));
        }

        List<Trip> trips = new ArrayList<>();
        for (int i = 0; i < bookings.size(); i++) {
            Trip t = new Trip();
            t.setCode("TRP-" + today.getYear() + "-" + String.format("%04d", i + 1));
            t.setName("Transit Trip " + (i + 1));
            t.setTripNumber("TRP-" + today.getYear() + "-" + String.format("%04d", i + 1));
            t.setTripDate(today.minusDays(3 + i));
            t.setBooking(bookings.get(i));
            t.setVehicle(vehicles.get(i));
            t.setDriver(drivers.get(i));
            t.setStatus("COMPLETED");
            t.setRemarks("Automated transit logged cleanly");
            t.setCompanyId(companyId);
            t.setBranchId(branchId);

            TripDetail detail = new TripDetail();
            detail.setCode("TRP-DTL-" + i);
            detail.setName("Trip Item Details");
            detail.setTrip(t);
            detail.setMaterial(mSand);
            detail.setQuantity(new BigDecimal("16.00"));
            detail.setRate(new BigDecimal("850.00"));
            detail.setLoadingCharges(new BigDecimal("320.00"));
            detail.setRoyalty(new BigDecimal("800.00"));
            detail.setDispatchTime(today.minusDays(3 + i).atTime(9, 0));
            detail.setArrivalTime(today.minusDays(3 + i).atTime(13, 0));
            detail.setCompanyId(companyId);
            detail.setBranchId(branchId);

            t.getDetails().add(detail);
            trips.add(tripRepository.save(t));
        }

        for (int i = 0; i < trips.size(); i++) {
            FuelEntry fe = new FuelEntry();
            fe.setCode("FUEL-" + today.getYear() + "-" + String.format("%04d", i + 1));
            fe.setName("Fuel Refill for " + vehicles.get(i).getName());
            fe.setFuelEntryNumber("FL-" + today.getYear() + "-" + String.format("%04d", i + 1));
            fe.setFuelDate(today.minusDays(3 + i));
            fe.setVehicle(vehicles.get(i));
            fe.setDriver(drivers.get(i));
            fe.setTrip(trips.get(i));
            fe.setFuelStation("CESS Service Station");
            fe.setFuelQuantity(new BigDecimal("45.00"));
            fe.setRatePerLitre(new BigDecimal("98.50"));
            fe.setTotalAmount(new BigDecimal("4432.50"));
            fe.setPaymentMethod("UPI");
            fe.setInvoiceNumber("FE-INV-" + (1000 + i));
            fe.setCurrentOdometer(new BigDecimal("12045.00").add(new BigDecimal(String.valueOf(i * 100))));
            fe.setPreviousOdometer(new BigDecimal("11780.00").add(new BigDecimal(String.valueOf(i * 100))));
            fe.setRemarks("Pre-seeded fuel log");
            fe.setCompanyId(companyId);
            fe.setBranchId(branchId);
            fuelEntryRepository.save(fe);
        }

        for (int i = 0; i < trips.size(); i++) {
            Expense exp = new Expense();
            exp.setCode("EXP-" + today.getYear() + "-" + String.format("%04d", i + 1));
            exp.setName("Driver Bata & Tolls for Trip " + trips.get(i).getTripNumber());
            exp.setExpenseNumber("EX-" + today.getYear() + "-" + String.format("%04d", i + 1));
            exp.setExpenseDate(today.minusDays(3 + i));
            exp.setCategory("DRIVER_BATA");
            exp.setVehicle(vehicles.get(i));
            exp.setDriver(drivers.get(i));
            exp.setTrip(trips.get(i));
            exp.setDescription("Seeded trip allowance and miscellaneous tolls");
            exp.setAmount(new BigDecimal("500.00"));
            exp.setGstAmount(BigDecimal.ZERO);
            exp.setTotalAmount(new BigDecimal("500.00"));
            exp.setPaymentMethod("CASH");
            exp.setStatus("PAID");
            exp.setCompanyId(companyId);
            exp.setBranchId(branchId);
            expenseRepository.save(exp);
        }

        for (int i = 0; i < bookings.size(); i++) {
            SalesInvoice si = new SalesInvoice();
            si.setCode("INV-" + today.getYear() + "-" + String.format("%04d", i + 1));
            si.setName("Tax Invoice for " + customers.get(i).getName());
            si.setInvoiceNumber("INV-" + today.getYear() + "-" + String.format("%04d", i + 1));
            si.setInvoiceDate(today.minusDays(2 + i));
            si.setCustomer(customers.get(i));
            si.setStatus("GENERATED");
            si.setPaymentTerms("NET_15");
            si.setSubtotal(new BigDecimal("16000.00"));
            si.setDiscount(BigDecimal.ZERO);
            si.setNetAmount(new BigDecimal("18880.00"));
            si.setCompanyId(companyId);
            si.setBranchId(branchId);
            salesInvoiceRepository.save(si);

            CustomerLedger ledger = new CustomerLedger();
            ledger.setCode("LDG-INV-" + i);
            ledger.setName("Invoice Debit " + si.getInvoiceNumber());
            ledger.setCustomer(customers.get(i));
            ledger.setDebitAmount(si.getNetAmount());
            ledger.setCreditAmount(BigDecimal.ZERO);
            ledger.setRunningBalance(si.getNetAmount());
            ledger.setRemarks("Debit against Sales Invoice " + si.getInvoiceNumber());
            ledger.setCompanyId(companyId);
            ledger.setBranchId(branchId);
            customerLedgerRepository.save(ledger);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", true);
        map.put("message", "Full Flow Seeding complete: Created 5 Drivers, 5 Vehicles, 3 Customers, 3 Bookings, 3 Trips, 3 Fuel records, 3 Expenses, and 3 Invoices!");
        map.put("companyId", companyId);
        map.put("driversSeeded", drivers.size());
        map.put("vehiclesSeeded", vehicles.size());
        map.put("customersSeeded", customers.size());
        map.put("tripsSeeded", trips.size());

        return map;
    }
}
