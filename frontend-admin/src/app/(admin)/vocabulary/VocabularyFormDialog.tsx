'use client'

import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { vocabularySchema, type VocabularyInput } from '@/schemas/learning.schema'
import vocabularyService from '@/services/vocabulary.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { JlptLevel } from '@/types/common.types'
import type { Topic, VocabularyItem } from '@/types/learning.types'
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

const JLPT_LEVELS: JlptLevel[] = ['N1', 'N2', 'N3', 'N4', 'N5']

interface VocabularyFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial: VocabularyItem | null
  topics: Topic[]
  onSaved: () => void
}

export function VocabularyFormDialog({
  open,
  onOpenChange,
  initial,
  topics,
  onSaved,
}: VocabularyFormDialogProps) {
  const toast = useAppToast()
  const isEdit = !!initial

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<VocabularyInput>({
    resolver: zodResolver(vocabularySchema),
    defaultValues: {
      term: '',
      meaning: '',
      pronunciation: '',
      example: '',
      exampleMeaning: '',
      audioPath: '',
      topicName: '',
      jlptLevel: 'N5',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        term: initial?.term ?? '',
        meaning: initial?.meaning ?? '',
        pronunciation: initial?.pronunciation ?? '',
        example: initial?.example ?? '',
        exampleMeaning: initial?.exampleMeaning ?? '',
        audioPath: initial?.audioPath ?? '',
        topicName: initial?.topicName ?? '',
        jlptLevel: initial?.jlptLevel ?? 'N5',
      })
    }
  }, [open, initial, reset])

  async function onSubmit(values: VocabularyInput) {
    try {
      if (isEdit && initial) {
        await vocabularyService.update(initial.vocabId, values)
        toast.success('Đã cập nhật từ vựng')
      } else {
        await vocabularyService.create(values)
        toast.success('Đã tạo từ vựng')
      }
      onOpenChange(false)
      onSaved()
    } catch (err) {
      toast.error(extractApiError(err, 'Lưu thất bại'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Sửa từ vựng' : 'Tạo từ vựng mới'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="term">Từ vựng</Label>
              <Input id="term" {...register('term')} aria-invalid={!!errors.term} />
              {errors.term && (
                <p className="text-destructive text-sm">{errors.term.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="pronunciation">Cách đọc</Label>
              <Input id="pronunciation" {...register('pronunciation')} />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="meaning">Nghĩa</Label>
            <Textarea
              id="meaning"
              rows={2}
              {...register('meaning')}
              aria-invalid={!!errors.meaning}
            />
            {errors.meaning && (
              <p className="text-destructive text-sm">{errors.meaning.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="example">Ví dụ</Label>
            <Textarea id="example" rows={2} {...register('example')} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="exampleMeaning">Nghĩa ví dụ</Label>
            <Textarea id="exampleMeaning" rows={2} {...register('exampleMeaning')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="topicName">Chủ đề</Label>
              <Controller
                control={control}
                name="topicName"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="topicName" aria-invalid={!!errors.topicName}>
                      <SelectValue placeholder="Chọn chủ đề" />
                    </SelectTrigger>
                    <SelectContent>
                      {topics.map((t) => (
                        <SelectItem key={t.topicId} value={t.name}>
                          {t.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.topicName && (
                <p className="text-destructive text-sm">{errors.topicName.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="jlptLevel">Cấp độ JLPT</Label>
              <Controller
                control={control}
                name="jlptLevel"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="jlptLevel">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
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
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="audioPath">Đường dẫn audio (tuỳ chọn)</Label>
            <Input id="audioPath" {...register('audioPath')} placeholder="https://..." />
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
