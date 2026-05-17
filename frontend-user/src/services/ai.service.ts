import api from '@/lib/api'
import { getAccessToken } from '@/lib/tokenStore'
import type {
  AIExplanationResponse,
  ChatResponse,
  ContentType,
  TTSCheckResponse,
  VocabularyExplanation,
  VocabularyExplanationRequest,
} from '@/types/ai.types'

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

const aiService = {
  /** Check whether the backend already has a cached TTS audio for this text */
  async checkTTSExists(text: string, contentType: ContentType): Promise<TTSCheckResponse> {
    try {
      const res = await api.get('/api/v1/ai/tts/check', {
        params: { text, contentType },
      })
      return res.data
    } catch {
      return { exists: false }
    }
  },

  async getExistingTTSAudio(text: string, contentType: ContentType): Promise<Blob> {
    const res = await api.get('/api/v1/ai/tts/audio', {
      params: { text, contentType },
      responseType: 'blob',
    })
    return res.data
  },

  /** Generate TTS audio for Japanese text. Reuses cached audio when available. */
  async generateTTS(
    text: string,
    contentType: ContentType,
    speechSpeed = 1.0,
    saveAudio = true,
  ): Promise<Blob> {
    try {
      const check = await this.checkTTSExists(text, contentType)
      if (check.exists) return await this.getExistingTTSAudio(text, contentType)
    } catch {
      // fall through to generation
    }

    const res = await api.post('/api/v1/ai/tts/generate', text, {
      headers: {
        'Content-Type': 'text/plain; charset=UTF-8',
        'Accept-Language': 'ja-JP',
        'X-Speech-Speed': speechSpeed.toString(),
        'X-Content-Language': 'ja',
        'X-Content-Type': contentType,
        'X-Save-Audio': saveAudio.toString(),
        Accept: 'audio/mpeg',
      },
      responseType: 'arraybuffer',
    })
    return new Blob([res.data], { type: 'audio/mpeg' })
  },

  /** Play an audio blob or URL. Resolves when playback ends. */
  async playAudio(audioData: Blob | string): Promise<void> {
    const audioUrl = typeof audioData === 'string' ? audioData : URL.createObjectURL(audioData)
    const audio = new Audio(audioUrl)
    try {
      await audio.play()
      await new Promise<void>((resolve) => {
        audio.onended = () => resolve()
      })
    } finally {
      if (typeof audioData !== 'string') URL.revokeObjectURL(audioUrl)
    }
  },

  async explainVocabulary(
    term: string,
    pronunciation?: string,
    meaning?: string,
    topicName?: string,
    example?: string,
  ): Promise<VocabularyExplanation> {
    const res = await api.post('/api/v1/ai/chat/vocabulary/explain', null, {
      params: { term, pronunciation, meaning, topicName, example },
    })
    let data = res.data
    if (typeof data === 'string') {
      try {
        const cleaned = data.replace(/```json/g, '').replace(/```/g, '').trim()
        data = JSON.parse(cleaned)
      } catch {
        return { explanation: data, examples: [] }
      }
    }
    return data
  },

  async chatAboutVocabulary(vocabWord: string, userMessage: string): Promise<ChatResponse> {
    const res = await api.post('/api/v1/ai/vocabulary/chat', null, {
      params: { vocabWord, userMessage },
    })
    let data = res.data
    if (typeof data === 'string') {
      try {
        const cleaned = data.replace(/```json/g, '').replace(/```/g, '').trim()
        data = JSON.parse(cleaned)
      } catch {
        return { message: data }
      }
    }
    return data
  },

  async getVocabularyExplanation(
    request: VocabularyExplanationRequest,
  ): Promise<AIExplanationResponse> {
    const params: Record<string, string> = {
      term: request.term,
      reading: request.reading || '',
      meaning: request.meaning || '',
      partOfSpeech: request.partOfSpeech || '',
      explanation: request.explanation || '',
      language: request.language || 'vi',
    }
    if (request.exampleSentences && request.exampleSentences.length > 0) {
      params.exampleSentences = request.exampleSentences[0]
    }
    const res = await api.post('/api/v1/ai/chat/vocabulary/explain', null, { params })
    const data = res.data
    if (typeof data === 'string') return { content: data }
    if (data?.content) return data
    if (data?.explanation) return { content: data.explanation }
    return { content: JSON.stringify(data) }
  },

  /**
   * Stream AI chat response via Server-Sent Events.
   * Uses fetch directly (not axios) because we need to read the response body
   * as a stream. Aborting? Pass an AbortSignal.
   */
  async streamVocabularyChat(
    vocabWord: string,
    userMessage: string,
    onChunk: (chunk: string) => void,
    onDone: () => void,
    onError: (err: Error) => void,
    signal?: AbortSignal,
  ): Promise<void> {
    const params = new URLSearchParams({ vocabWord, userMessage })
    const url = `${API_BASE}/api/v1/ai/chat/vocabulary/chat/stream?${params.toString()}`

    const token = getAccessToken()
    const headers: Record<string, string> = { Accept: 'text/event-stream' }
    if (token) headers.Authorization = `Bearer ${token}`

    let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
    try {
      const response = await fetch(url, { method: 'GET', headers, signal, credentials: 'include' })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      if (!response.body) throw new Error('No response body')

      reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trimStart()
            if (data && data !== '[DONE]') onChunk(data)
          }
        }
      }
      onDone()
    } catch (err) {
      const error = err as Error
      if (error.name !== 'AbortError') onError(error)
    } finally {
      reader?.releaseLock()
    }
  },
}

export default aiService
