<template>
  <nav class="navbar">
    <div class="container">
      <div class="logo">
        <router-link :to="{ name: 'home' }">Nihongo IT</router-link>
      </div>

      <div class="nav-links">
        <router-link :to="{ name: 'home' }" class="nav-link">Home</router-link>
        <router-link :to="{ name: 'vocabulary' }" class="nav-link"
          >Vocabulary</router-link
        >
        <router-link :to="{ name: 'vocabularyLearning' }" class="nav-link"
          >Exercises</router-link
        >
        <router-link :to="{ name: 'conversationLearning' }" class="nav-link"
          >Conversation</router-link
        >
        <router-link :to="{ name: 'flashcardStudy' }" class="nav-link"
          >Flashcards</router-link
        >

        <!-- Authenticated-only links -->
        <template v-if="authStore.isAuthenticated">
          <router-link :to="{ name: 'statistics' }" class="nav-link"
            >My Learning Path</router-link
          >
          <router-link :to="{ name: 'statistics' }" class="nav-link"
            >My Progress</router-link
          >
        </template>
      </div>

      <div class="auth-buttons">
        <template v-if="authStore.isAuthenticated">
          <div class="user-menu" @click="toggleDropdown" ref="userMenu">
            <div class="user-avatar" v-if="authStore.user?.profilePicture">
              <img
                :src="authStore.user.profilePicture"
                alt="Profile"
                loading="lazy"
              />
            </div>
            <div class="user-avatar default-avatar" v-else>
              {{ avatarInitials }}
            </div>
            <span class="user-name">{{ authStore.user?.fullName }}</span>

            <div class="dropdown-menu" v-if="showDropdown">
              <router-link :to="{ name: 'profile' }" class="dropdown-item"
                >Profile</router-link
              >
              <div class="dropdown-item" @click="logout">Logout</div>
            </div>
          </div>
        </template>
        <template v-else>
          <router-link :to="{ name: 'login' }" class="btn-login"
            >Login</router-link
          >
          <router-link :to="{ name: 'register' }" class="btn-register"
            >Register</router-link
          >
        </template>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores";
import { useAppToast } from "@/composables/useAppToast";

const router = useRouter();
const authStore = useAuthStore();
const $toast = useAppToast();
const showDropdown = ref(false);
const userMenu = ref<HTMLElement | null>(null);

// Get initials for avatar
const avatarInitials = computed(() => {
  if (!authStore.user?.fullName) return "";

  return authStore.user.fullName
    .split(" ")
    .map((name) => name.charAt(0))
    .join("")
    .toUpperCase();
});

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value;
};

const handleClickOutside = (event: MouseEvent) => {
  if (userMenu.value && !userMenu.value.contains(event.target as Node)) {
    showDropdown.value = false;
  }
};

const logout = () => {
  authStore.logout();
  showDropdown.value = false;
  $toast.success("You have been logged out successfully");
  router.push({ name: "login" });
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside);
});
</script>

<style scoped>
.navbar {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0.75rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo a {
  font-size: 1.25rem;
  font-weight: bold;
  color: #4caf50;
  text-decoration: none;
}

.nav-links {
  display: flex;
  gap: 1.5rem;
}

.nav-link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-link:hover,
.nav-link.router-link-active {
  color: #4caf50;
}

.auth-buttons {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.btn-login,
.btn-register {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-login {
  color: #4caf50;
  border: 1px solid #4caf50;
}

.btn-login:hover {
  background-color: rgba(76, 175, 80, 0.1);
}

.btn-register {
  background-color: #4caf50;
  color: white;
}

.btn-register:hover {
  background-color: #388e3c;
}

.user-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 0.5rem;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  background-color: #4caf50;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 1rem;
  font-weight: bold;
}

.user-name {
  font-weight: 500;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  min-width: 150px;
  margin-top: 0.5rem;
  z-index: 10;
}

.dropdown-item {
  padding: 0.75rem 1rem;
  color: #333;
  text-decoration: none;
  display: block;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

@media (max-width: 768px) {
  .container {
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-links {
    margin: 1rem 0;
    flex-wrap: wrap;
  }

  .auth-buttons {
    align-self: flex-end;
  }
}
</style>
