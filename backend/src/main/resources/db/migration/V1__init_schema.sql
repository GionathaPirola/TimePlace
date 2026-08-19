CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE photos (
    id              BIGSERIAL PRIMARY KEY,
    source          TEXT NOT NULL,          -- 'wikimedia' | 'europeana'
    source_id       TEXT NOT NULL,
    title           TEXT,
    image_url       TEXT NOT NULL,
    thumb_url       TEXT,
    taken_year      INT,                    -- normalized year (nullable)
    taken_date      DATE,                   -- exact date if available
    location        GEOGRAPHY(POINT, 4326) NOT NULL,
    license         TEXT,
    author          TEXT,
    attribution     TEXT,
    verified        BOOLEAN NOT NULL DEFAULT FALSE, -- position confirmed by a user
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source, source_id)
);

CREATE INDEX idx_photos_location ON photos USING GIST (location);
CREATE INDEX idx_photos_year     ON photos (taken_year);

-- user-proposed location corrections (crowdsourcing)
CREATE TABLE location_corrections (
    id           BIGSERIAL PRIMARY KEY,
    photo_id     BIGINT NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    new_location GEOGRAPHY(POINT, 4326) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_location_corrections_photo_id ON location_corrections (photo_id);
