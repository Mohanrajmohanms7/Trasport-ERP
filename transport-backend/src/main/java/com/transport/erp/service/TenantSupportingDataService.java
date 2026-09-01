package com.transport.erp.service;

import com.transport.erp.model.LookupValue;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.LookupValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds supporting system lookups for a tenant so UI dropdown selectors function properly.
 */
@Service
@RequiredArgsConstructor
public class TenantSupportingDataService {

    private final LookupValueRepository lookupValueRepository;
    private final BranchRepository branchRepository;

    @Transactional
    public Map<String, Object> seedForCompany(Long companyId, Long branchId) {
        Long branch = branchId;
        if (branch == null) {
            branch = branchRepository.findByCompanyIdAndIsDeletedFalse(companyId, org.springframework.data.domain.PageRequest.of(0, 1))
                    .stream().findFirst().map(b -> b.getId()).orElse(null);
        }

        int lookups = seedAllLookups(companyId, branch);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyId", companyId);
        result.put("lookupsAdded", lookups);
        result.put("materialsAdded", 0);
        result.put("quarriesAdded", 0);
        result.put("message", "Supporting system lookups ready for tenant onboarding.");
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
        return added;
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
}

