package com.timeplace.backend.ingestion;

import com.timeplace.backend.dto.IngestedPhoto;

import java.util.List;

/** Implemented by each external photo source (Wikimedia, Europeana, ...). */
public interface PhotoSourceClient {

    /** Short identifier stored in photos.source, e.g. "wikimedia" or "europeana". */
    String sourceName();

    List<IngestedPhoto> searchNearby(double lat, double lon, int radiusMeters);
}
