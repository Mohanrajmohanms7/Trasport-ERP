package com.transport.erp.util;

import com.transport.erp.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyShortNameTest {

    @Test
    void derivesInitials() {
        assertEquals("PKC", CompanyShortName.derive("PKC Transport"));
        assertEquals("SMT", CompanyShortName.derive("Sri Murugan Transports Pvt Ltd"));
        assertEquals("BAL", CompanyShortName.derive("Balaji"));
        assertEquals("KRL", CompanyShortName.derive("M/s Kaveri River Logistics"));
        assertNull(CompanyShortName.derive("  "));
    }

    @Test
    void normalizesEnteredValue() {
        assertEquals("PKC", CompanyShortName.normalize(" pkc ", "PKC Transport"));
        assertEquals("S&S", CompanyShortName.normalize("s&s", "S and S Movers"));
        assertEquals("PKC", CompanyShortName.normalize("", "PKC Transport"));
        assertThrows(BusinessValidationException.class, () -> CompanyShortName.normalize("P", "x"));
        assertThrows(BusinessValidationException.class, () -> CompanyShortName.normalize("TOOLONGNAME", "x"));
        assertThrows(BusinessValidationException.class, () -> CompanyShortName.normalize("P.K.C", "x"));
    }
}
