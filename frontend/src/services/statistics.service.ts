import api from "../utils/api";

// Interfaces for statistics data
export interface UserStatistics {
  userId: string;
  userName: string;
  email: string;
  summary?: {
    totalCards?: number;
    dueCardsNow?: number;
    currentStreak?: number;
    reviewsLast30Days?: number;
    overallRetentionRate?: number;
  };
  cardsByState?: Record<string, number>;
  lastActive?: string;
  progress?: number;
  retentionRate?: number;
  currentStreak?: number;
}

export interface ReviewHistoryItem {
  reviewId: string;
  flashcardId: string;
  rating: number;
  elapsedDays: number;
  scheduledDays: number;
  state: string;
  timestamp: string;
  vocabulary?: {
    vocabId: string;
    japanese?: string;
    english?: string;
    term?: string;
    meaning?: string;
    jlptLevel?: string;
  };
}

export interface UserStatisticsDetail extends UserStatistics {
  profileInfo?: {
    currentLevel?: string;
    jlptGoal?: string;
    createdAt?: string;
    lastLogin?: string;
  };
  cardsByJlptLevel?: Record<string, number>;
  dailyReviews?: Record<string, number>;
  retentionRateByDay?: Record<string, number>;
  memoryStrengthDistribution?: {
    weak?: number;
    medium?: number;
    strong?: number;
  };
  cardsDueByDay?: Record<string, number>;
  reviewHistory?: ReviewHistoryItem[];
}

// For admin statistics overview
export interface AdminStatisticsOverview {
  totalUsers: number;
  activeUsers: number;
  totalFlashcards: number;
  averageCardsPerUser: number;
  averageRetentionRate: number;
  usersByLevel: Record<string, number>;
  usersByJlptGoal: Record<string, number>;
  topPerformingUsers: UserStatistics[];
  mostActiveUsers: UserStatistics[];
}

class StatisticsService {
  // Get statistics for all users (admin only)
  async getAllUserStatistics(
    page: number,
    size: number,
    sortBy: string,
    sortDir: string,
    search?: string,
  ): Promise<{
    result?: {
      status: string;
      message: string;
    };
    data?: {
      users: UserStatistics[];
      totalItems: number;
      totalPages: number;
      currentPage: number;
    };
    users: UserStatistics[];
    totalItems: number;
    totalPages: number;
    currentPage: number;
  }> {
    try {
      const response = await api.get(
        "/api/v1/learning/admin/statistics/users",
        {
          params: {
            page,
            size,
            sortBy,
            sortDir,
            search,
          },
        },
      );
      return response.data; // Return the raw data that includes both result and data
    } catch (error) {
      throw error;
    }
  }

  // Get detailed statistics for a specific user (admin only)
  async getUserStatisticsById(userId: string): Promise<UserStatisticsDetail> {
    try {
      const response = await api.get(
        `/api/v1/learning/admin/statistics/users/${userId}`,
      );
      return response.data.data;
    } catch (error) {
      throw error;
    }
  }

  // Get overall statistics for admin dashboard
  async getStatisticsOverview(): Promise<AdminStatisticsOverview> {
    try {
      const response = await api.get(
        "/api/v1/learning/admin/statistics/overview",
      );
      return response.data.data;
    } catch (error) {
      throw error;
    }
  }
}

export default new StatisticsService();
