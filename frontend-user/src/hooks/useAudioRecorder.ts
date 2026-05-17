'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

export type RecorderState = 'idle' | 'recording' | 'stopped' | 'error'

/**
 * Browser MediaRecorder wrapper. Uses native APIs only — no extra deps.
 *
 * The backend's speech analyzer accepts webm/opus (and we never claim a
 * specific mime in the upload). If the backend requires mp3 specifically,
 * convert with ffmpeg.wasm on the client or have the gateway transcode.
 *
 * Returns blob (final WAV/webm) + lastError on stop. Caller decides what to do.
 */
export function useAudioRecorder() {
  const [state, setState] = useState<RecorderState>('idle')
  const [blob, setBlob] = useState<Blob | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [elapsedMs, setElapsedMs] = useState(0)

  const recorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<Blob[]>([])
  const streamRef = useRef<MediaStream | null>(null)
  const startedAtRef = useRef<number>(0)
  const tickRef = useRef<number | null>(null)

  const stopStream = useCallback(() => {
    streamRef.current?.getTracks().forEach((t) => t.stop())
    streamRef.current = null
  }, [])

  const stopTimer = useCallback(() => {
    if (tickRef.current) {
      window.clearInterval(tickRef.current)
      tickRef.current = null
    }
  }, [])

  const reset = useCallback(() => {
    setState('idle')
    setBlob(null)
    setError(null)
    setElapsedMs(0)
    chunksRef.current = []
    recorderRef.current = null
    stopStream()
    stopTimer()
  }, [stopStream, stopTimer])

  const start = useCallback(async () => {
    if (state === 'recording') return
    setError(null)
    setBlob(null)
    chunksRef.current = []

    if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      setError('Trình duyệt không hỗ trợ ghi âm')
      setState('error')
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : MediaRecorder.isTypeSupported('audio/webm')
          ? 'audio/webm'
          : ''
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType } : undefined)
      recorderRef.current = recorder

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }
      recorder.onstop = () => {
        const out = new Blob(chunksRef.current, {
          type: recorder.mimeType || 'audio/webm',
        })
        setBlob(out)
        setState('stopped')
        stopStream()
        stopTimer()
      }
      recorder.onerror = (e) => {
        setError((e as ErrorEvent).message || 'Lỗi ghi âm')
        setState('error')
        stopStream()
        stopTimer()
      }

      recorder.start(250) // chunk every 250ms so we always have data
      startedAtRef.current = Date.now()
      setElapsedMs(0)
      tickRef.current = window.setInterval(() => {
        setElapsedMs(Date.now() - startedAtRef.current)
      }, 100)
      setState('recording')
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Không thể truy cập micro'
      setError(msg)
      setState('error')
      stopStream()
    }
  }, [state, stopStream, stopTimer])

  const stop = useCallback(() => {
    const rec = recorderRef.current
    if (rec && rec.state !== 'inactive') {
      rec.stop()
    }
  }, [])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      const rec = recorderRef.current
      if (rec && rec.state !== 'inactive') {
        try {
          rec.stop()
        } catch {
          // ignore
        }
      }
      stopStream()
      stopTimer()
    }
  }, [stopStream, stopTimer])

  return { state, blob, error, elapsedMs, start, stop, reset }
}
