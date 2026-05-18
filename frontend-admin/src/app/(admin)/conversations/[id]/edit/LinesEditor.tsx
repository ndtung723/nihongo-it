'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import {
  closestCenter,
  DndContext,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { ArrowLeft, Plus, Save } from 'lucide-react'
import conversationService from '@/services/conversation.service'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import type { Conversation, ConversationLine } from '@/types/conversation.types'
import { Button } from '@/components/ui/button'
import { Loader } from '@/components/common/Loader'
import { SortableLine } from './SortableLine'

interface Props {
  conversationId: string
}

type LineWithId = ConversationLine & { tempId: string }

let counter = 0
const nextId = () => `line-${Date.now()}-${counter++}`

function withTempIds(lines: ConversationLine[]): LineWithId[] {
  return lines.map((line) => ({
    ...line,
    tempId: line.lineId ?? nextId(),
  }))
}

export function LinesEditor({ conversationId }: Props) {
  const router = useRouter()
  const toast = useAppToast()
  const confirm = useConfirm()

  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [lines, setLines] = useState<LineWithId[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [dirty, setDirty] = useState(false)

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  useEffect(() => {
    let cancelled = false
    conversationService
      .getById(conversationId)
      .then((c) => {
        if (cancelled) return
        setConversation(c)
        const sorted = (c.lines ?? []).slice().sort((a, b) => a.orderIndex - b.orderIndex)
        setLines(withTempIds(sorted))
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

  // Warn before leaving with unsaved changes
  useEffect(() => {
    if (!dirty) return
    function onBeforeUnload(e: BeforeUnloadEvent) {
      e.preventDefault()
    }
    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [dirty])

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { active, over } = event
    if (!over || active.id === over.id) return
    setLines((prev) => {
      const oldIndex = prev.findIndex((l) => l.tempId === active.id)
      const newIndex = prev.findIndex((l) => l.tempId === over.id)
      if (oldIndex === -1 || newIndex === -1) return prev
      return arrayMove(prev, oldIndex, newIndex)
    })
    setDirty(true)
  }, [])

  const updateLine = useCallback((tempId: string, patch: Partial<ConversationLine>) => {
    setLines((prev) => prev.map((l) => (l.tempId === tempId ? { ...l, ...patch } : l)))
    setDirty(true)
  }, [])

  const deleteLine = useCallback(
    async (tempId: string) => {
      const ok = await confirm({
        title: 'Xoá câu thoại',
        message: 'Câu này sẽ bị xoá khỏi hội thoại (chưa lưu vào server cho đến khi bấm Lưu).',
        confirmText: 'Xoá',
        variant: 'destructive',
      })
      if (!ok) return
      setLines((prev) => prev.filter((l) => l.tempId !== tempId))
      setDirty(true)
    },
    [confirm],
  )

  const addLine = useCallback(() => {
    setLines((prev) => [
      ...prev,
      {
        tempId: nextId(),
        speaker: 'bot',
        japaneseText: '',
        vietnameseTranslation: '',
        notes: '',
        importantVocab: '',
        orderIndex: prev.length,
      },
    ])
    setDirty(true)
  }, [])

  const save = async () => {
    if (!conversation) return
    setSaving(true)
    try {
      const payload: Conversation = {
        ...conversation,
        lines: lines.map((l, index) => {
          // Drop the client-only tempId before sending to server
          const { tempId, ...rest } = l
          void tempId
          return { ...rest, orderIndex: index }
        }),
      }
      await conversationService.update(conversationId, payload)
      toast.success('Đã lưu thay đổi')
      setDirty(false)
    } catch (err) {
      toast.error(extractApiError(err, 'Lưu thất bại'))
    } finally {
      setSaving(false)
    }
  }

  const handleBack = async () => {
    if (dirty) {
      const ok = await confirm({
        title: 'Bỏ thay đổi?',
        message: 'Bạn có thay đổi chưa lưu. Tiếp tục sẽ mất các thay đổi đó.',
        confirmText: 'Bỏ thay đổi',
        variant: 'destructive',
      })
      if (!ok) return
    }
    router.push(`/conversations/${conversationId}`)
  }

  if (loading) return <Loader label="Đang tải..." />
  if (!conversation) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy hội thoại.</p>
        <Button asChild variant="outline">
          <Link href="/conversations">← Danh sách</Link>
        </Button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex items-center justify-between gap-3">
        <Button variant="ghost" size="sm" onClick={handleBack} className="-ml-2">
          <ArrowLeft className="mr-1 size-4" />
          Quay lại
        </Button>
        <Button onClick={save} disabled={saving || !dirty}>
          <Save className="mr-1 size-4" />
          {saving ? 'Đang lưu...' : dirty ? 'Lưu thay đổi' : 'Đã lưu'}
        </Button>
      </div>

      <div>
        <h1 className="text-2xl font-bold">{conversation.title}</h1>
        <p className="text-muted-foreground text-sm">
          Kéo thả để sắp xếp · {lines.length} câu
        </p>
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={lines.map((l) => l.tempId)} strategy={verticalListSortingStrategy}>
          <div className="space-y-3">
            {lines.map((line, index) => (
              <SortableLine
                key={line.tempId}
                line={line}
                index={index}
                onChange={(patch) => updateLine(line.tempId, patch)}
                onDelete={() => void deleteLine(line.tempId)}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>

      <div className="flex justify-center pt-2">
        <Button variant="outline" onClick={addLine}>
          <Plus className="mr-1 size-4" />
          Thêm câu thoại
        </Button>
      </div>
    </div>
  )
}
