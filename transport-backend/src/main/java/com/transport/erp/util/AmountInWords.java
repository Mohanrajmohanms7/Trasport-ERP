package com.transport.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Indian-system amount in words for invoices: "Rupees One Crore Twenty-Five Lakh Only", "… and Fifty Paise Only". */
public final class AmountInWords {
    private static final String[] ONES = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    private AmountInWords() {}

    public static String rupees(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        BigDecimal a = amount.abs().setScale(2, RoundingMode.HALF_UP);
        long rupees = a.longValue();
        int paise = a.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValue();
        StringBuilder sb = new StringBuilder("Rupees ");
        sb.append(rupees == 0 ? "Zero" : indian(rupees));
        if (paise > 0) sb.append(" and ").append(twoDigits(paise)).append(" Paise");
        return sb.append(" Only").toString();
    }

    /** 1,25,00,000 → "One Crore Twenty-Five Lakh"; crores above 99 are spelled in the same system. */
    static String indian(long n) {
        StringBuilder sb = new StringBuilder();
        long crore = n / 1_00_00_000L; n %= 1_00_00_000L;
        long lakh = n / 1_00_000L; n %= 1_00_000L;
        long thousand = n / 1_000L; n %= 1_000L;
        long hundred = n / 100L; long rest = n % 100L;
        if (crore > 0) append(sb, indian(crore) + " Crore");
        if (lakh > 0) append(sb, twoDigits((int) lakh) + " Lakh");
        if (thousand > 0) append(sb, twoDigits((int) thousand) + " Thousand");
        if (hundred > 0) append(sb, ONES[(int) hundred] + " Hundred");
        if (rest > 0) append(sb, twoDigits((int) rest));
        return sb.toString();
    }

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return TENS[n / 10] + (n % 10 > 0 ? "-" + ONES[n % 10] : "");
    }

    private static void append(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(part);
    }
}
