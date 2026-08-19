package com.timeplace.backend.ingestion.europeana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Shape of the Europeana Record Search API (record/v2/search.json) response, trimmed to the
 * fields we use. NOTE: exact field availability (esp. edmPlaceLatitude/Longitude and year) varies
 * per record/dataset - verify against a live response once a real API key is configured.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EuropeanaApiResponse(boolean success, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String id,
            List<String> title,
            String edmIsShownBy,
            String edmPreview,
            List<String> year,
            List<String> edmPlaceLatitude,
            List<String> edmPlaceLongitude,
            List<String> rights,
            List<String> dcCreator,
            List<String> dataProvider
    ) {
    }
}
