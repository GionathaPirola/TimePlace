package com.timeplace.backend.dto;

import java.time.LocalDate;

/** Normalized photo data produced by an ingestion source client, before dedup/persistence. */
public record IngestedPhoto(
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
        String attribution
) {
}
