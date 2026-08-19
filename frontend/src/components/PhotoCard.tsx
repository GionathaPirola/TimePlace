import type { Photo } from '../types'

interface PhotoCardProps {
  photo: Photo
  selected: boolean
  onSelect: (photo: Photo) => void
}

export function PhotoCard({ photo, selected, onSelect }: PhotoCardProps) {
  return (
    <button
      type="button"
      className={`photo-card${selected ? ' photo-card--selected' : ''}`}
      onClick={() => onSelect(photo)}
    >
      <img
        src={photo.thumbUrl ?? photo.imageUrl}
        alt={photo.title ?? 'Historical photo'}
        loading="lazy"
      />
      <div className="photo-card__info">
        <span className="photo-card__year">{photo.takenYear ?? 'Unknown year'}</span>
        <span className="photo-card__distance">{Math.round(photo.distanceMeters)} m away</span>
      </div>
    </button>
  )
}
