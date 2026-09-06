package com.transport.erp.service;

import com.transport.erp.dto.BalanceSheetReportDTO;
import com.transport.erp.dto.GeneralLedgerReportDTO;
import com.transport.erp.dto.ProfitLossReportDTO;
import com.transport.erp.dto.TrialBalanceDTO;
import com.transport.erp.model.ChartOfAccount;
import com.transport.erp.model.JournalVoucher;
import com.transport.erp.repository.ChartOfAccountRepository;
import com.transport.erp.repository.JournalVoucherRepository;
import com.transport.erp.security.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Include Current Period Net Profit in Equity for Balance Sheet balancing
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
}
