'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { Eye, MoreHorizontal, Power, PowerOff, Search } from 'lucide-react'
import type { ColumnDef } from '@tanstack/react-table'
import adminService from '@/services/admin.service'
import { useDebounce } from '@/hooks/useDebounce'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import { ROLES } from '@/types/roles'
import type { UserInfo } from '@/types/user.types'
import { DataTable } from '@/components/data-table/DataTable'
import { Pagination } from '@/components/common/Pagination'
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

const PAGE_SIZE = 10

const fmtDate = (s?: string) => (s ? new Date(s).toLocaleString('vi-VN') : '—')

export function UsersClient() {
  const toast = useAppToast()
  const confirm = useConfirm()

  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebounce(keyword, 400)
  const [page, setPage] = useState(0)
  const [users, setUsers] = useState<UserInfo[]>([])
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
      const res = await adminService.getUsers(page, PAGE_SIZE, debouncedKeyword || undefined)
      setUsers(res.users ?? [])
      setTotalPages(res.totalPages ?? 0)
      setTotalItems(res.totalItems ?? 0)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được danh sách người dùng'))
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

  const toggleActive = useCallback(
    async (user: UserInfo) => {
      const willActivate = !user.isActive
      const ok = await confirm({
        title: willActivate ? 'Kích hoạt tài khoản' : 'Vô hiệu hoá tài khoản',
        message: `${user.email} sẽ ${willActivate ? 'được kích hoạt lại' : 'không thể đăng nhập'}.`,
        confirmText: willActivate ? 'Kích hoạt' : 'Vô hiệu hoá',
        variant: willActivate ? 'default' : 'destructive',
      })
      if (!ok) return
      try {
        if (willActivate) {
          await adminService.activateUser(user.userId)
        } else {
          await adminService.deactivateUser(user.userId)
        }
        toast.success(willActivate ? 'Đã kích hoạt' : 'Đã vô hiệu hoá')
        void fetchUsers()
      } catch (err) {
        toast.error(extractApiError(err, 'Cập nhật trạng thái thất bại'))
      }
    },
    [confirm, fetchUsers, toast],
  )

  const columns = useMemo<ColumnDef<UserInfo>[]>(
    () => [
      {
        accessorKey: 'email',
        header: 'Email',
        cell: ({ row }) => <span className="font-medium">{row.original.email}</span>,
      },
      { accessorKey: 'fullName', header: 'Họ tên' },
      {
        accessorKey: 'roleId',
        header: 'Quyền',
        cell: ({ row }) =>
          row.original.roleId === ROLES.ADMIN ? (
            <Badge>Admin</Badge>
          ) : (
            <Badge variant="secondary">User</Badge>
          ),
      },
      {
        accessorKey: 'isActive',
        header: 'Trạng thái',
        cell: ({ row }) =>
          row.original.isActive ? (
            <Badge variant="outline" className="border-emerald-500 text-emerald-700">
              Hoạt động
            </Badge>
          ) : (
            <Badge variant="outline" className="border-rose-500 text-rose-700">
              Vô hiệu hoá
            </Badge>
          ),
      },
      {
        accessorKey: 'lastLogin',
        header: 'Đăng nhập gần nhất',
        cell: ({ row }) => (
          <span className="text-muted-foreground text-xs tabular-nums">
            {fmtDate(row.original.lastLogin)}
          </span>
        ),
      },
      {
        id: 'actions',
        header: '',
        enableSorting: false,
        cell: ({ row }) => (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8" aria-label="Tác vụ">
                <MoreHorizontal className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem asChild>
                <Link href={`/users/${row.original.userId}`} className="cursor-pointer">
                  <Eye className="mr-2 size-4" />
                  Xem chi tiết
                </Link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => toggleActive(row.original)}>
                {row.original.isActive ? (
                  <>
                    <PowerOff className="mr-2 size-4" />
                    Vô hiệu hoá
                  </>
                ) : (
                  <>
                    <Power className="mr-2 size-4" />
                    Kích hoạt
                  </>
                )}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ),
      },
    ],
    [toggleActive],
  )

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">Người dùng</h1>
          <p className="text-muted-foreground text-sm">
            {totalItems > 0 ? `${totalItems} tài khoản` : 'Quản lý tài khoản người dùng'}
          </p>
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
