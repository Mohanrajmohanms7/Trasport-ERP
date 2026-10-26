package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.SalesInvoicePrintDTO;
import com.transport.erp.dto.SalesInvoicePrintDTO.SalesInvoicePrintLineItemDTO;

import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;   // not com.lowagie.text.List

/**
 * Invoice PDF — "Design A, Classic GST" (approved layout). Prints the saved invoice exactly (no recalculation):
 * title bar, company / invoice boxes, bill-to / ship-to, item table (header repeated on every page), amount in words,
 * totals, HSN-wise tax summary, bank / terms / signatory footer, DRAFT / CANCELLED watermark.
 * Empty optional values print as blank space; nothing here can fail the download.
 */
public final class SalesInvoicePdfGenerator {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Color LINE = new Color(30, 30, 30);
    private static final Color HEAD = new Color(240, 242, 246);
    private static final Color MUTED = new Color(90, 98, 112);
    private static final float PAD = 4f;

    private static BaseFont REG, BOLD;
    private static Font f(int size, boolean bold, Color c) { return new Font(bold ? BOLD : REG, size, Font.NORMAL, c); }

    private SalesInvoicePdfGenerator() {}

    public static void generateSalesInvoicePdf(SalesInvoicePrintDTO d, OutputStream os) throws Exception {
        initFonts();
        Document doc = new Document(PageSize.A4, 28, 28, 30, 34);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        writer.setPageEvent(new PageEvents(d.isProforma() ? "PROFORMA" : watermarkFor(d.getStatus())));
        doc.addTitle("Tax Invoice " + nz(d.getInvoiceNumber()));
        doc.open();

        boolean igst = isInterState(d);
        String sellerGstin = !blank(d.getBranchGSTIN()) ? d.getBranchGSTIN() : d.getCompanyGSTIN();
        String sellerAddress = !blank(d.getBranchAddress()) ? d.getBranchAddress()
                : join(", ", d.getCompanyAddress(), d.getCompanyCity(), d.getCompanyPincode());
        String sellerState = stateLabel(sellerGstin, d.getCompanyState());

        // 1. Title bar
        PdfPTable title = table(new float[]{25, 50, 25});
        title.addCell(cell("", f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM));
        PdfPCell t = cell(d.isProforma() ? "P R O F O R M A   I N V O I C E" : "T A X   I N V O I C E", f(14, true, Color.BLACK), Element.ALIGN_CENTER, Rectangle.TOP | Rectangle.BOTTOM);
        t.setPaddingTop(6); t.setPaddingBottom(7);
        title.addCell(t);
        title.addCell(cell(d.isProforma() ? "NOT A TAX INVOICE" : "ORIGINAL FOR RECIPIENT", f(7, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.RIGHT | Rectangle.TOP | Rectangle.BOTTOM));
        doc.add(title);

        // 2-3. Company box + invoice facts
        PdfPTable head = table(new float[]{56, 44});
        PdfPCell co = new PdfPCell(); box(co, Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); co.setPadding(6);
        Image logo = logo(d.getLogoImage());
        if (logo != null) { logo.scaleToFit(70, 38); co.addElement(logo); }
        co.addElement(para(nz(d.getCompanyName()), f(14, true, Color.BLACK)));
        co.addElement(para(sellerAddress, f(8, false, Color.BLACK)));
        co.addElement(para(join(" · ", label("Phone: ", d.getCompanyPhone()), d.getCompanyEmail()), f(8, false, Color.BLACK)));
        Paragraph tax = new Paragraph();
        if (!blank(sellerGstin)) { tax.add(new Chunk("GSTIN: " + sellerGstin, f(8, true, Color.BLACK))); }
        String rest = join(" · ", label("PAN: ", d.getCompanyPAN()), label("State: ", sellerState));
        if (!rest.isEmpty()) tax.add(new Chunk((tax.isEmpty() ? "" : " · ") + rest, f(8, false, Color.BLACK)));
        co.addElement(tax);
        head.addCell(co);

        PdfPTable facts = table(new float[]{50, 50});
        fact(facts, d.isProforma() ? "PROFORMA FOR INVOICE" : "INVOICE NO.", d.getInvoiceNumber(), true, true);
        fact(facts, "DATE", date(d.getInvoiceDate()), true, false);
        fact(facts, "BOOKING / ORDER", d.getBookingNumber(), false, true);
        fact(facts, "DUE DATE", date(d.getDueDate()), false, false);
        fact(facts, "PLACE OF SUPPLY", stateLabel(d.getPlaceOfSupply(), null), false, true);
        fact(facts, "SUPPLY TYPE", igst ? "Inter-state (IGST)" : "Intra-state (CGST + SGST)", false, false);
        PdfPCell fc = new PdfPCell(facts); box(fc, Rectangle.BOTTOM | Rectangle.RIGHT); fc.setPadding(0);
        head.addCell(fc);
        doc.add(head);

        // 4. Bill to / Ship to
        PdfPTable parties = table(new float[]{58, 42});
        PdfPCell bill = new PdfPCell(); box(bill, Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); bill.setPadding(6);
        bill.addElement(para("BILL TO", f(7, false, MUTED)));
        Paragraph bn = new Paragraph(nz(d.getCustomerName()), f(9, true, Color.BLACK));
        if (!blank(d.getCustomerCode())) bn.add(new Chunk(" (" + d.getCustomerCode() + ")", f(9, false, Color.BLACK)));
        bill.addElement(bn);
        bill.addElement(para(nz(d.getCustomerAddress()), f(8, false, Color.BLACK)));
        bill.addElement(para(label("Phone: ", d.getCustomerPhone()), f(8, false, Color.BLACK)));
        Paragraph cg = new Paragraph();
        if (!blank(d.getCustomerGSTIN())) cg.add(new Chunk("GSTIN: " + d.getCustomerGSTIN(), f(8, true, Color.BLACK)));
        String cs = stateLabel(d.getCustomerGSTIN(), null);
        if (!blank(cs)) cg.add(new Chunk((cg.isEmpty() ? "" : " · ") + "State: " + cs, f(8, false, Color.BLACK)));
        bill.addElement(cg);
        parties.addCell(bill);
        PdfPCell ship = new PdfPCell(); box(ship, Rectangle.BOTTOM | Rectangle.RIGHT); ship.setPadding(6);
        ship.addElement(para("SHIP TO / DELIVERY SITE", f(7, false, MUTED)));
        ship.addElement(para(nz(d.getShipToName()), f(9, true, Color.BLACK)));
        ship.addElement(para(nz(d.getShipToAddress()), f(8, false, Color.BLACK)));
        parties.addCell(ship);
        doc.add(parties);

        // 5. Items (header row repeats on every page; rows may split, text wraps)
        PdfPTable items = table(new float[]{4, 27, 7, 7, 6, 10, 12, 5, 10, 12});   // money columns wide enough for lakh amounts
        items.setHeaderRows(1);
        items.setSplitLate(true);   // a row moves whole to the next page (splits only if taller than a page)
        for (String h : new String[]{"#", "DESCRIPTION", "HSN", "QTY", "UNIT", "RATE / UNIT", "TAXABLE", "GST", "TAX", "AMOUNT"}) {
            PdfPCell hc = cell(h, f(7, true, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX); hc.setBackgroundColor(HEAD); items.addCell(hc);
        }
        BigDecimal qtySum = BigDecimal.ZERO, taxableSum = BigDecimal.ZERO, taxSum = BigDecimal.ZERO, amountSum = BigDecimal.ZERO;
        Map<String, BigDecimal[]> hsn = new LinkedHashMap<>();   // key "hsn|rate" -> [taxable, cgst, sgst, igst]
        List<SalesInvoicePrintLineItemDTO> lines = d.getItems() == null ? List.of() : d.getItems();
        int n = 0;
        for (SalesInvoicePrintLineItemDTO it : lines) {
            n++;
            BigDecimal qty = z(it.getQuantity());
            BigDecimal perUnit = z(it.getRate()).add(z(it.getFreightCharges())).add(z(it.getLoadingCharges())).add(z(it.getRoyalty()));
            BigDecimal lineTax = z(it.getLineTax()).signum() != 0 ? z(it.getLineTax()) : z(it.getCgst()).add(z(it.getSgst())).add(z(it.getIgst()));
            BigDecimal taxable = z(it.getLineTaxable());
            BigDecimal amount = z(it.getNetAmount()).signum() != 0 ? z(it.getNetAmount()) : taxable.add(lineTax);
            items.addCell(cell(String.valueOf(n), f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            PdfPCell desc = new PdfPCell(); box(desc, Rectangle.BOX); desc.setPadding(PAD);
            desc.addElement(para(blank(it.getMaterialName()) ? "Transport charges" : it.getMaterialName(), f(9, true, Color.BLACK)));
            String tripLine = join(" · ", label("Trip ", it.getTripNumber()), date(it.getTripDate()), it.getVehicleNumber());
            if (!tripLine.isEmpty()) desc.addElement(para(tripLine, f(7, false, MUTED)));
            String parts = d.isProforma() ? "" : join(" + ", part("Material", it.getRate()), part("Transport", it.getFreightCharges()),
                    part("Loading", it.getLoadingCharges()), part("Royalty", it.getRoyalty()));
            if (!parts.isEmpty()) desc.addElement(para(parts, f(7, false, MUTED)));
            if (!d.isProforma() && "ORDERED".equals(it.getQuantityBasis()) && it.getDeliveredQuantity() != null && qty.compareTo(it.getDeliveredQuantity()) != 0) {
                desc.addElement(para("Delivered " + plain(it.getDeliveredQuantity()) + " · billed as ordered", f(7, true, new Color(150, 90, 0))));
            }
            if (z(it.getLineDiscount()).signum() > 0) desc.addElement(para("Less discount ₹" + money(it.getLineDiscount()), f(7, false, MUTED)));
            items.addCell(desc);
            items.addCell(cell(nz(it.getHsnCode()), f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            items.addCell(cell(qty2(qty), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            items.addCell(cell(nz(it.getUomLabel()), f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            items.addCell(cell(money(perUnit), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            items.addCell(cell(money(taxable), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            items.addCell(cell(plain(z(it.getGstPercentage())) + "%", f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            items.addCell(cell(money(lineTax), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            items.addCell(cell(money(amount), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            qtySum = qtySum.add(qty); taxableSum = taxableSum.add(taxable); taxSum = taxSum.add(lineTax); amountSum = amountSum.add(amount);
            BigDecimal[] h = hsn.computeIfAbsent(nz(it.getHsnCode()) + "|" + plain(z(it.getGstPercentage())),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            h[0] = h[0].add(taxable); h[1] = h[1].add(z(it.getCgst())); h[2] = h[2].add(z(it.getSgst())); h[3] = h[3].add(z(it.getIgst()));
        }
        items.addCell(cell("", f(8, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell("Total", f(9, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell("", f(8, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell(qty2(qtySum), f(8, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        items.addCell(cell("", f(8, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell("", f(8, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell(money(taxableSum), f(8, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        items.addCell(cell("", f(8, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        items.addCell(cell(money(taxSum), f(8, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        items.addCell(cell(money(amountSum), f(8, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        doc.add(items);

        // 6. Amount in words + totals (saved invoice totals)
        BigDecimal taxableValue = z(d.getTaxableAmount());
        BigDecimal cgst = z(d.getTotalCGST()), sgst = z(d.getTotalSGST()), igstAmt = z(d.getTotalIGST());
        BigDecimal grand = z(d.getNetAmount());
        BigDecimal roundOff = grand.subtract(taxableValue.add(cgst).add(sgst).add(igstAmt)).setScale(2, RoundingMode.HALF_UP);
        PdfPTable sums = table(new float[]{58, 42});
        sums.setKeepTogether(true);
        PdfPCell words = new PdfPCell(); box(words, Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); words.setPadding(6);
        words.addElement(para("AMOUNT IN WORDS", f(7, false, MUTED)));
        words.addElement(para(AmountInWords.rupees(grand), f(9, true, Color.BLACK)));
        sums.addCell(words);
        PdfPTable tot = table(new float[]{60, 40});
        if (z(d.getDiscount()).signum() > 0) {
            totalRow(tot, "Subtotal", money(z(d.getSubtotal())), false);
            totalRow(tot, "Less: Discount", money(z(d.getDiscount())), false);
        }
        totalRow(tot, "Taxable value", money(taxableValue), false);
        if (igst) totalRow(tot, "IGST", money(igstAmt), false);
        else { totalRow(tot, "CGST", money(cgst), false); totalRow(tot, "SGST", money(sgst), false); }
        totalRow(tot, "Round off", money(roundOff), false);
        totalRow(tot, "Grand total", "₹ " + money(grand), true);
        PdfPCell tc = new PdfPCell(tot); box(tc, Rectangle.BOTTOM | Rectangle.RIGHT); tc.setPadding(4);
        sums.addCell(tc);
        doc.add(sums);

        // 7. HSN-wise tax summary
        PdfPTable hs = igst ? table(new float[]{18, 28, 16, 20, 18}) : table(new float[]{12, 20, 12, 14, 12, 14, 16});
        hs.setHeaderRows(1);
        String[] hh = igst ? new String[]{"HSN", "TAXABLE", "IGST %", "IGST", "TOTAL TAX"}
                : new String[]{"HSN", "TAXABLE", "CGST %", "CGST", "SGST %", "SGST", "TOTAL TAX"};
        for (String h : hh) { PdfPCell c = cell(h, f(7, true, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX); c.setBackgroundColor(HEAD); hs.addCell(c); }
        for (Map.Entry<String, BigDecimal[]> e : hsn.entrySet()) {
            String code = e.getKey().split("\\|", -1)[0];
            BigDecimal rate = new BigDecimal(e.getKey().split("\\|", -1)[1]);
            BigDecimal[] v = e.getValue();
            hs.addCell(cell(code.isEmpty() ? "—" : code, f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            hs.addCell(cell(money(v[0]), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            if (igst) {
                hs.addCell(cell(plain(rate) + "%", f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
                hs.addCell(cell(money(v[3]), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
                hs.addCell(cell(money(v[3]), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            } else {
                String half = plain(rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) + "%";
                hs.addCell(cell(half, f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
                hs.addCell(cell(money(v[1]), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
                hs.addCell(cell(half, f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
                hs.addCell(cell(money(v[2]), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
                hs.addCell(cell(money(v[1].add(v[2])), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
            }
        }
        doc.add(hs);

        // 8. Footer: bank, terms, signatory
        PdfPTable foot = table(new float[]{58, 42});
        foot.setKeepTogether(true);
        PdfPCell bank = new PdfPCell(); box(bank, Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); bank.setPadding(6);
        bank.addElement(para("BANK DETAILS", f(7, false, MUTED)));
        bank.addElement(para(join(", ", d.getBankName(), d.getBankBranch()), f(8, false, Color.BLACK)));
        bank.addElement(para(join(" · ", label("A/c No: ", d.getBankAccountNumber()), label("IFSC: ", d.getBankIfsc())), f(8, false, Color.BLACK)));
        bank.addElement(para(label("UPI: ", d.getUpiId()), f(8, false, Color.BLACK)));
        bank.addElement(para(" ", f(4, false, Color.BLACK)));
        bank.addElement(para("TERMS", f(7, false, MUTED)));
        bank.addElement(para(nz(d.getInvoiceTerms()), f(8, false, Color.BLACK)));
        foot.addCell(bank);
        PdfPCell sign = new PdfPCell(); box(sign, Rectangle.BOTTOM | Rectangle.RIGHT); sign.setPadding(6); sign.setMinimumHeight(86);
        Paragraph forCo = new Paragraph(); forCo.setAlignment(Element.ALIGN_RIGHT);
        forCo.add(new Chunk("For ", f(9, false, Color.BLACK))); forCo.add(new Chunk(nz(d.getCompanyName()), f(9, true, Color.BLACK)));
        sign.addElement(forCo);
        Paragraph gap = new Paragraph(" ", f(26, false, Color.BLACK)); sign.addElement(gap);
        Paragraph auth = new Paragraph("Authorised Signatory", f(9, false, Color.BLACK)); auth.setAlignment(Element.ALIGN_RIGHT);
        sign.addElement(auth);
        foot.addCell(sign);
        doc.add(foot);
        if (d.isProforma()) {
            Paragraph pn = new Paragraph("PROFORMA with adjusted figures — NOT a tax invoice. Official invoice " + nz(d.getInvoiceNumber())
                    + " total: ₹" + money(z(d.getOfficialNetAmount())) + ".", f(8, true, new Color(150, 30, 30)));
            pn.setAlignment(Element.ALIGN_CENTER); pn.setSpacingBefore(6);
            doc.add(pn);
        }
        Paragraph cg2 = new Paragraph(d.isProforma() ? "This is a computer-generated proforma." : "This is a computer-generated invoice.", f(7, false, MUTED)); cg2.setAlignment(Element.ALIGN_CENTER); cg2.setSpacingBefore(4);
        doc.add(cg2);
        doc.close();
    }

    // ------------------------------------------------------------------ page events: watermark + page number
    private static final class PageEvents extends PdfPageEventHelper {
        private final String mark;
        PageEvents(String mark) { this.mark = mark; }
        @Override public void onEndPage(PdfWriter w, Document doc) {
            PdfContentByte cb = w.getDirectContentUnder();
            if (mark != null) {
                cb.saveState();
                PdfGState gs = new PdfGState(); gs.setFillOpacity(0.12f); cb.setGState(gs);
                cb.beginText(); cb.setFontAndSize(BOLD, 90); cb.setColorFill(new Color(200, 30, 30));
                cb.showTextAligned(Element.ALIGN_CENTER, mark, doc.getPageSize().getWidth() / 2, doc.getPageSize().getHeight() / 2, 45);
                cb.endText(); cb.restoreState();
            }
            PdfContentByte top = w.getDirectContent();
            ColumnText.showTextAligned(top, Element.ALIGN_RIGHT, new Phrase("Page " + w.getPageNumber(), f(7, false, MUTED)),
                    doc.right(), doc.bottom() - 14, 0);
        }
    }

    // ------------------------------------------------------------------ helpers
    private static synchronized void initFonts() {
        if (REG != null) return;
        try {
            REG = BaseFont.createFont("DejaVuSansCondensed.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("/fonts/DejaVuSansCondensed.ttf"), null);
            BOLD = BaseFont.createFont("DejaVuSansCondensed-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("/fonts/DejaVuSansCondensed-Bold.ttf"), null);
        } catch (Exception e) {
            try {   // never fail the download: fall back to the built-in fonts (₹ prints as "Rs.")
                REG = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
                BOLD = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
            } catch (Exception ignored) { }
        }
    }
    private static byte[] bytes(String res) throws Exception {
        try (InputStream in = SalesInvoicePdfGenerator.class.getResourceAsStream(res)) {
            if (in == null) throw new IllegalStateException("missing " + res);
            return in.readAllBytes();
        }
    }
    private static Image logo(byte[] b) {
        if (b == null || b.length == 0) return null;
        try { return Image.getInstance(b); } catch (Exception e) { return null; }
    }
    private static String watermarkFor(String status) {
        if (status == null) return null;
        String s = status.toUpperCase();
        return "DRAFT".equals(s) ? "DRAFT" : ("CANCELLED".equals(s) ? "CANCELLED" : null);
    }
    private static boolean isInterState(SalesInvoicePrintDTO d) {
        if (d.getSupplyType() != null) return d.getSupplyType().toUpperCase().contains("INTER");
        return z(d.getTotalIGST()).signum() > 0;
    }
    private static PdfPTable table(float[] w) {
        PdfPTable t = new PdfPTable(w.length);
        t.setWidthPercentage(100);
        try { t.setWidths(w); } catch (DocumentException ignored) { }
        return t;
    }
    private static void box(PdfPCell c, int border) { c.setBorder(border); c.setBorderColor(LINE); c.setBorderWidth(0.8f); }
    private static PdfPCell cell(String text, Font font, int align, int border) {
        PdfPCell c = new PdfPCell(new Phrase(nz(text), font));
        c.setHorizontalAlignment(align); c.setVerticalAlignment(Element.ALIGN_TOP); c.setPadding(PAD); box(c, border);
        return c;
    }
    private static Paragraph para(String text, Font font) { Paragraph p = new Paragraph(nz(text), font); p.setLeading(font.getSize() * 1.25f); return p; }
    private static void fact(PdfPTable t, String label, String value, boolean bold, boolean leftColumn) {
        PdfPCell c = new PdfPCell(); box(c, Rectangle.BOTTOM | (leftColumn ? Rectangle.RIGHT : 0)); c.setPadding(5);
        c.addElement(para(label, f(7, false, MUTED)));
        c.addElement(para(blank(value) ? " " : value, f(bold ? 9 : 8, bold, Color.BLACK)));
        t.addCell(c);
    }
    private static void totalRow(PdfPTable t, String label, String value, boolean grand) {
        PdfPCell l = new PdfPCell(new Phrase(label, f(grand ? 11 : 8, grand, Color.BLACK))); l.setBorder(Rectangle.NO_BORDER); l.setPadding(2);
        PdfPCell v = new PdfPCell(new Phrase(value, f(grand ? 11 : 8, grand, Color.BLACK))); v.setBorder(Rectangle.NO_BORDER); v.setPadding(2);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (grand) { l.setPaddingTop(5); v.setPaddingTop(5); }
        t.addCell(l); t.addCell(v);
    }
    private static String part(String name, BigDecimal v) { return v == null || v.signum() == 0 ? "" : name + " ₹" + money(v); }
    private static String label(String prefix, String v) { return blank(v) ? "" : prefix + v; }
    private static String join(String sep, String... parts) {
        StringJoiner j = new StringJoiner(sep);
        for (String p : parts) if (!blank(p)) j.add(p.trim());
        return j.toString();
    }
    private static String date(LocalDate d) { return d == null ? "" : d.format(DMY); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static BigDecimal z(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String plain(BigDecimal v) { return z(v).stripTrailingZeros().toPlainString(); }
    private static String qty2(BigDecimal v) { return z(v).setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    /** Indian grouping: 1,25,000.00 */
    static String money(BigDecimal v) {
        BigDecimal a = z(v).setScale(2, RoundingMode.HALF_UP);
        boolean neg = a.signum() < 0;
        String s = a.abs().toPlainString();
        String intPart = s.substring(0, s.indexOf('.')), dec = s.substring(s.indexOf('.'));
        StringBuilder out = new StringBuilder();
        int len = intPart.length();
        if (len <= 3) out.append(intPart);
        else {
            String head = intPart.substring(0, len - 3), tail = intPart.substring(len - 3);
            StringBuilder h = new StringBuilder();
            for (int i = head.length(); i > 0; i -= 2) h.insert(0, head.substring(Math.max(0, i - 2), i) + (h.length() > 0 ? "," : ""));
            out.append(h).append(',').append(tail);
        }
        return (neg ? "-" : "") + out + dec;
    }

    private static final Map<String, String> STATES = Map.ofEntries(
            Map.entry("01", "Jammu and Kashmir"), Map.entry("02", "Himachal Pradesh"), Map.entry("03", "Punjab"), Map.entry("04", "Chandigarh"),
            Map.entry("05", "Uttarakhand"), Map.entry("06", "Haryana"), Map.entry("07", "Delhi"), Map.entry("08", "Rajasthan"),
            Map.entry("09", "Uttar Pradesh"), Map.entry("10", "Bihar"), Map.entry("11", "Sikkim"), Map.entry("12", "Arunachal Pradesh"),
            Map.entry("13", "Nagaland"), Map.entry("14", "Manipur"), Map.entry("15", "Mizoram"), Map.entry("16", "Tripura"),
            Map.entry("17", "Meghalaya"), Map.entry("18", "Assam"), Map.entry("19", "West Bengal"), Map.entry("20", "Jharkhand"),
            Map.entry("21", "Odisha"), Map.entry("22", "Chhattisgarh"), Map.entry("23", "Madhya Pradesh"), Map.entry("24", "Gujarat"),
            Map.entry("26", "Dadra and Nagar Haveli and Daman and Diu"), Map.entry("27", "Maharashtra"), Map.entry("29", "Karnataka"),
            Map.entry("30", "Goa"), Map.entry("31", "Lakshadweep"), Map.entry("32", "Kerala"), Map.entry("33", "Tamil Nadu"),
            Map.entry("34", "Puducherry"), Map.entry("35", "Andaman and Nicobar Islands"), Map.entry("36", "Telangana"),
            Map.entry("37", "Andhra Pradesh"), Map.entry("38", "Ladakh"));

    /** "Tamil Nadu (33)" from a GSTIN or a 2-digit code; falls back to a given state name. */
    static String stateLabel(String gstinOrCode, String fallback) {
        String code = gstinOrCode == null ? "" : gstinOrCode.trim();
        if (code.length() >= 2 && Character.isDigit(code.charAt(0)) && Character.isDigit(code.charAt(1))) {
            String c = code.substring(0, 2), name = STATES.get(c);
            if (name != null) return code.length() == 2 ? c + " – " + name : name + " (" + c + ")";
        }
        return blank(fallback) ? "" : fallback;
    }
}
