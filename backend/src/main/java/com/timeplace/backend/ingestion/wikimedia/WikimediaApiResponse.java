package com.timeplace.backend.ingestion.wikimedia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Shape of the Wikimedia Commons `action=query` geosearch response, trimmed to the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikimediaApiResponse(Query query) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Query(Map<String, Page> pages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(long pageid, String title, List<Coordinate> coordinates, List<ImageInfo> imageinfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coordinate(double lat, double lon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageInfo(String url, String thumburl, ExtMetadata extmetadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtMetadata(
            @JsonProperty("DateTimeOriginal") MetaValue dateTimeOriginal,
            @JsonProperty("LicenseShortName") MetaValue licenseShortName,
            @JsonProperty("UsageTerms") MetaValue usageTerms,
            @JsonProperty("Artist") MetaValue artist,
            @JsonProperty("Attribution") MetaValue attribution
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetaValue(@JsonProperty("value") String value) {
    }
}
