import { defineStore } from "pinia";
import { ref } from "vue";
import notificationService from "@/services/notification.service";
import type { NotificationItem } from "@/types/notification.types";

export const useNotificationsStore = defineStore("notifications", () => {
  const unreadCount = ref(0);
  const notifications = ref<NotificationItem[]>([]);
  const loading = ref(false);

  async function fetchUnreadCount() {
    try {
      unreadCount.value = await notificationService.getUnreadCount();
    } catch {
      // silent — badge stays at last known value
    }
  }

  async function fetchNotifications(page = 0) {
    loading.value = true;
    try {
      const res = await notificationService.getNotifications(page);
      notifications.value = res.content;
    } finally {
      loading.value = false;
    }
  }

  async function markAsRead(id: string) {
    await notificationService.markAsRead(id);
    const item = notifications.value.find((n) => n.id === id);
    if (item && !item.isRead) {
      item.isRead = true;
      if (unreadCount.value > 0) unreadCount.value--;
    }
  }

  async function markAllAsRead() {
    await notificationService.markAllAsRead();
    notifications.value.forEach((n) => {
      n.isRead = true;
    });
    unreadCount.value = 0;
  }

  return {
    unreadCount,
    notifications,
    loading,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
  };
});
