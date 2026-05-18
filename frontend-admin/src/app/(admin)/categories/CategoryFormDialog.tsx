'use client'

import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { categorySchema, type CategoryInput } from '@/schemas/learning.schema'
import categoryService from '@/services/category.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { Category } from '@/types/learning.types'
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

interface CategoryFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initial: Category | null
  onSaved: () => void
}

export function CategoryFormDialog({
  open,
  onOpenChange,
  initial,
  onSaved,
}: CategoryFormDialogProps) {
  const toast = useAppToast()
  const isEdit = !!initial

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CategoryInput>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: '', meaning: '', description: '', displayOrder: 0 },
  })

  useEffect(() => {
    if (open) {
      reset({
        name: initial?.name ?? '',
        meaning: initial?.meaning ?? '',
        description: '',
        displayOrder: initial?.displayOrder ?? 0,
      })
    }
  }, [open, initial, reset])

  async function onSubmit(values: CategoryInput) {
    try {
      if (isEdit && initial) {
        await categoryService.update(initial.categoryId, values)
        toast.success('Đã cập nhật danh mục')
      } else {
        await categoryService.create(values)
        toast.success('Đã tạo danh mục')
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
          <DialogTitle>{isEdit ? 'Sửa danh mục' : 'Tạo danh mục mới'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="name">Tên danh mục</Label>
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
