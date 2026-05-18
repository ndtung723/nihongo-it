'use client'

import { Mic, Square } from 'lucide-react'
import { useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { useAudioRecorder, type RecorderState } from '@/hooks/useAudioRecorder'

interface AudioRecorderButtonProps {
  /** Fires once when the user stops recording — Blob is ready. */
  onRecorded: (blob: Blob) => void
  disabled?: boolean
}

function fmtElapsed(ms: number): string {
  const sec = Math.floor(ms / 1000)
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

const STATE_LABEL: Record<RecorderState, string> = {
  idle: 'Bấm để ghi âm',
  recording: 'Đang ghi...',
  stopped: 'Đã ghi xong',
  error: 'Lỗi',
}

export function AudioRecorderButton({ onRecorded, disabled }: AudioRecorderButtonProps) {
  const { state, blob, error, elapsedMs, start, stop, reset } = useAudioRecorder()

  // Emit blob when recording finishes (state becomes 'stopped' with non-null blob)
  useEffect(() => {
    if (state === 'stopped' && blob) {
      onRecorded(blob)
      // Reset after handing off, so the next start is clean
      reset()
    }
  }, [state, blob, onRecorded, reset])

  const isRecording = state === 'recording'

  return (
    <div className="flex flex-col items-center gap-2">
      <Button
        type="button"
        size="lg"
        variant={isRecording ? 'destructive' : 'default'}
        onClick={isRecording ? stop : start}
        disabled={disabled}
        className="h-14 w-14 rounded-full p-0"
        aria-label={isRecording ? 'Dừng ghi' : 'Bắt đầu ghi'}
      >
        {isRecording ? <Square className="size-5" /> : <Mic className="size-5" />}
      </Button>
      <p className="text-muted-foreground text-xs">
        {isRecording ? `${STATE_LABEL[state]} ${fmtElapsed(elapsedMs)}` : STATE_LABEL[state]}
      </p>
      {error && <p className="text-destructive text-xs">{error}</p>}
    </div>
  )
}
