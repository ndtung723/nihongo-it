'use client'

import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { signupSchema, type SignupInput } from '@/schemas/auth.schema'
import { useAuthStore } from '@/stores/auth.store'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const JLPT_OPTIONS = ['N1', 'N2', 'N3', 'N4', 'N5'] as const

export default function RegisterPage() {
  const router = useRouter()
  const toast = useAppToast()
  const register = useAuthStore((s) => s.register)
  const loading = useAuthStore((s) => s.loading)

  const {
    register: registerField,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<SignupInput>({
    resolver: zodResolver(signupSchema),
    defaultValues: { email: '', password: '', fullName: '' },
  })

  async function onSubmit(values: SignupInput) {
    try {
      const message = await register(values)
      toast.success(message ?? 'Đăng ký thành công. Vui lòng kiểm tra email.')
      router.push('/login')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Đăng ký thất bại')
    }
  }

  const busy = loading || isSubmitting

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-xl">Tạo tài khoản</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              aria-invalid={!!errors.email}
              {...registerField('email')}
            />
            {errors.email && (
              <p className="text-destructive text-sm">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="fullName">Họ tên</Label>
            <Input
              id="fullName"
              autoComplete="name"
              aria-invalid={!!errors.fullName}
              {...registerField('fullName')}
            />
            {errors.fullName && (
              <p className="text-destructive text-sm">{errors.fullName.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="password">Mật khẩu</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.password}
              {...registerField('password')}
            />
            {errors.password && (
              <p className="text-destructive text-sm">{errors.password.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="currentLevel">Trình độ hiện tại</Label>
              <Controller
                control={control}
                name="currentLevel"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="currentLevel">
                      <SelectValue placeholder="Chọn" />
                    </SelectTrigger>
                    <SelectContent>
                      {JLPT_OPTIONS.map((lvl) => (
                        <SelectItem key={lvl} value={lvl}>
                          {lvl}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="jlptGoal">Mục tiêu JLPT</Label>
              <Controller
                control={control}
                name="jlptGoal"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="jlptGoal">
                      <SelectValue placeholder="Chọn" />
                    </SelectTrigger>
                    <SelectContent>
                      {JLPT_OPTIONS.map((lvl) => (
                        <SelectItem key={lvl} value={lvl}>
                          {lvl}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>

          <Button type="submit" className="w-full" disabled={busy}>
            {busy ? 'Đang đăng ký...' : 'Đăng ký'}
          </Button>

          <p className="text-center text-sm">
            Đã có tài khoản?{' '}
            <Link href="/login" className="text-primary hover:underline">
              Đăng nhập
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  )
}
