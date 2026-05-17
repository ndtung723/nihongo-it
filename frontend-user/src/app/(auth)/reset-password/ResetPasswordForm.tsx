'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import api from '@/lib/api'
import { extractApiError } from '@/types/common.types'
import { resetPasswordSchema, type ResetPasswordInput } from '@/schemas/auth.schema'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export function ResetPasswordForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const tokenFromUrl = searchParams.get('token') ?? ''
  const toast = useAppToast()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordInput>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { token: tokenFromUrl, newPassword: '', confirmPassword: '' },
  })

  async function onSubmit(values: ResetPasswordInput) {
    try {
      await api.post('/api/v1/user/auth/set-new-password', values)
      toast.success('Đặt lại mật khẩu thành công. Vui lòng đăng nhập.')
      router.push('/login')
    } catch (err) {
      toast.error(extractApiError(err, 'Đặt lại mật khẩu thất bại'))
    }
  }

  if (!tokenFromUrl) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-xl">Token không hợp lệ</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-muted-foreground text-sm">
            Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.
          </p>
          <Link href="/forgot-password" className="text-primary text-sm hover:underline">
            ← Yêu cầu liên kết mới
          </Link>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">Đặt lại mật khẩu</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <input type="hidden" {...register('token')} />

          <div className="space-y-1.5">
            <Label htmlFor="newPassword">Mật khẩu mới</Label>
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.newPassword}
              {...register('newPassword')}
            />
            {errors.newPassword && (
              <p className="text-destructive text-sm">{errors.newPassword.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
            {errors.confirmPassword && (
              <p className="text-destructive text-sm">{errors.confirmPassword.message}</p>
            )}
          </div>

          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Đang xử lý...' : 'Đặt lại mật khẩu'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
