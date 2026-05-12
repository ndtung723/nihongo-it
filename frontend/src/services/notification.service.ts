import api from "../utils/api";
import type { NotificationItem } from "@/types/notification.types";
import type { PagedResponse } from "@/types/common.types";

export type { NotificationItem };
type NotificationPage = PagedResponse<NotificationItem>;

class NotificationService {
  async getNotifications(page = 0, size = 20): Promise<NotificationPage> {
    const response = await api.get("/api/v1/notify/notifications", {
      params: { page, size },
    });
    return response.data;
  }

  async getUnreadCount(): Promise<number> {
    const response = await api.get("/api/v1/notify/notifications/unread-count");
    return response.data.count;
  }

  async markAsRead(id: string): Promise<void> {
    await api.put(`/api/v1/notify/notifications/${id}/read`);
  }

  async markAllAsRead(): Promise<void> {
    await api.put("/api/v1/notify/notifications/read-all");
  }

  async deleteNotification(id: string): Promise<void> {
    await api.delete(`/api/v1/notify/notifications/${id}`);
  }
}

export default new NotificationService();
