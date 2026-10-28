package com.transport.erp.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.transport.erp.dto.DriverPayrollPrintDTO;

import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

/**
 * Driver salary slip — same visual style as the invoice (title bar, boxed sections): company + slip facts, driver,
 * work summary, earnings | deductions, net pay in words, daily trip earnings with trip numbers, advance account,
 * signatures; DRAFT / CANCELLED watermark. Prints the saved payroll only (no recalculation). Basic salary shows only
 * when it is above 0 (trip-based pay).
 */
public final class DriverSalarySlipPdfGenerator {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd-MM");
    private static final Color LINE = new Color(30, 30, 30), HEAD = new Color(240, 242, 246), MUTED = new Color(90, 98, 112),
            GREEN = new Color(15, 81, 50);
    private static BaseFont REG, BOLD;
    private static Font f(float s, boolean b, Color c) { return new Font(b ? BOLD : REG, s, Font.NORMAL, c); }

    private DriverSalarySlipPdfGenerator() {}

    public static void generateSalarySlipPdf(DriverPayrollPrintDTO d, OutputStream os) throws Exception {
        initFonts();
        Document doc = new Document(PageSize.A4, 28, 28, 28, 30);
        PdfWriter w = PdfWriter.getInstance(doc, os);
        String st = d.getStatus() == null ? "" : d.getStatus().toUpperCase();
        w.setPageEvent(new Mark("DRAFT".equals(st) ? "DRAFT" : "CANCELLED".equals(st) ? "CANCELLED" : null));
        doc.addTitle("Salary Slip " + nz(d.getPayrollNumber()));
        doc.open();

        // 1. Title bar
        PdfPTable title = table(25, 50, 25);
        title.addCell(cell("", f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM));
        PdfPCell t = cell("S A L A R Y   S L I P", f(14, true, Color.BLACK), Element.ALIGN_CENTER, Rectangle.TOP | Rectangle.BOTTOM);
        t.setPaddingTop(6); t.setPaddingBottom(7); title.addCell(t);
        title.addCell(cell("DRIVER COPY", f(7, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.RIGHT | Rectangle.TOP | Rectangle.BOTTOM));
        doc.add(title);

        // 2. Company + slip facts
        PdfPTable head = table(56, 44);
        PdfPCell co = box(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); co.setPadding(6);
        co.addElement(para(nz(d.getCompanyName()), f(14, true, Color.BLACK)));
        String addr = !blank(d.getBranchAddress()) ? d.getBranchAddress() : d.getCompanyAddress();
        if (!blank(addr)) co.addElement(para(addr, f(8, false, Color.BLACK)));
        String contact = join(" · ", lbl("Phone: ", d.getCompanyPhone()), d.getCompanyEmail());
        if (!contact.isEmpty()) co.addElement(para(contact, f(8, false, Color.BLACK)));
        if (!blank(d.getCompanyGSTIN())) co.addElement(para("GSTIN: " + d.getCompanyGSTIN(), f(8, true, Color.BLACK)));
        head.addCell(co);
        PdfPTable facts = table(48, 52);
        fact(facts, "SLIP NO.", d.getPayrollNumber(), true, true);
        fact(facts, "PAY PERIOD", period(d), true, false);
        fact(facts, "PAY DATE", date(d.getPaidDate()), false, true);
        fact(facts, "PAID BY", join(" · ", pretty(d.getPaymentMethod()), lbl("Ref ", d.getPaymentReference())), false, false);
        fact(facts, "STATUS", pretty(d.getStatus()), false, true);
        fact(facts, "VOUCHERS", join(" · ", d.getAccrualJvNumber(), d.getPaymentJvNumber()), false, false);
        PdfPCell fc = new PdfPCell(facts); fc.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT); fc.setBorderColor(LINE); fc.setBorderWidth(0.8f); fc.setPadding(0);
        head.addCell(fc);
        doc.add(head);

        // 3. Driver
        PdfPTable drv = table(34, 33, 33);
        drv.addCell(info("DRIVER", join("  ", nz(d.getDriverName()), blank(d.getDriverCode()) ? "" : "(" + d.getDriverCode() + ")"), lbl("Phone: ", d.getDriverPhone()), Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT));
        drv.addCell(info("LICENCE", nz(d.getLicenseNumber()), d.getLicenseExpiryDate() == null ? "" : "Valid till " + date(d.getLicenseExpiryDate()), Rectangle.BOTTOM | Rectangle.RIGHT));
        drv.addCell(info("REGULAR VEHICLE", nz(d.getRegularVehicle()), blank(d.getBranchName()) ? "" : "Branch: " + d.getBranchName(), Rectangle.BOTTOM | Rectangle.RIGHT));
        doc.add(drv);

        // 4. Work summary
        List<DriverPayrollPrintDTO.DayLine> days = d.getDays() == null ? List.of() : d.getDays();
        long multi = days.stream().filter(x -> x.getTripCount() != null && x.getTripCount() >= 2).count();
        PdfPTable sum = table(25, 25, 25, 25);
        summary(sum, String.valueOf(d.getTripDays() == null ? days.size() : d.getTripDays()), "days with trips", true);
        summary(sum, String.valueOf(d.getTotalTrips() == null ? 0 : d.getTotalTrips()), "completed trips", true);
        summary(sum, String.valueOf(multi), "days with 2+ trips", true);
        summary(sum, "₹ " + money(d.getTripEarnings()), "trip earnings", false);
        doc.add(sum);

        // 5. Earnings | Deductions
        PdfPTable ed = table(34, 16, 34, 16);
        for (String h : new String[]{"EARNINGS", "AMOUNT (₹)", "DEDUCTIONS", "AMOUNT (₹)"}) {
            PdfPCell c = cell(h, f(7, true, Color.BLACK), h.startsWith("AMOUNT") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, Rectangle.BOTTOM);
            c.setBackgroundColor(HEAD); ed.addCell(c);
        }
        java.util.List<String[]> earn = new java.util.ArrayList<>();
        earn.add(new String[]{"Trip earnings (daily pay slabs)", money(d.getTripEarnings())});
        if (z(d.getBasicSalary()).signum() > 0) earn.add(new String[]{"Basic salary", money(d.getBasicSalary())});
        if (z(d.getAllowanceAmount()).signum() > 0) earn.add(new String[]{"Other allowance", money(d.getAllowanceAmount())});
        java.util.List<String[]> ded = new java.util.ArrayList<>();
        if (d.getDeductions() != null) for (var x : d.getDeductions()) {
            ded.add(new String[]{join(" – ", pretty(x.getDeductionType()), x.getRemarks()) + (x.getDeductionDate() == null ? "" : " (" + x.getDeductionDate().format(DM) + ")"), money(x.getAmount())});
        }
        if (z(d.getAdvanceAdjustment()).signum() > 0) ded.add(new String[]{"Advance recovery", money(d.getAdvanceAdjustment())});
        int n = Math.max(earn.size(), Math.max(ded.size(), 1));
        for (int i = 0; i < n; i++) {
            String[] e = i < earn.size() ? earn.get(i) : new String[]{"", ""};
            String[] x = i < ded.size() ? ded.get(i) : new String[]{i == 0 ? "No deductions" : "", ""};
            ed.addCell(cell(e[0], f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.NO_BORDER));
            PdfPCell a = cell(e[1], f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.RIGHT); ed.addCell(a);
            ed.addCell(cell(x[0], f(8, false, i < ded.size() ? Color.BLACK : MUTED), Element.ALIGN_LEFT, Rectangle.NO_BORDER));
            ed.addCell(cell(x[1], f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.NO_BORDER));
        }
        BigDecimal totalDed = z(d.getTotalDeductions()).signum() > 0 ? d.getTotalDeductions() : z(d.getDeductionAmount()).add(z(d.getAdvanceAdjustment()));
        ed.addCell(cell("Gross earnings", f(8.5f, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.TOP));
        ed.addCell(cell(money(d.getGrossEarnings()), f(8.5f, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.TOP | Rectangle.RIGHT));
        ed.addCell(cell("Total deductions", f(8.5f, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.TOP));
        ed.addCell(cell(money(totalDed), f(8.5f, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.TOP));
        PdfPCell edc = new PdfPCell(ed); edc.setBorder(Rectangle.BOX); edc.setBorderColor(LINE); edc.setBorderWidth(0.8f); edc.setPadding(0);
        PdfPTable edw = table(100); edw.addCell(edc); edw.setSpacingBefore(0); doc.add(edw);

        // 6. Net pay
        PdfPTable net = table(60, 40);
        PdfPCell words = box(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT); words.setPadding(6);
        words.addElement(para("NET PAY IN WORDS", f(6.5f, false, MUTED)));
        words.addElement(para(AmountInWords.rupees(z(d.getNetSalaryPayable())), f(9.5f, true, Color.BLACK)));
        net.addCell(words);
        PdfPCell np = new PdfPCell(new Phrase("NET PAY   ₹ " + money(d.getNetSalaryPayable()), f(14, true, GREEN)));
        np.setBorder(Rectangle.BOTTOM | Rectangle.RIGHT); np.setBorderColor(LINE); np.setBorderWidth(0.8f);
        np.setHorizontalAlignment(Element.ALIGN_RIGHT); np.setVerticalAlignment(Element.ALIGN_MIDDLE); np.setPadding(6);
        net.addCell(np);
        doc.add(net);
        if (z(d.getBataPaid()).signum() > 0) {
            Paragraph b = new Paragraph("Driver bata already paid during the month (for information, not in net pay): ₹ " + money(d.getBataPaid()), f(7, false, MUTED));
            b.setSpacingBefore(3); doc.add(b);
        }

        // 7. Daily trip earnings
        Paragraph dh = new Paragraph();
        dh.add(new Chunk("DAILY TRIP EARNINGS  ", f(8, true, Color.BLACK)));
        if (!blank(d.getSlabRule())) dh.add(new Chunk("(" + d.getSlabRule() + ")", f(7, false, MUTED)));
        dh.setSpacingBefore(8); dh.setSpacingAfter(3); doc.add(dh);
        PdfPTable dt = table(9, 7, 44, 14, 11, 15);
        dt.setHeaderRows(1);
        for (String h : new String[]{"DATE", "TRIPS", "TRIP NUMBERS", "VEHICLE", "SLAB", "DAY PAY (₹)"}) {
            PdfPCell c = cell(h, f(7, true, Color.BLACK), "DAY PAY (₹)".equals(h) ? Element.ALIGN_RIGHT : "TRIPS".equals(h) ? Element.ALIGN_CENTER : Element.ALIGN_LEFT, Rectangle.BOX);
            c.setBackgroundColor(HEAD); dt.addCell(c);
        }
        int tripsTotal = 0;
        for (var x : days) {
            tripsTotal += x.getTripCount() == null ? 0 : x.getTripCount();
            dt.addCell(cell(x.getWorkDate() == null ? "" : x.getWorkDate().format(DM), f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
            dt.addCell(cell(String.valueOf(x.getTripCount() == null ? 0 : x.getTripCount()), f(8, false, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
            dt.addCell(cell(nz(x.getTripNumbers()), f(7, false, MUTED), Element.ALIGN_LEFT, Rectangle.BOX));
            dt.addCell(cell(nz(x.getVehicles()), f(7.5f, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
            dt.addCell(cell(nz(x.getSlab()), f(7.5f, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
            dt.addCell(cell(money(x.getDailyAmount()), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        }
        if (days.isEmpty()) {
            PdfPCell none = cell("No completed trips in this period", f(8, false, MUTED), Element.ALIGN_CENTER, Rectangle.BOX); none.setColspan(6); dt.addCell(none);
        }
        dt.addCell(cell("Total", f(8.5f, true, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        dt.addCell(cell(String.valueOf(tripsTotal), f(8.5f, true, Color.BLACK), Element.ALIGN_CENTER, Rectangle.BOX));
        PdfPCell gap = cell("", f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX); gap.setColspan(3); dt.addCell(gap);
        dt.addCell(cell(money(d.getTripEarnings()), f(8.5f, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        doc.add(dt);

        // 8. Advance account
        PdfPTable ad = table(34, 22, 22, 22);
        ad.setSpacingBefore(8);
        for (String h : new String[]{"ADVANCE ACCOUNT", "GIVEN IN THIS PERIOD", "RECOVERED IN THIS SLIP", "OUTSTANDING TODAY"}) {
            PdfPCell c = cell(h, f(7, true, Color.BLACK), "ADVANCE ACCOUNT".equals(h) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT, Rectangle.BOX);
            c.setBackgroundColor(HEAD); ad.addCell(c);
        }
        ad.addCell(cell("Driver advance", f(8, false, Color.BLACK), Element.ALIGN_LEFT, Rectangle.BOX));
        ad.addCell(cell(money(d.getAdvanceGivenInPeriod()), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        ad.addCell(cell(money(d.getAdvanceAdjustment()), f(8, false, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        ad.addCell(cell(money(d.getAdvanceOutstandingNow()), f(8, true, Color.BLACK), Element.ALIGN_RIGHT, Rectangle.BOX));
        doc.add(ad);

        // 9. Signatures
        PdfPTable sig = table(50, 50);
        sig.setSpacingBefore(8);
        sig.setKeepTogether(true);
        PdfPCell s1 = box(Rectangle.BOX); s1.setPadding(6); s1.setMinimumHeight(70);
        s1.addElement(para("DRIVER", f(6.5f, false, MUTED)));
        s1.addElement(new Paragraph(" ", f(22, false, Color.BLACK)));
        s1.addElement(para("Signature of " + nz(d.getDriverName()), f(8.5f, false, Color.BLACK)));
        sig.addCell(s1);
        PdfPCell s2 = box(Rectangle.TOP | Rectangle.RIGHT | Rectangle.BOTTOM); s2.setPadding(6);
        Paragraph forCo = new Paragraph(); forCo.setAlignment(Element.ALIGN_RIGHT);
        forCo.add(new Chunk("For ", f(9, false, Color.BLACK))); forCo.add(new Chunk(nz(d.getCompanyName()), f(9, true, Color.BLACK)));
        s2.addElement(forCo);
        s2.addElement(new Paragraph(" ", f(22, false, Color.BLACK)));
        Paragraph au = new Paragraph("Authorised Signatory", f(9, false, Color.BLACK)); au.setAlignment(Element.ALIGN_RIGHT); s2.addElement(au);
        sig.addCell(s2);
        doc.add(sig);
        Paragraph foot = new Paragraph("This is a computer-generated salary slip. Pay is calculated from completed trips (by trip date) and the company's daily pay slabs.", f(7, false, MUTED));
        foot.setAlignment(Element.ALIGN_CENTER); foot.setSpacingBefore(4); doc.add(foot);
        doc.close();
    }

    // ------------------------------------------------------------------ helpers
    private static final class Mark extends PdfPageEventHelper {
        private final String m; Mark(String m) { this.m = m; }
        @Override public void onEndPage(PdfWriter w, Document doc) {
            if (m == null) return;
            PdfContentByte cb = w.getDirectContentUnder(); cb.saveState();
            PdfGState gs = new PdfGState(); gs.setFillOpacity(0.12f); cb.setGState(gs);
            cb.beginText(); cb.setFontAndSize(BOLD, 90); cb.setColorFill(new Color(200, 30, 30));
            cb.showTextAligned(Element.ALIGN_CENTER, m, doc.getPageSize().getWidth() / 2, doc.getPageSize().getHeight() / 2, 45);
            cb.endText(); cb.restoreState();
        }
    }
    private static synchronized void initFonts() {
        if (REG != null) return;
        try {
            REG = BaseFont.createFont("DejaVuSansCondensed.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("/fonts/DejaVuSansCondensed.ttf"), null);
            BOLD = BaseFont.createFont("DejaVuSansCondensed-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("/fonts/DejaVuSansCondensed-Bold.ttf"), null);
        } catch (Exception e) {
            try { REG = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false); BOLD = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false); }
            catch (Exception ignored) { }
        }
    }
    private static byte[] bytes(String r) throws Exception {
        try (InputStream in = DriverSalarySlipPdfGenerator.class.getResourceAsStream(r)) {
            if (in == null) throw new IllegalStateException("missing " + r); return in.readAllBytes();
        }
    }
    private static PdfPTable table(float... w) {
        PdfPTable t = new PdfPTable(w.length); t.setWidthPercentage(100);
        try { t.setWidths(w); } catch (DocumentException ignored) { }
        return t;
    }
    private static PdfPCell box(int border) { PdfPCell c = new PdfPCell(); c.setBorder(border); c.setBorderColor(LINE); c.setBorderWidth(0.8f); return c; }
    private static PdfPCell cell(String text, Font font, int align, int border) {
        PdfPCell c = new PdfPCell(new Phrase(nz(text), font)); c.setHorizontalAlignment(align); c.setPadding(4);
        c.setBorder(border); c.setBorderColor(LINE); c.setBorderWidth(0.6f); return c;
    }
    private static Paragraph para(String t, Font font) { Paragraph p = new Paragraph(nz(t), font); p.setLeading(font.getSize() * 1.25f); return p; }
    private static void fact(PdfPTable t, String l, String v, boolean bold, boolean left) {
        PdfPCell c = new PdfPCell(); c.setBorder(Rectangle.BOTTOM | (left ? Rectangle.RIGHT : 0)); c.setBorderColor(LINE); c.setBorderWidth(0.6f); c.setPadding(5);
        c.addElement(para(l, f(6.5f, false, MUTED))); c.addElement(para(blank(v) ? " " : v, f(bold ? 9 : 8, bold, Color.BLACK))); t.addCell(c);
    }
    private static PdfPCell info(String label, String line1, String line2, int border) {
        PdfPCell c = box(border); c.setPadding(5);
        c.addElement(para(label, f(6.5f, false, MUTED))); c.addElement(para(line1, f(8.5f, true, Color.BLACK)));
        if (!blank(line2)) c.addElement(para(line2, f(7.5f, false, Color.BLACK)));
        return c;
    }
    private static void summary(PdfPTable t, String big, String small, boolean divider) {
        PdfPCell c = new PdfPCell(); c.setBackgroundColor(HEAD); c.setBorder(Rectangle.BOTTOM | (divider ? Rectangle.RIGHT : 0) | (t.getRows().isEmpty() && t.getNumberOfColumns() > 0 && c.getColspan() == 1 ? 0 : 0));
        c.setBorderColor(LINE); c.setBorderWidth(0.6f); c.setPadding(5);
        Paragraph p1 = new Paragraph(big, f(11, true, Color.BLACK)); p1.setAlignment(Element.ALIGN_CENTER); c.addElement(p1);
        Paragraph p2 = new Paragraph(small, f(7, false, MUTED)); p2.setAlignment(Element.ALIGN_CENTER); c.addElement(p2);
        t.addCell(c);
    }
    private static String period(DriverPayrollPrintDTO d) {
        if (d.getPayYear() == null || d.getPayMonth() == null) return nz(d.getPayPeriod());
        YearMonth ym = YearMonth.of(d.getPayYear(), d.getPayMonth());
        return ym.atDay(1).format(DMY) + " to " + ym.atEndOfMonth().format(DMY);
    }
    private static String pretty(String s) {
        if (blank(s)) return "";
        String x = s.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(x.charAt(0)) + x.substring(1);
    }
    private static String lbl(String p, String v) { return blank(v) ? "" : p + v; }
    private static String join(String sep, String... parts) {
        StringJoiner j = new StringJoiner(sep); for (String p : parts) if (!blank(p)) j.add(p.trim()); return j.toString();
    }
    private static String date(LocalDate d) { return d == null ? "" : d.format(DMY); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static BigDecimal z(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String money(BigDecimal v) { return SalesInvoicePdfGenerator.money(z(v).setScale(2, RoundingMode.HALF_UP)); }
}
