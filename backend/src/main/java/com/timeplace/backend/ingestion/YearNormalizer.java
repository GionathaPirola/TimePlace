package com.timeplace.backend.ingestion;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a plausible photograph year out of the messy, free-text date fields returned by
 * Wikimedia/Europeana (HTML snippets, partial dates, "circa 1930", ranges, etc.).
 */
public final class YearNormalizer {

    // Matches any 4-digit year between 1000 and 2099, the plausible range for historical photos.
    private static final Pattern YEAR_PATTERN = Pattern.compile("(1[0-9]{3}|20[0-9]{2})");

    private YearNormalizer() {
    }

    public static Optional<Integer> extractYear(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String text = stripHtml(rawValue);
        Matcher matcher = YEAR_PATTERN.matcher(text);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    private static String stripHtml(String value) {
        return value.replaceAll("<[^>]*>", " ");
    }
}
