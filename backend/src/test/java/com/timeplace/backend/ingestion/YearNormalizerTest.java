package com.timeplace.backend.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class YearNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "1965, 1965",
            "1965-06-15, 1965",
            "1965-06-15 12:00:00, 1965",
            "circa 1930, 1930",
            "<time class='dtstart' datetime='1954'>1954</time>, 1954",
            "Taken in 2003 by unknown author, 2003"
    })
    void extractsYearFromVariousFormats(String raw, int expectedYear) {
        assertThat(YearNormalizer.extractYear(raw)).contains(expectedYear);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "n/a", "   "})
    void returnsEmptyForMissingOrUnparseableValues(String raw) {
        assertThat(YearNormalizer.extractYear(raw)).isEmpty();
    }

    @Test
    void picksFirstPlausibleYearWhenMultiplePresent() {
        assertThat(YearNormalizer.extractYear("scanned 2010, original circa 1948")).contains(2010);
    }
}
