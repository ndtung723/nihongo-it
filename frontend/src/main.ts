import '@/assets/styles/main.sass'
import 'vuetify/styles'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import vuetify from './plugins/vuetify'
import App from './App.vue'
import router from './router'
import VueToast from 'vue-toast-notification'
import 'vue-toast-notification/dist/theme-sugar.css'
import { useAuthStore } from './stores'
import axios from 'axios'
import { handleSessionExpiration } from './utils/sessionHandler'
import vue3GoogleLogin from 'vue3-google-login'

// Configure axios defaults
axios.defaults.headers.common['Accept'] = 'application/json'
axios.defaults.baseURL = import.meta.env.VITE_API_URL || ''

// Add request interceptor to handle API URLs and authentication
axios.interceptors.request.use(
  config => {
    // For API requests, ensure they go to the backend server
    if (config.url?.startsWith('/api')) {
      config.baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    }

    // Add authorization token if available
    const token = localStorage.getItem('auth_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  error => Promise.reject(error)
)

// Add response interceptor to handle unauthorized errors
axios.interceptors.response.use(
  response => response,
  error => {
    // Handle 401 Unauthorized errors
    if (error.response && error.response.status === 401) {

      // Check if this is a TTS API request (which should not trigger logout)
      const isTtsRequest = error.config &&
        (error.config.url?.includes('/api/v1/tts/') ||
         error.config.headers['X-Content-Language'] === 'ja' ||
         error.config.url?.includes('/tts/'));

      if (isTtsRequest) {
        // Just return the error for TTS requests without logging out
        return Promise.reject(error);
      }

      // Clear auth token and user data
      localStorage.removeItem('auth_token');

      // Get Pinia store outside of Vue component
      const pinia = createPinia();
      const authStore = useAuthStore(pinia);
      authStore.logout();

      // Handle session expiration with notification and redirect
      const currentPath = window.location.pathname;
      handleSessionExpiration(currentPath);
    }
    return Promise.reject(error);
  }
)

const app = createApp(App)
const pinia = createPinia()

app.use(vuetify)
app.use(pinia)
app.use(router)
app.use(VueToast)
app.use(vue3GoogleLogin, {
  clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID || 'YOUR_GOOGLE_CLIENT_ID_HERE',
})

// Initialize auth state before mounting the app
const authStore = useAuthStore()
authStore.initializeAuth()

app.mount('#app')
