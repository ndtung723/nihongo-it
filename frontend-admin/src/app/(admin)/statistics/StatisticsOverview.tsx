'use client'

import '@/lib/charts'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Bar, Doughnut } from 'react-chartjs-2'
import { Activity, BookOpen, Brain, TrendingUp, Users as UsersIcon } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Loader } from '@/components/common/Loader'
import statisticsService from '@/services/statistics.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { AdminStatisticsOverview } from '@/types/statistics.types'

const fmtPercent = (v?: number) => {
  if (typeof v !== 'number' || Number.isNaN(v)) return '—'
  return `${Math.round(v * 100)}%`
}

const sortedEntries = (obj?: Record<string, number>) =>
  obj ? Object.entries(obj).sort(([a], [b]) => a.localeCompare(b)) : []

export function StatisticsOverview() {
  const toast = useAppToast()
  const [stats, setStats] = useState<AdminStatisticsOverview | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    statisticsService
      .getOverview()
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

  if (loading) return <Loader label="Đang tải thống kê..." />

  if (!stats) {
    return (
      <p className="text-muted-foreground py-10 text-center">Chưa có dữ liệu thống kê.</p>
    )
  }

  const summaryCards = [
    { label: 'Tổng người dùng', value: stats.totalUsers, Icon: UsersIcon, tone: 'text-primary' },
    {
      label: 'Đang hoạt động',
      value: stats.activeUsers,
      Icon: Activity,
      tone: 'text-emerald-600',
    },
    {
      label: 'Tổng flashcard',
      value: stats.totalFlashcards,
      Icon: BookOpen,
      tone: 'text-amber-500',
    },
    {
      label: 'TB thẻ / user',
      value: Math.round(stats.averageCardsPerUser ?? 0),
      Icon: TrendingUp,
      tone: 'text-rose-500',
    },
    {
      label: 'TB ghi nhớ',
      value: fmtPercent(stats.averageRetentionRate),
      Icon: Brain,
      tone: 'text-violet-500',
    },
  ]

  const byLevel = sortedEntries(stats.usersByLevel)
  const byGoal = sortedEntries(stats.usersByJlptGoal)

  const levelChart = {
    labels: byLevel.map(([k]) => k),
    datasets: [
      {
        label: 'Trình độ hiện tại',
        data: byLevel.map(([, v]) => v),
        backgroundColor: 'rgba(59, 130, 246, 0.7)',
      },
    ],
  }

  const goalChart = {
    labels: byGoal.map(([k]) => k),
    datasets: [
      {
        data: byGoal.map(([, v]) => v),
        backgroundColor: [
          'rgba(239, 68, 68, 0.7)',
          'rgba(245, 158, 11, 0.7)',
          'rgba(16, 185, 129, 0.7)',
          'rgba(59, 130, 246, 0.7)',
          'rgba(139, 92, 246, 0.7)',
        ],
      },
    ],
  }

  const chartOpts = { maintainAspectRatio: false, responsive: true }

  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-bold">Thống kê</h1>
          <p className="text-muted-foreground text-sm">Tổng quan hệ thống</p>
        </div>
        <Button asChild variant="outline">
          <Link href="/statistics/users">Theo người dùng →</Link>
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        {summaryCards.map(({ label, value, Icon, tone }) => (
          <Card key={label}>
            <CardContent className="flex items-center justify-between p-4">
              <div>
                <p className="text-muted-foreground text-xs uppercase">{label}</p>
                <p className="mt-1 text-2xl font-semibold">{value}</p>
              </div>
              <Icon className={`size-7 ${tone}`} />
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">User theo trình độ hiện tại</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {byLevel.length > 0 ? (
                <Bar data={levelChart} options={chartOpts} />
              ) : (
                <p className="text-muted-foreground py-10 text-center text-sm">
                  Chưa có dữ liệu
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">User theo mục tiêu JLPT</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mx-auto h-64 max-w-sm">
              {byGoal.length > 0 ? (
                <Doughnut data={goalChart} options={chartOpts} />
              ) : (
                <p className="text-muted-foreground py-10 text-center text-sm">
                  Chưa có dữ liệu
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {stats.topPerformingUsers && stats.topPerformingUsers.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Top user ghi nhớ tốt nhất</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {stats.topPerformingUsers.slice(0, 5).map((u) => (
                <Link
                  key={u.userId}
                  href={`/statistics/users/${u.userId}`}
                  className="hover:bg-muted/50 flex items-center justify-between rounded-md border p-3 transition-colors"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{u.userName}</p>
                    <p className="text-muted-foreground truncate text-xs">{u.email}</p>
                  </div>
                  <span className="text-emerald-600 font-semibold tabular-nums">
                    {fmtPercent(u.summary?.overallRetentionRate ?? u.retentionRate)}
                  </span>
                </Link>
              ))}
            </CardContent>
          </Card>
        )}

        {stats.mostActiveUsers && stats.mostActiveUsers.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Top user hoạt động nhiều nhất</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {stats.mostActiveUsers.slice(0, 5).map((u) => (
                <Link
                  key={u.userId}
                  href={`/statistics/users/${u.userId}`}
                  className="hover:bg-muted/50 flex items-center justify-between rounded-md border p-3 transition-colors"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{u.userName}</p>
                    <p className="text-muted-foreground truncate text-xs">{u.email}</p>
                  </div>
                  <span className="text-primary font-semibold tabular-nums">
                    {u.summary?.reviewsLast30Days ?? 0}
                  </span>
                </Link>
              ))}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
