import { useEffect, useState } from 'react'
import './App.css'
import { correctPhotoLocation, fetchNearbyPhotos } from './api/photos'
import { MapView } from './components/MapView'
import { PhotoGallery } from './components/PhotoGallery'
import { PhotoModal } from './components/PhotoModal'
import { YearRangeSlider } from './components/YearRangeSlider'
import {
  DEFAULT_CENTER,
  DEFAULT_RADIUS_METERS,
  MAX_RADIUS_METERS,
  YEAR_RANGE_MAX,
  YEAR_RANGE_MIN,
} from './constants'
import type { LatLon, Photo } from './types'

function App() {
  const [center, setCenter] = useState<LatLon>(DEFAULT_CENTER)
  const [radius, setRadius] = useState(DEFAULT_RADIUS_METERS)
  const [yearFrom, setYearFrom] = useState(YEAR_RANGE_MIN)
  const [yearTo, setYearTo] = useState(YEAR_RANGE_MAX)

  const [photos, setPhotos] = useState<Photo[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null)
  const [correctionPhotoId, setCorrectionPhotoId] = useState<number | null>(null)
  const [pendingCorrection, setPendingCorrection] = useState<LatLon | null>(null)
  const [submittingCorrection, setSubmittingCorrection] = useState(false)
  const [correctionMessage, setCorrectionMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    fetchNearbyPhotos({ lat: center.lat, lon: center.lon, radius, yearFrom, yearTo })
      .then((result) => {
        if (!cancelled) setPhotos(result)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load photos')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [center, radius, yearFrom, yearTo])

  function handleUseMyLocation() {
    if (!navigator.geolocation) {
      setError('Geolocation is not supported by this browser.')
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCenter({ lat: position.coords.latitude, lon: position.coords.longitude })
      },
      () => setError('Could not retrieve your location. Pick a point on the map instead.'),
    )
  }

  function handleSelectPhoto(photo: Photo) {
    setSelectedPhoto(photo)
    setCorrectionPhotoId(null)
    setPendingCorrection(null)
  }

  function handleCloseModal() {
    setSelectedPhoto(null)
    setCorrectionPhotoId(null)
    setPendingCorrection(null)
  }

  function handleToggleCorrectionMode() {
    if (!selectedPhoto) return
    setCorrectionPhotoId(selectedPhoto.id)
    setPendingCorrection(null)
  }

  function handleCancelCorrection() {
    setCorrectionPhotoId(null)
    setPendingCorrection(null)
  }

  async function handleConfirmCorrection() {
    if (!selectedPhoto || !pendingCorrection) return
    setSubmittingCorrection(true)
    try {
      await correctPhotoLocation(selectedPhoto.id, pendingCorrection.lat, pendingCorrection.lon)
      setCorrectionMessage('Thanks! Your correction was submitted for review.')
      setCorrectionPhotoId(null)
      setPendingCorrection(null)
    } catch (err) {
      setCorrectionMessage(err instanceof Error ? err.message : 'Failed to submit correction')
    } finally {
      setSubmittingCorrection(false)
    }
  }

  return (
    <div className="app">
      <MapView
        center={center}
        photos={photos}
        selectedPhotoId={selectedPhoto?.id ?? null}
        correctionPhotoId={correctionPhotoId}
        onMapClick={setCenter}
        onMarkerClick={handleSelectPhoto}
        onCorrectionDragEnd={(_photoId, point) => setPendingCorrection(point)}
      />

      <aside className="sidebar">
        <header className="sidebar__header">
          <h1>TimePlace</h1>
          <p>Historical photos near a place, then and now.</p>
          <button type="button" onClick={handleUseMyLocation}>
            Use my location
          </button>
          <p className="sidebar__hint">...or click anywhere on the map.</p>
          {error && <p className="sidebar__error">{error}</p>}
        </header>

        <div className="sidebar__filters">
          <label>
            Radius: {radius} m
            <input
              type="range"
              min={100}
              max={MAX_RADIUS_METERS}
              step={100}
              value={radius}
              onChange={(e) => setRadius(Number(e.target.value))}
            />
          </label>
          <YearRangeSlider
            min={YEAR_RANGE_MIN}
            max={YEAR_RANGE_MAX}
            valueFrom={yearFrom}
            valueTo={yearTo}
            onChange={(from, to) => {
              setYearFrom(from)
              setYearTo(to)
            }}
          />
        </div>

        <PhotoGallery
          photos={photos}
          selectedPhotoId={selectedPhoto?.id ?? null}
          loading={loading}
          onSelect={handleSelectPhoto}
        />
      </aside>

      {selectedPhoto && (
        <PhotoModal
          photo={selectedPhoto}
          onClose={handleCloseModal}
          correctionMode={correctionPhotoId === selectedPhoto.id}
          onToggleCorrectionMode={handleToggleCorrectionMode}
          pendingCorrection={pendingCorrection}
          onConfirmCorrection={handleConfirmCorrection}
          onCancelCorrection={handleCancelCorrection}
          submitting={submittingCorrection}
        />
      )}

      {correctionMessage && (
        <div className="toast" onClick={() => setCorrectionMessage(null)}>
          {correctionMessage}
        </div>
      )}
    </div>
  )
}

export default App
