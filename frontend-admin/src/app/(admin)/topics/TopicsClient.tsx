'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import { MoreHorizontal, Pencil, Plus, Power, PowerOff, Trash2 } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import categoryService from '@/services/category.service'
import topicService from '@/services/topic.service'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import type { Category, Topic } from '@/types/learning.types'
import { DataTable } from '@/components/data-table/DataTable'
import { Loader } from '@/components/common/Loader'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { TopicFormDialog } from './TopicFormDialog'

const ALL = '__all__'

export function TopicsClient() {
  const toast = useAppToast()
  const confirm = useConfirm()

  const [categories, setCategories] = useState<Category[]>([])
  const [topics, setTopics] = useState<Topic[]>([])
  const [loading, setLoading] = useState(true)
  const [filterCategoryId, setFilterCategoryId] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Topic | null>(null)

  useEffect(() => {
    let cancelled = false
    categoryService
      .getAll()
      .then((cs) => {
        if (!cancelled) setCategories(cs)
      })
      .catch(() => {
        // non-fatal - the filter dropdown will be empty
      })
    return () => {
      cancelled = true
    }
  }, [])

  const fetchTopics = useCallback(async () => {
    setLoading(true)
    try {
      const data = filterCategoryId
        ? await topicService.getByCategoryId(filterCategoryId)
        : await topicService.getAll()
      setTopics(data)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được chủ đề'))
      setTopics([])
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterCategoryId])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchTopics()
  }, [fetchTopics])

  const handleDelete = useCallback(
    async (topic: Topic) => {
      const ok = await confirm({
        title: 'Xoá chủ đề',
        message: `"${topic.name}" sẽ bị xoá vĩnh viễn cùng với các từ vựng liên quan.`,
        confirmText: 'Xoá',
        variant: 'destructive',
      })
      if (!ok) return
      try {
        await topicService.delete(topic.topicId)
        toast.success('Đã xoá chủ đề')
        void fetchTopics()
      } catch (err) {
        toast.error(extractApiError(err, 'Xoá thất bại'))
      }
    },
    [confirm, fetchTopics, toast],
  )

  const handleToggle = useCallback(
    async (topic: Topic) => {
      try {
        await topicService.toggleStatus(topic.topicId)
        toast.success(topic.isActive ? 'Đã ẩn' : 'Đã hiển thị')
        void fetchTopics()
      } catch (err) {
        toast.error(extractApiError(err, 'Cập nhật thất bại'))
      }
    },
    [fetchTopics, toast],
  )

  const handleEdit = useCallback((topic: Topic) => {
    setEditing(topic)
    setDialogOpen(true)
  }, [])

  const columns = useMemo<ColumnDef<Topic>[]>(
    () => [
      { accessorKey: 'displayOrder', header: '#', size: 60 },
      {
        accessorKey: 'name',
        header: 'Tên',
        cell: ({ row }) => <span className="font-medium">{row.original.name}</span>,
      },
      { accessorKey: 'meaning', header: 'Nghĩa' },
      { accessorKey: 'categoryName', header: 'Danh mục' },
      {
        accessorKey: 'vocabularyCount',
        header: 'Từ vựng',
        cell: ({ row }) => row.original.vocabularyCount ?? 0,
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
          <h1 className="text-2xl font-bold">Chủ đề</h1>
          <p className="text-muted-foreground text-sm">{topics.length} chủ đề</p>
        </div>
        <Button
          onClick={() => {
            setEditing(null)
            setDialogOpen(true)
          }}
          disabled={categories.length === 0}
        >
          <Plus className="mr-1 size-4" />
          Tạo mới
        </Button>
      </div>

      <div className="grid gap-3 sm:max-w-md">
        <div className="space-y-1.5">
          <Label htmlFor="filter-category">Lọc theo danh mục</Label>
          <Select
            value={filterCategoryId ?? ALL}
            onValueChange={(v) => setFilterCategoryId(v === ALL ? null : v)}
          >
            <SelectTrigger id="filter-category">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>Tất cả</SelectItem>
              {categories.map((c) => (
                <SelectItem key={c.categoryId} value={c.categoryId}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {loading ? (
        <Loader label="Đang tải..." />
      ) : (
        <DataTable columns={columns} data={topics} />
      )}

      <TopicFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initial={editing}
        categories={categories}
        defaultCategoryId={filterCategoryId ?? undefined}
        onSaved={() => void fetchTopics()}
      />
    </div>
  )
}
