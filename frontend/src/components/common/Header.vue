<template>
  <!-- Navigation Header -->
  <v-app-bar flat density="compact" class="px-2 header-bar">
    <v-app-bar-title class="logo-container pa-0">
      <router-link :to="{ name: 'home' }" class="app-logo d-flex align-center text-decoration-none">
        <img src="/nihongo_it_logo_larger_text.svg" alt="Nihongo IT" class="logo-image" />
      </router-link>
    </v-app-bar-title>

    <v-spacer></v-spacer>

    <!-- Main Navigation Links - Desktop -->
    <div class="d-none d-md-flex align-center navigation-links">
      <v-btn
        variant="text"
        :to="{ name: 'home' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Trang chủ
      </v-btn>
      <v-btn
        variant="text"
        :to="{ name: 'vocabularyLearning' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Từ vựng
      </v-btn>
      <v-btn
        variant="text"
        :to="{ name: 'conversationLearning' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Hội thoại
      </v-btn>
      <v-btn
        variant="text"
        :to="{ name: 'statistics' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Tiến độ
      </v-btn>
      <v-btn
        variant="text"
        :to="{ name: 'translations' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Dịch thuật
      </v-btn>
      <!-- <v-btn
        variant="text"
        :to="{ name: 'furigana' }"
        density="compact"
        class="mx-1 nav-btn"
      >
        Furigana
      </v-btn> -->
    </div>

    <!-- Mobile Menu Button -->
    <v-app-bar-nav-icon
      class="d-md-none"
      @click="toggleMobileMenu"
    ></v-app-bar-nav-icon>

    <v-spacer></v-spacer>

    <!-- Login/Register Buttons (for logged out users) -->
    <div v-if="!isLoggedIn" class="d-flex align-center">
      <v-btn
        variant="text"
        :to="{ name: 'login' }"
        size="small"
        class="mx-1 d-none d-sm-flex"
      >
        Đăng nhập
      </v-btn>
      <v-btn
        :to="{ name: 'register' }"
        color="primary"
        variant="elevated"
        size="small"
        class="ml-1"
      >
        Đăng ký
      </v-btn>
    </div>

    <!-- User Avatar & Dropdown (for logged in users) -->
    <div v-else class="d-flex align-center">
      <!-- Notification Bell -->
      <v-menu
        v-model="notifOpen"
        :close-on-content-click="false"
        location="bottom end"
        max-width="380"
      >
        <template v-slot:activator="{ props: menuProps }">
          <v-btn icon variant="text" size="small" class="mr-1 d-none d-sm-flex" v-bind="menuProps" @click="openNotifications">
            <v-badge
              v-if="unreadCount > 0"
              :content="unreadCount > 99 ? '99+' : String(unreadCount)"
              color="error"
              floating
            >
              <v-icon>mdi-bell</v-icon>
            </v-badge>
            <v-icon v-else>mdi-bell-outline</v-icon>
          </v-btn>
        </template>

        <v-card min-width="320" max-height="480" class="d-flex flex-column">
          <v-card-title class="d-flex align-center justify-space-between py-2 px-4">
            <span class="text-subtitle-2">Thông báo</span>
            <v-btn
              v-if="unreadCount > 0"
              variant="text"
              size="x-small"
              color="primary"
              @click="doMarkAllRead"
            >Đánh dấu tất cả đã đọc</v-btn>
          </v-card-title>
          <v-divider />
          <div class="overflow-y-auto flex-grow-1">
            <div v-if="notifLoading" class="d-flex justify-center py-6">
              <v-progress-circular size="28" indeterminate color="primary" />
            </div>
            <div v-else-if="notifications.length === 0" class="text-center text-body-2 text-medium-emphasis py-8">
              Không có thông báo nào
            </div>
            <v-list v-else density="compact" lines="two">
              <v-list-item
                v-for="n in notifications"
                :key="n.id"
                :class="{ 'bg-blue-lighten-5': !n.isRead }"
                @click="handleNotifClick(n)"
              >
                <template v-slot:prepend>
                  <v-icon
                    :color="n.type === 'REVIEW_DUE' ? 'warning' : n.type === 'SYSTEM_ANNOUNCEMENT' ? 'info' : 'primary'"
                    size="small"
                    class="mr-1"
                  >
                    {{ n.type === 'REVIEW_DUE' ? 'mdi-cards' : n.type === 'SYSTEM_ANNOUNCEMENT' ? 'mdi-bullhorn' : 'mdi-bell' }}
                  </v-icon>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">{{ n.title }}</v-list-item-title>
                <v-list-item-subtitle class="text-caption">{{ n.message }}</v-list-item-subtitle>
                <template v-slot:append>
                  <v-btn
                    icon
                    size="x-small"
                    variant="text"
                    @click.stop="doDelete(n.id)"
                  >
                    <v-icon size="14">mdi-close</v-icon>
                  </v-btn>
                </template>
              </v-list-item>
            </v-list>
          </div>
        </v-card>
      </v-menu>

      <v-menu min-width="200px" rounded>
        <template v-slot:activator="{ props }">
          <v-btn
            variant="text"
            v-bind="props"
            size="small"
          >
            <v-avatar size="32" color="primary" class="mr-1">
              <v-img
                v-if="userProfilePicture"
                :src="userProfilePicture"
                alt="Ảnh đại diện"
              ></v-img>
              <span v-else class="text-subtitle-1 text-white">{{ avatarInitials }}</span>
            </v-avatar>
            <v-icon size="small" class="d-none d-sm-flex">mdi-chevron-down</v-icon>
          </v-btn>
        </template>

        <v-card>
          <v-card-text>
            <div class="d-flex align-center mb-2">
              <v-avatar size="32" color="primary" class="mr-2">
                <v-img
                  v-if="userProfilePicture"
                  :src="userProfilePicture"
                  alt="Ảnh đại diện"
                ></v-img>
                <span v-else class="text-subtitle-1 text-white">{{ avatarInitials }}</span>
              </v-avatar>
              <div>
                <div class="text-subtitle-2 font-weight-medium">{{ username }}</div>
                <div class="text-caption text-medium-emphasis">{{ userLevel }}</div>
              </div>
            </div>
          </v-card-text>
          <v-divider></v-divider>
          <v-list density="compact">
            <v-list-item :to="{ name: 'profile' }" density="compact" prepend-icon="mdi-account-outline">
              <v-list-item-title class="text-body-2">Hồ sơ cá nhân</v-list-item-title>
            </v-list-item>
            <v-list-item :to="{ name: 'accountSettings' }" density="compact" prepend-icon="mdi-cog-outline">
              <v-list-item-title class="text-body-2">Cài đặt</v-list-item-title>
            </v-list-item>
            <v-list-item v-if="isAdmin" :to="{ name: 'adminDashboard' }" density="compact" prepend-icon="mdi-shield-account">
              <v-list-item-title class="text-body-2">Quản trị viên</v-list-item-title>
            </v-list-item>
            <v-list-item @click="logout" density="compact" prepend-icon="mdi-logout">
              <v-list-item-title class="text-body-2">Đăng xuất</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-card>
      </v-menu>
    </div>
  </v-app-bar>

  <!-- Mobile Navigation Drawer -->
  <v-navigation-drawer
    v-model="mobileMenuOpen"
    temporary
    location="left"
  >
    <v-list density="compact">
      <v-list-item prepend-icon="mdi-home" title="Trang chủ" :to="{ name: 'home' }" />
      <v-list-item prepend-icon="mdi-book-open-variant" title="Từ vựng" :to="{ name: 'vocabularyLearning' }" />
      <v-list-item prepend-icon="mdi-chat" title="Hội thoại" :to="{ name: 'conversationLearning' }" />
      <v-list-item prepend-icon="mdi-translate" title="Dịch thuật" :to="{ name: 'translations' }" />
      <v-list-item prepend-icon="mdi-alphabetical-variant" title="Furigana" :to="{ name: 'furigana' }" />
      <v-list-item prepend-icon="mdi-chart-line" title="Tiến độ học tập" :to="{ name: 'statistics' }" />
    </v-list>
  </v-navigation-drawer>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-facing-decorator'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'
import { ROLES } from '@/types/roles'
import notificationService, { type NotificationItem } from '@/services/notification.service'

@Component({ name: 'AppHeader' })
export default class AppHeader extends Vue {
  private authStore = useAuthStore()
  private mobileMenuOpen = false

  // Notification state
  notifOpen = false
  notifLoading = false
  unreadCount = 0
  notifications: NotificationItem[] = []
  private pollTimer: ReturnType<typeof setInterval> | null = null

  get isLoggedIn(): boolean { return !!this.authStore.user }
  get username(): string { return this.authStore.user?.fullName || 'Guest' }
  get userLevel(): string { return this.authStore.user?.currentLevel || 'N5' }
  get userProfilePicture(): string { return this.authStore.user?.profilePicture || '' }
  get avatarInitials(): string { return this.username.charAt(0).toUpperCase() }
  get isAdmin(): boolean { return this.authStore.user?.roleId === ROLES.ADMIN }

  toggleMobileMenu(): void { this.mobileMenuOpen = !this.mobileMenuOpen }

  logout(): void {
    if (this.pollTimer) clearInterval(this.pollTimer)
    this.authStore.logout()
    useRouter().push({ name: 'login' })
  }

  async mounted() {
    if (this.isLoggedIn) {
      await this.fetchUnreadCount()
      this.pollTimer = setInterval(() => this.fetchUnreadCount(), 60_000)
    }
  }

  beforeUnmount() {
    if (this.pollTimer) clearInterval(this.pollTimer)
    this.mobileMenuOpen = false
  }

  async fetchUnreadCount() {
    try {
      this.unreadCount = await notificationService.getUnreadCount()
    } catch { /* silently ignore if service unavailable */ }
  }

  async openNotifications() {
    this.notifLoading = true
    try {
      const page = await notificationService.getNotifications(0, 20)
      this.notifications = page.content
      this.unreadCount = page.content.filter(n => !n.isRead).length
    } catch {
      this.notifications = []
    } finally {
      this.notifLoading = false
    }
  }

  async handleNotifClick(n: NotificationItem) {
    if (!n.isRead) {
      await notificationService.markAsRead(n.id)
      n.isRead = true
      this.unreadCount = Math.max(0, this.unreadCount - 1)
    }
    if (n.actionUrl) {
      this.notifOpen = false
      useRouter().push(n.actionUrl)
    }
  }

  async doMarkAllRead() {
    await notificationService.markAllAsRead()
    this.notifications.forEach(n => { n.isRead = true })
    this.unreadCount = 0
  }

  async doDelete(id: string) {
    await notificationService.deleteNotification(id)
    const idx = this.notifications.findIndex(n => n.id === id)
    if (idx !== -1) {
      if (!this.notifications[idx].isRead) this.unreadCount = Math.max(0, this.unreadCount - 1)
      this.notifications.splice(idx, 1)
    }
  }
}
</script>

<style lang="scss" scoped>
.header-bar {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
}

.logo-container {
  height: auto;
  display: flex;
  align-items: center;

  :deep(.v-app-bar-title__content) {
    width: auto !important;
    overflow: visible;
    height: auto !important;
  }
}

.logo-image {
  height: 56px;
  width: auto;
  max-height: 56px;
  display: block;

  &:hover {
    opacity: 0.9;
  }
}

.navigation-links {
  @media (min-width: 960px) {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
  }
}

.nav-btn {
  text-transform: none !important;
  letter-spacing: 0 !important;
  font-weight: 500 !important;
  font-size: 0.9rem !important;
}
</style>
