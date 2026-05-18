import type { ReactNode } from 'react'

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="bg-muted flex min-h-screen flex-col items-center justify-center p-4">
      <div className="mb-6 text-center">
        <h1 className="text-primary text-2xl font-semibold">Nihongo IT</h1>
        <p className="text-muted-foreground text-xs uppercase tracking-wider">Trang quản trị</p>
      </div>
      <div className="w-full max-w-md">{children}</div>
    </div>
  )
}
