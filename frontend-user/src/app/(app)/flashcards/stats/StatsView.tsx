'use client'

import '@/lib/charts' // side-effect: register chart components
import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Bar, Doughnut, Line } from 'react-chartjs-2'
import { ArrowLeft, Brain, CalendarClock, Flame, Layers } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Loader } from '@/components/common/Loader'
import flashcardService from '@/services/flashcard.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'

// Backend stats shape — narrowed locally because the service returns `unknown`.
interface StatsShape {
  summary?: {
    totalCards?: number
    dueCardsNow?: number
    currentStreak?: number
    overallRetentionRate?: number
  }
  dailyReviews?: Record<string, number>
  retentionRateByDay?: Record<string, number>
  cardsDueByDay?: Record<string, number>
  memoryStrengthDistribution?: {
    weak?: number
    medium?: number
    strong?: number
    new?: number
  }
  cardsByState?: Record<string, number>
}

const fmtPercent = (v?: number) => {
  if (typeof v !== 'number' || Number.isNaN(v)) return '—'
  return `${Math.round(v * 100)}%`
}

const sortedEntries = (obj?: Record<string, number>) =>
  obj ? Object.entries(obj).sort(([a], [b]) => a.localeCompare(b)) : []

export function StatsView() {
  const toast = useAppToast()
  const [stats, setStats] = useState<StatsShape | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    flashcardService
      .getStudyStatistics()
      .then((data) => {
        if (!cancelled) setStats(data as StatsShape)
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
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Chưa có dữ liệu thống kê.</p>
        <Button asChild variant="outline">
          <Link href="/flashcards/study">Bắt đầu học</Link>
        </Button>
      </div>
    )
  }

  const summary = stats.summary ?? {}
  const summaryCards = [
    { label: 'Tổng thẻ', value: summary.totalCards ?? 0, Icon: Layers, tone: 'text-primary' },
    {
      label: 'Đến hạn',
      value: summary.dueCardsNow ?? 0,
      Icon: CalendarClock,
      tone: 'text-amber-500',
    },
    {
      label: 'Chuỗi ngày',
      value: summary.currentStreak ?? 0,
      Icon: Flame,
      tone: 'text-rose-500',
    },
    {
      label: 'Ghi nhớ',
      value: fmtPercent(summary.overallRetentionRate),
      Icon: Brain,
      tone: 'text-emerald-600',
    },
  ]

  // ---- Daily reviews (line chart) ----
  const dailyReviews = sortedEntries(stats.dailyReviews)
  const dailyData = {
    labels: dailyReviews.map(([d]) => d),
    datasets: [
      {
        label: 'Lượt ôn',
        data: dailyReviews.map(([, v]) => v),
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        fill: true,
        tension: 0.3,
      },
    ],
  }

  // ---- Cards due forecast (bar chart) ----
  const cardsDue = sortedEntries(stats.cardsDueByDay)
  const dueData = {
    labels: cardsDue.map(([d]) => d),
    datasets: [
      {
        label: 'Thẻ đến hạn',
        data: cardsDue.map(([, v]) => v),
        backgroundColor: 'rgba(245, 158, 11, 0.7)',
      },
    ],
  }

  // ---- Memory strength (doughnut) ----
  const mem = stats.memoryStrengthDistribution ?? {}
  const memData = {
    labels: ['Yếu', 'Trung bình', 'Mạnh', 'Mới'],
    datasets: [
      {
        data: [mem.weak ?? 0, mem.medium ?? 0, mem.strong ?? 0, mem.new ?? 0],
        backgroundColor: [
          'rgba(239, 68, 68, 0.7)',
          'rgba(245, 158, 11, 0.7)',
          'rgba(16, 185, 129, 0.7)',
          'rgba(99, 102, 241, 0.7)',
        ],
      },
    ],
  }

  const chartOpts = { maintainAspectRatio: false, responsive: true }

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/flashcards/study">
          <ArrowLeft className="mr-1 size-4" />
          Quay lại học
        </Link>
      </Button>

      <h1 className="text-2xl font-bold">Thống kê học tập</h1>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {summaryCards.map(({ label, value, Icon, tone }) => (
          <Card key={label}>
            <CardContent className="flex items-center justify-between p-4">
              <div>
                <p className="text-muted-foreground text-xs uppercase tracking-wide">{label}</p>
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
            <CardTitle className="text-base">Hoạt động ôn tập</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {dailyReviews.length > 0 ? (
                <Line data={dailyData} options={chartOpts} />
              ) : (
                <p className="text-muted-foreground py-10 text-center text-sm">Chưa có dữ liệu</p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Dự báo thẻ đến hạn</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {cardsDue.length > 0 ? (
                <Bar data={dueData} options={chartOpts} />
              ) : (
                <p className="text-muted-foreground py-10 text-center text-sm">Chưa có dữ liệu</p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">Phân bố độ ghi nhớ</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mx-auto h-72 max-w-md">
              <Doughnut data={memData} options={chartOpts} />
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
