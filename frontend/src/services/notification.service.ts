import api from '../utils/api';

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'STUDY_REMINDER' | 'REVIEW_DUE' | 'SYSTEM_ANNOUNCEMENT';
  isRead: boolean;
  actionUrl?: string;
  sentAt: string;
  readAt?: string;
  reviewCount?: number;
  priorityLevel: number;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

class NotificationService {
  async getNotifications(page = 0, size = 20): Promise<NotificationPage> {
    const response = await api.get('/api/v1/notify/notifications', { params: { page, size } });
    return response.data;
  }

  async getUnreadCount(): Promise<number> {
    const response = await api.get('/api/v1/notify/notifications/unread-count');
    return response.data.count;
  }

  async markAsRead(id: string): Promise<void> {
    await api.put(`/api/v1/notify/notifications/${id}/read`);
  }

  async markAllAsRead(): Promise<void> {
    await api.put('/api/v1/notify/notifications/read-all');
  }

  async deleteNotification(id: string): Promise<void> {
    await api.delete(`/api/v1/notify/notifications/${id}`);
  }
}

export default new NotificationService();
