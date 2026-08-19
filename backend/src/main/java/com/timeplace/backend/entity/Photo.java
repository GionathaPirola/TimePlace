package com.timeplace.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "photos", uniqueConstraints = @UniqueConstraint(columnNames = {"source", "source_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String source;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    private String title;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "thumb_url")
    private String thumbUrl;

    @Column(name = "taken_year")
    private Integer takenYear;

    @Column(name = "taken_date")
    private LocalDate takenDate;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    private String license;

    private String author;

    private String attribution;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
