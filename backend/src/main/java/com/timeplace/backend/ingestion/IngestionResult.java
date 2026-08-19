package com.timeplace.backend.ingestion;

public record IngestionResult(
        int fetched,
        int saved,
        int skippedExisting,
        int skippedInvalid,
        int skippedDuplicate
) {
    public static IngestionResult empty() {
        return new IngestionResult(0, 0, 0, 0, 0);
    }

    public IngestionResult add(IngestionResult other) {
        return new IngestionResult(
                fetched + other.fetched,
                saved + other.saved,
                skippedExisting + other.skippedExisting,
                skippedInvalid + other.skippedInvalid,
                skippedDuplicate + other.skippedDuplicate);
    }
}
