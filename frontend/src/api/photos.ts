import { API_BASE_URL } from '../config'
import type { Photo } from '../types'

export interface NearbyParams {
  lat: number
  lon: number
  radius: number
  yearFrom?: number
  yearTo?: number
}

export async function fetchNearbyPhotos(params: NearbyParams): Promise<Photo[]> {
  const query = new URLSearchParams({
    lat: String(params.lat),
    lon: String(params.lon),
    radius: String(params.radius),
  })
  if (params.yearFrom !== undefined) query.set('yearFrom', String(params.yearFrom))
  if (params.yearTo !== undefined) query.set('yearTo', String(params.yearTo))

  const response = await fetch(`${API_BASE_URL}/photos/nearby?${query.toString()}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch nearby photos: ${response.status}`)
  }
  return response.json()
}

export async function correctPhotoLocation(photoId: number, lat: number, lon: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/photos/${photoId}/correct-location`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lat, lon }),
  })
  if (!response.ok) {
    throw new Error(`Failed to submit location correction: ${response.status}`)
  }
}
