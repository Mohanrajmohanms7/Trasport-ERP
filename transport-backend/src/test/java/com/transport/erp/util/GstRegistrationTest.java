package com.transport.erp.util;

import com.transport.erp.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GstRegistrationTest {
    private static final String VALID = "33ABCDE1234F1Z5";

    @Test void registeredWithValidGstin() {
        var r = GstRegistration.resolve("REGISTERED", " 33abcde1234f1z5 ", null, null);
        assertEquals("REGISTERED", r.status()); assertEquals(VALID, r.gstin());
    }
    @Test void registeredWithoutGstinRejected() {
        var e = assertThrows(BusinessValidationException.class, () -> GstRegistration.resolve("REGISTERED", "  ", null, null));
        assertTrue(e.getMessage().contains("GSTIN") || String.valueOf(e).contains("GSTIN"));
    }
    @Test void registeredWithBadFormatRejected() {
        assertThrows(BusinessValidationException.class, () -> GstRegistration.resolve("REGISTERED", "33ABC123", null, null));
    }
    @Test void unregisteredStoresNull() {
        var r = GstRegistration.resolve("UNREGISTERED", "", null, null);
        assertEquals("UNREGISTERED", r.status()); assertNull(r.gstin());
    }
    @Test void pendingStoresNull() {
        var r = GstRegistration.resolve("pending", null, null, null);
        assertEquals("PENDING", r.status()); assertNull(r.gstin());
    }
    @Test void gstinWithUnregisteredRejected() {
        assertThrows(BusinessValidationException.class, () -> GstRegistration.resolve("UNREGISTERED", VALID, null, null));
    }
    @Test void invalidStatusRejected() {
        assertThrows(BusinessValidationException.class, () -> GstRegistration.resolve("EXEMPT", null, null, null));
    }
    @Test void olderPayloadWithGstinMeansRegistered() {
        var r = GstRegistration.resolve(null, VALID, null, null);
        assertEquals("REGISTERED", r.status()); assertEquals(VALID, r.gstin());
    }
    @Test void olderPayloadWithoutGstinOnboardsUnregistered() {
        var r = GstRegistration.resolve(null, null, null, null);
        assertEquals("UNREGISTERED", r.status()); assertNull(r.gstin());
    }
    @Test void unrelatedUpdateKeepsSavedGstin() {
        var r = GstRegistration.resolve(null, null, "REGISTERED", VALID);
        assertEquals("REGISTERED", r.status()); assertEquals(VALID, r.gstin());
        var r2 = GstRegistration.resolve("REGISTERED", "", "REGISTERED", VALID);
        assertEquals(VALID, r2.gstin());
    }
    @Test void savedLegacyGstinNotRecheckedOnUnrelatedEdit() {
        var r = GstRegistration.resolve("REGISTERED", "33OLDFORMAT0001", "REGISTERED", "33OLDFORMAT0001");
        assertEquals("33OLDFORMAT0001", r.gstin());
    }
    @Test void switchingToPendingClearsGstin() {
        var r = GstRegistration.resolve("PENDING", null, "REGISTERED", VALID);
        assertEquals("PENDING", r.status()); assertNull(r.gstin());
    }
    @Test void registeringLaterAfterPending() {
        var r = GstRegistration.resolve("REGISTERED", VALID, "PENDING", null);
        assertEquals("REGISTERED", r.status()); assertEquals(VALID, r.gstin());
    }
}
