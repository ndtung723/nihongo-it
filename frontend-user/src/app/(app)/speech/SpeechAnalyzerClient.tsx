'use client'

import { useCallback, useState } from 'react'
import { Mic2, RotateCw } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Loader } from '@/components/common/Loader'
import { AudioRecorderButton } from '@/components/conversation/AudioRecorderButton'
import { AudioButton } from '@/components/vocabulary/AudioButton'
import { SpeechFeedback } from '@/components/conversation/SpeechFeedback'
import aiService from '@/services/ai.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'
import type { SpeechAnalysisResult } from '@/types/ai.types'

const EXAMPLE_TEXT = '日本語の発音を練習しています。'

export function SpeechAnalyzerClient() {
  const toast = useAppToast()
  const [text, setText] = useState(EXAMPLE_TEXT)
  const [result, setResult] = useState<SpeechAnalysisResult | null>(null)
  const [analyzing, setAnalyzing] = useState(false)

  const handleRecorded = useCallback(
    async (blob: Blob) => {
      const trimmed = text.trim()
      if (!trimmed) {
        toast.warning('Vui lòng nhập văn bản tham chiếu trước')
        return
      }
      setAnalyzing(true)
      try {
        const res = await aiService.analyzeSpeech(blob, trimmed, 'free-speech')
        setResult(res)
      } catch (err) {
        toast.error(extractApiError(err, 'Phân tích phát âm thất bại'))
      } finally {
        setAnalyzing(false)
      }
    },
    [text, toast],
  )

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div>
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <Mic2 className="text-primary size-6" />
          Phân tích phát âm
        </h1>
        <p className="text-muted-foreground text-sm">
          Nhập một đoạn tiếng Nhật, ghi âm bạn đọc, AI sẽ chấm điểm phát âm theo từng từ.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Văn bản tham chiếu</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <Label htmlFor="ref-text" className="sr-only">
            Văn bản
          </Label>
          <Textarea
            id="ref-text"
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={3}
            placeholder={EXAMPLE_TEXT}
          />
          <div className="flex items-center justify-between">
            <p className="text-muted-foreground text-xs">{text.length} ký tự</p>
            {text.trim() && (
              <div className="flex items-center gap-2">
                <span className="text-muted-foreground text-xs">Nghe mẫu:</span>
                <AudioButton text={text.trim()} contentType="conversation" size="sm" />
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-col items-center gap-3 py-2">
        {result ? (
          <Button variant="outline" size="sm" onClick={() => setResult(null)}>
            <RotateCw className="mr-1 size-4" />
            Thử lại
          </Button>
        ) : (
          <AudioRecorderButton onRecorded={handleRecorded} disabled={analyzing} />
        )}
        {analyzing && <Loader label="Đang phân tích..." />}
      </div>

      {result && <SpeechFeedback result={result} />}
    </div>
  )
}
