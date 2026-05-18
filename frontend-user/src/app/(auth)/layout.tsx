import Link from 'next/link'
import type { ReactNode } from 'react'

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="bg-muted flex min-h-screen flex-col items-center justify-center p-4">
      <Link href="/" className="text-primary mb-6 text-2xl font-semibold">
        Nihongo IT
      </Link>
      <div className="w-full max-w-md">{children}</div>
    </div>
  )
}
