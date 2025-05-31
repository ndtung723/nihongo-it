<template>
  <div class="category-detail-container">
    <!-- Header Section -->
    <div class="header px-4 py-3 d-flex justify-space-between align-center">
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

    <!-- Category Title -->
    <div v-if="category" class="px-4 mb-3">
      <div class="text-h4 font-weight-bold text-center japanese-text">{{ category.name }}</div>
      <div class="text-subtitle-1 text-center">{{ category.meaning }}</div>
    </div>

    <!-- Loading Indicator -->
    <div v-if="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="text-body-1 text-medium-emphasis mt-3">Loading...</p>
    </div>

    <!-- Topics List -->
    <div v-else-if="category" class="topics-list px-4">
      <div v-for="topic in topicsList" :key="topic.id" class="mb-4">
        <v-card
          class="topic-card"
          variant="outlined"
          rounded="lg"
          @click="handleTopicSelect(topic)"
        >
          <v-card-title class="d-flex align-center">
            <span class="japanese-text">{{ topic.name }}</span>
            <v-spacer></v-spacer>
            <v-chip size="small" color="success">{{ getVocabularyCount(topic) }} words</v-chip>
          </v-card-title>
          <v-card-text>
            <div class="text-body-1">{{ topic.meaning }}</div>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-8">
      <v-icon size="large" icon="mdi-book-search-outline" class="mb-4"></v-icon>
      <h3 class="text-h6">カテゴリーが見つかりません</h3>
      <p class="text-body-1 text-medium-emphasis">
        The requested category could not be found.
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import vocabularyService from '@/services/vocabulary.service'
import { useToast } from 'vue-toast-notification'

// Define interfaces for the data structures
interface Topic {
  id: string
  name: string
  meaning: string
  categoryId?: string
  category?: {
    id: string
    name: string
  }
  displayOrder: number
  vocabularyCount?: number
}

interface Category {
  id: string
  name: string
  meaning: string
  displayOrder: number
}

const router = useRouter()
const route = useRoute()
const toast = useToast()

// State variables
const loading = ref(true)
const category = ref<Category | null>(null)
const topics = ref<Topic[]>([])
const jlptLevels = ref<string[]>([])
const selectedJlptLevel = ref<string | null>(null)
const showFilterDialog = ref(false)

// Computed properties
const topicsList = computed(() => {
  // Sort topics by display order
  return [...topics.value].sort((a, b) => a.displayOrder - b.displayOrder)
})

// Get the category slug from the route
const categorySlug = computed(() => route.params.slug as string)

// Watchers
watch(() => route.params.slug, (newSlug) => {
  if (newSlug) {
    fetchCategoryDetails(newSlug as string)
  }
})

// Lifecycle hooks
onMounted(async () => {
  if (categorySlug.value) {
    await Promise.all([
      fetchCategoryDetails(categorySlug.value),
      fetchJlptLevels()
    ])
  }
})

// Methods
async function fetchCategoryDetails(slug: string) {
  try {
    loading.value = true

    // Fetch all categories first
    const categoriesResponse = await vocabularyService.getCategories()

    if (Array.isArray(categoriesResponse)) {
      // Find the category by slug (assuming slug is either the id or the name)
      const foundCategory = categoriesResponse.find(
        cat => cat.id === slug || cat.name === slug || encodeURIComponent(cat.name) === slug
      )

      if (foundCategory) {
        category.value = {
          id: foundCategory.id || foundCategory.categoryId,
          name: foundCategory.name,
          meaning: foundCategory.meaning,
          displayOrder: foundCategory.displayOrder || 0
        }

        // Fetch topics for this category
        await fetchTopicsByCategory(category.value.id)
      } else {
        category.value = null
        topics.value = []
        toast.error('Category not found', {
          position: 'top',
          duration: 3000
        })
      }
    }
  } catch (error) {
    toast.error('Failed to load category details', {
      position: 'top',
      duration: 3000
    })
    category.value = null
    topics.value = []
  } finally {
    loading.value = false
  }
}

async function fetchTopicsByCategory(categoryId: string) {
  try {
    const response = await vocabularyService.getTopicsByCategory(categoryId)

    if (Array.isArray(response)) {
      topics.value = response.map(topic => {
        return {
          id: topic.id || topic.topicId,
          name: topic.name,
          meaning: topic.meaning,
          categoryId: categoryId,
          displayOrder: topic.displayOrder || 0,
          vocabularyCount: topic.vocabularyCount || 0
        }
      })
    } else {
      topics.value = []
    }
  } catch (error) {
    toast.error('Failed to load topics for this category', {
      position: 'top',
      duration: 3000
    })
    topics.value = []
  }
}

async function fetchJlptLevels() {
  try {
    const response = await vocabularyService.getJlptLevels()
    jlptLevels.value = response
  } catch (error) {
    // Use default values if API call fails
    jlptLevels.value = ['N1', 'N2', 'N3', 'N4', 'N5']
  }
}

function handleTopicSelect(topic: Topic) {

  // Navigate to topic detail view with encoded topic name
  router.push({
    name: 'topicDetail',
    params: { name: encodeURIComponent(topic.name) }
  });
}

function getVocabularyCount(topic: Topic): number | string {
  return topic.vocabularyCount || '?'
}

function goBack() {
  router.back()
}

function goToCategories() {
  router.push({ name: 'vocabularyLearning' })
}

function openFilterDialog() {
  showFilterDialog.value = true
}

function applyFilters() {
  // Navigate to vocabulary list with filters
  router.push({
    name: 'vocabularyLearning',
    query: {
      jlptLevel: selectedJlptLevel.value,
      categoryId: category.value?.id
    }
  })
  showFilterDialog.value = false
}

function clearAllFilters() {
  selectedJlptLevel.value = null
}
</script>

<style scoped lang="scss">
.category-detail-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 64px;
}

.japanese-text {
  font-family: 'Noto Sans JP', sans-serif;
}

.topic-card {
  transition: transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
  }
}
</style>
