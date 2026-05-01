<template>
  <div class="vocabulary-storage-container">
    <!-- Header with Back Button -->
    <div class="header-container d-flex align-center px-3 py-2">
      <v-btn icon class="mr-2 back-btn" @click="goBack" variant="outlined" color="primary" size="x-small">
        <v-icon>mdi-chevron-left</v-icon>
      </v-btn>
      <h1 class="text-h6 font-weight-bold">Thẻ của tôi</h1>
    </div>

    <!-- Vocabulary Status Tabs -->
    <div class="vocab-status-tabs px-3 py-1">
      <div class="d-flex custom-tabs-container">
        <v-btn
          v-for="(tab, index) in tabs"
          :key="index"
          :color="activeTabIndex === index ? tab.activeColor : 'grey lighten-4'"
          :class="[
            'status-tab',
            'mr-2',
            { 'active-tab': activeTabIndex === index }
          ]"
          @click="activeTabIndex = index"
          :style="activeTabIndex === index ? { color: 'white', boxShadow: '0 4px 8px rgba(0,0,0,0.1)' } : {}"
          rounded="pill"
          variant="flat"
          density="compact"
        >
          {{ tab.name }}
          <span class="tab-count ml-1" v-if="getTabCount(index) > 0">({{ getTabCount(index) }})</span>
        </v-btn>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="text-center my-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="text-body-1 text-medium-emphasis mt-3">Đang tải dữ liệu...</p>
    </div>

    <div v-else-if="!loading && errorMessage" class="text-center my-8">
      <v-alert type="error" class="mx-4">
        {{ errorMessage }}
      </v-alert>
    </div>

    <div v-else-if="!loading && filteredVocabulary.length === 0" class="empty-state-container text-center px-4">
      <v-img
        src="https://cdn.iconscout.com/icon/free/png-256/free-box-1439-1156305.png?f=webp"
        max-width="180"
        class="mx-auto my-12 empty-box-image"
      ></v-img>
      <div class="text-h6 text-grey-darken-1 empty-message mb-8">
        {{ activeTabIndex === 0 ? 'Hiện không có thẻ nào đã được lưu.' : 'Không có thẻ nào trong trạng thái này.' }}
      </div>
    </div>

    <template v-else>
      <!-- Group vocabulary by topic -->
      <div v-for="(group, topic) in groupedVocabulary" :key="topic" class="mt-3">
        <!-- Divider with Topic Information -->
        <div class="category-divider px-3 py-1">
          <div class="d-flex justify-space-between align-center">
            <div class="category-label text-subtitle-2 font-weight-bold" :class="activeTabIndex === 0 ? 'text-primary' : 'text-grey'">
              {{ topic }}
            </div>
            <div class="count-label text-caption rounded-pill pa-1" :class="activeTabIndex === 0 ? 'primary-count' : 'grey-count'">
              {{ group.length }} từ
            </div>
          </div>
          <v-divider :color="activeTabIndex === 0 ? '#3B82F6' : '#e0e0e0'" class="mt-1 custom-divider"></v-divider>
        </div>

        <!-- Vocabulary List -->
        <div class="vocabulary-list px-3 mt-2">
          <div
            v-for="item in group"
            :key="item.vocabId"
            class="vocabulary-item d-flex align-center"
          >
            <v-btn
              icon
              variant="text"
              size="x-small"
              class="mr-2 audio-btn"
              :color="playingAudioId === item.vocabId ? 'primary' : 'grey'"
              :loading="playingAudioId === item.vocabId"
              @click.stop="playAudio(item)"
            >
              <v-icon>mdi-volume-high</v-icon>
            </v-btn>
            <div class="vocabulary-content" @click="openFlashcard(item)">
              <div class="text-subtitle-1 font-weight-bold japanese-text">{{ item.term }}</div>
              <div class="vocabulary-meaning text-caption text-grey">{{ item.meaning }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="d-flex justify-center my-6" v-if="totalPages > 1">
        <v-pagination
          v-model="currentPage"
          :length="totalPages"
          :total-visible="5"
          rounded="circle"
          @update:model-value="fetchSavedVocabulary"
        ></v-pagination>
      </div>
    </template>

    <!-- Bottom Navigation Placeholder -->
    <div class="bottom-navigation-placeholder" style="height: 56px"></div>

    <!-- Flashcard Dialog -->
    <v-dialog v-model="showFlashcard" max-width="600" height="500" persistent>
      <v-card class="flashcard-container" :class="{ 'flipped': isFlipped }">
        <!-- Close button in absolute top right position -->
        <v-btn
          icon="mdi-close"
          variant="text"
          color="red"
          size="medium"
          class="close-btn"
          @click.stop="closeFlashcard"
        ></v-btn>

        <div class="flashcard-inner" @click="flipCard">
          <!-- Front Side (Term) -->
          <div class="flashcard-front">
            <div class="flashcard-content">
              <v-chip
                v-if="currentVocab?.jlptLevel"
                size="small"
                :color="getJlptLevelColor(currentVocab.jlptLevel)"
                class="mb-4 absolute-top-left"
              >
                {{ currentVocab.jlptLevel }}
              </v-chip>

              <div class="text-h3 font-weight-bold japanese-text mb-6">{{ currentVocab?.term || '' }}</div>

              <div v-if="currentVocab?.pronunciation" class="text-h6 japanese-text text-medium-emphasis mb-6">
                {{ currentVocab.pronunciation }}
              </div>

              <div class="flip-hint">
                <v-icon icon="mdi-rotate-3d-variant" class="mr-2"></v-icon>
                <span>Nhấn để lật thẻ</span>
              </div>
            </div>
          </div>

          <!-- Back Side (Meaning) -->
          <div class="flashcard-back">
            <div class="flashcard-content">
              <v-chip
                v-if="currentVocab?.topicName"
                size="small"
                color="info"
                class="mb-2 absolute-top-left"
              >
                {{ currentVocab.topicName }}
              </v-chip>

              <div class="text-h4 font-weight-bold mb-4">{{ currentVocab?.meaning || '' }}</div>

              <v-divider class="my-4"></v-divider>

              <div v-if="currentVocab?.example" class="example-section pa-4 rounded-lg mb-4">
                <div class="japanese-text text-h6 mb-2">{{ currentVocab.example }}</div>
                <div class="text-body-1 text-medium-emphasis">{{ currentVocab.exampleMeaning }}</div>
              </div>

              <div class="term-reminder text-body-2 text-medium-emphasis mt-2">
                <span class="font-weight-medium">Từ vựng:</span> {{ currentVocab?.term }}
              </div>

              <div class="flip-hint">
                <v-icon icon="mdi-rotate-3d-variant" class="mr-2"></v-icon>
                <span>Nhấn để lật thẻ</span>
              </div>
            </div>
          </div>
        </div>

        <v-divider></v-divider>

        <!-- Flashcard Controls -->
        <v-card-actions class="flashcard-controls pa-4">
          <div class="d-flex justify-space-between align-center w-100 mb-3">
            <v-btn
              icon="mdi-volume-high"
              variant="text"
              color="primary"
              size="large"
              @click.stop="currentVocab && playAudio(currentVocab)"
              :loading="playingAudioId === currentVocab?.vocabId"
              :disabled="!currentVocab"
            ></v-btn>
            <div class="text-body-1 text-medium-emphasis">Đánh giá mức độ ghi nhớ</div>
            <div style="width: 48px"></div> <!-- Spacer to balance the layout -->
          </div>

          <div class="d-flex justify-space-between w-100 rating-buttons">
            <v-btn
              color="error"
              variant="elevated"
              class="rating-btn"
              @click.stop="rateCard('again')"
              elevation="2"
            >
              <v-icon icon="mdi-reload" size="small" class="mr-1"></v-icon>
              Again
            </v-btn>
            <v-btn
              color="warning"
              variant="elevated"
              class="rating-btn"
              @click.stop="rateCard('hard')"
              elevation="2"
            >
              <v-icon icon="mdi-brain" size="small" class="mr-1"></v-icon>
              Hard
            </v-btn>
            <v-btn
              color="success"
              variant="elevated"
              class="rating-btn"
              @click.stop="rateCard('good')"
              elevation="2"
            >
              <v-icon icon="mdi-check" size="small" class="mr-1"></v-icon>
              Good
            </v-btn>
            <v-btn
              color="primary"
              variant="elevated"
              class="rating-btn"
              @click.stop="rateCard('easy')"
              elevation="2"
            >
              <v-icon icon="mdi-star" size="small" class="mr-1"></v-icon>
              Easy
            </v-btn>
          </div>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'vue-toast-notification'
import { useVocabularyStore, useAuthStore } from '@/stores'
import type { VocabularyItem } from '@/services/vocabulary.service'
import authService from '@/services/auth.service'
import flashcardService from '@/services/flashcard.service'
import aiService from '@/services/ai.service'

const router = useRouter()
const toast = useToast()
const vocabularyStore = useVocabularyStore()
const authStore = useAuthStore()

// State
const loading = ref(false)
const errorMessage = ref('')
const savedVocabulary = ref<VocabularyItem[]>([])
const filteredVocabulary = ref<VocabularyItem[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalPages = ref(0)
const totalItems = ref(0)
const playingAudioId = ref<string | null>(null)
const showFlashcard = ref(false)
const isFlipped = ref(false)
const currentVocab = ref<VocabularyItem | null>(null)
const vocabFlashcardMap = ref<Map<string, any>>(new Map())

// Tab configuration
const tabs = [
  { name: 'Thẻ mới', activeColor: '#6366F1', state: 0 }, // New
  { name: 'Đang học', activeColor: '#F97316', state: 1 }, // Learning
  { name: 'Ôn tập', activeColor: '#22C55E', state: 2 }, // Review
  { name: 'Học lại', activeColor: '#3B82F6', state: 3 } // Relearning
]

const activeTabIndex = ref(0)

// Computed: Group vocabulary by topic
const groupedVocabulary = computed(() => {
  const groups: Record<string, VocabularyItem[]> = {};

  filteredVocabulary.value.forEach(item => {
    const topic = item.topicName || 'Chưa phân loại';
    if (!groups[topic]) {
      groups[topic] = [];
    }
    groups[topic].push(item);
  });

  return groups;
});

// Lifecycle hooks
onMounted(async () => {
  await fetchSavedVocabulary();
});

// Watch for tab changes
watch(activeTabIndex, () => {
  filterVocabularyByState();
});

// Watch for changes in savedVocabulary
watch(savedVocabulary, () => {
  filterVocabularyByState();
});

// Methods
async function fetchSavedVocabulary() {
  loading.value = true;
  errorMessage.value = '';

  try {
    // Use the store to fetch saved vocabulary
    await vocabularyStore.fetchSavedVocabulary(
      currentPage.value - 1,
      pageSize.value,
      '',  // No search filter
      'date_desc'  // Sort by date, newest first
    );

    // Update local state from store
    savedVocabulary.value = vocabularyStore.savedVocabulary;
    totalItems.value = vocabularyStore.totalSavedItems;
    totalPages.value = vocabularyStore.totalSavedPages;

    // Fetch flashcard states for the vocabulary items
    await fetchFlashcardStates();

  } catch (error) {
    errorMessage.value = 'Không thể tải dữ liệu từ vựng đã lưu.';
    toast.error('Không thể tải dữ liệu từ vựng đã lưu.', {
      position: 'top',
      duration: 3000
    });
  } finally {
    loading.value = false;
  }
}

// Fetch flashcard states for all vocabulary items
async function fetchFlashcardStates() {
  vocabFlashcardMap.value.clear();

  const fetchPromises = savedVocabulary.value.map(async (vocab) => {
    if (!vocab.vocabId) return;

    try {
      const flashcards = await flashcardService.getFlashcardsByVocabulary(vocab.vocabId);
      if (flashcards.length > 0) {
        vocabFlashcardMap.value.set(vocab.vocabId, flashcards[0]);
      }
    } catch (error) {
    }
  });

  await Promise.all(fetchPromises);

  // Initial filtering
  filterVocabularyByState();
}

// Filter vocabulary by flashcard state
function filterVocabularyByState() {
  const selectedState = tabs[activeTabIndex.value].state;

  if (activeTabIndex.value === 0 && vocabFlashcardMap.value.size === 0) {
    // If on first tab and no flashcards loaded yet, show all
    filteredVocabulary.value = savedVocabulary.value;
    return;
  }

  filteredVocabulary.value = savedVocabulary.value.filter(vocab => {
    if (!vocab.vocabId) return false;

    const flashcard = vocabFlashcardMap.value.get(vocab.vocabId);

    if (!flashcard) {
      // Vocabulary items without flashcards only show in the first tab (New)
      return activeTabIndex.value === 0;
    }

    // Match flashcard state with tab state
    return flashcard.state === getStateNameFromValue(selectedState);
  });
}

// Helper function to convert state number to string name
function getStateNameFromValue(state: number): string {
  switch (state) {
    case 0: return 'new';
    case 1: return 'learning';
    case 2: return 'review';
    case 3: return 'relearning';
    default: return 'new';
  }
}

// Method to go back
function goBack() {
  router.back();
}

// Method to play audio
async function playAudio(item: VocabularyItem) {
  // Set the ID of the item we're playing audio for
  playingAudioId.value = item.vocabId;

  try {
    if (item.audioPath) {
      // If there's an existing audio path, use it
      const audio = new Audio(item.audioPath);
      await audio.play();
    } else {
      // Verify authentication before proceeding
      if (!authStore.isAuthenticated) {
        toast.error('Vui lòng đăng nhập để sử dụng tính năng phát âm', {
          position: 'top',
          duration: 3000
        });
        return;
      }

      // Show loading indicator
      toast.info('Đang tạo âm thanh...', {
        position: 'top',
        duration: 2000
      });

      // Use AI service to generate and play audio
      const textToSpeak = item.pronunciation || item.term;
      const audioBlob = await aiService.generateTTS(textToSpeak, 'vocabulary', 0.9, true);

      // Create custom audio element to track playback state
      const audioUrl = URL.createObjectURL(audioBlob);
      const audio = new Audio(audioUrl);

      audio.onended = () => {
        URL.revokeObjectURL(audioUrl);
        playingAudioId.value = null;
      };

      await audio.play();
    }
  } catch (error) {
    toast.error('Không thể phát âm thanh. Vui lòng thử lại sau.', {
      position: 'top',
      duration: 3000
    });
  } finally {
    // Set audio ID to null if there was an issue
    if (item.audioPath) {
      playingAudioId.value = null;
    } else {
      // For TTS, this is handled by the audio.onended callback
      // But set a timeout as fallback
      setTimeout(() => {
        playingAudioId.value = null;
      }, 5000);
    }
  }
}

function openFlashcard(item: VocabularyItem) {
  currentVocab.value = item
  isFlipped.value = false
  showFlashcard.value = true
}

function closeFlashcard() {
  showFlashcard.value = false
  setTimeout(() => {
    currentVocab.value = null
    isFlipped.value = false
  }, 300)
}

function flipCard() {
  isFlipped.value = !isFlipped.value
}

async function rateCard(rating: 'again' | 'hard' | 'good' | 'easy') {
  if (!currentVocab.value?.vocabId) return;

  // Convert text ratings to numeric values for FSRS (1-4 scale where 1 is Again, 4 is Easy)
  const ratingMap = {
    again: 1, // Hardest rating
    hard: 2,
    good: 3,
    easy: 4   // Easiest rating
  };

  const numericRating = ratingMap[rating];
  loading.value = true;

  try {
    // Get the flashcards for this vocabulary
    const flashcards = await flashcardService.getFlashcardsByVocabulary(currentVocab.value.vocabId);

    if (flashcards.length > 0) {
      // Use the first flashcard found for this vocabulary
      const flashcard = flashcards[0];

      // Submit the rating with the flashcard ID
      const response = await flashcardService.reviewFlashcard(flashcard.id, numericRating);

      // Show success message based on rating
      const ratingMessages = {
        again: 'Bạn sẽ gặp lại từ này sớm',
        hard: 'Từ này khó, sẽ lặp lại sau',
        good: 'Tốt! Bạn đã nhớ được từ này',
        easy: 'Rất tốt! Bạn đã nắm vững từ này'
      };

      // Update next scheduled date in the UI if available
      const nextReview = response?.data?.due ?
        new Date(response.data.due).toLocaleDateString('vi-VN') :
        'soon';

      toast.success(`${ratingMessages[rating]} - Lần ôn tiếp theo: ${nextReview}`, {
        position: 'top',
        duration: 3000
      });

      // Close the flashcard after rating
      closeFlashcard();
    } else {
      toast.warning('Không tìm thấy thẻ ghi nhớ cho từ vựng này', {
        position: 'top',
        duration: 3000
      });
    }
  } catch (error) {
    toast.error('Không thể lưu đánh giá cho thẻ này. Vui lòng thử lại.', {
      position: 'top',
      duration: 3000
    });
  } finally {
    loading.value = false;
  }
}

function getJlptLevelColor(level: string): string {
  switch (level) {
    case 'N1': return 'deep-purple';
    case 'N2': return 'indigo';
    case 'N3': return 'blue';
    case 'N4': return 'teal';
    case 'N5': return 'green';
    default: return 'grey';
  }
}

// Function to get count of vocabulary for each tab
function getTabCount(index: number): number {
  const selectedState = tabs[index].state;

  if (index === 0 && vocabFlashcardMap.value.size === 0) {
    // If first tab and no flashcards loaded yet
    return savedVocabulary.value.length;
  }

  if (index === 0) {
    // For the first tab (New), include both new flashcards and vocabularies without flashcards
    return savedVocabulary.value.filter(vocab => {
      if (!vocab.vocabId) return false;
      const flashcard = vocabFlashcardMap.value.get(vocab.vocabId);
      return !flashcard || flashcard.state === 'new';
    }).length;
  }

  // For other tabs, only count flashcards with the matching state
  return savedVocabulary.value.filter(vocab => {
    if (!vocab.vocabId) return false;
    const flashcard = vocabFlashcardMap.value.get(vocab.vocabId);
    return flashcard && flashcard.state === getStateNameFromValue(selectedState);
  }).length;
}
</script>

<style scoped lang="scss">
.vocabulary-storage-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-top: 4px;
}

.header-container {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: white;
  margin-bottom: 4px;
  position: sticky;
  top: 0;
  z-index: 5;
}

.back-btn {
  border-radius: 50%;
}

.custom-tabs-container {
  overflow-x: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari, Opera */
  }
}

.status-tab {
  padding: 6px 16px;
  font-weight: 500;
  transition: all 0.3s ease;
  border-radius: 50px;
  text-transform: none;
  letter-spacing: 0;
  min-width: 100px;

  &.active-tab {
    font-weight: 600;
    transform: translateY(-2px);
  }
}

.tab-count {
  font-size: 0.8rem;
  opacity: 0.9;
  margin-left: 2px;
}

.primary-count {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3B82F6;
  font-weight: 500;
  font-size: 0.75rem;
}

.grey-count {
  background-color: rgba(0, 0, 0, 0.05);
  color: #616161;
  font-weight: 500;
  font-size: 0.75rem;
}

.custom-divider {
  height: 2px;
  border-radius: 2px;
}

.vocabulary-item {
  margin-bottom: 8px;
  padding: 10px;
  cursor: pointer;
  background-color: white;
  border-radius: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  }
}

.audio-btn {
  transition: all 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }
}

.vocabulary-meaning {
  margin-top: 2px;
}

.empty-box-image {
  opacity: 0.7;
  max-width: 120px !important;
  margin: 30px auto !important;
}

.empty-message {
  color: #757575;
  font-size: 0.9rem;
}

.vocabulary-content {
  cursor: pointer;
  flex-grow: 1;
}

.japanese-text {
  font-family: 'Noto Sans JP', sans-serif;
  letter-spacing: 0.02em;
}

.flashcard-container {
  perspective: 1000px;
  height: 500px;
  width: 100%;
  cursor: pointer;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.flashcard-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.8s;
  transform-style: preserve-3d;
}

.flashcard-container.flipped .flashcard-inner {
  transform: rotateY(180deg);
}

.flashcard-front, .flashcard-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
}

.flashcard-content {
  padding: 2.5rem;
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
  background-color: rgba(0, 0, 0, 0.03);
  border-left: 4px solid #3f51b5;
  border-radius: 10px;
  width: 100%;
  text-align: left;
  transition: all 0.3s ease;
  padding: 1rem;
  margin: 1rem 0;
}

.example-section:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.rating-btn {
  flex: 1;
  margin: 0 6px;
  text-transform: none;
  font-weight: 500;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.12);
  transition: all 0.2s ease;
  height: 44px;
  border-radius: 10px;
}

.rating-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.absolute-top-left {
  position: absolute;
  top: 12px;
  left: 12px;
}

.flip-hint {
  position: absolute;
  bottom: 1rem;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.5);
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flashcard-controls {
  background-color: #fafafa;
}

.rating-buttons {
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}
</style>
