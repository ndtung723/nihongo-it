<template>
  <v-container class="login-container" fluid>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <v-card class="login-card">
          <v-card-title class="text-center">
            <h1 class="title">Login</h1>
          </v-card-title>

          <v-card-text>
            <v-form ref="form" @submit.prevent="handleLogin">
              <v-text-field
                v-model="email"
                label="Email"
                type="email"
                prepend-icon="mdi-email"
                required
              ></v-text-field>

              <v-text-field
                v-model="password"
                label="Password"
                :type="showPassword ? 'text' : 'password'"
                prepend-icon="mdi-lock"
                :append-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
                @click:append="showPassword = !showPassword"
                required
              ></v-text-field>

              <div class="text-right mb-4">
                <router-link
                  :to="{ name: 'forgotPassword' }"
                  class="text-decoration-none forgot-password"
                >
                  Forgot Password?
                </router-link>
              </div>

              <v-btn
                type="submit"
                color="primary"
                block
                class="mt-4"
                :loading="loading"
              >
                Login
              </v-btn>

              <div class="text-center mt-4 mb-4">
                <span class="or-divider">OR</span>
              </div>

              <!-- Google Login Button -->
              <div class="google-login-container">
                <GoogleLogin :callback="handleGoogleLogin" />
              </div>

              <div class="text-center mt-4">
                <router-link
                  :to="{ name: 'register' }"
                  class="text-decoration-none"
                >
                  Don't have an account? Register
                </router-link>
              </div>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/stores";
import { useAppToast } from "@/composables/useAppToast";
import { GoogleLogin } from "vue3-google-login";

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const toast = useAppToast();

const form = ref<unknown>(null);
const email = ref("");
const password = ref("");
const showPassword = ref(false);

const loading = computed(() => authStore.loading);

onMounted(() => {
  if (authStore.isAuthenticated) {
    router.push({ name: "home" });
  } else {
    const errorMsg = route.query.error;
    if (errorMsg) {
      toast.error(String(errorMsg));
      if (window.history?.pushState) {
        const newUrl = new URL(window.location.href);
        newUrl.searchParams.delete("error");
        window.history.pushState(
          { path: newUrl.toString() },
          "",
          newUrl.toString(),
        );
      }
    }
  }
});

async function handleLogin() {
  if (!email.value || !password.value) {
    toast.error("Please enter email and password");
    return;
  }
  const success = await authStore.login({
    email: email.value,
    password: password.value,
  });
  handleAuthResult(success);
}

async function handleGoogleLogin(response: unknown) {
  const googleResponse = response as { credential?: string } | null;
  if (!googleResponse?.credential) {
    toast.error("Google login failed. Please try again.");
    return;
  }
  const success = await authStore.loginWithGoogle(googleResponse.credential);
  handleAuthResult(success);
}

function handleAuthResult(success: boolean) {
  if (success) {
    toast.success("Login successful!");
    const redirectPath = route.query.redirect
      ? String(route.query.redirect)
      : "/";
    router.push(redirectPath);
  } else {
    toast.error(authStore.error || "Login failed");
  }
}
</script>

<style lang="sass" scoped>
.login-container
  min-height: 100vh
  display: flex
  align-items: center

.login-card
  width: 100%
  padding: 1rem

.title
  color: #333
  margin-bottom: 1rem
  width: 100%

.forgot-password
  color: #666
  font-size: 0.9rem
  &:hover
    color: #1976d2

.or-divider
  position: relative
  display: inline-block
  color: #777
  font-size: 14px
  margin: 10px 0
  &:before, &:after
    content: ""
    position: absolute
    top: 50%
    width: 80px
    height: 1px
    background: #ddd
  &:before
    left: -90px
  &:after
    right: -90px

.google-login-container
  display: flex
  justify-content: center
  margin-bottom: 20px

::v-deep .v-text-field
  margin-bottom: 0.5rem
</style>
