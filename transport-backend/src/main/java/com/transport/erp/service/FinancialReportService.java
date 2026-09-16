package com.transport.erp.service;

import com.transport.erp.dto.*;
import com.transport.erp.model.*;
import com.transport.erp.repository.BranchRepository;
import com.transport.erp.repository.ChartOfAccountRepository;
import com.transport.erp.repository.CompanyRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import com.transport.erp.util.FinancialReportPdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class FinancialReportService {

    @Autowired
    private JournalVoucherRepository jvRepository;

    @Autowired
    private ChartOfAccountRepository coaRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TenantAccessService tenantAccess;

    @Transactional(readOnly = true)
    public TrialBalanceDTO.Response getTrialBalance(Long companyId, LocalDate startDate, LocalDate endDate) {
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        if (startDate == null) startDate = YearMonth.now().atDay(1);
        if (endDate == null) endDate = LocalDate.now();

        List<ChartOfAccount> accounts = coaRepository.findByCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(targetCompanyId);
        List<TrialBalanceDTO.Row> rows = new ArrayList<>();

        BigDecimal totalOpening = BigDecimal.ZERO;
        BigDecimal totalPeriodDebit = BigDecimal.ZERO;
        BigDecimal totalPeriodCredit = BigDecimal.ZERO;
        BigDecimal totalClosingDebit = BigDecimal.ZERO;
        BigDecimal totalClosingCredit = BigDecimal.ZERO;

        for (ChartOfAccount acc : accounts) {
            BigDecimal preDebit = jvRepository.sumDebitByAccountAndCompanyBeforeDate(acc.getId(), targetCompanyId, startDate);
            BigDecimal preCredit = jvRepository.sumCreditByAccountAndCompanyBeforeDate(acc.getId(), targetCompanyId, startDate);

            boolean isDebitType = "ASSET".equalsIgnoreCase(acc.getAccountType()) || "EXPENSE".equalsIgnoreCase(acc.getAccountType());
            BigDecimal openingNet = isDebitType 
                    ? acc.getOpeningBalance().add(preDebit).subtract(preCredit)
                    : acc.getOpeningBalance().add(preCredit).subtract(preDebit);

            BigDecimal pDebit = jvRepository.sumDebitByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);
            BigDecimal pCredit = jvRepository.sumCreditByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);

            BigDecimal closingDebit = BigDecimal.ZERO;
            BigDecimal closingCredit = BigDecimal.ZERO;

            if (isDebitType) {
                BigDecimal netClosing = openingNet.add(pDebit).subtract(pCredit);
                if (netClosing.compareTo(BigDecimal.ZERO) >= 0) {
                    closingDebit = netClosing;
                } else {
                    closingCredit = netClosing.abs();
                }
            } else {
                BigDecimal netClosing = openingNet.add(pCredit).subtract(pDebit);
                if (netClosing.compareTo(BigDecimal.ZERO) >= 0) {
                    closingCredit = netClosing;
                } else {
                    closingDebit = netClosing.abs();
                }
            }

            BigDecimal netClosingBalance = isDebitType ? closingDebit.subtract(closingCredit) : closingCredit.subtract(closingDebit);

            TrialBalanceDTO.Row row = TrialBalanceDTO.Row.builder()
                    .accountId(acc.getId())
                    .accountCode(acc.getAccountCode())
                    .accountName(acc.getAccountName())
                    .accountType(acc.getAccountType())
                    .openingBalance(openingNet)
                    .periodDebit(pDebit)
                    .periodCredit(pCredit)
                    .closingDebit(closingDebit)
                    .closingCredit(closingCredit)
                    .closingBalance(netClosingBalance)
                    .build();

            rows.add(row);

            totalOpening = totalOpening.add(openingNet);
            totalPeriodDebit = totalPeriodDebit.add(pDebit);
            totalPeriodCredit = totalPeriodCredit.add(pCredit);
            totalClosingDebit = totalClosingDebit.add(closingDebit);
            totalClosingCredit = totalClosingCredit.add(closingCredit);
        }

        BigDecimal difference = totalClosingDebit.subtract(totalClosingCredit);

        return TrialBalanceDTO.Response.builder()
                .startDate(startDate)
                .endDate(endDate)
                .rows(rows)
                .totalOpeningBalance(totalOpening)
                .totalPeriodDebit(totalPeriodDebit)
                .totalPeriodCredit(totalPeriodCredit)
                .totalClosingDebit(totalClosingDebit)
                .totalClosingCredit(totalClosingCredit)
                .difference(difference)
                .build();
    }

    @Transactional(readOnly = true)
    public GeneralLedgerReportDTO.Response getGeneralLedger(Long accountId, Long companyId, LocalDate startDate, LocalDate endDate) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required for General Ledger report");
        }

        ChartOfAccount account = coaRepository.findById(accountId)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Chart of Account not found: " + accountId));

        tenantAccess.assertCompanyAccess(account.getCompanyId());
        Long targetCompanyId = account.getCompanyId();

        if (startDate == null) startDate = YearMonth.now().atDay(1);
        if (endDate == null) endDate = LocalDate.now();

        BigDecimal preDebit = jvRepository.sumDebitByAccountAndCompanyBeforeDate(accountId, targetCompanyId, startDate);
        BigDecimal preCredit = jvRepository.sumCreditByAccountAndCompanyBeforeDate(accountId, targetCompanyId, startDate);

        boolean isDebitType = "ASSET".equalsIgnoreCase(account.getAccountType()) || "EXPENSE".equalsIgnoreCase(account.getAccountType());
        BigDecimal openingBalance = isDebitType 
                ? account.getOpeningBalance().add(preDebit).subtract(preCredit)
                : account.getOpeningBalance().add(preCredit).subtract(preDebit);

        List<JournalVoucher> jvs = jvRepository.findGeneralLedgerEntries(accountId, targetCompanyId, startDate, endDate);
        List<GeneralLedgerReportDTO.Entry> entries = new ArrayList<>();

        BigDecimal currentBalance = openingBalance;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalVoucher jv : jvs) {
            BigDecimal dr = BigDecimal.ZERO;
            BigDecimal cr = BigDecimal.ZERO;

            if (jv.getDebitAccount().getId().equals(accountId)) {
                dr = jv.getAmount();
            }
            if (jv.getCreditAccount().getId().equals(accountId)) {
                cr = jv.getAmount();
            }

            if (isDebitType) {
                currentBalance = currentBalance.add(dr).subtract(cr);
            } else {
                currentBalance = currentBalance.add(cr).subtract(dr);
            }

            totalDebit = totalDebit.add(dr);
            totalCredit = totalCredit.add(cr);

            entries.add(GeneralLedgerReportDTO.Entry.builder()
                    .jvId(jv.getId())
                    .voucherDate(jv.getVoucherDate())
                    .voucherNumber(jv.getVoucherNumber())
                    .referenceNumber(jv.getReferenceNumber())
                    .description(jv.getDescription())
                    .debit(dr)
                    .credit(cr)
                    .runningBalance(currentBalance)
                    .build());
        }

        return GeneralLedgerReportDTO.Response.builder()
                .accountId(account.getId())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .startDate(startDate)
                .endDate(endDate)
                .openingBalance(openingBalance)
                .closingBalance(currentBalance)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .entries(entries)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfitLossReportDTO.Response getProfitAndLoss(Long companyId, LocalDate startDate, LocalDate endDate) {
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        if (startDate == null) startDate = YearMonth.now().atDay(1);
        if (endDate == null) endDate = LocalDate.now();

        List<ChartOfAccount> accounts = coaRepository.findByCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(targetCompanyId);
        List<ProfitLossReportDTO.Row> incomeRows = new ArrayList<>();
        List<ProfitLossReportDTO.Row> expenseRows = new ArrayList<>();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (ChartOfAccount acc : accounts) {
            String type = acc.getAccountType() != null ? acc.getAccountType().toUpperCase() : "";
            if ("INCOME".equals(type) || "REVENUE".equals(type)) {
                BigDecimal pCredit = jvRepository.sumCreditByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);
                BigDecimal pDebit = jvRepository.sumDebitByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);
                BigDecimal netInc = pCredit.subtract(pDebit);

                incomeRows.add(ProfitLossReportDTO.Row.builder()
                        .accountId(acc.getId())
                        .accountCode(acc.getAccountCode())
                        .accountName(acc.getAccountName())
                        .accountType(acc.getAccountType())
                        .amount(netInc)
                        .build());
                totalIncome = totalIncome.add(netInc);
            } else if ("EXPENSE".equals(type)) {
                BigDecimal pDebit = jvRepository.sumDebitByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);
                BigDecimal pCredit = jvRepository.sumCreditByAccountAndCompanyAndDateRange(acc.getId(), targetCompanyId, startDate, endDate);
                BigDecimal netExp = pDebit.subtract(pCredit);

                expenseRows.add(ProfitLossReportDTO.Row.builder()
                        .accountId(acc.getId())
                        .accountCode(acc.getAccountCode())
                        .accountName(acc.getAccountName())
                        .accountType(acc.getAccountType())
                        .amount(netExp)
                        .build());
                totalExpense = totalExpense.add(netExp);
            }
        }

        BigDecimal netProfit = BigDecimal.ZERO;
        BigDecimal netLoss = BigDecimal.ZERO;
        boolean isProfitable = totalIncome.compareTo(totalExpense) >= 0;

        if (isProfitable) {
            netProfit = totalIncome.subtract(totalExpense);
        } else {
            netLoss = totalExpense.subtract(totalIncome);
        }

        return ProfitLossReportDTO.Response.builder()
                .startDate(startDate)
                .endDate(endDate)
                .incomeRows(incomeRows)
                .expenseRows(expenseRows)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .netLoss(netLoss)
                .isProfitable(isProfitable)
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceSheetReportDTO.Response getBalanceSheet(Long companyId, LocalDate asOfDate) {
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        if (asOfDate == null) asOfDate = LocalDate.now();

        List<ChartOfAccount> accounts = coaRepository.findByCompanyIdAndIsDeletedFalseOrderByAccountCodeAsc(targetCompanyId);
        List<BalanceSheetReportDTO.Row> assetRows = new ArrayList<>();
        List<BalanceSheetReportDTO.Row> liabilityRows = new ArrayList<>();
        List<BalanceSheetReportDTO.Row> equityRows = new ArrayList<>();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        BigDecimal allIncome = BigDecimal.ZERO;
        BigDecimal allExpense = BigDecimal.ZERO;

        for (ChartOfAccount acc : accounts) {
            String type = acc.getAccountType() != null ? acc.getAccountType().toUpperCase() : "";
            BigDecimal pDebit = jvRepository.sumDebitByAccountAndCompanyOnOrBeforeDate(acc.getId(), targetCompanyId, asOfDate);
            BigDecimal pCredit = jvRepository.sumCreditByAccountAndCompanyOnOrBeforeDate(acc.getId(), targetCompanyId, asOfDate);

            if ("ASSET".equals(type)) {
                BigDecimal bal = acc.getOpeningBalance().add(pDebit).subtract(pCredit);
                assetRows.add(BalanceSheetReportDTO.Row.builder()
                        .accountId(acc.getId())
                        .accountCode(acc.getAccountCode())
                        .accountName(acc.getAccountName())
                        .accountType(acc.getAccountType())
                        .balance(bal)
                        .build());
                totalAssets = totalAssets.add(bal);
            } else if ("LIABILITY".equals(type)) {
                BigDecimal bal = acc.getOpeningBalance().add(pCredit).subtract(pDebit);
                liabilityRows.add(BalanceSheetReportDTO.Row.builder()
                        .accountId(acc.getId())
                        .accountCode(acc.getAccountCode())
                        .accountName(acc.getAccountName())
                        .accountType(acc.getAccountType())
                        .balance(bal)
                        .build());
                totalLiabilities = totalLiabilities.add(bal);
            } else if ("EQUITY".equals(type)) {
                BigDecimal bal = acc.getOpeningBalance().add(pCredit).subtract(pDebit);
                equityRows.add(BalanceSheetReportDTO.Row.builder()
                        .accountId(acc.getId())
                        .accountCode(acc.getAccountCode())
                        .accountName(acc.getAccountName())
                        .accountType(acc.getAccountType())
                        .balance(bal)
                        .build());
                totalEquity = totalEquity.add(bal);
            } else if ("INCOME".equals(type) || "REVENUE".equals(type)) {
                BigDecimal netInc = pCredit.subtract(pDebit);
                allIncome = allIncome.add(netInc);
            } else if ("EXPENSE".equals(type)) {
                BigDecimal netExp = pDebit.subtract(pCredit);
                allExpense = allExpense.add(netExp);
            }
        }

        BigDecimal netProfitCurrentPeriod = allIncome.subtract(allExpense);

        equityRows.add(BalanceSheetReportDTO.Row.builder()
                .accountId(null)
                .accountCode("NET_PROFIT")
                .accountName("Current Period Net Profit / (Loss)")
                .accountType("EQUITY")
                .balance(netProfitCurrentPeriod)
                .build());
        totalEquity = totalEquity.add(netProfitCurrentPeriod);

        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);

        return BalanceSheetReportDTO.Response.builder()
                .asOfDate(asOfDate)
                .assetRows(assetRows)
                .liabilityRows(liabilityRows)
                .equityRows(equityRows)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .netProfitCurrentPeriod(netProfitCurrentPeriod)
                .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
                .difference(difference)
                .build();
    }

    // ==========================================
    // PRINT DATA WRAPPERS
    // ==========================================
    @Transactional(readOnly = true)
    public FinancialReportPrintDTO<TrialBalanceDTO.Response> getTrialBalancePrintData(Long companyId, LocalDate startDate, LocalDate endDate) {
        TrialBalanceDTO.Response data = getTrialBalance(companyId, startDate, endDate);
        return buildPrintWrapper(companyId, data);
    }

    @Transactional(readOnly = true)
    public FinancialReportPrintDTO<GeneralLedgerReportDTO.Response> getGeneralLedgerPrintData(Long accountId, Long companyId, LocalDate startDate, LocalDate endDate) {
        GeneralLedgerReportDTO.Response data = getGeneralLedger(accountId, companyId, startDate, endDate);
        return buildPrintWrapper(companyId, data);
    }

    @Transactional(readOnly = true)
    public FinancialReportPrintDTO<ProfitLossReportDTO.Response> getProfitLossPrintData(Long companyId, LocalDate startDate, LocalDate endDate) {
        ProfitLossReportDTO.Response data = getProfitAndLoss(companyId, startDate, endDate);
        return buildPrintWrapper(companyId, data);
    }

    @Transactional(readOnly = true)
    public FinancialReportPrintDTO<BalanceSheetReportDTO.Response> getBalanceSheetPrintData(Long companyId, LocalDate asOfDate) {
        BalanceSheetReportDTO.Response data = getBalanceSheet(companyId, asOfDate);
        return buildPrintWrapper(companyId, data);
    }

    // ==========================================
    // PDF GENERATION EXPORTS
    // ==========================================
    @Transactional(readOnly = true)
    public void generateTrialBalancePdf(Long companyId, LocalDate startDate, LocalDate endDate, OutputStream os) throws Exception {
        FinancialReportPrintDTO<TrialBalanceDTO.Response> printDTO = getTrialBalancePrintData(companyId, startDate, endDate);
        FinancialReportPdfGenerator.generateTrialBalancePdf(printDTO, os);
    }

    @Transactional(readOnly = true)
    public void generateGeneralLedgerPdf(Long accountId, Long companyId, LocalDate startDate, LocalDate endDate, OutputStream os) throws Exception {
        FinancialReportPrintDTO<GeneralLedgerReportDTO.Response> printDTO = getGeneralLedgerPrintData(accountId, companyId, startDate, endDate);
        FinancialReportPdfGenerator.generateGeneralLedgerPdf(printDTO, os);
    }

    @Transactional(readOnly = true)
    public void generateProfitLossPdf(Long companyId, LocalDate startDate, LocalDate endDate, OutputStream os) throws Exception {
        FinancialReportPrintDTO<ProfitLossReportDTO.Response> printDTO = getProfitLossPrintData(companyId, startDate, endDate);
        FinancialReportPdfGenerator.generateProfitLossPdf(printDTO, os);
    }

    @Transactional(readOnly = true)
    public void generateBalanceSheetPdf(Long companyId, LocalDate asOfDate, OutputStream os) throws Exception {
        FinancialReportPrintDTO<BalanceSheetReportDTO.Response> printDTO = getBalanceSheetPrintData(companyId, asOfDate);
        FinancialReportPdfGenerator.generateBalanceSheetPdf(printDTO, os);
    }

    // ==========================================
    // CSV EXPORTS
    // ==========================================
    @Transactional(readOnly = true)
    public String generateTrialBalanceCsv(Long companyId, LocalDate startDate, LocalDate endDate) {
        FinancialReportPrintDTO<TrialBalanceDTO.Response> printDTO = getTrialBalancePrintData(companyId, startDate, endDate);
        TrialBalanceDTO.Response data = printDTO.getReportData();

        StringBuilder sb = new StringBuilder();
        sb.append("TRIAL BALANCE STATEMENT\n");
        sb.append("Company: ").append(csv(printDTO.getCompanyName())).append("\n");
        sb.append("Period: ").append(data.getStartDate()).append(" to ").append(data.getEndDate()).append("\n");
        sb.append("Status: ").append(data.getDifference() != null && data.getDifference().signum() == 0 ? "BALANCED" : "UNBALANCED").append("\n\n");

        sb.append("Account Code,Account Name,Account Type,Opening Balance,Period Debit,Period Credit,Closing Debit,Closing Credit\n");
        if (data.getRows() != null) {
            for (TrialBalanceDTO.Row row : data.getRows()) {
                sb.append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getOpeningBalance()).append(',')
                        .append(row.getPeriodDebit()).append(',')
                        .append(row.getPeriodCredit()).append(',')
                        .append(row.getClosingDebit()).append(',')
                        .append(row.getClosingCredit()).append('\n');
            }
        }
        sb.append("TOTALS,,,").append(data.getTotalOpeningBalance()).append(',')
                .append(data.getTotalPeriodDebit()).append(',')
                .append(data.getTotalPeriodCredit()).append(',')
                .append(data.getTotalClosingDebit()).append(',')
                .append(data.getTotalClosingCredit()).append('\n');

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String generateGeneralLedgerCsv(Long accountId, Long companyId, LocalDate startDate, LocalDate endDate) {
        FinancialReportPrintDTO<GeneralLedgerReportDTO.Response> printDTO = getGeneralLedgerPrintData(accountId, companyId, startDate, endDate);
        GeneralLedgerReportDTO.Response data = printDTO.getReportData();

        StringBuilder sb = new StringBuilder();
        sb.append("GENERAL LEDGER STATEMENT\n");
        sb.append("Company: ").append(csv(printDTO.getCompanyName())).append("\n");
        sb.append("Account: ").append(csv(data.getAccountCode())).append(" - ").append(csv(data.getAccountName())).append(" (").append(csv(data.getAccountType())).append(")\n");
        sb.append("Period: ").append(data.getStartDate()).append(" to ").append(data.getEndDate()).append("\n");
        sb.append("Opening Balance: ").append(data.getOpeningBalance()).append("\n");
        sb.append("Total Debit: ").append(data.getTotalDebit()).append("\n");
        sb.append("Total Credit: ").append(data.getTotalCredit()).append("\n");
        sb.append("Closing Balance: ").append(data.getClosingBalance()).append("\n\n");

        sb.append("Voucher Date,Voucher Number,Reference Number,Description,Debit,Credit,Running Balance\n");
        if (data.getEntries() != null) {
            for (GeneralLedgerReportDTO.Entry entry : data.getEntries()) {
                sb.append(csv(entry.getVoucherDate() != null ? entry.getVoucherDate().toString() : "")).append(',')
                        .append(csv(entry.getVoucherNumber())).append(',')
                        .append(csv(entry.getReferenceNumber())).append(',')
                        .append(csv(entry.getDescription())).append(',')
                        .append(entry.getDebit()).append(',')
                        .append(entry.getCredit()).append(',')
                        .append(entry.getRunningBalance()).append('\n');
            }
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String generateProfitLossCsv(Long companyId, LocalDate startDate, LocalDate endDate) {
        FinancialReportPrintDTO<ProfitLossReportDTO.Response> printDTO = getProfitLossPrintData(companyId, startDate, endDate);
        ProfitLossReportDTO.Response data = printDTO.getReportData();

        StringBuilder sb = new StringBuilder();
        sb.append("PROFIT & LOSS STATEMENT\n");
        sb.append("Company: ").append(csv(printDTO.getCompanyName())).append("\n");
        sb.append("Period: ").append(data.getStartDate()).append(" to ").append(data.getEndDate()).append("\n");
        sb.append("Total Income: ").append(data.getTotalIncome()).append("\n");
        sb.append("Total Expenses: ").append(data.getTotalExpense()).append("\n");
        sb.append("Net Result: ").append(Boolean.TRUE.equals(data.getIsProfitable()) ? "PROFIT " + data.getNetProfit() : "LOSS " + data.getNetLoss()).append("\n\n");

        sb.append("Category,Account Code,Account Name,Account Type,Amount\n");
        if (data.getIncomeRows() != null) {
            for (ProfitLossReportDTO.Row row : data.getIncomeRows()) {
                sb.append("INCOME,").append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getAmount()).append('\n');
            }
        }
        if (data.getExpenseRows() != null) {
            for (ProfitLossReportDTO.Row row : data.getExpenseRows()) {
                sb.append("EXPENSE,").append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getAmount()).append('\n');
            }
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String generateBalanceSheetCsv(Long companyId, LocalDate asOfDate) {
        FinancialReportPrintDTO<BalanceSheetReportDTO.Response> printDTO = getBalanceSheetPrintData(companyId, asOfDate);
        BalanceSheetReportDTO.Response data = printDTO.getReportData();

        StringBuilder sb = new StringBuilder();
        sb.append("BALANCE SHEET STATEMENT\n");
        sb.append("Company: ").append(csv(printDTO.getCompanyName())).append("\n");
        sb.append("As Of Date: ").append(data.getAsOfDate()).append("\n");
        sb.append("Total Assets: ").append(data.getTotalAssets()).append("\n");
        sb.append("Total Liabilities: ").append(data.getTotalLiabilities()).append("\n");
        sb.append("Total Equity: ").append(data.getTotalEquity()).append("\n");
        sb.append("Total Liabilities & Equity: ").append(data.getTotalLiabilitiesAndEquity()).append("\n");
        sb.append("Difference: ").append(data.getDifference()).append("\n\n");

        sb.append("Category,Account Code,Account Name,Account Type,Balance\n");
        if (data.getAssetRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getAssetRows()) {
                sb.append("ASSETS,").append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getBalance()).append('\n');
            }
        }
        if (data.getLiabilityRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getLiabilityRows()) {
                sb.append("LIABILITIES,").append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getBalance()).append('\n');
            }
        }
        if (data.getEquityRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getEquityRows()) {
                sb.append("EQUITY,").append(csv(row.getAccountCode())).append(',')
                        .append(csv(row.getAccountName())).append(',')
                        .append(csv(row.getAccountType())).append(',')
                        .append(row.getBalance()).append('\n');
            }
        }

        return sb.toString();
    }

    private <T> FinancialReportPrintDTO<T> buildPrintWrapper(Long companyId, T reportData) {
        Long targetCompanyId = tenantAccess.resolveCompanyId(companyId);
        Company company = companyRepository.findById(targetCompanyId).orElse(null);
        AppUser user = null;
        try {
            user = tenantAccess.requireCurrentUser();
        } catch (Exception ignored) {}

        Branch branch = (user != null && user.getBranchId() != null) 
                ? branchRepository.findById(user.getBranchId()).orElse(null) 
                : null;

        return FinancialReportPrintDTO.<T>builder()
                .companyName(company != null ? company.getName() : "TRANSAFLOW TRANSPORT ERP")
                .companyAddress(company != null ? company.getAddress() : "")
                .companyPhone(company != null ? company.getPhone() : "")
                .companyEmail(company != null ? company.getEmail() : "")
                .companyGSTIN(company != null ? company.getGstNumber() : "")
                .companyPAN(company != null ? company.getPanNumber() : "")
                .branchName(branch != null ? branch.getName() : null)
                .reportData(reportData)
                .build();
    }

    private static String csv(String value) {
        if (value == null || "null".equals(value)) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
