package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.SalesInvoicePrintDTO;
import java.awt.Color;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SalesInvoicePdfGenerator {

    public static void generateSalesInvoicePdf(SalesInvoicePrintDTO data, OutputStream os) throws Exception {
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
        Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.RED);

        // Currency formatter
        NumberFormat curFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

        // Date Formatter
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        // 1. Status Watermark / Header Banner
        if ("CANCELLED".equals(data.getStatus())) {
            Paragraph cancelHeader = new Paragraph("CANCELLED INVOICE", warningFont);
            cancelHeader.setAlignment(Element.ALIGN_CENTER);
            cancelHeader.setSpacingAfter(10);
            document.add(cancelHeader);
        } else if ("DRAFT".equals(data.getStatus())) {
            Paragraph draftHeader = new Paragraph("DRAFT TAX INVOICE (PROVISIONAL)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.ORANGE));
            draftHeader.setAlignment(Element.ALIGN_CENTER);
            draftHeader.setSpacingAfter(10);
            document.add(draftHeader);
        }

        // 2. Company & Branch Header Table
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
        if (data.getCompanyPAN() != null && !data.getCompanyPAN().trim().isEmpty()) {
            compCell.addElement(new Paragraph("PAN: " + data.getCompanyPAN(), normalFont));
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

        // 3. Document Title
        Paragraph docTitle = new Paragraph("TAX INVOICE", titleFont);
        docTitle.setAlignment(Element.ALIGN_CENTER);
        docTitle.setSpacingAfter(12);
        document.add(docTitle);

        // 4. Two-Column Customer & Invoice Meta Table
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{50, 50});

        // Left Column: Customer Bill-To Info
        PdfPCell customerCell = new PdfPCell();
        customerCell.setPadding(8);
        customerCell.setBackgroundColor(lightGray);
        customerCell.setBorderColor(new Color(229, 231, 235));

        Paragraph billToHeader = new Paragraph("BILL TO (CUSTOMER DETAILS)", sectionHeaderFont);
        billToHeader.setSpacingAfter(4);
        customerCell.addElement(billToHeader);

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
        if (data.getCustomerEmail() != null && !data.getCustomerEmail().trim().isEmpty()) {
            customerCell.addElement(new Paragraph("Email: " + data.getCustomerEmail(), normalFont));
        }
        metaTable.addCell(customerCell);

        // Right Column: Invoice Reference Details
        PdfPCell invMetaCell = new PdfPCell();
        invMetaCell.setPadding(8);
        invMetaCell.setBackgroundColor(lightGray);
        invMetaCell.setBorderColor(new Color(229, 231, 235));

        Paragraph invMetaHeader = new Paragraph("INVOICE REFERENCE", sectionHeaderFont);
        invMetaHeader.setSpacingAfter(4);
        invMetaCell.addElement(invMetaHeader);

        PdfPTable innerMetaTable = new PdfPTable(2);
        innerMetaTable.setWidthPercentage(100);
        innerMetaTable.setWidths(new float[]{45, 55});

        innerMetaTable.addCell(createLabelCell("Invoice Number:", boldFont));
        innerMetaTable.addCell(createValueCell(data.getInvoiceNumber(), boldFont));

        innerMetaTable.addCell(createLabelCell("Invoice Date:", boldFont));
        innerMetaTable.addCell(createValueCell(data.getInvoiceDate() != null ? data.getInvoiceDate().format(dtFormatter) : "N/A", normalFont));

        innerMetaTable.addCell(createLabelCell("Payment Terms:", boldFont));
        innerMetaTable.addCell(createValueCell(data.getPaymentTerms() != null ? data.getPaymentTerms() : "N/A", normalFont));

        innerMetaTable.addCell(createLabelCell("Invoice Status:", boldFont));
        innerMetaTable.addCell(createValueCell(data.getStatus(), boldFont));

        innerMetaTable.addCell(createLabelCell("Payment Status:", boldFont));
        innerMetaTable.addCell(createValueCell(data.getPaymentStatus(), boldFont));

        invMetaCell.addElement(innerMetaTable);
        metaTable.addCell(invMetaCell);

        document.add(metaTable);
        document.add(new Paragraph(" "));

        // 5. Line Items Table
        Paragraph itemsTitle = new Paragraph("BILLING ITEM DETAILS", sectionHeaderFont);
        itemsTitle.setSpacingAfter(6);
        document.add(itemsTitle);

        if (data.getItems() == null || data.getItems().isEmpty()) {
            Paragraph noItems = new Paragraph("No billing line items available.", italicFont);
            document.add(noItems);
        } else {
            PdfPTable itemsTable = new PdfPTable(9);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{5, 22, 13, 8, 10, 12, 8, 10, 12});

            // Table Header
            itemsTable.addCell(createHeaderCell("#", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Material / Description", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Trip #", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Qty", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Rate (₹)", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Charges (₹)", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("GST %", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("GST Amt (₹)", boldFont, primaryColor));
            itemsTable.addCell(createHeaderCell("Net Total (₹)", boldFont, primaryColor));

            int count = 1;
            for (SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO item : data.getItems()) {
                BigDecimal addCharges = item.getFreightCharges().add(item.getLoadingCharges()).add(item.getRoyalty());

                itemsTable.addCell(createBodyCell(String.valueOf(count++), normalFont, Element.ALIGN_CENTER));
                itemsTable.addCell(createBodyCell(item.getMaterialName() != null ? item.getMaterialName() : "General Freight", normalFont));
                itemsTable.addCell(createBodyCell(item.getTripNumber() != null ? item.getTripNumber() : "N/A", normalFont));
                itemsTable.addCell(createBodyCell(item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "0", normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getRate()), normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(addCharges), normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(item.getGstPercentage() != null ? item.getGstPercentage().stripTrailingZeros().toPlainString() + "%" : "0%", normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getLineTax()), normalFont, Element.ALIGN_RIGHT));
                itemsTable.addCell(createBodyCell(curFormat.format(item.getNetAmount()), boldFont, Element.ALIGN_RIGHT));
            }
            document.add(itemsTable);
        }
        document.add(new Paragraph(" "));

        // 6. Summary Breakdown Table (Right Aligned)
        PdfPTable summaryOuterTable = new PdfPTable(2);
        summaryOuterTable.setWidthPercentage(100);
        summaryOuterTable.setWidths(new float[]{45, 55});

        // Left Cell: Empty / Payment Notes
        PdfPCell notesCell = new PdfPCell();
        notesCell.setBorder(Rectangle.NO_BORDER);
        Paragraph notesHeader = new Paragraph("PAYMENT INSTRUCTIONS & NOTES", sectionHeaderFont);
        notesHeader.setSpacingAfter(4);
        notesCell.addElement(notesHeader);
        notesCell.addElement(new Paragraph("1. Please make payment according to the agreed payment terms.", italicFont));
        notesCell.addElement(new Paragraph("2. Quote invoice number during payment transaction.", italicFont));
        notesCell.addElement(new Paragraph("3. Cheque / NEFT / RTGS payments accepted.", italicFont));
        summaryOuterTable.addCell(notesCell);

        // Right Cell: Financial Calculations Card
        PdfPCell calcCell = new PdfPCell();
        calcCell.setBorderColor(new Color(229, 231, 235));
        calcCell.setBackgroundColor(lightGray);
        calcCell.setPadding(8);

        PdfPTable calcTable = new PdfPTable(2);
        calcTable.setWidthPercentage(100);
        calcTable.setWidths(new float[]{55, 45});

        calcTable.addCell(createLabelCell("Subtotal:", boldFont));
        calcTable.addCell(createValueCell(curFormat.format(data.getSubtotal()), normalFont, Element.ALIGN_RIGHT));

        calcTable.addCell(createLabelCell("Discount:", boldFont));
        calcTable.addCell(createValueCell(curFormat.format(data.getDiscount()), normalFont, Element.ALIGN_RIGHT));

        calcTable.addCell(createLabelCell("Taxable Amount:", boldFont));
        calcTable.addCell(createValueCell(curFormat.format(data.getTaxableAmount()), normalFont, Element.ALIGN_RIGHT));

        boolean interState = "INTER_STATE".equals(data.getSupplyType())
                || (data.getTotalIGST() != null && data.getTotalIGST().compareTo(BigDecimal.ZERO) > 0);
        if (!interState) {
            calcTable.addCell(createLabelCell("CGST:", boldFont));
            calcTable.addCell(createValueCell(curFormat.format(data.getTotalCGST()), normalFont, Element.ALIGN_RIGHT));

            calcTable.addCell(createLabelCell("SGST:", boldFont));
            calcTable.addCell(createValueCell(curFormat.format(data.getTotalSGST()), normalFont, Element.ALIGN_RIGHT));
        }

        if (data.getPlaceOfSupply() != null) {
            calcTable.addCell(createLabelCell("Place of Supply:", boldFont));
            calcTable.addCell(createValueCell(data.getPlaceOfSupply(), normalFont, Element.ALIGN_RIGHT));
        }

        if (interState) {
            calcTable.addCell(createLabelCell("IGST:", boldFont));
            calcTable.addCell(createValueCell(curFormat.format(data.getTotalIGST()), normalFont, Element.ALIGN_RIGHT));
        }

        PdfPCell grandTotalLabel = createLabelCell("GRAND TOTAL:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor));
        PdfPCell grandTotalValue = createValueCell(curFormat.format(data.getNetAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryColor), Element.ALIGN_RIGHT);
        grandTotalLabel.setBorderWidthTop(1.5f);
        grandTotalValue.setBorderWidthTop(1.5f);
        grandTotalLabel.setBorderColorTop(primaryColor);
        grandTotalValue.setBorderColorTop(primaryColor);

        calcTable.addCell(grandTotalLabel);
        calcTable.addCell(grandTotalValue);

        calcTable.addCell(createLabelCell("Amount Paid:", boldFont));
        calcTable.addCell(createValueCell(curFormat.format(data.getPaidAmount()), normalFont, Element.ALIGN_RIGHT));

        calcTable.addCell(createLabelCell("Balance Due:", boldFont));
        calcTable.addCell(createValueCell(curFormat.format(data.getBalanceDue()), boldFont, Element.ALIGN_RIGHT));

        calcCell.addElement(calcTable);
        summaryOuterTable.addCell(calcCell);

        document.add(summaryOuterTable);
        document.add(new Paragraph(" "));

        // 7. Footer
        Paragraph footer = new Paragraph("This is a computer-generated tax invoice. Zero DB mutations during export.", italicFont);
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

    private static PdfPCell createValueCell(String text, Font font) {
        return createValueCell(text, font, Element.ALIGN_LEFT);
    }

    private static PdfPCell createValueCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(3);
        return cell;
    }
}
