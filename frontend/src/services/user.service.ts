import api from '../utils/api';

interface UserPreferences {
  notificationPreferences?: string;
  reminderEnabled?: boolean;
  reminderTime?: string;
  minCardThreshold?: number;
  leechNotificationsEnabled?: boolean;
}

class UserService {
  /**
   * Lấy preferences của người dùng
   */
  async getUserPreferences() {
    try {
      const response = await api.get('/user-service-api/v1/user/preferences');
      return response.data;
    } catch (error) {
      throw error;
    }
  }

  /**
   * Cập nhật preferences của người dùng
   */
  async updateUserPreferences(preferences: UserPreferences) {
    try {
      const response = await api.put('/user-service-api/v1/user/preferences', preferences);
      return response.data;
    } catch (error) {
      throw error;
    }
  }
}

export default new UserService();
