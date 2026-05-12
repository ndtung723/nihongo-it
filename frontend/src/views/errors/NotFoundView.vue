<template>
  <div class="not-found-page">
    <v-container class="text-center py-12">
      <div class="not-found-content">
        <h1 class="text-h2 font-weight-bold mb-4">404</h1>
        <h2 class="text-h4 mb-6">Không tìm thấy trang</h2>
        <p class="text-body-1 mb-8">
          Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
        </p>
        <v-btn color="primary" size="large" :to="{ name: 'home' }" class="px-8">
          Quay về Trang chủ
        </v-btn>
      </div>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";

onMounted(() => {
  document.title = "404 - Không tìm thấy trang | Nihongo IT";
});
</script>

<style lang="scss" scoped>
.not-found-page {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;

  .not-found-content {
    max-width: 600px;
    margin: 0 auto;
  }

  h1 {
    font-size: 5rem;
    color: var(--v-primary-base, #1976d2);
    line-height: 1.2;
  }

  h2 {
    color: #333;
    font-weight: 500;
  }

  p {
    color: #666;
    margin-bottom: 2rem;
  }
}
</style>
