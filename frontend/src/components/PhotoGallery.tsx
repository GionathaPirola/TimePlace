import type { Photo } from '../types'
import { PhotoCard } from './PhotoCard'

interface PhotoGalleryProps {
  photos: Photo[]
  selectedPhotoId: number | null
  loading: boolean
  onSelect: (photo: Photo) => void
}

export function PhotoGallery({ photos, selectedPhotoId, loading, onSelect }: PhotoGalleryProps) {
  if (loading) {
    return <div className="photo-gallery photo-gallery--empty">Loading photos...</div>
  }

  if (photos.length === 0) {
    return (
      <div className="photo-gallery photo-gallery--empty">
        No historical photos found nearby. Try a larger radius or a different spot.
      </div>
    )
  }

  return (
    <div className="photo-gallery">
      {photos.map((photo) => (
        <PhotoCard
          key={photo.id}
          photo={photo}
          selected={photo.id === selectedPhotoId}
          onSelect={onSelect}
        />
      ))}
    </div>
  )
}
