<template>
  <v-container fluid class="password-reset fill-height">
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card class="auth-card mx-auto" elevation="8">
          <v-card-item class="text-center pt-8 pb-4">
            <div class="icon-container mb-4">
              <v-icon class="mail-icon" icon="mdi-email-outline"></v-icon>
            </div>
            <v-card-title class="text-h4 font-weight-bold mb-2">
              Đặt Lại Mật Khẩu
            </v-card-title>
            <v-card-subtitle>
              Nhập email của bạn để nhận hướng dẫn đặt lại
            </v-card-subtitle>
          </v-card-item>

          <v-card-text class="px-6">
            <v-alert
              v-if="isSubmitted"
              color="success"
              icon="mdi-check-circle"
              variant="tonal"
              class="mb-6"
            >
              Nếu tồn tại tài khoản với email đó, chúng tôi đã gửi hướng dẫn đặt
              lại mật khẩu. Vui lòng kiểm tra email của bạn.
            </v-alert>

            <v-form v-else @submit.prevent="handleSubmit">
              <v-text-field
                v-model="email"
                :error-messages="emailError"
                label="Địa Chỉ Email"
                type="email"
                variant="outlined"
                prepend-inner-icon="mdi-email"
                class="mb-4"
                required
                autocomplete="email"
              ></v-text-field>

              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                :loading="isLoading"
                class="auth-submit-btn mb-6"
              >
                <v-icon start>mdi-send</v-icon>
                Gửi Hướng Dẫn Đặt Lại
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
import { ref } from "vue";
import { useAuthStore } from "@/stores/modules/auth";

const authStore = useAuthStore();

const email = ref("");
const emailError = ref("");
const isLoading = ref(false);
const isSubmitted = ref(false);

const validateEmail = (): boolean => {
  emailError.value = "";

  if (!email.value) {
    emailError.value = "Email là bắt buộc";
    return false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    emailError.value = "Vui lòng nhập địa chỉ email hợp lệ";
    return false;
  }

  return true;
};

const handleSubmit = async () => {
  if (!validateEmail()) return;

  isLoading.value = true;

  try {
    const success = await authStore.requestPasswordReset(email.value);
    if (success) {
      isSubmitted.value = true;
    }
  } catch {
  } finally {
    isLoading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.password-reset {
  background-color: rgb(var(--v-theme-background));
  min-height: calc(100vh - 64px);
}

.auth-card {
  border-radius: 16px;
  overflow: hidden;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
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
  background: linear-gradient(
    45deg,
    rgb(var(--v-theme-primary)) 0%,
    rgba(var(--v-theme-primary), 0.8) 100%
  );

  .mail-icon {
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
