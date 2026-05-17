import type { JlptLevel, DateString, UUID } from './common.types'

export interface ConversationLine {
  lineId?: UUID
  speaker: string
  japaneseText: string
  vietnameseTranslation?: string
  notes?: string
  importantVocab?: string
  orderIndex: number
  tempId?: string
}

export interface Conversation {
  conversationId?: UUID
  title: string
  description?: string
  jlptLevel?: JlptLevel
  unit?: number
  lines?: ConversationLine[]
  createdAt?: DateString
  updatedAt?: DateString
}
