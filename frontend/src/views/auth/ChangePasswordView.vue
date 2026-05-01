<template>
  <div class="change-password">
    <div class="change-password-container">
      <div class="change-password-header">
        <h2>Thay Đổi Mật Khẩu</h2>
      </div>

      <div class="change-password-form">
        <v-form @submit.prevent="handleSubmit" ref="form">
          <v-alert
            v-if="error"
            type="error"
            density="compact"
            class="mb-4"
            closable
          >
            {{ error }}
          </v-alert>

          <v-alert
            v-if="success"
            type="success"
            density="compact"
            class="mb-4"
            closable
          >
            {{ success }}
          </v-alert>

          <v-text-field
            v-model="currentPassword"
            label="Mật khẩu hiện tại"
            type="password"
            :rules="[rules.required]"
            variant="outlined"
            prepend-inner-icon="mdi-lock-outline"
            class="mb-2"
            autocomplete="current-password"
          ></v-text-field>

          <v-text-field
            v-model="newPassword"
            label="Mật khẩu mới"
            type="password"
            :rules="[rules.required, rules.minLength]"
            variant="outlined"
            prepend-inner-icon="mdi-lock-outline"
            class="mb-2"
            autocomplete="new-password"
            hint="Mật khẩu phải có ít nhất 8 ký tự"
            persistent-hint
          ></v-text-field>

          <v-text-field
            v-model="confirmPassword"
            label="Xác nhận mật khẩu mới"
            type="password"
            :rules="[rules.required, rules.passwordMatch]"
            variant="outlined"
            prepend-inner-icon="mdi-lock-outline"
            class="mb-2"
            autocomplete="new-password"
          ></v-text-field>

          <v-btn
            type="submit"
            block
            color="primary"
            class="mt-2"
            size="large"
            :loading="loading"
          >
            Xác nhận
          </v-btn>

          <div class="text-center mt-4">
            <v-btn
              :to="{ name: 'accountSettings' }"
              color="primary"
              variant="text"
              prepend-icon="mdi-arrow-left"
            >
              Back
            </v-btn>
          </div>
        </v-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const success = ref('')
const loading = ref(false)
const form = ref<any>(null)

const rules = {
  required: (v: string) => !!v || 'Trường này là bắt buộc',
  minLength: (v: string) => v.length >= 8 || 'Mật khẩu phải có ít nhất 8 ký tự',
  passwordMatch: (v: string) => v === newPassword.value || 'Mật khẩu không khớp'
}

const handleSubmit = async () => {
  // Check form validation
  const { valid } = await form.value.validate()

  if (!valid) {
    return
  }

  if (newPassword.value === currentPassword.value) {
    error.value = 'Mật khẩu mới không được trùng với mật khẩu hiện tại'
    return
  }

  error.value = ''
  success.value = ''
  loading.value = true

  try {
    const result = await authStore.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    })

    if (result) {
      success.value = 'Mật khẩu của bạn đã được thay đổi thành công'
      // Clear form
      currentPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''

      // Redirect to profile after 3 seconds
      setTimeout(() => {
        router.push({ name: 'profile' })
      }, 3000)
    } else if (authStore.error) {
      error.value = translateError(authStore.error)
    }
  } catch (err: any) {
    error.value = err.message || 'Đã xảy ra lỗi. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const translateError = (error: string): string => {
  const errorMap: Record<string, string> = {
    'Current password is incorrect': 'Mật khẩu hiện tại không chính xác',
    'User not found': 'Không tìm thấy người dùng',
    'Authentication required. Please log in again.': 'Yêu cầu xác thực. Vui lòng đăng nhập lại.',
    'An unexpected error occurred. Please try again.': 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.'
  }

  return errorMap[error] || error
}
</script>

<style scoped lang="scss">
.change-password {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
  padding: 1rem;
  background: #f5f5f5;

  &-container {
    max-width: 450px;
    width: 100%;
    background: white;
    border-radius: 12px;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
    padding: 2rem;
    transition: transform 0.3s ease, box-shadow 0.3s ease;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
    }
  }

  &-header {
    text-align: center;
    margin-bottom: 2rem;

    h1 {
      margin-bottom: 0.5rem;
      font-weight: 600;
      color: #333;
    }

    p {
      color: #666;
      font-size: 0.9rem;
      line-height: 1.4;
    }
  }
}

:deep(.v-btn) {
  text-transform: none;
  letter-spacing: 0;
  font-weight: 600;
}
</style>
