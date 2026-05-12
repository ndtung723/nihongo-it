<template>
  <div class="vocabulary-topic-container">
    <!-- Header Section -->
    <div class="header px-4 py-3 d-flex justify-space-between align-center">
      <div class="d-flex align-center">
        <v-btn icon class="mr-2" @click="goBack">
          <v-icon>mdi-chevron-left</v-icon>
        </v-btn>
        <div
          class="text-subtitle-1 font-weight-bold text-dark d-none d-md-block"
        >
          <span class="text-primary">IT</span>
          <span class="japanese-text">単語学習</span>
        </div>
      </div>

      <!-- Topic Info (Centered) - Responsive Design -->
      <div v-if="topic" class="topic-info d-flex flex-column align-center">
        <div class="text-subtitle-1 font-weight-bold japanese-text text-center">
          {{ topic.name }}
        </div>
        <div class="text-caption text-center d-md-inline d-block">
          ({{ topic.meaning }})
        </div>
      </div>

      <div class="d-flex align-center">
        <v-btn icon variant="text" @click="openFilterDialog">
          <v-icon>mdi-filter-variant</v-icon>
        </v-btn>
      </div>
    </div>

    <!-- Search Field -->
    <div class="search-container px-4 mb-2">
      <div class="search-field d-flex align-center pa-2 rounded-pill">
        <v-text-field
          v-model="search"
          placeholder="検索 / Tìm kiếm từ vựng"
          prepend-inner-icon="mdi-magnify"
          variant="plain"
          hide-details
          single-line
          density="compact"
          @update:model-value="debouncedFilterVocabulary"
        ></v-text-field>
      </div>
    </div>

    <!-- Active Filters (if any) -->
    <div class="active-filters px-4 mb-2" v-if="hasActiveFilters">
      <div class="d-flex flex-wrap">
        <v-chip
          v-if="selectedJlptLevel"
          size="x-small"
          color="primary"
          class="mr-2 mb-1"
          closable
          @click:close="clearFilter('jlptLevel')"
        >
          {{ selectedJlptLevel }}
        </v-chip>
        <v-chip v-if="topic" size="x-small" color="success" class="mr-2 mb-1">
          {{ topic.meaning }}
        </v-chip>
      </div>
    </div>

    <!-- Loading Indicator -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="text-body-1 text-medium-emphasis mt-3">Loading...</p>
    </div>

    <!-- Vocabulary List for Topic -->
    <div v-else-if="topic" class="vocabulary-section px-4">
      <div v-if="vocabularyItems.length === 0" class="text-center py-8">
        <v-icon
          size="large"
          icon="mdi-book-search-outline"
          class="mb-4"
        ></v-icon>
        <h3 class="text-h6">Không tìm thấy từ vựng</h3>
        <p class="text-body-1 text-medium-emphasis">
          Hãy điều chỉnh bộ lọc tìm kiếm hoặc chọn chủ đề khác.
        </p>
      </div>

      <div v-else>
        <!-- Pagination at the top -->
        <div class="d-flex justify-center mb-2" v-if="totalPages > 1">
          <v-pagination
            v-model="currentPage"
            :length="totalPages"
            :total-visible="5"
            rounded="circle"
            density="compact"
            size="small"
            @update:modelValue="handlePageChange"
          ></v-pagination>
        </div>

        <v-card
          v-for="item in vocabularyItems"
          :key="item.vocabId"
          class="vocabulary-card mb-3"
          :class="{ expanded: expandedItems.includes(item.vocabId) }"
          elevation="1"
          rounded="lg"
          @click="toggleExpand(item.vocabId)"
        >
          <!-- Main vocabulary item content -->
          <div class="pa-3">
            <div class="d-flex align-center justify-space-between">
              <div class="item-text">
                <div class="term-container">
                  <div class="text-subtitle-2 japanese-text font-weight-bold">
                    {{ item.term }}
                  </div>
                  <div
                    v-if="item.pronunciation"
                    class="text-body-2 japanese-text text-medium-emphasis pronunciation d-none d-md-inline-block ml-2"
                  >
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
                <div
                  v-if="item.pronunciation"
                  class="text-body-2 japanese-text text-medium-emphasis d-md-none pronunciation-mobile"
                >
                  {{ item.pronunciation }}
                </div>
                <div class="text-body-2 meaning-text mt-1">
                  {{ item.meaning }}
                </div>
              </div>
              <v-spacer></v-spacer>

              <!-- Audio button - always visible -->
              <v-btn
                icon="mdi-volume-high"
                size="small"
                variant="text"
                @click.stop="playAudio(item)"
                class="mx-1 audio-btn"
                color="blue"
                title="Phát âm"
                :loading="playingAudioId === item.vocabId"
                :disabled="playingAudioId === item.vocabId"
              ></v-btn>

              <!-- Desktop buttons - hidden on mobile -->
              <div
                class="action-buttons-container d-none d-md-flex"
                @click.stop
              >
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
                  @click.stop="navigateToDetail(item.term)"
                  class="mx-1"
                  title="Xem chi tiết"
                  icon="mdi-open-in-new"
                ></v-btn>
                <v-btn
                  size="small"
                  variant="text"
                  color="success"
                  @click.stop.prevent="toggleChatGPT(item.vocabId)"
                  :disabled="loadingChatGPT === item.vocabId"
                  :loading="loadingChatGPT === item.vocabId"
                  class="chat-gpt-btn mx-1"
                  title="Hỏi ChatGPT"
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
                          {{
                            item.isSaved
                              ? "mdi-bookmark"
                              : "mdi-bookmark-outline"
                          }}
                        </v-icon>
                      </template>
                      <v-list-item-title>{{
                        item.isSaved ? "Bỏ lưu" : "Lưu từ vựng"
                      }}</v-list-item-title>
                    </v-list-item>

                    <v-list-item @click.stop="navigateToDetail(item.term)">
                      <template v-slot:prepend>
                        <v-icon color="primary">mdi-open-in-new</v-icon>
                      </template>
                      <v-list-item-title>Xem chi tiết</v-list-item-title>
                    </v-list-item>

                    <v-list-item
                      @click.stop="toggleChatGPT(item.vocabId)"
                      :disabled="loadingChatGPT === item.vocabId"
                    >
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
            <div
              v-if="expandedItems.includes(item.vocabId)"
              class="mt-2"
              @click.stop
            >
              <v-divider class="mb-2"></v-divider>

              <!-- Example Section -->
              <div
                v-if="item.example"
                class="example-section mb-2 pa-2 rounded"
              >
                <div class="d-flex align-items-center">
                  <v-icon class="mr-1" size="x-small" color="grey"
                    >mdi-format-quote-open</v-icon
                  >
                  <div class="flex-grow-1 japanese-text text-body-2">
                    {{ item.example }}
                  </div>
                  <v-btn
                    icon="mdi-volume-high"
                    size="x-small"
                    variant="text"
                    @click.stop="playAudio(item, true)"
                    color="blue"
                    title="Phát âm câu ví dụ"
                    :loading="playingExampleAudioId === item.vocabId"
                    :disabled="playingExampleAudioId === item.vocabId"
                  ></v-btn>
                </div>
                <div
                  v-if="item.exampleMeaning"
                  class="example-translation ml-4 mt-1 text-caption text-medium-emphasis"
                >
                  {{ item.exampleMeaning }}
                </div>
              </div>
            </div>
          </div>

          <!-- ChatGPT Content (completely independent section) -->
          <div
            v-if="chatGPTItems.includes(item.vocabId)"
            class="pa-3 pt-0"
            @click.stop
          >
            <div class="chatgpt-content py-2 px-2 rounded" @click.stop>
              <v-divider class="mb-2"></v-divider>

              <!-- Initial AI Explanation -->
              <v-card
                flat
                class="chatgpt-card pa-2 mb-2"
                v-if="item.aiExplanation"
                @click.stop
              >
                <div class="d-flex align-items-start mb-1">
                  <v-avatar size="24" color="green" class="mr-2 mt-1">
                    <span class="text-caption text-white">AI</span>
                  </v-avatar>
                  <div>
                    <div class="text-caption font-weight-medium">
                      Trợ lý ChatGPT
                    </div>
                    <div class="chatgpt-message text-body-2 mt-1">
                      <p v-html="displayedText[item.vocabId] || ''"></p>
                      <span
                        v-if="
                          typingInProgress === item.vocabId &&
                          typingExamples[item.vocabId] === undefined
                        "
                        class="typing-cursor"
                        >|</span
                      >

                      <div
                        v-if="
                          item.aiExamples &&
                          item.aiExamples.length > 0 &&
                          typingExamples[item.vocabId] !== undefined
                        "
                        class="mt-2"
                      >
                        <p class="font-weight-medium text-caption">
                          Câu ví dụ:
                        </p>
                        <div
                          v-for="(example, exIndex) in item.aiExamples"
                          :key="exIndex"
                          class="mt-1"
                        >
                          <template
                            v-if="
                              typingExamples[item.vocabId] > exIndex ||
                              typingExamples[item.vocabId] === -1
                            "
                          >
                            <!-- Fully displayed examples -->
                            <p class="example-text text-caption">
                              {{ example.japanese }}
                            </p>
                            <p class="text-caption ml-3">
                              {{ example.vietnamese }}
                            </p>
                          </template>
                          <template
                            v-else-if="typingExamples[item.vocabId] === exIndex"
                          >
                            <!-- Currently typing example -->
                            <p class="example-text text-caption">
                              {{
                                getExampleProperty(
                                  example,
                                  "japaneseDisplayed",
                                ) || ""
                              }}
                              <span
                                v-if="
                                  isJapaneseComplete(example) &&
                                  !hasVietnameseStarted(example)
                                "
                                class="typing-cursor"
                                >|</span
                              >
                            </p>
                            <p
                              v-if="hasVietnameseStarted(example)"
                              class="text-caption ml-3"
                            >
                              {{
                                getExampleProperty(
                                  example,
                                  "vietnameseDisplayed",
                                ) || ""
                              }}
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
                  @click.stop
                >
                  <!-- User Message -->
                  <div
                    v-if="message.role === 'user'"
                    class="d-flex align-items-start"
                  >
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
                  <v-card v-else flat class="chatgpt-card pa-2" @click.stop>
                    <div class="d-flex align-items-start">
                      <v-avatar size="24" color="green" class="mr-2 mt-1">
                        <span class="text-caption text-white">AI</span>
                      </v-avatar>
                      <div>
                        <div class="text-caption font-weight-medium">
                          Trợ lý ChatGPT
                        </div>
                        <div class="chatgpt-message text-body-2 mt-1">
                          <span v-html="message.content"></span>
                          <span
                            v-if="
                              typingInProgress === item.vocabId &&
                              msgIndex === item.chatHistory?.length - 1
                            "
                            class="typing-cursor"
                            >|</span
                          >
                        </div>
                      </div>
                    </div>
                  </v-card>
                </div>
              </template>

              <!-- User Input -->
              <div class="chat-input-container d-flex mt-2">
                <v-text-field
                  v-model="chatInputs[item.vocabId]"
                  placeholder="Nhập tin nhắn của bạn..."
                  variant="outlined"
                  density="compact"
                  hide-details
                  @keyup.enter="sendChatMessage(item.vocabId)"
                  @click.stop.prevent
                  class="mr-1"
                ></v-text-field>
                <v-btn
                  color="primary"
                  size="small"
                  :disabled="!chatInputs[item.vocabId]"
                  @click.stop.prevent="sendChatMessage(item.vocabId)"
                >
                  <v-icon size="small">mdi-send</v-icon>
                </v-btn>
              </div>
            </div>
          </div>
        </v-card>

        <!-- Pagination at the bottom -->
        <div class="d-flex justify-center mt-2 mb-2" v-if="totalPages > 1">
          <v-pagination
            v-model="currentPage"
            :length="totalPages"
            :total-visible="5"
            rounded="circle"
            density="compact"
            size="small"
            @update:modelValue="handlePageChange"
          ></v-pagination>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-8">
      <v-icon size="large" icon="mdi-book-search-outline" class="mb-4"></v-icon>
      <h3 class="text-h6">トピックが見つかりません</h3>
      <p class="text-body-1 text-medium-emphasis">
        The requested topic could not be found.
      </p>
      <v-btn color="primary" class="mt-4" @click="goToCategories">
        View All Categories
      </v-btn>
    </div>

    <!-- Filter Dialog -->
    <v-dialog v-model="showFilterDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 font-weight-bold">
          Lọc từ vựng
        </v-card-title>
        <v-card-text>
          <v-select
            v-model="selectedJlptLevel"
            label="Trình độ JLPT"
            :items="jlptLevels"
            clearable
            variant="outlined"
            class="mb-4"
          ></v-select>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text @click="clearAllFilters">Xóa tất cả</v-btn>
          <v-btn color="primary" @click="applyFilters">Áp dụng</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import vocabularyService from "@/services/vocabulary.service";
import type { VocabularyItem, VocabularyFilter } from "@/types/learning.types";
import { useAppToast } from "@/composables/useAppToast";
import api from "@/utils/api";
import { isAxiosError } from "axios";
import authService from "@/services/auth.service";
import aiService from "@/services/ai.service";

// Define interfaces for the data structures
interface Topic {
  id: string;
  name: string;
  meaning: string;
  categoryId?: string;
  category?: {
    id: string;
    name: string;
  };
  displayOrder: number;
  vocabularyCount?: number;
}

// Interface for animated examples
interface AnimatedExample {
  japanese: string;
  vietnamese: string;
  japaneseDisplayed?: string;
  vietnameseDisplayed?: string;
}

const router = useRouter();
const route = useRoute();
const toast = useAppToast();

// State variables
const loading = ref(true);
const topic = ref<Topic | null>(null);
const vocabularyItems = ref<VocabularyItem[]>([]);
const totalItems = ref(0);
const totalPages = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const jlptLevels = ref<string[]>([]);
const search = ref("");
const selectedJlptLevel = ref<string | null>(null);
const expandedItems = ref<string[]>([]);
const showFilterDialog = ref(false);
const playingAudioId = ref<string | null>(null);
const playingExampleAudioId = ref<string | null>(null);
const loadingChatGPT = ref<string | null>(null);
const chatGPTItems = ref<string[]>([]);
const chatInputs = ref<Record<string, string>>({});
const typingInProgress = ref<string | null>(null);
const displayedText = ref<Record<string, string>>({});
const typingExamples = ref<Record<string, number>>({});
const typingSpeed = 20; // ms between characters

// Get topic name from route
const topicParam = computed(() => route.params.name as string);

// Computed properties
const hasActiveFilters = computed(() => {
  return selectedJlptLevel.value || search.value;
});

// Watchers
watch(
  topicParam,
  async (newValue) => {
    if (newValue) {
      await fetchTopicDetails();
      fetchVocabulary();
    }
  },
  { immediate: true },
);

watch([selectedJlptLevel, search], () => {
  if (topic.value) {
    currentPage.value = 1; // Reset to first page when filters change
    fetchVocabulary();
  }
});

watch(currentPage, () => {
  if (topic.value) {
    fetchVocabulary();
  }
});

// Lifecycle hooks
onMounted(async () => {
  // Fetch JLPT levels
  fetchJlptLevels();

  // Note: Topic details and vocabulary will be loaded by the topicParam watcher
});

// Debounce function for search
let searchTimeout: ReturnType<typeof setTimeout> | null = null;
function debouncedFilterVocabulary() {
  clearTimeout(searchTimeout ?? undefined);
  searchTimeout = setTimeout(() => {
    fetchVocabulary();
  }, 500);
}

// Methods
async function fetchTopicDetails() {
  try {
    loading.value = true;

    // Fetch all topics first
    const topicsResponse = await vocabularyService.getTopics();

    if (Array.isArray(topicsResponse)) {
      // Decode the URL parameter which could be a name or ID
      const paramValue = topicParam.value;
      const decodedParam = decodeURIComponent(paramValue);

      // Try finding the topic by ID first (backward compatibility)
      let foundTopic = topicsResponse.find(
        (t) => t.id === paramValue || t.topicId === paramValue,
      );

      // If not found by ID, try with decoded name
      if (!foundTopic) {
        foundTopic = topicsResponse.find(
          (t) =>
            t.name === decodedParam ||
            encodeURIComponent(t.name) === paramValue,
        );
      }

      if (foundTopic) {
        topic.value = {
          id: foundTopic.id || foundTopic.topicId,
          name: foundTopic.name,
          meaning: foundTopic.meaning,
          categoryId: foundTopic.categoryId || foundTopic.category?.id,
          displayOrder: foundTopic.displayOrder || 0,
          vocabularyCount: foundTopic.vocabularyCount || 0,
        };
      } else {
        topic.value = null;
        vocabularyItems.value = [];
        toast.error("Topic not found");
      }
    }
  } catch {
    toast.error("Failed to load topic details");
    topic.value = null;
    vocabularyItems.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchJlptLevels() {
  try {
    const response = await vocabularyService.getJlptLevels();
    jlptLevels.value = response as string[];
  } catch {
    // Use default values if API call fails
    jlptLevels.value = ["N1", "N2", "N3", "N4", "N5"];
  }
}

async function fetchVocabulary() {
  try {
    loading.value = true;

    if (!topic.value) {
      vocabularyItems.value = [];
      totalItems.value = 0;
      totalPages.value = 0;
      return;
    }

    // Create filter object
    const filter: VocabularyFilter = {
      keyword: search.value || null,
      jlptLevel: selectedJlptLevel.value as
        | import("@/types/common.types").JlptLevel
        | null,
      topicName: topic.value.name, // Using topic name for filtering
      page: currentPage.value - 1, // API uses 0-based pagination
      size: pageSize.value,
    };

    const response = await vocabularyService.getVocabulary(filter);

    if (response && Array.isArray(response.content)) {
      vocabularyItems.value = response.content;
      totalItems.value = response.totalElements;
      totalPages.value = response.totalPages;
    } else {
      vocabularyItems.value = [];
      totalItems.value = 0;
      totalPages.value = 0;
    }
  } catch {
    toast.error("Failed to load vocabulary");
    vocabularyItems.value = [];
    totalItems.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
}

async function toggleSave(item: VocabularyItem) {
  try {
    if (item.isSaved) {
      await vocabularyService.removeSavedVocabulary(item.vocabId);
      toast.success("Removed from saved items");
    } else {
      await vocabularyService.saveVocabulary(item.vocabId);
      toast.success("Added to saved items");
    }

    // Toggle the state locally
    item.isSaved = !item.isSaved;
  } catch {
    toast.error("Failed to update saved status");
  }
}

async function playAudio(item: VocabularyItem, isExample = false) {
  // Set the ID of the item we're playing audio for
  if (isExample) {
    playingExampleAudioId.value = item.vocabId;
  } else {
    playingAudioId.value = item.vocabId;
  }

  const audioPath = item.audioPath || null;
  try {
    if (audioPath) {
      // If there's an existing audio path, use it
      const audio = new Audio(audioPath);
      await audio.play();
    } else {
      // Verify authentication before proceeding
      const authToken = authService.getToken();
      if (!authToken) {
        toast.error("Please log in to use text-to-speech");
        // Redirect to login page after a short delay
        setTimeout(() => {
          router.push({
            name: "login",
            query: { redirect: router.currentRoute.value.fullPath },
          });
        }, 1500);
        return;
      }

      // Determine which text to use for speech
      let textToSpeak = "";
      if (isExample && item.example) {
        // Use example sentence if requested and available
        textToSpeak = item.example;
      } else {
        // Use term for vocabulary pronunciation
        textToSpeak = item.pronunciation ? item.pronunciation : item.term;
      }

      // Show loading indicator
      toast.info("Generating audio...");

      // Set speed: 0.9 for vocabulary, 1.0 for example sentences
      const speed = isExample ? 1.0 : 0.9;

      // Call the TTS API with Authorization header
      const response = await api.post("/api/v1/ai/tts/generate", textToSpeak, {
        headers: {
          "Content-Type": "text/plain; charset=UTF-8",
          "Accept-Language": "ja-JP",
          "X-Speech-Speed": speed.toString(),
          "X-Content-Language": "ja",
          "X-Content-Type": isExample ? "example" : "vocabulary",
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
        if (isExample) {
          playingExampleAudioId.value = null;
        } else {
          playingAudioId.value = null;
        }
      };
      await audio.play();
    }
  } catch (error) {
    // Special handling for 401 errors
    if (isAxiosError(error) && error.response?.status === 401) {
      toast.error(
        "TTS service requires authentication. Please log in again when convenient.",
      );
    } else {
      toast.error(
        error instanceof Error ? error.message : "Failed to generate speech",
      );
    }
  } finally {
    // Reset loading state if no audio was played (in case of error)
    // For successful audio playback, the onended event will handle this
    if (!audioPath) {
      setTimeout(() => {
        if (isExample) {
          playingExampleAudioId.value = null;
        } else {
          playingAudioId.value = null;
        }
      }, 3000);
    } else {
      if (isExample) {
        playingExampleAudioId.value = null;
      } else {
        playingAudioId.value = null;
      }
    }
  }
}

function toggleExpand(vocabId: string) {
  const index = expandedItems.value.indexOf(vocabId);
  if (index === -1) {
    expandedItems.value.push(vocabId);
  } else {
    expandedItems.value.splice(index, 1);
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchVocabulary();
}

function navigateToDetail(term: string) {
  router.push({
    name: "vocabularyDetail",
    params: {
      term: encodeURIComponent(term),
    },
  });
}

function goBack() {
  router.back();
}

function goToCategories() {
  router.push({ name: "vocabularyLearning" });
}

function openFilterDialog() {
  showFilterDialog.value = true;
}

function applyFilters() {
  showFilterDialog.value = false;
  fetchVocabulary();
}

function clearFilter(type: string) {
  if (type === "jlptLevel") {
    selectedJlptLevel.value = null;
  }
  fetchVocabulary();
}

function clearAllFilters() {
  selectedJlptLevel.value = null;
  search.value = "";
  fetchVocabulary();
  showFilterDialog.value = false;
}

function getJlptColor(level: string): string {
  switch (level) {
    case "N1":
      return "red";
    case "N2":
      return "orange";
    case "N3":
      return "amber";
    case "N4":
      return "light-green";
    case "N5":
      return "green";
    default:
      return "grey";
  }
}

async function toggleChatGPT(vocabId: string) {
  const index = chatGPTItems.value.indexOf(vocabId);

  // Remove the automatic expansion of row content
  // if (!expandedItems.value.includes(vocabId)) {
  //   expandedItems.value.push(vocabId)
  // }

  if (index >= 0) {
    // If already open, close it
    chatGPTItems.value.splice(index, 1);
  } else {
    // Start loading state
    loadingChatGPT.value = vocabId;

    // Find the vocabulary item by ID
    const vocabItem = vocabularyItems.value.find(
      (item) => item.vocabId === vocabId,
    );

    if (!vocabItem) {
      loadingChatGPT.value = null;
      return;
    }

    // Initialize chat input for this item if not exists
    if (!chatInputs.value[vocabId]) {
      chatInputs.value[vocabId] = "";
    }

    // Query the AI for vocabulary explanation
    api
      .post("/api/v1/ai/chat/vocabulary/explain", null, {
        params: {
          term: vocabItem.term || "",
          pronunciation: vocabItem.pronunciation || "",
          meaning: vocabItem.meaning || "",
          topicName: vocabItem.topicName || "",
          example: vocabItem.example || "",
        },
      })
      .then((response) => {
        // Store the AI response for this vocabulary
        const vocabIndex = vocabularyItems.value.findIndex(
          (item) => item.vocabId === vocabId,
        );
        if (vocabIndex !== -1) {
          let data = response.data;
          // If data is a string, try to parse it as JSON
          if (typeof data === "string") {
            try {
              // Clean string by removing markdown code blocks if present
              const cleanedString = data
                .replace(/```json/g, "")
                .replace(/```/g, "")
                .trim();

              data = JSON.parse(cleanedString);
            } catch {
              // If parsing fails, create a fallback object
              data = {
                explanation: data, // Use the raw string as explanation
                examples: [],
              };
            }
          }

          // Add AI response data to the vocabulary item
          vocabularyItems.value[vocabIndex].aiExplanation = data.explanation;
          vocabularyItems.value[vocabIndex].aiExamples = data.examples || [];

          // Initialize the displayed text as empty
          displayedText.value[vocabId] = "";

          // Add to visible chatGPT items
          chatGPTItems.value.push(vocabId);

          // Start the typing animation
          animateTyping(vocabId, data.explanation);
        }
      })
      .catch(() => {
        toast.error("Failed to fetch AI explanation");
      })
      .finally(() => {
        loadingChatGPT.value = null;
      });
  }
}

function animateTyping(vocabId: string, text: string) {
  typingInProgress.value = vocabId;
  let currentIndex = 0;
  displayedText.value[vocabId] = "";

  const typeNextChar = () => {
    if (currentIndex < text.length) {
      displayedText.value[vocabId] += text.charAt(currentIndex);
      currentIndex++;
      setTimeout(typeNextChar, typingSpeed);
    } else {
      // Start animating examples after main text is complete
      const vocabItem = vocabularyItems.value.find(
        (item) => item.vocabId === vocabId,
      );
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

function animateExamples(vocabId: string, examples: unknown[]) {
  if (
    !examples ||
    examples.length === 0 ||
    typingExamples.value[vocabId] >= examples.length
  ) {
    typingExamples.value[vocabId] = -1; // All examples are displayed
    typingInProgress.value = null;
    return;
  }

  const currentExampleIndex = typingExamples.value[vocabId];
  const example = examples[currentExampleIndex] as AnimatedExample;

  // Animate Japanese text first
  animateExampleText(vocabId, "japanese", example.japanese, () => {
    // Then animate Vietnamese text
    animateExampleText(vocabId, "vietnamese", example.vietnamese, () => {
      // Move to next example
      typingExamples.value[vocabId]++;
      setTimeout(() => {
        animateExamples(vocabId, examples);
      }, 300); // Pause between examples
    });
  });
}

function animateExampleText(
  vocabId: string,
  field: string,
  text: string,
  callback: () => void,
) {
  let currentIndex = 0;
  const exampleIndex = typingExamples.value[vocabId];

  // Initialize the example text if needed
  const vocabIndex = vocabularyItems.value.findIndex(
    (item) => item.vocabId === vocabId,
  );
  if (vocabIndex !== -1 && vocabularyItems.value[vocabIndex].aiExamples) {
    const example = vocabularyItems.value[vocabIndex].aiExamples[
      exampleIndex
    ] as unknown;
    if (!(example as Record<string, unknown>)[field + "Displayed"]) {
      (example as Record<string, unknown>)[field + "Displayed"] = "";
    }
  }

  const typeNextChar = () => {
    if (currentIndex < text.length) {
      const vocabIndex = vocabularyItems.value.findIndex(
        (item) => item.vocabId === vocabId,
      );
      if (vocabIndex !== -1 && vocabularyItems.value[vocabIndex].aiExamples) {
        const example = vocabularyItems.value[vocabIndex].aiExamples[
          exampleIndex
        ] as unknown;
        (example as Record<string, unknown>)[field + "Displayed"] =
          text.substring(0, currentIndex + 1);
      }
      currentIndex++;
      setTimeout(typeNextChar, typingSpeed);
    } else {
      callback();
    }
  };

  typeNextChar();
}

async function sendChatMessage(vocabId: string) {
  if (!chatInputs.value[vocabId]) return;

  const message = chatInputs.value[vocabId];
  chatInputs.value[vocabId] = ""; // Clear input immediately for better UX

  const vocabItem = vocabularyItems.value.find(
    (item) => item.vocabId === vocabId,
  );
  if (!vocabItem) {
    return;
  }

  // Create a local copy to maintain reactivity
  const localMessageHistory = vocabItem.chatHistory || [];

  // Add user message to history
  localMessageHistory.push({
    role: "user",
    content: message,
  });

  // Update the vocabulary item with the new message
  const vocabIndex = vocabularyItems.value.findIndex(
    (item) => item.vocabId === vocabId,
  );
  if (vocabIndex !== -1) {
    // Ensure chatHistory exists and is an array
    if (!vocabularyItems.value[vocabIndex].chatHistory) {
      vocabularyItems.value[vocabIndex].chatHistory = [];
    }
    vocabularyItems.value[vocabIndex].chatHistory = [...localMessageHistory];
  }

  // Stream AI response
  if (vocabIndex === -1) return;
  if (!vocabularyItems.value[vocabIndex].chatHistory) {
    vocabularyItems.value[vocabIndex].chatHistory = [];
  }
  const aiMessageIndex = vocabularyItems.value[vocabIndex].chatHistory!.length;
  vocabularyItems.value[vocabIndex].chatHistory!.push({
    role: "assistant",
    content: "",
  });
  typingInProgress.value = vocabId;

  try {
    const vocabWord = vocabItem.term || "";
    await aiService.streamVocabularyChat(
      vocabWord,
      message,
      (chunk) => {
        if (vocabularyItems.value[vocabIndex]?.chatHistory?.[aiMessageIndex]) {
          vocabularyItems.value[vocabIndex].chatHistory![
            aiMessageIndex
          ].content += chunk;
        }
      },
      () => {
        typingInProgress.value = null;
      },
      () => {
        typingInProgress.value = null;
        toast.error("Failed to get AI response");
      },
    );
  } catch {
    typingInProgress.value = null;
    toast.error("Failed to get AI response", {
      position: "top",
      duration: 3000,
    });
    vocabularyItems.value[vocabIndex].chatHistory![aiMessageIndex] = {
      role: "assistant",
      content:
        "Sorry, I encountered an error processing your request. Please try again.",
    };
  }
}

// Helper methods for example display
function getExampleProperty(
  example: AnimatedExample,
  property: keyof AnimatedExample,
): string {
  return example && example[property] ? String(example[property]) : "";
}

function isJapaneseComplete(example: AnimatedExample): boolean {
  return !!(
    example &&
    example.japaneseDisplayed &&
    example.japanese &&
    example.japaneseDisplayed.length === example.japanese.length
  );
}

function hasVietnameseStarted(example: AnimatedExample): boolean {
  return example && example.vietnameseDisplayed ? true : false;
}
</script>

<style scoped lang="scss">
.vocabulary-topic-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 64px;
}

.japanese-text {
  font-family: "Noto Sans JP", sans-serif;
  font-size: 0.8rem !important;
  line-height: 1.5;
}

.search-field {
  background-color: #f0f0f0;
  height: 40px;
  border-radius: 20px;
}

.vocabulary-section {
  width: 100%;
  margin: 0 auto;
}

.vocabulary-card {
  transition: all 0.2s;
  overflow: hidden;
  background-color: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.1);
  cursor: pointer;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
    transform: translateY(-2px);
  }

  &.expanded {
    background-color: #f9fdff;
    border-color: rgba(25, 118, 210, 0.2);
  }
}

.item-text {
  max-width: calc(100% - 180px);
}

.meaning-text {
  color: rgba(0, 0, 0, 0.6);
  margin-top: 2px;
  margin-bottom: 2px;
  line-height: 1.4;
}

.example-section {
  background-color: #f5f9ff;
  border-radius: 8px;
  padding: 10px;
  border-left: none;
}

.example-translation {
  color: rgba(0, 0, 0, 0.6);
  font-style: italic;
  line-height: 1.2;
}

.chat-gpt-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  height: 28px;
  transition: all 0.2s ease;

  &:hover {
    opacity: 1;
    transform: scale(1.05);
  }
}

/* ChatGPT styles */
.chatgpt-content {
  background-color: #f0f7ff;
  border-top: 1px dashed rgba(0, 150, 0, 0.15);
  border-bottom: 1px dashed rgba(0, 150, 0, 0.15);
  transform: none !important;
  transition: none !important;
  z-index: 10;
  position: relative;
  pointer-events: auto;
  padding-top: 0.5rem;
  border-radius: 6px;
  cursor: default;
}

/* When ChatGPT is visible but the item is not expanded,
   add a divider to separate from the meaning text */
.vocabulary-card:not(.expanded) .chatgpt-content {
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  padding-top: 0.5rem;
}

.chatgpt-card {
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
}

.chatgpt-message {
  line-height: 1.3;
  color: rgba(0, 0, 0, 0.7);
}

.example-text {
  color: rgba(0, 0, 0, 0.6);
  font-style: italic;
  padding-left: 8px;
  border-left: 2px solid rgba(0, 150, 0, 0.3);
  line-height: 1.2;
  margin-bottom: 2px;
}

.chat-input-container {
  background-color: rgba(255, 255, 255, 0.8);
  padding: 4px;
  border-radius: 6px;
}

.action-buttons-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.action-buttons-container .v-btn {
  margin: 0 2px;
  min-width: 28px;
  height: 28px;
}

.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #333;
  margin-left: 2px;
  animation: blink 0.7s infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* Responsive styles for mobile */
.mobile-actions {
  display: none;
}

@media (max-width: 960px) {
  .mobile-actions {
    display: block;
  }

  .chat-gpt-btn span {
    display: none;
  }

  .item-text {
    max-width: calc(100% - 100px);
  }

  /* Ensure the audio button has enough space on mobile */
  .audio-btn {
    margin-right: 8px !important;
  }

  /* Adjust term container for mobile */
  .term-container {
    flex-wrap: wrap;
  }
}

@media (max-width: 600px) {
  .vocabulary-card {
    margin-left: -12px;
    margin-right: -12px;
    border-radius: 0;
    border-left: none;
    border-right: none;
  }

  .vocabulary-section {
    padding-left: 12px;
    padding-right: 12px;
  }
}

/* Customize menu overlay styles */
:deep(.v-menu > .v-overlay__content) {
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15) !important;
  max-width: 250px;
}

:deep(.v-list-item--density-compact) {
  min-height: 40px !important;
}

:deep(.v-list-item__prepend) {
  margin-right: 8px !important;
}

:deep(.v-list) {
  padding: 4px !important;
}

.term-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.pronunciation-mobile {
  color: rgba(0, 0, 0, 0.7);
  font-style: italic;
  font-size: 0.8rem !important;
}

.header {
  padding-top: 8px !important;
  padding-bottom: 8px !important;
  position: relative;
}

.topic-info {
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 960px) {
  .topic-info {
    max-width: 70%;
  }
}

@media (max-width: 600px) {
  .topic-info {
    max-width: 70%;
    /* Show as column on very small screens */
    flex-direction: column;
    align-items: center;
  }
}
</style>
