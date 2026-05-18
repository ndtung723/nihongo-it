'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import Link from 'next/link'
import api from '@/lib/api'
import { extractApiError } from '@/types/common.types'
import { forgotPasswordSchema, type ForgotPasswordInput } from '@/schemas/auth.schema'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function ForgotPasswordPage() {
  const toast = useAppToast()
  const [submitted, setSubmitted] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordInput>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  async function onSubmit(values: ForgotPasswordInput) {
    try {
      await api.post('/api/v1/user/auth/forgot-password', values)
      setSubmitted(true)
      toast.success('Đã gửi email hướng dẫn đặt lại mật khẩu')
    } catch (err) {
      toast.error(extractApiError(err, 'Gửi yêu cầu thất bại'))
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">Quên mật khẩu</CardTitle>
      </CardHeader>
      <CardContent>
        {submitted ? (
          <div className="space-y-4">
            <p className="text-muted-foreground text-sm">
              Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu. Vui
              lòng kiểm tra hộp thư của bạn.
            </p>
            <Link href="/login" className="text-primary text-sm hover:underline">
              ← Quay về đăng nhập
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <p className="text-muted-foreground text-sm">
              Nhập email tài khoản để nhận liên kết đặt lại mật khẩu.
            </p>

            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                aria-invalid={!!errors.email}
                {...register('email')}
              />
              {errors.email && (
                <p className="text-destructive text-sm">{errors.email.message}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? 'Đang gửi...' : 'Gửi liên kết'}
            </Button>

            <Link href="/login" className="text-primary block text-sm hover:underline">
              ← Quay về đăng nhập
            </Link>
          </form>
        )}
      </CardContent>
    </Card>
  )
}
