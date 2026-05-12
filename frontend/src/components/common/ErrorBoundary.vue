<template>
  <slot v-if="!hasError" />
  <v-alert v-else type="error" variant="tonal" class="ma-4">
    <v-alert-title>Đã xảy ra lỗi</v-alert-title>
    <p class="text-body-2 mt-1">{{ errorMessage }}</p>
    <v-btn variant="outlined" size="small" class="mt-2" @click="reset">
      Thử lại
    </v-btn>
  </v-alert>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from "vue";

const hasError = ref(false);
const errorMessage = ref("Vui lòng thử lại sau.");

onErrorCaptured((err: Error) => {
  hasError.value = true;
  errorMessage.value = err.message || "Vui lòng thử lại sau.";
  return false;
});

function reset() {
  hasError.value = false;
  errorMessage.value = "Vui lòng thử lại sau.";
}
</script>
