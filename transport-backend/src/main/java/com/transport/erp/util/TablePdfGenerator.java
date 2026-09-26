package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Landscape A4 PDF of any list: title, company, generated time, header row repeated on every page, page numbers. */
public final class TablePdfGenerator {

    private static final Color HEADER_BG = new Color(0x17, 0x22, 0x31);
    private static final Color ZEBRA = new Color(0xF3, 0xF5, 0xF8);
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private TablePdfGenerator() {
    }

    public static byte[] render(String title, String companyName, String[] headers, List<Object[]> rows) {
        return render(title, companyName, null, headers, rows);
    }

    /** subtitle: e.g. applied filters ("01-09-2026 to 30-09-2026 · Vehicle TN01"). */
    public static byte[] render(String title, String companyName, String subtitle, String[] headers, List<Object[]> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 30);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    PdfContentByte cb = w.getDirectContent();
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase("Page " + w.getPageNumber(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)),
                            d.right(), d.bottom() - 14, 0);
                }
            });
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph t = new Paragraph(title, titleFont);
            doc.add(t);
            Paragraph meta = new Paragraph((companyName == null || companyName.isBlank() ? "" : companyName + "   ")
                    + (subtitle == null || subtitle.isBlank() ? "" : subtitle + "   ")
                    + "Generated " + LocalDateTime.now().format(DT) + "   Rows: " + rows.size(), metaFont);
            meta.setSpacingAfter(8);
            doc.add(meta);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headFont));
                c.setBackgroundColor(HEADER_BG);
                c.setPadding(4);
                table.addCell(c);
            }
            int r = 0;
            for (Object[] row : rows) {
                for (int i = 0; i < headers.length; i++) {
                    Object v = i < row.length ? row[i] : null;
                    PdfPCell c = new PdfPCell(new Phrase(format(v), cellFont));
                    c.setPadding(3);
                    if (v instanceof Number) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    if (r % 2 == 1) c.setBackgroundColor(ZEBRA);
                    table.addCell(c);
                }
                r++;
            }
            if (rows.isEmpty()) {
                PdfPCell c = new PdfPCell(new Phrase("No records", cellFont));
                c.setColspan(headers.length);
                c.setPadding(6);
                table.addCell(c);
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not build PDF: " + e.getMessage(), e);
        }
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof BigDecimal b) return MONEY.format(b);
        if (v instanceof Double || v instanceof Float) return MONEY.format(((Number) v).doubleValue());
        if (v instanceof LocalDateTime dt) return dt.format(DT);
        if (v instanceof LocalDate d) return d.format(D);
        return String.valueOf(v);
    }
}
