package com.transport.erp.controller;

import com.transport.erp.dto.*;
import com.transport.erp.service.FinancialReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/financial-reports")
@CrossOrigin(origins = "*")
public class FinancialReportController {

    @Autowired
    private FinancialReportService reportService;

    // ==========================================
    // 1. TRIAL BALANCE ENDPOINTS
    // ==========================================
    @GetMapping("/trial-balance")
    public ApiResponse<TrialBalanceDTO.Response> getTrialBalance(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        TrialBalanceDTO.Response data = reportService.getTrialBalance(companyId, startDate, endDate);
        return ApiResponse.success(data, "Trial Balance report generated successfully");
    }

    @GetMapping("/trial-balance/print")
    public ApiResponse<FinancialReportPrintDTO<TrialBalanceDTO.Response>> getTrialBalancePrintData(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FinancialReportPrintDTO<TrialBalanceDTO.Response> data = reportService.getTrialBalancePrintData(companyId, startDate, endDate);
        return ApiResponse.success(data, "Trial Balance print data fetched successfully");
    }

    @GetMapping("/trial-balance/pdf")
    public ResponseEntity<byte[]> getTrialBalancePdf(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateTrialBalancePdf(companyId, startDate, endDate, baos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Trial_Balance_Report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/trial-balance/csv")
    public ResponseEntity<String> getTrialBalanceCsv(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String csvData = reportService.generateTrialBalanceCsv(companyId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Trial_Balance_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    // ==========================================
    // 2. GENERAL LEDGER ENDPOINTS
    // ==========================================
    @GetMapping("/general-ledger")
    public ApiResponse<GeneralLedgerReportDTO.Response> getGeneralLedger(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        GeneralLedgerReportDTO.Response data = reportService.getGeneralLedger(accountId, companyId, startDate, endDate);
        return ApiResponse.success(data, "General Ledger report generated successfully");
    }

    @GetMapping("/general-ledger/print")
    public ApiResponse<FinancialReportPrintDTO<GeneralLedgerReportDTO.Response>> getGeneralLedgerPrintData(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FinancialReportPrintDTO<GeneralLedgerReportDTO.Response> data = reportService.getGeneralLedgerPrintData(accountId, companyId, startDate, endDate);
        return ApiResponse.success(data, "General Ledger print data fetched successfully");
    }

    @GetMapping("/general-ledger/pdf")
    public ResponseEntity<byte[]> getGeneralLedgerPdf(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateGeneralLedgerPdf(accountId, companyId, startDate, endDate, baos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"General_Ledger_Report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/general-ledger/csv")
    public ResponseEntity<String> getGeneralLedgerCsv(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String csvData = reportService.generateGeneralLedgerCsv(accountId, companyId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"General_Ledger_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    // ==========================================
    // 3. PROFIT & LOSS ENDPOINTS
    // ==========================================
    @GetMapping("/profit-loss")
    public ApiResponse<ProfitLossReportDTO.Response> getProfitAndLoss(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ProfitLossReportDTO.Response data = reportService.getProfitAndLoss(companyId, startDate, endDate);
        return ApiResponse.success(data, "Profit & Loss report generated successfully");
    }

    @GetMapping("/profit-loss/print")
    public ApiResponse<FinancialReportPrintDTO<ProfitLossReportDTO.Response>> getProfitLossPrintData(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        FinancialReportPrintDTO<ProfitLossReportDTO.Response> data = reportService.getProfitLossPrintData(companyId, startDate, endDate);
        return ApiResponse.success(data, "Profit & Loss print data fetched successfully");
    }

    @GetMapping("/profit-loss/pdf")
    public ResponseEntity<byte[]> getProfitLossPdf(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateProfitLossPdf(companyId, startDate, endDate, baos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Profit_Loss_Report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/profit-loss/csv")
    public ResponseEntity<String> getProfitLossCsv(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String csvData = reportService.generateProfitLossCsv(companyId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Profit_Loss_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    // ==========================================
    // 4. BALANCE SHEET ENDPOINTS
    // ==========================================
    @GetMapping("/balance-sheet")
    public ApiResponse<BalanceSheetReportDTO.Response> getBalanceSheet(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        BalanceSheetReportDTO.Response data = reportService.getBalanceSheet(companyId, asOfDate);
        return ApiResponse.success(data, "Balance Sheet report generated successfully");
    }

    @GetMapping("/balance-sheet/print")
    public ApiResponse<FinancialReportPrintDTO<BalanceSheetReportDTO.Response>> getBalanceSheetPrintData(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        FinancialReportPrintDTO<BalanceSheetReportDTO.Response> data = reportService.getBalanceSheetPrintData(companyId, asOfDate);
        return ApiResponse.success(data, "Balance Sheet print data fetched successfully");
    }

    @GetMapping("/balance-sheet/pdf")
    public ResponseEntity<byte[]> getBalanceSheetPdf(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reportService.generateBalanceSheetPdf(companyId, asOfDate, baos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Balance_Sheet_Report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    @GetMapping("/balance-sheet/csv")
    public ResponseEntity<String> getBalanceSheetCsv(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        String csvData = reportService.generateBalanceSheetCsv(companyId, asOfDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Balance_Sheet_Report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
