package com.timeplace.backend.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Runs the ingestion job as part of a normal Spring Boot startup, gated by app.ingestion.enabled
 * so the regular web server startup is unaffected. Invoke with, e.g.:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--app.ingestion.enabled=true --app.ingestion.city=Turin --app.ingestion.lat=45.0703 --app.ingestion.lon=7.6869 --app.ingestion.radius-meters=1000"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionRunner implements ApplicationRunner {

    private final IngestionProperties properties;
    private final IngestionService ingestionService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        log.info("Starting ingestion for city='{}' at ({}, {}), radius={}m, sources={}",
                properties.getCity(), properties.getLat(), properties.getLon(),
                properties.getRadiusMeters(), properties.getSources());

        IngestionResult result = ingestionService.ingest(
                properties.getLat(), properties.getLon(), properties.getRadiusMeters(), properties.getSources());

        log.info("Ingestion finished: {}", result);
        System.exit(SpringApplication.exit(applicationContext, () -> 0));
    }
}
