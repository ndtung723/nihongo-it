'use client'

import { useCallback, useEffect, useState } from 'react'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { VocabularyItem } from '@/types/learning.types'
import vocabularyService from '@/services/vocabulary.service'
import { VocabularyCard } from '@/components/vocabulary/VocabularyCard'
import { Pagination } from '@/components/common/Pagination'
import { Loader } from '@/components/common/Loader'

interface Props {
  topicName: string
}

const PAGE_SIZE = 12

export function TopicVocabularyList({ topicName }: Props) {
  const toast = useAppToast()
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<VocabularyItem[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)

  const fetchItems = useCallback(async () => {
    setLoading(true)
    try {
      const res = await vocabularyService.getVocabulary({
        keyword: null,
        jlptLevel: null,
        topicName,
        page,
        size: PAGE_SIZE,
      })
      setItems(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được danh sách từ vựng'))
      setItems([])
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [topicName, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchItems()
  }, [fetchItems])

  if (loading) return <Loader label="Đang tải..." />
  if (items.length === 0) {
    return <p className="text-muted-foreground py-6 text-center">Chưa có từ vựng nào.</p>
  }
  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {items.map((item) => (
          <VocabularyCard key={item.vocabId} item={item} />
        ))}
      </div>
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
