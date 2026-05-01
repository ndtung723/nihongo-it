<template>
  <Transition name="toast-fade">
    <div v-if="show" class="study-reminder-toast" :class="{ 'hide': hiding }">
      <div class="toast-content" @click="navigateToStudy">
        <v-icon color="white" icon="mdi-book-open-variant" class="me-2" />
        <div class="toast-message">
          <div class="toast-title">Thẻ đến hạn ôn tập</div>
          <div class="toast-subtitle">{{ dueCardCount }} thẻ đang chờ bạn học!</div>
        </div>
      </div>
      <v-btn icon variant="text" density="compact" color="white" @click.stop="dismissToast">
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import flashcardService from '@/services/flashcard.service'

const router = useRouter()
const show = ref(false)
const hiding = ref(false)
const dueCardCount = ref(0)

// Use sessionStorage instead of localStorage to track when toast was dismissed
const TOAST_DISMISSED_KEY = 'study_reminder_dismissed_session'
// Generate session ID to identify the current browser session
const SESSION_ID_KEY = 'current_session_id'
// Check for due cards every 30 minutes
const CHECK_INTERVAL = 30 * 60 * 1000
let checkInterval: number | null = null

// Function to generate a random session ID
function generateSessionId() {
  return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
}

// Check or set session ID
function ensureSessionId() {
  if (!sessionStorage.getItem(SESSION_ID_KEY)) {
    sessionStorage.setItem(SESSION_ID_KEY, generateSessionId())
    // Clear dismissed status on new session
    sessionStorage.removeItem(TOAST_DISMISSED_KEY)
  }
}

// Function to check for due cards
async function checkDueCards() {
  try {
    const dueCards = await flashcardService.getDueCards()
    dueCardCount.value = dueCards.length

    // Only show toast if there are due cards and it hasn't been dismissed
    if (dueCardCount.value > 0 && !sessionStorage.getItem(TOAST_DISMISSED_KEY)) {
      show.value = true
    }
  } catch (error) {
  }
}

// Set up periodic checking
function setupPeriodicChecking() {
  // Clear any existing interval
  if (checkInterval) {
    clearInterval(checkInterval)
  }

  // Initial check
  checkDueCards()

  // Set interval for periodic checks
  checkInterval = window.setInterval(() => {
    // Only check if the toast isn't showing and isn't dismissed in this session
    if (!show.value && !sessionStorage.getItem(TOAST_DISMISSED_KEY)) {
      checkDueCards()
    }
  }, CHECK_INTERVAL)
}

// Document visibility change handling
function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    // User has returned to the tab/website
    checkDueCards()
  }
}

onMounted(() => {
  // Ensure we have a session ID
  ensureSessionId()

  // Setup periodic checking
  setupPeriodicChecking()

  // Setup visibility change listener to check when user returns to the tab
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  // Clean up on component unmount
  if (checkInterval) {
    clearInterval(checkInterval)
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

function dismissToast() {
  hiding.value = true

  // Save dismissal state for this session only
  sessionStorage.setItem(TOAST_DISMISSED_KEY, 'true')

  // Animate out before removing
  setTimeout(() => {
    show.value = false
    hiding.value = false
  }, 300)
}

function navigateToStudy() {
  router.push({ name: 'flashcardStudy' })
  dismissToast()
}
</script>

<style scoped>
.study-reminder-toast {
  position: fixed;
  bottom: 88px;
  right: 20px;
  z-index: 10000;
  background-color: #4caf50;
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 300px;
  max-width: 400px;
  cursor: pointer;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.study-reminder-toast.hide {
  transform: translateX(420px);
  opacity: 0;
}

.toast-content {
  display: flex;
  align-items: center;
  flex: 1;
}

.toast-message {
  margin-left: 4px;
}

.toast-title {
  font-weight: 600;
  font-size: 1rem;
}

.toast-subtitle {
  font-size: 0.875rem;
  opacity: 0.9;
}

.toast-fade-enter-active, .toast-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.toast-fade-enter-from, .toast-fade-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
