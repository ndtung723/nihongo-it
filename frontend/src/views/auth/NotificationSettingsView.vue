<template>
  <div class="notification-settings-container">
    <div class="header-container d-flex align-center">
      <v-btn icon @click="$router.back()" class="back-btn" size="small">
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>
      <h1 class="text-subtitle-1 font-weight-bold text-center flex-grow-1">Cài đặt thông báo</h1>
    </div>

    <!-- All settings in one card -->
    <v-card class="settings-card mt-2">
      <!-- General Notifications -->
      <v-card-item class="py-2">
        <template v-slot:prepend>
          <v-icon color="primary" size="small">mdi-bell-outline</v-icon>
        </template>
        <v-card-title class="text-subtitle-2 px-0">Thông báo chung</v-card-title>
      </v-card-item>

      <v-card-text class="py-0">
        <div class="d-flex justify-space-between align-center option-row">
          <div class="d-flex align-center">
            <v-icon color="primary" size="small" class="mr-2">mdi-bell</v-icon>
            <span class="text-body-2">Thông báo từ hệ thống</span>
          </div>
          <v-switch
            v-model="preferences.system"
            color="primary"
            hide-details
            density="compact"
            inset
            class="mini-switch"
          ></v-switch>
        </div>

        <div class="d-flex justify-space-between align-center option-row">
          <div class="d-flex align-center">
            <v-icon color="primary" size="small" class="mr-2">mdi-bell-ring</v-icon>
            <span class="text-body-2">Thông báo mục tiêu hàng ngày</span>
          </div>
          <v-switch
            v-model="preferences.dailyGoal"
            color="primary"
            hide-details
            density="compact"
            inset
            class="mini-switch"
          ></v-switch>
        </div>
      </v-card-text>

      <v-divider class="mx-3"></v-divider>

      <!-- Flashcard Reminders -->
      <v-card-item class="py-2">
        <template v-slot:prepend>
          <v-icon color="primary" size="small">mdi-card-text-outline</v-icon>
        </template>
        <v-card-title class="text-subtitle-2 px-0">Nhắc nhở học thẻ ghi nhớ</v-card-title>
      </v-card-item>

      <v-card-text class="py-0">
        <div class="d-flex justify-space-between align-center option-row">
          <div>
            <div class="d-flex align-center">
              <v-icon color="primary" size="small" class="mr-2">mdi-cards</v-icon>
              <span class="text-body-2">Bật nhắc nhở ôn tập</span>
            </div>
            <div class="text-caption text-medium-emphasis ml-8">Nhận thông báo khi có thẻ cần ôn tập</div>
          </div>
          <v-switch
            v-model="preferences.dueCards"
            color="primary"
            hide-details
            density="compact"
            inset
            class="mini-switch"
          ></v-switch>
        </div>

        <!-- Time selector -->
        <div class="option-row ml-8" :class="{ 'option-disabled': !preferences.dueCards }">
          <div class="text-body-2 mb-1">Thời gian nhắc nhở</div>
          <div class="d-flex align-center time-selector">
            <v-select
              v-model="timeHour"
              :items="hours"
              variant="outlined"
              density="compact"
              class="time-select mr-1"
              hide-details
              bg-color="white"
              :disabled="!preferences.dueCards"
              @update:model-value="updateReminderTime"
            ></v-select>
            <span class="mx-1 text-body-2">:</span>
            <v-select
              v-model="timeMinute"
              :items="minutes"
              variant="outlined"
              density="compact"
              class="time-select ml-1"
              hide-details
              bg-color="white"
              :disabled="!preferences.dueCards"
              @update:model-value="updateReminderTime"
            ></v-select>
          </div>
          <div class="text-caption text-medium-emphasis mt-1">
            Thông báo hàng ngày vào {{ timeHour }}:{{ timeMinute }}
          </div>
        </div>

        <!-- Card threshold -->
        <div class="option-row ml-8 mt-3" :class="{ 'option-disabled': !preferences.dueCards }">
          <div class="text-body-2 mb-1">Số lượng thẻ tối thiểu</div>
          <div class="d-flex align-center justify-space-between">
            <div class="text-caption text-medium-emphasis flex-shrink-1 mr-2">Chỉ thông báo khi có trên {{ minCardThreshold }} thẻ</div>
            <v-slider
              v-model="minCardThreshold"
              :min="1"
              :max="20"
              :step="1"
              :ticks="[1, 5, 10, 15, 20]"
              show-ticks="always"
              thumb-label
              :disabled="!preferences.dueCards"
              density="compact"
              hide-details
              class="w-50"
            ></v-slider>
          </div>
        </div>
      </v-card-text>

      <v-divider class="mx-3"></v-divider>

      <!-- Notification Channels -->
      <v-card-item class="py-2">
        <template v-slot:prepend>
          <v-icon color="primary" size="small">mdi-message-settings-outline</v-icon>
        </template>
        <v-card-title class="text-subtitle-2 px-0">Kênh thông báo</v-card-title>
      </v-card-item>

      <v-card-text class="py-0">
        <div class="d-flex justify-space-between align-center option-row">
          <div class="d-flex align-center">
            <v-icon color="primary" size="small" class="mr-2">mdi-bell</v-icon>
            <span class="text-body-2">Thông báo trong ứng dụng</span>
          </div>
          <v-switch
            v-model="channels.app"
            color="primary"
            hide-details
            density="compact"
            inset
            class="mini-switch"
          ></v-switch>
        </div>

        <div class="d-flex justify-space-between align-center option-row">
          <div class="d-flex align-center">
            <v-icon color="primary" size="small" class="mr-2">mdi-email</v-icon>
            <span class="text-body-2">Email</span>
          </div>
          <v-switch
            v-model="channels.email"
            color="primary"
            hide-details
            density="compact"
            inset
            class="mini-switch"
          ></v-switch>
        </div>
      </v-card-text>
    </v-card>

    <div class="d-flex justify-center my-4">
      <v-btn
        color="primary"
        class="px-6"
        @click="savePreferences"
        :loading="loading"
        size="small"
        rounded
      >
        <v-icon size="small" class="mr-1">mdi-content-save</v-icon>
        Lưu thay đổi
      </v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useToast } from 'vue-toast-notification'
import userService from '@/services/user.service'
import authService from '@/services/auth.service'

const toast = useToast()
const loading = ref(false)

// Reminder settings
const preferences = ref({
  system: true,
  dailyGoal: true,
  dueCards: true,
  leechCards: true
})

// Notification channels
const channels = ref({
  app: true,
  email: true
})

// Time picker for reminder time
const reminderTime = ref('20:00')

// Minimum card threshold
const minCardThreshold = ref(5)

// Time selector components
const timeHour = ref('20')
const timeMinute = ref('00')
const hours = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, '0'))
const minutes = Array.from({ length: 60 }, (_, i) => i.toString().padStart(2, '0'))

// Load user preferences when component mounts
onMounted(async () => {
  await loadUserPreferences()
})

async function loadUserPreferences() {
  try {
    loading.value = true

    // Get preferences from the backend using userService
    const response = await userService.getUserPreferences()

    // If preferences exist, set them
    if (response) {
      if (response.notificationPreferences) {
        const notifPrefs = response.notificationPreferences.split(',').map((p: string) => p.trim())
        channels.value.app = notifPrefs.includes('app')
        channels.value.email = notifPrefs.includes('email')
      }

      if (response.reminderEnabled !== undefined) {
        preferences.value.dueCards = response.reminderEnabled
      }

      if (response.reminderTime) {
        reminderTime.value = response.reminderTime

        // Parse the time values for the selectors
        const [hour, minute] = response.reminderTime.split(':')
        timeHour.value = hour.padStart(2, '0')
        timeMinute.value = minute.padStart(2, '0')
      }

      if (response.minCardThreshold !== undefined) {
        minCardThreshold.value = response.minCardThreshold
      }

      if (response.leechNotificationsEnabled !== undefined) {
        preferences.value.leechCards = response.leechNotificationsEnabled
      }
    }
  } catch (error) {
    toast.error('Không thể tải thiết lập thông báo', {
      position: 'top'
    })
  } finally {
    loading.value = false
  }
}

async function savePreferences() {
  try {
    loading.value = true

    // Construct notification preferences string
    const notificationPrefsArray = []
    if (channels.value.app) notificationPrefsArray.push('app')
    if (channels.value.email) notificationPrefsArray.push('email')

    const userData = {
      notificationPreferences: notificationPrefsArray.join(','),
      reminderEnabled: preferences.value.dueCards,
      reminderTime: reminderTime.value,
      minCardThreshold: minCardThreshold.value,
      leechNotificationsEnabled: preferences.value.leechCards
    }

    // Send to the backend using userService
    await userService.updateUserPreferences(userData)

    toast.success('Cài đặt thông báo đã được lưu', {
      position: 'top'
    })
  } catch (error) {
    toast.error('Không thể lưu thiết lập thông báo', {
      position: 'top'
    })
  } finally {
    loading.value = false
  }
}

function updateReminderTime() {
  reminderTime.value = `${timeHour.value}:${timeMinute.value}`
}
</script>

<style scoped>
.notification-settings-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 8px;
}

.header-container {
  margin-bottom: 12px;
}

.back-btn {
  margin-right: 8px;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 50%;
}

.settings-card {
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05) !important;
}

.option-row {
  padding: 6px 0;
}

.option-disabled {
  opacity: 0.7;
}

.time-selector {
  max-width: 200px;
}

.time-select {
  width: 80px;
}

.mini-switch {
  margin-top: 0;
  margin-bottom: 0;
}

:deep(.mini-switch .v-switch__track) {
  opacity: 0.5;
  transform: scale(0.75);
}

:deep(.mini-switch .v-switch__thumb) {
  transform: scale(0.75);
}

:deep(.v-slider .v-slider-track__fill) {
  background-color: var(--v-theme-primary);
}
</style>
