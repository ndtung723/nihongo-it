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
      <!-- Notification Button -->
      <v-btn icon color="blue" variant="text" size="small" class="mr-1 d-none d-sm-flex">
        <v-icon>mdi-bell</v-icon>
      </v-btn>

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
import { Component, Vue, Prop } from 'vue-facing-decorator'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'

@Component({
  name: 'AppHeader'
})
export default class AppHeader extends Vue {
  private authStore = useAuthStore()
  private mobileMenuOpen = false

  get isLoggedIn(): boolean {
    return !!this.authStore.user
  }

  get username(): string {
    return this.authStore.user?.fullName || 'Guest'
  }

  get userLevel(): string {
    return this.authStore.user?.currentLevel || 'N5'
  }

  get userProfilePicture(): string {
    return this.authStore.user?.profilePicture || ''
  }

  get avatarInitials(): string {
    return this.username.charAt(0).toUpperCase()
  }

  get isAdmin(): boolean {
    return this.authStore.user?.roleId === 1
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  logout(): void {
    this.authStore.logout()
    const router = useRouter()
    router.push({ name: 'login' })
  }

  // Đảm bảo giải phóng tham chiếu khi component bị hủy
  beforeUnmount() {
    this.mobileMenuOpen = false;
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
