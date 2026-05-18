'use client'

import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { updateProfileSchema, type UpdateProfileInput } from '@/schemas/auth.schema'
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

export default function ProfilePage() {
  const toast = useAppToast()
  const user = useAuthStore((s) => s.user)
  const updateProfile = useAuthStore((s) => s.updateProfile)

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors, isSubmitting },
  } = useForm<UpdateProfileInput>({
    resolver: zodResolver(updateProfileSchema),
    defaultValues: {
      fullName: '',
      currentLevel: 'N5',
      jlptGoal: 'N3',
    },
  })

  // Populate form once user data arrives (async from AuthInitializer)
  useEffect(() => {
    if (user) {
      reset({
        fullName: user.fullName,
        currentLevel: user.currentLevel ?? 'N5',
        jlptGoal: user.jlptGoal ?? 'N3',
      })
    }
  }, [user, reset])

  async function onSubmit(values: UpdateProfileInput) {
    try {
      await updateProfile(values)
      toast.success('Cập nhật hồ sơ thành công')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Cập nhật thất bại')
    }
  }

  if (!user) return null

  return (
    <div className="mx-auto max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>Hồ sơ cá nhân</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" value={user.email} disabled />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="fullName">Họ tên</Label>
              <Input
                id="fullName"
                aria-invalid={!!errors.fullName}
                {...register('fullName')}
              />
              {errors.fullName && (
                <p className="text-destructive text-sm">{errors.fullName.message}</p>
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
                        <SelectValue />
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
                        <SelectValue />
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

            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
