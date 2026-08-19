package com.timeplace.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/** Requires Docker: spins up a real PostGIS container and exercises the nearby + correction endpoints. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PhotosNearbyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long insertPhoto(String sourceId, int year, double lat, double lon) {
        jdbcTemplate.update("""
                INSERT INTO photos (source, source_id, title, image_url, taken_year, location, license)
                VALUES ('wikimedia', ?, ?, 'https://example.org/img.jpg', ?,
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, 'CC BY-SA 4.0')
                """, sourceId, "Photo " + sourceId, year, lon, lat);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM photos WHERE source = 'wikimedia' AND source_id = ?", Long.class, sourceId);
    }

    @Test
    void nearbyReturnsOnlyPhotosWithinRadiusOrderedByYear() {
        insertPhoto("close-old", 1930, 45.0703, 7.6869);
        insertPhoto("close-new", 1980, 45.0704, 7.6870);
        insertPhoto("far-away", 1950, 46.5, 9.5);

        String url = "http://localhost:%d/api/photos/nearby?lat=45.0703&lon=7.6869&radius=500".formatted(port);
        ResponseEntity<PhotoDtoTestModel[]> response = restTemplate.getForEntity(url, PhotoDtoTestModel[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(p -> p.sourceId).containsExactly("close-old", "close-new");
    }

    @Test
    void nearbyFiltersByYearRange() {
        insertPhoto("year-old", 1900, 45.0703, 7.6869);
        insertPhoto("year-new", 2020, 45.0703, 7.6869);

        String url = "http://localhost:%d/api/photos/nearby?lat=45.0703&lon=7.6869&radius=500&yearFrom=2000"
                .formatted(port);
        ResponseEntity<PhotoDtoTestModel[]> response = restTemplate.getForEntity(url, PhotoDtoTestModel[].class);

        assertThat(response.getBody()).extracting(p -> p.sourceId).containsExactly("year-new");
    }

    @Test
    void correctLocationAcceptsValidRequestAndPersistsCorrection() {
        long photoId = insertPhoto("correct-me", 1960, 45.0703, 7.6869);

        String url = "http://localhost:%d/api/photos/%d/correct-location".formatted(port, photoId);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, new CorrectLocationBody(45.08, 7.70), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM location_corrections WHERE photo_id = ?", Integer.class, photoId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void correctLocationReturns404ForUnknownPhoto() {
        String url = "http://localhost:%d/api/photos/999999/correct-location".formatted(port);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, new CorrectLocationBody(45.08, 7.70), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private record CorrectLocationBody(double lat, double lon) {
    }

    /** Mirrors just the field this test asserts on; Jackson ignores the rest of PhotoDto's JSON. */
    private static final class PhotoDtoTestModel {
        public String sourceId;
    }
}
