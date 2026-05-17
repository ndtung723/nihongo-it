'use client'

import { Check, X } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import type { SpeechAnalysisResult } from '@/types/ai.types'

// Reusable little progress bar (shadcn's Progress is not installed; bare div).
function ScoreBar({ value, label }: { value?: number; label: string }) {
  const v = Math.max(0, Math.min(100, value ?? 0))
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-medium tabular-nums">{Math.round(v)}</span>
      </div>
      <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
        <div
          className={
            v >= 80
              ? 'h-full bg-emerald-500'
              : v >= 60
                ? 'h-full bg-amber-500'
                : 'h-full bg-rose-500'
          }
          style={{ width: `${v}%` }}
        />
      </div>
    </div>
  )
}

interface SpeechFeedbackProps {
  result: SpeechAnalysisResult
}

export function SpeechFeedback({ result }: SpeechFeedbackProps) {
  const overall = Math.round(result.score ?? 0)
  return (
    <Card>
      <CardContent className="space-y-4 p-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold uppercase tracking-wide">Kết quả phát âm</h3>
          <div
            className={`rounded-full px-3 py-1 text-sm font-bold ${
              overall >= 80
                ? 'bg-emerald-100 text-emerald-700'
                : overall >= 60
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-rose-100 text-rose-700'
            }`}
          >
            {overall}
          </div>
        </div>

        <div className="grid gap-2 sm:grid-cols-3">
          {typeof result.textScore === 'number' && (
            <ScoreBar value={result.textScore} label="Độ chính xác" />
          )}
          {typeof result.intonationScore === 'number' && (
            <ScoreBar value={result.intonationScore} label="Ngữ điệu" />
          )}
          {typeof result.clarityScore === 'number' && (
            <ScoreBar value={result.clarityScore} label="Rõ ràng" />
          )}
        </div>

        {result.transcription && (
          <div>
            <p className="text-muted-foreground mb-1 text-xs uppercase tracking-wide">
              Bạn đã nói
            </p>
            <p className="text-sm">{result.transcription}</p>
          </div>
        )}

        {result.words && result.words.length > 0 && (
          <div>
            <p className="text-muted-foreground mb-1 text-xs uppercase tracking-wide">
              Theo từ
            </p>
            <div className="flex flex-wrap gap-1.5">
              {result.words.map((w, i) => (
                <span
                  key={i}
                  className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-sm ${
                    w.isCorrect
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-rose-100 text-rose-700'
                  }`}
                  title={w.suggestion}
                >
                  {w.isCorrect ? <Check className="size-3" /> : <X className="size-3" />}
                  {w.text}
                </span>
              ))}
            </div>
          </div>
        )}

        {(result.feedback || result.personalizedFeedback) && (
          <div className="bg-muted/40 rounded-lg p-3 text-sm">
            {result.personalizedFeedback ?? result.feedback}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

