<template>
  <v-container fluid class="reset-password fill-height">
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card class="auth-card mx-auto" elevation="8">
          <v-card-item class="text-center pt-8 pb-4">
            <div class="icon-container mb-4">
              <v-icon class="lock-icon" icon="mdi-lock-reset"></v-icon>
            </div>
            <v-card-title class="text-h4 font-weight-bold mb-2">
              Đặt Mật Khẩu Mới
            </v-card-title>
            <v-card-subtitle>
              Tạo mật khẩu mạnh cho tài khoản của bạn
            </v-card-subtitle>
          </v-card-item>

          <v-card-text class="px-6">
            <v-alert
              v-if="isSuccess"
              color="success"
              icon="mdi-check-circle"
              variant="tonal"
              class="mb-6"
            >
              Mật khẩu của bạn đã được đặt lại thành công. Bây giờ bạn có thể
              <router-link :to="{ name: 'login' }" class="font-weight-bold">
                đăng nhập
              </router-link>
              với mật khẩu mới.
            </v-alert>

            <v-form v-else @submit.prevent="handleSubmit">
              <v-alert
                v-if="authStore.error"
                type="error"
                density="compact"
                class="mb-4"
                closable
              >
                {{ translateError(authStore.error) }}
              </v-alert>

              <v-text-field
                v-model="password"
                :error-messages="passwordError"
                label="Mật Khẩu Mới"
                type="password"
                variant="outlined"
                prepend-inner-icon="mdi-lock"
                hint="Mật khẩu phải có ít nhất 8 ký tự"
                persistent-hint
                class="mb-4"
                required
                autocomplete="new-password"
              ></v-text-field>

              <v-text-field
                v-model="confirmPassword"
                :error-messages="confirmPasswordError"
                label="Xác Nhận Mật Khẩu"
                type="password"
                variant="outlined"
                prepend-inner-icon="mdi-lock-check"
                class="mb-6"
                required
                autocomplete="new-password"
              ></v-text-field>

              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                :loading="authStore.loading"
                class="auth-submit-btn mb-6"
              >
                <v-icon start>mdi-check-circle</v-icon>
                Đặt Lại Mật Khẩu
              </v-btn>

              <div class="text-center">
                <v-btn
                  :to="{ name: 'login' }"
                  color="primary"
                  variant="text"
                  prepend-icon="mdi-arrow-left"
                >
                  Quay Lại Đăng Nhập
                </v-btn>
              </div>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/modules/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const token = ref('');
const password = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const confirmPasswordError = ref('');
const isSuccess = ref(false);

onMounted(() => {
  // Get token from URL query parameters
  token.value = route.query.token?.toString() || '';

  if (!token.value) {
    authStore.error = 'Invalid or missing reset token. Please request a new password reset.';
  }
});

const translateError = (error: string): string => {
  const errorMap: Record<string, string> = {
    'Invalid or missing reset token. Please request a new password reset.': 'Mã token đặt lại không hợp lệ hoặc thiếu. Vui lòng yêu cầu đặt lại mật khẩu mới.',
    'Failed to reset password. Please try again.': 'Không thể đặt lại mật khẩu. Vui lòng thử lại.',
    'Reset token has expired. Please request a new password reset.': 'Mã token đặt lại đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu mới.',
    'Passwords do not match': 'Mật khẩu không khớp',
    'Password is required': 'Mật khẩu là bắt buộc',
    'Password must be at least 8 characters': 'Mật khẩu phải có ít nhất 8 ký tự'
  };

  return errorMap[error] || error;
};

const validateForm = (): boolean => {
  let isValid = true;
  passwordError.value = '';
  confirmPasswordError.value = '';

  if (!password.value) {
    passwordError.value = 'Mật khẩu là bắt buộc';
    isValid = false;
  } else if (password.value.length < 8) {
    passwordError.value = 'Mật khẩu phải có ít nhất 8 ký tự';
    isValid = false;
  }

  if (!confirmPassword.value) {
    confirmPasswordError.value = 'Vui lòng xác nhận mật khẩu của bạn';
    isValid = false;
  } else if (password.value !== confirmPassword.value) {
    confirmPasswordError.value = 'Mật khẩu không khớp';
    isValid = false;
  }

  return isValid;
};

const handleSubmit = async () => {
  if (!validateForm()) return;
  if (!token.value) {
    authStore.error = 'Invalid or missing reset token. Please request a new password reset.';
    return;
  }

  try {
    const success = await authStore.resetPassword(token.value, password.value);
    if (success) {
      isSuccess.value = true;
    }
  } catch (err: any) {
  }
};
</script>

<style lang="scss" scoped>
.reset-password {
  background-color: rgb(var(--v-theme-background));
  min-height: calc(100vh - 64px);
}

.auth-card {
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  max-width: 500px;

  &:hover {
    transform: translateY(-5px);
  }
}

.icon-container {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  background: linear-gradient(45deg, rgb(var(--v-theme-primary)) 0%, rgba(var(--v-theme-primary), 0.8) 100%);

  .lock-icon {
    font-size: 36px;
    color: white;
  }
}

.auth-submit-btn {
  border-radius: 8px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
  }
}
</style>
