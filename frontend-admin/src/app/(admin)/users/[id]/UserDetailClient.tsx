'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Power, PowerOff, ShieldCheck, UserCog } from 'lucide-react'
import adminService from '@/services/admin.service'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { extractApiError } from '@/types/common.types'
import { ROLES } from '@/types/roles'
import type { UserDetailInfo } from '@/types/user.types'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Loader } from '@/components/common/Loader'

interface Props {
  userId: string
}

const fmtDate = (s?: string) => (s ? new Date(s).toLocaleString('vi-VN') : '—')

function StatRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b py-2 text-sm last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value ?? '—'}</span>
    </div>
  )
}

export function UserDetailClient({ userId }: Props) {
  const router = useRouter()
  const toast = useAppToast()
  const confirm = useConfirm()

  const [user, setUser] = useState<UserDetailInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionBusy, setActionBusy] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await adminService.getUserById(userId)
      setUser(data)
    } catch (err) {
      toast.error(extractApiError(err, 'Không tải được thông tin người dùng'))
    } finally {
      setLoading(false)
    }
  }, [userId, toast])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()
  }, [load])

  const toggleActive = async () => {
    if (!user) return
    const willActivate = !user.isActive
    const ok = await confirm({
      title: willActivate ? 'Kích hoạt tài khoản' : 'Vô hiệu hoá tài khoản',
      message: `${user.email} sẽ ${willActivate ? 'được kích hoạt lại' : 'không thể đăng nhập'}.`,
      confirmText: willActivate ? 'Kích hoạt' : 'Vô hiệu hoá',
      variant: willActivate ? 'default' : 'destructive',
    })
    if (!ok) return
    setActionBusy(true)
    try {
      if (willActivate) await adminService.activateUser(user.userId)
      else await adminService.deactivateUser(user.userId)
      toast.success(willActivate ? 'Đã kích hoạt' : 'Đã vô hiệu hoá')
      void load()
    } catch (err) {
      toast.error(extractApiError(err, 'Cập nhật thất bại'))
    } finally {
      setActionBusy(false)
    }
  }

  const toggleRole = async () => {
    if (!user) return
    const willPromote = user.roleId !== ROLES.ADMIN
    const ok = await confirm({
      title: willPromote ? 'Cấp quyền Admin' : 'Thu hồi quyền Admin',
      message: willPromote
        ? `${user.email} sẽ có toàn quyền quản trị hệ thống.`
        : `${user.email} sẽ trở thành người dùng thường.`,
      confirmText: willPromote ? 'Cấp quyền' : 'Thu hồi',
      variant: willPromote ? 'default' : 'destructive',
    })
    if (!ok) return
    setActionBusy(true)
    try {
      await adminService.changeUserRole(user.userId, willPromote ? ROLES.ADMIN : ROLES.USER)
      toast.success(willPromote ? 'Đã cấp quyền Admin' : 'Đã thu hồi quyền Admin')
      void load()
    } catch (err) {
      toast.error(extractApiError(err, 'Đổi quyền thất bại'))
    } finally {
      setActionBusy(false)
    }
  }

  if (loading && !user) return <Loader label="Đang tải..." />
  if (!user) {
    return (
      <div className="space-y-4 py-10 text-center">
        <p className="text-muted-foreground">Không tìm thấy người dùng.</p>
        <Button onClick={() => router.push('/users')} variant="outline">
          ← Danh sách người dùng
        </Button>
      </div>
    )
  }

  const initials =
    user.fullName
      ?.split(' ')
      .map((w) => w[0])
      .join('')
      .slice(0, 2)
      .toUpperCase() ?? '?'

  const isAdmin = user.roleId === ROLES.ADMIN

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm" className="-ml-2">
        <Link href="/users">
          <ArrowLeft className="mr-1 size-4" />
          Danh sách
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <Avatar className="size-14">
                {user.profilePicture && (
                  <AvatarImage src={user.profilePicture} alt={user.fullName} />
                )}
                <AvatarFallback className="text-lg">{initials}</AvatarFallback>
              </Avatar>
              <div>
                <CardTitle>{user.fullName}</CardTitle>
                <p className="text-muted-foreground text-sm">{user.email}</p>
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  {isAdmin ? <Badge>Admin</Badge> : <Badge variant="secondary">User</Badge>}
                  {user.isActive ? (
                    <Badge variant="outline" className="border-emerald-500 text-emerald-700">
                      Hoạt động
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="border-rose-500 text-rose-700">
                      Vô hiệu hoá
                    </Badge>
                  )}
                  {user.isEmailVerified && (
                    <Badge variant="outline" className="border-blue-500 text-blue-700">
                      Email đã xác thực
                    </Badge>
                  )}
                </div>
              </div>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                variant={user.isActive ? 'outline' : 'default'}
                size="sm"
                onClick={toggleActive}
                disabled={actionBusy}
              >
                {user.isActive ? (
                  <>
                    <PowerOff className="mr-1 size-4" />
                    Vô hiệu hoá
                  </>
                ) : (
                  <>
                    <Power className="mr-1 size-4" />
                    Kích hoạt
                  </>
                )}
              </Button>
              <Button
                variant={isAdmin ? 'outline' : 'default'}
                size="sm"
                onClick={toggleRole}
                disabled={actionBusy}
              >
                {isAdmin ? (
                  <>
                    <UserCog className="mr-1 size-4" />
                    Thu hồi Admin
                  </>
                ) : (
                  <>
                    <ShieldCheck className="mr-1 size-4" />
                    Cấp Admin
                  </>
                )}
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Thông tin tài khoản</CardTitle>
          </CardHeader>
          <CardContent>
            <StatRow label="ID" value={<span className="font-mono text-xs">{user.userId}</span>} />
            <StatRow label="Trình độ hiện tại" value={user.currentLevel} />
            <StatRow label="Mục tiêu JLPT" value={user.jlptGoal} />
            <StatRow label="Đăng nhập gần nhất" value={fmtDate(user.lastLogin)} />
            <StatRow label="Ngày tạo" value={fmtDate(user.createdAt)} />
            <StatRow label="Cập nhật gần nhất" value={fmtDate(user.updatedAt)} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tiến độ học tập</CardTitle>
          </CardHeader>
          <CardContent>
            <StatRow label="Chuỗi ngày học" value={user.streakCount ?? 0} />
            <StatRow label="Điểm" value={user.points ?? 0} />
            <StatRow label="Tổng flashcard" value={user.flashcardCount ?? 0} />
            <StatRow label="Thẻ mới" value={user.newCards ?? 0} />
            <StatRow label="Đang học" value={user.learningCards ?? 0} />
            <StatRow label="Đã thuộc" value={user.masteredCards ?? 0} />
          </CardContent>
        </Card>
      </div>

      {(user.reminderEnabled || user.notificationPreferences) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tuỳ chọn thông báo</CardTitle>
          </CardHeader>
          <CardContent>
            <StatRow
              label="Nhắc nhở"
              value={user.reminderEnabled ? 'Bật' : 'Tắt'}
            />
            {user.reminderTime && <StatRow label="Giờ nhắc" value={user.reminderTime} />}
            {user.minCardThreshold && (
              <StatRow label="Ngưỡng tối thiểu" value={`${user.minCardThreshold} thẻ`} />
            )}
            {user.notificationPreferences && (
              <StatRow
                label="Kênh"
                value={<span className="text-xs">{user.notificationPreferences}</span>}
              />
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
