package com.timeplace.backend.ingestion.wikimedia;

import com.timeplace.backend.dto.IngestedPhoto;
import com.timeplace.backend.ingestion.LicenseFilter;
import com.timeplace.backend.ingestion.PhotoSourceClient;
import com.timeplace.backend.ingestion.YearNormalizer;
import com.timeplace.backend.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Free, no-API-key source: Wikimedia Commons `geosearch` generator.
 * https://www.mediawiki.org/wiki/Extension:GeoData#geosearch
 */
@Component
@Slf4j
public class WikimediaGeoSearchClient implements PhotoSourceClient {

    public static final String SOURCE_NAME = "wikimedia";

    // Commons enforces this as the hard upper bound for ggsradius, regardless of the requested radius.
    private static final int MAX_API_RADIUS_METERS = 10_000;
    private static final int PAGE_LIMIT = 50;

    private final RestClient restClient;
    private final String baseUrl;

    public WikimediaGeoSearchClient(RestClient.Builder restClientBuilder,
                                     @Value("${app.wikimedia.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<IngestedPhoto> searchNearby(double lat, double lon, int radiusMeters) {
        int clampedRadius = Math.min(radiusMeters, MAX_API_RADIUS_METERS);
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("action", "query")
                .queryParam("format", "json")
                .queryParam("generator", "geosearch")
                .queryParam("ggsnamespace", "6")
                .queryParam("ggscoord", lat + "|" + lon)
                .queryParam("ggsradius", clampedRadius)
                .queryParam("ggslimit", PAGE_LIMIT)
                .queryParam("prop", "coordinates|imageinfo")
                .queryParam("iiprop", "url|extmetadata")
                .queryParam("iiurlwidth", "400")
                .build()
                .toUriString();

        WikimediaApiResponse response;
        try {
            response = restClient.get().uri(url).retrieve().body(WikimediaApiResponse.class);
        } catch (Exception e) {
            log.warn("Wikimedia geosearch call failed for ({}, {}): {}", lat, lon, e.getMessage());
            return List.of();
        }

        if (response == null || response.query() == null || response.query().pages() == null) {
            return List.of();
        }

        List<IngestedPhoto> results = new ArrayList<>();
        for (Map.Entry<String, WikimediaApiResponse.Page> entry : response.query().pages().entrySet()) {
            toIngestedPhoto(entry.getValue()).ifPresent(results::add);
        }
        return results;
    }

    private java.util.Optional<IngestedPhoto> toIngestedPhoto(WikimediaApiResponse.Page page) {
        if (page.coordinates() == null || page.coordinates().isEmpty()) {
            return java.util.Optional.empty(); // no coordinates -> discard, per ingestion rules
        }
        if (page.imageinfo() == null || page.imageinfo().isEmpty()) {
            return java.util.Optional.empty();
        }

        WikimediaApiResponse.Coordinate coordinate = page.coordinates().get(0);
        WikimediaApiResponse.ImageInfo imageInfo = page.imageinfo().get(0);
        WikimediaApiResponse.ExtMetadata metadata = imageInfo.extmetadata();

        String licenseShortName = value(metadata == null ? null : metadata.licenseShortName());
        String usageTerms = value(metadata == null ? null : metadata.usageTerms());
        if (!LicenseFilter.isFree(licenseShortName, usageTerms)) {
            return java.util.Optional.empty(); // no free license -> discard
        }

        String dateTimeOriginal = value(metadata == null ? null : metadata.dateTimeOriginal());
        Integer year = YearNormalizer.extractYear(dateTimeOriginal).orElse(null);

        String artist = TextUtils.stripHtml(value(metadata == null ? null : metadata.artist()));
        String attribution = TextUtils.stripHtml(value(metadata == null ? null : metadata.attribution()));

        return java.util.Optional.of(new IngestedPhoto(
                SOURCE_NAME,
                String.valueOf(page.pageid()),
                page.title(),
                imageInfo.url(),
                imageInfo.thumburl(),
                year,
                null, // Commons rarely exposes a clean ISO date; year is the reliable signal
                coordinate.lat(),
                coordinate.lon(),
                licenseShortName != null ? licenseShortName : usageTerms,
                artist,
                attribution
        ));
    }

    private static String value(WikimediaApiResponse.MetaValue metaValue) {
        return metaValue == null ? null : metaValue.value();
    }
}
