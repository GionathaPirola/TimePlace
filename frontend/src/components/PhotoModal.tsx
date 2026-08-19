import type { LatLon, Photo } from '../types'
import { ThenNowSlider } from './ThenNowSlider'

interface PhotoModalProps {
  photo: Photo
  onClose: () => void
  correctionMode: boolean
  onToggleCorrectionMode: () => void
  pendingCorrection: LatLon | null
  onConfirmCorrection: () => void
  onCancelCorrection: () => void
  submitting: boolean
}

export function PhotoModal({
  photo,
  onClose,
  correctionMode,
  onToggleCorrectionMode,
  pendingCorrection,
  onConfirmCorrection,
  onCancelCorrection,
  submitting,
}: PhotoModalProps) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button type="button" className="modal__close" onClick={onClose} aria-label="Close">
          &times;
        </button>

        <ThenNowSlider photo={photo} />

        <div className="modal__details">
          <h2>{photo.title ?? 'Untitled photo'}</h2>
          <dl>
            <dt>Year</dt>
            <dd>{photo.takenYear ?? 'Unknown'}</dd>
            <dt>Author</dt>
            <dd>{photo.author ?? 'Unknown'}</dd>
            <dt>License</dt>
            <dd>{photo.license ?? 'Unknown'}</dd>
            <dt>Attribution</dt>
            <dd>{photo.attribution ?? '-'}</dd>
            <dt>Source</dt>
            <dd>{photo.source}</dd>
          </dl>
        </div>

        <div className="modal__correction">
          {!correctionMode && (
            <button type="button" onClick={onToggleCorrectionMode}>
              Fix photo location
            </button>
          )}
          {correctionMode && (
            <div className="modal__correction-active">
              <p>Drag the marker on the map to the correct spot, then confirm.</p>
              <div className="modal__correction-actions">
                <button
                  type="button"
                  disabled={!pendingCorrection || submitting}
                  onClick={onConfirmCorrection}
                >
                  {submitting ? 'Saving...' : 'Confirm new location'}
                </button>
                <button type="button" onClick={onCancelCorrection} disabled={submitting}>
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
