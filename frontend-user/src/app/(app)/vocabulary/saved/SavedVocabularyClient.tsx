'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useVocabularyStore } from '@/stores/vocabulary.store'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { VocabularyCard } from '@/components/vocabulary/VocabularyCard'
import { Pagination } from '@/components/common/Pagination'
import { Loader } from '@/components/common/Loader'

export function SavedVocabularyClient() {
  const toast = useAppToast()
  const items = useVocabularyStore((s) => s.savedVocabulary)
  const totalPages = useVocabularyStore((s) => s.totalSavedPages)
  const totalItems = useVocabularyStore((s) => s.totalSavedItems)
  const loading = useVocabularyStore((s) => s.savedLoading)
  const fetchSaved = useVocabularyStore((s) => s.fetchSavedVocabulary)
  const removeSaved = useVocabularyStore((s) => s.removeSavedItem)
  const resetSaved = useVocabularyStore((s) => s.resetSaved)

  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [page, setPage] = useState(0)

  function handleKeywordChange(v: string) {
    setPage(0)
    setKeyword(v)
  }

  useEffect(() => {
    void fetchSaved(page, 12, debouncedKeyword || undefined).catch((err) => {
      toast.error(err instanceof Error ? err.message : 'Lỗi tải dữ liệu')
    })
    return () => resetSaved()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword, page])

  async function handleSavedChange(vocabId: string, isSaved: boolean) {
    // Card was un-saved from the saved list → remove from view
    if (!isSaved) {
      try {
        await removeSaved(vocabId)
      } catch {
        // Toast already shown by the card's optimistic flow
      }
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Từ vựng đã lưu</h1>
        <p className="text-muted-foreground text-sm">
          {totalItems > 0 ? `${totalItems} từ đã lưu` : 'Lưu các từ vựng để ôn tập sau'}
        </p>
      </div>

      <div className="relative max-w-md">
        <Search className="text-muted-foreground absolute left-2.5 top-1/2 size-4 -translate-y-1/2" />
        <Input
          value={keyword}
          onChange={(e) => handleKeywordChange(e.target.value)}
          placeholder="Tìm trong danh sách đã lưu..."
          className="pl-8"
        />
      </div>

      {loading ? (
        <Loader label="Đang tải..." />
      ) : items.length === 0 ? (
        <div className="space-y-4 py-10 text-center">
          <p className="text-muted-foreground">
            {keyword ? 'Không tìm thấy từ vựng phù hợp.' : 'Chưa có từ vựng nào được lưu.'}
          </p>
          {!keyword && (
            <Button asChild>
              <Link href="/vocabulary">Khám phá từ vựng</Link>
            </Button>
          )}
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {items.map((item) => (
            <VocabularyCard key={item.vocabId} item={item} onSavedChange={handleSavedChange} />
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
