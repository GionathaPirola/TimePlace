package com.timeplace.backend.repository;

import com.timeplace.backend.entity.LocationCorrection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationCorrectionRepository extends JpaRepository<LocationCorrection, Long> {
}
