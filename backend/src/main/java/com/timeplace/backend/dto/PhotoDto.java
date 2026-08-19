package com.timeplace.backend.dto;

import java.time.LocalDate;

/** Flat, API-facing view of a photo, including its distance from the query point. */
public record PhotoDto(
        long id,
        String source,
        String sourceId,
        String title,
        String imageUrl,
        String thumbUrl,
        Integer takenYear,
        LocalDate takenDate,
        double lat,
        double lon,
        String license,
        String author,
        String attribution,
        boolean verified,
        double distanceMeters
) {
}
