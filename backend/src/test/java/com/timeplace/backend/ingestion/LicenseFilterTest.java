package com.timeplace.backend.ingestion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "CC0", "Public Domain", "PDM 1.0", "CC BY 4.0", "CC BY-SA 4.0", "CC BY 2.0", "No restrictions"
    })
    void acceptsFreeLicenses(String license) {
        assertThat(LicenseFilter.isFree(license, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CC BY-NC 4.0", "CC BY-ND 4.0", "CC BY-NC-SA 4.0", "All rights reserved", "In copyright"
    })
    void rejectsNonFreeLicenses(String license) {
        assertThat(LicenseFilter.isFree(license, null)).isFalse();
    }

    @Test
    void rejectsMissingLicense() {
        assertThat(LicenseFilter.isFree(null, null)).isFalse();
        assertThat(LicenseFilter.isFree("", "")).isFalse();
    }

    @Test
    void fallsBackToUsageTermsWhenLicenseShortNameMissing() {
        assertThat(LicenseFilter.isFree(null, "CC BY-SA 3.0")).isTrue();
        assertThat(LicenseFilter.isFree(null, "CC BY-NC 3.0")).isFalse();
    }
}
