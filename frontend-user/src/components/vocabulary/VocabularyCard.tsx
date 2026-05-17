'use client'

import Link from 'next/link'
import { Bookmark, BookmarkCheck } from 'lucide-react'
import { useState } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import vocabularyService from '@/services/vocabulary.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { VocabularyItem } from '@/types/learning.types'

interface VocabularyCardProps {
  item: VocabularyItem
  onSavedChange?: (vocabId: string, isSaved: boolean) => void
}

export function VocabularyCard({ item, onSavedChange }: VocabularyCardProps) {
  const [isSaved, setIsSaved] = useState(item.isSaved)
  const [busy, setBusy] = useState(false)
  const toast = useAppToast()

  async function handleToggle(e: React.MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    if (busy) return
    setBusy(true)
    const next = !isSaved
    // Optimistic toggle
    setIsSaved(next)
    try {
      if (next) await vocabularyService.saveVocabulary(item.vocabId)
      else await vocabularyService.removeSavedVocabulary(item.vocabId)
      onSavedChange?.(item.vocabId, next)
    } catch (err) {
      setIsSaved(!next) // revert
      toast.error(extractApiError(err, 'Không cập nhật được trạng thái'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Link href={`/vocabulary/${item.vocabId}`} className="block">
      <Card className="hover:border-primary/50 h-full transition-colors">
        <CardContent className="p-4">
          <div className="mb-2 flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <h3 className="truncate text-lg font-semibold">{item.term}</h3>
              {item.pronunciation && (
                <p className="text-muted-foreground truncate text-sm">{item.pronunciation}</p>
              )}
            </div>
            <Button
              size="icon"
              variant="ghost"
              onClick={handleToggle}
              disabled={busy}
              aria-label={isSaved ? 'Bỏ lưu' : 'Lưu từ vựng'}
              className="-mr-2 h-8 w-8 shrink-0"
            >
              {isSaved ? (
                <BookmarkCheck className="text-primary size-4" />
              ) : (
                <Bookmark className="text-muted-foreground size-4" />
              )}
            </Button>
          </div>
          <p className="line-clamp-2 text-sm">{item.meaning}</p>
          <div className="mt-3 flex items-center gap-2">
            <Badge variant="secondary">{item.jlptLevel}</Badge>
            {item.topicName && (
              <Badge variant="outline" className="truncate">
                {item.topicName}
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
