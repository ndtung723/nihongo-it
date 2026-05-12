<template>
  <div class="admin-statistics-overview">
    <h1 class="text-h4 mb-4">Thống kê tổng quan</h1>

    <!-- Loading State -->
    <v-row v-if="loading">
      <v-col
        cols="12"
        class="d-flex justify-center align-center"
        style="min-height: 300px"
      >
        <v-progress-circular
          indeterminate
          color="primary"
          size="64"
        ></v-progress-circular>
      </v-col>
    </v-row>

    <!-- Error State -->
    <v-row v-else-if="error">
      <v-col cols="12">
        <v-alert type="error" variant="tonal">
          Không thể tải dữ liệu thống kê. Vui lòng thử lại sau.
          <v-btn
            color="error"
            variant="text"
            class="mt-2"
            @click="fetchStatistics"
            >Thử lại</v-btn
          >
        </v-alert>
      </v-col>
    </v-row>

    <!-- Data display -->
    <template v-else-if="stats">
      <!-- Summary Cards -->
      <v-row>
        <v-col cols="12" sm="6" md="3">
          <v-card class="summary-card">
            <v-card-text>
              <div class="d-flex justify-space-between align-center">
                <div>
                  <div class="text-overline">Tổng người dùng</div>
                  <div class="text-h4">{{ stats.totalUsers || 0 }}</div>
                </div>
                <v-icon
                  icon="mdi-account-group"
                  size="36"
                  color="primary"
                ></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
          <v-card class="summary-card">
            <v-card-text>
              <div class="d-flex justify-space-between align-center">
                <div>
                  <div class="text-overline">Người dùng hoạt động</div>
                  <div class="text-h4">{{ stats.activeUsers || 0 }}</div>
                </div>
                <v-icon
                  icon="mdi-account-check"
                  size="36"
                  color="success"
                ></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
          <v-card class="summary-card">
            <v-card-text>
              <div class="d-flex justify-space-between align-center">
                <div>
                  <div class="text-overline">Tổng thẻ ghi nhớ</div>
                  <div class="text-h4">{{ stats.totalFlashcards || 0 }}</div>
                </div>
                <v-icon
                  icon="mdi-cards-outline"
                  size="36"
                  color="error"
                ></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
          <v-card class="summary-card">
            <v-card-text>
              <div class="d-flex justify-space-between align-center">
                <div>
                  <div class="text-overline">Tỷ lệ ghi nhớ trung bình</div>
                  <div class="text-h4">
                    {{ formatPercent(stats.averageRetentionRate) }}
                  </div>
                </div>
                <v-icon icon="mdi-brain" size="36" color="warning"></v-icon>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- User Distribution Charts -->
      <v-row class="mt-4">
        <!-- Users by Current Level -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-chart-bar"></v-icon>
              Người dùng theo cấp độ hiện tại
            </v-card-title>
            <v-card-text>
              <canvas ref="usersByLevelChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Users by JLPT Goal -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-chart-bar"></v-icon>
              Người dùng theo mục tiêu JLPT
            </v-card-title>
            <v-card-text>
              <canvas ref="usersByJlptGoalChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Top Performers and Most Active Users -->
      <v-row class="mt-4">
        <!-- Top Performing Users -->
        <v-col cols="12" md="6">
          <v-card min-height="400">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-podium"></v-icon>
              Người dùng có hiệu suất cao nhất
            </v-card-title>
            <v-card-text>
              <v-list lines="two">
                <v-list-item
                  v-for="(user, index) in stats.topPerformingUsers"
                  :key="user.userId"
                  link
                  @click="viewUserDetails(user.userId)"
                >
                  <template v-slot:prepend>
                    <v-avatar color="primary" class="text-white">
                      {{ index + 1 }}
                    </v-avatar>
                  </template>
                  <v-list-item-title>{{ user.userName }}</v-list-item-title>
                  <v-list-item-subtitle>
                    <v-chip size="small" color="success" class="mr-2">
                      {{ formatPercent(user.summary?.overallRetentionRate) }}
                    </v-chip>
                    {{ user.email }}
                  </v-list-item-subtitle>
                </v-list-item>
                <v-list-item v-if="!stats.topPerformingUsers?.length">
                  <v-list-item-title class="text-center"
                    >Không có dữ liệu</v-list-item-title
                  >
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Most Active Users -->
        <v-col cols="12" md="6">
          <v-card min-height="400">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-fire"></v-icon>
              Người dùng hoạt động nhiều nhất
            </v-card-title>
            <v-card-text>
              <v-list lines="two">
                <v-list-item
                  v-for="(user, index) in stats.mostActiveUsers"
                  :key="user.userId"
                  link
                  @click="viewUserDetails(user.userId)"
                >
                  <template v-slot:prepend>
                    <v-avatar color="error" class="text-white">
                      {{ index + 1 }}
                    </v-avatar>
                  </template>
                  <v-list-item-title>{{ user.userName }}</v-list-item-title>
                  <v-list-item-subtitle>
                    <v-chip size="small" color="primary" class="mr-2">
                      {{ user.summary?.reviewsLast30Days || 0 }} lượt ôn tập
                    </v-chip>
                    {{ formatDate(user.lastActive) }}
                  </v-list-item-subtitle>
                </v-list-item>
                <v-list-item v-if="!stats.mostActiveUsers?.length">
                  <v-list-item-title class="text-center"
                    >Không có dữ liệu</v-list-item-title
                  >
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- User List Button -->
      <v-row class="mt-4">
        <v-col cols="12" class="text-center">
          <v-btn color="primary" size="large" @click="goToUserStatistics">
            <v-icon start>mdi-eye</v-icon>
            Xem thống kê chi tiết theo học viên
          </v-btn>
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from "vue";
import { useRouter } from "vue-router";
import statisticsService from "@/services/statistics.service";
import type { AdminStatisticsOverview } from "@/services/statistics.service";
import type { Chart as ChartType } from "chart.js";

const router = useRouter();

// State for statistics
const loading = ref(true);
const error = ref(false);
const stats = ref<AdminStatisticsOverview | null>(null);
const charts = ref<{ [key: string]: ChartType }>({});

// Chart references
const usersByLevelChart = ref<HTMLCanvasElement | null>(null);
const usersByJlptGoalChart = ref<HTMLCanvasElement | null>(null);

// Format helpers
const formatPercent = (value: number | undefined) => {
  if (value === undefined) return "0%";
  return `${Math.round(value)}%`;
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return "Không rõ";
  const date = new Date(dateString);
  return date.toLocaleDateString("vi-VN", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// Navigation functions
const viewUserDetails = (userId: string) => {
  router.push(`/admin/statistics/users/${userId}`);
};

const goToUserStatistics = () => {
  router.push({ name: "adminUserStatistics" });
};

// Fetch statistics
const fetchStatistics = async () => {
  loading.value = true;
  error.value = false;

  try {
    stats.value = await statisticsService.getStatisticsOverview();
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
};

// Initialize charts — Chart.js loaded on-demand
const initCharts = async () => {
  if (!stats.value) return;

  const { default: Chart } = await import("chart.js/auto");

  // Clean up existing charts
  Object.values(charts.value).forEach((chart) => chart.destroy());
  charts.value = {};

  // Initialize users by level chart
  if (usersByLevelChart.value) {
    const usersByLevel = stats.value.usersByLevel || {};
    const levels = Object.keys(usersByLevel);
    const counts = levels.map((level) => usersByLevel[level]);

    charts.value.usersByLevel = new Chart(usersByLevelChart.value, {
      type: "bar",
      data: {
        labels: levels,
        datasets: [
          {
            label: "Số người dùng",
            data: counts,
            backgroundColor: [
              "rgba(75, 192, 192, 0.6)",
              "rgba(54, 162, 235, 0.6)",
              "rgba(255, 206, 86, 0.6)",
              "rgba(255, 99, 132, 0.6)",
              "rgba(153, 102, 255, 0.6)",
            ],
            borderColor: [
              "rgba(75, 192, 192, 1)",
              "rgba(54, 162, 235, 1)",
              "rgba(255, 206, 86, 1)",
              "rgba(255, 99, 132, 1)",
              "rgba(153, 102, 255, 1)",
            ],
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0,
            },
          },
        },
      },
    });
  }

  // Initialize users by JLPT goal chart
  if (usersByJlptGoalChart.value) {
    const usersByJlptGoal = stats.value.usersByJlptGoal || {};
    const goals = Object.keys(usersByJlptGoal);
    const counts = goals.map((goal) => usersByJlptGoal[goal]);

    charts.value.usersByJlptGoal = new Chart(usersByJlptGoalChart.value, {
      type: "pie",
      data: {
        labels: goals,
        datasets: [
          {
            data: counts,
            backgroundColor: [
              "rgba(255, 99, 132, 0.7)",
              "rgba(255, 159, 64, 0.7)",
              "rgba(255, 205, 86, 0.7)",
              "rgba(75, 192, 192, 0.7)",
              "rgba(54, 162, 235, 0.7)",
              "rgba(153, 102, 255, 0.7)",
            ],
            borderColor: [
              "rgba(255, 99, 132, 1)",
              "rgba(255, 159, 64, 1)",
              "rgba(255, 205, 86, 1)",
              "rgba(75, 192, 192, 1)",
              "rgba(54, 162, 235, 1)",
              "rgba(153, 102, 255, 1)",
            ],
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "right",
          },
        },
      },
    });
  }
};

// Lifecycle hooks
onMounted(() => {
  fetchStatistics();
});

watch(
  stats,
  () => {
    if (stats.value) {
      nextTick(() => {
        initCharts();
      });
    }
  },
  { deep: true },
);
</script>

<style scoped lang="scss">
.summary-card {
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-5px);
  }
}
</style>
