package com.timeplace.backend.ingestion;

import java.util.Set;

/**
 * Accepts only "free culture" licenses (public domain or CC0/BY/BY-SA), rejecting NC/ND variants
 * and anything unrecognized, per the project's "free/open license only" constraint.
 */
public final class LicenseFilter {

    private static final Set<String> REJECTED_TOKENS = Set.of("NC", "ND");

    private LicenseFilter() {
    }

    public static boolean isFree(String licenseShortName, String usageTerms) {
        String license = firstNonBlank(licenseShortName, usageTerms);
        if (license == null) {
            return false;
        }
        String normalized = license.toUpperCase();

        if (normalized.contains("PUBLIC DOMAIN") || normalized.contains("PDM") || normalized.equals("CC0")
                || normalized.contains("CC0") || normalized.contains("NO RESTRICTIONS")) {
            return true;
        }
        if (!normalized.contains("CC") && !normalized.contains("CREATIVE COMMONS")) {
            return false;
        }
        for (String token : normalized.split("[^A-Z0-9]+")) {
            if (REJECTED_TOKENS.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
