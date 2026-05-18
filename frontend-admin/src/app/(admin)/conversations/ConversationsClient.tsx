'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import {
  Eye,
  MoreHorizontal,
  Pencil,
  Plus,
  Search,
  SquarePen,
  Trash2,
} from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import conversationService from '@/services/conversation.service'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import type { JlptLevel } from '@/types/common.types'
import type { Conversation } from '@/types/conversation.types'
import { DataTable } from '@/components/data-table/DataTable'
import { Pagination } from '@/components/common/Pagination'
import { Loader } from '@/components/common/Loader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
import { ConversationFormDialog } from './ConversationFormDialog'

const ALL = '__all__'
const JLPT_LEVELS: JlptLevel[] = ['N1', 'N2', 'N3', 'N4', 'N5']
const PAGE_SIZE = 10

export function ConversationsClient() {
  const toast = useAppToast()
  const confirm = useConfirm()

  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [jlptLevel, setJlptLevel] = useState<JlptLevel | null>(null)
  const [page, setPage] = useState(0)

  const [items, setItems] = useState<Conversation[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Conversation | null>(null)

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
      const res = jlptLevel
        ? await conversationService.getByJlptLevel(jlptLevel, page, PAGE_SIZE)
        : await conversationService.list({
            page,
            size: PAGE_SIZE,
            search: debouncedKeyword || undefined,
          })
      setItems(res.content ?? [])
      setTotalPages(res.totalPages ?? 0)
      setTotalElements(res.totalElements ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được hội thoại'))
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

  const handleDelete = useCallback(
    async (c: Conversation) => {
      if (!c.conversationId) return
      const ok = await confirm({
        title: 'Xoá hội thoại',
        message: `"${c.title}" và toàn bộ câu thoại sẽ bị xoá vĩnh viễn.`,
        confirmText: 'Xoá',
        variant: 'destructive',
      })
      if (!ok) return
      try {
        await conversationService.delete(c.conversationId)
        toast.success('Đã xoá hội thoại')
        void fetchItems()
      } catch (err) {
        toast.error(extractApiError(err, 'Xoá thất bại'))
      }
    },
    [confirm, fetchItems, toast],
  )

  const handleEdit = useCallback((c: Conversation) => {
    setEditing(c)
    setDialogOpen(true)
  }, [])

  const columns = useMemo<ColumnDef<Conversation>[]>(
    () => [
      {
        accessorKey: 'title',
        header: 'Tiêu đề',
        cell: ({ row }) => <span className="font-medium">{row.original.title}</span>,
      },
      {
        accessorKey: 'jlptLevel',
        header: 'JLPT',
        cell: ({ row }) =>
          row.original.jlptLevel ? (
            <Badge variant="secondary">{row.original.jlptLevel}</Badge>
          ) : (
            <span className="text-muted-foreground text-xs">—</span>
          ),
      },
      {
        accessorKey: 'unit',
        header: 'Bài',
        cell: ({ row }) =>
          typeof row.original.unit === 'number' ? row.original.unit : '—',
      },
      {
        id: 'lineCount',
        header: 'Câu',
        cell: ({ row }) => row.original.lines?.length ?? 0,
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
              <DropdownMenuItem asChild>
                <Link
                  href={`/conversations/${row.original.conversationId}`}
                  className="cursor-pointer"
                >
                  <Eye className="mr-2 size-4" />
                  Xem
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link
                  href={`/conversations/${row.original.conversationId}/edit`}
                  className="cursor-pointer"
                >
                  <SquarePen className="mr-2 size-4" />
                  Sửa câu thoại
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => handleEdit(row.original)}>
                <Pencil className="mr-2 size-4" />
                Sửa thông tin
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
    [handleDelete, handleEdit],
  )

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Hội thoại</h1>
          <p className="text-muted-foreground text-sm">{totalElements} hội thoại</p>
        </div>
        <Button
          onClick={() => {
            setEditing(null)
            setDialogOpen(true)
          }}
        >
          <Plus className="mr-1 size-4" />
          Tạo mới
        </Button>
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
      ) : (
        <DataTable columns={columns} data={items} />
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      <ConversationFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        initial={editing}
        onSaved={() => void fetchItems()}
      />
    </div>
  )
}
