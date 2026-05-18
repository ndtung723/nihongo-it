'use client'

import { useEffect, useState } from 'react'
import {
  Activity,
  BookOpen,
  FolderTree,
  Search,
  Tag,
  TrendingUp,
  UserPlus,
  Users as UsersIcon,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Loader } from '@/components/common/Loader'
import adminService from '@/services/admin.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { DashboardStats } from '@/types/user.types'

const SUMMARY_CARDS = [
  { key: 'userCount', label: 'Tổng người dùng', Icon: UsersIcon, tone: 'text-primary' },
  { key: 'vocabularyCount', label: 'Từ vựng', Icon: BookOpen, tone: 'text-emerald-600' },
  { key: 'categoryCount', label: 'Danh mục', Icon: FolderTree, tone: 'text-amber-500' },
  { key: 'topicCount', label: 'Chủ đề', Icon: Tag, tone: 'text-rose-500' },
] as const

const TODAY_CARDS = [
  { key: 'newUsers', label: 'Tài khoản mới', Icon: UserPlus, tone: 'text-primary' },
  { key: 'activeUsers', label: 'Đang hoạt động', Icon: Activity, tone: 'text-emerald-600' },
  { key: 'searchesToday', label: 'Lượt tra cứu', Icon: Search, tone: 'text-amber-500' },
  {
    key: 'flashcardsStudiedToday',
    label: 'Lượt học flashcard',
    Icon: TrendingUp,
    tone: 'text-rose-500',
  },
] as const

export function DashboardClient() {
  const toast = useAppToast()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    adminService
      .getDashboardStats()
      .then((data) => {
        if (!cancelled) setStats(data)
      })
      .catch((err) => {
        if (!cancelled) toast.error(extractApiError(err, 'Không tải được thống kê'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (loading) return <Loader label="Đang tải dashboard..." />

  if (!stats) {
    return (
      <p className="text-muted-foreground py-10 text-center">Chưa có dữ liệu thống kê.</p>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground text-sm">Tổng quan hệ thống Nihongo IT</p>
      </div>

      <section>
        <h2 className="text-muted-foreground mb-2 text-xs uppercase tracking-wide">
          Tổng quan
        </h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {SUMMARY_CARDS.map(({ key, label, Icon, tone }) => (
            <Card key={key}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-muted-foreground text-xs uppercase">{label}</p>
                  <p className="mt-1 text-2xl font-semibold">{stats[key] ?? 0}</p>
                </div>
                <Icon className={`size-7 ${tone}`} />
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <section>
        <h2 className="text-muted-foreground mb-2 text-xs uppercase tracking-wide">Hôm nay</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {TODAY_CARDS.map(({ key, label, Icon, tone }) => (
            <Card key={key}>
              <CardContent className="flex items-center justify-between p-4">
                <div>
                  <p className="text-muted-foreground text-xs uppercase">{label}</p>
                  <p className="mt-1 text-2xl font-semibold">{stats[key] ?? 0}</p>
                </div>
                <Icon className={`size-7 ${tone}`} />
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      {stats.recentActivities && stats.recentActivities.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Hoạt động gần đây</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {stats.recentActivities.slice(0, 10).map((a, i) => (
              <div
                key={i}
                className="flex items-center justify-between border-b py-2 text-sm last:border-0"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">{a.user}</p>
                  <p className="text-muted-foreground truncate text-xs">{a.action}</p>
                </div>
                <span className="text-muted-foreground ml-2 shrink-0 text-xs tabular-nums">
                  {new Date(a.timestamp).toLocaleString('vi-VN')}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
