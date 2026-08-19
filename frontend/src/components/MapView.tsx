import { useEffect, useRef } from 'react'
import maplibregl from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import type { LatLon, Photo } from '../types'

// Free, no-API-key vector style (OpenFreeMap). Alternative: MapTiler styles need a free API key
// but offer more visual polish - swap the URL below if you register one.
const MAP_STYLE_URL = 'https://tiles.openfreemap.org/styles/liberty'

interface MapViewProps {
  center: LatLon
  photos: Photo[]
  selectedPhotoId: number | null
  correctionPhotoId: number | null
  onMapClick: (point: LatLon) => void
  onMarkerClick: (photo: Photo) => void
  onCorrectionDragEnd: (photoId: number, point: LatLon) => void
}

export function MapView({
  center,
  photos,
  selectedPhotoId,
  correctionPhotoId,
  onMapClick,
  onMarkerClick,
  onCorrectionDragEnd,
}: MapViewProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const mapRef = useRef<maplibregl.Map | null>(null)
  const centerMarkerRef = useRef<maplibregl.Marker | null>(null)
  const photoMarkersRef = useRef<Map<number, maplibregl.Marker>>(new Map())
  const callbacksRef = useRef({ onMapClick, onMarkerClick, onCorrectionDragEnd })

  useEffect(() => {
    callbacksRef.current = { onMapClick, onMarkerClick, onCorrectionDragEnd }
  }, [onMapClick, onMarkerClick, onCorrectionDragEnd])

  // Map instance is created once; center/photos are synced via separate effects below.
  useEffect(() => {
    if (!containerRef.current) return

    const map = new maplibregl.Map({
      container: containerRef.current,
      style: MAP_STYLE_URL,
      center: [center.lon, center.lat],
      zoom: 14,
    })
    map.addControl(new maplibregl.NavigationControl(), 'top-right')
    map.on('click', (e) => {
      callbacksRef.current.onMapClick({ lat: e.lngLat.lat, lon: e.lngLat.lng })
    })
    mapRef.current = map

    return () => {
      map.remove()
      mapRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Recenter the map (e.g. after "use my location" or a map click) without recreating it.
  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    map.flyTo({ center: [center.lon, center.lat], essential: true })

    if (centerMarkerRef.current) {
      centerMarkerRef.current.setLngLat([center.lon, center.lat])
    } else {
      centerMarkerRef.current = new maplibregl.Marker({ color: '#1a73e8' })
        .setLngLat([center.lon, center.lat])
        .addTo(map)
    }
  }, [center])

  // Re-render photo markers whenever the result set, selection, or correction mode changes.
  useEffect(() => {
    const map = mapRef.current
    if (!map) return

    for (const marker of photoMarkersRef.current.values()) {
      marker.remove()
    }
    photoMarkersRef.current.clear()

    for (const photo of photos) {
      const isSelected = photo.id === selectedPhotoId
      const isCorrecting = photo.id === correctionPhotoId
      const marker = new maplibregl.Marker({
        color: isCorrecting ? '#f9ab00' : isSelected ? '#d93025' : '#188038',
        draggable: isCorrecting,
      })
        .setLngLat([photo.lon, photo.lat])
        .addTo(map)

      marker.getElement().addEventListener('click', (event) => {
        event.stopPropagation()
        callbacksRef.current.onMarkerClick(photo)
      })

      if (isCorrecting) {
        marker.on('dragend', () => {
          const lngLat = marker.getLngLat()
          callbacksRef.current.onCorrectionDragEnd(photo.id, { lat: lngLat.lat, lon: lngLat.lng })
        })
      }

      photoMarkersRef.current.set(photo.id, marker)
    }
  }, [photos, selectedPhotoId, correctionPhotoId])

  return <div ref={containerRef} className="map-view" />
}
