package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.*;

import java.awt.Color;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FinancialReportPdfGenerator {

    private static final Color PRIMARY_COLOR = new Color(30, 58, 138); // Dark Navy Blue
    private static final Color LIGHT_GRAY = new Color(243, 244, 246);
    private static final Color TEXT_DARK = new Color(17, 24, 39);
    private static final Color BORDER_GRAY = new Color(229, 231, 235);
    private static final Color EMERALD_GREEN = new Color(16, 185, 129);
    private static final Color ROSE_RED = new Color(239, 68, 68);

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    // ==========================================
    // 1. TRIAL BALANCE PDF
    // ==========================================
    public static void generateTrialBalancePdf(FinancialReportPrintDTO<TrialBalanceDTO.Response> printDTO, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        TrialBalanceDTO.Response data = printDTO.getReportData();

        // Company Header
        addCompanyHeader(document, printDTO, "TRIAL BALANCE STATEMENT", "Period: " + formatDate(data.getStartDate()) + " to " + formatDate(data.getEndDate()));

        // Table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{10, 24, 12, 13, 13, 13, 13, 12});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK);

        table.addCell(createHeaderCell("Code", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Account Name", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Type", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Opening Bal (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Period Debit (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Period Credit (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Closing Debit (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Closing Credit (₹)", headerFont, PRIMARY_COLOR));

        if (data.getRows() != null) {
            for (TrialBalanceDTO.Row row : data.getRows()) {
                table.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(row.getAccountType() != null ? row.getAccountType() : "", normalFont, Element.ALIGN_CENTER));
                table.addCell(createBodyCell(formatCurrency(row.getOpeningBalance()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(row.getPeriodDebit()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(row.getPeriodCredit()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(row.getClosingDebit()), boldFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(row.getClosingCredit()), boldFont, Element.ALIGN_RIGHT));
            }
        }

        // Totals Footer Row
        table.addCell(createSummaryCell("Totals", boldFont, Element.ALIGN_RIGHT, 3));
        table.addCell(createSummaryCell(formatCurrency(data.getTotalOpeningBalance()), boldFont, Element.ALIGN_RIGHT, 1));
        table.addCell(createSummaryCell(formatCurrency(data.getTotalPeriodDebit()), boldFont, Element.ALIGN_RIGHT, 1));
        table.addCell(createSummaryCell(formatCurrency(data.getTotalPeriodCredit()), boldFont, Element.ALIGN_RIGHT, 1));
        table.addCell(createSummaryCell(formatCurrency(data.getTotalClosingDebit()), boldFont, Element.ALIGN_RIGHT, 1));
        table.addCell(createSummaryCell(formatCurrency(data.getTotalClosingCredit()), boldFont, Element.ALIGN_RIGHT, 1));

        document.add(table);
        document.add(new Paragraph(" "));

        // Reconciled Check Footer
        addFooterCheck(document, "Difference: " + formatCurrency(data.getDifference()), data.getDifference() != null && data.getDifference().signum() == 0);

        document.close();
    }

    // ==========================================
    // 2. GENERAL LEDGER PDF
    // ==========================================
    public static void generateGeneralLedgerPdf(FinancialReportPrintDTO<GeneralLedgerReportDTO.Response> printDTO, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        GeneralLedgerReportDTO.Response data = printDTO.getReportData();

        String subTitle = "Account: " + (data.getAccountCode() != null ? data.getAccountCode() : "") + " - " 
                + (data.getAccountName() != null ? data.getAccountName() : "") 
                + " (" + (data.getAccountType() != null ? data.getAccountType() : "") + ")"
                + " | Period: " + formatDate(data.getStartDate()) + " to " + formatDate(data.getEndDate());

        addCompanyHeader(document, printDTO, "GENERAL LEDGER STATEMENT", subTitle);

        // Account Summary Card
        PdfPTable cardTable = new PdfPTable(4);
        cardTable.setWidthPercentage(100);
        cardTable.setSpacingAfter(10);
        cardTable.setWidths(new float[]{25, 25, 25, 25});

        Font cardLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARY_COLOR);
        Font cardValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_DARK);

        addCardCell(cardTable, "Opening Balance", formatCurrency(data.getOpeningBalance()), cardLabelFont, cardValueFont);
        addCardCell(cardTable, "Total Period Debit", formatCurrency(data.getTotalDebit()), cardLabelFont, cardValueFont);
        addCardCell(cardTable, "Total Period Credit", formatCurrency(data.getTotalCredit()), cardLabelFont, cardValueFont);
        addCardCell(cardTable, "Closing Balance", formatCurrency(data.getClosingBalance()), cardLabelFont, cardValueFont);

        document.add(cardTable);

        // Transactions Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{12, 16, 16, 26, 10, 10, 10});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK);

        table.addCell(createHeaderCell("Date", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Voucher No", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Reference No", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Description", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Debit (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Credit (₹)", headerFont, PRIMARY_COLOR));
        table.addCell(createHeaderCell("Running Bal (₹)", headerFont, PRIMARY_COLOR));

        if (data.getEntries() != null) {
            for (GeneralLedgerReportDTO.Entry entry : data.getEntries()) {
                table.addCell(createBodyCell(formatDate(entry.getVoucherDate()), normalFont, Element.ALIGN_CENTER));
                table.addCell(createBodyCell(entry.getVoucherNumber() != null ? entry.getVoucherNumber() : "", boldFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(entry.getReferenceNumber() != null ? entry.getReferenceNumber() : "", normalFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(entry.getDescription() != null ? entry.getDescription() : "", normalFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(formatCurrency(entry.getDebit()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(entry.getCredit()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell(formatCurrency(entry.getRunningBalance()), boldFont, Element.ALIGN_RIGHT));
            }
        }

        document.add(table);
        document.add(new Paragraph(" "));
        addFooterCheck(document, "End of General Ledger Statement. Zero DB Mutations.", true);

        document.close();
    }

    // ==========================================
    // 3. PROFIT & LOSS PDF
    // ==========================================
    public static void generateProfitLossPdf(FinancialReportPrintDTO<ProfitLossReportDTO.Response> printDTO, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        ProfitLossReportDTO.Response data = printDTO.getReportData();

        addCompanyHeader(document, printDTO, "PROFIT & LOSS STATEMENT", "Period: " + formatDate(data.getStartDate()) + " to " + formatDate(data.getEndDate()));

        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        // Income Table
        Paragraph incTitle = new Paragraph("INCOME / REVENUE", sectionHeaderFont);
        incTitle.setSpacingAfter(4);
        document.add(incTitle);

        PdfPTable incTable = new PdfPTable(3);
        incTable.setWidthPercentage(100);
        incTable.setWidths(new float[]{20, 55, 25});

        incTable.addCell(createHeaderCell("Code", headerFont, EMERALD_GREEN));
        incTable.addCell(createHeaderCell("Account Name", headerFont, EMERALD_GREEN));
        incTable.addCell(createHeaderCell("Amount (₹)", headerFont, EMERALD_GREEN));

        if (data.getIncomeRows() != null) {
            for (ProfitLossReportDTO.Row row : data.getIncomeRows()) {
                incTable.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                incTable.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                incTable.addCell(createBodyCell(formatCurrency(row.getAmount()), boldFont, Element.ALIGN_RIGHT));
            }
        }
        incTable.addCell(createSummaryCell("Total Income", boldFont, Element.ALIGN_RIGHT, 2));
        incTable.addCell(createSummaryCell(formatCurrency(data.getTotalIncome()), boldFont, Element.ALIGN_RIGHT, 1));
        document.add(incTable);
        document.add(new Paragraph(" "));

        // Expense Table
        Paragraph expTitle = new Paragraph("OPERATING EXPENSES", sectionHeaderFont);
        expTitle.setSpacingAfter(4);
        document.add(expTitle);

        PdfPTable expTable = new PdfPTable(3);
        expTable.setWidthPercentage(100);
        expTable.setWidths(new float[]{20, 55, 25});

        expTable.addCell(createHeaderCell("Code", headerFont, ROSE_RED));
        expTable.addCell(createHeaderCell("Account Name", headerFont, ROSE_RED));
        expTable.addCell(createHeaderCell("Amount (₹)", headerFont, ROSE_RED));

        if (data.getExpenseRows() != null) {
            for (ProfitLossReportDTO.Row row : data.getExpenseRows()) {
                expTable.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                expTable.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                expTable.addCell(createBodyCell(formatCurrency(row.getAmount()), boldFont, Element.ALIGN_RIGHT));
            }
        }
        expTable.addCell(createSummaryCell("Total Expenses", boldFont, Element.ALIGN_RIGHT, 2));
        expTable.addCell(createSummaryCell(formatCurrency(data.getTotalExpense()), boldFont, Element.ALIGN_RIGHT, 1));
        document.add(expTable);
        document.add(new Paragraph(" "));

        // Net Summary Card
        boolean isProfitable = Boolean.TRUE.equals(data.getIsProfitable());
        String netLabel = isProfitable ? "NET PROFIT FOR THE PERIOD" : "NET LOSS FOR THE PERIOD";
        String netValue = formatCurrency(isProfitable ? data.getNetProfit() : data.getNetLoss());
        addFooterCheck(document, netLabel + ": " + netValue, isProfitable);

        document.close();
    }

    // ==========================================
    // 4. BALANCE SHEET PDF
    // ==========================================
    public static void generateBalanceSheetPdf(FinancialReportPrintDTO<BalanceSheetReportDTO.Response> printDTO, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        BalanceSheetReportDTO.Response data = printDTO.getReportData();

        addCompanyHeader(document, printDTO, "BALANCE SHEET STATEMENT", "As of Date: " + formatDate(data.getAsOfDate()));

        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        // 1. Assets
        Paragraph assetTitle = new Paragraph("ASSETS", sectionHeaderFont);
        assetTitle.setSpacingAfter(4);
        document.add(assetTitle);

        PdfPTable assetTable = new PdfPTable(3);
        assetTable.setWidthPercentage(100);
        assetTable.setWidths(new float[]{20, 55, 25});

        assetTable.addCell(createHeaderCell("Code", headerFont, PRIMARY_COLOR));
        assetTable.addCell(createHeaderCell("Account Name", headerFont, PRIMARY_COLOR));
        assetTable.addCell(createHeaderCell("Balance (₹)", headerFont, PRIMARY_COLOR));

        if (data.getAssetRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getAssetRows()) {
                assetTable.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                assetTable.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                assetTable.addCell(createBodyCell(formatCurrency(row.getBalance()), boldFont, Element.ALIGN_RIGHT));
            }
        }
        assetTable.addCell(createSummaryCell("Total Assets", boldFont, Element.ALIGN_RIGHT, 2));
        assetTable.addCell(createSummaryCell(formatCurrency(data.getTotalAssets()), boldFont, Element.ALIGN_RIGHT, 1));
        document.add(assetTable);
        document.add(new Paragraph(" "));

        // 2. Liabilities
        Paragraph liabTitle = new Paragraph("LIABILITIES", sectionHeaderFont);
        liabTitle.setSpacingAfter(4);
        document.add(liabTitle);

        PdfPTable liabTable = new PdfPTable(3);
        liabTable.setWidthPercentage(100);
        liabTable.setWidths(new float[]{20, 55, 25});

        liabTable.addCell(createHeaderCell("Code", headerFont, PRIMARY_COLOR));
        liabTable.addCell(createHeaderCell("Account Name", headerFont, PRIMARY_COLOR));
        liabTable.addCell(createHeaderCell("Balance (₹)", headerFont, PRIMARY_COLOR));

        if (data.getLiabilityRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getLiabilityRows()) {
                liabTable.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                liabTable.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                liabTable.addCell(createBodyCell(formatCurrency(row.getBalance()), boldFont, Element.ALIGN_RIGHT));
            }
        }
        liabTable.addCell(createSummaryCell("Total Liabilities", boldFont, Element.ALIGN_RIGHT, 2));
        liabTable.addCell(createSummaryCell(formatCurrency(data.getTotalLiabilities()), boldFont, Element.ALIGN_RIGHT, 1));
        document.add(liabTable);
        document.add(new Paragraph(" "));

        // 3. Equity
        Paragraph eqTitle = new Paragraph("EQUITY", sectionHeaderFont);
        eqTitle.setSpacingAfter(4);
        document.add(eqTitle);

        PdfPTable eqTable = new PdfPTable(3);
        eqTable.setWidthPercentage(100);
        eqTable.setWidths(new float[]{20, 55, 25});

        eqTable.addCell(createHeaderCell("Code", headerFont, PRIMARY_COLOR));
        eqTable.addCell(createHeaderCell("Account Name", headerFont, PRIMARY_COLOR));
        eqTable.addCell(createHeaderCell("Balance (₹)", headerFont, PRIMARY_COLOR));

        if (data.getEquityRows() != null) {
            for (BalanceSheetReportDTO.Row row : data.getEquityRows()) {
                eqTable.addCell(createBodyCell(row.getAccountCode() != null ? row.getAccountCode() : "", boldFont, Element.ALIGN_LEFT));
                eqTable.addCell(createBodyCell(row.getAccountName() != null ? row.getAccountName() : "", normalFont, Element.ALIGN_LEFT));
                eqTable.addCell(createBodyCell(formatCurrency(row.getBalance()), boldFont, Element.ALIGN_RIGHT));
            }
        }
        eqTable.addCell(createSummaryCell("Total Equity", boldFont, Element.ALIGN_RIGHT, 2));
        eqTable.addCell(createSummaryCell(formatCurrency(data.getTotalEquity()), boldFont, Element.ALIGN_RIGHT, 1));
        document.add(eqTable);
        document.add(new Paragraph(" "));

        // Equation Check
        boolean isBalanced = data.getDifference() != null && data.getDifference().signum() == 0;
        String eqCheckText = "Total Liabilities & Equity: " + formatCurrency(data.getTotalLiabilitiesAndEquity()) + " | Difference: " + formatCurrency(data.getDifference());
        addFooterCheck(document, eqCheckText, isBalanced);

        document.close();
    }

    // ==========================================
    // HELPER METHODS FOR UTILITY RENDERING
    // ==========================================
    private static void addCompanyHeader(Document document, FinancialReportPrintDTO<?> dto, String reportTitle, String subTitle) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.addElement(new Paragraph(dto.getCompanyName() != null ? dto.getCompanyName().toUpperCase() : "TRANSAFLOW TRANSPORT ERP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR)));
        if (dto.getCompanyAddress() != null && !dto.getCompanyAddress().trim().isEmpty()) {
            compCell.addElement(new Paragraph(dto.getCompanyAddress(), FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK)));
        }
        if (dto.getCompanyPhone() != null && !dto.getCompanyPhone().trim().isEmpty()) {
            compCell.addElement(new Paragraph("Phone: " + dto.getCompanyPhone() + " | Email: " + (dto.getCompanyEmail() != null ? dto.getCompanyEmail() : ""), FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK)));
        }
        if (dto.getCompanyGSTIN() != null && !dto.getCompanyGSTIN().trim().isEmpty()) {
            compCell.addElement(new Paragraph("GSTIN: " + dto.getCompanyGSTIN(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK)));
        }
        headerTable.addCell(compCell);

        PdfPCell branchCell = new PdfPCell();
        branchCell.setBorder(Rectangle.NO_BORDER);
        branchCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph bTitle = new Paragraph(reportTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR));
        bTitle.setAlignment(Element.ALIGN_RIGHT);
        branchCell.addElement(bTitle);

        if (subTitle != null) {
            Paragraph sTitle = new Paragraph(subTitle, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_DARK));
            sTitle.setAlignment(Element.ALIGN_RIGHT);
            branchCell.addElement(sTitle);
        }
        if (dto.getBranchName() != null) {
            Paragraph bName = new Paragraph("Branch: " + dto.getBranchName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK));
            bName.setAlignment(Element.ALIGN_RIGHT);
            branchCell.addElement(bName);
        }
        headerTable.addCell(branchCell);
        document.add(headerTable);

        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100, PRIMARY_COLOR, Element.ALIGN_CENTER, -4)));
        document.add(new Paragraph(" "));
    }

    private static void addCardCell(PdfPTable cardTable, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorderColor(BORDER_GRAY);
        cell.addElement(new Paragraph(label.toUpperCase(), labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        cardTable.addCell(cell);
    }

    private static void addFooterCheck(Document document, String text, boolean isOk) throws DocumentException {
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(isOk ? new Color(236, 253, 245) : new Color(254, 242, 242));
        cell.setBorderColor(isOk ? EMERALD_GREEN : ROSE_RED);

        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, isOk ? EMERALD_GREEN : ROSE_RED);
        Paragraph p = new Paragraph(text + " [" + (isOk ? "BALANCED" : "UNBALANCED") + "]", font);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        footerTable.addCell(cell);
        document.add(footerTable);
    }

    private static PdfPCell createHeaderCell(String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    private static PdfPCell createBodyCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    private static PdfPCell createSummaryCell(String text, Font font, int align, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorderColor(BORDER_GRAY);
        return cell;
    }

    private static String formatCurrency(java.math.BigDecimal val) {
        if (val == null) return "₹0.00";
        return CURRENCY_FORMAT.format(val);
    }

    private static String formatDate(java.time.LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }
}
