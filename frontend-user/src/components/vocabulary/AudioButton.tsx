'use client'

import { useState } from 'react'
import { Loader2, Volume2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import aiService from '@/services/ai.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { ContentType } from '@/types/ai.types'

interface AudioButtonProps {
  text: string
  contentType?: ContentType
  size?: 'sm' | 'default'
}

/** Plays Japanese TTS audio for the given text, with cache. */
export function AudioButton({ text, contentType = 'vocabulary', size = 'default' }: AudioButtonProps) {
  const [busy, setBusy] = useState(false)
  const toast = useAppToast()

  async function handlePlay() {
    if (busy) return
    setBusy(true)
    try {
      const blob = await aiService.generateTTS(text, contentType)
      await aiService.playAudio(blob)
    } catch (err) {
      toast.error(extractApiError(err, 'Không phát được âm thanh'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      onClick={handlePlay}
      disabled={busy}
      aria-label="Nghe phát âm"
      className={size === 'sm' ? 'h-7 w-7' : 'h-9 w-9'}
    >
      {busy ? <Loader2 className="size-4 animate-spin" /> : <Volume2 className="size-4" />}
    </Button>
  )
}
