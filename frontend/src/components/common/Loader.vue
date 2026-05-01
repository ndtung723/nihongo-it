<template>
  <div class="loader-container" :class="{ fullscreen: fullscreen }">
    <v-progress-circular
      :size="size"
      :width="3"
      color="primary"
      indeterminate
    />
    <div v-if="text" class="loader-text text-body-2 text-medium-emphasis mt-3">{{ text }}</div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  fullscreen?: boolean
  size?: number
  text?: string
}>(), {
  fullscreen: false,
  size: 48,
})
</script>

<style scoped>
.loader-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.loader-container.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.8);
}
</style>
