# TimePlace

Historical geolocated photos: given a location, show old photographs taken nearby,
ordered by year, with a "then/now" comparison slider and crowdsourced location corrections.

All phases of the roadmap (0-7) have been implemented. See "Configuration / TODO placeholders"
below for the handful of things you still need to fill in before this runs against real data.

## Stack

- **Backend:** Java 21 + Spring Boot 3.3 (Maven) - REST API
- **Database:** PostgreSQL 16 + PostGIS (spatial queries), Flyway migrations
- **Frontend:** React 18 + TypeScript + Vite, map with MapLibre GL
- **Data sources:** Wikimedia Commons `geosearch` (no key) and Europeana (free API key)

## Repository structure

```
TimePlace/
|-- docker-compose.yml      # PostgreSQL + PostGIS (+ backend/frontend under the "full" profile)
|-- backend/                # Spring Boot REST API + ingestion job (Maven)
|   |-- Dockerfile
|   |-- fly.toml.example
|   `-- src/main/java/com/timeplace/backend/
|       |-- entity/  repository/  dto/    # JPA + spatial JDBC query
|       |-- ingestion/                    # Wikimedia + Europeana clients, dedup, CLI runner
|       |-- web/  service/  exception/    # REST API
|       `-- util/                         # JTS/geo helpers
`-- frontend/               # React + TypeScript app (Vite)
    |-- Dockerfile
    `-- src/
        |-- components/                   # MapView, gallery, modal, then/now & year sliders
        `-- api/                          # backend client
```

## Prerequisites

You need three tools installed: a JDK 21, Node.js, and Docker. Check what you have:

```powershell
java -version     # need 21.x  (this machine's DEFAULT is 17 - see the JDK note below)
node -v           # 18/20 LTS recommended (this machine has 16.20.2)
npm -v
docker --version  # required for the database (NOT installed on this machine yet)
```

Current state of this dev machine and what to do about it:

- **JDK 21** IS installed at `C:\Users\g.pirola\AppData\Local\jdks\jdk-21.0.10`, but the system
  default `java`/`mvn` still points at **JDK 17**. Before running any Maven command you must point
  Maven at JDK 21 in that terminal (see step 2). The build fails on JDK 17 because the project
  targets Java 21.
- **Node.js is 16.20.2** (EOL). The frontend was pinned to Vite 4 + `maplibre-gl@3` so it still runs
  on Node 16, so you can start it as-is. Upgrading to Node 20 LTS is recommended but not required.
- **Docker is NOT installed.** The database runs as a Docker container, so you must install
  **Docker Desktop** (https://www.docker.com/products/docker-desktop/) before you can start the DB,
  run the Testcontainers-based tests, or build the container images. Without Docker, only the
  Docker-free unit tests (step 5) and the frontend UI (step 4, with no data) will work.

## Getting started (local dev) - exact steps

Run these in order. Use **three separate PowerShell terminals** (DB, backend, frontend) so each can
keep running.

<details>
<summary>TL;DR - the whole thing in one glance (details for each step below)</summary>

```powershell
# terminal 1 (repo root): database
docker compose up -d

# terminal 2 (backend): use JDK 21, then run
$env:JAVA_HOME = "C:\Users\g.pirola\AppData\Local\jdks\jdk-21.0.10"; $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
cd backend
# load data once (Ctrl+C first if the app is already running):
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.ingestion.enabled=true --app.ingestion.lat=45.0703 --app.ingestion.lon=7.6869 --app.ingestion.radius-meters=1000"
mvn spring-boot:run   # then serve the API

# terminal 3 (frontend)
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173.
</details>

### Step 1 - Start the database (terminal 1)

From the repository root (`TimePlace/`):

```powershell
docker compose up -d
docker compose ps        # wait until the "db" service shows "healthy"
```

This starts Postgres 16 + PostGIS on `localhost:5432` (db=`timeplace`, user=`timeplace`,
password=`timeplace`). The data is kept in a named volume, so it survives restarts.
Stop it later with `docker compose down` (add `-v` to also wipe the data).

### Step 2 - Start the backend (terminal 2)

Point this terminal at JDK 21, then run the app from the `backend/` folder:

```powershell
$env:JAVA_HOME = "C:\Users\g.pirola\AppData\Local\jdks\jdk-21.0.10"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version            # confirm it now prints 21.x

cd backend
mvn spring-boot:run
```

On startup, Flyway automatically creates the schema (`photos`, `location_corrections`).
Verify it is up:

```powershell
# in any terminal
curl http://localhost:8080/actuator/health   # expect {"status":"UP"}
```

Leave this terminal running. Stop the backend with Ctrl+C.

### Step 3 - Load some photos into the database (terminal 2, one-off)

The database is empty until you ingest data. Stop the backend (Ctrl+C), then run the ingestion job.
It reuses the same app but is gated by `app.ingestion.enabled`, so it never runs during a normal
start. It downloads photos from Wikimedia Commons around a point and exits when done:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--app.ingestion.enabled=true --app.ingestion.city=Turin --app.ingestion.lat=45.0703 --app.ingestion.lon=7.6869 --app.ingestion.radius-meters=1000"
```

- Change `--app.ingestion.lat/lon/city` to YOUR target city (Turin is just a placeholder).
- Add `--app.ingestion.sources=wikimedia` to run only one source (default: all sources).
- Europeana is skipped automatically unless you set the `EUROPEANA_API_KEY` env var first.
- Watch the log line `Ingestion finished: IngestionResult[...]` for how many photos were saved.

Then restart the backend normally (step 2) so it serves the data you just ingested.

### Step 4 - Start the frontend (terminal 3)

```powershell
cd frontend
npm install        # first time only
npm run dev
```

Open the URL Vite prints (default http://localhost:5173). The backend URL defaults to
`http://localhost:8080/api`; to change it, copy `.env.example` to `.env.local` and edit
`VITE_API_BASE_URL`.

In the app: click **"Use my location"** (or click anywhere on the map) to load nearby photos.
Click a photo to open the **then/now slider** with year/author/license; use **"Fix photo location"**
to drag its marker and submit a correction.

> If the gallery stays empty, you either haven't ingested photos yet (step 3) or you're looking at a
> different area than where you ingested. Click near your ingestion point, or widen the radius slider.

### Step 5 - Run the tests (optional)

```powershell
# in the backend/ folder, with JDK 21 active (step 2)
mvn test                     # all tests; the integration ones need Docker running (step 1)

# Docker-free tests only (no database needed):
mvn "-Dtest=YearNormalizerTest,LicenseFilterTest,IngestionServiceTest,PhotoControllerValidationTest" test
```

`YearNormalizerTest`, `LicenseFilterTest`, `IngestionServiceTest` and `PhotoControllerValidationTest`
are plain unit/slice tests (no Docker). `BackendApplicationTests` and `PhotosNearbyIntegrationTest`
spin up a disposable PostGIS container via Testcontainers and require Docker.

## REST API

- `GET /api/photos/nearby?lat=&lon=&radius=&yearFrom=&yearTo=` - photos within `radius` meters
  (max 10000), ordered by year then distance.
- `POST /api/photos/{id}/correct-location` body `{ "lat": ..., "lon": ... }` - stores a proposed
  correction in `location_corrections` (doesn't move the photo; that's left for a manual/admin
  review step, out of scope for the MVP).

## Configuration / TODO placeholders

- **Europeana API key**: get a free one at https://apikey.europeana.eu/ and set the
  `EUROPEANA_API_KEY` env var. Without it, the Europeana source is silently skipped during
  ingestion. Its geo-query field names (`pl_wgs84_pos_lat`/`long`, `edmPlaceLatitude`/`Longitude`)
  are a best-effort mapping from the public docs and haven't been verified against a live key -
  double check `backend/src/main/java/.../ingestion/europeana/EuropeanaClient.java` once you have one.
- **Target city**: `frontend/src/constants.ts` (`DEFAULT_CENTER`) and the ingestion command above
  both default to Turin, Italy as a placeholder - change both to your MVP city.
- Datasource credentials in `backend/src/main/resources/application.yml` default to
  `timeplace`/`timeplace`, matching `docker-compose.yml`. Override via `DB_HOST`, `DB_PORT`,
  `DB_NAME`, `DB_USER`, `DB_PASSWORD` env vars for other environments.
- **CORS**: the backend allows the Vite dev origins (`http://localhost:5173`, `:4173`) out of the
  box. In production set `APP_CORS_ALLOWED_ORIGINS` to your deployed frontend origin
  (e.g. `https://timeplace.pages.dev`), comma-separated for multiple.
- The map style (`MAP_STYLE_URL` in `MapView.tsx`/`ThenNowSlider.tsx`) uses OpenFreeMap
  (`https://tiles.openfreemap.org/styles/liberty`), free and keyless. MapTiler is a nicer-looking
  alternative but requires registering a free API key.

## Deployment (Phase 7)

Recommended free/low-cost setup for a single-city MVP:

| Layer    | Recommended       | Why / alternative |
|----------|--------------------|--------------------|
| Frontend | Cloudflare Pages   | Generous free static hosting, builds `frontend/` on push. Alternative: Netlify/Vercel (similar free tiers). |
| Backend  | Fly.io             | Small free-tier VM, deploys straight from `backend/Dockerfile`. Alternative: Railway (simpler single dashboard, but time-limited free credit rather than a perpetual free tier). |
| Database | Supabase           | Managed Postgres with PostGIS enabled via one click/`CREATE EXTENSION`, free tier includes ~500MB. Alternative: run Postgres+PostGIS yourself on the same Fly.io app (cheaper at scale, more ops work). |

Steps:
1. **Database**: create a Supabase project, enable the `postgis` extension (Database > Extensions,
   or run the SQL in `backend/src/main/resources/db/migration/V1__init_schema.sql` once - Flyway
   will also try to run it automatically on backend startup). Grab the connection details.
2. **Backend**: `backend/Dockerfile` is a multi-stage build (Maven -> `eclipse-temurin:21-jre-alpine`).
   `backend/fly.toml.example` documents the expected Fly.io config - run `fly launch --no-deploy`
   inside `backend/`, adjust it, then `fly secrets set DB_HOST=... DB_PORT=... DB_NAME=... DB_USER=... DB_PASSWORD=... EUROPEANA_API_KEY=...`
   and `fly deploy`.
3. **Frontend**: point Cloudflare Pages at this repo with build command `npm run build`, output
   directory `dist`, root directory `frontend/`, and set the `VITE_API_BASE_URL` build environment
   variable to your deployed backend's URL (e.g. `https://timeplace-backend.fly.dev/api`).

### Local "full stack in containers" check

To sanity-check the Docker images end to end without any cloud account:

```powershell
docker compose --profile full up --build
```

This builds and runs `db` + `backend` + `frontend` together (frontend on http://localhost:5173,
backend on http://localhost:8080). Day-to-day development should keep using `docker compose up -d`
(db only) plus `mvn spring-boot:run` / `npm run dev` for fast reload.

## Roadmap

0. **Setup** - repo, Docker Compose (Postgres+PostGIS), Spring Boot skeleton, Vite+React skeleton
1. **Wikimedia ingestion** - `geosearch` client, year/license normalization, `PhotoSourceClient` abstraction
2. **`GET /api/photos/nearby`** - PostGIS spatial query, validation, correction endpoint
3. **Frontend base** - MapLibre map, geolocation, gallery sidebar
4. **Photo detail + then/now slider**
5. **Year filter (dual slider) + crowdsourced location corrections**
6. **Europeana source + cross-source deduplication**
7. **Deployment** - Dockerfiles, Compose "full" profile, Fly.io/Supabase/Cloudflare Pages guidance

