<template>
  <div class="user-statistics-detail">
    <div class="d-flex justify-space-between align-center mb-4">
      <h1 class="text-h4">Thống kê học viên</h1>
      <v-btn color="primary" prepend-icon="mdi-arrow-left" @click="goBack">
        Quay lại
      </v-btn>
    </div>

    <!-- Loading State -->
    <v-row v-if="loading">
      <v-col cols="12" class="d-flex justify-center align-center" style="min-height: 300px;">
        <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      </v-col>
    </v-row>

    <!-- Error State -->
    <v-row v-else-if="error">
      <v-col cols="12">
        <v-alert type="error" variant="tonal">
          Không thể tải dữ liệu thống kê. Vui lòng thử lại sau.
          <v-btn color="error" variant="text" class="mt-2" @click="fetchUserStatistics">Thử lại</v-btn>
        </v-alert>
      </v-col>
    </v-row>

    <template v-else-if="userStats">
      <!-- User Profile Section -->
      <v-card class="mb-4">
        <v-card-text>
          <v-row>
            <v-col cols="12" sm="6" md="4" class="d-flex align-center">
              <v-avatar color="primary" size="64" class="mr-4">
                <span class="text-h4 text-white">{{ userStats.userName?.charAt(0) || 'U' }}</span>
              </v-avatar>
              <div>
                <h2 class="text-h5">{{ userStats.userName }}</h2>
                <p class="text-body-1">{{ userStats.email }}</p>
                <div class="d-flex align-center mt-1">
                  <v-icon icon="mdi-calendar" size="small" class="mr-1"></v-icon>
                  <span class="text-caption">Tham gia: {{ formatDate(userStats.profileInfo?.createdAt) }}</span>
                </div>
                <div class="d-flex align-center mt-1">
                  <v-icon icon="mdi-login" size="small" class="mr-1"></v-icon>
                  <span class="text-caption">Đăng nhập gần đây: {{ formatDate(userStats.profileInfo?.lastLogin) }}</span>
                </div>
              </div>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <div class="d-flex flex-column h-100 justify-center">
                <div class="text-center mb-2">Tiến độ học tập</div>
                <v-progress-linear
                  :model-value="userStats.progress || 0"
                  height="20"
                  color="primary"
                  striped
                >
                  <template v-slot:default="{ value }">
                    <span class="text-subtitle-2 font-weight-bold">{{ Math.round(value) }}%</span>
                  </template>
                </v-progress-linear>
                <div class="d-flex justify-space-between mt-2">
                  <span class="text-caption">Số thẻ đã học: {{ (userStats.summary?.totalCards || 0) - (userStats.cardsByState?.new || 0) }}</span>
                  <span class="text-caption">Tổng thẻ: {{ userStats.summary?.totalCards || 0 }}</span>
                </div>
              </div>
            </v-col>
            <v-col cols="12" md="4">
              <div class="d-flex flex-wrap justify-center h-100 align-center">
                <div class="text-center mx-3">
                  <div class="text-h5 font-weight-bold">{{ userStats.summary?.currentStreak || 0 }}</div>
                  <div class="text-caption">Chuỗi ngày</div>
                </div>
                <div class="text-center mx-3">
                  <div class="text-h5 font-weight-bold">{{ userStats.summary?.reviewsLast30Days || 0 }}</div>
                  <div class="text-caption">Lần ôn tập</div>
                </div>
                <div class="text-center mx-3">
                  <div class="text-h5 font-weight-bold">{{ formatPercent(userStats.summary?.overallRetentionRate) }}</div>
                  <div class="text-caption">Tỷ lệ ghi nhớ</div>
                </div>
                <div class="text-center mx-3">
                  <div class="text-h5 font-weight-bold">{{ userStats.profileInfo?.currentLevel || 'N/A' }}</div>
                  <div class="text-caption">Cấp độ hiện tại</div>
                </div>
                <div class="text-center mx-3">
                  <div class="text-h5 font-weight-bold">{{ userStats.profileInfo?.jlptGoal || 'N/A' }}</div>
                  <div class="text-caption">Mục tiêu JLPT</div>
                </div>
              </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <!-- Statistics Cards -->
      <v-row>
        <!-- Review Activity Chart -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-chart-line"></v-icon>
              Hoạt động ôn tập
            </v-card-title>
            <v-card-text>
              <canvas ref="reviewActivityChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Retention Rate Chart -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-percent"></v-icon>
              Tỷ lệ ghi nhớ theo ngày
            </v-card-title>
            <v-card-text>
              <canvas ref="retentionRateChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-row class="mt-4">
        <!-- Cards Due Forecast -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-chart-bar"></v-icon>
              Dự báo thẻ đến hạn
            </v-card-title>
            <v-card-text>
              <canvas ref="cardsDueChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Memory Strength Distribution -->
        <v-col cols="12" md="6">
          <v-card height="350">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-chart-donut"></v-icon>
              Phân bố độ ghi nhớ
            </v-card-title>
            <v-card-text class="d-flex justify-center align-center">
              <canvas ref="memoryStrengthChart" height="280"></canvas>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Card Distribution Section -->
      <v-row class="mt-4">
        <v-col cols="12" md="6">
          <!-- Card State Distribution -->
          <v-card height="300">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-layers"></v-icon>
              Phân bố trạng thái thẻ
            </v-card-title>
            <v-card-text>
              <canvas ref="cardStateChart" height="230"></canvas>
            </v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" md="6">
          <!-- JLPT Level Distribution -->
          <v-card height="300">
            <v-card-title class="text-subtitle-1">
              <v-icon start icon="mdi-certificate"></v-icon>
              Phân bố thẻ theo cấp độ JLPT
            </v-card-title>
            <v-card-text>
              <div class="d-flex flex-wrap justify-center gap-4">
                <!-- Display all JLPT levels in ascending order from N5 to N1 -->
                <template v-for="level in ['N5', 'N4', 'N3', 'N2', 'N1']" :key="level">
                  <div
                    class="jlpt-level-card mx-2 my-2"
                    :class="`jlpt-${level.toLowerCase()}`"
                  >
                    <div class="text-h5 font-weight-bold">{{ level }}</div>
                    <div class="text-body-1">{{ userStats.cardsByJlptLevel?.[level] || 0 }} thẻ</div>
                    <div class="text-caption">{{ calculateJlptPercent(level, userStats.cardsByJlptLevel?.[level] || 0) }}%</div>
                  </div>
                </template>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Recent Review History -->
      <v-card class="mt-4">
        <v-card-title class="text-subtitle-1">
          <v-icon start icon="mdi-history"></v-icon>
          Lịch sử ôn tập gần đây
        </v-card-title>
        <v-card-text>
          <v-data-table
            :headers="reviewHistoryHeaders"
            :items="userStats.reviewHistory || []"
            :items-per-page="5"
            class="elevation-0"
          >
            <!-- Timestamp column -->
            <template v-slot:item.timestamp="{ item }">
              {{ formatDateTime((item as ReviewHistoryItem).timestamp) }}
            </template>

            <!-- Card info column -->
            <template v-slot:item.vocabulary="{ item }">
              <div v-if="(item as ReviewHistoryItem).vocabulary">
                <div>{{ (item as ReviewHistoryItem).vocabulary?.japanese || (item as ReviewHistoryItem).vocabulary?.term }}</div>
                <div class="text-caption">{{ (item as ReviewHistoryItem).vocabulary?.english || (item as ReviewHistoryItem).vocabulary?.meaning }}</div>
                <v-chip size="x-small" color="info" class="mt-1">{{ (item as ReviewHistoryItem).vocabulary?.jlptLevel }}</v-chip>
              </div>
              <div v-else class="text-caption">Không có thông tin</div>
            </template>

            <!-- Rating column -->
            <template v-slot:item.rating="{ item }">
              <v-chip
                :color="getRatingColor((item as ReviewHistoryItem).rating)"
                size="small"
              >
                {{ getRatingText((item as ReviewHistoryItem).rating) }}
              </v-chip>
            </template>

            <!-- State column -->
            <template v-slot:item.state="{ item }">
              {{ formatState((item as ReviewHistoryItem).state) }}
            </template>
          </v-data-table>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import statisticsService from '@/services/statistics.service';
import type { UserStatisticsDetail, ReviewHistoryItem } from '@/services/statistics.service';
import Chart from 'chart.js/auto';

const router = useRouter();
const route = useRoute();

// State
const loading = ref(true);
const error = ref(false);
const userStats = ref<UserStatisticsDetail | null>(null);
const charts = ref<{ [key: string]: Chart }>({});

// Chart references
const reviewActivityChart = ref<HTMLCanvasElement | null>(null);
const retentionRateChart = ref<HTMLCanvasElement | null>(null);
const cardsDueChart = ref<HTMLCanvasElement | null>(null);
const memoryStrengthChart = ref<HTMLCanvasElement | null>(null);
const cardStateChart = ref<HTMLCanvasElement | null>(null);

// Review history table headers
const reviewHistoryHeaders = [
  { title: 'Thời gian', key: 'timestamp', sortable: true },
  { title: 'Từ vựng', key: 'vocabulary', sortable: false },
  { title: 'Đánh giá', key: 'rating', sortable: true },
  { title: 'Trạng thái', key: 'state', sortable: true },
];

// User ID from route
const userId = computed(() => route.params.id as string);

// Format helpers
const formatPercent = (value: number | undefined) => {
  if (value === undefined) return '0%';
  return `${Math.round(value)}%`;
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return 'Không rõ';
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
};

const formatDateTime = (dateString: string | undefined) => {
  if (!dateString) return 'Không rõ';
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatState = (state: string) => {
  switch (state) {
    case 'new': return 'Mới';
    case 'learning': return 'Đang học';
    case 'review': return 'Ôn tập';
    case 'relearning': return 'Học lại';
    case 'graduated': return 'Đã tốt nghiệp';
    default: return state;
  }
};

const getRatingColor = (rating: number) => {
  switch (rating) {
    case 4: return 'success';
    case 3: return 'info';
    case 2: return 'warning';
    case 1: return 'error';
    default: return 'grey';
  }
};

const getRatingText = (rating: number) => {
  switch (rating) {
    case 4: return 'Dễ';
    case 3: return 'Tốt';
    case 2: return 'Khó';
    case 1: return 'Lại';
    default: return 'N/A';
  }
};

const calculateJlptPercent = (level: string, count: number) => {
  if (!userStats.value?.summary?.totalCards || userStats.value.summary.totalCards === 0) return 0;
  return Math.round((count / userStats.value.summary.totalCards) * 100);
};

// Navigation
const goBack = () => {
  router.push({ name: 'adminUserStatistics' });
};

// Format date for charts
const formatChartDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' });
};

// Fetch user statistics
const fetchUserStatistics = async () => {
  loading.value = true;
  error.value = false;

  try {
    userStats.value = await statisticsService.getUserStatisticsById(userId.value);
  } catch (err) {
    error.value = true;
  } finally {
    loading.value = false;
  }
};

// Initialize charts
const initCharts = () => {
  if (!userStats.value) return;

  // Clean up existing charts
  Object.values(charts.value).forEach(chart => chart.destroy());
  charts.value = {};

  // Initialize review activity chart
  if (reviewActivityChart.value) {
    const dailyReviews = userStats.value.dailyReviews || {};
    const dates = Object.keys(dailyReviews).sort();
    const reviewCounts = dates.map(date => dailyReviews[date] || 0);

    charts.value.reviewActivity = new Chart(reviewActivityChart.value, {
      type: 'line',
      data: {
        labels: dates.map(formatChartDate),
        datasets: [{
          label: 'Số lượt ôn tập',
          data: reviewCounts,
          fill: false,
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          borderColor: 'rgba(75, 192, 192, 1)',
          tension: 0.4,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        }
      }
    });
  }

  // Initialize retention rate chart
  if (retentionRateChart.value) {
    const retentionRateByDay = userStats.value.retentionRateByDay || {};
    const dates = Object.keys(retentionRateByDay).sort();
    const retentionRates = dates.map(date => retentionRateByDay[date] || 0);

    charts.value.retentionRate = new Chart(retentionRateChart.value, {
      type: 'line',
      data: {
        labels: dates.map(formatChartDate),
        datasets: [{
          label: 'Tỷ lệ ghi nhớ (%)',
          data: retentionRates,
          fill: false,
          backgroundColor: 'rgba(153, 102, 255, 0.2)',
          borderColor: 'rgba(153, 102, 255, 1)',
          tension: 0.4,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            min: 0,
            max: 100,
            ticks: {
              callback: (value) => `${value}%`
            }
          }
        }
      }
    });
  }

  // Initialize cards due chart
  if (cardsDueChart.value) {
    const cardsDueByDay = userStats.value.cardsDueByDay || {};
    const dates = Object.keys(cardsDueByDay).sort();
    const dueCounts = dates.map(date => cardsDueByDay[date] || 0);

    charts.value.cardsDue = new Chart(cardsDueChart.value, {
      type: 'bar',
      data: {
        labels: dates.map(formatChartDate),
        datasets: [{
          label: 'Thẻ đến hạn',
          data: dueCounts,
          backgroundColor: 'rgba(255, 159, 64, 0.7)',
          borderColor: 'rgba(255, 159, 64, 1)',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        }
      }
    });
  }

  // Initialize memory strength chart
  if (memoryStrengthChart.value) {
    const memoryStrength = userStats.value.memoryStrengthDistribution || {};
    const labels = ['Yếu', 'Trung bình', 'Mạnh'];
    const data = [
      memoryStrength.weak || 0,
      memoryStrength.medium || 0,
      memoryStrength.strong || 0
    ];

    charts.value.memoryStrength = new Chart(memoryStrengthChart.value, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: [
            'rgba(255, 99, 132, 0.7)',
            'rgba(255, 206, 86, 0.7)',
            'rgba(54, 162, 235, 0.7)'
          ],
          borderColor: [
            'rgba(255, 99, 132, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(54, 162, 235, 1)'
          ],
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'right'
          }
        }
      }
    });
  }

  // Initialize card state chart
  if (cardStateChart.value) {
    const cardsByState = userStats.value?.cardsByState || {};
    const states = Object.keys(cardsByState);
    const stateLabels = states.map(state => formatState(state));
    const stateCounts = states.map(state => cardsByState[state] || 0);

    const backgroundColors = states.map(state => {
      switch (state) {
        case 'new': return 'rgba(54, 162, 235, 0.7)';
        case 'learning': return 'rgba(255, 159, 64, 0.7)';
        case 'review': return 'rgba(75, 192, 192, 0.7)';
        case 'relearning': return 'rgba(255, 99, 132, 0.7)';
        case 'graduated': return 'rgba(153, 102, 255, 0.7)';
        default: return 'rgba(201, 203, 207, 0.7)';
      }
    });

    charts.value.cardState = new Chart(cardStateChart.value, {
      type: 'pie',
      data: {
        labels: stateLabels,
        datasets: [{
          data: stateCounts,
          backgroundColor: backgroundColors,
          borderColor: backgroundColors.map(color => color.replace('0.7', '1')),
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'right'
          }
        }
      }
    });
  }
};

// Lifecycle hooks
onMounted(() => {
  fetchUserStatistics();
});

watch(userStats, () => {
  if (userStats.value) {
    nextTick(() => {
      initCharts();
    });
  }
}, { deep: true });
</script>

<style scoped lang="scss">
.jlpt-level-card {
  padding: 16px;
  border-radius: 12px;
  min-width: 120px;
  width: 140px;
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  }

  &.jlpt-n1 {
    background-color: rgba(244, 67, 54, 0.1);
    border: 1px solid rgba(244, 67, 54, 0.4);
    color: rgb(244, 67, 54);
  }

  &.jlpt-n2 {
    background-color: rgba(255, 152, 0, 0.1);
    border: 1px solid rgba(255, 152, 0, 0.4);
    color: rgb(255, 152, 0);
  }

  &.jlpt-n3 {
    background-color: rgba(255, 193, 7, 0.1);
    border: 1px solid rgba(255, 193, 7, 0.4);
    color: rgb(255, 193, 7);
  }

  &.jlpt-n4 {
    background-color: rgba(76, 175, 80, 0.1);
    border: 1px solid rgba(76, 175, 80, 0.4);
    color: rgb(76, 175, 80);
  }

  &.jlpt-n5 {
    background-color: rgba(33, 150, 243, 0.1);
    border: 1px solid rgba(33, 150, 243, 0.4);
    color: rgb(33, 150, 243);
  }
}
</style>
