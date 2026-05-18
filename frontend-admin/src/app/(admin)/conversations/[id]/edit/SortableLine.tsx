'use client'

import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { GripVertical, Trash2 } from 'lucide-react'
import type { ConversationLine } from '@/types/conversation.types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

type LineWithId = ConversationLine & { tempId: string }

const SPEAKERS = [
  { value: 'bot', label: 'Nihongo IT (bot)' },
  { value: 'user', label: 'Người dùng' },
]

interface Props {
  line: LineWithId
  index: number
  onChange: (patch: Partial<ConversationLine>) => void
  onDelete: () => void
}

export function SortableLine({ line, index, onChange, onDelete }: Props) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: line.tempId,
  })

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="bg-card flex items-stretch gap-2 rounded-lg border p-3"
    >
      <button
        type="button"
        className="text-muted-foreground hover:text-foreground flex w-6 cursor-grab items-center justify-center active:cursor-grabbing"
        aria-label={`Kéo để sắp xếp câu ${index + 1}`}
        {...attributes}
        {...listeners}
      >
        <GripVertical className="size-5" />
      </button>

      <div className="flex-1 space-y-2">
        <div className="grid gap-2 sm:grid-cols-[160px_1fr]">
          <div className="space-y-1">
            <Label className="text-muted-foreground text-xs uppercase">Người nói</Label>
            <Select
              value={line.speaker || 'bot'}
              onValueChange={(v) => onChange({ speaker: v })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {SPEAKERS.map((s) => (
                  <SelectItem key={s.value} value={s.value}>
                    {s.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1">
            <Label className="text-muted-foreground text-xs uppercase">Câu tiếng Nhật</Label>
            <Textarea
              value={line.japaneseText}
              onChange={(e) => onChange({ japaneseText: e.target.value })}
              rows={2}
              className="resize-none"
            />
          </div>
        </div>

        <div className="grid gap-2 sm:grid-cols-2">
          <div className="space-y-1">
            <Label className="text-muted-foreground text-xs uppercase">Dịch tiếng Việt</Label>
            <Textarea
              value={line.vietnameseTranslation ?? ''}
              onChange={(e) => onChange({ vietnameseTranslation: e.target.value })}
              rows={2}
              className="resize-none"
            />
          </div>

          <div className="space-y-1">
            <Label className="text-muted-foreground text-xs uppercase">Ghi chú</Label>
            <Input
              value={line.notes ?? ''}
              onChange={(e) => onChange({ notes: e.target.value })}
              placeholder="Tuỳ chọn"
            />
          </div>
        </div>

        <div className="space-y-1">
          <Label className="text-muted-foreground text-xs uppercase">Từ quan trọng</Label>
          <Input
            value={line.importantVocab ?? ''}
            onChange={(e) => onChange({ importantVocab: e.target.value })}
            placeholder="Tuỳ chọn — cách nhau bằng dấu phẩy"
          />
        </div>
      </div>

      <Button
        type="button"
        variant="ghost"
        size="icon"
        onClick={onDelete}
        className="text-destructive shrink-0 self-start"
        aria-label={`Xoá câu ${index + 1}`}
      >
        <Trash2 className="size-4" />
      </Button>
    </div>
  )
}
