package com.timeplace.backend.repository;

import com.timeplace.backend.dto.PhotoDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

/**
 * Raw JDBC spatial query: kept outside Spring Data JPA because the result set mixes entity
 * columns with a computed distance_m column and lon/lat extracted from the geography point.
 */
@Repository
public class PhotoNearbyRepository {

    private static final int MAX_RESULTS = 100;

    private static final String FIND_NEARBY_SQL = """
            SELECT id, source, source_id, title, image_url, thumb_url, taken_year, taken_date,
                   ST_Y(location::geometry) AS lat, ST_X(location::geometry) AS lon,
                   license, author, attribution, verified,
                   ST_Distance(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) AS distance_m
            FROM photos
            WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radius)
              AND (:yearFrom IS NULL OR taken_year >= :yearFrom)
              AND (:yearTo IS NULL OR taken_year <= :yearTo)
            ORDER BY taken_year NULLS LAST, distance_m ASC
            LIMIT %d
            """.formatted(MAX_RESULTS);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PhotoNearbyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PhotoDto> findNearby(double lat, double lon, double radiusMeters, Integer yearFrom, Integer yearTo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lat", lat)
                .addValue("lon", lon)
                .addValue("radius", radiusMeters)
                .addValue("yearFrom", yearFrom, Types.INTEGER)
                .addValue("yearTo", yearTo, Types.INTEGER);

        return jdbcTemplate.query(FIND_NEARBY_SQL, params, (rs, rowNum) -> new PhotoDto(
                rs.getLong("id"),
                rs.getString("source"),
                rs.getString("source_id"),
                rs.getString("title"),
                rs.getString("image_url"),
                rs.getString("thumb_url"),
                (Integer) rs.getObject("taken_year"),
                rs.getObject("taken_date", java.time.LocalDate.class),
                rs.getDouble("lat"),
                rs.getDouble("lon"),
                rs.getString("license"),
                rs.getString("author"),
                rs.getString("attribution"),
                rs.getBoolean("verified"),
                rs.getDouble("distance_m")
        ));
    }
}
