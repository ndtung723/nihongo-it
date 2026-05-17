'use client'

import { useEffect, useRef, useState } from 'react'
import { Send, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import aiService from '@/services/ai.service'
import { useAppToast } from '@/hooks/useAppToast'
import { extractApiError } from '@/types/common.types'

interface AIChatProps {
  vocabWord: string
}

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
}

export function AIChat({ vocabWord }: AIChatProps) {
  const toast = useAppToast()
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const abortRef = useRef<AbortController | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  const idCounter = useRef(0)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  // Abort in-flight stream on unmount or vocabWord change
  useEffect(() => {
    return () => {
      abortRef.current?.abort()
    }
  }, [vocabWord])

  async function handleSend() {
    const text = input.trim()
    if (!text || streaming) return

    const userMsg: Message = { id: ++idCounter.current, role: 'user', content: text }
    const assistantMsg: Message = { id: ++idCounter.current, role: 'assistant', content: '' }
    setMessages((prev) => [...prev, userMsg, assistantMsg])
    setInput('')
    setStreaming(true)

    const controller = new AbortController()
    abortRef.current = controller

    await aiService.streamVocabularyChat(
      vocabWord,
      text,
      (chunk) => {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantMsg.id ? { ...m, content: m.content + chunk } : m,
          ),
        )
      },
      () => {
        setStreaming(false)
        abortRef.current = null
      },
      (err) => {
        setStreaming(false)
        abortRef.current = null
        toast.error(extractApiError(err, 'Lỗi khi nhận phản hồi AI'))
      },
      controller.signal,
    )
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void handleSend()
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <Sparkles className="text-primary size-4" />
          Hỏi AI về &ldquo;{vocabWord}&rdquo;
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="bg-muted/40 max-h-[400px] min-h-[200px] space-y-3 overflow-y-auto rounded-lg p-3">
          {messages.length === 0 ? (
            <p className="text-muted-foreground py-8 text-center text-sm">
              Hỏi bất cứ điều gì về từ này — cách dùng, ngữ cảnh, từ liên quan, v.v.
            </p>
          ) : (
            messages.map((m) => (
              <div
                key={m.id}
                className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${
                    m.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-card border'
                  }`}
                >
                  {m.content || (
                    <span className="text-muted-foreground italic">Đang trả lời...</span>
                  )}
                </div>
              </div>
            ))
          )}
          <div ref={bottomRef} />
        </div>

        <div className="flex gap-2">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Nhập câu hỏi..."
            disabled={streaming}
          />
          <Button onClick={handleSend} disabled={streaming || !input.trim()} size="icon">
            <Send className="size-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
