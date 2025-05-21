<template>
  <v-container fluid class="vocabulary-container pa-0">
    <!-- Header Section -->
    <div class="header px-4 py-3 d-flex justify-space-between align-center mb-2">
      <div class="d-flex align-center">
        <div class="text-subtitle-1 font-weight-bold text-dark">
          <span class="text-primary">Từ vựng tiếng Nhật</span>
          <span class="japanese-text ms-2">単語学習</span>
        </div>
      </div>
    </div>

    <!-- Search and Filter Bar -->
    <div class="search-filter-container px-4 mb-4">
      <v-card class="filter-card elevation-1 rounded-lg">
        <v-card-text>
          <v-row align="center">
            <!-- Search Box -->
            <v-col cols="12" sm="4">
              <div class="search-field d-flex align-center pa-1 rounded-pill">
                <v-text-field
                  v-model="filters.keyword"
                  prepend-inner-icon="mdi-magnify"
                  placeholder="検索 / Tìm kiếm từ vựng"
                  clearable
                  hide-details
                  density="comfortable"
                  variant="plain"
                  @update:model-value="debounceSearch"
                ></v-text-field>
              </div>
            </v-col>

            <!-- JLPT Level Filter -->
            <v-col cols="12" sm="3">
              <v-select
                v-model="filters.jlptLevel"
                label="Trình độ JLPT"
                :items="jlptLevels"
                clearable
                hide-details
                density="comfortable"
                variant="outlined"
                rounded="lg"
                @update:model-value="fetchVocabulary"
              ></v-select>
            </v-col>

            <!-- Topic Filter -->
            <v-col cols="12" sm="5">
              <v-select
                v-model="filters.topicName"
                label="Chủ đề"
                :items="topics"
                item-title="name"
                item-value="name"
                clearable
                hide-details
                density="comfortable"
                variant="outlined"
                rounded="lg"
                @update:model-value="fetchVocabulary"
              ></v-select>
            </v-col>
          </v-row>

          <!-- Active Filters -->
          <v-row class="mt-3" v-if="hasActiveFilters">
            <v-col cols="12">
              <div class="d-flex flex-wrap align-center">
                <span class="text-body-2 text-medium-emphasis mr-2">Bộ lọc đang dùng:</span>
                <v-chip
                  v-if="filters.keyword"
                  size="small"
                  class="mr-2 mb-1"
                  closable
                  @click:close="clearFilter('keyword')"
                >
                  Tìm kiếm: {{ filters.keyword }}
                </v-chip>
                <v-chip
                  v-if="filters.jlptLevel"
                  size="small"
                  color="primary"
                  class="mr-2 mb-1"
                  closable
                  @click:close="clearFilter('jlptLevel')"
                >
                  Trình độ: {{ filters.jlptLevel }}
                </v-chip>
                <v-chip
                  v-if="filters.topicName"
                  size="small"
                  color="success"
                  class="mr-2 mb-1"
                  closable
                  @click:close="clearFilter('topicName')"
                >
                  Chủ đề: {{ filters.topicName }}
                </v-chip>
                <v-btn
                  size="small"
                  variant="text"
                  density="comfortable"
                  @click="clearAllFilters"
                >
                  Xóa tất cả
                </v-btn>
              </div>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="d-flex justify-center my-8">
      <v-progress-circular
        indeterminate
        color="primary"
        size="64"
      ></v-progress-circular>
    </div>

    <!-- Vocabulary Content -->
    <div v-else class="vocabulary-content px-4">
      <div v-if="vocabulary.length === 0" class="text-center pa-6">
        <v-icon size="large" icon="mdi-book-search-outline" class="mb-4"></v-icon>
        <h3 class="text-h6">Không tìm thấy từ vựng</h3>
        <p class="text-body-1 text-medium-emphasis mb-4">
          Hãy điều chỉnh bộ lọc tìm kiếm hoặc thêm từ vựng mới.
        </p>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="router.push('/admin/vocabulary')">
          Thêm từ vựng
        </v-btn>
      </div>

      <div v-else>
        <!-- Vocabulary Cards -->
        <v-card
          v-for="item in vocabulary"
          :key="item.vocabId"
          class="vocabulary-card mb-3"
          :class="{ 'expanded': expandedItems.includes(item.vocabId) }"
          variant="outlined"
          rounded="lg"
          @click="toggleExpand(item.vocabId)"
        >
          <!-- Main vocabulary item content -->
          <div class="pa-3">
            <div class="d-flex align-center justify-space-between">
              <div class="item-text">
                <div class="term-container">
                  <div class="text-subtitle-2 japanese-text font-weight-bold">{{ item.term }}</div>
                  <div v-if="item.pronunciation" class="japanese-text d-none d-md-inline-block ml-2">
                    ({{ item.pronunciation }})
                  </div>
                  <v-chip
                    :color="getJlptColor(item.jlptLevel)"
                    size="x-small"
                    class="ml-2"
                  >
                    {{ item.jlptLevel }}
                  </v-chip>
                </div>
                <!-- Mobile pronunciation display - only shows on mobile -->
                <div v-if="item.pronunciation" class="text-body-2 japanese-text text-medium-emphasis d-md-none pronunciation-mobile">
                  {{ item.pronunciation }}
                </div>
                <div class="text-body-2 meaning-text mt-1">{{ item.meaning }}</div>
              </div>
              <v-spacer></v-spacer>

              <!-- Audio button - always visible -->
              <v-btn
                icon="mdi-volume-high"
                size="small"
                variant="text"
                @click.stop="playAudio(item.audioPath || null, item)"
                class="mx-1 audio-btn"
                color="blue"
                title="Phát âm"
                :loading="playingAudioId === item.vocabId"
                :disabled="playingAudioId !== null && playingAudioId !== item.vocabId || playingExampleAudioId !== null"
              ></v-btn>

              <!-- Desktop buttons - hidden on mobile -->
              <div class="action-buttons-container d-none d-md-flex" @click.stop>
                <v-btn
                  :icon="item.isSaved ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
                  size="small"
                  variant="text"
                  :color="item.isSaved ? 'warning' : undefined"
                  @click.stop="toggleSave(item)"
                  class="mx-1"
                  :title="item.isSaved ? 'Bỏ lưu' : 'Lưu từ vựng'"
                ></v-btn>
                <v-btn
                  variant="text"
                  color="primary"
                  size="small"
                  @click.stop="viewDetails(item)"
                  class="mx-1"
                  title="Xem chi tiết"
                  icon="mdi-open-in-new"
                ></v-btn>
                <v-btn
                  size="small"
                  variant="text"
                  color="success"
                  @click.stop="toggleChatGPT(item.vocabId)"
                  class="chat-gpt-btn mx-1"
                  :disabled="loadingChatGPT === item.vocabId"
                  :loading="loadingChatGPT === item.vocabId"
                  prepend-icon="mdi-chat-processing"
                >
                  <span class="d-none d-sm-inline">Hỏi ChatGPT</span>
                </v-btn>
              </div>

              <!-- Mobile Actions Menu - excluding audio button -->
              <div class="mobile-actions d-md-none" @click.stop>
                <v-menu>
                  <template v-slot:activator="{ props }">
                    <v-btn
                      icon="mdi-dots-vertical"
                      size="small"
                      variant="text"
                      v-bind="props"
                      @click.stop
                    ></v-btn>
                  </template>
                  <v-list density="compact">
                    <v-list-item @click.stop="toggleSave(item)">
                      <template v-slot:prepend>
                        <v-icon :color="item.isSaved ? 'warning' : undefined">
                          {{ item.isSaved ? 'mdi-bookmark' : 'mdi-bookmark-outline' }}
                        </v-icon>
                      </template>
                      <v-list-item-title>{{ item.isSaved ? 'Bỏ lưu' : 'Lưu từ vựng' }}</v-list-item-title>
                    </v-list-item>

                    <v-list-item @click.stop="viewDetails(item)">
                      <template v-slot:prepend>
                        <v-icon color="primary">mdi-open-in-new</v-icon>
                      </template>
                      <v-list-item-title>Xem chi tiết</v-list-item-title>
                    </v-list-item>

                    <v-list-item @click.stop="toggleChatGPT(item.vocabId)" :disabled="loadingChatGPT === item.vocabId">
                      <template v-slot:prepend>
                        <v-icon color="success">mdi-chat-processing</v-icon>
                      </template>
                      <v-list-item-title>Hỏi ChatGPT</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-menu>
              </div>
            </div>

            <!-- Expanded Content -->
            <div v-if="expandedItems.includes(item.vocabId)" class="mt-2" @click.stop>
              <v-divider class="mb-2"></v-divider>

              <!-- Example Section -->
              <div v-if="item.example" class="example-section mb-2 pa-2 rounded">
                <div class="d-flex align-items-center">
                  <v-icon class="mr-1" size="x-small" color="grey">mdi-format-quote-open</v-icon>
                  <div class="flex-grow-1 japanese-text text-body-2">{{ item.example }}</div>
                  <v-btn
                    icon="mdi-volume-high"
                    size="x-small"
                    variant="text"
                    @click.stop="playAudio(null, item, true)"
                    color="blue"
                    title="Phát âm câu ví dụ"
                    :loading="playingExampleAudioId === item.vocabId"
                    :disabled="playingExampleAudioId !== null && playingExampleAudioId !== item.vocabId || playingAudioId !== null"
                  ></v-btn>
                </div>
                <div v-if="item.exampleMeaning" class="example-translation ml-4 mt-1 text-caption text-medium-emphasis">
                  {{ item.exampleMeaning }}
                </div>
              </div>

              <div class="additional-info d-flex flex-wrap mt-2">
                <div class="mr-4 mb-1">
                  <span class="text-caption text-medium-emphasis">Ngày tạo:</span>
                  <span class="text-caption ml-1">{{ formatDate(item.createdAt ?? '1999') }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- ChatGPT Content (completely independent section) -->
          <div v-if="chatGPTItems.includes(item.vocabId)" class="pa-3 pt-0" @click.stop>
            <div class="chatgpt-content py-2 px-2 rounded" @click.stop>
              <v-divider class="mb-2"></v-divider>

              <!-- Initial AI Explanation -->
              <v-card flat class="chatgpt-card pa-2 mb-2" v-if="item.aiExplanation" @click.stop>
                <div class="d-flex align-items-start mb-1">
                  <v-avatar size="24" color="green" class="mr-2 mt-1">
                    <span class="text-caption text-white">AI</span>
                  </v-avatar>
                  <div>
                    <div class="text-caption font-weight-medium">Trợ lý ChatGPT</div>
                    <div class="chatgpt-message text-body-2 mt-1">
                      <p v-html="displayedText[item.vocabId] || ''"></p>
                      <span v-if="typingInProgress === item.vocabId && typingExamples[item.vocabId] === undefined" class="typing-cursor">|</span>

                      <div v-if="item.aiExamples && item.aiExamples.length > 0 && typingExamples[item.vocabId] !== undefined" class="mt-2">
                        <p class="font-weight-medium text-caption">Câu ví dụ:</p>
                        <div v-for="(example, exIndex) in item.aiExamples" :key="exIndex" class="mt-1">
                          <template v-if="typingExamples[item.vocabId] > exIndex || typingExamples[item.vocabId] === -1">
                            <!-- Fully displayed examples -->
                            <p class="example-text text-caption">{{ example.japanese }}</p>
                            <p class="text-caption ml-3">{{ example.vietnamese }}</p>
                          </template>
                          <template v-else-if="typingExamples[item.vocabId] === exIndex">
                            <!-- Currently typing example -->
                            <p class="example-text text-caption">
                              {{ getExampleProperty(example, 'japaneseDisplayed') || '' }}
                              <span v-if="isJapaneseComplete(example) && !hasVietnameseStarted(example)" class="typing-cursor">|</span>
                            </p>
                            <p v-if="hasVietnameseStarted(example)" class="text-caption ml-3">
                              {{ getExampleProperty(example, 'vietnameseDisplayed') || '' }}
                              <span class="typing-cursor">|</span>
                            </p>
                          </template>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </v-card>

              <!-- Chat History -->
              <template v-if="item.chatHistory && item.chatHistory.length > 0">
                <div
                  v-for="(message, msgIndex) in item.chatHistory"
                  :key="msgIndex"
                  class="mb-2"
                >
                  <!-- User Message -->
                  <div v-if="message.role === 'user'" class="d-flex align-items-start">
                    <v-avatar size="24" color="blue" class="mr-2 mt-1">
                      <span class="text-caption text-white">Bạn</span>
                    </v-avatar>
                    <div>
                      <div class="text-caption font-weight-medium">Bạn</div>
                      <div class="chatgpt-message text-body-2 mt-1">
                        {{ message.content }}
                      </div>
                    </div>
                  </div>

                  <!-- AI Response -->
                  <v-card v-else flat class="chatgpt-card pa-2">
                    <div class="d-flex align-items-start">
                      <v-avatar size="24" color="green" class="mr-2 mt-1">
                        <span class="text-caption text-white">AI</span>
                      </v-avatar>
                      <div>
                        <div class="text-caption font-weight-medium">Trợ lý ChatGPT</div>
                        <div class="chatgpt-message text-body-2 mt-1">
                          <span v-html="message.content"></span>
                          <span v-if="typingInProgress === item.vocabId &&
                                      msgIndex === (item.chatHistory?.length - 1)"
                                class="typing-cursor">|</span>
                        </div>
                      </div>
                    </div>
                  </v-card>
                </div>
              </template>

              <!-- User Input -->
              <div class="chat-input-container d-flex mt-3">
                <v-text-field
                  v-model="chatInputs[item.vocabId]"
                  placeholder="Nhập tin nhắn của bạn..."
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  @keyup.enter="sendChatMessage(item.vocabId)"
                  class="mr-2"
                ></v-text-field>
                <v-btn
                  color="primary"
                  :disabled="!chatInputs[item.vocabId]"
                  @click.stop="sendChatMessage(item.vocabId)"
                >
                  <v-icon>mdi-send</v-icon>
                </v-btn>
              </div>
            </div>
          </div>
        </v-card>

        <!-- Bottom pagination & Item per page selector -->
        <div class="d-flex align-center justify-space-between mt-4">
          <v-select
            v-model="filters.size"
            label="Số mục trên trang"
            :items="[10, 20, 50, 100]"
            density="compact"
            variant="outlined"
            style="max-width: 150px"
            @update:model-value="changeItemsPerPage"
            hide-details
          ></v-select>

          <v-pagination
            v-if="totalPages > 1"
            v-model="currentPage"
            :length="totalPages"
            :total-visible="7"
            rounded="circle"
            @update:model-value="changePage"
          ></v-pagination>

          <div v-else class="text-caption text-medium-emphasis">
            Hiển thị {{ vocabulary.length }} mục
          </div>

          <!-- Add Vocabulary Button -->
          <v-btn
            color="primary"
            prepend-icon="mdi-plus"
            @click="openAddDialog"
          >
            Thêm từ vựng
          </v-btn>
        </div>
      </div>
    </div>
  </v-container>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useToast } from 'vue-toast-notification'
import { useRouter, useRoute } from 'vue-router'
import vocabularyService from '@/services/vocabulary.service'
import authService from '@/services/auth.service'
import type { VocabularyItem, VocabularyFilter, ExampleSentence, ChatMessage } from '@/services/vocabulary.service'
import axios from 'axios'

// Router và route
const router = useRouter()
const route = useRoute()
const toast = useToast()

// Extended interface for examples with typing animation
interface AnimatedExample {
  japanese: string;
  vietnamese: string;
  japaneseDisplayed?: string;
  vietnameseDisplayed?: string;
}

// State
const vocabulary = ref<VocabularyItem[]>([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(0)
const totalItems = ref(0)
const expandedItems = ref<string[]>([])
const chatGPTItems = ref<string[]>([])
const loadingChatGPT = ref<string | null>(null)
const chatInputs = ref<Record<string, string>>({})
const playingAudioId = ref<string | null>(null)
const playingExampleAudioId = ref<string | null>(null)
const typingInProgress = ref<string | null>(null)
const displayedText = ref<Record<string, string>>({})
const typingSpeed = ref(20) // ms between characters
const typingExamples = ref<Record<string, number>>({}) // Track which example is currently being animated

const filters = reactive({
  keyword: null as string | null,
  jlptLevel: null as string | null,
  topicName: null as string | null,
  page: 0,
  size: 10
} as VocabularyFilter)

// Filter options
const jlptLevels = ref(['N1', 'N2', 'N3', 'N4', 'N5'])
const topics = ref<any[]>([])

// Computed Properties
const hasActiveFilters = computed(() => {
  return !!(
    filters.keyword ||
    filters.jlptLevel ||
    filters.topicName
  )
})

// Methods
const toggleExpand = (vocabId: string) => {
  const index = expandedItems.value.indexOf(vocabId);

  if (index >= 0) {
    // If already expanded, collapse it
    expandedItems.value.splice(index, 1);
  } else {
    // If not expanded, expand it
    expandedItems.value.push(vocabId);
  }
}

const formatDate = (dateString?: string): string => {
  if (!dateString) return 'Unknown';

  try {
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  } catch (e) {
    return dateString;
  }
}

const fetchVocabulary = async () => {
  loading.value = true;

  try {
    console.log('Fetching vocabulary with filters:', filters);
    const response = await vocabularyService.getVocabulary(filters);
    console.log('Vocabulary response:', response);

    if (response && Array.isArray(response.content)) {
      vocabulary.value = response.content;
      totalPages.value = response.totalPages;
      totalItems.value = response.totalElements;
      console.log('Processed vocabulary items:', vocabulary.value.length);
    } else {
      console.error('Invalid response format:', response);
      vocabulary.value = [];
      totalPages.value = 0;
      totalItems.value = 0;
      toast.error('Received invalid data format from server', {
        position: 'top',
        duration: 3000
      });
    }
  } catch (error) {
    console.error('Error fetching vocabulary:', error);
    vocabulary.value = [];
    totalPages.value = 0;
    totalItems.value = 0;
    toast.error('Failed to load vocabulary items', {
      position: 'top',
      duration: 3000
    });
  } finally {
    loading.value = false;
  }
}

// Debounce function
const debounce = (fn: Function, delay: number) => {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  return function(this: any, ...args: any[]) {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
    timeoutId = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}

const debounceSearch = debounce(() => {
  fetchVocabulary();
}, 500);

const fetchTopics = async () => {
  try {
    topics.value = await vocabularyService.getTopics();
  } catch (error) {
    console.error('Error fetching topics:', error);
  }
}

const changePage = (page: number) => {
  currentPage.value = page;
  filters.page = page - 1; // API uses 0-based indexing
  expandedItems.value = []; // Close all expanded items when changing page
  fetchVocabulary();
}

const changeItemsPerPage = (size: number) => {
  // Ensure size is a positive number
  const validSize = size > 0 ? size : 10;
  filters.size = validSize;
  filters.page = 0;
  currentPage.value = 1;
  expandedItems.value = []; // Close all expanded items when changing items per page
  fetchVocabulary();
}

const clearFilter = (filterName: 'keyword' | 'jlptLevel' | 'topicName') => {
  filters[filterName] = null;
  fetchVocabulary();
}

const clearAllFilters = () => {
  filters.keyword = null;
  filters.jlptLevel = null;
  filters.topicName = null;
  filters.page = 0;
  currentPage.value = 1;
  fetchVocabulary();
}

const toggleSave = async (item: VocabularyItem) => {
  try {
    if (item.isSaved) {
      await vocabularyService.removeSavedVocabulary(item.vocabId);
      toast.success('Removed from saved items', {
        position: 'top',
        duration: 2000
      });
    } else {
      await vocabularyService.saveVocabulary(item.vocabId);
      toast.success('Added to saved items', {
        position: 'top',
        duration: 2000
      });
    }

    // Toggle the state locally
    item.isSaved = !item.isSaved;

  } catch (error) {
    console.error('Error toggling save status:', error);
    toast.error('Failed to update saved status', {
      position: 'top',
      duration: 3000
    });
  }
}

const getJlptColor = (level: string): string => {
  switch (level) {
    case 'N1': return 'red';
    case 'N2': return 'orange';
    case 'N3': return 'amber';
    case 'N4': return 'light-green';
    case 'N5': return 'green';
    default: return 'grey';
  }
}

const playAudio = async (audioPath: string | null, vocabItem?: VocabularyItem, isExampleSentence: boolean = false): Promise<void> => {
  if (!vocabItem) {
    toast.error('Could not identify vocabulary item');
    return;
  }

  // Set loading state based on whether it's a word or example
  if (isExampleSentence) {
    playingExampleAudioId.value = vocabItem.vocabId;
  } else {
    playingAudioId.value = vocabItem.vocabId;
  }

  try {
    if (audioPath) {
      // If there's an existing audio path, use it
      const audio = new Audio(audioPath);
      await audio.play();
    } else {
      // Verify authentication before proceeding
      const authToken = authService.getToken();
      if (!authToken) {
        toast.error('Please log in to use text-to-speech', {
          position: 'top',
          duration: 4000
        });
        // Redirect to login page after a short delay
        setTimeout(() => {
          router.push({
            name: 'login',
            query: { redirect: route.fullPath }
          });
        }, 1500);
        return;
      }

      // No audio path available, use TTS API
      // Get the item either from the parameter or by finding it in the vocabulary array
      const currentItem = vocabItem || getCurrentItemFromEvent();

      if (!currentItem) {
        toast.warning('Could not identify vocabulary item', {
          position: 'top',
          duration: 3000
        });
        return;
      }

      // Determine which text to use for speech
      let textToSpeak = '';

      if (isExampleSentence && currentItem.example) {
        // Use example sentence if requested and available
        textToSpeak = currentItem.example;
      } else {
        // Use term for vocabulary pronunciation
        textToSpeak = currentItem.term;
      }

      // Show loading indicator
      toast.info('Generating audio...', {
        position: 'top',
        duration: 2000
      });

      // Get the backend API URL
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

      // Set speed: 0.9 for vocabulary, 1.0 for example sentences
      const speed = isExampleSentence ? 1.0 : 0.9;

      // Call the TTS API with Authorization header
      const response = await axios.post(`${apiUrl}/ai-service-api/v1/tts/generate`, textToSpeak, {
        headers: {
          'Content-Type': 'text/plain; charset=UTF-8',
          'Accept-Language': 'ja-JP',
          'X-Speech-Speed': speed.toString(),
          'X-Content-Language': 'ja',
          'X-Content-Type': isExampleSentence ? 'example' : 'vocabulary',
          'Authorization': `Bearer ${authToken}`,
          'Accept': 'audio/mpeg'
        },
        responseType: 'arraybuffer'
      });

      // Convert response to blob and create audio URL
      const audioBlob = new Blob([response.data], { type: 'audio/mpeg' });
      const audioUrl = URL.createObjectURL(audioBlob);

      // Play the audio
      const audio = new Audio(audioUrl);
      audio.onended = () => {
        URL.revokeObjectURL(audioUrl);
        if (isExampleSentence) {
          playingExampleAudioId.value = null;
        } else {
          playingAudioId.value = null;
        }
      };

      await audio.play();
    }
  } catch (error) {
    console.error('Error generating or playing TTS audio:', error);

    // Special handling for 401 errors to prevent logout
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      toast.error('TTS service requires authentication. Please log in again when convenient.', {
        position: 'top',
        duration: 3000
      });
    } else {
      toast.error(error instanceof Error ? error.message : 'Failed to generate speech', {
        position: 'top',
        duration: 3000
      });
    }
  } finally {
    // Reset loading state if no audio was played (in case of error)
    // For successful audio playback, the onended event will handle this
    if (!audioPath) {
      setTimeout(() => {
        if (isExampleSentence) {
          playingExampleAudioId.value = null;
        } else {
          playingAudioId.value = null;
        }
      }, 3000);
    } else {
      if (isExampleSentence) {
        playingExampleAudioId.value = null;
      } else {
        playingAudioId.value = null;
      }
    }
  }
}

// Helper method to get current item from event context
const getCurrentItemFromEvent = (): VocabularyItem | null => {
  // This relies on the event coming from a button inside a list item
  // The list item has the vocab ID
  const button = event?.target as HTMLElement;
  const listItem = button?.closest('.vocabulary-item');
  const vocabId = listItem?.getAttribute('data-vocab-id');

  if (vocabId) {
    return vocabulary.value.find(item => item.vocabId === vocabId) || null;
  }

  return null;
}

const viewDetails = (item: VocabularyItem) => {
  router.push({
    name: 'vocabularyDetail',
    params: {
      term: encodeURIComponent(item.term)
    }
  });
}

const openAddDialog = () => {
  // To be implemented - open dialog to add new vocabulary
  // This is a placeholder for future functionality
}

const toggleChatGPT = (vocabId: string) => {
  const index = chatGPTItems.value.indexOf(vocabId);

  if (index >= 0) {
    // If already open, close it
    chatGPTItems.value.splice(index, 1);
  } else {
    // Start loading state
    loadingChatGPT.value = vocabId;

    // Find the vocabulary item by ID
    const vocabItem = vocabulary.value.find(item => item.vocabId === vocabId);

    if (!vocabItem) {
      console.error('Vocabulary item not found');
      loadingChatGPT.value = null;
      return;
    }

    // Initialize chat input for this item if not exists
    if (!chatInputs.value[vocabId]) {
      chatInputs.value[vocabId] = '';
    }

    // Get the backend API URL
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

    // Query the AI for vocabulary explanation
    axios.post(`${apiUrl}/ai-service-api/v1/chat/vocabulary/explain`, null, {
      params: {
        term: vocabItem.term || '',
        pronunciation: vocabItem.pronunciation || '',
        meaning: vocabItem.meaning || '',
        topicName: vocabItem.topicName || '',
        example: vocabItem.example || ''
      }
    })
    .then(response => {
      // Store the AI response for this vocabulary
      const vocabIndex = vocabulary.value.findIndex(item => item.vocabId === vocabId);
      if (vocabIndex !== -1) {
        let data = response.data;
        // If data is a string, try to parse it as JSON
        if (typeof data === 'string') {
          try {
            // Clean string by removing markdown code blocks if present
            const cleanedString = data
              .replace(/```json/g, '')
              .replace(/```/g, '')
              .trim();

            data = JSON.parse(cleanedString);
          } catch (error) {
            console.error('Failed to parse response as JSON:', error);
            // If parsing fails, create a fallback object
            data = {
              explanation: data, // Use the raw string as explanation
              examples: []
            };
          }
        }

        // Add AI response data to the vocabulary item
        vocabulary.value[vocabIndex].aiExplanation = data.explanation;
        vocabulary.value[vocabIndex].aiExamples = data.examples || [];

        // Initialize the displayed text as empty
        displayedText.value[vocabId] = '';

        // Add to visible chatGPT items
        chatGPTItems.value.push(vocabId);

        // Start the typing animation
        animateTyping(vocabId, data.explanation);
      }
    })
    .catch(error => {
      console.error('Error fetching AI explanation:', error);
      toast.error('Failed to fetch AI explanation', {
        position: 'top',
        duration: 3000
      });
    })
    .finally(() => {
      loadingChatGPT.value = null;
    });
  }
}

const animateTyping = (vocabId: string, text: string) => {
  typingInProgress.value = vocabId;
  let currentIndex = 0;
  displayedText.value[vocabId] = '';

  const typeNextChar = () => {
    if (currentIndex < text.length) {
      displayedText.value[vocabId] += text.charAt(currentIndex);
      currentIndex++;
      setTimeout(typeNextChar, typingSpeed.value);
    } else {
      // Start animating examples after main text is complete
      const vocabItem = vocabulary.value.find(item => item.vocabId === vocabId);
      if (vocabItem?.aiExamples && vocabItem.aiExamples.length > 0) {
        typingExamples.value[vocabId] = 0;
        animateExamples(vocabId, vocabItem.aiExamples);
      } else {
        typingInProgress.value = null;
      }
    }
  };

  typeNextChar();
}

const animateExamples = (vocabId: string, examples: any[]) => {
  if (!examples || examples.length === 0 || typingExamples.value[vocabId] >= examples.length) {
    typingExamples.value[vocabId] = -1; // All examples are displayed
    typingInProgress.value = null;
    return;
  }

  const currentExampleIndex = typingExamples.value[vocabId];
  const example = examples[currentExampleIndex] as AnimatedExample;

  // Animate Japanese text first
  animateExampleText(vocabId, 'japanese', example.japanese, () => {
    // Then animate Vietnamese text
    animateExampleText(vocabId, 'vietnamese', example.vietnamese, () => {
      // Move to next example
      typingExamples.value[vocabId]++;
      setTimeout(() => {
        animateExamples(vocabId, examples);
      }, 300); // Pause between examples
    });
  });
}

const animateExampleText = (vocabId: string, field: string, text: string, callback: () => void) => {
  let currentIndex = 0;
  const exampleIndex = typingExamples.value[vocabId];

  // Initialize the example text if needed
  const vocabIndex = vocabulary.value.findIndex(item => item.vocabId === vocabId);
  if (vocabIndex !== -1 && vocabulary.value[vocabIndex].aiExamples) {
    const example = vocabulary.value[vocabIndex].aiExamples[exampleIndex] as any;
    if (!example[field + 'Displayed']) {
      example[field + 'Displayed'] = '';
    }
  }

  const typeNextChar = () => {
    if (currentIndex < text.length) {
      const vocabIndex = vocabulary.value.findIndex(item => item.vocabId === vocabId);
      if (vocabIndex !== -1 && vocabulary.value[vocabIndex].aiExamples) {
        const example = vocabulary.value[vocabIndex].aiExamples[exampleIndex] as any;
        example[field + 'Displayed'] = text.substring(0, currentIndex + 1);
      }
      currentIndex++;
      setTimeout(typeNextChar, typingSpeed.value);
    } else {
      callback();
    }
  };

  typeNextChar();
}

const sendChatMessage = async (vocabId: string) => {
  if (!chatInputs.value[vocabId]) return;

  const message = chatInputs.value[vocabId];
  chatInputs.value[vocabId] = ''; // Clear input immediately for better UX

  const vocabItem = vocabulary.value.find(item => item.vocabId === vocabId);
  if (!vocabItem) {
    console.error('Vocabulary item not found');
    return;
  }

  // Create a local copy to maintain reactivity
  const localMessageHistory = vocabItem.chatHistory || [];

  // Add user message to history
  localMessageHistory.push({
    role: 'user',
    content: message
  });

  // Update the vocabulary item with the new message
  const vocabIndex = vocabulary.value.findIndex(item => item.vocabId === vocabId);
  if (vocabIndex !== -1) {
    // Ensure chatHistory exists and is an array
    if (!vocabulary.value[vocabIndex].chatHistory) {
      vocabulary.value[vocabIndex].chatHistory = [];
    }
    vocabulary.value[vocabIndex].chatHistory = [...localMessageHistory];
  }

  // Fetch AI response for the user message
  try {
    // Get the backend API URL
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

    const vocabWord = vocabItem.term || '';

    const response = await axios.post(`${apiUrl}/ai-service-api/v1/chat/vocabulary/chat`, null, {
      params: {
        vocabWord: vocabWord,
        userMessage: message
      }
    });

    // Process the response data
    let data = response.data;
    // If data is a string, try to parse it as JSON
    if (typeof data === 'string') {
      try {
        // Clean string by removing markdown code blocks if present
        const cleanedString = data
          .replace(/```json/g, '')
          .replace(/```/g, '')
          .trim();

        data = JSON.parse(cleanedString);
      } catch (error) {
        console.error('Failed to parse chat response as JSON:', error);
        // If parsing fails, create a fallback object with raw string as message
        data = {
          message: data
        };
      }
    }

    // Add AI response to chat history
    if (vocabIndex !== -1) {
      // Ensure chatHistory exists and is an array
      if (!vocabulary.value[vocabIndex].chatHistory) {
        vocabulary.value[vocabIndex].chatHistory = [];
      }

      // Use the message from the parsed data, or a fallback message
      const responseMessage = data && data.message
        ? data.message
        : "I'm sorry, I don't have a specific response for that. Could you try asking something else?";

      // Add empty response message first (for typing animation)
      const aiMessageIndex = vocabulary.value[vocabIndex].chatHistory!.length;
      vocabulary.value[vocabIndex].chatHistory!.push({
        role: 'assistant',
        content: '' // Empty content initially
      });

      // Start typing animation for the response
      animateTypingChatResponse(vocabId, responseMessage, vocabIndex, aiMessageIndex);
    }
  } catch (error) {
    console.error('Error sending chat message:', error);

    toast.error('Failed to get AI response', {
      position: 'top',
      duration: 3000
    });

    // Add error message to chat history
    if (vocabIndex !== -1) {
      // Ensure chatHistory exists and is an array
      if (!vocabulary.value[vocabIndex].chatHistory) {
        vocabulary.value[vocabIndex].chatHistory = [];
      }
      vocabulary.value[vocabIndex].chatHistory!.push({
        role: 'assistant',
        content: 'Sorry, I encountered an error processing your request. Please try again.'
      });
    }
  }
}

const animateTypingChatResponse = (vocabId: string, text: string, vocabIndex: number, messageIndex: number) => {
  typingInProgress.value = vocabId;
  let currentIndex = 0;

  const typeNextChar = () => {
    if (currentIndex < text.length) {
      if (vocabulary.value[vocabIndex]?.chatHistory &&
          vocabulary.value[vocabIndex].chatHistory![messageIndex]) {
        vocabulary.value[vocabIndex].chatHistory![messageIndex].content =
          text.substring(0, currentIndex + 1);
      }
      currentIndex++;
      setTimeout(typeNextChar, typingSpeed.value);
    } else {
      typingInProgress.value = null;
    }
  };

  typeNextChar();
}

// Helper method to safely access example display properties
const getExampleProperty = (example: any, property: string): string => {
  return example && example[property] ? example[property] : '';
}

// Check if example has complete Japanese text
const isJapaneseComplete = (example: any): boolean => {
  return example &&
         example.japaneseDisplayed &&
         example.japanese &&
         example.japaneseDisplayed.length === example.japanese.length;
}

// Check if Vietnamese display has started
const hasVietnameseStarted = (example: any): boolean => {
  return example && example.vietnameseDisplayed ? true : false;
}

// Lifecycle hooks
onMounted(async () => {
  await fetchVocabulary();
  await fetchTopics();
})
</script>

<style lang="sass" scoped>
.vocabulary-container
  background-color: #f8f9fa
  min-height: 100vh

.header
  background-color: #ffffff
  border-bottom: 1px solid rgba(0,0,0,0.06)
  box-shadow: 0 1px 3px rgba(0,0,0,0.05)

.search-filter-container
  margin-top: 12px

.filter-card
  border-radius: 12px
  background-color: #ffffff
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05)

.search-field
  background-color: #f5f5f7
  border-radius: 24px

  :deep(.v-field__field)
    padding-top: 0
    padding-bottom: 0

  :deep(.v-field__outline)
    display: none

.vocabulary-content
  margin-bottom: 24px

.vocabulary-card
  transition: all 0.2s
  overflow: hidden
  background-color: #ffffff
  border: 1px solid rgba(0,0,0,0.1)

  &:hover
    box-shadow: 0 4px 12px rgba(0,0,0,0.08)
    transform: translateY(-2px)

  &.expanded
    background-color: #f9fdff
    border-color: rgba(25, 118, 210, 0.2)

.japanese-text
  font-family: 'Noto Sans JP', sans-serif
  font-size: 0.8rem !important
  line-height: 1.5

.meaning-text
  color: rgba(0, 0, 0, 0.6)
  margin-top: 6px
  line-height: 1.4

.action-buttons-container
  display: flex
  align-items: center

.example-section
  background-color: #f5f9ff
  border-radius: 8px
  padding: 10px
  margin-top: 12px

.example-translation
  color: rgba(0, 0, 0, 0.6)
  font-style: italic

.additional-info
  margin-top: 12px
  display: flex
  flex-wrap: wrap

  .text-caption
    font-size: 0.7rem

.chatgpt-content
  background-color: #f0f7ff
  border-radius: 8px
  margin-top: 8px

.chatgpt-card
  background-color: rgba(255, 255, 255, 0.7)
  border-radius: 8px

.chatgpt-message
  line-height: 1.5
  color: rgba(0, 0, 0, 0.7)

.chat-input-container
  background-color: rgba(255, 255, 255, 0.8)
  padding: 8px
  border-radius: 8px

.chat-gpt-btn
  font-size: 0.75rem
  height: 32px
  transition: all 0.2s ease

  &:hover
    opacity: 1
    transform: scale(1.05)

  &:active
    transform: scale(0.98)

.typing-cursor
  display: inline-block
  width: 2px
  height: 16px
  background: #333
  margin-left: 2px
  animation: blink 0.7s infinite

@keyframes blink
  0%, 100%
    opacity: 1
  50%
    opacity: 0

.example-text
  color: rgba(0, 0, 0, 0.6)
  font-style: italic
  border-left: 2px solid rgba(0, 150, 0, 0.3)
  padding-left: 8px

// Mobile responsive styles
.mobile-actions
  display: none

@media (max-width: 960px)
  .mobile-actions
    display: block

  .chat-gpt-btn span
    display: none

  .item-text
    max-width: calc(100% - 100px)

  // Ensure the audio button has enough space on mobile
  .audio-btn
    margin-right: 8px !important

@media (max-width: 600px)
  .vocabulary-card
    margin-left: -12px
    margin-right: -12px
    border-radius: 0
    border-left: none
    border-right: none

  .vocabulary-container
    padding: 0

  .vocabulary-content
    padding-left: 12px
    padding-right: 12px

// Customize menu overlay styles
:deep(.v-menu > .v-overlay__content)
  border-radius: 8px
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15) !important
  max-width: 250px

:deep(.v-list-item--density-compact)
  min-height: 40px !important

:deep(.v-list-item__prepend)
  margin-right: 8px !important

:deep(.v-list)
  padding: 4px !important

:deep(.v-pagination)
  justify-content: center

:deep(.v-pagination__item)
  transition: all 0.2s ease
  font-weight: 500

:deep(.v-pagination__item--active)
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1)
  transform: scale(1.05)

:deep(.v-pagination__item:hover)
  background-color: rgba(0, 0, 0, 0.05)

.term-container
  display: flex
  align-items: center
  flex-wrap: wrap

.pronunciation-mobile
  margin-top: 2px
  margin-bottom: 2px
  color: rgba(0, 0, 0, 0.7)
  font-style: italic
</style>
