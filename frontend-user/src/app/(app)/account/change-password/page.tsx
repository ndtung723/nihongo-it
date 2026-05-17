'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { changePasswordSchema, type ChangePasswordInput } from '@/schemas/auth.schema'
import { useAuthStore } from '@/stores/auth.store'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function ChangePasswordPage() {
  const toast = useAppToast()
  const changePassword = useAuthStore((s) => s.changePassword)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordInput>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  async function onSubmit(values: ChangePasswordInput) {
    try {
      await changePassword(values)
      toast.success('Đổi mật khẩu thành công')
      reset()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Đổi mật khẩu thất bại')
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <Card>
        <CardHeader>
          <CardTitle>Đổi mật khẩu</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-1.5">
              <Label htmlFor="currentPassword">Mật khẩu hiện tại</Label>
              <Input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                aria-invalid={!!errors.currentPassword}
                {...register('currentPassword')}
              />
              {errors.currentPassword && (
                <p className="text-destructive text-sm">{errors.currentPassword.message}</p>
              )}
            </div>

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
              <Label htmlFor="confirmPassword">Xác nhận mật khẩu mới</Label>
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

            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : 'Đổi mật khẩu'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
