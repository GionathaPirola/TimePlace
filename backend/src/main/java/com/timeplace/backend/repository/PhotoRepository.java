package com.timeplace.backend.repository;

import com.timeplace.backend.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    boolean existsBySourceAndSourceId(String source, String sourceId);

    /** True if another source already has a photo of the same year within thresholdMeters (cross-source dedup). */
    @Query(nativeQuery = true, value = """
            SELECT EXISTS (
                SELECT 1 FROM photos
                WHERE source <> :excludeSource
                  AND taken_year = :year
                  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :thresholdMeters)
            )
            """)
    boolean existsNearDuplicate(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("year") int year,
            @Param("thresholdMeters") double thresholdMeters,
            @Param("excludeSource") String excludeSource);
}
