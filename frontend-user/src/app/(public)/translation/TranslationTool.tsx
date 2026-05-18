'use client'

import { useEffect, useState } from 'react'
import { ArrowRightLeft, Languages, Trash2 } from 'lucide-react'
import api from '@/lib/api'
import { extractApiError } from '@/types/common.types'
import { useAppToast } from '@/hooks/useAppToast'
import { useConfirm } from '@/hooks/useConfirm'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'

type Direction = 'vn-to-jp' | 'jp-to-vn'

interface HistoryEntry {
  id: number
  direction: Direction
  source: string
  result: string
  useGpt4: boolean
  at: number
}

const HISTORY_KEY = 'nihongo:translation-history'
const HISTORY_LIMIT = 20

const labelFor = (d: Direction) => (d === 'vn-to-jp' ? 'VN → JP' : 'JP → VN')
const sourceLabel = (d: Direction) => (d === 'vn-to-jp' ? 'Tiếng Việt' : 'Tiếng Nhật')
const targetLabel = (d: Direction) => (d === 'vn-to-jp' ? 'Tiếng Nhật' : 'Tiếng Việt')

export function TranslationTool() {
  const toast = useAppToast()
  const confirm = useConfirm()

  const [direction, setDirection] = useState<Direction>('vn-to-jp')
  const [useGpt4, setUseGpt4] = useState(false)
  const [source, setSource] = useState('')
  const [result, setResult] = useState('')
  const [translating, setTranslating] = useState(false)
  const [history, setHistory] = useState<HistoryEntry[]>([])

  // Load history from localStorage on mount
  useEffect(() => {
    try {
      const raw = localStorage.getItem(HISTORY_KEY)
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (raw) setHistory(JSON.parse(raw) as HistoryEntry[])
    } catch {
      // Corrupt storage — ignore
    }
  }, [])

  function pushHistory(entry: HistoryEntry) {
    setHistory((prev) => {
      const next = [entry, ...prev].slice(0, HISTORY_LIMIT)
      try {
        localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
      } catch {
        // QuotaExceeded etc. — ignore
      }
      return next
    })
  }

  async function translate() {
    const trimmed = source.trim()
    if (!trimmed) {
      toast.warning('Vui lòng nhập văn bản cần dịch')
      return
    }
    setTranslating(true)
    try {
      const endpoint = useGpt4 ? 'translate' : 'translate/economy'
      const res = await api.post<{ translation: string }>(
        `/api/v1/ai/chat/${endpoint}?direction=${direction}`,
        trimmed,
        {
          headers: { 'Content-Type': 'text/plain', Accept: 'application/json' },
        },
      )
      const translation = res.data.translation ?? ''
      setResult(translation)
      pushHistory({
        id: Date.now(),
        direction,
        source: trimmed,
        result: translation,
        useGpt4,
        at: Date.now(),
      })
    } catch (err) {
      toast.error(extractApiError(err, 'Dịch thất bại'))
    } finally {
      setTranslating(false)
    }
  }

  function swapDirection() {
    setDirection((d) => (d === 'vn-to-jp' ? 'jp-to-vn' : 'vn-to-jp'))
    // Swap content too — keeps the conversation flowing
    setSource(result)
    setResult(source)
  }

  async function clearHistory() {
    const ok = await confirm({
      title: 'Xoá lịch sử dịch',
      message: 'Toàn bộ lịch sử dịch sẽ bị xoá. Hành động này không thể hoàn tác.',
      confirmText: 'Xoá',
      variant: 'destructive',
    })
    if (!ok) return
    setHistory([])
    try {
      localStorage.removeItem(HISTORY_KEY)
    } catch {
      // ignore
    }
    toast.success('Đã xoá lịch sử')
  }

  function restoreEntry(entry: HistoryEntry) {
    setDirection(entry.direction)
    setUseGpt4(entry.useGpt4)
    setSource(entry.source)
    setResult(entry.result)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Công cụ dịch thuật</h1>
        <p className="text-muted-foreground text-sm">Tiếng Việt ↔ Tiếng Nhật</p>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Languages className="text-primary size-4" />
            Dịch
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-4">
            <Button
              variant="outline"
              size="sm"
              onClick={swapDirection}
              className="font-medium"
            >
              {labelFor(direction)}
              <ArrowRightLeft className="ml-2 size-4" />
            </Button>

            <div className="flex items-center gap-2">
              <Switch id="gpt4" checked={useGpt4} onCheckedChange={setUseGpt4} />
              <Label htmlFor="gpt4" className="cursor-pointer text-sm">
                {useGpt4 ? (
                  <span className="text-primary">GPT-4o</span>
                ) : (
                  'GPT-3.5 Turbo'
                )}
              </Label>
            </div>
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <div className="space-y-1.5">
              <Label className="text-muted-foreground text-xs uppercase">
                {sourceLabel(direction)}
              </Label>
              <Textarea
                value={source}
                onChange={(e) => setSource(e.target.value)}
                rows={6}
                placeholder={
                  direction === 'vn-to-jp'
                    ? 'Nhập văn bản tiếng Việt...'
                    : '日本語を入力してください...'
                }
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-muted-foreground text-xs uppercase">
                {targetLabel(direction)}
              </Label>
              <Textarea
                value={result}
                readOnly
                rows={6}
                placeholder="Kết quả dịch sẽ hiển thị ở đây..."
                className="bg-muted/40"
              />
            </div>
          </div>

          <div className="flex justify-end">
            <Button onClick={translate} disabled={translating}>
              {translating ? 'Đang dịch...' : 'Dịch'}
            </Button>
          </div>
        </CardContent>
      </Card>

      {history.length > 0 && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <CardTitle className="text-base">Lịch sử ({history.length})</CardTitle>
            <Button size="sm" variant="ghost" onClick={clearHistory}>
              <Trash2 className="mr-1 size-4" />
              Xoá tất cả
            </Button>
          </CardHeader>
          <CardContent className="space-y-2">
            {history.map((h) => (
              <button
                key={h.id}
                type="button"
                onClick={() => restoreEntry(h)}
                className="hover:bg-muted/50 block w-full rounded-lg border p-3 text-left transition-colors"
              >
                <div className="mb-1 flex items-center gap-2">
                  <Badge variant="outline" className="text-xs">
                    {labelFor(h.direction)}
                  </Badge>
                  {h.useGpt4 && (
                    <Badge variant="secondary" className="text-xs">
                      GPT-4o
                    </Badge>
                  )}
                </div>
                <p className="line-clamp-1 text-sm">{h.source}</p>
                <p className="text-muted-foreground line-clamp-1 text-sm">→ {h.result}</p>
              </button>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
