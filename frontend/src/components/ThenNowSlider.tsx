import { useEffect, useRef, useState } from 'react'
import maplibregl from 'maplibre-gl'
import type { Photo } from '../types'

const MAP_STYLE_URL = 'https://tiles.openfreemap.org/styles/liberty'

interface ThenNowSliderProps {
  photo: Photo
}

/**
 * "Then/now" comparison: the historical photo is clipped over a live small map centered on the
 * same coordinates. A true photo-vs-photo comparison would need a paid Street View / current
 * photo API; the live map is the free/no-key stand-in for the MVP.
 */
export function ThenNowSlider({ photo }: ThenNowSliderProps) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const [position, setPosition] = useState(50)

  useEffect(() => {
    if (!containerRef.current) return
    const map = new maplibregl.Map({
      container: containerRef.current,
      style: MAP_STYLE_URL,
      center: [photo.lon, photo.lat],
      zoom: 17,
      interactive: false,
      attributionControl: false,
    })
    return () => map.remove()
  }, [photo.lon, photo.lat])

  return (
    <div className="then-now">
      <div className="then-now__stage">
        <div ref={containerRef} className="then-now__now" />
        <div className="then-now__then" style={{ clipPath: `inset(0 ${100 - position}% 0 0)` }}>
          <img src={photo.imageUrl} alt={photo.title ?? 'Historical photo'} />
        </div>
        <div className="then-now__divider" style={{ left: `${position}%` }} />
      </div>
      <input
        type="range"
        min={0}
        max={100}
        value={position}
        onChange={(e) => setPosition(Number(e.target.value))}
        className="then-now__slider"
        aria-label="Then/now comparison slider"
      />
      <div className="then-now__labels">
        <span>Then ({photo.takenYear ?? 'unknown year'})</span>
        <span>Now (map view)</span>
      </div>
    </div>
  )
}
