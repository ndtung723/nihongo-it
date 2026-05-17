'use client'

import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { JlptLevel } from '@/types/common.types'
import type { Topic } from '@/types/learning.types'

const ALL = '__all__'
const JLPT_LEVELS: JlptLevel[] = ['N1', 'N2', 'N3', 'N4', 'N5']

interface VocabularyFilterProps {
  keyword: string
  onKeywordChange: (v: string) => void
  jlptLevel: JlptLevel | null
  onJlptLevelChange: (v: JlptLevel | null) => void
  topicName: string | null
  onTopicNameChange: (v: string | null) => void
  topics?: Topic[]
}

export function VocabularyFilter({
  keyword,
  onKeywordChange,
  jlptLevel,
  onJlptLevelChange,
  topicName,
  onTopicNameChange,
  topics = [],
}: VocabularyFilterProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-[1fr_140px_200px]">
      <div className="space-y-1.5">
        <Label htmlFor="search">Tìm kiếm</Label>
        <div className="relative">
          <Search className="text-muted-foreground absolute left-2.5 top-1/2 size-4 -translate-y-1/2" />
          <Input
            id="search"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="Nhập từ hoặc nghĩa..."
            className="pl-8"
          />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="jlpt">JLPT</Label>
        <Select
          value={jlptLevel ?? ALL}
          onValueChange={(v) => onJlptLevelChange(v === ALL ? null : (v as JlptLevel))}
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

      <div className="space-y-1.5">
        <Label htmlFor="topic">Chủ đề</Label>
        <Select
          value={topicName ?? ALL}
          onValueChange={(v) => onTopicNameChange(v === ALL ? null : v)}
        >
          <SelectTrigger id="topic">
            <SelectValue placeholder="Tất cả" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>Tất cả chủ đề</SelectItem>
            {topics.map((t) => (
              <SelectItem key={t.topicId} value={t.name}>
                {t.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  )
}
