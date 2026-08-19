package com.timeplace.backend.ingestion;

import com.timeplace.backend.dto.IngestedPhoto;
import com.timeplace.backend.repository.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionServiceTest {

    private PhotoRepository photoRepository;

    @BeforeEach
    void setUp() {
        photoRepository = mock(PhotoRepository.class);
    }

    private IngestedPhoto samplePhoto(String sourceId) {
        return new IngestedPhoto("wikimedia", sourceId, "Title " + sourceId,
                "https://example.org/" + sourceId + ".jpg", "https://example.org/" + sourceId + "_thumb.jpg",
                1950, null, 45.07, 7.68, "CC BY-SA 4.0", "Some Author", "Some Attribution");
    }

    @Test
    void savesNewPhotosAndSkipsAlreadyIngestedOnes() {
        FakePhotoSourceClient client = new FakePhotoSourceClient("wikimedia",
                List.of(samplePhoto("1"), samplePhoto("2")));
        when(photoRepository.existsBySourceAndSourceId("wikimedia", "1")).thenReturn(true);
        when(photoRepository.existsBySourceAndSourceId("wikimedia", "2")).thenReturn(false);
        when(photoRepository.existsNearDuplicate(anyDouble(), anyDouble(), anyInt(), anyDouble(), anyString()))
                .thenReturn(false);

        IngestionService service = new IngestionService(photoRepository, List.of(client));
        IngestionResult result = service.ingest(45.07, 7.68, 1000, List.of());

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skippedExisting()).isEqualTo(1);
        verify(photoRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsCrossSourceDuplicates() {
        FakePhotoSourceClient client = new FakePhotoSourceClient("wikimedia", List.of(samplePhoto("3")));
        when(photoRepository.existsBySourceAndSourceId(anyString(), anyString())).thenReturn(false);
        when(photoRepository.existsNearDuplicate(45.07, 7.68, 1950, 25, "wikimedia")).thenReturn(true);

        IngestionService service = new IngestionService(photoRepository, List.of(client));
        IngestionResult result = service.ingest(45.07, 7.68, 1000, List.of());

        assertThat(result.skippedDuplicate()).isEqualTo(1);
        verify(photoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onlyRunsRequestedSources() {
        FakePhotoSourceClient wikimedia = new FakePhotoSourceClient("wikimedia", List.of(samplePhoto("4")));
        FakePhotoSourceClient europeana = new FakePhotoSourceClient("europeana", List.of(samplePhoto("5")));
        when(photoRepository.existsBySourceAndSourceId(anyString(), anyString())).thenReturn(false);
        when(photoRepository.existsNearDuplicate(anyDouble(), anyDouble(), anyInt(), anyDouble(), anyString()))
                .thenReturn(false);

        IngestionService service = new IngestionService(photoRepository, List.of(wikimedia, europeana));
        IngestionResult result = service.ingest(45.07, 7.68, 1000, List.of("wikimedia"));

        assertThat(result.fetched()).isEqualTo(1);
        assertThat(wikimedia.wasCalled()).isTrue();
        assertThat(europeana.wasCalled()).isFalse();
    }

    private static final class FakePhotoSourceClient implements PhotoSourceClient {
        private final String sourceName;
        private final List<IngestedPhoto> photos;
        private boolean called = false;

        private FakePhotoSourceClient(String sourceName, List<IngestedPhoto> photos) {
            this.sourceName = sourceName;
            this.photos = photos;
        }

        @Override
        public String sourceName() {
            return sourceName;
        }

        @Override
        public List<IngestedPhoto> searchNearby(double lat, double lon, int radiusMeters) {
            called = true;
            return photos;
        }

        boolean wasCalled() {
            return called;
        }
    }
}
