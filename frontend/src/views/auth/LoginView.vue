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
                <router-link :to="{ name: 'forgotPassword' }" class="text-decoration-none forgot-password">
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
                <router-link :to="{ name: 'register' }" class="text-decoration-none">
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

<script lang="ts">
import { Component, Vue, Ref } from 'vue-facing-decorator'
import { useAuthStore } from '@/stores'
import { useToast } from 'vue-toast-notification'
import 'vue-toast-notification/dist/theme-sugar.css'
import { GoogleLogin } from 'vue3-google-login'

@Component({
  name: 'LoginView',
  components: {
    GoogleLogin
  }
})
export default class LoginView extends Vue {
  private authStore = useAuthStore()
  private getToast() {
    return useToast()
  }

  @Ref('form') readonly form!: any

  email = ''
  password = ''
  showPassword = false

  get loading(): boolean {
    return this.authStore.loading
  }

  mounted(): void {
    if (this.authStore.isAuthenticated) {
      this.$router.push({ name: 'home' })
    } else {
      // Check for error message in query params
      const errorMsg = this.$route.query.error
      if (errorMsg) {
        const toast = this.getToast()
        toast.error(String(errorMsg), {
          position: 'top',
          duration: 5000
        })

        // Remove the error parameter from URL to prevent showing the message again on refresh
        if (window.history && window.history.pushState) {
          const newUrl = new URL(window.location.href)
          newUrl.searchParams.delete('error')
          window.history.pushState({ path: newUrl.toString() }, '', newUrl.toString())
        }
      }
    }
  }

  async handleLogin(): Promise<void> {
    const toast = this.getToast()

    if (!this.email || !this.password) {
      toast.error('Please enter email and password', {
        position: 'top',
        duration: 3000
      })
      return
    }

    const success = await this.authStore.login({
      email: this.email,
      password: this.password
    })

    this.handleAuthResult(success)
  }

  async handleGoogleLogin(response: any): Promise<void> {
    if (!response || !response.credential) {
      const toast = this.getToast()
      toast.error('Google login failed. Please try again.', {
        position: 'top',
        duration: 3000
      })
      return
    }

    const success = await this.authStore.loginWithGoogle(response.credential)

    this.handleAuthResult(success)
  }

  handleAuthResult(success: boolean): void {
    const toast = this.getToast()

    if (success) {
      toast.success('Login successful!', {
        position: 'top',
        duration: 3000
      })

      // Redirect to the requested page or dashboard
      const redirectPath = this.$route.query.redirect
        ? String(this.$route.query.redirect)
        : '/'
      this.$router.push(redirectPath)
    } else {
      toast.error(this.authStore.error || 'Login failed', {
        position: 'top',
        duration: 3000
      })
    }
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
