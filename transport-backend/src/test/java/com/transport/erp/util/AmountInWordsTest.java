package com.transport.erp.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountInWordsTest {
    private static String w(String v) { return AmountInWords.rupees(new BigDecimal(v)); }

    @Test void sampleInvoiceTotal() { assertEquals("Rupees Forty-Three Thousand Six Hundred Eighty Only", w("43680")); }
    @Test void croreAndLakh() { assertEquals("Rupees One Crore Twenty-Five Lakh Only", w("12500000")); }
    @Test void paise() { assertEquals("Rupees Twelve Thousand Four Hundred and Fifty Paise Only", w("12400.50")); }
    @Test void zero() { assertEquals("Rupees Zero Only", w("0")); }
    @Test void teens() { assertEquals("Rupees Nineteen Lakh Eleven Thousand Fifteen Only", w("1911015")); }
    @Test void exactLakhBoundary() { assertEquals("Rupees One Lakh Only", w("100000")); }
    @Test void justBelowLakh() { assertEquals("Rupees Ninety-Nine Thousand Nine Hundred Ninety-Nine Only", w("99999")); }
    @Test void largeCrore() { assertEquals("Rupees One Hundred Twenty-Three Crore Forty-Five Lakh Sixty-Seven Thousand Eight Hundred Ninety Only", w("1234567890")); }
}
