'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { ArrowLeft, Eye, Search } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import statisticsService from '@/services/statistics.service'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { UserStatistics } from '@/types/statistics.types'
import { DataTable } from '@/components/data-table/DataTable'
import { Pagination } from '@/components/common/Pagination'
import { Loader } from '@/components/common/Loader'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

const PAGE_SIZE = 10

const fmtPercent = (v?: number) => {
  if (typeof v !== 'number' || Number.isNaN(v)) return '—'
  return `${Math.round(v * 100)}%`
}

export function UserStatsListClient() {
  const toast = useAppToast()
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [page, setPage] = useState(0)
  const [users, setUsers] = useState<UserStatistics[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalItems, setTotalItems] = useState(0)
  const [loading, setLoading] = useState(true)

  const handleKeywordChange = (v: string) => {
    setPage(0)
    setKeyword(v)
  }

  const fetchUsers = useCallback(async () => {
    setLoading(true)
    try {
      const res = await statisticsService.getUsers({
        page,
        size: PAGE_SIZE,
        search: debouncedKeyword || undefined,
      })
      setUsers(res.users ?? [])
      setTotalPages(res.totalPages ?? 0)
      setTotalItems(res.totalItems ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được thống kê người dùng'))
      setUsers([])
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, debouncedKeyword])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchUsers()
  }, [fetchUsers])

  const columns = useMemo<ColumnDef<UserStatistics>[]>(
    () => [
      {
        accessorKey: 'userName',
        header: 'Người dùng',
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.userName}</p>
            <p className="text-muted-foreground text-xs">{row.original.email}</p>
          </div>
        ),
      },
      {
        id: 'totalCards',
        header: 'Tổng thẻ',
        cell: ({ row }) => row.original.summary?.totalCards ?? 0,
      },
      {
        id: 'dueCardsNow',
        header: 'Đến hạn',
        cell: ({ row }) => row.original.summary?.dueCardsNow ?? 0,
      },
      {
        id: 'currentStreak',
        header: 'Streak',
        cell: ({ row }) =>
          row.original.summary?.currentStreak ?? row.original.currentStreak ?? 0,
      },
      {
        id: 'reviews30d',
        header: 'Lượt ôn 30d',
        cell: ({ row }) => row.original.summary?.reviewsLast30Days ?? 0,
      },
      {
        id: 'retention',
        header: 'Ghi nhớ',
        cell: ({ row }) => (
          <span className="font-semibold tabular-nums">
            {fmtPercent(row.original.summary?.overallRetentionRate ?? row.original.retentionRate)}
          </span>
        ),
      },
      {
        id: 'actions',
        header: '',
        enableSorting: false,
        cell: ({ row }) => (
          <Button asChild variant="ghost" size="icon" className="h-8 w-8">
            <Link href={`/statistics/users/${row.original.userId}`} aria-label="Chi tiết">
              <Eye className="size-4" />
            </Link>
          </Button>
        ),
      },
    ],
    [],
  )

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-3">
        <div>
          <Button asChild variant="ghost" size="sm" className="-ml-2 mb-1">
            <Link href="/statistics">
              <ArrowLeft className="mr-1 size-4" />
              Tổng quan
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">Thống kê theo người dùng</h1>
          <p className="text-muted-foreground text-sm">{totalItems} tài khoản</p>
        </div>
      </div>

      <div className="relative max-w-md">
        <Search className="text-muted-foreground absolute left-2.5 top-1/2 size-4 -translate-y-1/2" />
        <Input
          value={keyword}
          onChange={(e) => handleKeywordChange(e.target.value)}
          placeholder="Tìm theo email hoặc tên..."
          className="pl-8"
        />
      </div>

      {loading ? <Loader label="Đang tải..." /> : <DataTable columns={columns} data={users} />}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  )
}
