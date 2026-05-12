<template>
  <div class="vocabulary-learning-container">
    <!-- Header Section -->
    <div class="header py-3 d-flex justify-space-between align-center">
      <div class="d-flex align-center">
        <v-btn icon class="mr-2" @click="goBack">
          <v-icon>mdi-chevron-left</v-icon>
        </v-btn>
        <div class="text-h6 font-weight-bold text-dark">
          <span class="text-primary">IT</span>
          <span class="japanese-text">単語学習</span>
        </div>
      </div>
      <div class="d-flex align-center">
        <v-btn icon variant="text" @click="openFilterDialog">
          <v-icon>mdi-filter-variant</v-icon>
        </v-btn>
      </div>
    </div>

    <!-- Search Field -->
    <div class="search-container px-4 mb-4">
      <div class="search-field d-flex align-center pa-2 rounded-pill">
        <v-text-field
          v-model="search"
          placeholder="検索 / Tìm kiếm từ vựng"
          prepend-inner-icon="mdi-magnify"
          variant="plain"
          hide-details
          single-line
          density="compact"
          @update:model-value="debouncedFilterCategories"
        ></v-text-field>
      </div>
    </div>

    <div class="content-grid">
      <!-- Empty Left Column -->
      <div class="empty-column"></div>

      <!-- Main Content Column -->
      <div class="main-content-column">
        <!-- Main Content -->
        <div v-if="!loading">
          <!-- Category View -->
          <div class="category-section">
            <div
              v-for="category in filteredCategories"
              :key="category.id"
              class="category-item"
            >
              <div class="text-center mb-2">
                <div class="text-h6 font-weight-bold text-center japanese-text">
                  {{ category.name }}
                </div>
              </div>

              <div class="category-card">
                <v-card
                  class="category-lesson-card"
                  variant="flat"
                  rounded="lg"
                  @click="selectCategory(category)"
                >
                  <v-img
                    :src="getImagePath(category.name)"
                    height="180px"
                    width="100%"
                    aspect-ratio="1"
                    cover
                    class="position-relative"
                  >
                    <div
                      class="category-overlay d-flex flex-column justify-center align-center"
                    >
                      <div
                        class="text-subtitle-1 font-weight-bold text-white text-center"
                      >
                        {{ category.meaning }}
                      </div>
                      <div class="text-caption text-white mt-1">
                        {{ getTopicsCount(category) }} topics
                      </div>
                    </div>
                  </v-img>
                </v-card>
              </div>
            </div>

            <!-- Empty State when no categories match search -->
            <div
              v-if="filteredCategories.length === 0"
              class="text-center py-8"
            >
              <v-icon
                size="large"
                icon="mdi-book-search-outline"
                class="mb-4"
              ></v-icon>
              <h3 class="text-h6">カテゴリーが見つかりません</h3>
              <p class="text-body-1 text-medium-emphasis">
                No categories match your search criteria.
              </p>
              <v-btn color="primary" class="mt-4" @click="clearAllFilters">
                Clear Search
              </v-btn>
            </div>
          </div>
        </div>

        <!-- Loading Skeleton -->
        <template v-else>
          <v-skeleton-loader
            v-for="i in 4"
            :key="i"
            type="list-item-avatar"
            class="mb-2"
          />
        </template>
      </div>

      <!-- Right Column: Banners/Guidelines -->
      <div class="banner-column">
        <!-- Learning Path Banner -->
        <v-card class="mb-4 learning-path-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-map-marker-path</v-icon>
            学習経路
          </v-card-title>
          <v-card-text>
            <p class="text-body-2">
              カテゴリーとトピックを選んで、IT用語の学習を始めましょう。
            </p>
            <div class="d-flex align-center mt-2">
              <v-icon size="small" color="success" class="mr-2"
                >mdi-check-circle</v-icon
              >
              <span class="text-body-2">基本から応用まで体系的に学べます</span>
            </div>
          </v-card-text>
        </v-card>

        <!-- Study Tips Banner -->
        <v-card class="mb-4 study-tips-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-lightbulb-on</v-icon>
            学習のコツ
          </v-card-title>
          <v-card-text>
            <ul class="pl-4">
              <li class="text-body-2 mb-1">毎日少しずつ学習しましょう</li>
              <li class="text-body-2 mb-1">音声を聞いて発音を練習しましょう</li>
              <li class="text-body-2 mb-1">例文を使って実践的に学びましょう</li>
              <li class="text-body-2">不明点はChatGPTに質問しましょう</li>
            </ul>
          </v-card-text>
        </v-card>

        <!-- Resources Banner -->
        <v-card class="resources-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-bookshelf</v-icon>
            学習リソース
          </v-card-title>
          <v-card-text>
            <v-img
              src="/images/study-resources.png"
              height="120"
              cover
              class="mb-2 rounded"
            ></v-img>
            <p class="text-body-2">
              IT分野の専門用語を効率よく習得するための追加リソースをご利用ください。
            </p>
            <v-btn
              color="primary"
              variant="text"
              class="px-0 mt-2"
              prepend-icon="mdi-open-in-new"
              density="comfortable"
            >
              リソースを見る
            </v-btn>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- Filter Dialog -->
    <v-dialog v-model="showFilterDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 font-weight-bold">
          カテゴリーフィルター
        </v-card-title>
        <v-card-text>
          <v-text-field
            v-model="search"
            label="カテゴリー検索"
            variant="outlined"
            clearable
            class="mb-4"
            prepend-inner-icon="mdi-magnify"
            @update:model-value="debouncedFilterCategories"
          ></v-text-field>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text @click="clearAllFilters">キャンセル</v-btn>
          <v-btn color="primary" @click="showFilterDialog = false">確認</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import vocabularyService from "@/services/vocabulary.service";
import { useAppToast } from "@/composables/useAppToast";
import { useCategoriesStore } from "@/stores";

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

interface Category {
  id: string;
  name: string;
  meaning: string;
  displayOrder: number;
}

const router = useRouter();
const toast = useAppToast();
const categoriesStore = useCategoriesStore();

// State variables
const loading = ref(false);
const categories = ref<Category[]>([]);
const topics = ref<Topic[]>([]);
const search = ref("");
const showFilterDialog = ref(false);

// Computed properties
const filteredCategories = computed(() => {
  if (!search.value) return categories.value;

  return categories.value.filter((category) => {
    return (
      category.name.toLowerCase().includes(search.value.toLowerCase()) ||
      category.meaning.toLowerCase().includes(search.value.toLowerCase())
    );
  });
});

// Debounce function for category search
let categorySearchTimeout: ReturnType<typeof setTimeout> | null = null;
function debouncedFilterCategories() {
  clearTimeout(categorySearchTimeout ?? undefined);
  categorySearchTimeout = setTimeout(() => {
    // We don't need to fetch categories as they're already loaded
    // Just let the v-for directive filter them based on search.value
  }, 300);
}

// Lifecycle hooks
onMounted(async () => {
  await Promise.all([fetchCategories(), fetchTopics()]);
});

// API Methods
async function fetchCategories() {
  try {
    loading.value = true;
    await categoriesStore.fetchCategories();

    categories.value = categoriesStore.categories.map((category) => ({
      id: category.categoryId,
      name: category.name,
      meaning: category.meaning ?? "",
      displayOrder: category.displayOrder || 0,
    }));
  } catch {
    toast.error("Failed to load categories");
  } finally {
    loading.value = false;
  }
}

async function fetchTopics() {
  try {
    const response = await vocabularyService.getTopics();

    if (Array.isArray(response)) {
      topics.value = response.map((topic) => {
        return {
          id: topic.id || topic.topicId,
          name: topic.name,
          meaning: topic.meaning,
          categoryId: topic.categoryId || topic.category?.id,
          displayOrder: topic.displayOrder || 0,
          vocabularyCount: topic.vocabularyCount || 0,
        };
      });
    } else {
      topics.value = [];
    }
  } catch {
    toast.error("Failed to load topics");
  }
}

// Navigation and UI Methods
function goBack() {
  router.back();
}

function getImagePath(categoryName: string): string {
  const categoryImages: Record<string, string> = {
    IT基礎: "it-basics.png",
    プログラミング: "programming.png",
    ウェブ開発: "web-dev.png",
    データベース: "database.png",
    "人工知能・データ": "ai-data.png",
    コミュニケーション: "communication.png",
    実務とキャリア: "career.png",
  };

  return `/images/categories/${categoryImages[categoryName] || "default.png"}`;
}

function getTopicsCount(category: Category): number {
  const matchingTopics = topics.value.filter((t) => {
    const topicCategoryId = t.categoryId || (t.category && t.category.id);
    return topicCategoryId === category.id;
  });
  return matchingTopics.length;
}

function selectCategory(category: Category) {
  // Navigate to the category detail page
  router.push({
    name: "categoryDetail",
    params: { slug: encodeURIComponent(category.name) },
  });
}

function openFilterDialog() {
  showFilterDialog.value = true;
}

function clearAllFilters() {
  search.value = "";
  showFilterDialog.value = false;
}
</script>

<style scoped lang="scss">
.vocabulary-learning-container {
  background-color: #f7f8fa;
  min-height: 100vh;
  padding-bottom: 64px;
}

.content-grid {
  display: grid;
  grid-template-columns: 180px 1fr 320px 120px;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
}

.empty-column {
  width: 100%;
}

.main-content-column {
  width: 100%;
  background-color: #f0f1f1;
  border-radius: 8px;
  padding-left: 8px;
  padding-right: 8px;
}

.banner-column {
  width: 100%;
}

/* Responsive layout for different screen sizes */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 40px 1fr 320px 40px;
  }
}

@media (max-width: 960px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .banner-column,
  .empty-column {
    display: none;
  }

  .main-content-column {
    padding-left: 8px;
    padding-right: 8px;
  }

  .category-section {
    max-width: 100%;
  }
}

/* Make categories 2 column on mobile */
@media (max-width: 768px) {
  .category-section {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .category-card {
    max-width: 200px;
  }

  .category-item {
    margin-bottom: 20px;
  }

  .text-h6.japanese-text {
    font-size: 1rem !important;
  }
}

@media (max-width: 600px) {
  .category-card {
    max-width: 150px;
  }

  .text-h6.japanese-text {
    font-size: 0.9rem !important;
  }

  .category-item .text-caption {
    font-size: 0.75rem !important;
  }
}

.japanese-text {
  font-family: "Noto Sans JP", sans-serif;
}

.search-field {
  background-color: #f0f0f0;
  height: 48px;
  border-radius: 24px;
}

.category-item {
  margin-bottom: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.category-card {
  width: 100%;
  position: relative;
  aspect-ratio: 1;
  max-width: 300px;
}

.category-lesson-card {
  overflow: hidden;
  transition: transform 0.2s ease;
  max-width: 100%;
  height: 100%;
  background-color: transparent;
  display: flex;
  flex-direction: column;
  border: none;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1) !important;
  }
}

.category-overlay {
  background: linear-gradient(
    0deg,
    rgba(0, 0, 0, 0.75) 0%,
    rgba(0, 0, 0, 0.4) 100%
  );
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  padding-left: 8px;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

/* Banner card styles */
.learning-path-card {
  border-left: 4px solid #42a5f5 !important;
}

.study-tips-card {
  border-left: 4px solid #66bb6a !important;
}

.resources-card {
  border-left: 4px solid #ffa726 !important;
}

.category-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 2px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}
</style>
