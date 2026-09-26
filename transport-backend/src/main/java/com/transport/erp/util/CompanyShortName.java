package com.transport.erp.util;

import com.transport.erp.exception.BusinessValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Company initials shown in the app header ("PKC" for "PKC Transport").
 * Entered in Platform Admin; when blank it is derived from the company name.
 */
public final class CompanyShortName {

    public static final int MIN = 2;
    public static final int MAX = 6;
    private static final Set<String> NOISE = Set.of("PVT", "PRIVATE", "LTD", "LIMITED", "LLP", "INC", "CO", "COMPANY",
            "THE", "AND", "&", "OF", "M/S", "MS");

    private CompanyShortName() {
    }

    /** Validates an entered value (letters, digits, &), upper-cased; blank -> derived from the name. */
    public static String normalize(String entered, String companyName) {
        if (entered == null || entered.isBlank()) {
            return derive(companyName);
        }
        String v = entered.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (!v.matches("[A-Z0-9&]{" + MIN + "," + MAX + "}")) {
            throw new BusinessValidationException("Invalid Short Name", "COMPANY_SHORT_NAME_INVALID",
                    "Short name must be " + MIN + " to " + MAX + " letters or digits (for example PKC).",
                    "Use the company's initials without spaces or symbols.");
        }
        return v;
    }

    /** "PKC Transport" -> PKC, "Sri Murugan Transports Pvt Ltd" -> SMT, "Balaji" -> BAL. */
    public static String derive(String companyName) {
        if (companyName == null || companyName.isBlank()) return null;
        List<String> words = new ArrayList<>();
        for (String w : companyName.trim().split("[^A-Za-z0-9&]+")) {
            if (w.isEmpty() || NOISE.contains(w.toUpperCase(Locale.ROOT))) continue;
            words.add(w);
        }
        if (words.isEmpty()) return null;
        String first = words.get(0);
        // Name already starts with an acronym (PKC Transport, SKR Logistics).
        if (first.length() >= MIN && first.length() <= 5 && first.equals(first.toUpperCase(Locale.ROOT)) && first.matches("[A-Z0-9]+")) {
            return first;
        }
        if (words.size() == 1) {
            String w = first.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
            return w.length() <= 3 ? w : w.substring(0, 3);
        }
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() >= 4) break;
            sb.append(Character.toUpperCase(w.charAt(0)));
        }
        return sb.toString();
    }
}
