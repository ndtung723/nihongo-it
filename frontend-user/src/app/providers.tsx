'use client'

import { ThemeProvider } from 'next-themes'
import { GoogleOAuthProvider } from '@react-oauth/google'
import type { ReactNode } from 'react'
import { Toaster } from '@/components/ui/sonner'
import { ConfirmProvider } from '@/hooks/useConfirm'
import { AuthInitializer } from '@/components/AuthInitializer'

const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? ''

export function Providers({ children }: { children: ReactNode }) {
  const tree = (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false}>
      <ConfirmProvider>
        <AuthInitializer />
        {children}
        <Toaster position="top-right" richColors closeButton />
      </ConfirmProvider>
    </ThemeProvider>
  )

  // Google OAuth provider only wraps when client id is configured — avoids
  // confusing console errors in dev without credentials.
  if (!googleClientId) return tree
  return <GoogleOAuthProvider clientId={googleClientId}>{tree}</GoogleOAuthProvider>
}
