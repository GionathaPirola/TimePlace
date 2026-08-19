package com.timeplace.backend.web;

import com.timeplace.backend.dto.CorrectLocationRequest;
import com.timeplace.backend.dto.PhotoDto;
import com.timeplace.backend.repository.PhotoNearbyRepository;
import com.timeplace.backend.service.LocationCorrectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Validated
public class PhotoController {

    private final PhotoNearbyRepository photoNearbyRepository;
    private final LocationCorrectionService locationCorrectionService;

    @GetMapping("/nearby")
    public List<PhotoDto> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon,
            @RequestParam(defaultValue = "500") @Positive @Max(10_000) double radius,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo
    ) {
        return photoNearbyRepository.findNearby(lat, lon, radius, yearFrom, yearTo);
    }

    @PostMapping("/{id}/correct-location")
    public ResponseEntity<Void> correctLocation(@PathVariable long id, @Valid @RequestBody CorrectLocationRequest request) {
        locationCorrectionService.proposeCorrection(id, request.lat(), request.lon());
        return ResponseEntity.accepted().build();
    }
}
