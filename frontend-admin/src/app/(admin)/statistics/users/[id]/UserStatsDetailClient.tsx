'use client'

import '@/lib/charts'
import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Bar, Doughnut, Line } from 'react-chartjs-2'
import { ArrowLeft } from 'lucide-react'
import statisticsService from '@/services/statistics.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { UserStatisticsDetail } from '@/types/statistics.types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Loader } from '@/components/common/Loader'

interface Props {
  userId: string
}

const fmtPercent = (v?: number) => {
  if (typeof v !== 'number' || Number.isNaN(v)) return '—'
  return `${Math.round(v * 100)}%`
}

const fmtDate = (s?: string) => (s ? new Date(s).toLocaleString('vi-VN') : '—')

const sortedEntries = (obj?: Record<string, number>) =>
  obj ? Object.entries(obj).sort(([a], [b]) => a.localeCompare(b)) : []

const RATING_LABEL: Record<number, { text: string; tone: string }> = {
  1: { text: 'Quên', tone: 'bg-rose-100 text-rose-700' },
  2: { text: 'Khó', tone: 'bg-amber-100 text-amber-700' },
  3: { text: 'Tốt', tone: 'bg-emerald-100 text-emerald-700' },
  4: { text: 'Dễ', tone: 'bg-primary/10 text-primary' },
}

function StatRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b py-2 text-sm last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value ?? '—'}</span>
    </div>
  )
}

export function UserStatsDetailClient({ userId }: Props) {
  const toast = useAppToast()
  const [stats, setStats] = useState<UserStatisticsDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    statisticsService
      .getUserDetail(userId)
      .then((data) => {
        if (!cancelled) setStats(data)
      })
      .catch((err) => {
        if (!cancelled) toast.error(extractApiError(err, 'Không tải được dữ liệu'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId])

  if (loading) return <Loader label="Đang tải..." />

  if (!stats) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy dữ liệu thống kê.</p>
        <Button asChild variant="outline">
          <Link href="/statistics/users">← Quay lại</Link>
        </Button>
      </div>
    )
  }

  const dailyReviews = sortedEntries(stats.dailyReviews)
  const retentionDaily = sortedEntries(stats.retentionRateByDay)
  const cardsDue = sortedEntries(stats.cardsDueByDay)
  const cardsByJlpt = sortedEntries(stats.cardsByJlptLevel)

  const dailyChart = {
    labels: dailyReviews.map(([d]) => d),
    datasets: [
      {
        label: 'Lượt ôn / ngày',
        data: dailyReviews.map(([, v]) => v),
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        fill: true,
        tension: 0.3,
      },
    ],
  }

  const retentionChart = {
    labels: retentionDaily.map(([d]) => d),
    datasets: [
      {
        label: 'Tỷ lệ ghi nhớ',
        data: retentionDaily.map(([, v]) => Math.round(v * 100)),
        borderColor: 'rgb(16, 185, 129)',
        backgroundColor: 'rgba(16, 185, 129, 0.2)',
        fill: true,
        tension: 0.3,
      },
    ],
  }

  const dueChart = {
    labels: cardsDue.map(([d]) => d),
    datasets: [
      {
        label: 'Thẻ đến hạn',
        data: cardsDue.map(([, v]) => v),
        backgroundColor: 'rgba(245, 158, 11, 0.7)',
      },
    ],
  }

  const jlptChart = {
    labels: cardsByJlpt.map(([k]) => k),
    datasets: [
      {
        data: cardsByJlpt.map(([, v]) => v),
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
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/statistics/users">
          <ArrowLeft className="mr-1 size-4" />
          Danh sách
        </Link>
      </Button>

      <div>
        <h1 className="text-2xl font-bold">{stats.userName}</h1>
        <p className="text-muted-foreground text-sm">{stats.email}</p>
        <div className="mt-2 flex flex-wrap gap-2">
          {stats.profileInfo?.currentLevel && (
            <Badge variant="secondary">Hiện tại: {stats.profileInfo.currentLevel}</Badge>
          )}
          {stats.profileInfo?.jlptGoal && (
            <Badge variant="outline">Mục tiêu: {stats.profileInfo.jlptGoal}</Badge>
          )}
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tóm tắt</CardTitle>
          </CardHeader>
          <CardContent>
            <StatRow label="Tổng thẻ" value={stats.summary?.totalCards ?? 0} />
            <StatRow label="Đến hạn" value={stats.summary?.dueCardsNow ?? 0} />
            <StatRow
              label="Chuỗi ngày"
              value={stats.summary?.currentStreak ?? stats.currentStreak ?? 0}
            />
            <StatRow label="Lượt ôn 30 ngày" value={stats.summary?.reviewsLast30Days ?? 0} />
            <StatRow
              label="Tỷ lệ ghi nhớ"
              value={fmtPercent(stats.summary?.overallRetentionRate ?? stats.retentionRate)}
            />
            <StatRow label="Hoạt động gần nhất" value={fmtDate(stats.lastActive)} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tài khoản</CardTitle>
          </CardHeader>
          <CardContent>
            <StatRow label="Ngày tạo" value={fmtDate(stats.profileInfo?.createdAt)} />
            <StatRow label="Đăng nhập gần nhất" value={fmtDate(stats.profileInfo?.lastLogin)} />
            <StatRow
              label="Độ ghi nhớ yếu"
              value={stats.memoryStrengthDistribution?.weak ?? 0}
            />
            <StatRow
              label="Độ ghi nhớ trung bình"
              value={stats.memoryStrengthDistribution?.medium ?? 0}
            />
            <StatRow
              label="Độ ghi nhớ mạnh"
              value={stats.memoryStrengthDistribution?.strong ?? 0}
            />
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Lượt ôn theo ngày</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {dailyReviews.length > 0 ? (
                <Line data={dailyChart} options={chartOpts} />
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
            <CardTitle className="text-base">Tỷ lệ ghi nhớ theo ngày</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {retentionDaily.length > 0 ? (
                <Line data={retentionChart} options={chartOpts} />
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
            <CardTitle className="text-base">Dự báo thẻ đến hạn</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64">
              {cardsDue.length > 0 ? (
                <Bar data={dueChart} options={chartOpts} />
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
            <CardTitle className="text-base">Phân bố theo JLPT</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mx-auto h-64 max-w-sm">
              {cardsByJlpt.length > 0 ? (
                <Doughnut data={jlptChart} options={chartOpts} />
              ) : (
                <p className="text-muted-foreground py-10 text-center text-sm">
                  Chưa có dữ liệu
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {stats.reviewHistory && stats.reviewHistory.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              Lịch sử ôn tập gần đây ({stats.reviewHistory.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {stats.reviewHistory.slice(0, 20).map((r) => {
              const rating = RATING_LABEL[r.rating] ?? {
                text: `${r.rating}`,
                tone: 'bg-muted text-foreground',
              }
              return (
                <div
                  key={r.reviewId}
                  className="flex items-center justify-between gap-3 border-b py-2 text-sm last:border-0"
                >
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">
                      {r.vocabulary?.term ?? r.vocabulary?.japanese ?? '—'}
                    </p>
                    <p className="text-muted-foreground truncate text-xs">
                      {r.vocabulary?.meaning ?? r.vocabulary?.english ?? ''}
                    </p>
                  </div>
                  <span className={`rounded px-2 py-0.5 text-xs font-medium ${rating.tone}`}>
                    {rating.text}
                  </span>
                  <span className="text-muted-foreground text-xs tabular-nums">
                    {fmtDate(r.timestamp)}
                  </span>
                </div>
              )
            })}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
