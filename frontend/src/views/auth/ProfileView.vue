<template>
  <v-container class="profile-container">
    <v-row justify="center" align="center">
      <v-col cols="12" sm="10" md="8" lg="6">
        <v-card class="profile-card">
          <v-card-title class="text-center">
            <h2 class="title">Hồ Sơ Của Tôi</h2>
          </v-card-title>

          <v-card-text>
            <v-progress-circular
              v-if="loading"
              indeterminate
              color="primary"
              class="mx-auto d-block my-6"
            ></v-progress-circular>

            <template v-else-if="user">
              <div class="profile-header">
                <v-avatar size="80" class="mr-4">
                  <v-img
                    v-if="user.profilePicture"
                    :src="user.profilePicture"
                    alt="Ảnh đại diện"
                  ></v-img>
                  <span v-else class="text-h4 white--text">{{ avatarInitials }}</span>
                </v-avatar>
                <div class="profile-name">
                  <h2 class="text-h5 mb-1">{{ user.fullName }}</h2>
                  <p class="email text-subtitle-1 text-medium-emphasis">{{ user.email }}</p>
                </div>
              </div>

              <v-divider class="my-4"></v-divider>

              <v-list>
                <v-list-item>
                  <v-list-item-title class="detail-label">Trình Độ Hiện Tại:</v-list-item-title>
                  <v-list-item-subtitle class="detail-value">{{ user.currentLevel || 'Chưa thiết lập' }}</v-list-item-subtitle>
                </v-list-item>

                <v-list-item>
                  <v-list-item-title class="detail-label">Mục Tiêu JLPT:</v-list-item-title>
                  <v-list-item-subtitle class="detail-value">{{ user.jlptGoal || 'Chưa thiết lập' }}</v-list-item-subtitle>
                </v-list-item>

                <v-list-item>
                  <v-list-item-title class="detail-label">Lần đăng nhập cuối:</v-list-item-title>
                  <v-list-item-subtitle class="detail-value">{{ formatDate(user.lastLogin) }}</v-list-item-subtitle>
                </v-list-item>
              </v-list>
            </template>

            <v-alert
              v-else
              type="error"
              class="my-4"
            >
              Không thể tải thông tin hồ sơ. Vui lòng thử lại sau.
              <v-btn
                color="error"
                text
                @click="fetchProfile"
                class="mt-2"
              >
                Thử Lại
              </v-btn>
            </v-alert>
          </v-card-text>

          <v-card-actions v-if="user" class="justify-space-between">
            <v-btn
              color="secondary"
              variant="outlined"
              @click="navigateToSettings"
              class="px-6"
            >
              Cài Đặt
            </v-btn>
            <v-btn
              color="primary"
              class="px-6"
              @click="showUpdateDialog = true"
            >
              Cập Nhật
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <!-- Update Profile Dialog -->
    <v-dialog v-model="showUpdateDialog" max-width="500">
      <v-card>
        <v-card-title class="text-center">
          <h3 class="text-h5">Cập Nhật Hồ Sơ</h3>
        </v-card-title>
        <v-card-text>
          <v-form @submit.prevent="updateProfile" ref="form">
            <v-text-field
              v-model="profileForm.fullName"
              label="Họ và tên"
              variant="outlined"
              :rules="[v => !!v || 'Vui lòng nhập họ tên', v => v.length >= 2 || 'Họ tên phải có ít nhất 2 ký tự']"
              required
              class="mb-3"
            ></v-text-field>

            <!-- Updated selects with cross-field validation -->
            <v-select
              v-model="profileForm.currentLevel"
              label="Trình Độ Hiện Tại"
              :items="jlptLevels"
              variant="outlined"
              :rules="[
                v => !!v || 'Vui lòng chọn trình độ hiện tại',
                () => isValidJlptProgression() || 'Trình độ hiện tại không thể cao hơn mục tiêu JLPT'
              ]"
              required
              class="mb-3"
              @update:model-value="validateForm"
            ></v-select>

            <v-select
              v-model="profileForm.jlptGoal"
              label="Mục Tiêu JLPT"
              :items="jlptLevels"
              variant="outlined"
              :rules="[
                v => !!v || 'Vui lòng chọn mục tiêu JLPT',
                () => isValidJlptProgression() || 'Trình độ hiện tại không thể cao hơn mục tiêu JLPT'
              ]"
              required
              class="mb-3"
              @update:model-value="validateForm"
            ></v-select>
          </v-form>
        </v-card-text>
        <v-card-actions class="px-4 pb-4">
          <v-btn
            color="grey-darken-1"
            variant="text"
            @click="closeUpdateDialog"
            class="px-4"
          >
            Hủy
          </v-btn>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            :loading="updating"
            @click="updateProfile"
            class="px-4"
          >
            Lưu Thay Đổi
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores'
import type { UserInfo } from '@/services/auth.service'
import { useToast } from 'vue-toast-notification'
import authService from '@/services/auth.service'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const form = ref(null)

const user = computed<UserInfo | null>(() => {
  return authStore.user
})

const loading = computed<boolean>(() => {
  return authStore.loading
})

const avatarInitials = computed<string>(() => {
  if (!user.value?.fullName) return ''

  return user.value.fullName
    .split(' ')
    .map(name => name.charAt(0))
    .join('')
    .toUpperCase()
})

// Update profile related
const showUpdateDialog = ref<boolean>(false)
const updating = ref<boolean>(false)
const jlptLevels = ref<string[]>(['N1', 'N2', 'N3', 'N4', 'N5'])

// Add JLPT level comparison function
const getJlptLevelValue = (level: string): number => {
  // Convert JLPT levels to numeric values for comparison
  // N5(5) < N4(4) < N3(3) < N2(2) < N1(1) - smaller value is higher level
  const match = level.match(/N(\d)/)
  return match ? parseInt(match[1]) : 5
}

// Check if currentLevel is less than or equal to jlptGoal
const isValidJlptProgression = (): boolean => {
  if (!profileForm.value.currentLevel || !profileForm.value.jlptGoal) return true

  const currentValue = getJlptLevelValue(profileForm.value.currentLevel)
  const goalValue = getJlptLevelValue(profileForm.value.jlptGoal)

  // Current level should be less than or equal to goal level
  // Since N5(5) < N4(4) < N3(3) < N2(2) < N1(1)
  // Current N5(5), Goal N3(3): currentValue(5) > goalValue(3) - INVALID
  // Current N3(3), Goal N5(5): currentValue(3) < goalValue(5) - VALID
  // Current N3(3), Goal N1(1): currentValue(3) > goalValue(1) - INVALID
  return currentValue >= goalValue // Higher numerical value = lower JLPT level
}

const validateJlptLevels = (): string | true => {
  if (!isValidJlptProgression()) {
    return 'Mục tiêu JLPT phải cao hơn hoặc bằng trình độ hiện tại (N5 < N4 < N3 < N2 < N1)'
  }
  return true
}

const profileForm = ref({
  fullName: '',
  currentLevel: 'N5' as string,
  jlptGoal: 'N5' as string
})

const formatDate = (dateString?: string): string => {
  if (!dateString) return 'Chưa bao giờ'

  try {
    const date = new Date(dateString)
    const options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }

    return date.toLocaleDateString('vi-VN', options)
  } catch (e) {
    return dateString
  }
}

const fetchProfile = async (): Promise<void> => {
  try {
    await authStore.fetchCurrentUser()
  } catch (error) {
    toast.error('Không thể tải thông tin hồ sơ')
  }
}

const navigateToSettings = (): void => {
  router.push({ name: 'accountSettings' })
}

const closeUpdateDialog = (): void => {
  showUpdateDialog.value = false
  // Reset form to current user values
  if (user.value) {
    profileForm.value = {
      fullName: user.value.fullName || '',
      currentLevel: user.value.currentLevel || 'N5',
      jlptGoal: user.value.jlptGoal || 'N5'
    }
  }
}

const updateProfile = async (): Promise<void> => {
  // Validate form
  if (form.value && typeof (form.value as any).validate === 'function') {
    const { valid } = await (form.value as any).validate()
    if (!valid) return
  }

  updating.value = true
  try {
    // Call API to update profile
    const response = await authService.updateProfile({
      fullName: profileForm.value.fullName,
      currentLevel: profileForm.value.currentLevel || 'N5',
      jlptGoal: profileForm.value.jlptGoal || 'N5'
    })

    if (response.status === 'OK') {
      toast.success('Hồ sơ đã được cập nhật thành công', {
        position: 'top',
        duration: 3000
      })

      // Refresh user data
      await authStore.fetchCurrentUser()

      // Close dialog
      showUpdateDialog.value = false
    } else {
      toast.error('Cập nhật hồ sơ thất bại', {
        position: 'top',
        duration: 3000
      })
    }
  } catch (error) {
    toast.error('Không thể cập nhật hồ sơ. Vui lòng thử lại sau.', {
      position: 'top',
      duration: 3000
    })
  } finally {
    updating.value = false
  }
}

// Add validateForm function
const validateForm = (): void => {
  if (form.value && typeof (form.value as any).validate === 'function') {
    (form.value as any).validate()
  }
}

onMounted((): void => {
  if (!user.value) {
    fetchProfile()
  } else {
    // Initialize form with current user data
    profileForm.value = {
      fullName: user.value.fullName || '',
      currentLevel: user.value.currentLevel || 'N5',
      jlptGoal: user.value.jlptGoal || 'N5'
    }
  }
})
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 80vh;
  display: flex;
  align-items: center;
  padding: 2rem 0;
}

.profile-card {
  width: 100%;
  padding: 1rem;
}

.title {
  color: rgba(0, 0, 0, 0.8);
  margin-bottom: 1rem;
  text-align: center;
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.profile-name {
  flex: 1;
}

.detail-label {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.7);
}

.detail-value {
  color: rgba(0, 0, 0, 0.6);
}

:deep(.v-avatar) {
  background-color: rgb(var(--v-theme-primary));
}
</style>
