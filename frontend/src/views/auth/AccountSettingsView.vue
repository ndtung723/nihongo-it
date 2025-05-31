<template>
  <div class="account-settings-container">
    <div class="header-container d-flex align-center">
      <v-btn icon @click="$router.back()" class="back-btn">
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>
      <h1 class="text-h6 text-center flex-grow-1">Cài đặt tài khoản</h1>
    </div>

    <div class="settings-options">
      <!-- Update Profile -->
      <v-card variant="outlined" class="setting-card mb-3" @click="navigateTo('/account/profile')">
        <div class="d-flex align-center pa-4">
          <v-avatar color="primary" class="setting-icon">
            <v-icon color="white">mdi-account</v-icon>
          </v-avatar>
          <span class="text-subtitle-1 ml-4">Thông tin tài khoản</span>
          <v-spacer></v-spacer>
          <v-icon>mdi-chevron-right</v-icon>
        </div>
      </v-card>

      <!-- Change Password -->
      <v-card variant="outlined" class="setting-card mb-3" @click="navigateTo('/account/change-password')">
        <div class="d-flex align-center pa-4">
          <v-avatar color="primary" class="setting-icon">
            <v-icon color="white">mdi-key</v-icon>
          </v-avatar>
          <span class="text-subtitle-1 ml-4">Đổi mật khẩu</span>
          <v-spacer></v-spacer>
          <v-icon>mdi-chevron-right</v-icon>
        </div>
      </v-card>


      <!-- Feedback -->
      <v-card variant="outlined" class="setting-card mb-3" @click="navigateTo('/feedback')">
        <div class="d-flex align-center pa-4">
          <v-avatar color="primary" class="setting-icon">
            <v-icon color="white">mdi-message-text</v-icon>
          </v-avatar>
          <span class="text-subtitle-1 ml-4">Góp ý</span>
          <v-spacer></v-spacer>
          <v-icon>mdi-chevron-right</v-icon>
        </div>
      </v-card>

      <!-- Notifications -->
      <v-card variant="outlined" class="setting-card mb-3" @click="navigateTo('/account/notifications')">
        <div class="d-flex align-center pa-4">
          <v-avatar color="primary" class="setting-icon">
            <v-icon color="white">mdi-bell</v-icon>
          </v-avatar>
          <span class="text-subtitle-1 ml-4">Thông báo</span>
          <v-spacer></v-spacer>
        </div>
      </v-card>

    </div>

    <!-- App Version -->
    <div class="text-center text-caption text-medium-emphasis mb-4">
      Phiên bản ứng dụng {{ appVersion }}
    </div>

    <!-- Logout Button -->
    <div class="logout-container">
      <v-btn block color="error" class="logout-btn" @click="logout">
        Đăng xuất
      </v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'
import { useToast } from 'vue-toast-notification'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const notificationsEnabled = ref(true)
const appVersion = ref('1.0.0')

function navigateTo(path: string) {
  router.push(path)
}

async function logout() {
  try {
    await authStore.logout()
    toast.success('Đã đăng xuất thành công', {
      position: 'top',
      duration: 2000
    })
    router.push({ name: 'login' })
  } catch (error) {
    toast.error('Có lỗi xảy ra khi đăng xuất', {
      position: 'top',
      duration: 3000
    })
  }
}
</script>

<style scoped lang="scss">
.account-settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 16px;
  padding-bottom: 64px;
}

.header-container {
  position: sticky;
  top: 0;
  z-index: 10;
  height: 56px;
  background-color: white;
  margin-bottom: 16px;
}

.setting-card {
  border-radius: 12px;
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}

.setting-icon {
  width: 40px;
  height: 40px;
}

.logout-container {
  margin-top: 20px;
}

.logout-btn {
  border-radius: 50px;
  height: 48px;
  font-weight: 600;
}
</style>
