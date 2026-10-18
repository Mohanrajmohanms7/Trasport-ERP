package com.transport.erp.util;

import com.transport.erp.exception.BusinessValidationException;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Company GST registration rules (V87), shared by onboarding, Platform Admin client edit and the company profile.
 * REGISTERED needs a GSTIN in the standard format; UNREGISTERED / PENDING have no GSTIN (stored as null).
 * Older payloads without a status keep working: a GSTIN means REGISTERED; nothing given on an update keeps what is saved.
 */
public final class GstRegistration {
    public static final String REGISTERED = "REGISTERED";
    public static final String UNREGISTERED = "UNREGISTERED";
    public static final String PENDING = "PENDING";
    private static final Set<String> STATUSES = Set.of(REGISTERED, UNREGISTERED, PENDING);
    /** 2-digit state code, PAN (5 letters, 4 digits, 1 letter), entity code, 'Z', check character. */
    private static final Pattern GSTIN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    private GstRegistration() {}

    /** Result: the status and GSTIN to store. */
    public record Result(String status, String gstin) {}

    /**
     * @param requestedStatus status sent by the screen / API (may be null for older payloads)
     * @param requestedGstin  GSTIN sent (null / blank = not given)
     * @param currentStatus   saved status (null when onboarding)
     * @param currentGstin    saved GSTIN (null when onboarding)
     */
    public static Result resolve(String requestedStatus, String requestedGstin, String currentStatus, String currentGstin) {
        String gstin = requestedGstin == null || requestedGstin.isBlank() ? null : requestedGstin.trim().toUpperCase();
        String status = requestedStatus == null || requestedStatus.isBlank() ? null : requestedStatus.trim().toUpperCase();
        if (status != null && !STATUSES.contains(status)) {
            throw err("Invalid GST Registration Status", "GST_STATUS_INVALID",
                    "GST registration status must be Registered, Unregistered or Pending.", "Choose one of the three options.");
        }
        if (status == null) {   // older payload: infer, never erase on an unrelated update
            status = gstin != null ? REGISTERED : (currentStatus != null ? currentStatus : UNREGISTERED);
        }
        if (!REGISTERED.equals(status)) {
            if (gstin != null) {
                throw err("GSTIN Not Expected", "GSTIN_NOT_EXPECTED",
                        "A GSTIN can be entered only when the GST registration status is Registered.",
                        "Choose Registered to save this GSTIN, or remove the GSTIN.");
            }
            return new Result(status, null);
        }
        String toStore = gstin != null ? gstin : currentGstin;    // keep the saved GSTIN when none is sent
        if (toStore == null || toStore.isBlank()) {
            throw err("GSTIN Required", "GSTIN_REQUIRED",
                    "GSTIN is required when the GST registration status is Registered.",
                    "Enter the 15-character GSTIN, or choose Unregistered / Pending.");
        }
        boolean changed = gstin != null && !gstin.equals(currentGstin);
        if (changed && !GSTIN.matcher(gstin).matches()) {   // a saved legacy value is not re-checked on unrelated edits
            throw err("Invalid GSTIN", "GSTIN_INVALID",
                    "GSTIN " + gstin + " is not in the correct format (e.g. 33ABCDE1234F1Z5).",
                    "Check the 15 characters: state code, PAN, entity number, Z and check character.");
        }
        return new Result(REGISTERED, toStore);
    }

    private static BusinessValidationException err(String title, String code, String msg, String action) {
        return new BusinessValidationException(title, code, msg, action);
    }
}
