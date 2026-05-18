'use client'

import { Button } from '@/components/ui/button'

// FSRS rating scale: 1=Again, 2=Hard, 3=Good, 4=Easy
const RATINGS = [
  { value: 1, label: 'Quên', shortcut: '1', tone: 'destructive' },
  { value: 2, label: 'Khó', shortcut: '2', tone: 'warning' },
  { value: 3, label: 'Tốt', shortcut: '3', tone: 'success' },
  { value: 4, label: 'Dễ', shortcut: '4', tone: 'primary' },
] as const

const TONE_CLASS: Record<(typeof RATINGS)[number]['tone'], string> = {
  destructive: 'bg-destructive text-white hover:bg-destructive/90',
  warning: 'bg-amber-500 text-white hover:bg-amber-500/90',
  success: 'bg-emerald-600 text-white hover:bg-emerald-600/90',
  primary: 'bg-primary text-primary-foreground hover:bg-primary/90',
}

interface RatingButtonsProps {
  onRate: (rating: number) => void
  disabled?: boolean
}

export function RatingButtons({ onRate, disabled }: RatingButtonsProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      {RATINGS.map((r) => (
        <Button
          key={r.value}
          type="button"
          disabled={disabled}
          onClick={() => onRate(r.value)}
          className={`h-auto flex-col py-3 ${TONE_CLASS[r.tone]}`}
        >
          <span className="text-base font-semibold">{r.label}</span>
          <span className="text-xs opacity-80">[{r.shortcut}]</span>
        </Button>
      ))}
    </div>
  )
}
