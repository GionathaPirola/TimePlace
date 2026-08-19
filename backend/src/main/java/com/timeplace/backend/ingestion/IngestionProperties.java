package com.timeplace.backend.ingestion;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Bound from `app.ingestion.*` - see application.yml. Disabled by default; enabled via a run argument. */
@Component
@ConfigurationProperties(prefix = "app.ingestion")
@Getter
@Setter
public class IngestionProperties {

    /** If false, IngestionRunner does nothing (normal web app startup). */
    private boolean enabled = false;

    /** Free-text label, only used for logging. */
    private String city = "";

    private double lat;

    private double lon;

    private int radiusMeters = 1000;

    /** Which PhotoSourceClient beans (by sourceName()) to run; empty = all registered sources. */
    private List<String> sources = List.of();
}
