import type { DateString, UUID } from './common.types'

export interface UserSummaryStats {
  totalCards?: number
  dueCardsNow?: number
  currentStreak?: number
  reviewsLast30Days?: number
  overallRetentionRate?: number
}

export interface UserStatistics {
  userId: UUID
  userName: string
  email: string
  summary?: UserSummaryStats
  cardsByState?: Record<string, number>
  lastActive?: DateString
  progress?: number
  retentionRate?: number
  currentStreak?: number
}

export interface ReviewHistoryItem {
  reviewId: UUID
  flashcardId: UUID
  rating: number
  elapsedDays: number
  scheduledDays: number
  state: string
  timestamp: DateString
  vocabulary?: {
    vocabId: UUID
    japanese?: string
    english?: string
    term?: string
    meaning?: string
    jlptLevel?: string
  }
}

export interface UserStatisticsDetail extends UserStatistics {
  profileInfo?: {
    currentLevel?: string
    jlptGoal?: string
    createdAt?: DateString
    lastLogin?: DateString
  }
  cardsByJlptLevel?: Record<string, number>
  dailyReviews?: Record<string, number>
  retentionRateByDay?: Record<string, number>
  memoryStrengthDistribution?: {
    weak?: number
    medium?: number
    strong?: number
  }
  cardsDueByDay?: Record<string, number>
  reviewHistory?: ReviewHistoryItem[]
}

export interface AdminStatisticsOverview {
  totalUsers: number
  activeUsers: number
  totalFlashcards: number
  averageCardsPerUser: number
  averageRetentionRate: number
  usersByLevel: Record<string, number>
  usersByJlptGoal: Record<string, number>
  topPerformingUsers: UserStatistics[]
  mostActiveUsers: UserStatistics[]
}

export interface UserStatisticsListResponse {
  users: UserStatistics[]
  totalItems: number
  totalPages: number
  currentPage: number
}
