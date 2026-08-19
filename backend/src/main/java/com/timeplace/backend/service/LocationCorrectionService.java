package com.timeplace.backend.service;

import com.timeplace.backend.entity.LocationCorrection;
import com.timeplace.backend.exception.PhotoNotFoundException;
import com.timeplace.backend.repository.LocationCorrectionRepository;
import com.timeplace.backend.repository.PhotoRepository;
import com.timeplace.backend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationCorrectionService {

    private final PhotoRepository photoRepository;
    private final LocationCorrectionRepository locationCorrectionRepository;

    @Transactional
    public void proposeCorrection(long photoId, double lat, double lon) {
        if (!photoRepository.existsById(photoId)) {
            throw new PhotoNotFoundException(photoId);
        }
        LocationCorrection correction = LocationCorrection.builder()
                .photoId(photoId)
                .newLocation(GeoUtils.point(lon, lat))
                .build();
        locationCorrectionRepository.save(correction);
    }
}
