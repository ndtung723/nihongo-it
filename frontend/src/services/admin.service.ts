import api from '../utils/api';

export interface UserCreateRequest {
  email: string;
  password: string;
  fullName: string;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
  roleId?: number;
}

export interface UserUpdateRequest {
  email?: string;
  password?: string;
  fullName?: string;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
  isActive?: boolean;
  isEmailVerified?: boolean;
  roleId?: number;
  reminderEnabled?: boolean;
  reminderTime?: string;
  notificationPreferences?: string;
  minCardThreshold?: number;
}

export interface UserInfo {
  userId: string;
  email: string;
  fullName: string;
  roleId: number;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
  lastLogin?: string;
  isActive?: boolean;
  isEmailVerified?: boolean;
  createdAt?: string;
  updatedAt?: string;
  streakCount?: number;
  points?: number;
  reminderEnabled?: boolean;
  reminderTime?: string;
  notificationPreferences?: string;
  minCardThreshold?: number;
  flashcardCount?: number;
  newCards?: number;
  learningCards?: number;
  masteredCards?: number;
}

export interface UserListResponse {
  users: UserInfo[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
}

export interface DashboardStats {
  userCount: number;
  vocabularyCount: number;
  categoryCount: number;
  topicCount: number;
  newUsers: number;
  activeUsers: number;
  searchesToday: number;
  flashcardsCreatedToday: number;
  flashcardsStudiedToday: number;
  recentActivities: {
    user: string;
    action: string;
    timestamp: string;
  }[];
}

class AdminService {
  /**
   * Get dashboard statistics
   */
  async getDashboardStats(): Promise<DashboardStats> {
    try {
      const response = await api.get('/api/v1/learning/admin/dashboard/stats');
      return response.data.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Get all users with pagination and optional search
   */
  async getUsers(page = 0, size = 10, search?: string, sortBy = 'email', sortDir = 'asc'): Promise<UserListResponse> {
    try {
      const params = { page, size, sortBy, sortDir };
      if (search) {
        Object.assign(params, { search });
      }
      const response = await api.get('/api/v1/user/admin/users', { params });
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Get user by ID
   */
  async getUserById(userId: string): Promise<UserInfo> {
    try {
      const response = await api.get(`/api/v1/user/admin/users/${userId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Create a new user
   */
  async createUser(userData: UserCreateRequest): Promise<UserInfo> {
    try {
      const response = await api.post('/api/v1/user/admin/users', userData);
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Update an existing user
   */
  async updateUser(userId: string, userData: UserUpdateRequest): Promise<UserInfo> {
    try {
      const response = await api.put(`/api/v1/user/admin/users/${userId}`, userData);
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Deactivate a user (soft delete)
   */
  async deactivateUser(userId: string): Promise<{ status: string; message: string }> {
    try {
      const response = await api.delete(`/api/v1/user/admin/users/${userId}`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Activate a user
   */
  async activateUser(userId: string): Promise<{ status: string; message: string }> {
    try {
      const response = await api.put(`/api/v1/user/admin/users/${userId}/activate`);
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Change a user's role
   */
  async changeUserRole(userId: string, roleId: number): Promise<{ status: string; message: string }> {
    try {
      const response = await api.put(`/api/v1/user/admin/users/${userId}/change-role`, null, {
        params: { roleId }
      });
      return response.data;
    } catch (error) {
      throw error;
    }
  }
}

export default new AdminService();
