'use client'

import { useEffect } from 'react'
import { RotateCw } from 'lucide-react'
import { Card } from '@/components/ui/card'
import type { FlashcardDTO } from '@/types/learning.types'

interface FlashcardReviewProps {
  card: FlashcardDTO
  flipped: boolean
  onFlip: () => void
}

/**
 * Two-sided card. Click anywhere to flip. The 3D rotateY transform requires
 * `perspective` on the outer wrapper and `transform-style: preserve-3d` on the
 * inner element — both inlined here because Tailwind 4 ships these as utilities
 * (`perspective-[1000px]`, `transform-3d`, `backface-hidden`, `rotate-y-180`).
 */
export function FlashcardReview({ card, flipped, onFlip }: FlashcardReviewProps) {
  // Reset flip on card change is handled by parent (sets flipped=false when card switches).

  // Keyboard: space/enter flips the card
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.key === ' ' || e.key === 'Enter') && !flipped) {
        e.preventDefault()
        onFlip()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [flipped, onFlip])

  return (
    <div className="perspective-card mx-auto w-full max-w-xl">
      <button
        type="button"
        onClick={onFlip}
        className="relative block h-72 w-full cursor-pointer text-left focus-visible:outline-none sm:h-80"
        aria-label={flipped ? 'Mặt sau thẻ' : 'Bấm để lật thẻ'}
      >
        <div
          className={`transform-3d relative h-full w-full transition-transform duration-500 ${
            flipped ? 'rotate-y-180' : ''
          }`}
        >
          {/* Front */}
          <Card className="backface-hidden absolute inset-0 flex items-center justify-center p-6">
            <div className="text-center">
              <p className="text-4xl font-semibold sm:text-5xl">{card.frontText}</p>
              <div className="text-muted-foreground mt-6 inline-flex items-center gap-1 text-xs">
                <RotateCw className="size-3" />
                Bấm để lật
              </div>
            </div>
          </Card>

          {/* Back */}
          <Card className="backface-hidden rotate-y-180 absolute inset-0 flex items-center justify-center p-6">
            <p className="text-center text-2xl font-medium sm:text-3xl">{card.backText}</p>
          </Card>
        </div>
      </button>
    </div>
  )
}
