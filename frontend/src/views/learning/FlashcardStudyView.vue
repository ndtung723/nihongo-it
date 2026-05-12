<template>
  <div class="flashcard-study-container">
    <!-- App Bar -->
    <v-app-bar flat color="white" density="compact" class="app-header">
      <v-btn
        icon
        variant="text"
        size="small"
        @click="goBack"
        class="back-btn mr-1"
      >
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>
      <v-app-bar-title class="text-subtitle-1 font-weight-bold"
        >Học Thẻ Ghi Nhớ</v-app-bar-title
      >
      <v-spacer></v-spacer>
      <v-btn
        icon
        size="small"
        variant="text"
        @click="showStats = true"
        class="mr-1"
      >
        <v-icon>mdi-chart-box</v-icon>
      </v-btn>
      <v-btn
        icon
        color="primary"
        size="small"
        variant="text"
        @click="goToStatisticsDashboard"
        title="Xem thống kê chi tiết"
      >
        <v-icon>mdi-chart-areaspline</v-icon>
      </v-btn>
    </v-app-bar>

    <!-- Loading State -->
    <div v-if="loading" class="px-4 pt-4">
      <v-skeleton-loader type="card" class="mb-4" />
      <v-skeleton-loader type="actions" />
    </div>

    <!-- No Cards State -->
    <v-container
      v-else-if="dueCards.length === 0 && !loading"
      class="text-center py-6"
    >
      <v-card class="pa-6 mx-auto empty-state-card" max-width="500">
        <v-icon size="64" color="primary" class="mb-4 empty-icon"
          >mdi-check-circle</v-icon
        >
        <h2 class="text-h5 mb-2">Chúc Mừng!</h2>
        <p class="text-body-1 text-medium-emphasis mb-4">
          Bạn đã hoàn thành tất cả các thẻ ghi nhớ cần ôn tập hôm nay.
        </p>
        <p class="text-body-2 mb-4">
          Hãy quay lại vào ngày mai hoặc thêm thẻ ghi nhớ mới từ danh sách từ
          vựng của bạn.
        </p>
        <div class="d-flex justify-center">
          <v-btn
            color="primary"
            variant="outlined"
            class="mr-2"
            size="small"
            @click="goToVocabulary"
          >
            Khám Phá Từ Vựng
          </v-btn>
          <v-btn color="primary" size="small" @click="refreshDueCards">
            Tải Lại
          </v-btn>
        </div>
      </v-card>
    </v-container>

    <!-- Study Cards -->
    <v-container v-else-if="currentCard" class="py-4">
      <div class="mb-3 d-flex align-center progress-container px-2">
        <div class="text-caption text-medium-emphasis">
          {{ remainingCount }} thẻ còn lại
        </div>
        <v-spacer></v-spacer>
        <v-chip
          :color="getStateColor(currentCard.state)"
          size="x-small"
          class="state-chip"
        >
          {{ getStateText(currentCard.state) }}
        </v-chip>
      </div>

      <!-- Progress bar -->
      <v-progress-linear
        :model-value="getProgressPercent()"
        color="primary"
        height="4"
        rounded
        class="mb-4 mx-2"
      ></v-progress-linear>

      <!-- Flashcard Component - Enhanced Version -->
      <v-card
        class="flashcard-container mx-auto"
        :class="{ flipped: isFlipped }"
        elevation="3"
        @click="flipCard"
        height="350px"
      >
        <div class="flashcard-inner">
          <!-- Front of Card -->
          <div class="flashcard-front">
            <div class="flashcard-content">
              <v-chip
                v-if="currentCard.state"
                size="x-small"
                :color="getStateColor(currentCard.state)"
                class="mb-4 absolute-top-left"
              >
                {{ getStateText(currentCard.state) }}
              </v-chip>

              <div class="text-h4 font-weight-bold japanese-text mb-4">
                {{ currentCard.frontText }}
              </div>

              <v-btn
                icon
                size="small"
                color="primary"
                variant="text"
                @click.stop="playAudio(currentCard)"
                :loading="isPlayingAudio"
                class="mb-4 audio-btn"
              >
                <v-icon>mdi-volume-high</v-icon>
              </v-btn>

              <div class="flip-hint">
                <v-icon
                  icon="mdi-rotate-3d-variant"
                  size="small"
                  class="mr-1"
                ></v-icon>
                <span class="text-caption">Nhấn để lật thẻ</span>
              </div>
            </div>
          </div>

          <!-- Back of Card -->
          <div class="flashcard-back">
            <div class="flashcard-content">
              <div class="text-subtitle-1 font-weight-bold mb-3">
                {{ getBackTitleText(currentCard.backText) }}
              </div>

              <v-divider class="my-3"></v-divider>

              <div
                v-if="hasExample(currentCard.backText)"
                class="example-section pa-3 rounded-lg mb-3"
              >
                <div class="example-content">
                  <div class="example-text-container">
                    <div
                      v-html="getExampleHtml(currentCard.backText)"
                      class="text-body-2"
                    ></div>
                  </div>
                  <v-btn
                    icon
                    size="x-small"
                    color="primary"
                    variant="text"
                    @click.stop="playExampleAudio(currentCard)"
                    :loading="isPlayingExampleAudio"
                    class="example-audio-btn"
                  >
                    <v-icon>mdi-volume-high</v-icon>
                  </v-btn>
                </div>
              </div>

              <div class="term-reminder text-caption text-medium-emphasis mt-2">
                <span class="font-weight-medium">Từ vựng:</span>
                {{ currentCard.frontText }}
              </div>

              <div class="flip-hint">
                <v-icon
                  icon="mdi-rotate-3d-variant"
                  size="small"
                  class="mr-1"
                ></v-icon>
                <span class="text-caption">Nhấn để lật thẻ</span>
              </div>
            </div>
          </div>
        </div>
      </v-card>

      <!-- Rating Buttons -->
      <div class="rating-buttons mt-8 px-2">
        <div class="d-flex justify-space-between w-100">
          <v-btn
            color="error"
            variant="elevated"
            class="rating-btn"
            @click="rateCard(1)"
            :disabled="!isFlipped"
            elevation="1"
            size="large"
            density="comfortable"
          >
            <v-icon icon="mdi-reload" size="small" class="mr-1"></v-icon>
            Again
          </v-btn>
          <v-btn
            color="warning"
            variant="elevated"
            class="rating-btn"
            @click="rateCard(2)"
            :disabled="!isFlipped"
            elevation="1"
            size="large"
            density="comfortable"
          >
            <v-icon icon="mdi-brain" size="small" class="mr-1"></v-icon>
            Hard
          </v-btn>
          <v-btn
            color="success"
            variant="elevated"
            class="rating-btn"
            @click="rateCard(3)"
            :disabled="!isFlipped"
            elevation="1"
            size="large"
            density="comfortable"
          >
            <v-icon icon="mdi-check" size="small" class="mr-1"></v-icon>
            Good
          </v-btn>
          <v-btn
            color="primary"
            variant="elevated"
            class="rating-btn"
            @click="rateCard(4)"
            :disabled="!isFlipped"
            elevation="1"
            size="large"
            density="comfortable"
          >
            <v-icon icon="mdi-star" size="small" class="mr-1"></v-icon>
            Easy
          </v-btn>
        </div>
      </div>
    </v-container>

    <!-- Statistics Dialog -->
    <v-dialog v-model="showStats" max-width="400">
      <v-card>
        <v-card-title class="text-subtitle-1 pt-4 pb-2">
          Thống Kê Học Tập
        </v-card-title>

        <v-card-text v-if="stats">
          <div class="d-flex justify-space-between align-center mb-2">
            <span class="text-body-2">Tổng số thẻ:</span>
            <span class="text-subtitle-2 font-weight-bold">{{
              stats.summary.totalCards || 0
            }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mb-2">
            <span class="text-body-2">Thẻ cần ôn hôm nay:</span>
            <span class="text-subtitle-2 font-weight-bold">{{
              stats.summary.dueCardsNow || 0
            }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mb-2">
            <span class="text-body-2">Ôn tập trong 30 ngày qua:</span>
            <span class="text-subtitle-2 font-weight-bold">{{
              stats.summary.reviewsLast30Days || 0
            }}</span>
          </div>
          <div class="d-flex justify-space-between align-center mb-2">
            <span class="text-body-2">Tỉ lệ ghi nhớ:</span>
            <span class="text-subtitle-2 font-weight-bold">{{
              formatPercent(stats.summary.overallRetentionRate)
            }}</span>
          </div>
        </v-card-text>

        <v-card-actions>
          <v-btn
            color="primary"
            variant="text"
            @click="
              goToStatisticsDashboard();
              showStats = false;
            "
            prepend-icon="mdi-chart-areaspline"
            size="small"
          >
            Xem chi tiết
          </v-btn>
          <v-spacer></v-spacer>
          <v-btn
            color="grey"
            variant="text"
            @click="showStats = false"
            size="small"
          >
            Đóng
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAppToast } from "@/composables/useAppToast";
import flashcardService from "@/services/flashcard.service";
import type { FlashcardDTO } from "@/types/learning.types";
import api from "@/utils/api";

interface StudyStatsSummary {
  totalCards: number;
  dueCardsNow: number;
  reviewsLast30Days: number;
  overallRetentionRate: number;
}

interface StudyStats {
  summary: StudyStatsSummary;
}

// Router and toast
const router = useRouter();
const toast = useAppToast();

// State
const loading = ref(true);
const dueCards = ref<FlashcardDTO[]>([]);
const currentCardIndex = ref(0);
const isFlipped = ref(false);
const isPlayingAudio = ref(false);
const isPlayingExampleAudio = ref(false);
const showStats = ref(false);
const stats = ref<StudyStats | null>(null);

// Computed
const currentCard = computed(() => {
  if (
    dueCards.value.length > 0 &&
    currentCardIndex.value < dueCards.value.length
  ) {
    return dueCards.value[currentCardIndex.value];
  }
  return null;
});

const remainingCount = computed(() => {
  return dueCards.value.length - currentCardIndex.value;
});

// Lifecycle hooks
onMounted(async () => {
  await fetchDueCards();
  await fetchStats();
});

// Methods
async function fetchDueCards() {
  loading.value = true;
  try {
    dueCards.value = await flashcardService.getDueCards();
    currentCardIndex.value = 0;
    isFlipped.value = false;
  } catch {
    toast.error("Không thể tải thẻ ghi nhớ. Vui lòng thử lại sau.");
  } finally {
    loading.value = false;
  }
}

async function fetchStats() {
  try {
    stats.value = (await flashcardService.getStudyStatistics()) as StudyStats;
  } catch {}
}

// Get the title (first line) from back text
function getBackTitleText(text: string) {
  if (!text) return "";
  return text.split("\n")[0];
}

// Check if the back text contains an example
function hasExample(text: string) {
  if (!text) return false;
  return text.includes("Example:");
}

// Format the example part of the back text
function getExampleHtml(text: string) {
  if (!text) return "";

  const lines = text.split("\n");
  const exampleIndex = lines.findIndex((line) => line.includes("Example:"));

  if (exampleIndex === -1) return "";

  // Add the example and translation separately
  if (exampleIndex >= 0 && exampleIndex < lines.length) {
    const exampleText = lines[exampleIndex].replace("Example: ", "");
    let result = `<div class="japanese-text text-h6 mb-2">${exampleText}</div>`;

    if (exampleIndex + 1 < lines.length) {
      result += `<div class="text-body-1 text-medium-emphasis">${lines[exampleIndex + 1]}</div>`;
    }

    return result;
  }

  return "";
}

function flipCard() {
  isFlipped.value = !isFlipped.value;
}

async function rateCard(rating: number) {
  if (!currentCard.value) return;
  const card = currentCard.value;
  const prevIndex = currentCardIndex.value;

  // Optimistic: advance immediately
  currentCardIndex.value++;
  isFlipped.value = false;

  try {
    const response = await flashcardService.reviewFlashcard(card.id, rating);

    // Show success message based on rating with next review date
    const ratingMessages: Record<number, string> = {
      1: "Bạn sẽ gặp lại từ này sớm",
      2: "Từ này khó, sẽ lặp lại sau",
      3: "Tốt! Bạn đã nhớ được từ này",
      4: "Rất tốt! Bạn đã nắm vững từ này",
    };

    // Format next review date
    const nextReview = response?.data?.due
      ? new Date(response.data.due).toLocaleDateString("vi-VN")
      : "soon";

    toast.success(
      `${ratingMessages[rating]} - Lần ôn tiếp theo: ${nextReview}`,
    );

    // Check if we've reached the end
    if (currentCardIndex.value >= dueCards.value.length) {
      setTimeout(() => {
        toast.success(
          "Chúc mừng! Bạn đã hoàn thành tất cả các thẻ cần ôn tập.",
        );
      }, 1000);
    }

    await fetchStats();
  } catch {
    // Rollback
    currentCardIndex.value = prevIndex;
    isFlipped.value = true;
    toast.error("Không thể lưu đánh giá. Vui lòng thử lại.");
  }
}

function getStateColor(state: string) {
  switch (state) {
    case "new":
      return "info";
    case "learning":
      return "warning";
    case "review":
      return "success";
    case "relearning":
      return "error";
    default:
      return "grey";
  }
}

function getStateText(state: string) {
  switch (state) {
    case "new":
      return "Mới";
    case "learning":
      return "Đang học";
    case "review":
      return "Ôn tập";
    case "relearning":
      return "Học lại";
    default:
      return state;
  }
}

async function playAudio(card: FlashcardDTO) {
  if (!card.vocabularyId || isPlayingAudio.value) return;

  isPlayingAudio.value = true;

  try {
    // First try to get the audio URL for this vocabulary term from the API
    try {
      const response = await api.get(
        `/api/v1/speech/vocabulary/${card.vocabularyId}/audio`,
        {
          responseType: "blob",
        },
      );

      const audioBlob = response.data;
      const audioUrl = URL.createObjectURL(audioBlob);
      const audio = new Audio(audioUrl);

      audio.onended = () => {
        isPlayingAudio.value = false;
        URL.revokeObjectURL(audioUrl);
      };

      await audio.play();
      return; // Successfully played the audio, no need to generate TTS
    } catch {
      // Continue to TTS generation if the audio file doesn't exist
    }

    // If we get here, try to generate TTS for the term
    // Show loading indicator
    toast.info("Đang tạo âm thanh...");

    // Call the TTS API with Authorization header
    const response = await api.post("/api/v1/ai/tts/generate", card.frontText, {
      headers: {
        "Content-Type": "text/plain; charset=UTF-8",
        "Accept-Language": "ja-JP",
        "X-Speech-Speed": "0.9",
        "X-Content-Language": "ja",
        "X-Content-Type": "vocabulary",
        Accept: "audio/mpeg",
      },
      responseType: "arraybuffer",
    });

    // Convert response to blob and create audio URL
    const audioBlob = new Blob([response.data], { type: "audio/mpeg" });
    const audioUrl = URL.createObjectURL(audioBlob);

    // Play the audio
    const audio = new Audio(audioUrl);
    audio.onended = () => {
      URL.revokeObjectURL(audioUrl);
      isPlayingAudio.value = false;
    };
    await audio.play();
  } catch {
    toast.error("Không thể phát âm thanh. Vui lòng thử lại sau.");
    isPlayingAudio.value = false;
  } finally {
    // Reset loading state if no audio was played
    setTimeout(() => {
      if (isPlayingAudio.value) {
        isPlayingAudio.value = false;
      }
    }, 3000);
  }
}

// Get the example sentence from the back text
function getExampleSentence(text: string): string {
  if (!text) return "";

  const lines = text.split("\n");
  const exampleIndex = lines.findIndex((line) => line.includes("Example:"));

  if (exampleIndex === -1) return "";

  // Return the example sentence without the "Example: " prefix
  if (exampleIndex >= 0 && exampleIndex < lines.length) {
    return lines[exampleIndex].replace("Example: ", "");
  }

  return "";
}

async function playExampleAudio(card: FlashcardDTO) {
  if (!card.vocabularyId || isPlayingExampleAudio.value) return;

  // Extract the example sentence from the card
  const exampleSentence = getExampleSentence(card.backText);
  if (!exampleSentence) {
    toast.error("Không tìm thấy câu ví dụ");
    return;
  }

  isPlayingExampleAudio.value = true;

  try {
    // Show loading indicator
    toast.info("Đang tạo âm thanh...");

    // Get the backend API URL
    // Call the TTS API with Authorization header for the example sentence
    const response = await api.post(
      "/api/v1/ai/tts/generate",
      exampleSentence,
      {
        headers: {
          "Content-Type": "text/plain; charset=UTF-8",
          "Accept-Language": "ja-JP",
          "X-Speech-Speed": "0.9",
          "X-Content-Language": "ja",
          "X-Content-Type": "example",
          Accept: "audio/mpeg",
        },
        responseType: "arraybuffer",
      },
    );

    // Convert response to blob and create audio URL
    const audioBlob = new Blob([response.data], { type: "audio/mpeg" });
    const audioUrl = URL.createObjectURL(audioBlob);

    // Play the audio
    const audio = new Audio(audioUrl);
    audio.onended = () => {
      URL.revokeObjectURL(audioUrl);
      isPlayingExampleAudio.value = false;
    };
    await audio.play();
  } catch {
    toast.error("Không thể phát âm thanh ví dụ. Vui lòng thử lại sau.");
    isPlayingExampleAudio.value = false;
  } finally {
    // Reset loading state if no audio was played
    setTimeout(() => {
      if (isPlayingExampleAudio.value) {
        isPlayingExampleAudio.value = false;
      }
    }, 3000);
  }
}

function formatPercent(value: number) {
  if (value === undefined || value === null) return "0%";
  return `${Math.round(value)}%`;
}

function refreshDueCards() {
  fetchDueCards();
}

function goToVocabulary() {
  router.push({ name: "vocabulary" });
}

function goBack() {
  router.back();
}

function goToStatisticsDashboard() {
  router.push({ name: "flashcardStatistics" });
}

// Add this new method to compute progress percentage
function getProgressPercent() {
  if (dueCards.value.length === 0) return 0;
  return (
    ((dueCards.value.length - remainingCount.value) / dueCards.value.length) *
    100
  );
}
</script>

<style scoped lang="scss">
.flashcard-study-container {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.app-header {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
}

.back-btn {
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 50%;
}

.progress-container {
  margin-top: 8px;
}

.state-chip {
  font-size: 0.7rem;
  height: 20px;
}

.empty-state-card {
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
}

.empty-icon {
  opacity: 0.8;
}

.flashcard-container {
  perspective: 1000px;
  width: 100%;
  max-width: 500px;
  cursor: pointer;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  margin: 0 auto;
  background: white;
}

.flashcard-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.6s;
  transform-style: preserve-3d;
}

.flashcard-container.flipped .flashcard-inner {
  transform: rotateY(180deg);
}

.flashcard-front,
.flashcard-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  background: linear-gradient(145deg, #ffffff 0%, #f8f8f8 100%);
}

.flashcard-content {
  padding: 1.5rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  position: relative;
}

.flashcard-back {
  transform: rotateY(180deg);
}

.example-section {
  background-color: rgba(0, 0, 0, 0.02);
  border-left: 3px solid #3b82f6;
  border-radius: 8px;
  width: 100%;
  text-align: left;
  transition: all 0.3s ease;
  padding: 0.75rem;
  margin: 0.75rem 0;

  &:hover {
    background-color: rgba(0, 0, 0, 0.03);
  }
}

.japanese-text {
  font-family: "Noto Sans JP", sans-serif;
  letter-spacing: 0.02em;
}

.rating-btn {
  flex: 1;
  margin: 0 4px;
  text-transform: none;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  border-radius: 8px;
  font-size: 0.85rem;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 3px 6px rgba(0, 0, 0, 0.12);
  }

  &:active {
    transform: translateY(1px);
  }
}

.absolute-top-left {
  position: absolute;
  top: 8px;
  left: 8px;
}

.flip-hint {
  position: absolute;
  bottom: 0.75rem;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.4);
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rating-buttons {
  animation: fadeIn 0.4s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.audio-btn {
  padding: 6px;
  transition: all 0.3s ease;
  border-radius: 50%;

  &:hover {
    background-color: rgba(59, 130, 246, 0.1);
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }
}

.example-section .v-btn {
  padding: 4px;
  transition: all 0.3s ease;
}

.example-content {
  display: flex;
  align-items: center;
}

.example-text-container {
  flex: 1;
}

.example-audio-btn {
  margin-left: 12px;
  align-self: center;
}
</style>
