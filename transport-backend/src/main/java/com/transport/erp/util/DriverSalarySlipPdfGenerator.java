package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.DriverPayrollPrintDTO;

import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DriverSalarySlipPdfGenerator {

    public static void generateSalarySlipPdf(DriverPayrollPrintDTO data, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        // Colors
        Color primaryColor = new Color(30, 58, 138); // Dark Navy Blue
        Color lightGray = new Color(243, 244, 246);
        Color textDark = new Color(17, 24, 39);
        Color accentGreen = new Color(16, 185, 129);

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, primaryColor);
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryColor);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textDark);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, textDark);
        Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.RED);
        Font netSalaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor);

        // Currency formatter
        NumberFormat curFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

        // 1. Status Header Banner (if Cancelled or Draft)
        if ("CANCELLED".equals(data.getStatus())) {
            Paragraph cancelHeader = new Paragraph("CANCELLED SALARY SLIP", warningFont);
            cancelHeader.setAlignment(Element.ALIGN_CENTER);
            cancelHeader.setSpacingAfter(10);
            document.add(cancelHeader);
        } else if ("DRAFT".equals(data.getStatus())) {
            Paragraph draftHeader = new Paragraph("DRAFT SALARY SLIP (PROVISIONAL)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.ORANGE));
            draftHeader.setAlignment(Element.ALIGN_CENTER);
            draftHeader.setSpacingAfter(10);
            document.add(draftHeader);
        }

        // 2. Company & Header Block
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.addElement(new Paragraph(data.getCompanyName() != null ? data.getCompanyName() : "TRANSPORT ERP", titleFont));
        if (data.getCompanyAddress() != null && !data.getCompanyAddress().isBlank()) {
            compCell.addElement(new Paragraph(data.getCompanyAddress(), normalFont));
        }
        if (data.getCompanyPhone() != null || data.getCompanyEmail() != null) {
            String contact = "Contact: " + (data.getCompanyPhone() != null ? data.getCompanyPhone() : "") +
                             (data.getCompanyEmail() != null ? " | " + data.getCompanyEmail() : "");
            compCell.addElement(new Paragraph(contact, normalFont));
        }
        if (data.getCompanyGSTIN() != null && !data.getCompanyGSTIN().isBlank()) {
            compCell.addElement(new Paragraph("GSTIN: " + data.getCompanyGSTIN(), normalFont));
        }
        headerTable.addCell(compCell);

        PdfPCell docMetaCell = new PdfPCell();
        docMetaCell.setBorder(Rectangle.NO_BORDER);
        docMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph docTitle = new Paragraph("DRIVER SALARY SLIP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor));
        docTitle.setAlignment(Element.ALIGN_RIGHT);
        docMetaCell.addElement(docTitle);

        Paragraph pNum = new Paragraph("Payroll No: " + (data.getPayrollNumber() != null ? data.getPayrollNumber() : "PR-" + data.getPayrollId()), boldFont);
        pNum.setAlignment(Element.ALIGN_RIGHT);
        docMetaCell.addElement(pNum);

        Paragraph pPeriod = new Paragraph("Pay Period: " + (data.getPayPeriod() != null ? data.getPayPeriod() : data.getPayMonth() + "/" + data.getPayYear()), normalFont);
        pPeriod.setAlignment(Element.ALIGN_RIGHT);
        docMetaCell.addElement(pPeriod);

        Paragraph pStatus = new Paragraph("Status: " + (data.getStatus() != null ? data.getStatus() : "ACTIVE"), boldFont);
        pStatus.setAlignment(Element.ALIGN_RIGHT);
        docMetaCell.addElement(pStatus);

        headerTable.addCell(docMetaCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100, primaryColor, Element.ALIGN_CENTER, -2)));
        document.add(new Paragraph(" "));

        // 3. Driver Information Block
        Paragraph driverSecHeader = new Paragraph("DRIVER EMPLOYEE DETAILS", sectionHeaderFont);
        driverSecHeader.setSpacingAfter(6);
        document.add(driverSecHeader);

        PdfPTable driverTable = new PdfPTable(4);
        driverTable.setWidthPercentage(100);
        driverTable.setWidths(new float[]{25, 25, 25, 25});

        driverTable.addCell(createLabelCell("Driver Name:", boldFont));
        driverTable.addCell(createValueCell(data.getDriverName() != null ? data.getDriverName() : "N/A", normalFont));

        driverTable.addCell(createLabelCell("Driver Code:", boldFont));
        driverTable.addCell(createValueCell(data.getDriverCode() != null ? data.getDriverCode() : "N/A", normalFont));

        driverTable.addCell(createLabelCell("Mobile / Phone:", boldFont));
        driverTable.addCell(createValueCell(data.getDriverPhone() != null ? data.getDriverPhone() : "N/A", normalFont));

        driverTable.addCell(createLabelCell("License Number:", boldFont));
        driverTable.addCell(createValueCell(data.getLicenseNumber() != null ? data.getLicenseNumber() : "N/A", normalFont));

        document.add(driverTable);
        document.add(new Paragraph(" "));

        // 4. Earnings & Deductions Breakdown Table
        Paragraph salaryBreakdownHeader = new Paragraph("SALARY COMPONENTS BREAKDOWN", sectionHeaderFont);
        salaryBreakdownHeader.setSpacingAfter(6);
        document.add(salaryBreakdownHeader);

        PdfPTable componentTable = new PdfPTable(4);
        componentTable.setWidthPercentage(100);
        componentTable.setWidths(new float[]{35, 15, 35, 15});

        // Headers
        componentTable.addCell(createHeaderCell("Earnings Description", boldFont, primaryColor));
        componentTable.addCell(createHeaderCell("Amount (₹)", boldFont, primaryColor));
        componentTable.addCell(createHeaderCell("Deductions Description", boldFont, primaryColor));
        componentTable.addCell(createHeaderCell("Amount (₹)", boldFont, primaryColor));

        // Line 1: Basic Salary vs Deductions
        componentTable.addCell(createBodyCell("Basic Salary", normalFont));
        componentTable.addCell(createAmountCell(data.getBasicSalary(), normalFont));
        componentTable.addCell(createBodyCell("Standard Deductions", normalFont));
        componentTable.addCell(createAmountCell(data.getDeductionAmount(), normalFont));

        // Line 2: Allowances vs Advance Adjustments
        componentTable.addCell(createBodyCell("Allowances / Incentives", normalFont));
        componentTable.addCell(createAmountCell(data.getAllowanceAmount(), normalFont));
        componentTable.addCell(createBodyCell("Advance / Loan Recovery", normalFont));
        componentTable.addCell(createAmountCell(data.getAdvanceAdjustment(), normalFont));

        // Subtotals Row
        componentTable.addCell(createTotalHeaderCell("Gross Earnings", boldFont));
        componentTable.addCell(createTotalAmountCell(data.getGrossEarnings(), boldFont));
        componentTable.addCell(createTotalHeaderCell("Total Deductions", boldFont));
        componentTable.addCell(createTotalAmountCell(data.getTotalDeductions(), boldFont));

        document.add(componentTable);
        document.add(new Paragraph(" "));

        // 5. Net Payable Highlight Box
        PdfPTable netTable = new PdfPTable(1);
        netTable.setWidthPercentage(100);
        PdfPCell netCell = new PdfPCell();
        netCell.setBackgroundColor(lightGray);
        netCell.setPadding(10);
        netCell.setBorderColor(primaryColor);
        netCell.setBorderWidth(1.5f);

        Paragraph netText = new Paragraph("NET SALARY PAYABLE: " + curFormat.format(data.getNetSalaryPayable() != null ? data.getNetSalaryPayable() : BigDecimal.ZERO), netSalaryFont);
        netText.setAlignment(Element.ALIGN_CENTER);
        netCell.addElement(netText);
        netTable.addCell(netCell);

        document.add(netTable);
        document.add(new Paragraph(" "));

        // 6. Accounting JV Information (if posted / paid)
        if (data.getAccrualJvNumber() != null || data.getPaymentJvNumber() != null) {
            Paragraph acctHeader = new Paragraph("ACCOUNTING VOUCHER REFERENCES", sectionHeaderFont);
            acctHeader.setSpacingAfter(6);
            document.add(acctHeader);

            PdfPTable acctTable = new PdfPTable(2);
            acctTable.setWidthPercentage(100);
            acctTable.setWidths(new float[]{50, 50});

            acctTable.addCell(createLabelCell("Accrual JV Number:", boldFont));
            acctTable.addCell(createValueCell(data.getAccrualJvNumber() != null ? data.getAccrualJvNumber() : "N/A", normalFont));

            acctTable.addCell(createLabelCell("Payment JV Number:", boldFont));
            acctTable.addCell(createValueCell(data.getPaymentJvNumber() != null ? data.getPaymentJvNumber() : "N/A", normalFont));

            document.add(acctTable);
            document.add(new Paragraph(" "));
        }

        // 7. Footer Sign-off Block
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.5f, 100, Color.LIGHT_GRAY, Element.ALIGN_CENTER, 0)));
        document.add(new Paragraph(" "));

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{50, 50});

        PdfPCell fLeft = new PdfPCell();
        fLeft.setBorder(Rectangle.NO_BORDER);
        fLeft.addElement(new Paragraph("Employee Signature: __________________", italicFont));
        footerTable.addCell(fLeft);

        PdfPCell fRight = new PdfPCell();
        fRight.setBorder(Rectangle.NO_BORDER);
        fRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph sysGen = new Paragraph("System-Generated Salary Slip", italicFont);
        sysGen.setAlignment(Element.ALIGN_RIGHT);
        fRight.addElement(sysGen);
        Paragraph printTime = new Paragraph("Generated At: " + LocalDateTime.now().format(dtFormatter), italicFont);
        printTime.setAlignment(Element.ALIGN_RIGHT);
        fRight.addElement(printTime);
        footerTable.addCell(fRight);

        document.add(footerTable);
        document.close();
    }

    private static PdfPCell createHeaderCell(String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(239, 246, 255));
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createBodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createAmountCell(BigDecimal amount, Font font) {
        String formatted = amount != null ? String.format("₹%,.2f", amount) : "₹0.00";
        PdfPCell cell = new PdfPCell(new Phrase(formatted, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(249, 250, 251));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell createTotalHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(243, 244, 246));
        cell.setPadding(6);
        cell.setBorderColor(Color.GRAY);
        return cell;
    }

    private static PdfPCell createTotalAmountCell(BigDecimal amount, Font font) {
        String formatted = amount != null ? String.format("₹%,.2f", amount) : "₹0.00";
        PdfPCell cell = new PdfPCell(new Phrase(formatted, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBackgroundColor(new Color(243, 244, 246));
        cell.setPadding(6);
        cell.setBorderColor(Color.GRAY);
        return cell;
    }
}
