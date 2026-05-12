<template>
  <div class="conversation-learning-container">
    <!-- Header Section -->
    <div class="header py-3 d-flex justify-space-between align-center px-4">
      <div class="d-flex align-center">
        <v-btn icon class="mr-2 back-button" @click="goBack" variant="tonal">
          <v-icon>mdi-chevron-left</v-icon>
        </v-btn>
        <div class="text-h6 font-weight-bold text-dark">
          <span class="text-primary">会話</span>
          <span class="japanese-text">練習</span>
          <div class="title-underline"></div>
        </div>
      </div>
      <div class="d-flex align-center">
        <v-btn
          icon
          variant="tonal"
          class="filter-button"
          @click="openFilterDialog"
        >
          <v-icon>mdi-filter-variant</v-icon>
        </v-btn>
      </div>
    </div>

    <!-- Search Field -->
    <div class="search-container px-4 mb-4">
      <div class="search-field d-flex align-center pa-2 rounded-pill">
        <v-text-field
          v-model="search"
          placeholder="検索 / Tìm kiếm hội thoại"
          prepend-inner-icon="mdi-magnify"
          variant="plain"
          hide-details
          single-line
          density="compact"
          @update:model-value="debouncedSearch"
          @focus="searchFieldFocused = true"
          @blur="searchFieldFocused = false"
          :class="{ 'search-focused': searchFieldFocused }"
        ></v-text-field>
      </div>
    </div>

    <div class="content-grid">
      <!-- Empty Left Column -->
      <div class="empty-column"></div>

      <!-- Main Content Column -->
      <div class="main-content-column">
        <!-- Loading Skeleton -->
        <div v-if="loading" class="pt-0">
          <v-sheet
            v-for="i in 3"
            :key="i"
            class="pa-4 mb-4 rounded-lg"
            :elevation="2"
          >
            <v-skeleton-loader
              type="list-item-avatar-two-line, divider, actions"
              class="skeleton-loader"
            ></v-skeleton-loader>
          </v-sheet>
        </div>

        <!-- Error Alert -->
        <div v-else-if="error" class="py-0">
          <v-alert
            type="error"
            variant="tonal"
            class="mb-6 error-alert"
            border="start"
            :border-color="'error'"
            elevation="2"
          >
            <template v-slot:prepend>
              <v-icon color="error" size="large">mdi-alert-circle</v-icon>
            </template>
            <div class="text-body-1 font-weight-medium">{{ errorMessage }}</div>
            <v-btn
              class="mt-3"
              color="error"
              variant="tonal"
              @click="fetchConversations"
              prepend-icon="mdi-refresh"
            >
              Thử lại
            </v-btn>
          </v-alert>
        </div>

        <!-- No Conversations Found -->
        <div v-else-if="filteredConversations.length === 0" class="py-0">
          <v-alert
            type="info"
            variant="tonal"
            class="mb-6 empty-alert"
            border="start"
            :border-color="'info'"
            elevation="2"
          >
            <template v-slot:prepend>
              <v-icon color="info" size="large">mdi-information</v-icon>
            </template>
            <div class="text-body-1 font-weight-medium">
              Không tìm thấy cuộc hội thoại nào. Vui lòng quay lại sau.
            </div>
            <v-btn
              v-if="search"
              class="mt-3"
              color="primary"
              variant="tonal"
              @click="clearSearch"
            >
              Xóa tìm kiếm
            </v-btn>
          </v-alert>
        </div>

        <!-- Conversation List -->
        <div v-else class="conversation-list py-0">
          <div class="active-filters mb-4" v-if="search || jlptFilter">
            <div class="d-flex align-center">
              <v-icon size="small" color="primary" class="mr-2"
                >mdi-filter-outline</v-icon
              >
              <span class="text-body-2 text-medium-emphasis"
                >Bộ lọc hiện tại:</span
              >

              <v-chip
                v-if="search"
                size="small"
                closable
                class="ml-2"
                @click:close="search = ''"
              >
                Từ khóa: {{ search }}
              </v-chip>

              <v-chip
                v-if="jlptFilter"
                size="small"
                closable
                class="ml-2"
                :color="getJlptColor(jlptFilter)"
                @click:close="jlptFilter = ''"
              >
                {{ jlptFilter }}
              </v-chip>

              <v-btn
                v-if="search || jlptFilter"
                variant="text"
                size="small"
                color="primary"
                class="ml-auto"
                @click="clearAllFilters"
              >
                Xóa tất cả
              </v-btn>
            </div>
          </div>

          <transition-group
            name="conversation-list"
            tag="div"
            class="conversation-list-container"
          >
            <v-card
              v-for="(conversation, index) in filteredConversations"
              :key="conversation.conversationId"
              class="conversation-card mb-4"
              variant="outlined"
              @click="navigateToConversation(conversation.conversationId)"
              :style="{ animationDelay: `${index * 0.1}s` }"
              :class="`jlpt-${conversation.jlptLevel?.toLowerCase()}-card`"
            >
              <div class="d-flex">
                <div
                  class="conversation-content flex-grow-1 d-flex flex-column"
                >
                  <v-card-item>
                    <template v-slot:prepend>
                      <v-avatar
                        color="transparent"
                        variant="tonal"
                        class="conversation-avatar mr-3"
                      >
                        <v-img
                          :src="getConversationImage(conversation.jlptLevel)"
                        />
                      </v-avatar>
                    </template>
                    <v-card-title
                      class="text-h6 font-weight-bold mb-1 d-flex align-center"
                    >
                      {{ conversation.title }}
                    </v-card-title>
                    <v-card-subtitle class="text-body-2">
                      {{ getDescription(conversation) }}
                    </v-card-subtitle>
                  </v-card-item>

                  <v-divider class="mx-4 my-1"></v-divider>

                  <v-card-text class="pt-2">
                    <div class="d-flex flex-wrap conversation-meta">
                      <div class="meta-item mr-4">
                        <v-icon size="small" color="primary" class="mr-1"
                          >mdi-message-text</v-icon
                        >
                        <span class="text-body-2 font-weight-medium"
                          >{{ getLineCount(conversation) }} câu hội thoại</span
                        >
                      </div>

                      <div class="meta-item mr-4">
                        <v-icon size="small" color="primary" class="mr-1"
                          >mdi-book-open-variant</v-icon
                        >
                        <span class="text-body-2 font-weight-medium"
                          >Bài {{ conversation.unit || "N/A" }}</span
                        >
                      </div>

                      <div class="meta-item">
                        <v-icon
                          size="small"
                          :color="getDifficultyColor(conversation.jlptLevel)"
                          class="mr-1"
                        >
                          {{ getDifficultyIcon(conversation.jlptLevel) }}
                        </v-icon>
                        <span
                          class="text-body-2 font-weight-medium"
                          :class="`text-${getDifficultyColor(conversation.jlptLevel)}`"
                        >
                          {{ getDifficultyText(conversation.jlptLevel) }}
                        </span>
                      </div>

                      <v-spacer></v-spacer>

                      <v-btn
                        :color="getJlptColor(conversation.jlptLevel)"
                        variant="tonal"
                        class="action-button"
                        rounded
                        density="comfortable"
                      >
                        Học ngay
                        <v-icon end>mdi-arrow-right</v-icon>
                      </v-btn>
                    </div>
                  </v-card-text>
                </div>
              </div>
            </v-card>
          </transition-group>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="d-flex justify-center mt-6">
            <v-pagination
              v-model="currentPage"
              :length="totalPages"
              @update:model-value="handlePageChange"
              rounded
              color="primary"
              active-color="primary"
              class="custom-pagination"
            ></v-pagination>
          </div>
        </div>
      </div>

      <!-- Right Column: Banners/Guidelines -->
      <div class="banner-column">
        <!-- JLPT Levels Banner -->
        <v-card class="mb-4 jlpt-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-certificate-outline</v-icon>
            JLPT レベル
          </v-card-title>
          <v-card-text>
            <div class="d-flex flex-wrap jlpt-chips">
              <v-chip
                v-for="level in jlptLevels"
                :key="level"
                :color="getJlptColor(level)"
                class="ma-1 jlpt-chip"
                @click="jlptFilter = level === jlptFilter ? '' : level"
                :class="{ 'jlpt-selected': level === jlptFilter }"
              >
                {{ level }}
              </v-chip>
            </div>
            <p class="text-body-2 mt-2">
              レベルをクリックして、フィルターを適用します。
            </p>
          </v-card-text>
        </v-card>

        <!-- Learning Path Banner -->
        <v-card class="mb-4 learning-path-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-map-marker-path</v-icon>
            学習経路
          </v-card-title>
          <v-card-text>
            <p class="text-body-2">
              様々なシチュエーションの会話を練習しましょう。
            </p>
            <div class="d-flex align-center mt-2">
              <v-icon size="small" color="success" class="mr-2"
                >mdi-check-circle</v-icon
              >
              <span class="text-body-2">基本的な会話からスタート</span>
            </div>
            <div class="d-flex align-center mt-2">
              <v-icon size="small" color="success" class="mr-2"
                >mdi-check-circle</v-icon
              >
              <span class="text-body-2">実践的な練習を通して上達</span>
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
              <li class="text-body-2 mb-1">会話文を声に出して練習しましょう</li>
              <li class="text-body-2 mb-1">単語と文法に注目しましょう</li>
              <li class="text-body-2 mb-1">簡単な会話から練習を始めましょう</li>
              <li class="text-body-2">繰り返し練習することで上達します</li>
            </ul>
          </v-card-text>
        </v-card>

        <!-- Resources Banner -->
        <v-card class="resources-card" variant="outlined" rounded="lg">
          <v-card-title class="text-h6 font-weight-bold">
            <v-icon start class="mr-2">mdi-bookshelf</v-icon>
            会話リソース
          </v-card-title>
          <v-card-text>
            <v-img
              src="/images/study-resources.png"
              height="120"
              cover
              class="mb-2 rounded"
            ></v-img>
            <p class="text-body-2">
              日本語会話のための追加リソースをご利用ください。
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
    <v-dialog v-model="showFilterDialog" max-width="400" class="filter-dialog">
      <v-card>
        <v-card-title class="text-h6 font-weight-bold">
          <v-icon start class="mr-2">mdi-filter-variant</v-icon>
          フィルター設定
        </v-card-title>
        <v-card-text>
          <v-text-field
            v-model="search"
            label="キーワード検索"
            variant="outlined"
            clearable
            class="mb-4"
            prepend-inner-icon="mdi-magnify"
            @update:model-value="debouncedSearch"
          ></v-text-field>

          <v-select
            v-model="jlptFilter"
            label="JLPT レベル"
            :items="jlptLevels"
            variant="outlined"
            clearable
            class="mb-4"
            @update:model-value="debouncedSearch"
          ></v-select>

          <p class="text-caption text-medium-emphasis mt-2">
            適用されたフィルター: {{ getActiveFiltersCount() }}
          </p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text @click="clearAllFilters">フィルタークリア</v-btn>
          <v-btn color="primary" @click="showFilterDialog = false">確認</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import conversationService from "@/services/conversation.service";
import type {
  Conversation,
  PagedResponse,
} from "@/services/conversation.service";
import { useAppToast } from "@/composables/useAppToast";

// Router and toast
const router = useRouter();
const toast = useAppToast();

// State variables
const conversations = ref<Conversation[]>([]);
const loading = ref(true);
const error = ref(false);
const errorMessage = ref("Đã xảy ra lỗi khi tải dữ liệu hội thoại.");
const currentPage = ref(1); // 1-based for v-pagination
const totalPages = ref(0);
const pageSize = ref(10);
const search = ref("");
const jlptFilter = ref("");
const showFilterDialog = ref(false);
const jlptLevels = ref(["N1", "N2", "N3", "N4", "N5"]);
const searchFieldFocused = ref(false);

// Computed properties
const filteredConversations = computed(() => {
  if (!search.value && !jlptFilter.value) return conversations.value;

  return conversations.value.filter((conversation) => {
    const matchesSearch =
      !search.value ||
      conversation.title?.toLowerCase().includes(search.value.toLowerCase()) ||
      (conversation.description?.toLowerCase() || "").includes(
        search.value.toLowerCase(),
      );

    const matchesJlpt =
      !jlptFilter.value || conversation.jlptLevel === jlptFilter.value;

    return matchesSearch && matchesJlpt;
  });
});

// Debounce function
let searchTimeout: ReturnType<typeof setTimeout> | null = null;
function debouncedSearch() {
  clearTimeout(searchTimeout ?? undefined);
  searchTimeout = setTimeout(() => {
    // Filter is handled by computed property
  }, 300);
}

// Methods
const getJlptColor = (level: string | undefined): string => {
  if (!level) return "grey";

  const colors: Record<string, string> = {
    N1: "red",
    N2: "orange",
    N3: "amber",
    N4: "light-green",
    N5: "green",
  };
  return colors[level] || "grey";
};

const getDifficultyText = (jlptLevel: string | undefined): string => {
  if (!jlptLevel) return "Không xác định";

  const difficulties: Record<string, string> = {
    N1: "Khó",
    N2: "Khó",
    N3: "Trung bình",
    N4: "Trung bình",
    N5: "Dễ",
  };
  return difficulties[jlptLevel] || "Không xác định";
};

const getDifficultyColor = (jlptLevel: string | undefined): string => {
  if (!jlptLevel) return "grey";

  const difficultyText = getDifficultyText(jlptLevel);
  if (difficultyText === "Dễ") return "success";
  if (difficultyText === "Trung bình") return "warning";
  if (difficultyText === "Khó") return "error";
  return "grey";
};

const getDifficultyIcon = (jlptLevel: string | undefined): string => {
  if (!jlptLevel) return "mdi-help-circle-outline";

  const difficultyText = getDifficultyText(jlptLevel);
  if (difficultyText === "Dễ") return "mdi-star-outline";
  if (difficultyText === "Trung bình") return "mdi-star-half-full";
  if (difficultyText === "Khó") return "mdi-star";
  return "mdi-help-circle-outline";
};

const getLineCount = (conversation: Conversation): number => {
  return conversation.lines?.length || 0;
};

const getDescription = (conversation: Conversation): string => {
  return conversation.description || "Không có mô tả";
};

const getActiveFiltersCount = (): number => {
  let count = 0;
  if (search.value) count++;
  if (jlptFilter.value) count++;
  return count;
};

const getConversationImage = (jlptLevel: string | undefined): string => {
  // Nếu có ảnh thực, trả về path ảnh
  // Trong trường hợp này, tạm thời chưa có ảnh thực
  // return `/images/conversations/${jlptLevel?.toLowerCase() || 'default'}.jpg`;

  // Thay vào đó, trả về SVG encoded dựa trên cấp độ JLPT
  const color = getJlptColorHex(jlptLevel);
  const bgColor = getJlptColorHexLight(jlptLevel);

  // SVG cho biểu tượng hội thoại
  const svgContent = `
    <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
      <rect width="120" height="120" fill="${bgColor}" />
      <g transform="translate(20, 30)">
        <path d="M10,0 L50,0 C55.5228,0 60,4.47715 60,10 L60,30 C60,35.5228 55.5228,40 50,40 L40,40 L30,50 L30,40 L10,40 C4.47715,40 0,35.5228 0,30 L0,10 C0,4.47715 4.47715,0 10,0 Z" fill="${color}" opacity="0.9" />
        <path d="M70,15 L80,15 C85.5228,15 90,19.4772 90,25 L90,45 C90,50.5229 85.5228,55 80,55 L75,55 L65,65 L65,55 L50,55 C44.4772,55 40,50.5229 40,45 L40,35" fill="${color}" opacity="0.7" />
        <text x="22" y="26" font-family="Arial" font-size="16" font-weight="bold" fill="white">${jlptLevel || "JP"}</text>
      </g>
    </svg>
  `;

  // Chuyển đổi SVG thành data URL
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgContent)}`;
};

// Hàm lấy mã màu hex cho mỗi cấp độ JLPT
const getJlptColorHex = (level: string | undefined): string => {
  if (!level) return "#9E9E9E"; // Màu xám mặc định

  const colors: Record<string, string> = {
    N1: "#F44336", // Đỏ
    N2: "#FF9800", // Cam
    N3: "#FFC107", // Vàng
    N4: "#8BC34A", // Xanh lá nhạt
    N5: "#4CAF50", // Xanh lá
  };

  return colors[level] || "#9E9E9E";
};

// Hàm lấy màu hex nhạt cho nền
const getJlptColorHexLight = (level: string | undefined): string => {
  if (!level) return "#F5F5F5"; // Màu xám nhạt mặc định

  const colors: Record<string, string> = {
    N1: "#FFEBEE", // Đỏ nhạt
    N2: "#FFF3E0", // Cam nhạt
    N3: "#FFF8E1", // Vàng nhạt
    N4: "#F1F8E9", // Xanh lá nhạt
    N5: "#E8F5E9", // Xanh lá nhạt
  };

  return colors[level] || "#F5F5F5";
};

const navigateToConversation = (id: string | undefined) => {
  if (!id) {
    toast.error("ID cuộc hội thoại không hợp lệ");
    return;
  }

  router.push({
    name: "conversationPractice",
    params: { id },
  });
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  // Scroll to top when changing page
  window.scrollTo({ top: 0, behavior: "smooth" });
  fetchConversations();
};

const goBack = () => {
  router.back();
};

const openFilterDialog = () => {
  showFilterDialog.value = true;
};

const clearSearch = () => {
  search.value = "";
};

const clearAllFilters = () => {
  search.value = "";
  jlptFilter.value = "";
  showFilterDialog.value = false;
};

// Fetch conversations from API
const fetchConversations = async () => {
  loading.value = true;
  error.value = false;

  try {
    // Chuyển đổi currentPage từ 1-based sang 0-based cho API
    const apiPage = currentPage.value - 1;

    // Sử dụng API public thay vì API admin vì API public đã được triển khai
    const response = await conversationService.getConversations(
      apiPage,
      pageSize.value,
      search.value, // search
      "unit", // sortBy - sắp xếp theo bài học
      "asc", // sortDir - tăng dần
    );

    // Lấy dữ liệu từ response
    const data: PagedResponse<Conversation> = response.data;

    conversations.value = data.content;
    totalPages.value = data.totalPages;

    if (conversations.value.length === 0 && currentPage.value > 1) {
      // Nếu trang hiện tại không có dữ liệu và không phải trang đầu tiên, quay lại trang trước
      currentPage.value--;
      await fetchConversations();
    }
  } catch {
    error.value = true;
    errorMessage.value =
      "Đã xảy ra lỗi khi tải dữ liệu hội thoại. Vui lòng thử lại sau.";

    // Show error toast
    toast.error("Không thể tải danh sách hội thoại");
  } finally {
    loading.value = false;
  }
};

// Initial data fetch
onMounted(() => {
  fetchConversations();
});
</script>

<style scoped lang="scss">
.conversation-learning-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 64px;

  .header {
    position: relative;

    .text-h6 {
      position: relative;

      .title-underline {
        position: absolute;
        bottom: -4px;
        left: 0;
        width: 40px;
        height: 2px;
        background-color: rgb(var(--v-theme-primary));
        transition: width 0.3s ease;
      }

      &:hover .title-underline {
        width: 100%;
      }
    }
  }
}

.japanese-text {
  font-family: "Noto Sans JP", sans-serif;
}

.content-grid {
  display: grid;
  grid-template-columns: 72px 1fr 320px 120px;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
  padding-right: 24px;
}

.empty-column {
  width: 100%;
}

.main-content-column {
  width: 100%;
  background-color: transparent;
  padding: 0;
}

.banner-column {
  width: 100%;
  padding-top: 0;
}

.search-field {
  background-color: rgba(var(--v-theme-surface-variant), 0.1);
  height: 48px;
  border-radius: 24px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

  &.search-focused {
    box-shadow: 0 3px 10px rgba(var(--v-theme-primary), 0.1);
    background-color: rgba(var(--v-theme-surface-variant), 0.2);
  }
}

.active-filters {
  background-color: rgba(var(--v-theme-surface-variant), 0.05);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px dashed rgba(var(--v-theme-primary), 0.2);
  margin-bottom: 12px;
}

.conversation-list-container {
  position: relative;
}

.conversation-list {
  margin-top: 0;
}

.conversation-card:first-child {
  margin-top: 0;
}

// Animation for list items
.conversation-list-enter-active,
.conversation-list-leave-active {
  transition: all 0.3s ease;
}
.conversation-list-enter-from,
.conversation-list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.conversation-card {
  overflow: hidden;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgba(var(--v-theme-primary), 0.1);
  animation: fadeIn 0.5s ease-out forwards;
  opacity: 0;

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &.jlpt-n1-card {
    border-left: 4px solid #f44336 !important;
    background-color: rgba(244, 67, 54, 0.03);
  }

  &.jlpt-n2-card {
    border-left: 4px solid #ff9800 !important;
    background-color: rgba(255, 152, 0, 0.03);
  }

  &.jlpt-n3-card {
    border-left: 4px solid #ffc107 !important;
    background-color: rgba(255, 193, 7, 0.03);
  }

  &.jlpt-n4-card {
    border-left: 4px solid #8bc34a !important;
    background-color: rgba(139, 195, 74, 0.03);
  }

  &.jlpt-n5-card {
    border-left: 4px solid #4caf50 !important;
    background-color: rgba(76, 175, 80, 0.03);
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1) !important;
    border-color: rgba(var(--v-theme-primary), 0.2);

    &.jlpt-n1-card {
      background-color: rgba(244, 67, 54, 0.05);
    }

    &.jlpt-n2-card {
      background-color: rgba(255, 152, 0, 0.05);
    }

    &.jlpt-n3-card {
      background-color: rgba(255, 193, 7, 0.05);
    }

    &.jlpt-n4-card {
      background-color: rgba(139, 195, 74, 0.05);
    }

    &.jlpt-n5-card {
      background-color: rgba(76, 175, 80, 0.05);
    }

    .conversation-img {
      transform: scale(1.05);
    }

    .action-button {
      background-color: rgb(var(--v-theme-primary)) !important;
      color: white !important;
      transform: scale(1.05);
    }
  }

  .conversation-avatar {
    width: 52px;
    height: 52px;
    transition: all 0.3s ease;
    border-radius: 50%;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    &:hover {
      transform: scale(1.1);
    }
  }

  .conversation-content {
    padding: 0 16px;
    width: 100%;

    .v-card-title {
      font-size: 1.25rem;
      line-height: 1.4;
      letter-spacing: -0.01em;
    }

    .v-card-subtitle {
      line-height: 1.5;
    }

    .conversation-meta {
      .meta-item {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
      }
    }

    .difficulty-chip {
      font-weight: 600;
      letter-spacing: 0.01em;
    }
  }
}

/* Banner card styles */
.jlpt-card,
.conversation-card:first-child {
  margin-top: 0;
}

.jlpt-card {
  border-left: 4px solid #9c27b0 !important;
  background-color: rgba(156, 39, 176, 0.05);

  .jlpt-chips {
    justify-content: center;

    .jlpt-chip {
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
      }

      &.jlpt-selected {
        transform: scale(1.1);
        font-weight: bold;
      }
    }
  }
}

.learning-path-card {
  border-left: 4px solid #42a5f5 !important;
  background-color: rgba(66, 165, 245, 0.05);
}

.study-tips-card {
  border-left: 4px solid #66bb6a !important;
  background-color: rgba(102, 187, 106, 0.05);
}

.resources-card {
  border-left: 4px solid #ffa726 !important;
  background-color: rgba(255, 167, 38, 0.05);
}

.error-alert,
.empty-alert {
  border-radius: 12px;
  overflow: hidden;
}

.skeleton-loader {
  background-color: rgba(var(--v-theme-surface-variant), 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.back-button,
.filter-button {
  transition: all 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }
}

.custom-pagination {
  :deep(.v-pagination__item) {
    transition: transform 0.2s ease;

    &:hover:not(.v-pagination__item--active) {
      transform: scale(1.1);
    }
  }

  :deep(.v-pagination__item--active) {
    transform: scale(1.1);
    font-weight: bold;
  }
}

.filter-dialog {
  :deep(.v-card) {
    border-radius: 16px;
    overflow: hidden;

    .v-card-title {
      font-weight: 600;
      padding-bottom: 16px;
      border-bottom: 1px solid rgba(var(--v-theme-surface-variant), 0.2);
    }
  }
}

/* Responsive layout */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 40px 1fr 320px;
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
    padding: 12px;
  }

  .conversation-card {
    .v-card-title {
      font-size: 1.1rem !important;
      line-height: 1.3;

      .v-chip {
        font-size: 0.7rem;
      }
    }
  }
}

@media (max-width: 768px) {
  .conversation-card {
    .d-flex {
      flex-direction: column;
    }

    .conversation-image {
      width: 100%;
      height: 140px;
    }

    .conversation-action {
      width: 100%;
      padding: 16px;
      justify-content: flex-end !important;
    }

    .v-card-title {
      font-size: 1rem !important;
    }

    .conversation-meta {
      flex-direction: column;
      align-items: flex-start;

      .meta-item {
        margin-right: 0;
        margin-bottom: 6px;
      }
    }
  }
}

@media (max-width: 600px) {
  .conversation-card {
    .conversation-content {
      padding: 8px;
    }

    .v-card-title {
      font-size: 0.95rem !important;
      flex-wrap: wrap;

      .v-chip {
        margin-top: 4px;
        margin-left: 0 !important;
        font-size: 0.65rem;
      }
    }

    .conversation-avatar {
      width: 42px;
      height: 42px;
    }

    .conversation-meta {
      .text-body-2 {
        font-size: 0.8rem !important;
      }
    }
  }
}
</style>
