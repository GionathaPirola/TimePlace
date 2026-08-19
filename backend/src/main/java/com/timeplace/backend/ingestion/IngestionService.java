package com.timeplace.backend.ingestion;

import com.timeplace.backend.dto.IngestedPhoto;
import com.timeplace.backend.entity.Photo;
import com.timeplace.backend.repository.PhotoRepository;
import com.timeplace.backend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    // Two photos from different sources within this distance and with the same year are treated as duplicates.
    private static final double CROSS_SOURCE_DUPLICATE_RADIUS_METERS = 25;

    private final PhotoRepository photoRepository;
    private final List<PhotoSourceClient> sourceClients;

    /** Runs every registered source client (or a filtered subset) against the given point. */
    public IngestionResult ingest(double lat, double lon, int radiusMeters, List<String> sourceNames) {
        IngestionResult total = IngestionResult.empty();
        for (PhotoSourceClient client : sourceClients) {
            if (!sourceNames.isEmpty() && !sourceNames.contains(client.sourceName())) {
                continue;
            }
            total = total.add(ingestFromSource(client, lat, lon, radiusMeters));
        }
        return total;
    }

    private IngestionResult ingestFromSource(PhotoSourceClient client, double lat, double lon, int radiusMeters) {
        List<IngestedPhoto> raw = client.searchNearby(lat, lon, radiusMeters);
        int saved = 0;
        int skippedExisting = 0;
        int skippedDuplicate = 0;

        for (IngestedPhoto photo : raw) {
            if (photoRepository.existsBySourceAndSourceId(photo.source(), photo.sourceId())) {
                skippedExisting++;
                continue;
            }
            if (photo.takenYear() != null && photoRepository.existsNearDuplicate(
                    photo.lat(), photo.lon(), photo.takenYear(), CROSS_SOURCE_DUPLICATE_RADIUS_METERS, photo.source())) {
                skippedDuplicate++;
                continue;
            }
            photoRepository.save(toEntity(photo));
            saved++;
        }

        log.info("Ingested from {}: fetched={}, saved={}, skippedExisting={}, skippedDuplicate={}",
                client.sourceName(), raw.size(), saved, skippedExisting, skippedDuplicate);

        return new IngestionResult(raw.size(), saved, skippedExisting, 0, skippedDuplicate);
    }

    private Photo toEntity(IngestedPhoto photo) {
        return Photo.builder()
                .source(photo.source())
                .sourceId(photo.sourceId())
                .title(photo.title())
                .imageUrl(photo.imageUrl())
                .thumbUrl(photo.thumbUrl())
                .takenYear(photo.takenYear())
                .takenDate(photo.takenDate())
                .location(GeoUtils.point(photo.lon(), photo.lat()))
                .license(photo.license())
                .author(photo.author())
                .attribution(photo.attribution())
                .verified(false)
                .build();
    }
}
