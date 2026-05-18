'use client'

import { useEffect } from 'react'
import { useAuthStore } from '@/stores/auth.store'

/**
 * Silently restores the session on first app mount by calling auth store's
 * initialize() — which exchanges the httpOnly refresh_token cookie for an
 * access token. Renders nothing.
 */
export function AuthInitializer() {
  const initialize = useAuthStore((s) => s.initialize)
  useEffect(() => {
    void initialize()
  }, [initialize])
  return null
}
