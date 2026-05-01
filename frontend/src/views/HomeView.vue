<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section text-center py-16">
      <v-container>
        <h1 class="hero-title display-2 font-weight-bold mb-4">Chinh phục Tiếng Nhật IT</h1>
        <p class="hero-subtitle headline mb-8">Nền tảng toàn diện giúp bạn tự tin giao tiếp và làm việc trong ngành CNTT tại Nhật Bản.</p>
        <v-btn color="primary" large :to="{ name: 'vocabularyLearning' }" class="mx-2">Bắt đầu lộ trình</v-btn>
        <v-btn color="secondary" large :to="{ name: 'vocabulary' }" class="mx-2">Khám phá từ vựng</v-btn>
      </v-container>
    </section>

    <!-- User Dashboard Summary (If logged in) -->
    <section v-if="isLoggedIn" class="user-dashboard-summary py-10">
      <v-container>
        <v-row>
          <v-col cols="12">
            <h2 class="section-heading text-h5 font-weight-bold mb-3">Chào mừng trở lại, {{ username }}!</h2>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="12" md="4">
            <v-card class="pa-4 fill-height d-flex flex-column">
              <v-card-title>Tiến độ của bạn</v-card-title>
              <v-card-text class="flex-grow-1">
                <p>Trình độ hiện tại: <strong>{{ currentLevel }}</strong></p>
                <v-progress-linear :model-value="dailyGoalProgress" color="success" height="20" rounded class="my-2">
                  <strong>{{ dailyGoalProgress }}% mục tiêu ngày</strong>
                </v-progress-linear>
                <p>{{ dailyProgress.minutesStudied }} / {{ studyPlan.dailyGoalMinutes }} phút đã học</p>
              </v-card-text>
              <v-card-actions>
                <v-btn color="primary" block :to="{ name: 'vocabularyLearning' }">Tiếp tục học</v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
          <v-col cols="12" md="8">
            <v-card class="pa-4 fill-height">
              <v-card-title>Gợi ý cho hôm nay</v-card-title>
              <v-card-text v-if="suggestedLessons.length > 0">
                <v-list-item :to="getLessonLink(suggestedLessons[0])">
                  <v-list-item-content>
                    <v-list-item-title class="font-weight-medium">{{ suggestedLessons[0].title }}</v-list-item-title>
                    <v-list-item-subtitle>{{ suggestedLessons[0].description }}</v-list-item-subtitle>
                  </v-list-item-content>
                  <v-list-item-action>
                    <v-icon>mdi-arrow-right</v-icon>
                  </v-list-item-action>
                </v-list-item>
                 <v-divider class="my-2" v-if="suggestedLessons.length > 1"></v-divider>
                 <v-list-item v-if="suggestedLessons.length > 1" :to="getLessonLink(suggestedLessons[1])">
                   <v-list-item-content>
                    <v-list-item-title class="font-weight-medium">{{ suggestedLessons[1].title }}</v-list-item-title>
                  </v-list-item-content>
                  <v-list-item-action>
                    <v-icon>mdi-arrow-right</v-icon>
                  </v-list-item-action>
                 </v-list-item>
              </v-card-text>
              <v-card-text v-else>
                  <p>Không có bài học nào được đề xuất hôm nay. Hãy khám phá các chủ đề khác!</p>
              </v-card-text>
              <v-card-actions>
                <v-btn text color="primary" :to="{ name: 'vocabularyLearning' }">Xem tất cả bài học</v-btn>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <!-- Features Highlight Section -->
    <section class="features-highlight py-12 bg-grey-lighten-4">
      <v-container>
        <h2 class="section-heading text-h5 font-weight-bold text-center mb-10">Công cụ & Tài nguyên Ưu Việt</h2>
        <v-row>
          <v-col cols="12" md="4">
            <v-card class="text-center pa-5 fill-height feature-item-card">
              <v-icon size="50" color="primary" class="mb-3">mdi-book-open-page-variant-outline</v-icon>
              <h3 class="text-h6 mb-2">Từ vựng IT Chuyên sâu</h3>
              <p>Học và tra cứu hàng ngàn thuật ngữ IT tiếng Nhật.</p>
              <v-btn color="primary" variant="outlined" :to="{ name: 'vocabulary' }">Khám phá Từ vựng</v-btn>
            </v-card>
          </v-col>
          <v-col cols="12" md="4">
            <v-card class="text-center pa-5 fill-height feature-item-card">
              <v-icon size="50" color="primary" class="mb-3">mdi-translate</v-icon>
              <h3 class="text-h6 mb-2">Dịch thuật Thông minh</h3>
              <p>Dịch Việt-Nhật & Nhật-Việt nhanh chóng, chính xác.</p>
              <v-btn color="primary" variant="outlined" @click="scrollToTranslation">Thử Dịch ngay</v-btn>
            </v-card>
          </v-col>
          <v-col cols="12" md="4">
            <v-card class="text-center pa-5 fill-height feature-item-card">
              <v-icon size="50" color="primary" class="mb-3">mdi-alphabet-japanese</v-icon>
              <h3 class="text-h6 mb-2">Tạo Furigana Tức thì</h3>
              <p>Dễ dàng thêm Furigana vào bất kỳ văn bản tiếng Nhật nào.</p>
              <v-btn color="primary" variant="outlined" :to="{ name: 'furigana' }">Tạo Furigana</v-btn>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <!-- Main Content Area (Lessons, Vocab, Tools) -->
    <v-container class="main-content py-10">
      <v-row>
        <!-- Left Column: Main tools and content -->
        <v-col cols="12" md="8">
            <!-- Search Bar -->
            <v-card variant="outlined" class="search-card-integrated mb-8">
              <v-text-field
                v-model="searchQuery"
                prepend-inner-icon="mdi-magnify"
                placeholder="Tìm kiếm từ vựng, bài học..."
                hide-details
                variant="solo"
                density="comfortable"
                class="search-input"
                @keyup.enter="searchVocabulary"
              ></v-text-field>
              <v-btn color="primary" @click="searchVocabulary" class="search-btn-integrated">Tìm</v-btn>
            </v-card>

          <!-- Translation Box (can be ref'd for scrolling) -->
          <div ref="translationSection">
             <v-card class="translation-card mb-8">
                 <v-card-title class="translation-title">
                    <div class="d-flex align-center">
                        <v-icon color="amber-darken-2" class="me-2">mdi-translate</v-icon>
                        Công cụ dịch thuật
                    </div>
                </v-card-title>
                <v-card-text class="pa-0">
                <div class="translation-container">
                    <div class="translation-options pa-3">
                    <v-btn-toggle
                        v-model="translationDirection"
                        mandatory
                        density="comfortable"
                        color="primary"
                        variant="outlined"
                        divided
                        class="w-100"
                    >
                        <v-btn value="vn-to-jp" class="flex-grow-1">Tiếng Việt → Tiếng Nhật</v-btn>
                        <v-btn value="jp-to-vn" class="flex-grow-1">Tiếng Nhật → Tiếng Việt</v-btn>
                    </v-btn-toggle>
                    </div>

                    <v-textarea
                    v-model="translationText"
                    :label="translationDirection === 'vn-to-jp' ? 'Nhập văn bản tiếng Việt' : 'Nhập văn bản tiếng Nhật'"
                    placeholder="Nhập văn bản cần dịch..."
                    variant="filled"
                    rows="4"
                    class="translation-input pa-3"
                    hide-details
                    ></v-textarea>

                    <div class="translation-actions pa-3 d-flex justify-end">
                    <v-btn color="primary" @click="goToTranslationPage" :disabled="!translationText.trim()">
                        Dịch & Xem chi tiết
                    </v-btn>
                    </div>
                </div>
                </v-card-text>
             </v-card>
          </div>

          <!-- Recommended Lessons -->
          <h2 class="section-heading text-h5 font-weight-bold mb-4 mt-6">Bài học Cho Bạn</h2>
          <v-row>
            <v-col v-for="lesson in suggestedLessons.slice(0, 2)" :key="lesson.id" cols="12" sm="6">
              <v-card class="lesson-card fill-height d-flex flex-column">
                <v-card-title class="lesson-title text-subtitle-1 font-weight-medium">{{ lesson.title }}</v-card-title>
                <v-card-subtitle>
                  <v-chip size="small" :color="getLessonChipColor(lesson.type)" class="me-2">{{ getLessonTypeText(lesson.type) }}</v-chip>
                  <v-chip size="small" class="me-2">{{ lesson.level }}</v-chip>
                  <span>{{ lesson.estimatedMinutes }} phút</span>
                </v-card-subtitle>
                <v-card-text class="flex-grow-1">
                  {{ lesson.description }}
                </v-card-text>
                <v-card-actions>
                  <v-btn :to="getLessonLink(lesson)" color="primary" variant="tonal">Bắt đầu học</v-btn>
                </v-card-actions>
              </v-card>
            </v-col>
          </v-row>
           <div class="text-center mt-4" v-if="suggestedLessons.length > 2">
                <v-btn color="secondary" :to="{ name: 'vocabularyLearning' }">Xem tất cả bài học</v-btn>
            </div>
        </v-col>

        <!-- Right Column: Supplementary content -->
        <v-col cols="12" md="4">
          <!-- Recent Vocabulary Section -->
          <h3 class="section-heading text-h6 font-weight-medium mb-3">Từ vựng Mới</h3>
          <v-card variant="outlined" class="recent-vocab-card mb-6">
            <v-list class="recent-vocab-list py-0">
              <v-list-item
                v-for="(vocab) in recentVocabulary.slice(0,5)" :key="vocab.id"
                :to="{ name: 'vocabulary', query: { keyword: (vocab.kanji || vocab.hiragana) } }"
                class="vocab-item"
              >
                <template v-slot:prepend>
                    <v-icon color="primary" class="me-3">mdi-label-outline</v-icon>
                </template>
                <div class="w-100">
                    <div class="d-flex align-center justify-space-between">
                        <span class="vocab-text font-weight-medium">{{ vocab.kanji || vocab.hiragana }}</span>
                        <v-chip size="x-small" color="primary" variant="tonal">{{ vocab.level }}</v-chip>
                    </div>
                    <div class="vocab-meaning text-caption">{{ vocab.meaning }}</div>
                </div>
                 <template v-slot:append>
                    <v-icon>mdi-chevron-right</v-icon>
                 </template>
              </v-list-item>
            </v-list>
            <v-card-actions class="justify-center pa-2">
              <v-btn color="primary" variant="text" :to="{ name: 'vocabulary' }">Xem tất cả từ vựng</v-btn>
            </v-card-actions>
          </v-card>

          <!-- Trending Searches Section -->
           <h3 class="section-heading text-h6 font-weight-medium mb-3">Tìm kiếm Phổ biến</h3>
          <v-card class="search-ranking-card">
            <v-list density="compact" class="py-0">
              <v-list-item
                v-for="(term, index) in trendingSearches.slice(0,5)" :key="index"
                :to="{ name: 'vocabulary', query: { keyword: term.keyword } }"
                class="ranking-item"
              >
                <template v-slot:prepend>
                  <div class="ranking-number-new" :class="index < 3 ? 'top-rank' : ''">{{ index + 1 }}</div>
                </template>
                <v-list-item-title class="ranking-item-text">{{ term.keyword }}</v-list-item-title>
                 <template v-slot:append>
                    <v-icon size="small">mdi-magnify</v-icon>
                 </template>
              </v-list-item>
            </v-list>
             <v-card-actions class="justify-center pa-2">
              <v-btn size="small" variant="text" color="primary" :to="{ name: 'vocabulary' }">Xem thêm xu hướng</v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-facing-decorator'
import { useAuthStore } from '@/stores'

interface Lesson {
  id: string
  title: string
  type: string // 'vocabulary', 'conversation', 'grammar'
  level: string
  estimatedMinutes: number
  description: string
}

interface VocabularyItem {
  id: string
  kanji: string | null
  hiragana: string
  meaning: string
  level: string
}

interface TrendingSearch {
  keyword: string
  count: number
}

@Component({
  name: 'HomeView',
})
export default class HomeView extends Vue {
  searchQuery = ''
  translationDirection = 'vn-to-jp'
  translationText = ''

  private authStore = useAuthStore()

  get isLoggedIn(): boolean {
    // Ensure this matches how your authStore exposes login state
    return !!this.authStore.user // Common pattern: user object exists if logged in
  }

  get username(): string {
    return this.authStore.user?.fullName || 'Người dùng'
  }

  get currentLevel(): string {
    return this.authStore.user?.currentLevel || 'N/A'
  }

  dailyProgress = {
    minutesStudied: 25, // Example data
  }

  studyPlan = {
    dailyGoalMinutes: 60, // Example data
  }

  get dailyGoalProgress(): number {
    if (!this.studyPlan.dailyGoalMinutes || this.studyPlan.dailyGoalMinutes === 0) {
      return 0
    }
    const progress = (this.dailyProgress.minutesStudied / this.studyPlan.dailyGoalMinutes) * 100
    return Math.min(Math.round(progress), 100)
  }

  trendingSearches: TrendingSearch[] = [
    { keyword: 'プログラミング', count: 243 },
    { keyword: 'データベース', count: 198 },
    { keyword: 'アプリ開発', count: 175 },
    { keyword: 'クラウド', count: 156 },
    { keyword: 'セキュリティ', count: 143 },
    { keyword: '人工知能', count: 132 },
    { keyword: 'ネットワーク', count: 121 },
    { keyword: 'バグ修正', count: 112 },
    { keyword: 'サーバー', count: 104 },
    { keyword: 'コーディング', count: 98 }
  ]

  recentVocabulary: VocabularyItem[] = [
    { id: '1', kanji: 'プログラム', hiragana: 'ぷろぐらむ', meaning: 'Program', level: 'N3' },
    { id: '2', kanji: 'データ', hiragana: 'でーた', meaning: 'Data', level: 'N4' },
    { id: '3', kanji: '更新', hiragana: 'こうしん', meaning: 'Update', level: 'N3' },
    { id: '4', kanji: '設定', hiragana: 'せってい', meaning: 'Settings/Configuration', level: 'N4' },
    { id: '5', kanji: 'アップロード', hiragana: 'あっぷろーど', meaning: 'Upload', level: 'N3' }
  ]

  suggestedLessons: Lesson[] = [
    {
      id: '1',
      title: 'Basic IT Greetings',
      type: 'conversation',
      level: 'N5',
      estimatedMinutes: 15,
      description: 'Learn essential Japanese greetings for IT workplace.',
    },
    {
      id: '2',
      title: 'Programming Terms',
      type: 'vocabulary',
      level: 'N4',
      estimatedMinutes: 20,
      description: 'Master basic programming terminology in Japanese.',
    },
    {
      id: '3',
      title: 'IT Workplace Phrases',
      type: 'conversation',
      level: 'N4',
      estimatedMinutes: 25,
      description: 'Common phrases used in Japanese IT companies.',
    },
    {
      id: '4',
      title: 'Web Development Terms',
      type: 'vocabulary',
      level: 'N3',
      estimatedMinutes: 30,
      description: 'Essential vocabulary for web developers.',
    }
  ]

  getLessonLink(lesson: Lesson): any {
    // Hàm này tạo đường dẫn phù hợp dựa vào loại bài học
    switch (lesson.type) {
      case 'conversation':
        // Đường dẫn đến trang thực hành hội thoại cụ thể
        return { name: 'conversationPractice', params: { id: lesson.id } };
      case 'vocabulary':
        // Đường dẫn đến trang học từ vựng chung
        return { name: 'vocabularyLearning' };
      case 'grammar':
        // Có thể thêm routing cho bài học ngữ pháp nếu cần
        return { name: 'vocabularyLearning' };
      default:
        // Fallback đến trang danh mục từ vựng
        return { name: 'vocabularyLearning' };
    }
  }

  searchVocabulary() {
    if (this.searchQuery.trim()) {
      this.$router.push({
        name: 'vocabulary',
        query: { keyword: this.searchQuery }
      });
    }
  }

  goToTranslationPage() {
    if (!this.translationText.trim()) return;
    this.$router.push({
      name: 'translations',
      query: {
        text: this.translationText,
        dir: this.translationDirection
      }
    });
  }

  scrollToTranslation() {
    const translationEl = this.$refs.translationSection as HTMLElement;
    if (translationEl) {
      translationEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  getLessonChipColor(type: string): string {
    switch (type) {
      case 'vocabulary': return 'info';
      case 'conversation': return 'success';
      case 'grammar': return 'warning';
      default: return 'grey';
    }
  }

  getLessonTypeText(type: string): string {
    switch (type) {
      case 'vocabulary': return 'Từ vựng';
      case 'conversation': return 'Hội thoại';
      case 'grammar': return 'Ngữ pháp';
      default: return 'Bài học';
    }
  }
}
</script>

<style lang="scss" scoped>
// General Page Styles
.home-page {
  // background-color: #f9f9f9; // A very light grey for the overall page if desired
}

.section-heading {
  color: #2c3e50; // Darker, more professional text color
}

.fill-height {
  height: 100%;
}

// Hero Section
.hero-section {
  background: linear-gradient(135deg, #3f51b5 0%, #2196f3 100%); // Indigo to Blue gradient
  color: white;
  padding-top: 4rem !important; // Vuetify py-16 might be too much with navbar
  padding-bottom: 4rem !important;

  .hero-title {
    font-size: 2.8rem !important; // Adjusted for responsiveness
    font-weight: 700 !important;
    line-height: 1.2;
    letter-spacing: -0.5px;
  }
  .hero-subtitle {
    font-size: 1.2rem !important;
    opacity: 0.9;
    max-width: 700px;
    margin-left: auto;
    margin-right: auto;
  }
  .v-btn {
    padding: 0.75rem 1.8rem !important;
    font-size: 0.95rem !important;
    font-weight: 500;
    text-transform: none; // Keep button text normal case
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }
}

// User Dashboard Summary
.user-dashboard-summary {
  background-color: #ffffff;
  border-bottom: 1px solid #e0e0e0; // Subtle separator
  .v-card {
    box-shadow: 0 3px 10px rgba(0,0,0,0.07);
    border-radius: 12px;
    border: none; // Remove default card border if any
  }
  .v-progress-linear {
    strong {
      color: white;
      font-size: 0.75rem;
    }
  }
  .v-card-title {
    font-weight: 600;
    color: #333;
  }
}

// Features Highlight Section
.features-highlight {
   background-color: #f8f9fa; // Lighter than default grey
   .feature-item-card {
     border-radius: 12px;
     transition: transform 0.3s ease, box-shadow 0.3s ease;
     border: 1px solid #e0e0e0;
     background-color: #fff;
     &:hover {
       transform: translateY(-6px);
       box-shadow: 0 6px 18px rgba(0,0,0,0.1);
     }
     .v-icon {
       transition: transform 0.3s ease;
     }
     &:hover .v-icon {
       transform: scale(1.1);
     }
     h3 {
       color: #3f51b5; // Theme color for feature titles
       font-weight: 600;
     }
     p {
       font-size: 0.9rem;
       color: #555;
     }
     .v-btn {
       text-transform: none;
       font-weight: 500;
       border-radius: 6px;
     }
   }
}

// Main Content Area
.main-content {
  .search-card-integrated {
    display: flex;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #ccc; // Softer border for solo field
    box-shadow: 0 2px 5px rgba(0,0,0,0.05);
    .search-input { // class on v-text-field
      // Overriding solo style for seamless integration
      ::v-deep .v-input__control .v-field {
        background-color: #fff !important;
        box-shadow: none !important;
      }
    }
    .search-btn-integrated { // class on v-btn
      border-radius: 0 8px 8px 0;
      height: auto;
      align-self: stretch;
      box-shadow: none;
      border-left: 1px solid #ccc;
    }
  }

  .translation-card {
    border-radius: 12px;
    box-shadow: 0 3px 10px rgba(0,0,0,0.07);
    border: 1px solid #e0e0e0;
     .translation-title {
        font-weight: 600;
        background-color: #f8f9fa;
        border-bottom: 1px solid #e0e0e0;
        padding: 12px 16px;
        font-size: 1.05rem; // Slightly smaller
    }
    .v-btn-toggle .v-btn { // Ensure buttons in toggle take full width if needed
        // flex-grow: 1; // This is already on the button in template
    }
    .v-textarea ::v-deep .v-field__field {
        font-size: 0.95rem;
    }
  }

  .lesson-card {
    border-radius: 12px;
    transition: transform 0.2s, box-shadow 0.2s;
    box-shadow: 0 3px 10px rgba(0,0,0,0.07);
    border: 1px solid #e0e0e0;
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
    }
    .lesson-title {
      color: #333;
      line-height: 1.3;
    }
    .v-card-subtitle {
      padding-top: 0.5rem;
    }
    .v-card-actions .v-btn {
      text-transform: none;
      font-weight: 500;
    }
  }

  .recent-vocab-card, .search-ranking-card {
    border-radius: 12px;
    box-shadow: 0 3px 10px rgba(0,0,0,0.07);
    border: 1px solid #e0e0e0;
    background-color: #fff;
    .v-list-item {
      border-bottom: 1px solid #f0f0f0;
      padding: 10px 12px;
      transition: background-color 0.2s;
      &:last-child {
        border-bottom: none;
      }
      &:hover {
        background-color: #f5f5f5;
      }
    }
    .v-card-actions .v-btn {
        text-transform: none;
        font-size: 0.9rem;
    }
  }

  .vocab-text {
    font-family: 'Noto Sans JP', sans-serif;
    font-size: 1rem; // Slightly reduced
    color: #2c3e50;
  }
  .vocab-meaning {
    color: #555;
    font-size: 0.8rem;
  }

  .ranking-number-new {
    min-width: 22px;
    height: 22px;
    border-radius: 4px;
    background-color: #e9ecef;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
    font-weight: 600;
    color: #495057;
    margin-right: 10px;
    &.top-rank {
      background-color: #3f51b5;
      color: white;
    }
  }
  .ranking-item-text {
    font-size: 0.9rem;
    color: #333;
  }
}

// Responsive adjustments
@media (max-width: 960px) { // md breakpoint
  .hero-section {
    padding-top: 3rem !important;
    padding-bottom: 3rem !important;
    .hero-title {
      font-size: 2.2rem !important;
    }
    .hero-subtitle {
      font-size: 1.1rem !important;
    }
  }
  .features-highlight .feature-item-card {
    margin-bottom: 1.5rem; // Add space between cards on smaller screens when they stack
    &:last-child {
        margin-bottom: 0;
    }
  }
}

@media (max-width: 600px) { // sm breakpoint
  .hero-section {
    .hero-title {
      font-size: 1.8rem !important;
    }
    .hero-subtitle {
      font-size: 1rem !important;
    }
    .v-btn {
      padding: 0.6rem 1.2rem !important;
      font-size: 0.9rem !important;
      margin-top: 0.5rem;
    }
    .v-btn + .v-btn { // Add margin between buttons if they stack
        margin-left: 0 !important;
    }
  }
  .user-dashboard-summary .v-card-title,
  .main-content .section-heading.text-h5 {
    font-size: 1.25rem !important; // Adjust heading sizes for smaller screens
  }
  .main-content .section-heading.text-h6 {
    font-size: 1.1rem !important;
  }
}
</style>
