<template>
  <v-app>
    <app-header />

    <v-main>
      <v-container fluid>
        <suspense>
          <template #default>
            <RouterView />
          </template>
          <template #fallback>
            <div class="d-flex justify-center align-center" style="height: 60vh">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>
          </template>
        </suspense>
      </v-container>
    </v-main>

    <!-- Admin Quick Menu -->
    <admin-quick-menu v-if="isAuthenticated && isAdmin" />

    <!-- Study Reminder Toast -->
    <study-reminder-toast v-if="isAuthenticated" />
  </v-app>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import { computed, onErrorCaptured, ref } from 'vue'
import AppHeader from './components/common/Header.vue'
import StudyReminderToast from './components/common/StudyReminderToast.vue'
import AdminQuickMenu from './components/admin/AdminQuickMenu.vue'
import { useAuthStore } from './stores'

const authStore = useAuthStore()
const isAuthenticated = computed(() => authStore.isAuthenticated)
const isAdmin = computed(() => authStore.user?.roleId === 1)
const error = ref<Error | null>(null)

// Xử lý lỗi trong Suspense và NavigationDrawers
onErrorCaptured((e: Error) => {
  error.value = e
  return true // ngăn chặn lỗi lan truyền
})
</script>

<style lang="scss">
// Global styles
:root {
  --app-background: #fafafa;
}

body {
  background-color: var(--app-background);
  margin: 0;
  font-family: 'Roboto', sans-serif;
}

.v-application {
  background-color: var(--app-background) !important;
}

// Fix for navigation drawer slot issues
.v-navigation-drawer {
  &__scrim {
    z-index: 5;
  }
}
</style>
