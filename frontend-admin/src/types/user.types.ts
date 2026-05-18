import type { JlptLevel, DateString, UUID } from './common.types'

export interface UserInfo {
  userId: UUID
  email: string
  fullName: string
  roleId: number
  profilePicture?: string
  currentLevel?: JlptLevel
  jlptGoal?: JlptLevel
  lastLogin?: DateString
  isActive?: boolean
  isEmailVerified?: boolean
  createdAt?: DateString
  updatedAt?: DateString
}

export interface UserDetailInfo extends UserInfo {
  streakCount?: number
  points?: number
  reminderEnabled?: boolean
  reminderTime?: string
  notificationPreferences?: string
  minCardThreshold?: number
  flashcardCount?: number
  newCards?: number
  learningCards?: number
  masteredCards?: number
}

export interface UserListResponse {
  users: UserInfo[]
  totalItems: number
  totalPages: number
  currentPage: number
}

export interface UserPreferences {
  reminderEnabled?: boolean
  reminderTime?: string
  minCardThreshold?: number
  leechNotificationsEnabled?: boolean
  notificationPreferences?: Record<string, boolean>
}

export interface LoginRequest {
  email: string
  password: string
}
export interface LoginResponse {
  token?: string
  message?: string
}
export interface SignupRequest {
  email: string
  password: string
  fullName: string
  profilePicture?: string
  currentLevel?: string
  jlptGoal?: string
}
export interface SignupResponse {
  message?: string
}
export interface GetCurrentUserResponse {
  status: 'OK' | 'NG'
  userInfo?: UserInfo
}
export interface UpdateProfileRequest {
  fullName: string
  currentLevel: string
  jlptGoal: string
}
export interface UpdateProfileResponse {
  status: 'OK' | 'NG'
  message?: string
}
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UserCreateRequest {
  email: string
  password: string
  fullName: string
  profilePicture?: string
  currentLevel?: string
  jlptGoal?: string
  roleId?: number
}
export interface UserUpdateRequest {
  email?: string
  password?: string
  fullName?: string
  profilePicture?: string
  currentLevel?: string
  jlptGoal?: string
  isActive?: boolean
  isEmailVerified?: boolean
  roleId?: number
  reminderEnabled?: boolean
  reminderTime?: string
  notificationPreferences?: string
  minCardThreshold?: number
}
export interface DashboardStats {
  userCount: number
  vocabularyCount: number
  categoryCount: number
  topicCount: number
  newUsers: number
  activeUsers: number
  searchesToday: number
  flashcardsCreatedToday: number
  flashcardsStudiedToday: number
  recentActivities: { user: string; action: string; timestamp: DateString }[]
}
