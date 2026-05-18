'use client'

import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { topicSchema, type TopicInput } from '@/schemas/learning.schema'
import topicService from '@/services/topic.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { Category, Topic } from '@/types/learning.types'
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

interface TopicFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial: Topic | null
  categories: Category[]
  defaultCategoryId?: string
  onSaved: () => void
}

export function TopicFormDialog({
  open,
  onOpenChange,
  initial,
  categories,
  defaultCategoryId,
  onSaved,
}: TopicFormDialogProps) {
  const toast = useAppToast()
  const isEdit = !!initial

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<TopicInput>({
    resolver: zodResolver(topicSchema),
    defaultValues: {
      name: '',
      meaning: '',
      description: '',
      displayOrder: 0,
      categoryId: '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        name: initial?.name ?? '',
        meaning: initial?.meaning ?? '',
        description: '',
        displayOrder: initial?.displayOrder ?? 0,
        categoryId: initial?.categoryId ?? defaultCategoryId ?? '',
      })
    }
  }, [open, initial, defaultCategoryId, reset])

  async function onSubmit(values: TopicInput) {
    try {
      if (isEdit && initial) {
        await topicService.update(initial.topicId, values)
        toast.success('Đã cập nhật chủ đề')
      } else {
        await topicService.create(values)
        toast.success('Đã tạo chủ đề')
      }
      onOpenChange(false)
      onSaved()
    } catch (err) {
      toast.error(extractApiError(err, 'Lưu thất bại'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Sửa chủ đề' : 'Tạo chủ đề mới'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="categoryId">Danh mục</Label>
            <Controller
              control={control}
              name="categoryId"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="categoryId" aria-invalid={!!errors.categoryId}>
                    <SelectValue placeholder="Chọn danh mục" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => (
                      <SelectItem key={c.categoryId} value={c.categoryId}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {errors.categoryId && (
              <p className="text-destructive text-sm">{errors.categoryId.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="name">Tên chủ đề</Label>
            <Input id="name" {...register('name')} aria-invalid={!!errors.name} />
            {errors.name && (
              <p className="text-destructive text-sm">{errors.name.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="meaning">Nghĩa</Label>
            <Input id="meaning" {...register('meaning')} aria-invalid={!!errors.meaning} />
            {errors.meaning && (
              <p className="text-destructive text-sm">{errors.meaning.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Mô tả</Label>
            <Textarea id="description" rows={3} {...register('description')} />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="displayOrder">Thứ tự hiển thị</Label>
            <Input
              id="displayOrder"
              type="number"
              min={0}
              {...register('displayOrder', { valueAsNumber: true })}
            />
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
