<template>
  <v-app>
    <app-header />

    <v-main>
      <v-container fluid class="main-container">
        <suspense>
          <template #default>
            <RouterView />
          </template>
          <template #fallback>
            <div
              class="d-flex justify-center align-center"
              style="height: 60vh"
            >
              <v-progress-circular
                indeterminate
                color="primary"
              ></v-progress-circular>
            </div>
          </template>
        </suspense>
      </v-container>
    </v-main>

    <!-- Footer -->
    <app-footer />

    <!-- Admin Quick Menu -->
    <admin-quick-menu v-if="isAuthenticated && isAdmin" />

    <!-- Study Reminder Toast -->
    <study-reminder-toast v-if="isAuthenticated" />

    <!-- Global confirmation dialog -->
    <confirm-dialog />
  </v-app>
</template>

<script setup lang="ts">
import { RouterView } from "vue-router";
import { computed, onErrorCaptured, ref } from "vue";
import AppHeader from "./components/common/Header.vue";
import AppFooter from "./components/common/Footer.vue";
import StudyReminderToast from "./components/common/StudyReminderToast.vue";
import AdminQuickMenu from "./components/admin/AdminQuickMenu.vue";
import ConfirmDialog from "./components/common/ConfirmDialog.vue";
import { useAuthStore } from "./stores";
import { ROLES } from "./types/roles";

const authStore = useAuthStore();
const isAuthenticated = computed(() => authStore.isAuthenticated);
const isAdmin = computed(() => authStore.user?.roleId === ROLES.ADMIN);
const error = ref<Error | null>(null);

// Xử lý lỗi trong Suspense và NavigationDrawers
onErrorCaptured((e: Error) => {
  error.value = e;
  return true; // ngăn chặn lỗi lan truyền
});
</script>

<style lang="scss">
// Global styles
:root {
  --app-background: #fafafa;
}

html,
body {
  height: 100%;
}

body {
  background-color: var(--app-background);
  margin: 0;
  font-family: "Roboto", sans-serif;
}

.v-application {
  background-color: var(--app-background) !important;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.v-main {
  flex: 1;
}

.main-container {
  min-height: calc(
    100vh - 200px
  ); /* Adjust based on header and footer height */
  padding-bottom: 2rem;
}

// Fix for navigation drawer slot issues
.v-navigation-drawer {
  &__scrim {
    z-index: 5;
  }
}
</style>
