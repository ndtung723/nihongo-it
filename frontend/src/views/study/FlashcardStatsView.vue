<template>
  <div class="flashcard-stats-container">
    <v-container>
      <v-row>
        <v-col cols="12" class="d-flex align-center">
          <v-btn
            color="primary"
            variant="text"
            prepend-icon="mdi-arrow-left"
            class="mr-4"
            @click="goToFlashcardStudy"
          >
            Quay lại học tập
          </v-btn>
          <h1 class="text-h4">Thống kê học tập</h1>
        </v-col>
      </v-row>

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
            <v-btn color="error" variant="text" class="mt-2" @click="fetchStatistics">Thử lại</v-btn>
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
                    <div class="text-overline">Tổng thẻ</div>
                    <div class="text-h4">{{ stats.summary?.totalCards || 0 }}</div>
                  </div>
                  <v-icon icon="mdi-cards-outline" size="36" color="primary"></v-icon>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" sm="6" md="3">
            <v-card class="summary-card">
              <v-card-text>
                <div class="d-flex justify-space-between align-center">
                  <div>
                    <div class="text-overline">Thẻ đến hạn</div>
                    <div class="text-h4">{{ stats.summary?.dueCardsNow || 0 }}</div>
                  </div>
                  <v-icon icon="mdi-calendar-clock" size="36" color="warning"></v-icon>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" sm="6" md="3">
            <v-card class="summary-card">
              <v-card-text>
                <div class="d-flex justify-space-between align-center">
                  <div>
                    <div class="text-overline">Chuỗi ngày học</div>
                    <div class="text-h4">{{ stats.summary?.currentStreak || 0 }}</div>
                  </div>
                  <v-icon icon="mdi-fire" size="36" color="error"></v-icon>
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <v-col cols="12" sm="6" md="3">
            <v-card class="summary-card">
              <v-card-text>
                <div class="d-flex justify-space-between align-center">
                  <div>
                    <div class="text-overline">Tỷ lệ ghi nhớ</div>
                    <div class="text-h4">{{ formatPercent(stats.summary?.overallRetentionRate) }}</div>
                  </div>
                  <v-icon icon="mdi-brain" size="36" color="success"></v-icon>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- Charts Section -->
        <v-row class="mt-4">
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

        <!-- JLPT Level Distribution -->
        <v-row class="mt-4" v-if="stats.cardsByJlptLevel && hasJlptCards">
          <v-col cols="12">
            <v-card>
              <v-card-title class="text-subtitle-1">
                <v-icon start icon="mdi-certificate"></v-icon>
                Phân bố thẻ theo cấp độ JLPT
              </v-card-title>
              <v-card-text>
                <div class="d-flex flex-wrap justify-center gap-4">
                  <template v-for="(count, level) in stats.cardsByJlptLevel" :key="level">
                    <div
                      v-if="String(level) !== 'unknown' && Number(count) > 0"
                      class="jlpt-level-card"
                      :class="`jlpt-${String(level).toLowerCase()}`"
                    >
                      <div class="text-h5 font-weight-bold">{{ level }}</div>
                      <div class="text-body-1">{{ count }} thẻ</div>
                      <div class="text-caption">{{ calculateJlptPercent(String(level), Number(count)) }}%</div>
                    </div>
                  </template>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>

        <!-- Card State Distribution -->
        <v-row class="mt-4">
          <v-col cols="12">
            <v-card>
              <v-card-title class="text-subtitle-1">
                <v-icon start icon="mdi-layers"></v-icon>
                Phân bố trạng thái thẻ
              </v-card-title>
              <v-card-text>
                <div class="d-flex flex-wrap justify-center gap-4">
                  <div
                    v-for="(count, state) in stats.cardsByState"
                    :key="state"
                    class="state-card"
                    :class="`state-${state}`"
                  >
                    <div class="text-h6 text-capitalize">{{ formatState(String(state)) }}</div>
                    <div class="text-body-1">{{ count }} thẻ</div>
                    <div class="text-caption">{{ calculateStatePercent(String(state), count) }}%</div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </template>
    </v-container>

    <!-- Floating action button for mobile -->
    <v-btn
      class="floating-back-btn"
      color="primary"
      icon
      @click="goToFlashcardStudy"
      size="large"
    >
      <v-icon>mdi-arrow-left</v-icon>
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import flashcardService from '@/services/flashcard.service';
import Chart from 'chart.js/auto';

// Router
const router = useRouter();

// State
const loading = ref(true);
const error = ref(false);
const stats = ref<any>(null);
const charts = ref<{ [key: string]: Chart }>({});

// Chart references
const reviewActivityChart = ref<HTMLCanvasElement | null>(null);
const retentionRateChart = ref<HTMLCanvasElement | null>(null);
const cardsDueChart = ref<HTMLCanvasElement | null>(null);
const memoryStrengthChart = ref<HTMLCanvasElement | null>(null);

// Computed properties
const hasJlptCards = computed(() => {
  if (!stats.value?.cardsByJlptLevel) return false;

  const jlptLevels = Object.keys(stats.value.cardsByJlptLevel)
    .filter(level => level !== 'unknown');

  return jlptLevels.some(level => stats.value.cardsByJlptLevel[level] > 0);
});

// Fetch statistics
const fetchStatistics = async () => {
  loading.value = true;
  error.value = false;

  try {
    stats.value = await flashcardService.getStudyStatistics();
  } catch (err) {
    error.value = true;
  } finally {
    loading.value = false;
  }
};

// Navigation
const goToFlashcardStudy = () => {
  router.push({ name: 'flashcardStudy' })
};

// Format helpers
const formatPercent = (value: number | undefined) => {
  if (value === undefined) return '0%';
  return `${Math.round(value)}%`;
};

const formatState = (state: string) => {
  switch (state) {
    case 'new': return 'Mới';
    case 'learning': return 'Đang học';
    case 'review': return 'Ôn tập';
    case 'relearning': return 'Học lại';
    default: return state;
  }
};

const calculateJlptPercent = (level: string, count: number) => {
  if (!stats.value?.summary?.totalCards || stats.value.summary.totalCards === 0) return 0;
  return Math.round((count / stats.value.summary.totalCards) * 100);
};

const calculateStatePercent = (state: string, count: number) => {
  if (!stats.value?.summary?.totalCards || stats.value.summary.totalCards === 0) return 0;
  return Math.round((count / stats.value.summary.totalCards) * 100);
};

// Generate chart data objects
const getReviewActivityChartData = () => {
  if (!stats.value?.dailyReviews) return null;

  // Sort dates chronologically
  const dates = Object.keys(stats.value.dailyReviews).sort();
  const reviewCounts = dates.map(date => stats.value.dailyReviews[date] || 0);

  return {
    labels: dates.map(date => formatDate(date)),
    datasets: [{
      label: 'Số lượt ôn tập',
      data: reviewCounts,
      fill: true,
      backgroundColor: 'rgba(75, 192, 192, 0.2)',
      borderColor: 'rgba(75, 192, 192, 1)',
      tension: 0.4,
    }]
  };
};

const getRetentionRateChartData = () => {
  if (!stats.value?.retentionRateByDay) return null;

  // Sort dates chronologically
  const dates = Object.keys(stats.value.retentionRateByDay).sort();
  const retentionRates = dates.map(date => stats.value.retentionRateByDay[date] || 0);

  return {
    labels: dates.map(date => formatDate(date)),
    datasets: [{
      label: 'Tỷ lệ ghi nhớ (%)',
      data: retentionRates,
      fill: false,
      backgroundColor: 'rgba(153, 102, 255, 0.2)',
      borderColor: 'rgba(153, 102, 255, 1)',
      tension: 0.4,
    }]
  };
};

const getCardsDueChartData = () => {
  if (!stats.value?.cardsDueByDay) return null;

  // Sort dates chronologically
  const dates = Object.keys(stats.value.cardsDueByDay).sort();
  const dueCounts = dates.map(date => stats.value.cardsDueByDay[date] || 0);

  return {
    labels: dates.map(date => formatDate(date)),
    datasets: [{
      label: 'Thẻ đến hạn',
      data: dueCounts,
      backgroundColor: 'rgba(255, 159, 64, 0.7)',
      borderColor: 'rgba(255, 159, 64, 1)',
      borderWidth: 1
    }]
  };
};

const getMemoryStrengthChartData = () => {
  if (!stats.value?.memoryStrengthDistribution) return null;

  const labels = ['Yếu', 'Trung bình', 'Mạnh'];
  const counts = [
    stats.value.memoryStrengthDistribution.weak || 0,
    stats.value.memoryStrengthDistribution.medium || 0,
    stats.value.memoryStrengthDistribution.strong || 0
  ];

  return {
    labels,
    datasets: [{
      data: counts,
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
  };
};

// Format date helper
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' });
};

// Initialize charts
const initCharts = () => {
  if (!stats.value) return;

  // Clean up existing charts
  Object.values(charts.value).forEach(chart => chart.destroy());
  charts.value = {};

  // Initialize new charts
  if (reviewActivityChart.value) {
    const data = getReviewActivityChartData();
    if (data) {
      charts.value.reviewActivity = new Chart(reviewActivityChart.value, {
        type: 'line',
        data,
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
  }

  if (retentionRateChart.value) {
    const data = getRetentionRateChartData();
    if (data) {
      charts.value.retentionRate = new Chart(retentionRateChart.value, {
        type: 'line',
        data,
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
  }

  if (cardsDueChart.value) {
    const data = getCardsDueChartData();
    if (data) {
      charts.value.cardsDue = new Chart(cardsDueChart.value, {
        type: 'bar',
        data,
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
  }

  if (memoryStrengthChart.value) {
    const data = getMemoryStrengthChartData();
    if (data) {
      charts.value.memoryStrength = new Chart(memoryStrengthChart.value, {
        type: 'doughnut',
        data,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom'
            }
          }
        }
      });
    }
  }
};

// Watch for changes to stats
watch(stats, () => {
  if (stats.value) {
    setTimeout(() => {
      initCharts();
    }, 100);
  }
}, { deep: true });

// Lifecycle hooks
onMounted(() => {
  fetchStatistics();
});
</script>

<style scoped lang="scss">
.flashcard-stats-container {
  padding: 20px 0;
  position: relative;
}

.floating-back-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 99;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);

  @media (min-width: 960px) {
    display: none;
  }
}

.summary-card {
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-5px);
  }
}

.jlpt-level-card {
  padding: 16px;
  border-radius: 8px;
  min-width: 100px;
  text-align: center;

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

.state-card {
  padding: 16px;
  border-radius: 8px;
  min-width: 120px;
  text-align: center;
  margin: 4px;

  &.state-new {
    background-color: rgba(33, 150, 243, 0.1);
    border: 1px solid rgba(33, 150, 243, 0.4);
    color: rgb(33, 150, 243);
  }

  &.state-learning {
    background-color: rgba(255, 152, 0, 0.1);
    border: 1px solid rgba(255, 152, 0, 0.4);
    color: rgb(255, 152, 0);
  }

  &.state-review {
    background-color: rgba(76, 175, 80, 0.1);
    border: 1px solid rgba(76, 175, 80, 0.4);
    color: rgb(76, 175, 80);
  }

  &.state-relearning {
    background-color: rgba(244, 67, 54, 0.1);
    border: 1px solid rgba(244, 67, 54, 0.4);
    color: rgb(244, 67, 54);
  }
}
</style>
