package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.SalesInvoice;
import com.transport.erp.model.SalesInvoiceDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GST calculation for sales invoices. Pure logic, no database access.
 *
 * Per line:  taxable = quantity x (rate + freight + loading + royalty)
 * Invoice discount is spread over lines in proportion to their taxable value and
 * reduces the taxable value BEFORE GST is charged.
 * Inter-state supply -> IGST; intra-state -> CGST + SGST (half each).
 * All money is rounded to 2 decimals (HALF_UP).
 */
public final class InvoiceTaxCalculator {

    public static final String INTRA_STATE = "INTRA_STATE";
    public static final String INTER_STATE = "INTER_STATE";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Pattern GSTIN = Pattern.compile("^\\d{2}[A-Z0-9]{13}$");
    private static final Pattern STATE_CODE = Pattern.compile("^\\d{2}$");

    private InvoiceTaxCalculator() {
    }

    public static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** First two digits of a valid GSTIN, else null. */
    public static String stateCodeFromGstin(String gstin) {
        if (gstin == null) return null;
        String g = gstin.trim().toUpperCase();
        return GSTIN.matcher(g).matches() ? g.substring(0, 2) : null;
    }

    public static String normalizeStateCode(String code) {
        if (code == null) return null;
        String c = code.trim();
        if (c.isEmpty()) return null;
        if (c.length() == 1 && Character.isDigit(c.charAt(0))) c = "0" + c;
        if (!STATE_CODE.matcher(c).matches()) {
            throw new BusinessValidationException(
                    "Invalid Place of Supply",
                    "INVALID_PLACE_OF_SUPPLY",
                    "Place of supply must be a 2-digit GST state code (for example 33 for Tamil Nadu).",
                    "Enter the 2-digit state code or leave it empty to use the customer's GSTIN.");
        }
        return c;
    }

    /**
     * @param supplierStateCode state of the billing branch/company (from its GSTIN), may be null
     * @param explicitPlaceOfSupply state code chosen on the invoice, may be null
     * @param customerGstin customer GSTIN, may be null
     */
    public static void apply(SalesInvoice invoice, String supplierStateCode, String explicitPlaceOfSupply, String customerGstin) {
        String pos = normalizeStateCode(explicitPlaceOfSupply);
        if (pos == null) pos = stateCodeFromGstin(customerGstin);
        boolean interState = supplierStateCode != null && pos != null && !supplierStateCode.equals(pos);
        invoice.setPlaceOfSupply(pos != null ? pos : supplierStateCode);
        invoice.setSupplyType(interState ? INTER_STATE : INTRA_STATE);

        List<SalesInvoiceDetail> lines = invoice.getDetails();
        BigDecimal subtotal = BigDecimal.ZERO;
        if (lines != null) {
            for (SalesInvoiceDetail d : lines) {
                validateLine(d);
                BigDecimal base = nz(d.getRate()).add(nz(d.getFreightCharges())).add(nz(d.getLoadingCharges())).add(nz(d.getRoyalty()));
                BigDecimal lineTaxable = money(d.getQuantity().multiply(base));
                d.setTaxableAmount(lineTaxable); // pre-discount for now
                subtotal = subtotal.add(lineTaxable);
            }
        }

        BigDecimal discount = money(invoice.getDiscount());
        if (discount.signum() < 0) {
            throw new BusinessValidationException("Invalid Discount", "INVALID_DISCOUNT",
                    "Discount cannot be negative.", "Enter a discount of 0 or more.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessValidationException("Invalid Discount", "DISCOUNT_EXCEEDS_SUBTOTAL",
                    String.format("Discount ₹%.2f is more than the taxable subtotal ₹%.2f.", discount, subtotal),
                    "Reduce the discount so it does not exceed the invoice subtotal.");
        }

        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal discountLeft = discount;
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                SalesInvoiceDetail d = lines.get(i);
                BigDecimal preDiscount = d.getTaxableAmount();
                BigDecimal share;
                if (i == lines.size() - 1) {
                    share = discountLeft; // remainder keeps the sum exact
                } else if (subtotal.signum() == 0) {
                    share = BigDecimal.ZERO;
                } else {
                    share = money(discount.multiply(preDiscount).divide(subtotal, 10, RoundingMode.HALF_UP));
                }
                if (share.compareTo(preDiscount) > 0) share = preDiscount;
                discountLeft = discountLeft.subtract(share);

                BigDecimal taxable = preDiscount.subtract(share);
                BigDecimal tax = money(taxable.multiply(nz(d.getGstPercentage())).divide(HUNDRED, 10, RoundingMode.HALF_UP));

                d.setDiscountAmount(share);
                d.setTaxableAmount(taxable);
                if (interState) {
                    d.setIgst(tax);
                    d.setCgst(BigDecimal.ZERO.setScale(2));
                    d.setSgst(BigDecimal.ZERO.setScale(2));
                } else {
                    BigDecimal cgst = money(tax.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP));
                    d.setCgst(cgst);
                    d.setSgst(tax.subtract(cgst));
                    d.setIgst(BigDecimal.ZERO.setScale(2));
                }
                d.setNetAmount(taxable.add(tax));
                totalTax = totalTax.add(tax);
                taxableTotal = taxableTotal.add(taxable);
            }
        }

        invoice.setDiscount(subtotal.subtract(taxableTotal));
        invoice.setSubtotal(subtotal);
        invoice.setTaxableAmount(taxableTotal);
        invoice.setTaxAmount(totalTax);
        invoice.setNetAmount(taxableTotal.add(totalTax));
    }

    private static void validateLine(SalesInvoiceDetail d) {
        if (d.getMaterial() == null || d.getMaterial().getId() == null) {
            throw new BusinessValidationException("Material Required", "INVOICE_LINE_MATERIAL_REQUIRED",
                    "Every invoice line needs a material.", "Select a material on each billing row.");
        }
        if (d.getQuantity() == null || d.getQuantity().signum() <= 0) {
            throw new BusinessValidationException("Invalid Quantity", "INVOICE_LINE_QUANTITY_INVALID",
                    "Invoice line quantity must be greater than zero.", "Enter a positive quantity on each billing row.");
        }
        for (BigDecimal v : new BigDecimal[]{d.getRate(), d.getFreightCharges(), d.getLoadingCharges(), d.getRoyalty()}) {
            if (v != null && v.signum() < 0) {
                throw new BusinessValidationException("Invalid Amount", "INVOICE_LINE_NEGATIVE_AMOUNT",
                        "Rate, freight, loading and royalty cannot be negative.", "Correct the negative amount on the billing row.");
            }
        }
        d.setQuantity(d.getQuantity().setScale(2, RoundingMode.HALF_UP));
        if (d.getQuantity().signum() <= 0) {
            throw new BusinessValidationException("Invalid Quantity", "INVOICE_LINE_QUANTITY_INVALID",
                    "Invoice line quantity must be at least 0.01.", "Enter a positive quantity on each billing row.");
        }
        BigDecimal gst = nz(d.getGstPercentage());
        if (gst.signum() < 0 || gst.compareTo(BigDecimal.valueOf(28)) > 0) {
            throw new BusinessValidationException("Invalid GST Rate", "INVOICE_LINE_GST_INVALID",
                    "GST % must be between 0 and 28.", "Correct the GST % on the billing row.");
        }
        d.setRate(money(d.getRate()));
        d.setFreightCharges(money(d.getFreightCharges()));
        d.setLoadingCharges(money(d.getLoadingCharges()));
        d.setRoyalty(money(d.getRoyalty()));
        d.setGstPercentage(gst.setScale(2, RoundingMode.HALF_UP));
    }
}
