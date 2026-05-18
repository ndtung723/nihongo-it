'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { MessageSquare, Search } from 'lucide-react'
import conversationService from '@/services/conversation.service'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { JlptLevel } from '@/types/common.types'
import type { Conversation } from '@/types/conversation.types'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Loader } from '@/components/common/Loader'
import { Pagination } from '@/components/common/Pagination'

const ALL = '__all__'
const JLPT_LEVELS: JlptLevel[] = ['N1', 'N2', 'N3', 'N4', 'N5']
const PAGE_SIZE = 12

export function ConversationList() {
  const toast = useAppToast()
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [jlptLevel, setJlptLevel] = useState<JlptLevel | null>(null)
  const [page, setPage] = useState(0)

  const [items, setItems] = useState<Conversation[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)

  const handleKeywordChange = (v: string) => {
    setPage(0)
    setKeyword(v)
  }
  const handleJlptChange = (v: JlptLevel | null) => {
    setPage(0)
    setJlptLevel(v)
  }

  const fetchItems = useCallback(async () => {
    setLoading(true)
    try {
      // Server has separate /jlpt/{level} endpoint when filtering by level
      const res = jlptLevel
        ? await conversationService.getConversationsByJlptLevel(jlptLevel, page, PAGE_SIZE)
        : await conversationService.getConversations(page, PAGE_SIZE, debouncedKeyword)
      setItems(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setTotalElements(res.totalElements ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được danh sách hội thoại'))
      setItems([])
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword, jlptLevel, page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchItems()
  }, [fetchItems])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Hội thoại</h1>
        <p className="text-muted-foreground text-sm">
          {totalElements > 0
            ? `${totalElements} bài hội thoại`
            : 'Luyện phát âm theo các bài hội thoại tiếng Nhật'}
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-[1fr_140px]">
        <div className="space-y-1.5">
          <Label htmlFor="search">Tìm kiếm</Label>
          <div className="relative">
            <Search className="text-muted-foreground absolute left-2.5 top-1/2 size-4 -translate-y-1/2" />
            <Input
              id="search"
              value={keyword}
              onChange={(e) => handleKeywordChange(e.target.value)}
              placeholder="Tìm theo tiêu đề..."
              className="pl-8"
              disabled={!!jlptLevel}
            />
          </div>
          {jlptLevel && (
            <p className="text-muted-foreground text-xs">
              Tìm kiếm tạm khoá khi đang lọc theo JLPT.
            </p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="jlpt">JLPT</Label>
          <Select
            value={jlptLevel ?? ALL}
            onValueChange={(v) => handleJlptChange(v === ALL ? null : (v as JlptLevel))}
          >
            <SelectTrigger id="jlpt">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>Tất cả</SelectItem>
              {JLPT_LEVELS.map((lvl) => (
                <SelectItem key={lvl} value={lvl}>
                  {lvl}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {loading ? (
        <Loader label="Đang tải..." />
      ) : items.length === 0 ? (
        <p className="text-muted-foreground py-10 text-center">Không tìm thấy hội thoại phù hợp.</p>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((c) => (
            <Link
              key={c.conversationId}
              href={`/conversation/${c.conversationId}/practice`}
              className="block"
            >
              <Card className="hover:border-primary/50 h-full transition-colors">
                <CardContent className="p-4">
                  <div className="flex items-start gap-2">
                    <MessageSquare className="text-primary mt-0.5 size-4 shrink-0" />
                    <h3 className="line-clamp-2 flex-1 font-semibold">{c.title}</h3>
                  </div>
                  {c.description && (
                    <p className="text-muted-foreground mt-2 line-clamp-2 text-sm">
                      {c.description}
                    </p>
                  )}
                  <div className="mt-3 flex items-center gap-2">
                    {c.jlptLevel && <Badge variant="secondary">{c.jlptLevel}</Badge>}
                    {typeof c.unit === 'number' && (
                      <Badge variant="outline">Bài {c.unit}</Badge>
                    )}
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
