import { createRouter, createWebHistory } from "vue-router";
import { requireAuth, requireAdmin, redirectIfAuthenticated } from "./guards";

// Định nghĩa các path prefix để nhất quán và dễ bảo trì
const PATH_PREFIX = {
  LEARNING: "/learning",
  ACCOUNT: "/account",
  ADMIN: "/admin",
  TOOLS: "/tools",
};

// Nhóm các route theo chức năng
// Các routes công khai hoặc trang chủ
const publicRoutes = [
  {
    path: "/",
    name: "home",
    component: () => import("@/views/HomeView.vue"),
    meta: {
      public: true,
      title: "Trang chủ",
    },
  },
  // 404 catch-all route - Phải được đặt ở CUỐI mảng routes chính, nhưng đưa vào đây để tổ chức code
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: () => import("@/views/errors/NotFoundView.vue"),
    meta: {
      public: true,
      title: "404 - Không tìm thấy trang",
    },
  },
];

// Routes xác thực người dùng
const authRoutes = [
  {
    path: `${PATH_PREFIX.ACCOUNT}/login`,
    name: "login",
    component: () => import("@/views/auth/LoginView.vue"),
    beforeEnter: redirectIfAuthenticated,
    meta: {
      public: true,
      title: "Đăng nhập",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/register`,
    name: "register",
    component: () => import("@/views/auth/RegisterView.vue"),
    beforeEnter: redirectIfAuthenticated,
    meta: {
      public: true,
      title: "Đăng ký",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/profile`,
    name: "profile",
    component: () => import("@/views/auth/ProfileView.vue"),
    meta: {
      requiresAuth: true,
      title: "Hồ sơ cá nhân",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/settings`,
    name: "accountSettings",
    component: () => import("@/views/auth/AccountSettingsView.vue"),
    meta: {
      requiresAuth: true,
      title: "Cài đặt tài khoản",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/notifications`,
    name: "notificationSettings",
    component: () => import("@/views/auth/NotificationSettingsView.vue"),
    meta: {
      requiresAuth: true,
      title: "Cài đặt thông báo",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/forgot-password`,
    name: "forgotPassword",
    component: () => import("@/views/auth/ForgotPasswordView.vue"),
    meta: {
      public: true,
      title: "Quên mật khẩu",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/reset-password`,
    name: "resetPassword",
    component: () => import("@/views/auth/ResetPasswordView.vue"),
    meta: {
      public: true,
      title: "Đặt lại mật khẩu",
    },
  },
  {
    path: `${PATH_PREFIX.ACCOUNT}/change-password`,
    name: "changePassword",
    component: () => import("@/views/auth/ChangePasswordView.vue"),
    meta: {
      requiresAuth: true,
      title: "Thay đổi mật khẩu",
    },
  },
];

// Routes học từ vựng
const vocabularyRoutes = [
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary/all`,
    name: "vocabulary",
    component: () => import("@/views/learning/vocabulary/VocabularyView.vue"),
    meta: {
      requiresAuth: true,
      title: "Từ vựng IT",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary/categories`,
    name: "vocabularyLearning",
    component: () => import("@/views/learning/VocabularyLearningView.vue"),
    meta: {
      requiresAuth: true,
      title: "Học từ vựng theo danh mục",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary/categories/:slug`,
    name: "categoryDetail",
    component: () => import("@/views/learning/CategoryDetailView.vue"),
    meta: {
      requiresAuth: true,
      title: "Chi tiết danh mục từ vựng",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary/topics/:name`,
    name: "topicDetail",
    component: () => import("@/views/learning/VocabularyTopicView.vue"),
    meta: {
      requiresAuth: true,
      title: "Từ vựng theo chủ đề",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary/details/:term`,
    name: "vocabularyDetail",
    component: () =>
      import("@/views/learning/vocabulary/VocabularyDetailView.vue"),
    meta: {
      requiresAuth: true,
      title: "Chi tiết từ vựng",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/vocabulary-storage`,
    name: "vocabularyStorage",
    component: () => import("@/views/learning/VocabularyStorageView.vue"),
    meta: {
      requiresAuth: true,
      title: "Từ vựng đã lưu",
    },
  },
];

// Routes học hội thoại
const conversationRoutes = [
  {
    path: `${PATH_PREFIX.LEARNING}/conversation`,
    name: "conversationLearning",
    component: () =>
      import("@/views/learning/conversation/ConversationLearningView.vue"),
    meta: {
      requiresAuth: true,
      title: "Học hội thoại",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/conversation/:id`,
    name: "conversationPractice",
    component: () =>
      import("@/views/learning/conversation/ConversationPracticeView.vue"),
    meta: {
      requiresAuth: true,
      title: "Thực hành hội thoại",
    },
  },
];

// Routes học flashcards và theo dõi tiến độ
const studyRoutes = [
  {
    path: `${PATH_PREFIX.LEARNING}/statistics`,
    name: "statistics",
    component: () => import("@/views/learning/StatisticsView.vue"),
    meta: {
      requiresAuth: true,
      title: "Thống kê học tập",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/flashcards/study`,
    name: "flashcardStudy",
    component: () => import("@/views/learning/FlashcardStudyView.vue"),
    meta: {
      requiresAuth: true,
      title: "Học flashcards",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/flashcards/statistics`,
    name: "flashcardStatistics",
    component: () => import("@/views/study/FlashcardStatsView.vue"),
    meta: {
      requiresAuth: true,
      title: "Thống kê flashcards",
    },
  },
  {
    path: `${PATH_PREFIX.LEARNING}/speech-analyzer`,
    name: "speechAnalyzer",
    component: () => import("@/views/learning/SpeechAnalyzer.vue"),
    meta: {
      requiresAuth: true,
      title: "Phân tích phát âm",
    },
  },
];

// Routes công cụ hỗ trợ
const toolRoutes = [
  {
    path: `${PATH_PREFIX.TOOLS}/furigana`,
    name: "furigana",
    component: () => import("@/views/learning/FuriganaView.vue"),
    meta: {
      public: true,
      title: "Tạo Furigana",
    },
  },
  {
    path: `${PATH_PREFIX.TOOLS}/translations`,
    name: "translations",
    component: () => import("@/views/learning/TranslationView.vue"),
    meta: {
      public: true,
      title: "Dịch thuật",
    },
  },
];

// Routes quản trị
const adminRoutes = [
  {
    path: `${PATH_PREFIX.ADMIN}`,
    name: "adminDashboard",
    component: () => import("@/views/admin/DashboardView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Bảng điều khiển quản trị",
    },
  },
  // Quản lý người dùng
  {
    path: `${PATH_PREFIX.ADMIN}/users`,
    name: "adminUsers",
    component: () => import("@/views/admin/UsersView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Quản lý người dùng",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/users/:id`,
    name: "adminUserDetail",
    component: () => import("@/views/admin/UserDetailView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Chi tiết người dùng",
    },
  },
  // Quản lý nội dung
  {
    path: `${PATH_PREFIX.ADMIN}/categories`,
    name: "adminCategories",
    component: () => import("@/views/admin/CategoryManagementView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Quản lý danh mục",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/topics`,
    name: "adminTopics",
    component: () => import("@/views/admin/TopicManagementView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Quản lý chủ đề",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/vocabulary`,
    name: "adminVocabulary",
    component: () => import("@/views/admin/VocabularyManagementView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Quản lý từ vựng",
    },
  },
  // Quản lý hội thoại
  {
    path: `${PATH_PREFIX.ADMIN}/conversations`,
    name: "adminConversations",
    component: () => import("@/views/admin/ConversationManagementView.vue"),
    meta: {
      title: "Quản lý hội thoại",
      requiresAuth: true,
      requiresAdmin: true,
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/conversations/:id`,
    name: "adminConversationDetail",
    component: () => import("@/views/admin/ConversationDetailView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Chi tiết hội thoại",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/conversations/:id/edit`,
    name: "adminConversationEdit",
    component: () => import("@/views/admin/ConversationLinesEditView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Chỉnh sửa hội thoại",
    },
  },
  // Thống kê
  {
    path: `${PATH_PREFIX.ADMIN}/statistics`,
    name: "adminStatistics",
    component: () => import("@/views/admin/StatisticsOverviewView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Tổng quan thống kê",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/statistics/users`,
    name: "adminUserStatistics",
    component: () => import("@/views/admin/UserStatisticsView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Thống kê người dùng",
    },
  },
  {
    path: `${PATH_PREFIX.ADMIN}/statistics/users/:id`,
    name: "adminUserStatisticsDetail",
    component: () => import("@/views/admin/UserStatisticsDetailView.vue"),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      title: "Chi tiết thống kê người dùng",
    },
  },
];

// Tạo mảng routes chính từ các nhóm routes
const routes = [
  ...publicRoutes,
  ...authRoutes,
  ...vocabularyRoutes,
  ...conversationRoutes,
  ...studyRoutes,
  ...toolRoutes,
  ...adminRoutes,
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} | Nihongo IT`;
  }
  if (to.meta.requiresAdmin) return requireAdmin(to, from, next);
  if (to.meta.requiresAuth) return requireAuth(to, from, next);
  return next();
});

export default router;
