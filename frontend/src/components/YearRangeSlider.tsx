import type { ChangeEvent } from 'react'

interface YearRangeSliderProps {
  min: number
  max: number
  valueFrom: number
  valueTo: number
  onChange: (from: number, to: number) => void
}

/** Dual-thumb range slider built from two overlapping <input type="range"> elements. */
export function YearRangeSlider({ min, max, valueFrom, valueTo, onChange }: YearRangeSliderProps) {
  const handleFromChange = (e: ChangeEvent<HTMLInputElement>) => {
    const next = Math.min(Number(e.target.value), valueTo)
    onChange(next, valueTo)
  }

  const handleToChange = (e: ChangeEvent<HTMLInputElement>) => {
    const next = Math.max(Number(e.target.value), valueFrom)
    onChange(valueFrom, next)
  }

  return (
    <div className="year-range">
      <div className="year-range__labels">
        <span>{valueFrom}</span>
        <span>Year range</span>
        <span>{valueTo}</span>
      </div>
      <div className="year-range__track">
        <input type="range" min={min} max={max} value={valueFrom} onChange={handleFromChange} />
        <input type="range" min={min} max={max} value={valueTo} onChange={handleToChange} />
      </div>
    </div>
  )
}
