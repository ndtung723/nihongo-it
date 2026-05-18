'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { MoreHorizontal, Pencil, Plus, Power, PowerOff, Search, Trash2 } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import categoryService from '@/services/category.service'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import type { Category } from '@/types/learning.types'
import { DataTable } from '@/components/data-table/DataTable'
import { Loader } from '@/components/common/Loader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { CategoryFormDialog } from './CategoryFormDialog'

export function CategoriesClient() {
  const toast = useAppToast()
  const confirm = useConfirm()

  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Category | null>(null)

  const fetchCategories = useCallback(async () => {
    setLoading(true)
    try {
      const data = debouncedKeyword
        ? await categoryService.search(debouncedKeyword)
        : await categoryService.getAll()
      setCategories(data)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được danh mục'))
      setCategories([])
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedKeyword])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchCategories()
  }, [fetchCategories])

  const handleDelete = useCallback(
    async (cat: Category) => {
      const ok = await confirm({
        title: 'Xoá danh mục',
        message: `"${cat.name}" sẽ bị xoá vĩnh viễn cùng với các chủ đề và từ vựng liên quan.`,
        confirmText: 'Xoá',
        variant: 'destructive',
      })
      if (!ok) return
      try {
        await categoryService.delete(cat.categoryId)
        toast.success('Đã xoá danh mục')
        void fetchCategories()
      } catch (err) {
        toast.error(extractApiError(err, 'Xoá thất bại'))
      }
    },
    [confirm, fetchCategories, toast],
  )

  const handleToggle = useCallback(
    async (cat: Category) => {
      try {
        await categoryService.toggleStatus(cat.categoryId)
        toast.success(cat.isActive ? 'Đã ẩn danh mục' : 'Đã hiển thị danh mục')
        void fetchCategories()
      } catch (err) {
        toast.error(extractApiError(err, 'Cập nhật thất bại'))
      }
    },
    [fetchCategories, toast],
  )

  const handleEdit = useCallback((cat: Category) => {
    setEditing(cat)
    setDialogOpen(true)
  }, [])

  const handleCreate = () => {
    setEditing(null)
    setDialogOpen(true)
  }

  const columns = useMemo<ColumnDef<Category>[]>(
    () => [
      { accessorKey: 'displayOrder', header: '#', size: 60 },
      {
        accessorKey: 'name',
        header: 'Tên',
        cell: ({ row }) => <span className="font-medium">{row.original.name}</span>,
      },
      { accessorKey: 'meaning', header: 'Nghĩa' },
      {
        accessorKey: 'topicCount',
        header: 'Chủ đề',
        cell: ({ row }) => row.original.topicCount ?? 0,
      },
      {
        accessorKey: 'isActive',
        header: 'Trạng thái',
        cell: ({ row }) =>
          row.original.isActive ? (
            <Badge variant="outline" className="border-emerald-500 text-emerald-700">
              Hiện
            </Badge>
          ) : (
            <Badge variant="outline" className="border-muted-foreground text-muted-foreground">
              Ẩn
            </Badge>
          ),
      },
      {
        id: 'actions',
        header: '',
        enableSorting: false,
        cell: ({ row }) => (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreHorizontal className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => handleEdit(row.original)}>
                <Pencil className="mr-2 size-4" />
                Sửa
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => handleToggle(row.original)}>
                {row.original.isActive ? (
                  <>
                    <PowerOff className="mr-2 size-4" />
                    Ẩn
                  </>
                ) : (
                  <>
                    <Power className="mr-2 size-4" />
                    Hiển thị
                  </>
                )}
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => handleDelete(row.original)}
                className="text-destructive"
              >
                <Trash2 className="mr-2 size-4" />
                Xoá
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ),
      },
    ],
    [handleDelete, handleEdit, handleToggle],
  )

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Danh mục</h1>
          <p className="text-muted-foreground text-sm">{categories.length} danh mục</p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="mr-1 size-4" />
          Tạo mới
        </Button>
      </div>

      <div className="relative max-w-md">
        <Search className="text-muted-foreground absolute left-2.5 top-1/2 size-4 -translate-y-1/2" />
        <Input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Tìm theo tên..."
          className="pl-8"
        />
      </div>

      {loading ? (
        <Loader label="Đang tải..." />
      ) : (
        <DataTable columns={columns} data={categories} />
      )}

      <CategoryFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initial={editing}
        onSaved={() => void fetchCategories()}
      />
    </div>
  )
}
