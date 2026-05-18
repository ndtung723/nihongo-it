export type ContentType = 'vocabulary' | 'example' | 'conversation'

export interface TTSCheckResponse {
  exists: boolean
  url?: string
}

export interface WordAnalysis {
  text: string
  isCorrect: boolean
  suggestion?: string
}

export interface SpeechAnalysisResult {
  score: number
  feedback?: string
  personalizedFeedback?: string
  transcription?: string
  intonationScore?: number
  clarityScore?: number
  textScore?: number
  words?: WordAnalysis[]
}

export interface VocabularyExplanation {
  explanation: string
  examples?: { japanese: string; vietnamese: string }[]
}

export interface ChatResponse {
  message: string
}

export interface VocabularyExplanationRequest {
  term: string
  reading?: string
  meaning?: string
  partOfSpeech?: string
  explanation?: string
  exampleSentences?: string[]
  language?: string
}

export interface AIExplanationResponse {
  content: string
}

export interface FeedbackSummary {
  summary: string
  common_errors: string[]
  improvement_tips: string[]
  attempts?: number
  avg_score?: number
  max_score?: number
}
