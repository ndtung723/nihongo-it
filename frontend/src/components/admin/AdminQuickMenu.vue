<template>
  <div class="admin-quick-menu">
    <v-navigation-drawer
      v-model="drawer"
      location="right"
      temporary
      width="280"
      class="admin-menu-drawer"
      elevation="4"
    >
      <v-list>
        <v-list-item
          prepend-icon="mdi-view-dashboard"
          title="Dashboard"
          :to="{ name: 'adminDashboard' }"
          class="admin-menu-item"
        />
        <v-divider class="my-2"></v-divider>

        <v-list-subheader class="text-uppercase font-weight-medium">Quản lý nội dung</v-list-subheader>
        <v-list-item
          prepend-icon="mdi-book-open-variant"
          title="Từ vựng"
          :to="{ name: 'adminVocabulary' }"
          class="admin-menu-item"
        />
        <v-list-item
          prepend-icon="mdi-folder"
          title="Danh mục"
          :to="{ name: 'adminCategories' }"
          class="admin-menu-item"
        />
        <v-list-item
          prepend-icon="mdi-tag-multiple"
          title="Chủ đề"
          :to="{ name: 'adminTopics' }"
          class="admin-menu-item"
        />
        <v-list-item
          prepend-icon="mdi-chat"
          title="Hội thoại"
          :to="{ name: 'adminConversations' }"
          class="admin-menu-item"
        />

        <v-divider class="my-2"></v-divider>

        <v-list-subheader class="text-uppercase font-weight-medium">Người dùng</v-list-subheader>
        <v-list-item
          prepend-icon="mdi-account-group"
          title="Quản lý người dùng"
          :to="{ name: 'adminUsers' }"
          class="admin-menu-item"
        />
        <v-list-item
          prepend-icon="mdi-chart-bar"
          title="Thống kê"
          :to="{ name: 'adminStatistics' }"
          class="admin-menu-item"
        />
      </v-list>
    </v-navigation-drawer>

    <div
      class="admin-toggle-btn"
      :class="{ 'admin-toggle-btn-open': drawer }"
      @click="toggleDrawer"
    >
      <v-icon :icon="drawer ? 'mdi-chevron-right' : 'mdi-chevron-left'" color="white"></v-icon>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-facing-decorator'

@Component({
  name: 'AdminQuickMenu'
})
export default class AdminQuickMenu extends Vue {
  drawer = false;

  toggleDrawer(): void {
    this.drawer = !this.drawer;
  }

  // Giải phóng tham chiếu để tránh lỗi parentNode
  beforeUnmount() {
    this.drawer = false;
  }
}
</script>

<style scoped>
.admin-quick-menu {
  position: relative;
}

.admin-toggle-btn {
  position: fixed;
  right: 0;
  top: 50%;
  background-color: #1976d2;
  border-radius: 4px 0 0 4px;
  padding: 8px 2px;
  cursor: pointer;
  z-index: 999;
  transform: translateY(-50%);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  box-shadow: -2px 0 5px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-toggle-btn:hover {
  background-color: #1565c0;
  padding-left: 6px;
}

.admin-toggle-btn-open {
  right: 280px;
}

.admin-menu-drawer {
  z-index: 1000;
  border-left: 1px solid rgba(0, 0, 0, 0.12);
}

.admin-menu-item {
  border-radius: 4px;
  margin: 0 8px 4px 8px;
  transition: all 0.2s ease;
}

.admin-menu-item:hover {
  background-color: rgba(25, 118, 210, 0.08);
}
</style>
