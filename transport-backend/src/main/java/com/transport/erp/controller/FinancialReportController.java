package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.dto.BalanceSheetReportDTO;
import com.transport.erp.dto.GeneralLedgerReportDTO;
import com.transport.erp.dto.ProfitLossReportDTO;
import com.transport.erp.dto.TrialBalanceDTO;
import com.transport.erp.service.FinancialReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/financial-reports")
@CrossOrigin(origins = "*")
public class FinancialReportController {

    @Autowired
    private FinancialReportService reportService;

    @GetMapping("/trial-balance")
    public ApiResponse<TrialBalanceDTO.Response> getTrialBalance(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        TrialBalanceDTO.Response data = reportService.getTrialBalance(companyId, startDate, endDate);
        return ApiResponse.success(data, "Trial Balance report generated successfully");
    }

    @GetMapping("/general-ledger")
    public ApiResponse<GeneralLedgerReportDTO.Response> getGeneralLedger(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        GeneralLedgerReportDTO.Response data = reportService.getGeneralLedger(accountId, companyId, startDate, endDate);
        return ApiResponse.success(data, "General Ledger report generated successfully");
    }

    @GetMapping("/profit-loss")
    public ApiResponse<ProfitLossReportDTO.Response> getProfitAndLoss(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ProfitLossReportDTO.Response data = reportService.getProfitAndLoss(companyId, startDate, endDate);
        return ApiResponse.success(data, "Profit & Loss report generated successfully");
    }

    @GetMapping("/balance-sheet")
    public ApiResponse<BalanceSheetReportDTO.Response> getBalanceSheet(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        BalanceSheetReportDTO.Response data = reportService.getBalanceSheet(companyId, asOfDate);
        return ApiResponse.success(data, "Balance Sheet report generated successfully");
    }
}
