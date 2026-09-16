package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.CustomerLedgerPrintDTO;
import java.awt.Color;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CustomerLedgerPdfGenerator {

    public static void generateCustomerLedgerPdf(CustomerLedgerPrintDTO data, OutputStream os) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, os);
        writer.setCompressionLevel(0);
        document.open();

        // Colors
        Color primaryColor = new Color(30, 58, 138); // Sleek Dark Blue
        Color lightGray = new Color(243, 244, 246);
        Color textDark = new Color(17, 24, 39);

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryColor);
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryColor);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textDark);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, textDark);
        Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

        // Formatter
        NumberFormat curFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

        // 1. Company & Branch Header Table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{55, 45});

        // Left: Company Info
        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.addElement(new Paragraph(data.getCompanyName() != null ? data.getCompanyName().toUpperCase() : "TRANSAFLOW TRANSPORT ERP", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor)));
        if (data.getCompanyAddress() != null && !data.getCompanyAddress().trim().isEmpty()) {
            compCell.addElement(new Paragraph(data.getCompanyAddress(), normalFont));
        }
        if (data.getCompanyPhone() != null && !data.getCompanyPhone().trim().isEmpty()) {
            compCell.addElement(new Paragraph("Phone: " + data.getCompanyPhone(), normalFont));
        }
        if (data.getCompanyEmail() != null && !data.getCompanyEmail().trim().isEmpty()) {
            compCell.addElement(new Paragraph("Email: " + data.getCompanyEmail(), normalFont));
        }
        if (data.getCompanyGSTIN() != null && !data.getCompanyGSTIN().trim().isEmpty()) {
            compCell.addElement(new Paragraph("GSTIN: " + data.getCompanyGSTIN(), boldFont));
        }
        headerTable.addCell(compCell);

        // Right: Branch Info
        PdfPCell branchCell = new PdfPCell();
        branchCell.setBorder(Rectangle.NO_BORDER);
        branchCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph bTitle = new Paragraph("BRANCH DETAILS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor));
        bTitle.setAlignment(Element.ALIGN_RIGHT);
        branchCell.addElement(bTitle);
        if (data.getBranchName() != null) {
            Paragraph bName = new Paragraph(data.getBranchName(), boldFont);
            bName.setAlignment(Element.ALIGN_RIGHT);
            branchCell.addElement(bName);
        }
        if (data.getBranchAddress() != null) {
            Paragraph bAddr = new Paragraph(data.getBranchAddress(), normalFont);
            bAddr.setAlignment(Element.ALIGN_RIGHT);
            branchCell.addElement(bAddr);
        }
        headerTable.addCell(branchCell);
        document.add(headerTable);

        // Line separator
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100, primaryColor, Element.ALIGN_CENTER, -5)));
        document.add(new Paragraph(" "));

        // 2. Document Title
        Paragraph docTitle = new Paragraph("CUSTOMER LEDGER STATEMENT", titleFont);
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(12);
        document.add(docTitle);

        // 3. Customer Information Card
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{50, 50});

        PdfPCell customerCell = new PdfPCell();
        customerCell.setPadding(8);
        customerCell.setBackgroundColor(lightGray);
        customerCell.setBorderColor(new Color(229, 231, 235));

        Paragraph custHeader = new Paragraph("CUSTOMER ACCOUNT INFORMATION", sectionHeaderFont);
        custHeader.setSpacingAfter(4);
        customerCell.addElement(custHeader);

        customerCell.addElement(new Paragraph(data.getCustomerName() != null ? data.getCustomerName() : "N/A", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textDark)));
        if (data.getCustomerCode() != null) {
            customerCell.addElement(new Paragraph("Customer Code: " + data.getCustomerCode(), normalFont));
        }
        if (data.getCustomerAddress() != null && !data.getCustomerAddress().trim().isEmpty()) {
            customerCell.addElement(new Paragraph("Address: " + data.getCustomerAddress(), normalFont));
        }
        if (data.getCustomerGSTIN() != null && !data.getCustomerGSTIN().trim().isEmpty()) {
            customerCell.addElement(new Paragraph("GSTIN: " + data.getCustomerGSTIN(), boldFont));
        }
        if (data.getCustomerPhone() != null && !data.getCustomerPhone().trim().isEmpty()) {
            customerCell.addElement(new Paragraph("Phone: " + data.getCustomerPhone(), normalFont));
        }
        metaTable.addCell(customerCell);

        // Summary Card Cell
        PdfPCell summaryCell = new PdfPCell();
        summaryCell.setPadding(8);
        summaryCell.setBackgroundColor(lightGray);
        summaryCell.setBorderColor(new Color(229, 231, 235));

        Paragraph summaryHeader = new Paragraph("ACCOUNT STATEMENT SUMMARY", sectionHeaderFont);
        summaryHeader.setSpacingAfter(4);
        summaryCell.addElement(summaryHeader);

        PdfPTable innerSummaryTable = new PdfPTable(2);
        innerSummaryTable.setWidthPercentage(100);
        innerSummaryTable.setWidths(new float[]{55, 45});

        innerSummaryTable.addCell(createLabelCell("Opening Balance:", boldFont));
        innerSummaryTable.addCell(createValueCell(curFormat.format(data.getOpeningBalance()), normalFont, Element.ALIGN_RIGHT));

        innerSummaryTable.addCell(createLabelCell("Total Debit (Billed):", boldFont));
        innerSummaryTable.addCell(createValueCell(curFormat.format(data.getTotalDebit()), normalFont, Element.ALIGN_RIGHT));

        innerSummaryTable.addCell(createLabelCell("Total Credit (Paid):", boldFont));
        innerSummaryTable.addCell(createValueCell(curFormat.format(data.getTotalCredit()), normalFont, Element.ALIGN_RIGHT));

        innerSummaryTable.addCell(createLabelCell("Closing Balance:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, primaryColor)));
        innerSummaryTable.addCell(createValueCell(curFormat.format(data.getClosingBalance()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor), Element.ALIGN_RIGHT));

        summaryCell.addElement(innerSummaryTable);
        metaTable.addCell(summaryCell);

        document.add(metaTable);
        document.add(new Paragraph(" "));

        // 4. Ledger Transactions Table
        Paragraph itemsTitle = new Paragraph("TRANSACTION LEDGER DETAILS", sectionHeaderFont);
        itemsTitle.setSpacingAfter(6);
        document.add(itemsTitle);

        if (data.getItems() == null || data.getItems().isEmpty()) {
            Paragraph noItems = new Paragraph("No transactions posted for this customer.", italicFont);
            document.add(noItems);
        } else {
            PdfPTable itemsTable = new PdfPTable(7);
            itemsTable.setWidthPercentage(100);
            itemsTable.setHeaderRows(1);
            itemsTable.setWidths(new float[]{5, 18, 15, 18, 16, 14, 14});

            // Table Header
            itemsTable.addCell(createHeaderCell("#", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Date & Time", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Type", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Remarks / Description", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Debit (₹)", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Credit (₹)", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Balance (₹)", boldFont, primaryColor));

            int count = 1;
            for (CustomerLedgerPrintDTO.CustomerLedgerItemDTO item : data.getItems()) {
                String dateStr = item.getTransactionDate() != null ? item.getTransactionDate().format(dtFormatter) : "";

                itemsTable.addCell(createBodyCell(String.valueOf(count++), normalFont, Element.ALIGN_CENTER));
                itemsTable.addCell(createBodyCell(dateStr, normalFont));
                itemsTable.addCell(createBodyCell(item.getTransactionType() != null ? item.getTransactionType() : "Adjustment", boldFont));
                itemsTable.addCell(createBodyCell(item.getRemarks() != null ? item.getRemarks() : "", normalFont));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getDebitAmount()), normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getCreditAmount()), normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getRunningBalance()), boldFont, Element.ALIGN_RIGHT));
            }
            document.add(itemsTable);
        }
        document.add(new Paragraph(" "));

        // 5. Footer
        Paragraph footer = new Paragraph("This is an official computer-generated Customer Ledger Statement. Zero DB mutations.", italicFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        document.add(footer);

        document.close();
    }

    private static PdfPCell createHeaderCell(String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(font.getFamilyname(), font.getSize(), Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(new Color(229, 231, 235));
        return cell;
    }

    private static PdfPCell createBodyCell(String text, Font font) {
        return createBodyCell(text, font, Element.ALIGN_LEFT);
    }

    private static PdfPCell createBodyCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(new Color(229, 231, 235));
        return cell;
    }

    private static PdfPCell createLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private static PdfPCell createValueCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3);
        return cell;
    }
}
