'use client'

import { ThemeProvider } from 'next-themes'
import type { ReactNode } from 'react'
import { Toaster } from '@/components/ui/sonner'
import { ConfirmProvider } from '@/hooks/useConfirm'

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false}>
      <ConfirmProvider>
        {children}
        <Toaster position="top-right" richColors closeButton />
      </ConfirmProvider>
    </ThemeProvider>
  )
}
