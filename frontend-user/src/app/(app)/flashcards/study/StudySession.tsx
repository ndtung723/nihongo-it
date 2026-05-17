'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { BarChart3, CheckCircle2, RefreshCw } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Loader } from '@/components/common/Loader'
import { FlashcardReview } from '@/components/flashcard/FlashcardReview'
import { RatingButtons } from '@/components/flashcard/RatingButtons'
import { useFlashcardsStore } from '@/stores/flashcards.store'
import flashcardService from '@/services/flashcard.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'

export function StudySession() {
  const toast = useAppToast()
  const dueCards = useFlashcardsStore((s) => s.dueCards)
  const loading = useFlashcardsStore((s) => s.loading)
  const fetchDueCards = useFlashcardsStore((s) => s.fetchDueCards)
  const removeFromDue = useFlashcardsStore((s) => s.removeFromDue)

  const [flipped, setFlipped] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [completedThisSession, setCompletedThisSession] = useState(0)

  useEffect(() => {
    void fetchDueCards().catch((err) => {
      toast.error(err instanceof Error ? err.message : 'Lỗi tải dữ liệu')
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Always work with the first due card — removeFromDue shifts the next one in
  const currentCard = dueCards[0]

  const handleRate = useCallback(
    async (rating: number) => {
      if (!currentCard || submitting) return
      setSubmitting(true)

      const cardId = currentCard.id
      // Optimistic: advance immediately, send review in background
      removeFromDue(cardId)
      setCompletedThisSession((n) => n + 1)
      setFlipped(false)

      try {
        await flashcardService.reviewFlashcard(cardId, rating)
      } catch (err) {
        toast.error(extractApiError(err, 'Gửi đánh giá thất bại'))
      } finally {
        setSubmitting(false)
      }
    },
    [currentCard, submitting, removeFromDue, toast],
  )

  // Keyboard shortcuts 1–4 trigger ratings when card is flipped
  useEffect(() => {
    if (!flipped) return
    function onKey(e: KeyboardEvent) {
      if (e.key >= '1' && e.key <= '4') {
        e.preventDefault()
        void handleRate(Number(e.key))
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [flipped, handleRate])

  if (loading && dueCards.length === 0) return <Loader label="Đang tải thẻ..." />

  // No more due cards — session complete
  if (!currentCard) {
    return (
      <div className="mx-auto max-w-md">
        <Card>
          <CardContent className="space-y-4 p-8 text-center">
            <CheckCircle2 className="text-primary mx-auto size-16" />
            <h2 className="text-2xl font-semibold">
              {completedThisSession > 0 ? 'Hoàn thành!' : 'Không còn thẻ nào'}
            </h2>
            <p className="text-muted-foreground text-sm">
              {completedThisSession > 0
                ? `Bạn đã ôn ${completedThisSession} thẻ trong phiên này.`
                : 'Bạn đã hoàn thành tất cả thẻ ghi nhớ hôm nay.'}
            </p>
            <div className="flex flex-col gap-2 sm:flex-row sm:justify-center">
              <Button asChild variant="outline">
                <Link href="/vocabulary">
                  <RefreshCw className="mr-2 size-4" />
                  Khám phá từ vựng
                </Link>
              </Button>
              <Button asChild>
                <Link href="/flashcards/stats">
                  <BarChart3 className="mr-2 size-4" />
                  Xem thống kê
                </Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  const total = dueCards.length + completedThisSession
  const progress = total > 0 ? (completedThisSession / total) * 100 : 0

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Học thẻ ghi nhớ</h1>
          <p className="text-muted-foreground text-sm">
            {dueCards.length} thẻ còn lại · đã ôn {completedThisSession}
          </p>
        </div>
        <Button asChild variant="ghost" size="sm">
          <Link href="/flashcards/stats">
            <BarChart3 className="size-4" />
          </Link>
        </Button>
      </div>

      <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
        <div
          className="bg-primary h-full transition-all duration-300"
          style={{ width: `${progress}%` }}
        />
      </div>

      <FlashcardReview
        card={currentCard}
        flipped={flipped}
        onFlip={() => setFlipped(true)}
      />

      <div className="min-h-[80px]">
        {flipped ? (
          <RatingButtons onRate={handleRate} disabled={submitting} />
        ) : (
          <p className="text-muted-foreground text-center text-sm">
            Bấm vào thẻ (hoặc <kbd className="rounded border px-1.5 text-xs">Space</kbd>) để xem
            đáp án
          </p>
        )}
      </div>

      {/* Skip to next without rating (rare — used when card display has issues) */}
      <p className="text-muted-foreground text-center text-xs">
        Phím tắt: <kbd className="rounded border px-1.5">1</kbd> Quên ·{' '}
        <kbd className="rounded border px-1.5">2</kbd> Khó ·{' '}
        <kbd className="rounded border px-1.5">3</kbd> Tốt ·{' '}
        <kbd className="rounded border px-1.5">4</kbd> Dễ
      </p>
    </div>
  )
}
