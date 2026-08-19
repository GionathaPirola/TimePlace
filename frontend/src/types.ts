// Mirrors backend com.timeplace.backend.dto.PhotoDto (JSON field names match record component names).
export interface Photo {
  id: number
  source: string
  sourceId: string
  title: string | null
  imageUrl: string
  thumbUrl: string | null
  takenYear: number | null
  takenDate: string | null
  lat: number
  lon: number
  license: string | null
  author: string | null
  attribution: string | null
  verified: boolean
  distanceMeters: number
}

export interface LatLon {
  lat: number
  lon: number
}
