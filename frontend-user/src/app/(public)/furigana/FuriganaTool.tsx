'use client'

import { useState } from 'react'
import { Copy, FileText, Sparkles } from 'lucide-react'
import api from '@/lib/api'
import { extractApiError } from '@/types/common.types'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'

interface Token {
  text: string
  reading?: string
  isKanji?: boolean
}

export function FuriganaTool() {
  const toast = useAppToast()
  const [text, setText] = useState('日本語を勉強しています。')
  const [tokens, setTokens] = useState<Token[]>([])
  const [showFurigana, setShowFurigana] = useState(true)
  const [loading, setLoading] = useState(false)

  async function generate() {
    const trimmed = text.trim()
    if (!trimmed) {
      toast.warning('Vui lòng nhập văn bản tiếng Nhật')
      return
    }
    setLoading(true)
    try {
      const res = await api.get<Token[]>('/api/v1/learning/furigana', {
        params: { text: trimmed },
      })
      setTokens(res.data)
    } catch (err) {
      toast.error(extractApiError(err, 'Tạo furigana thất bại'))
    } finally {
      setLoading(false)
    }
  }

  function copyHtml() {
    if (tokens.length === 0) return
    const html = tokens
      .map((t) =>
        t.isKanji && t.reading ? `<ruby>${t.text}<rt>${t.reading}</rt></ruby>` : t.text,
      )
      .join('')
    navigator.clipboard.writeText(html).then(
      () => toast.success('Đã sao chép HTML'),
      () => toast.error('Sao chép thất bại'),
    )
  }

  function copyPlain() {
    if (tokens.length === 0) return
    const plain = tokens.map((t) => t.text).join('')
    navigator.clipboard.writeText(plain).then(
      () => toast.success('Đã sao chép văn bản'),
      () => toast.error('Sao chép thất bại'),
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="text-center">
        <h1 className="text-2xl font-bold">日本語ふりがな生成器</h1>
        <p className="text-muted-foreground text-sm">Japanese Furigana Generator</p>
      </div>

      <Card>
        <CardContent className="space-y-3 p-4">
          <Textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={3}
            placeholder="例: 日本語を勉強しています。"
            className="resize-none"
          />
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Switch
                id="show-furigana"
                checked={showFurigana}
                onCheckedChange={setShowFurigana}
              />
              <Label htmlFor="show-furigana" className="cursor-pointer text-sm">
                Hiện furigana
              </Label>
            </div>
            <Button onClick={generate} disabled={loading}>
              <Sparkles className="mr-2 size-4" />
              {loading ? 'Đang tạo...' : 'Tạo furigana'}
            </Button>
          </div>
        </CardContent>
      </Card>

      {tokens.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Kết quả</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="leading-loose text-lg">
              {tokens.map((t, i) => (
                <ruby key={i}>
                  {t.text}
                  {t.isKanji && t.reading && showFurigana && (
                    <rt className="text-muted-foreground text-xs">{t.reading}</rt>
                  )}
                </ruby>
              ))}
            </div>
            <div className="flex justify-end gap-2 border-t pt-3">
              <Button size="sm" variant="outline" onClick={copyHtml}>
                <Copy className="mr-1 size-3.5" />
                HTML
              </Button>
              <Button size="sm" variant="outline" onClick={copyPlain}>
                <FileText className="mr-1 size-3.5" />
                Văn bản
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
