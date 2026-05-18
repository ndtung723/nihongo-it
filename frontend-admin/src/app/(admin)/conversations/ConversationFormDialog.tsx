'use client'

import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { conversationSchema, type ConversationInput } from '@/schemas/conversation.schema'
import conversationService from '@/services/conversation.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { JlptLevel } from '@/types/common.types'
import type { Conversation } from '@/types/conversation.types'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
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

const NONE = '__none__'
const JLPT_LEVELS: JlptLevel[] = ['N1', 'N2', 'N3', 'N4', 'N5']

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial: Conversation | null
  onSaved: (saved: Conversation) => void
}

export function ConversationFormDialog({ open, onOpenChange, initial, onSaved }: Props) {
  const toast = useAppToast()
  const isEdit = !!initial?.conversationId

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<ConversationInput>({
    resolver: zodResolver(conversationSchema),
    defaultValues: { title: '', description: '', jlptLevel: undefined, unit: 0 },
  })

  useEffect(() => {
    if (open) {
      reset({
        title: initial?.title ?? '',
        description: initial?.description ?? '',
        jlptLevel: initial?.jlptLevel,
        unit: initial?.unit ?? 0,
      })
    }
  }, [open, initial, reset])

  async function onSubmit(values: ConversationInput) {
    try {
      const payload: Conversation = {
        ...initial,
        title: values.title,
        description: values.description,
        jlptLevel: values.jlptLevel,
        unit: values.unit,
        // Preserve lines on edit (server-side merge would otherwise drop them
        // if absent from the payload — verified empirically against existing flow)
        lines: initial?.lines,
      }
      const saved =
        isEdit && initial?.conversationId
          ? await conversationService.update(initial.conversationId, payload)
          : await conversationService.create(payload)
      toast.success(isEdit ? 'Đã cập nhật hội thoại' : 'Đã tạo hội thoại')
      onOpenChange(false)
      onSaved(saved)
    } catch (err) {
      toast.error(extractApiError(err, 'Lưu thất bại'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Sửa hội thoại' : 'Tạo hội thoại mới'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="title">Tiêu đề</Label>
            <Input id="title" {...register('title')} aria-invalid={!!errors.title} />
            {errors.title && (
              <p className="text-destructive text-sm">{errors.title.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Mô tả</Label>
            <Textarea id="description" rows={3} {...register('description')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="jlptLevel">Cấp độ JLPT</Label>
              <Controller
                control={control}
                name="jlptLevel"
                render={({ field }) => (
                  <Select
                    value={field.value ?? NONE}
                    onValueChange={(v) =>
                      field.onChange(v === NONE ? undefined : (v as JlptLevel))
                    }
                  >
                    <SelectTrigger id="jlptLevel">
                      <SelectValue placeholder="Chọn cấp độ" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>Không xác định</SelectItem>
                      {JLPT_LEVELS.map((lvl) => (
                        <SelectItem key={lvl} value={lvl}>
                          {lvl}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="unit">Bài số</Label>
              <Input
                id="unit"
                type="number"
                min={0}
                {...register('unit', { valueAsNumber: true })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Huỷ
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
