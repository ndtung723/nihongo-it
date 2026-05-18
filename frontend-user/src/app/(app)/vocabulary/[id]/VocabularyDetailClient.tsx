'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { ArrowLeft, Bookmark, BookmarkCheck, Sparkles } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Loader } from '@/components/common/Loader'
import { AudioButton } from '@/components/vocabulary/AudioButton'
import { AIChat } from '@/components/vocabulary/AIChat'
import { VocabularyCard } from '@/components/vocabulary/VocabularyCard'
import { useVocabularyStore } from '@/stores/vocabulary.store'
import aiService from '@/services/ai.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'

interface Props {
  vocabId: string
}

export function VocabularyDetailClient({ vocabId }: Props) {
  const toast = useAppToast()
  const vocab = useVocabularyStore((s) => s.currentVocabulary)
  const related = useVocabularyStore((s) => s.relatedVocabulary)
  const loading = useVocabularyStore((s) => s.loading)
  const fetchVocabularyById = useVocabularyStore((s) => s.fetchVocabularyById)
  const toggleFavorite = useVocabularyStore((s) => s.toggleFavorite)
  const reset = useVocabularyStore((s) => s.reset)

  const [aiExplanation, setAiExplanation] = useState<string | null>(null)
  const [explanationLoading, setExplanationLoading] = useState(false)

  useEffect(() => {
    void fetchVocabularyById(vocabId).catch((err) => {
      toast.error(err instanceof Error ? err.message : 'Lỗi tải dữ liệu')
    })
    return () => reset()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vocabId])

  const loadAiExplanation = useCallback(async () => {
    if (!vocab || explanationLoading) return
    setExplanationLoading(true)
    try {
      const res = await aiService.explainVocabulary(
        vocab.term,
        vocab.pronunciation,
        vocab.meaning,
        vocab.topicName,
        vocab.example,
      )
      setAiExplanation(res.explanation)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được giải thích AI'))
    } finally {
      setExplanationLoading(false)
    }
  }, [vocab, explanationLoading, toast])

  async function handleToggleFavorite() {
    try {
      await toggleFavorite()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Không cập nhật được')
    }
  }

  if (loading && !vocab) return <Loader label="Đang tải..." />
  if (!vocab) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy từ vựng.</p>
        <Button asChild variant="outline">
          <Link href="/vocabulary">← Quay về danh sách</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/vocabulary">
          <ArrowLeft className="mr-1 size-4" />
          Danh sách từ vựng
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <CardTitle className="text-3xl">{vocab.term}</CardTitle>
                <AudioButton text={vocab.term} contentType="vocabulary" />
              </div>
              {vocab.pronunciation && (
                <p className="text-muted-foreground mt-1 text-lg">{vocab.pronunciation}</p>
              )}
            </div>
            <Button
              variant={vocab.isSaved ? 'default' : 'outline'}
              onClick={handleToggleFavorite}
              aria-label={vocab.isSaved ? 'Bỏ lưu' : 'Lưu từ'}
            >
              {vocab.isSaved ? (
                <>
                  <BookmarkCheck className="mr-2 size-4" />
                  Đã lưu
                </>
              ) : (
                <>
                  <Bookmark className="mr-2 size-4" />
                  Lưu
                </>
              )}
            </Button>
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <Badge variant="secondary">{vocab.jlptLevel}</Badge>
            {vocab.topicName && <Badge variant="outline">{vocab.topicName}</Badge>}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <section>
            <h2 className="mb-1 text-sm font-semibold uppercase tracking-wide">Nghĩa</h2>
            <p>{vocab.meaning}</p>
          </section>

          {vocab.example && (
            <section>
              <h2 className="mb-1 text-sm font-semibold uppercase tracking-wide">Ví dụ</h2>
              <div className="bg-muted/40 rounded-lg p-3">
                <div className="flex items-start gap-2">
                  <p className="flex-1 text-lg">{vocab.example}</p>
                  <AudioButton text={vocab.example} contentType="example" size="sm" />
                </div>
                {vocab.exampleMeaning && (
                  <p className="text-muted-foreground mt-2 text-sm">{vocab.exampleMeaning}</p>
                )}
              </div>
            </section>
          )}

          <section>
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-wide">Giải thích AI</h2>
              {!aiExplanation && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={loadAiExplanation}
                  disabled={explanationLoading}
                >
                  <Sparkles className="mr-1 size-3.5" />
                  {explanationLoading ? 'Đang tạo...' : 'Tạo giải thích'}
                </Button>
              )}
            </div>
            {aiExplanation ? (
              <div className="bg-muted/40 whitespace-pre-wrap rounded-lg p-3 text-sm">
                {aiExplanation}
              </div>
            ) : (
              <p className="text-muted-foreground text-sm">
                Nhấn &ldquo;Tạo giải thích&rdquo; để AI giải thích chi tiết về từ này.
              </p>
            )}
          </section>
        </CardContent>
      </Card>

      <AIChat vocabWord={vocab.term} />

      {related.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold">Từ liên quan</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {related.map((item) => (
              <VocabularyCard key={item.vocabId} item={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
