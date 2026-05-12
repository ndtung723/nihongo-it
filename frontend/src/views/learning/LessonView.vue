<template>
  <div class="lesson-view-container">
    <!-- Status Bar -->
    <div class="status-bar d-flex justify-space-between align-center px-4 py-2">
      <div class="time-display">{{ currentTime }}</div>
      <div class="status-icons">
        <span class="data-rate text-caption me-2">{{ dataRate }}</span>
        <v-icon small class="mr-1">mdi-bluetooth</v-icon>
        <v-icon small class="mr-1">mdi-signal-cellular-2</v-icon>
        <v-icon small class="mr-1">mdi-wifi</v-icon>
        <span class="battery-status">
          <v-icon small>mdi-battery-60</v-icon>
          <span class="text-caption">68</span>
        </span>
      </div>
    </div>

    <!-- Header with Back Button -->
    <div class="header-container d-flex align-center px-4 py-3">
      <v-btn icon class="mr-2" @click="goBack">
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>
      <h1 class="text-h6 font-weight-bold">Lesson Content</h1>
    </div>

    <!-- Placeholder Content -->
    <div class="px-4 py-8 text-center">
      <v-icon size="64" color="primary" class="mb-4">mdi-school</v-icon>
      <h2 class="text-h5 font-weight-bold mb-2">Individual Lesson View</h2>
      <p class="text-subtitle-1 text-medium-emphasis">
        Lesson ID: {{ lessonId }}
      </p>
      <p class="text-body-1 mt-4">
        This lesson content will be implemented in the future.
      </p>
      <v-btn color="primary" class="mt-6" @click="goBack">Go Back</v-btn>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter, useRoute } from "vue-router";

const router = useRouter();
const route = useRoute();
const currentTime = ref("21:21");
const dataRate = ref("0.16 KB/s");

// Computed
const lessonId = computed(() => route.params.lessonId as string);

// Methods
function updateTime() {
  const date = new Date();
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  currentTime.value = `${hours}:${minutes}`;
}

function goBack() {
  router.back();
}

// Lifecycle hooks
onMounted(() => {
  updateTime();

  // Update time every minute
  setInterval(updateTime, 60000);
});
</script>

<style scoped lang="scss">
.lesson-view-container {
  background-color: #f8f9fa;
  min-height: 100vh;
}

.status-bar {
  margin-bottom: 4px;
}

.time-display {
  font-weight: 500;
}

.data-rate {
  font-size: 0.7rem;
}

.status-icons {
  display: flex;
  align-items: center;
}

.battery-status {
  display: flex;
  align-items: center;
}
</style>
