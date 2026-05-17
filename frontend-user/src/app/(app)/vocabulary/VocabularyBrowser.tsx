'use client'

import { useCallback, useEffect, useState } from 'react'
import vocabularyService from '@/services/vocabulary.service'
import topicService from '@/services/topic.service'
import { extractApiError } from '@/types/common.types'
import type { JlptLevel } from '@/types/common.types'
import type { Topic, VocabularyItem } from '@/types/learning.types'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { VocabularyCard } from '@/components/vocabulary/VocabularyCard'
import { VocabularyFilter } from '@/components/vocabulary/VocabularyFilter'
import { Pagination } from '@/components/common/Pagination'
import { Loader } from '@/components/common/Loader'

const PAGE_SIZE = 12

export function VocabularyBrowser() {
  const toast = useAppToast()
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [jlptLevel, setJlptLevel] = useState<JlptLevel | null>(null)
  const [topicName, setTopicName] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const [items, setItems] = useState<VocabularyItem[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [topics, setTopics] = useState<Topic[]>([])

  // Wrapped setters that reset pagination — avoids an effect for the same purpose
  const handleKeywordChange = (v: string) => {
    setPage(0)
    setKeyword(v)
  }
  const handleJlptChange = (v: JlptLevel | null) => {
    setPage(0)
    setJlptLevel(v)
  }
  const handleTopicChange = (v: string | null) => {
    setPage(0)
    setTopicName(v)
  }

  // Load topic list once for the filter dropdown
  useEffect(() => {
    let cancelled = false
    topicService
      .getAllTopics()
      .then((t) => {
        if (!cancelled) setTopics(t)
      })
      .catch(() => {
        // Topic dropdown is non-critical — silent fail
      })
    return () => {
      cancelled = true
    }
  }, [])

  const fetchItems = useCallback(async () => {
    setLoading(true)
    try {
      const res = await vocabularyService.getVocabulary({
        keyword: debouncedKeyword || null,
        jlptLevel,
        topicName,
        page,
        size: PAGE_SIZE,
      })
      setItems(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setTotalElements(res.totalElements ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được danh sách từ vựng'))
      setItems([])
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword, jlptLevel, topicName, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchItems()
  }, [fetchItems])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Từ vựng</h1>
        <p className="text-muted-foreground text-sm">
          {totalElements > 0 ? `${totalElements} từ vựng` : 'Khám phá từ vựng IT theo cấp độ JLPT'}
        </p>
      </div>

      <VocabularyFilter
        keyword={keyword}
        onKeywordChange={handleKeywordChange}
        jlptLevel={jlptLevel}
        onJlptLevelChange={handleJlptChange}
        topicName={topicName}
        onTopicNameChange={handleTopicChange}
        topics={topics}
      />

      {loading ? (
        <Loader label="Đang tải..." />
      ) : items.length === 0 ? (
        <p className="text-muted-foreground py-10 text-center">Không tìm thấy từ vựng phù hợp.</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {items.map((item) => (
            <VocabularyCard key={item.vocabId} item={item} />
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
