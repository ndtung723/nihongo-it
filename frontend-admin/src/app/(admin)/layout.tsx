'use client'

import { useEffect, type ReactNode } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/stores/auth.store'
import { ROLES } from '@/types/roles'
import { Loader } from '@/components/common/Loader'
import { Sidebar } from '@/components/layout/Sidebar'
import { AdminHeader } from '@/components/layout/AdminHeader'

/**
 * Admin route group layout. Role guard happens here (not in proxy.ts) because
 * proxy can only check cookie presence — actual role check needs the decoded
 * JWT or the /current call, both of which are post-init concerns.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  const router = useRouter()
  const user = useAuthStore((s) => s.user)
  const initialized = useAuthStore((s) => s.initialized)
  const logout = useAuthStore((s) => s.logout)

  useEffect(() => {
    // Wait until initialize() has finished before deciding
    if (!initialized) return
    if (!user) {
      router.replace('/login')
      return
    }
    if (user.roleId !== ROLES.ADMIN) {
      void logout().then(() => router.replace('/login'))
    }
  }, [initialized, user, logout, router])

  // Show loader while session is being restored, or while we're about to redirect
  if (!initialized || !user || user.roleId !== ROLES.ADMIN) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader label="Đang xác thực..." />
      </div>
    )
  }

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <AdminHeader />
        <main className="container mx-auto flex-1 px-4 py-6">{children}</main>
      </div>
    </div>
  )
}
