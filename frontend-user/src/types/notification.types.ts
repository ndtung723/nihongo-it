import type { DateString, UUID } from './common.types'

export type NotificationType = 'STUDY_REMINDER' | 'REVIEW_DUE' | 'SYSTEM_ANNOUNCEMENT'

export interface NotificationItem {
  id: UUID
  title: string
  message: string
  type: NotificationType
  isRead: boolean
  actionUrl?: string
  sentAt: DateString
  readAt?: DateString
  reviewCount?: number
  priorityLevel?: number
}
