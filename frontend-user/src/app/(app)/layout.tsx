import type { ReactNode } from 'react'
import { Header } from '@/components/layout/Header'

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="container mx-auto flex-1 px-4 py-6">{children}</main>
    </div>
  )
}
