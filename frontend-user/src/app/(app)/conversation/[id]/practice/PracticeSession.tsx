'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowLeft, ChevronLeft, ChevronRight, RotateCw } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Loader } from '@/components/common/Loader'
import { AudioButton } from '@/components/vocabulary/AudioButton'
import { AudioRecorderButton } from '@/components/conversation/AudioRecorderButton'
import { SpeechFeedback } from '@/components/conversation/SpeechFeedback'
import conversationService from '@/services/conversation.service'
import aiService from '@/services/ai.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { Conversation, ConversationLine } from '@/types/conversation.types'
import type { SpeechAnalysisResult } from '@/types/ai.types'

interface Props {
  conversationId: string
}

export function PracticeSession({ conversationId }: Props) {
  const toast = useAppToast()

  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeIndex, setActiveIndex] = useState(0)

  // Per-line feedback keyed by lineIndex
  const [feedback, setFeedback] = useState<Record<number, SpeechAnalysisResult>>({})
  const [analyzing, setAnalyzing] = useState(false)

  useEffect(() => {
    let cancelled = false
    conversationService
      .getConversationById(conversationId)
      .then((c) => {
        if (!cancelled) setConversation(c)
      })
      .catch((err) => {
        if (!cancelled) toast.error(extractApiError(err, 'Không tải được hội thoại'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversationId])

  const lines: ConversationLine[] = conversation?.lines ?? []
  const currentLine = lines[activeIndex]

  const handleRecorded = useCallback(
    async (blob: Blob) => {
      if (!currentLine) return
      setAnalyzing(true)
      try {
        const result = await aiService.analyzeSpeech(
          blob,
          currentLine.japaneseText,
          'conversation',
        )
        setFeedback((prev) => ({ ...prev, [activeIndex]: result }))
      } catch (err) {
        toast.error(extractApiError(err, 'Phân tích phát âm thất bại'))
      } finally {
        setAnalyzing(false)
      }
    },
    [currentLine, activeIndex, toast],
  )

  const goPrev = () => setActiveIndex((i) => Math.max(0, i - 1))
  const goNext = () => setActiveIndex((i) => Math.min(lines.length - 1, i + 1))
  const retry = () =>
    setFeedback((prev) => {
      const next = { ...prev }
      delete next[activeIndex]
      return next
    })

  if (loading) return <Loader label="Đang tải hội thoại..." />

  if (!conversation) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy hội thoại.</p>
        <Button asChild variant="outline">
          <Link href="/conversation">← Danh sách hội thoại</Link>
        </Button>
      </div>
    )
  }

  if (lines.length === 0) {
    return (
      <div className="space-y-4">
        <Button asChild variant="ghost" size="sm" className="-ml-2">
          <Link href="/conversation">
            <ArrowLeft className="mr-1 size-4" />
            Danh sách
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">{conversation.title}</h1>
        <p className="text-muted-foreground py-6 text-center">
          Hội thoại này chưa có câu nào để luyện tập.
        </p>
      </div>
    )
  }

  const currentFeedback = feedback[activeIndex]
  const completedCount = Object.keys(feedback).length

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/conversation">
          <ArrowLeft className="mr-1 size-4" />
          Danh sách
        </Link>
      </Button>

      <div>
        <h1 className="text-xl font-semibold">{conversation.title}</h1>
        <p className="text-muted-foreground text-sm">
          Câu {activeIndex + 1} / {lines.length} · đã luyện {completedCount}
        </p>
      </div>

      <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
        <div
          className="bg-primary h-full transition-all duration-300"
          style={{ width: `${((activeIndex + 1) / lines.length) * 100}%` }}
        />
      </div>

      <Card>
        <CardContent className="space-y-4 p-5">
          <div className="flex items-start justify-between gap-2">
            <Badge variant="outline" className="capitalize">
              {currentLine?.speaker || '—'}
            </Badge>
            {currentLine && (
              <AudioButton text={currentLine.japaneseText} contentType="conversation" size="sm" />
            )}
          </div>

          <p className="text-2xl leading-relaxed">{currentLine?.japaneseText}</p>

          {currentLine?.vietnameseTranslation && (
            <p className="text-muted-foreground text-sm italic">
              {currentLine.vietnameseTranslation}
            </p>
          )}

          {currentLine?.notes && (
            <div className="bg-muted/40 rounded-md p-3 text-sm">
              <span className="text-muted-foreground mr-1 text-xs uppercase">Ghi chú:</span>
              {currentLine.notes}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col items-center gap-3 py-2">
        {currentFeedback ? (
          <Button variant="outline" size="sm" onClick={retry}>
            <RotateCw className="mr-1 size-4" />
            Thử lại
          </Button>
        ) : (
          <AudioRecorderButton onRecorded={handleRecorded} disabled={analyzing} />
        )}
        {analyzing && <Loader label="Đang phân tích phát âm..." />}
      </div>

      {currentFeedback && <SpeechFeedback result={currentFeedback} />}

      <div className="flex items-center justify-between pt-2">
        <Button variant="outline" onClick={goPrev} disabled={activeIndex === 0}>
          <ChevronLeft className="mr-1 size-4" />
          Câu trước
        </Button>
        <Button onClick={goNext} disabled={activeIndex === lines.length - 1}>
          Câu sau
          <ChevronRight className="ml-1 size-4" />
        </Button>
      </div>
    </div>
  )
}
