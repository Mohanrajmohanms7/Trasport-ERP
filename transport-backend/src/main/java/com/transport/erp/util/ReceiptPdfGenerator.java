package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.CustomerReceiptPrintDTO;
import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptPdfGenerator {

    public static void generateReceiptPdf(CustomerReceiptPrintDTO data, OutputStream os) throws Exception {
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
        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryColor);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textDark);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, textDark);
        Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
        Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.RED);

        // Currency formatter
        NumberFormat curFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Date Formatters
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

        // 1. CANCELLED Watermark / Title if cancelled
        if (Boolean.TRUE.equals(data.getIsCancelled()) || "CANCELLED".equals(data.getStatus())) {
            Paragraph cancelHeader = new Paragraph("CANCELLED", warningFont);
            cancelHeader.setAlignment(Element.ALIGN_CENTER);
            cancelHeader.setSpacingAfter(10);
            document.add(cancelHeader);
        } else if ("DRAFT".equals(data.getStatus())) {
            Paragraph draftHeader = new Paragraph("DRAFT RECEIPT (PROVISIONAL)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.ORANGE));
            draftHeader.setAlignment(Element.ALIGN_CENTER);
            draftHeader.setSpacingAfter(10);
            document.add(draftHeader);
        }

        // 2. Company & Branch Header Table
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{50, 50});

        // Left: Company Info
        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.addElement(new Paragraph(data.getCompanyName() != null ? data.getCompanyName().toUpperCase() : "COMPANY NAME", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor)));
        compCell.addElement(new Paragraph(data.getCompanyAddress() != null ? data.getCompanyAddress() : "", normalFont));
        if (data.getCompanyPhone() != null) compCell.addElement(new Paragraph("Phone: " + data.getCompanyPhone(), normalFont));
        if (data.getCompanyEmail() != null) compCell.addElement(new Paragraph("Email: " + data.getCompanyEmail(), normalFont));
        if (data.getCompanyGSTIN() != null) compCell.addElement(new Paragraph("GSTIN: " + data.getCompanyGSTIN(), boldFont));
        headerTable.addCell(compCell);

        // Right: Branch Info
        PdfPCell branchCell = new PdfPCell();
        branchCell.setBorder(Rectangle.NO_BORDER);
        branchCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph bTitle = new Paragraph("BRANCH DETAILS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, primaryColor));
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
        if (data.getBranchPhone() != null) {
            Paragraph bPhone = new Paragraph("Contact: " + data.getBranchPhone(), normalFont);
            bPhone.setAlignment(Element.ALIGN_RIGHT);
            branchCell.addElement(bPhone);
        }
        headerTable.addCell(branchCell);
        document.add(headerTable);

        // Line separator
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100, primaryColor, Element.ALIGN_CENTER, -5)));
        document.add(new Paragraph(" "));

        // 3. Document Title & Main Meta Table
        Paragraph docTitle = new Paragraph("CUSTOMER PAYMENT RECEIPT", titleFont);
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(15);
        document.add(docTitle);

        PdfPTable metaTable = new PdfPTable(4);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{25, 25, 25, 25});

        // Row 1
        metaTable.addCell(createLabelCell("Receipt Number:", boldFont));
        metaTable.addCell(createValueCell(data.getReceiptNumber(), normalFont));
        metaTable.addCell(createLabelCell("Receipt Date:", boldFont));
        metaTable.addCell(createValueCell(data.getReceiptDate() != null ? data.getReceiptDate().toString() : "", normalFont));

        // Row 2
        metaTable.addCell(createLabelCell("Reference Number:", boldFont));
        metaTable.addCell(createValueCell(data.getReferenceNumber() != null ? data.getReferenceNumber() : "N/A", normalFont));
        metaTable.addCell(createLabelCell("Status:", boldFont));
        metaTable.addCell(createValueCell(data.getStatus(), boldFont));

        // Row 3
        metaTable.addCell(createLabelCell("Customer Name:", boldFont));
        metaTable.addCell(createValueCell(data.getCustomerName(), normalFont));
        metaTable.addCell(createLabelCell("Customer Code:", boldFont));
        metaTable.addCell(createValueCell(data.getCustomerCode() != null ? data.getCustomerCode() : "N/A", normalFont));

        // Row 4
        metaTable.addCell(createLabelCell("GSTIN:", boldFont));
        metaTable.addCell(createValueCell(data.getCustomerGSTIN() != null ? data.getCustomerGSTIN() : "N/A", normalFont));
        metaTable.addCell(createLabelCell("Payment Method:", boldFont));
        metaTable.addCell(createValueCell(data.getPaymentMethod(), normalFont));

        document.add(metaTable);
        document.add(new Paragraph(" "));

        // 4. Payment Summary Cards
        Paragraph pSummaryTitle = new Paragraph("PAYMENT SUMMARY", sectionHeaderFont);
        pSummaryTitle.setSpacingAfter(8);
        document.add(pSummaryTitle);

        PdfPTable summaryTable = new PdfPTable(3);
        summaryTable.setWidthPercentage(100);
        summaryTable.setWidths(new float[]{33.3f, 33.3f, 33.3f});

        summaryTable.addCell(createSummaryCard("Amount Received", curFormat.format(data.getAmountReceived()), lightGray, boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor)));
        summaryTable.addCell(createSummaryCard("Allocated Amount", curFormat.format(data.getTotalAllocated()), lightGray, boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor)));
        summaryTable.addCell(createSummaryCard("Advance / Unallocated", curFormat.format(data.getTotalAdvance()), lightGray, boldFont, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor)));

        document.add(summaryTable);
        document.add(new Paragraph(" "));

        // 5. Invoices Allocation Table
        Paragraph allocTitle = new Paragraph("INVOICE ALLOCATIONS", sectionHeaderFont);
        allocTitle.setSpacingAfter(8);
        document.add(allocTitle);

        if (data.getAllocations() == null || data.getAllocations().isEmpty()) {
            Paragraph noAlloc = new Paragraph("No invoice allocations for this receipt voucher.", italicFont);
            document.add(noAlloc);
        } else {
            PdfPTable allocTable = new PdfPTable(7);
            allocTable.setWidthPercentage(100);
            allocTable.setWidths(new float[]{18, 14, 14, 14, 14, 14, 12});

            // Headers
            allocTable.addCell(createHeaderCell("Invoice Number", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Invoice Date", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Invoice Total", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Prev Paid", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Allocated Now", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Paid After", boldFont, primaryColor));
            allocTable.addCell(createHeaderCell("Status", boldFont, primaryColor));

            for (CustomerReceiptPrintDTO.AllocationDetailDTO alloc : data.getAllocations()) {
                allocTable.addCell(createBodyCell(alloc.getInvoiceNumber(), normalFont));
                allocTable.addCell(createBodyCell(alloc.getInvoiceDate() != null ? alloc.getInvoiceDate().toString() : "", normalFont));
                allocTable.addCell(createBodyCell(curFormat.format(alloc.getInvoiceTotal()), normalFont, Element.ALIGN_RIGHT));
                allocTable.addCell(createBodyCell(curFormat.format(alloc.getInvoicePaidAmount()), normalFont, Element.ALIGN_RIGHT));
                allocTable.addCell(createBodyCell(curFormat.format(alloc.getAllocatedAmount()), boldFont, Element.ALIGN_RIGHT));
                allocTable.addCell(createBodyCell(curFormat.format(alloc.getPaidAfterReceipt()), normalFont, Element.ALIGN_RIGHT));
                allocTable.addCell(createBodyCell(alloc.getPaymentStatus(), normalFont));
            }
            document.add(allocTable);
        }
        document.add(new Paragraph(" "));

        // 6. Accounting Information
        Paragraph acctTitle = new Paragraph("ACCOUNTING INFORMATION", sectionHeaderFont);
        acctTitle.setSpacingAfter(8);
        document.add(acctTitle);

        if ("DRAFT".equals(data.getStatus())) {
            Paragraph draftNote = new Paragraph("Accounting posting pending approval.", italicFont);
            document.add(draftNote);
        } else {
            PdfPTable acctTable = new PdfPTable(3);
            acctTable.setWidthPercentage(100);
            acctTable.setWidths(new float[]{30, 35, 35});

            acctTable.addCell(createHeaderCell("JV Reference", boldFont, primaryColor));
            acctTable.addCell(createHeaderCell("Debit Account", boldFont, primaryColor));
            acctTable.addCell(createHeaderCell("Credit Account", boldFont, primaryColor));

            acctTable.addCell(createBodyCell(data.getJournalVoucherReference() != null ? data.getJournalVoucherReference() : "N/A", boldFont));
            acctTable.addCell(createBodyCell(data.getDebitAccount() != null ? data.getDebitAccount() : "N/A", normalFont));
            acctTable.addCell(createBodyCell(data.getCreditAccount() != null ? data.getCreditAccount() : "N/A", normalFont));
            document.add(acctTable);
        }
        document.add(new Paragraph(" "));

        // Remarks
        if (data.getRemarks() != null && !data.getRemarks().isBlank()) {
            Paragraph remarksLabel = new Paragraph("Remarks: ", boldFont);
            remarksLabel.add(new Chunk(data.getRemarks(), normalFont));
            remarksLabel.setSpacingAfter(15);
            document.add(remarksLabel);
        }

        // 7. Footer & Sign-off Details
        document.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.5f, 100, Color.LIGHT_GRAY, Element.ALIGN_CENTER, 0)));
        document.add(new Paragraph(" "));

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);
        footerTable.setWidths(new float[]{60, 40});

        PdfPCell fLeft = new PdfPCell();
        fLeft.setBorder(Rectangle.NO_BORDER);
        if (data.getApprovedBy() != null) {
            fLeft.addElement(new Paragraph("Approved By: " + data.getApprovedBy(), normalFont));
            fLeft.addElement(new Paragraph("Approved At: " + data.getApprovedAt().format(dtFormatter), normalFont));
        }
        if (Boolean.TRUE.equals(data.getIsCancelled())) {
            fLeft.addElement(new Paragraph("Cancelled By: " + data.getCancelledBy(), boldFont));
            fLeft.addElement(new Paragraph("Cancelled At: " + data.getCancelledAt().format(dtFormatter), boldFont));
        }
        footerTable.addCell(fLeft);

        PdfPCell fRight = new PdfPCell();
        fRight.setBorder(Rectangle.NO_BORDER);
        fRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph sysGen = new Paragraph("System-Generated Financial Document", italicFont);
        sysGen.setAlignment(Element.ALIGN_RIGHT);
        fRight.addElement(sysGen);
        Paragraph printedTime = new Paragraph("Printed At: " + LocalDateTime.now().format(dtFormatter), italicFont);
        printedTime.setAlignment(Element.ALIGN_RIGHT);
        fRight.addElement(printedTime);
        footerTable.addCell(fRight);

        document.add(footerTable);

        document.close();
    }

    private static PdfPCell createLabelCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setBackgroundColor(new Color(249, 250, 251));
        cell.setPadding(6);
        return cell;
    }

    private static PdfPCell createValueCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "", font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(6);
        return cell;
    }

    private static PdfPCell createHeaderCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.WHITE);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell createBodyCell(String text, Font font) {
        return createBodyCell(text, font, Element.ALIGN_LEFT);
    }

    private static PdfPCell createBodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private static PdfPCell createSummaryCard(String title, String value, Color bg, Font titleFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pTitle = new Paragraph(title.toUpperCase(), titleFont);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pTitle);

        Paragraph pVal = new Paragraph(value, valFont);
        pVal.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pVal);

        return cell;
    }
}
