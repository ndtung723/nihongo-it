<template>
  <div class="statistics-container">
    <!-- Flashcard Stats Section -->
    <v-container>
      <v-row class="mt-4">
        <v-col cols="12">
          <h2 class="text-h5 mb-4">Thống kê thẻ ghi nhớ</h2>
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
        <v-row class="mt-4" v-if="stats.cardsByJlptLevel">
          <v-col cols="12">
            <v-card>
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
                      <div class="text-body-1">{{ stats.cardsByJlptLevel[level] || 0 }} thẻ</div>
                      <div class="text-caption">{{ calculateJlptPercent(level, stats.cardsByJlptLevel[level] || 0) }}%</div>
                    </div>
                  </template>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </template>

      <v-card class="mt-4">
        <div class="d-flex justify-space-between align-center">
          <v-card-title class="text-subtitle-1">
                  <v-icon start icon="mdi-layers"></v-icon>
                  Phân bố trạng thái thẻ
          </v-card-title>
          <span class="text-info cursor-pointer pr-4 link-underline" @click="navigateToVocabularyStorage">Xem chi tiết</span>
        </div>

        <v-card-text>
          <div v-if="vocabLoading" class="text-center my-6">
            <v-progress-circular indeterminate color="warning" size="64"></v-progress-circular>
          </div>

          <template v-else>
            <div class="text-center mt-6">
              <div class="vocab-progress-container position-relative mx-auto" style="width: 150px; height: 150px;">
                <canvas ref="vocabDonutChart"></canvas>
                <div class="vocab-count-overlay">
                  <div class="text-h4 text-grey-darken-1">{{ vocabStats.totalVocabulary }}</div>
                  <div class="text-caption text-grey-darken-1">từ</div>
                </div>
              </div>
              <div class="text-center mt-2">
                <div class="text-body-2">thu nạp</div>
                <div class="text-h5 font-weight-bold" v-if="stats?.cardsByState && stats.summary?.totalCards">
                  {{ calculateRetentionRate() }}%
                </div>
                <div class="text-h5 font-weight-bold" v-else>
                  0%
                </div>
              </div>
            </div>

            <div class="d-flex flex-wrap justify-center mt-4 mb-2">
              <div class="legend-item mx-3 d-flex align-center">
                <div class="legend-color" style="background-color: #6366F1;"></div>
                <span class="legend-text">thẻ mới ({{ vocabStats.newCount }} từ)</span>
              </div>
              <div class="legend-item mx-3 d-flex align-center">
                <div class="legend-color" style="background-color: #F97316;"></div>
                <span class="legend-text">đang học ({{ vocabStats.learningCount }} từ)</span>
              </div>
              <div class="legend-item mx-3 d-flex align-center">
                <div class="legend-color" style="background-color: #22C55E;"></div>
                <span class="legend-text">ôn tập ({{ vocabStats.reviewCount }} từ)</span>
              </div>
              <div class="legend-item mx-3 d-flex align-center">
                <div class="legend-color" style="background-color: #3B82F6;"></div>
                <span class="legend-text">học lại ({{ vocabStats.matureCount }} từ)</span>
              </div>
            </div>
          </template>
        </v-card-text>
      </v-card>
    </v-container>

    <!-- Vocabulary Learning Card -->


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
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import flashcardService from '@/services/flashcard.service';
import { useVocabularyStore } from '@/stores';
import Chart from 'chart.js/auto';

const router = useRouter();
const vocabularyStore = useVocabularyStore();

// State for statistics
const loading = ref(true);
const error = ref(false);
const stats = ref<any>(null);
const charts = ref<{ [key: string]: Chart }>({});
const vocabStats = ref({
  totalVocabulary: 0,
  newCount: 0,
  learningCount: 0,
  reviewCount: 0,
  matureCount: 0
});
const vocabLoading = ref(false);

// Chart references
const reviewActivityChart = ref<HTMLCanvasElement | null>(null);
const retentionRateChart = ref<HTMLCanvasElement | null>(null);
const cardsDueChart = ref<HTMLCanvasElement | null>(null);
const memoryStrengthChart = ref<HTMLCanvasElement | null>(null);
const vocabDonutChart = ref<HTMLCanvasElement | null>(null);
let vocabChart: Chart | null = null;

// Date and time
const currentTime = ref('21:18');
const currentDate = ref('20/04/2025');

// Computed properties
const hasJlptCards = computed(() => {
  if (!stats.value?.cardsByJlptLevel) return false;

  const jlptLevels = Object.keys(stats.value.cardsByJlptLevel)
    .filter(level => level !== 'unknown');

  return jlptLevels.some(level => stats.value.cardsByJlptLevel[level] > 0);
});

// Method to format display date
function formatDisplayDate() {
  const date = new Date();
  const day = String(date.getDate()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const year = date.getFullYear();
  currentDate.value = `${day}/${month}/${year}`;
}

// Method to update time
function updateTime() {
  const date = new Date();
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  currentTime.value = `${hours}:${minutes}`;
}

// Navigation methods
function goToFlashcardStudy() {
  router.push({ name: 'flashcardStudy' });
}

function navigateToVocabularyStorage() {
  router.push({ name: 'vocabularyStorage' });
}

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

const calculateStatePercent = () => {
  if (!stats.value?.cardsByState || !stats.value?.summary?.totalCards) {
    return {
      new: 0,
      learning: 0,
      reviewing: 0,
      relearning: 0,
      graduated: 0
    };
  }

  const newCount = stats.value.cardsByState['new'] || 0;
  const learningCount = stats.value.cardsByState['learning'] || 0;
  const reviewCount = stats.value.cardsByState['review'] || 0;
  const relearningCount = stats.value.cardsByState['relearning'] || 0;
  const graduatedCount = stats.value.cardsByState['graduated'] || 0;

  return {
    new: newCount,
    learning: learningCount,
    reviewing: reviewCount,
    relearning: relearningCount,
    graduated: graduatedCount
  };
};

// Format date for charts
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric' });
};

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

// Fetch vocabulary statistics
const fetchVocabularyStats = async () => {
  vocabLoading.value = true;

  try {
    // Use the store to fetch saved vocabulary (all saved items)
    await vocabularyStore.fetchSavedVocabulary(
      0,  // First page
      100, // Larger page size to get more items at once
      '',  // No search filter
      'date_desc'  // Sort by date, newest first
    );

    // Get flashcard states for all vocabulary items
    const fetchPromises = vocabularyStore.savedVocabulary.map(async (vocab) => {
      if (!vocab.vocabId) return null;

      try {
        const flashcards = await flashcardService.getFlashcardsByVocabulary(vocab.vocabId);
        return { vocab, flashcards: flashcards.length > 0 ? flashcards[0] : null };
      } catch (error) {
        return { vocab, flashcards: null };
      }
    });

    const results = await Promise.all(fetchPromises);

    // Count by states
    let newCount = 0;
    let learningCount = 0;
    let reviewCount = 0;
    let matureCount = 0;

    results.forEach(result => {
      if (!result) return;

      const { flashcards } = result;

      if (!flashcards) {
        // No flashcard means it's new
        newCount++;
      } else {
        // Count based on flashcard state
        switch (flashcards.state) {
          case 'new':
            newCount++;
            break;
          case 'learning':
            learningCount++;
            break;
          case 'review':
            reviewCount++;
            break;
          case 'relearning':
            matureCount++;
            break;
          default:
            // Unknown state, count as new
            newCount++;
        }
      }
    });

    // Update state
    vocabStats.value = {
      totalVocabulary: vocabularyStore.totalSavedItems,
      newCount,
      learningCount,
      reviewCount,
      matureCount
    };

  } catch (error) {
  } finally {
    vocabLoading.value = false;
  }
};

// Update chart data functions to use formatDate
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
      fill: false,
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

// Fix memoryStrengthChart to prevent errors
const getMemoryStrengthChartData = () => {
  if (!stats.value?.memoryStrengthDistribution) return null;

  const labels = ['Mới', 'Yếu', 'Trung bình', 'Mạnh'];
  const counts = [
    stats.value.memoryStrengthDistribution.new || 0,
    stats.value.memoryStrengthDistribution.weak || 0,
    stats.value.memoryStrengthDistribution.medium || 0,
    stats.value.memoryStrengthDistribution.strong || 0
  ];

  return {
    labels,
    datasets: [{
      data: counts,
      backgroundColor: [
        'rgba(255, 159, 64, 0.7)',
        'rgba(255, 99, 132, 0.7)',
        'rgba(255, 206, 86, 0.7)',
        'rgba(54, 162, 235, 0.7)'
      ],
      borderColor: [
        'rgba(255, 159, 64, 1)',
        'rgba(255, 99, 132, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(54, 162, 235, 1)'
      ],
      borderWidth: 1
    }]
  };
};

// Simplify the initCharts function to match FlashcardStatsView.vue
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

// Simplify the initVocabChart function
const initVocabChart = () => {
  if (!vocabDonutChart.value) return;

  // Destroy previous chart if it exists
  if (vocabChart) {
    vocabChart.destroy();
    vocabChart = null;
  }

  const ctx = vocabDonutChart.value.getContext('2d');
  if (!ctx) return;

  // Properly access the vocabStats ref values
  const newCount = vocabStats.value.newCount || 0;
  const learningCount = vocabStats.value.learningCount || 0;
  const reviewCount = vocabStats.value.reviewCount || 0;
  const matureCount = vocabStats.value.matureCount || 0;
  const total = newCount + learningCount + reviewCount + matureCount;

  // If no data, show empty chart with more visible placeholder
  if (total === 0) {
    vocabChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Không có dữ liệu'],
        datasets: [{
          data: [1],
          backgroundColor: ['rgba(200, 200, 200, 0.5)'],
          borderWidth: 0
        }]
      },
      options: {
        cutout: '70%',
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            enabled: false
          }
        }
      }
    });
    return;
  }

  // Create chart with real data (simplified)
  vocabChart = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Thẻ mới', 'Đang học', 'Ôn tập', 'Học lại'],
      datasets: [{
        data: [newCount, learningCount, reviewCount, matureCount],
        backgroundColor: [
          '#6366F1', // Thẻ mới - Indigo
          '#F97316', // Đang học - Orange
          '#22C55E', // Ôn tập - Green
          '#3B82F6'  // Học lại - Blue
        ],
        borderWidth: 1,
        borderColor: '#fff'
      }]
    },
    options: {
      cutout: '70%',
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: function(context: any) {
              const label = context.label || '';
              const value = context.raw !== undefined ? Number(context.raw) : 0;
              const percentage = total > 0 ? Math.round((value / total) * 100) : 0;
              return `${label}: ${percentage}%`;
            }
          }
        }
      }
    }
  });
};

// Update lifecycle hooks - simplify to match FlashcardStatsView.vue
onMounted(() => {
  // Format date and start time interval
  formatDisplayDate();
  updateTime();
  setInterval(updateTime, 60000);

  // Fetch statistics data and vocabulary stats
  fetchStatistics();
  fetchVocabularyStats();
});

// Simplify the watch functions
watch(stats, () => {
  if (stats.value) {
    setTimeout(() => {
      initCharts();
    }, 100);
  }
}, { deep: true });

watch(vocabStats, () => {
  if (vocabStats.value) {
    setTimeout(() => {
      initVocabChart();
    }, 100);
  }
}, { deep: true });

// Add a new function to calculate overall retention rate
const calculateRetentionRate = () => {
  if (!stats.value?.cardsByState || !stats.value?.summary?.totalCards) return 0;

  // Calculate retention based on ratio of review and relearning cards
  const reviewCount = stats.value.cardsByState['review'] || 0;
  const relearningCount = stats.value.cardsByState['relearning'] || 0;
  const totalCards = stats.value.summary.totalCards;

  if (totalCards === 0) return 0;

  // Calculate retention as percentage of cards in mature states
  return Math.round(((reviewCount + relearningCount) / totalCards) * 100);
};
</script>

<style scoped lang="scss">

.header-section {
  padding-top: 8px;
  padding-bottom: 80px;
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
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

// Stats cards
.summary-card {
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-5px);
  }
}

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

.state-card {
  padding: 16px;
  border-radius: 12px;
  min-width: 120px;
  width: 140px;
  text-align: center;
  margin: 4px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
  }

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

// Original vocabulary card styling
.vocab-progress-container {
  width: 180px;
  height: 180px;
  margin: 0 auto;
  position: relative;
}

.vocab-count-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.legend-item {
  margin-bottom: 8px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

.legend-text {
  font-size: 0.85rem;
  color: rgba(0, 0, 0, 0.6);
}

.cursor-pointer {
  cursor: pointer;
}

.link-underline {
  text-decoration: underline;
  color: #1867c0;
  font-weight: 500;
}
</style>
