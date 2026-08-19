package com.timeplace.backend.ingestion.europeana;

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
import java.util.Locale;
import java.util.Optional;

/**
 * Europeana Search API - requires a free API key (https://apikey.europeana.eu/).
 * TODO: Europeana's geo-range query syntax/field names (pl_wgs84_pos_lat/long) and the exact
 * shape of geo fields on results should be double-checked against a live response once
 * EUROPEANA_API_KEY is set; this is a best-effort mapping based on the public API docs.
 */
@Component
@Slf4j
public class EuropeanaClient implements PhotoSourceClient {

    public static final String SOURCE_NAME = "europeana";

    private static final double METERS_PER_DEGREE_LAT = 111_320d;
    private static final int ROWS = 50;

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;

    public EuropeanaClient(RestClient.Builder restClientBuilder,
                            @Value("${app.europeana.base-url}") String baseUrl,
                            @Value("${app.europeana.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public List<IngestedPhoto> searchNearby(double lat, double lon, int radiusMeters) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Europeana ingestion skipped: no API key configured (app.europeana.api-key).");
            return List.of();
        }

        double deltaLat = radiusMeters / METERS_PER_DEGREE_LAT;
        double metersPerDegreeLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat));
        double deltaLon = metersPerDegreeLon <= 0 ? deltaLat : radiusMeters / metersPerDegreeLon;

        String latRange = "[%s TO %s]".formatted(fmt(lat - deltaLat), fmt(lat + deltaLat));
        String lonRange = "[%s TO %s]".formatted(fmt(lon - deltaLon), fmt(lon + deltaLon));

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("wskey", apiKey)
                .queryParam("query", "*:*")
                .queryParam("reusability", "open")
                .queryParam("media", "true")
                .queryParam("rows", ROWS)
                .queryParam("qf", "TYPE:IMAGE")
                .queryParam("qf", "pl_wgs84_pos_lat:" + latRange)
                .queryParam("qf", "pl_wgs84_pos_long:" + lonRange)
                .build()
                .toUriString();

        EuropeanaApiResponse response;
        try {
            response = restClient.get().uri(url).retrieve().body(EuropeanaApiResponse.class);
        } catch (Exception e) {
            log.warn("Europeana search call failed for ({}, {}): {}", lat, lon, e.getMessage());
            return List.of();
        }

        if (response == null || response.items() == null) {
            return List.of();
        }

        List<IngestedPhoto> results = new ArrayList<>();
        for (EuropeanaApiResponse.Item item : response.items()) {
            toIngestedPhoto(item, lat, lon, radiusMeters).ifPresent(results::add);
        }
        return results;
    }

    private Optional<IngestedPhoto> toIngestedPhoto(EuropeanaApiResponse.Item item, double centerLat, double centerLon, int radiusMeters) {
        if (item.edmIsShownBy() == null || item.edmIsShownBy().isBlank()) {
            return Optional.empty();
        }
        Double itemLat = firstAsDouble(item.edmPlaceLatitude());
        Double itemLon = firstAsDouble(item.edmPlaceLongitude());
        if (itemLat == null || itemLon == null) {
            return Optional.empty(); // no coordinates -> discard, per ingestion rules
        }
        if (haversineMeters(centerLat, centerLon, itemLat, itemLon) > radiusMeters) {
            return Optional.empty(); // Europeana has no exact radius filter server-side; enforce it here
        }

        String rightsUri = item.rights() == null || item.rights().isEmpty() ? null : item.rights().get(0);
        String license = toLicenseLabel(rightsUri);
        if (!LicenseFilter.isFree(license, null)) {
            return Optional.empty();
        }

        String yearRaw = item.year() == null || item.year().isEmpty() ? null : item.year().get(0);
        Integer year = YearNormalizer.extractYear(yearRaw).orElse(null);

        String title = item.title() == null || item.title().isEmpty() ? null : item.title().get(0);
        String author = TextUtils.stripHtml(firstOrNull(item.dcCreator()));
        String attribution = TextUtils.stripHtml(firstOrNull(item.dataProvider()));

        return Optional.of(new IngestedPhoto(
                SOURCE_NAME,
                item.id(),
                title,
                item.edmIsShownBy(),
                item.edmPreview(),
                year,
                null,
                itemLat,
                itemLon,
                license,
                author,
                attribution
        ));
    }

    private static String toLicenseLabel(String rightsUri) {
        if (rightsUri == null) {
            return null;
        }
        String lower = rightsUri.toLowerCase(Locale.ROOT);
        if (lower.contains("publicdomain")) {
            return "Public Domain";
        }
        if (lower.contains("creativecommons.org/licenses/")) {
            String[] parts = lower.split("/licenses/");
            String rest = parts.length > 1 ? parts[1] : "";
            String code = rest.split("/")[0]; // e.g. "by-sa"
            return "CC " + code.toUpperCase(Locale.ROOT);
        }
        return rightsUri;
    }

    private static Double firstAsDouble(List<String> values) {
        String raw = firstOrNull(values);
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
