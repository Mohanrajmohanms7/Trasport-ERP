package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.Material;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.SalesInvoiceDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceTaxCalculatorTest {

    private static SalesInvoiceDetail line(String qty, String rate, String freight, String gst) {
        SalesInvoiceDetail d = new SalesInvoiceDetail();
        Material m = new Material();
        m.setId(1L);
        d.setMaterial(m);
        d.setQuantity(new BigDecimal(qty));
        d.setRate(new BigDecimal(rate));
        d.setFreightCharges(new BigDecimal(freight));
        d.setLoadingCharges(BigDecimal.ZERO);
        d.setRoyalty(BigDecimal.ZERO);
        d.setGstPercentage(new BigDecimal(gst));
        return d;
    }

    private static SalesInvoice invoice(String discount, SalesInvoiceDetail... lines) {
        SalesInvoice inv = new SalesInvoice();
        inv.setDiscount(new BigDecimal(discount));
        inv.setDetails(new ArrayList<>(java.util.List.of(lines)));
        return inv;
    }

    @Test
    void discountReducesTaxableValueBeforeGst() {
        // 10 t x (900 + 100) = 10000 taxable, discount 1000 -> 9000, GST 5% = 450
        SalesInvoice inv = invoice("1000", line("10", "900", "100", "5"));
        InvoiceTaxCalculator.apply(inv, "33", null, "33AAAAA0000A1Z5");

        assertEquals(new BigDecimal("10000.00"), inv.getSubtotal());
        assertEquals(new BigDecimal("9000.00"), inv.getTaxableAmount());
        assertEquals(new BigDecimal("450.00"), inv.getTaxAmount());
        assertEquals(new BigDecimal("9450.00"), inv.getNetAmount());
        SalesInvoiceDetail d = inv.getDetails().get(0);
        assertEquals(new BigDecimal("225.00"), d.getCgst());
        assertEquals(new BigDecimal("225.00"), d.getSgst());
        assertEquals(0, d.getIgst().signum());
        assertEquals("INTRA_STATE", inv.getSupplyType());
    }

    @Test
    void interStateCustomerGetsIgst() {
        SalesInvoice inv = invoice("0", line("2", "1000", "0", "18"));
        InvoiceTaxCalculator.apply(inv, "33", null, "29AAAAA0000A1Z5");

        SalesInvoiceDetail d = inv.getDetails().get(0);
        assertEquals("INTER_STATE", inv.getSupplyType());
        assertEquals("29", inv.getPlaceOfSupply());
        assertEquals(new BigDecimal("360.00"), d.getIgst());
        assertEquals(0, d.getCgst().signum());
        assertEquals(new BigDecimal("2360.00"), inv.getNetAmount());
    }

    @Test
    void explicitPlaceOfSupplyOverridesCustomerGstin() {
        SalesInvoice inv = invoice("0", line("1", "100", "0", "18"));
        InvoiceTaxCalculator.apply(inv, "33", "33", "29AAAAA0000A1Z5");
        assertEquals("INTRA_STATE", inv.getSupplyType());
    }

    @Test
    void discountSplitAcrossLinesSumsExactlyAndOddPaiseGoToSgst() {
        SalesInvoice inv = invoice("100", line("1", "333.33", "0", "5"), line("1", "666.67", "0", "5"));
        InvoiceTaxCalculator.apply(inv, null, null, null);

        BigDecimal shares = inv.getDetails().stream().map(SalesInvoiceDetail::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100.00"), shares);
        assertEquals(new BigDecimal("900.00"), inv.getTaxableAmount());
        for (SalesInvoiceDetail d : inv.getDetails()) {
            assertEquals(d.getTaxableAmount().add(d.getCgst()).add(d.getSgst()), d.getNetAmount());
            assertEquals(2, d.getCgst().scale());
        }
        assertEquals(inv.getTaxableAmount().add(inv.getTaxAmount()), inv.getNetAmount());
    }

    @Test
    void fractionalTonnageIsRounded() {
        SalesInvoice inv = invoice("0", line("12.345", "1000", "0", "5"));
        InvoiceTaxCalculator.apply(inv, null, null, null);
        assertEquals(new BigDecimal("12350.00"), inv.getSubtotal()); // 12.35 t after 2-dp rounding
        assertEquals(2, inv.getNetAmount().scale());
    }

    @Test
    void discountLargerThanSubtotalIsRejected() {
        SalesInvoice inv = invoice("5000", line("1", "100", "0", "5"));
        assertThrows(BusinessValidationException.class, () -> InvoiceTaxCalculator.apply(inv, null, null, null));
    }

    @Test
    void zeroQuantityIsRejected() {
        SalesInvoice inv = invoice("0", line("0", "100", "0", "5"));
        assertThrows(BusinessValidationException.class, () -> InvoiceTaxCalculator.apply(inv, null, null, null));
    }

    @Test
    void financialYearLabelsAndFormat() {
        assertEquals("2627", DocumentNumberService.financialYearLabel(LocalDate.of(2026, 4, 1)));
        assertEquals("2526", DocumentNumberService.financialYearLabel(LocalDate.of(2026, 3, 31)));
        assertEquals("9900", DocumentNumberService.financialYearLabel(LocalDate.of(2099, 12, 1)));
        assertEquals("INV-2627/00042", DocumentNumberService.format("INV-", "2627", 42));
    }
}
