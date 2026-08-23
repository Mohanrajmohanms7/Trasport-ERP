package com.transport.erp.service;

import com.transport.erp.model.Expense;
import com.transport.erp.model.FuelEntry;
import com.transport.erp.model.ReportTemplate;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.Trip;
import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.ExpenseRepository;
import com.transport.erp.repository.FuelEntryRepository;
import com.transport.erp.repository.SalesInvoiceRepository;
import com.transport.erp.repository.TripRepository;
import com.transport.erp.repository.VehicleRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    @Autowired private TenantAccessService tenantAccess;
    @Autowired private ReportTemplateService templateService;
    @Autowired private SalesInvoiceRepository salesInvoiceRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private FuelEntryRepository fuelEntryRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Map<String, String> exportTemplate(Long templateId) {
        ReportTemplate template = templateService.getTemplateById(templateId);
        tenantAccess.assertCompanyAccess(template.getCompanyId());
        Long companyId = template.getCompanyId();

        String reportType = template.getReportType() != null ? template.getReportType().toUpperCase() : "REVENUE";
        String csv = switch (reportType) {
            case "EXPENSE" -> exportExpenses(companyId);
            case "FUEL" -> exportFuel(companyId);
            case "TRIP" -> exportTrips(companyId);
            case "FLEET" -> exportFleet(companyId);
            default -> exportRevenue(companyId);
        };

        String safeName = (template.getTemplateName() != null ? template.getTemplateName() : "report")
                .replaceAll("[^a-zA-Z0-9-_ ]", "")
                .trim()
                .replace(' ', '_');

        Map<String, String> result = new HashMap<>();
        result.put("fileName", safeName + ".csv");
        result.put("mimeType", "text/csv");
        result.put("contentBase64", Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)));
        result.put("status", "GENERATED");
        return result;
    }

    private String exportRevenue(Long companyId) {
        List<SalesInvoice> rows = salesInvoiceRepository
                .findByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 500))
                .getContent();
        StringBuilder sb = new StringBuilder("invoiceNumber,invoiceDate,status,customer,netAmount\n");
        for (SalesInvoice i : rows) {
            sb.append(csv(i.getInvoiceNumber())).append(',')
                    .append(csv(String.valueOf(i.getInvoiceDate()))).append(',')
                    .append(csv(i.getStatus())).append(',')
                    .append(csv(i.getCustomer() != null ? i.getCustomer().getName() : "")).append(',')
                    .append(amount(i.getNetAmount())).append('\n');
        }
        return sb.toString();
    }

    private String exportExpenses(Long companyId) {
        List<Expense> rows = expenseRepository
                .findByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 500))
                .getContent();
        StringBuilder sb = new StringBuilder("expenseNumber,expenseDate,category,status,totalAmount\n");
        for (Expense e : rows) {
            sb.append(csv(e.getExpenseNumber())).append(',')
                    .append(csv(String.valueOf(e.getExpenseDate()))).append(',')
                    .append(csv(e.getCategory())).append(',')
                    .append(csv(e.getStatus())).append(',')
                    .append(amount(e.getTotalAmount())).append('\n');
        }
        return sb.toString();
    }

    private String exportFuel(Long companyId) {
        List<FuelEntry> rows = fuelEntryRepository
                .findByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 500))
                .getContent();
        StringBuilder sb = new StringBuilder("fuelEntryNumber,fuelDate,vehicle,litres,totalAmount\n");
        for (FuelEntry f : rows) {
            sb.append(csv(f.getFuelEntryNumber())).append(',')
                    .append(csv(String.valueOf(f.getFuelDate()))).append(',')
                    .append(csv(f.getVehicle() != null ? f.getVehicle().getName() : "")).append(',')
                    .append(amount(f.getFuelQuantity())).append(',')
                    .append(amount(f.getTotalAmount())).append('\n');
        }
        return sb.toString();
    }

    private String exportTrips(Long companyId) {
        List<Trip> rows = tripRepository
                .findByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 500))
                .getContent();
        StringBuilder sb = new StringBuilder("tripNumber,tripDate,status,vehicle,driver\n");
        for (Trip t : rows) {
            sb.append(csv(t.getTripNumber())).append(',')
                    .append(csv(String.valueOf(t.getTripDate()))).append(',')
                    .append(csv(t.getStatus())).append(',')
                    .append(csv(t.getVehicle() != null ? t.getVehicle().getName() : "")).append(',')
                    .append(csv(t.getDriver() != null ? t.getDriver().getName() : "")).append('\n');
        }
        return sb.toString();
    }

    private String exportFleet(Long companyId) {
        List<Vehicle> rows = vehicleRepository
                .findByCompanyIdAndIsDeletedFalse(companyId, PageRequest.of(0, 500))
                .getContent();
        StringBuilder sb = new StringBuilder("code,name,status,insuranceExpiry,permitExpiry\n");
        for (Vehicle v : rows) {
            sb.append(csv(v.getCode())).append(',')
                    .append(csv(v.getName())).append(',')
                    .append(csv(v.getStatus())).append(',')
                    .append(csv(String.valueOf(v.getInsuranceExpiryDate()))).append(',')
                    .append(csv(String.valueOf(v.getPermitExpiryDate()))).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null || "null".equals(value)) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String amount(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }
}
