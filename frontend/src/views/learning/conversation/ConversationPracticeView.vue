<template>
  <div class="conversation-practice-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <v-progress-circular
        indeterminate
        color="primary"
        size="64"
      ></v-progress-circular>
      <span class="mt-4 text-body-1">Đang tải dữ liệu hội thoại...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <v-alert type="error" class="mb-4">{{ error }}</v-alert>
      <v-btn @click="navigateBack" color="primary" variant="outlined">
        <v-icon start>mdi-arrow-left</v-icon>
        Quay lại danh sách hội thoại
      </v-btn>
    </div>

    <!-- Main Content -->
    <template v-else-if="conversation">
      <!-- Conversation Header -->
      <div class="conversation-header mb-1">
        <div class="d-flex align-center">
          <v-btn
            icon
            @click="navigateBack"
            class="mr-1"
            size="small"
            color="secondary"
            variant="text"
          >
            <v-icon>mdi-arrow-left</v-icon>
          </v-btn>
          <span class="text-subtitle-1 font-weight-medium"
            >Luyện hội thoại</span
          >
        </div>

        <v-card class="" variant="flat">
          <div class="d-flex align-center px-2 py-1 conversation-card-header">
            <div class="conversation-info d-flex align-center">
              <h2 class="text-body-1 font-weight-bold mb-0 mr-2">
                {{ conversation.title }}
              </h2>
              <v-chip
                :color="getJlptColor(conversation.jlptLevel)"
                size="x-small"
                density="compact"
              >
                {{ conversation.jlptLevel }}
              </v-chip>
              <div
                class="text-caption text-medium-emphasis ml-3 d-none d-sm-block"
              >
                {{ conversation.description }}
              </div>
            </div>
            <v-spacer></v-spacer>

            <!-- Furigana Toggle Button -->
            <v-switch
              v-model="showFurigana"
              :label="$vuetify.display.mdAndUp ? 'Furigana' : undefined"
              color="primary"
              hide-details
              density="compact"
              class="mini-switch mr-2"
              inset
            ></v-switch>
          </div>
        </v-card>
      </div>

      <!-- Conversation Card -->
      <v-card class="main-conversation-card mb-6" variant="elevated">
        <v-divider></v-divider>

        <!-- Conversation Practice Section -->
        <v-card-text
          class="py-4"
          style="position: relative; overflow: visible; min-height: 60vh"
        >
          <div class="d-flex mb-4">
            <v-chip color="info" label>
              <v-icon start>mdi-robot</v-icon>
              Nihongo IT
            </v-chip>
          </div>

          <!-- Chat Messages Container -->
          <div class="chat-container">
            <template v-for="(line, i) in conversation.dialogue" :key="i">
              <div
                v-if="
                  visibleLineIndices.includes(i) ||
                  dimmedLineIndices.includes(i)
                "
                class="message-row mb-2"
                :class="[
                  line.speaker === 'user' ? 'justify-end' : 'justify-start',
                  dimmedLineIndices.includes(i) ? 'dimmed-line' : '',
                ]"
                :data-index="i"
              >
                <!-- Avatar for Nihongo IT -->
                <div
                  v-if="line.speaker !== 'user'"
                  class="avatar-container mr-2"
                >
                  <v-avatar size="36" color="info">
                    <v-icon color="white">mdi-robot</v-icon>
                  </v-avatar>
                </div>

                <!-- Message Bubble -->
                <div
                  class="message-container"
                  :class="{
                    'user-message': line.speaker === 'user',
                    'bot-message': line.speaker !== 'user',
                    'completed-message':
                      lineCompletionStatus[i] && line.speaker === 'user',
                    'score-excellent':
                      lineCompletionStatus[i] && pronunciationScores[i] >= 90,
                    'score-very-good':
                      lineCompletionStatus[i] &&
                      pronunciationScores[i] >= 80 &&
                      pronunciationScores[i] < 90,
                    'score-good':
                      lineCompletionStatus[i] &&
                      pronunciationScores[i] >= 70 &&
                      pronunciationScores[i] < 80,
                    'score-fair':
                      lineCompletionStatus[i] &&
                      pronunciationScores[i] >= 60 &&
                      pronunciationScores[i] < 70,
                    'score-poor':
                      lineCompletionStatus[i] &&
                      pronunciationScores[i] > 0 &&
                      pronunciationScores[i] < 60,
                  }"
                  :style="{
                    maxWidth: '80%',
                    minWidth: '270px',
                  }"
                >
                  <!-- Message Content -->
                  <div class="message-content pa-2">
                    <div class="d-flex justify-space-between align-center mb-0">
                      <div
                        class="japanese-text text-subtitle-1 font-weight-medium"
                      >
                        <!-- Show typing effect for latest message (both user and native) -->
                        <span
                          v-if="
                            isTyping &&
                            i ===
                              visibleLineIndices[visibleLineIndices.length - 1]
                          "
                        >
                          {{ typedText }}<span class="typing-cursor">|</span>
                        </span>
                        <span
                          v-else-if="
                            line.speaker === 'user' &&
                            lineCompletionStatus[i] &&
                            lineAnalysisResults[i]
                          "
                        >
                          <span
                            v-html="
                              formatJapaneseWithHighlights(
                                line.japanese,
                                lineAnalysisResults[i],
                              )
                            "
                          ></span>
                        </span>
                        <span
                          v-else-if="
                            showFurigana &&
                            line.furiganaTokens &&
                            Array.isArray(line.furiganaTokens) &&
                            line.furiganaTokens.length > 0
                          "
                        >
                          <ruby
                            v-for="(token, tokenIndex) in line.furiganaTokens"
                            :key="`${i}-${tokenIndex}`"
                            class="ruby-text"
                          >
                            {{ token.text }}
                            <rt v-if="token.reading && token.isKanji">{{
                              token.reading
                            }}</rt>
                          </ruby>
                        </span>
                        <span v-else>
                          {{ line.japanese }}
                          <small
                            v-if="showFurigana"
                            class="text-caption text-disabled ml-1"
                          >
                            (no furigana)
                          </small>
                        </span>
                      </div>
                      <!-- Audio Button for all messages -->
                      <v-btn
                        icon
                        density="comfortable"
                        size="x-small"
                        @click="playAudio(line)"
                        color="info"
                        variant="text"
                        title="Nghe phát âm mẫu"
                      >
                        <v-icon size="small">mdi-volume-high</v-icon>
                      </v-btn>
                    </div>
                    <div class="text-body-2 text-medium-emphasis">
                      <!-- Also apply typing effect to the meaning -->
                      <span
                        v-if="
                          isTyping &&
                          i ===
                            visibleLineIndices[visibleLineIndices.length - 1]
                        "
                      >
                        {{
                          isTyping && typedText.length > 0 ? line.meaning : ""
                        }}
                      </span>
                      <span v-else>{{ line.meaning }}</span>
                    </div>

                    <!-- Interim Text Display -->
                    <div
                      v-if="
                        isUserLine(line) &&
                        activeLineIndex === i &&
                        isRecording &&
                        interimText
                      "
                      class="mt-2 text-caption font-italic azure-interim-text"
                    >
                      <span>{{ interimText }}</span>
                    </div>
                  </div>

                  <!-- User Controls section below message -->
                  <div v-if="isUserLine(line)" class="user-controls mt-0">
                    <!-- Row of controls -->
                    <div class="d-flex align-center flex-wrap">
                      <!-- Recording Controls -->
                      <div class="d-flex gap-2">
                        <v-btn
                          size="small"
                          color="error"
                          variant="text"
                          :disabled="isRecording && activeLineIndex !== i"
                          @click="startRecording(i)"
                          :loading="isProcessing && activeLineIndex === i"
                        >
                          <v-icon size="small" class="mr-1"
                            >mdi-microphone</v-icon
                          >
                          <span
                            v-if="!(isRecording && activeLineIndex === i)"
                            class="d-none d-sm-inline"
                            >Ghi âm</span
                          >
                        </v-btn>

                        <v-btn
                          v-if="isRecording && activeLineIndex === i"
                          size="small"
                          color="primary"
                          variant="text"
                          @click="stopRecording"
                        >
                          <v-icon size="small" class="mr-1">mdi-stop</v-icon>
                          <span class="d-none d-sm-inline">Dừng</span>
                        </v-btn>

                        <v-btn
                          v-if="
                            recordedAudioUrls[i] &&
                            !(isRecording && activeLineIndex === i)
                          "
                          size="small"
                          color="secondary"
                          variant="text"
                          :disabled="isRecording"
                          @click="playRecordedAudio(i)"
                        >
                          <v-icon size="small" class="mr-1">mdi-play</v-icon>
                          <span class="d-none d-sm-inline">Nghe lại</span>
                        </v-btn>
                      </div>

                      <v-spacer></v-spacer>

                      <!-- Score display inline with controls -->
                      <div
                        v-if="pronunciationScores[i] > 0"
                        class="d-flex align-center"
                      >
                        <v-tooltip location="top">
                          <template v-slot:activator="{ props }">
                            <v-chip
                              v-bind="props"
                              :color="getScoreColor(pronunciationScores[i])"
                              size="small"
                              class="mr-2"
                            >
                              {{ pronunciationScores[i] }}
                            </v-chip>
                          </template>
                          {{ getDetailedFeedback(i) }}
                        </v-tooltip>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Avatar for User -->
                <div
                  v-if="line.speaker === 'user'"
                  class="avatar-container ml-2"
                >
                  <v-avatar size="36" color="primary">
                    <v-img
                      v-if="userProfilePicture"
                      :src="userProfilePicture"
                      alt="Ảnh đại diện của bạn"
                    ></v-img>
                    <span v-else class="text-subtitle-2 text-white">{{
                      avatarInitials
                    }}</span>
                  </v-avatar>
                </div>
              </div>
            </template>
          </div>
        </v-card-text>
      </v-card>

      <!-- Feedback Summary Section (replaces dialog) -->
      <v-expand-transition>
        <v-card
          v-if="isConversationCompleted && feedbackSummary"
          class="feedback-summary-card mb-6"
          variant="elevated"
        >
          <v-card-title
            class="text-h5 d-flex align-center bg-primary text-white"
          >
            <v-icon color="white" class="mr-2">mdi-chart-box</v-icon>
            Tổng kết luyện tập
          </v-card-title>
          <v-card-text>
            <div class="feedback-summary-container">
              <!-- Summary Stats -->
              <div
                class="d-flex justify-space-between align-center mb-4 stats-container"
              >
                <div class="stat-item">
                  <div class="text-subtitle-1">Số câu hội thoại</div>
                  <div class="text-h5 text-primary">
                    {{ feedbackSummary.attempts || 0 }}
                  </div>
                </div>
                <div class="stat-item">
                  <div class="text-subtitle-1">Điểm trung bình</div>
                  <div
                    class="text-h5"
                    :class="getScoreColorClass(feedbackSummary.avg_score || 0)"
                  >
                    {{ Math.round(feedbackSummary.avg_score || 0) }}
                  </div>
                </div>
                <div class="stat-item">
                  <div class="text-subtitle-1">Điểm cao nhất</div>
                  <div
                    class="text-h5"
                    :class="getScoreColorClass(feedbackSummary.max_score || 0)"
                  >
                    {{ Math.round(feedbackSummary.max_score || 0) }}
                  </div>
                </div>
              </div>

              <!-- Summary Text -->
              <div class="summary-text mb-4">
                <v-card flat color="grey-lighten-4" class="pa-3">
                  <p class="text-body-1">{{ feedbackSummary.summary }}</p>
                </v-card>
              </div>

              <div class="d-flex flex-wrap gap-4">
                <!-- Common Errors -->
                <div
                  class="common-errors flex-grow-1"
                  v-if="
                    feedbackSummary.common_errors &&
                    feedbackSummary.common_errors.length > 0
                  "
                >
                  <h3 class="text-subtitle-1 font-weight-bold mb-2">
                    <v-icon color="error" size="small" class="mr-1"
                      >mdi-alert-circle</v-icon
                    >
                    Lỗi thường gặp
                  </h3>
                  <v-list density="compact" class="error-list">
                    <v-list-item
                      v-for="(error, index) in feedbackSummary.common_errors"
                      :key="index"
                      class="error-item"
                    >
                      <template v-slot:prepend>
                        <v-icon size="small" color="error"
                          >mdi-close-circle</v-icon
                        >
                      </template>
                      <v-list-item-title>{{ error }}</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </div>

                <!-- Improvement Tips -->
                <div
                  class="improvement-tips flex-grow-1"
                  v-if="
                    feedbackSummary.improvement_tips &&
                    feedbackSummary.improvement_tips.length > 0
                  "
                >
                  <h3 class="text-subtitle-1 font-weight-bold mb-2">
                    <v-icon color="success" size="small" class="mr-1"
                      >mdi-lightbulb</v-icon
                    >
                    Lời khuyên cải thiện
                  </h3>
                  <v-list density="compact" class="tip-list">
                    <v-list-item
                      v-for="(tip, index) in feedbackSummary.improvement_tips"
                      :key="index"
                      class="tip-item"
                    >
                      <template v-slot:prepend>
                        <v-icon size="small" color="success"
                          >mdi-check-circle</v-icon
                        >
                      </template>
                      <v-list-item-title>{{ tip }}</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </div>
              </div>
            </div>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="primary"
              variant="elevated"
              @click="restartConversation"
            >
              <v-icon start>mdi-refresh</v-icon>
              Luyện tập lại
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-expand-transition>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppToast } from "@/composables/useAppToast";
import { useAuthStore } from "@/stores";
import { AxiosError } from "axios";
import api from "@/utils/api";
import authService from "@/services/auth.service";
import conversationService from "@/services/conversation.service";
import aiService from "@/services/ai.service";
import feedbackService from "@/services/feedback.service";
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import * as speechsdk from "microsoft-cognitiveservices-speech-sdk";
import speechRecognitionService from "@/services/SpeechRecognitionService";
import type { FeedbackSummary } from "@/services/ai.service";

// Define types
interface FuriganaToken {
  text: string;
  reading?: string;
  isKanji: boolean;
}

interface ConversationLine {
  speaker: "user" | "bot";
  japanese: string;
  meaning: string;
  audioUrl?: string;
  furiganaTokens?: FuriganaToken[];
}

interface Conversation {
  id: string;
  title: string;
  description: string;
  jlptLevel?: string;
  isSaved?: boolean;
  dialogue: ConversationLine[];
}

interface WordAnalysis {
  text: string;
  isCorrect: boolean;
  suggestion?: string;
}

interface SpeechAnalysisResult {
  score: number;
  feedback?: string;
  intonation?: string;
  clarity?: string;
  rhythm?: string;
  transcription?: string;
  words?: WordAnalysis[];
  personalizedFeedback?: string;
  textAnalysis?: {
    originalStructure?: {
      tokens?: {
        original: string;
        isCorrect: boolean;
        position: {
          start: number;
          end: number;
        };
      }[];
    };
  };
}

// Router
const route = useRoute();
const router = useRouter();
const toast = useAppToast();
const authStore = useAuthStore();

// State
const loading = ref(false);
const error = ref("");
const conversation = ref<Conversation | null>(null);
const isRecording = ref(false);
const isProcessing = ref(false);
const activeLineIndex = ref(-1);
const recordedAudioUrls = ref<(string | null)[]>([]);
const recordedAudioBlobs = ref<(Blob | null)[]>([]);
const pronunciationScores = ref<number[]>([]);
const lineCompletionStatus = ref<boolean[]>([]);
const lineAnalysisResults = ref<(SpeechAnalysisResult | null)[]>([]);
const mediaRecorder = ref<MediaRecorder | null>(null);
const audioChunks = ref<Blob[]>([]);
const isSilent = ref(false);
const hasSpoken = ref(false); // Biến để theo dõi xem người dùng đã nói gì chưa

// Thêm trạng thái cho Furigana
const showFurigana = ref(true); // Mặc định hiển thị furigana

// Audio stream
let audioStream: MediaStream | null = null;

// Thêm refs và computed mới để quản lý hiệu ứng typing và hiển thị từng dòng
const visibleLineIndices = ref<number[]>([0]); // Ban đầu chỉ hiển thị dòng đầu tiên
const dimmedLineIndices = ref<number[]>([]); // Danh sách các dòng hiển thị mờ
const isTyping = ref(true); // Bắt đầu với hiệu ứng typing
const typedText = ref("");
const currentTypeIndex = ref(0);
const typeSpeed = ref(60); // ms per character - tốc độ typing có thể điều chỉnh
const typingVariation = ref(15); // Thêm biến đổi ngẫu nhiên cho tốc độ typing để tự nhiên hơn

// Biến để lưu interim text
const interimText = ref("");

// Azure Speech Configuration - chỉ để nhận dạng và hiển thị text
const azureSpeechKey = ref(import.meta.env.VITE_AZURE_SPEECH_KEY || "");
const azureSpeechRegion = ref(import.meta.env.VITE_AZURE_SPEECH_REGION || "");

// Cập nhật biến để theo dõi trạng thái đang phát âm
const isPlayingAudio = ref(false);

// Update the state for feedback summary - remove showFeedbackSummary since we're not using a dialog
const feedbackSummary = ref<FeedbackSummary | null>(null);
const feedbackHistory = ref<SpeechAnalysisResult[]>([]);

// Computed
const isConversationCompleted = computed(() => {
  const userLines =
    conversation.value?.dialogue.filter((line) => line.speaker === "user") ||
    [];
  return (
    userLines.length > 0 &&
    userLines.every(
      (_, index) => lineCompletionStatus.value[getUserLineIndex(index)],
    )
  );
});

// Computed cho thông tin user
const userProfilePicture = computed(
  () => authStore.user?.profilePicture || null,
);
const avatarInitials = computed(() => {
  if (!authStore.user?.fullName) return "U";
  return authStore.user.fullName
    .split(" ")
    .map((name) => name.charAt(0))
    .join("")
    .toUpperCase();
});

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

const getScoreColor = (score: number): string => {
  if (score >= 90) return "success";
  if (score >= 80) return "light-green";
  if (score >= 70) return "lime";
  if (score >= 60) return "warning";
  return "error";
};

const getVietnameseFeedback = (score: number): string => {
  if (score >= 90) {
    return "Phát âm xuất sắc!";
  } else if (score >= 80) {
    return "Phát âm rất tốt";
  } else if (score >= 70) {
    return "Phát âm tốt";
  } else if (score >= 60) {
    return "Phát âm khá";
  } else {
    return "Cần cải thiện";
  }
};

// Hàm mới để lấy feedback chi tiết từ analysis results
const getDetailedFeedback = (lineIndex: number): string => {
  const analysisResult = lineAnalysisResults.value[lineIndex];
  if (analysisResult && analysisResult.feedback) {
    return analysisResult.feedback;
  }
  // Fallback to simple feedback if no detailed feedback available
  return getVietnameseFeedback(pronunciationScores.value[lineIndex] || 0);
};

const playAudio = async (line: ConversationLine) => {
  if (!line.japanese || isPlayingAudio.value) return;

  try {
    isPlayingAudio.value = true;

    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      toast.error("Vui lòng đăng nhập để sử dụng tính năng đọc văn bản");
      // Redirect to login page after a short delay
      setTimeout(() => {
        router.push({
          name: "login",
          query: { redirect: router.currentRoute.value.fullPath },
        });
      }, 1500);
      isPlayingAudio.value = false;
      return;
    }

    // Text to speak
    const textToSpeak = line.japanese;

    // Show loading indicator
    toast.info("Đang phát âm thanh...");

    // Generate and play audio using AI service
    const audioBlob = await aiService.generateTTS(
      textToSpeak,
      "conversation",
      1.0,
      true,
    );

    // Custom playing to ensure we set isPlayingAudio.value = false when done
    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);

    audio.onended = () => {
      URL.revokeObjectURL(audioUrl);
      isPlayingAudio.value = false;
    };

    await audio.play();
  } catch (error) {
    isPlayingAudio.value = false;

    // Special handling for 401 errors
    if (error instanceof AxiosError && error.response?.status === 401) {
      toast.error(
        "Dịch vụ TTS yêu cầu xác thực. Vui lòng đăng nhập lại khi có thể.",
      );
    } else {
      toast.error(
        error instanceof Error ? error.message : "Không thể tạo giọng nói",
      );
    }
  }
};

const isUserLine = (line: ConversationLine): boolean => {
  return line.speaker === "user";
};

const getUserLineIndex = (userIndex: number): number => {
  // Find the index in the dialogue array for the nth user line
  if (!conversation.value) return -1;

  let count = 0;
  for (let i = 0; i < conversation.value.dialogue.length; i++) {
    if (conversation.value.dialogue[i].speaker === "user") {
      if (count === userIndex) return i;
      count++;
    }
  }
  return -1;
};

// Khởi tạo Azure Speech trước thay vì mỗi lần ghi âm
onMounted(() => {
  loading.value = true;
  fetchConversationData();

  // Thêm watcher để tự động cuộn xuống khi có dòng mới
  watch(
    visibleLineIndices,
    () => {
      scrollToLatestMessage();
    },
    { deep: true },
  );

  // Khởi tạo Speech Recognition Service nếu có cấu hình
  if (azureSpeechKey.value && azureSpeechRegion.value) {
    speechRecognitionService.initialize(
      azureSpeechKey.value,
      azureSpeechRegion.value,
    );
  }
});

// Hàm lấy dữ liệu hội thoại từ API
const fetchConversationData = async () => {
  try {
    const conversationId = route.params.id;
    if (!conversationId) {
      error.value = "Không tìm thấy ID hội thoại trong URL";
      loading.value = false;
      return;
    }

    // Gọi API để lấy dữ liệu hội thoại
    const response = await conversationService.getConversationById(
      conversationId as string,
    );

    // Kiểm tra dữ liệu hợp lệ
    if (!response || !response.data) {
      error.value = "Không thể tải dữ liệu hội thoại";
      loading.value = false;
      return;
    }

    const conversationData = response.data;

    // Chuyển đổi dữ liệu từ API sang định dạng sử dụng trong component
    conversation.value = {
      id: conversationData.conversationId || "",
      title: conversationData.title || "",
      description: conversationData.description || "",
      jlptLevel: conversationData.jlptLevel || "",
      isSaved: false, // Mặc định là chưa lưu
      dialogue: [],
    };

    // Kiểm tra trạng thái lưu của hội thoại
    if (conversationData.conversationId) {
      try {
        const savedResponse = await conversationService.checkSavedConversation(
          conversationData.conversationId,
        );
        if (savedResponse && savedResponse.data) {
          conversation.value.isSaved = savedResponse.data.saved;
        }
      } catch {}
    }

    // Chuyển đổi lines từ API sang dialogue
    if (conversationData.lines && Array.isArray(conversationData.lines)) {
      // Sắp xếp các dòng theo orderIndex
      const sortedLines = [...conversationData.lines].sort(
        (a, b) => a.orderIndex - b.orderIndex,
      );

      conversation.value.dialogue = sortedLines.map((line) => ({
        speaker: line.speaker === "user" ? "user" : ("bot" as "user" | "bot"),
        japanese: line.japaneseText || "",
        meaning: line.vietnameseTranslation || "",
      }));
    }

    // Initialize arrays based on dialogue length
    const dialogueLength = conversation.value.dialogue.length;
    recordedAudioUrls.value = new Array(dialogueLength).fill(null);
    recordedAudioBlobs.value = new Array(dialogueLength).fill(null);
    pronunciationScores.value = new Array(dialogueLength).fill(0);
    lineCompletionStatus.value = new Array(dialogueLength).fill(false);
    lineAnalysisResults.value = new Array(dialogueLength).fill(null);

    // Đặt lại visibleLineIndices
    visibleLineIndices.value = [0];
    dimmedLineIndices.value = [];
    isTyping.value = true;
    typedText.value = "";
    currentTypeIndex.value = 0;

    // Kiểm tra xem dòng thứ 2 có phải của Nihongo IT không để hiển thị mờ
    if (
      dialogueLength > 1 &&
      conversation.value.dialogue[1].speaker !== "user"
    ) {
      dimmedLineIndices.value.push(1);
    }

    // Bắt đầu hiệu ứng typing cho tin nhắn đầu tiên
    if (dialogueLength > 0) {
      typeTextEffect(conversation.value.dialogue[0].japanese);
    }

    // Lấy dữ liệu furigana cho các dòng hội thoại
    await initializeFurigana();
  } catch {
    error.value =
      "Đã xảy ra lỗi khi tải dữ liệu hội thoại. Vui lòng thử lại sau.";
  } finally {
    loading.value = false;
  }
};

// Cập nhật hàm processRecording để kiểm tra lỗi nhận dạng giọng nói
const processRecording = async (index: number) => {
  if (activeLineIndex.value !== index) return;

  isProcessing.value = true;

  try {
    // Xóa interim text khi không dùng Azure
    if (!speechRecognitionService.isServiceInitialized()) {
      interimText.value = "";
    }

    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      toast.error("Vui lòng đăng nhập để sử dụng tính năng phân tích phát âm");
      isProcessing.value = false;
      return;
    }

    if (!recordedAudioBlobs.value[index]) {
      throw new Error("Không tìm thấy bản ghi âm");
    }

    if (!conversation.value?.dialogue[index]?.japanese) {
      throw new Error("Không tìm thấy nội dung tham chiếu");
    }

    // Get reference text
    const referenceText = conversation.value.dialogue[index].japanese;
    const type = "conversation";

    // Use AI service to analyze speech
    const analysis = await aiService.analyzeSpeech(
      recordedAudioBlobs.value[index] as Blob,
      referenceText,
      type,
    );

    // Process response
    pronunciationScores.value[index] = Math.round(analysis.score);
    lineAnalysisResults.value[index] = analysis;

    // Chỉ đánh dấu hoàn thành nếu là dòng cuối cùng đang hiển thị
    const lastVisibleIndex =
      visibleLineIndices.value[visibleLineIndices.value.length - 1];

    if (index === lastVisibleIndex && pronunciationScores.value[index] > 50) {
      markAsComplete(index);
    } else {
      // Nếu không phải dòng cuối cùng, chỉ cập nhật trạng thái hoàn thành
      lineCompletionStatus.value[index] = true;
    }
  } catch {
    // Xóa interim text nếu xử lý lỗi và không dùng Azure
    if (!speechRecognitionService.isServiceInitialized()) {
      interimText.value = "";
    }

    // Sử dụng điểm ngẫu nhiên nếu API không hoạt động
    const randomScore = Math.floor(Math.random() * 51) + 50;
    pronunciationScores.value[index] = randomScore;

    toast.warning("Đang sử dụng điểm mẫu do lỗi phân tích phát âm");
  } finally {
    isProcessing.value = false;
  }
};

// Hàm định dạng văn bản tiếng Nhật với các từ được tô màu
const formatJapaneseWithHighlights = (
  text: string,
  analysis: SpeechAnalysisResult,
): string => {
  if (
    !analysis ||
    !analysis.textAnalysis ||
    !analysis.textAnalysis.originalStructure
  ) {
    return text;
  }

  try {
    const tokens = analysis.textAnalysis.originalStructure.tokens;
    if (!tokens || !Array.isArray(tokens)) {
      return text;
    }

    // Tạo bản sao của văn bản gốc để xử lý
    let formattedText = text;

    // Sắp xếp tokens theo vị trí từ cuối về đầu để tránh việc thay đổi vị trí khi replace
    const sortedTokens = [...tokens].sort(
      (a, b) => b.position.start - a.position.start,
    );

    // Duyệt qua từng token và highlight dựa trên isCorrect
    sortedTokens.forEach((token) => {
      const { original, isCorrect, position } = token;

      // Kiểm tra xem token có trong văn bản không
      if (position && position.start >= 0 && position.end <= text.length) {
        const tokenText = text.substring(position.start, position.end);

        // Chỉ highlight nếu token text khớp với original
        if (tokenText === original) {
          let replacement;

          if (isCorrect) {
            // Tô màu xanh cho từ đúng
            replacement = `<span style="color: #4CAF50; font-weight: bold; background-color: rgba(76, 175, 80, 0.1); padding: 1px 2px; border-radius: 3px;">${original}</span>`;
          } else {
            // Tô màu đỏ cho từ sai
            replacement = `<span style="color: #F44336; font-weight: bold; background-color: rgba(244, 67, 54, 0.1); padding: 1px 2px; border-radius: 3px; text-decoration: underline wavy #F44336;">${original}</span>`;
          }

          // Thay thế token trong văn bản
          formattedText =
            formattedText.substring(0, position.start) +
            replacement +
            formattedText.substring(position.end);
        }
      }
    });

    return formattedText;
  } catch (error) {
    console.error("Error in formatJapaneseWithHighlights:", error);
    return text;
  }
};

// Navigation function
const navigateBack = () => {
  router.push({
    name: "conversationLearning",
  });
};

// Cập nhật hàm startRecording
const startRecording = async (index: number) => {
  try {
    // Reset recording state
    audioChunks.value = [];
    activeLineIndex.value = index;
    isSilent.value = false;
    interimText.value = "";
    hasSpoken.value = false; // Reset biến theo dõi trạng thái nói

    // Bắt đầu nhận dạng giọng nói nếu service đã được khởi tạo
    if (speechRecognitionService.isServiceInitialized()) {
      speechRecognitionService.startRecognition(
        // Xử lý văn bản đang nhận dạng (interim)
        (text) => {
          interimText.value = text;
          // Nếu có text, đánh dấu là đã phát hiện lời nói
          if (text && text.trim().length > 0) {
            hasSpoken.value = true;
          }
        },
        // Xử lý văn bản đã nhận dạng xong (final)
        (text) => {
          if (text.trim()) {
            // Đánh dấu là đã nói
            hasSpoken.value = true;

            // Lưu kết quả nhận dạng để sử dụng sau khi kết thúc ghi âm
            if (lineAnalysisResults.value[index]) {
              lineAnalysisResults.value[index]!.transcription = text;
            } else {
              lineAnalysisResults.value[index] = {
                score: 0,
                transcription: text,
              };
            }
          }
        },
        // Xử lý lỗi - không tự động dừng khi có lỗi nữa
        (error) => {
          console.error("Speech recognition error:", error);
        },
      );
    }

    // Vẫn sử dụng MediaRecorder để ghi âm như trước
    audioStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    });

    if (audioStream) {
      mediaRecorder.value = new MediaRecorder(audioStream);

      mediaRecorder.value.ondataavailable = (event: BlobEvent) => {
        if (event.data.size > 0) {
          audioChunks.value.push(event.data);
        }
      };

      mediaRecorder.value.onstop = () => {
        // Nếu im lặng thì không xử lý kết quả
        if (isSilent.value && !hasSpoken.value) {
          toast.warning("Không phát hiện giọng nói, vui lòng thử lại");
          return;
        }

        const audioBlob = new Blob(audioChunks.value, { type: "audio/mp3" });

        // Kiểm tra kích thước blob để xác định có âm thanh không
        if (audioBlob.size < 1000) {
          toast.warning("Không phát hiện giọng nói, vui lòng thử lại");
          return;
        }

        recordedAudioBlobs.value[index] = audioBlob;
        recordedAudioUrls.value[index] = URL.createObjectURL(audioBlob);

        processRecording(index);
      };

      mediaRecorder.value.start();
      isRecording.value = true;
      toast.info(
        "Bắt đầu ghi âm - Hãy phát âm câu hội thoại và nhấn nút Dừng khi hoàn thành",
      );

      // Đặt thời gian tối đa cho phiên ghi âm là 7 giây, chỉ dừng nếu chưa nói gì
      setTimeout(() => {
        if (
          isRecording.value &&
          mediaRecorder.value &&
          mediaRecorder.value.state === "recording"
        ) {
          if (!hasSpoken.value) {
            isSilent.value = true;
            toast.warning("Không phát hiện giọng nói sau 7 giây, dừng ghi âm");
            stopRecording();
          }
          // Không còn dòng stopRecording() ở đây để chỉ dừng khi chưa nói
        }
      }, 15000);
    }
  } catch {
    toast.error("Không thể truy cập microphone");
  }
};

const playRecordedAudio = (index: number) => {
  if (!recordedAudioUrls.value[index]) return;

  try {
    const audio = new Audio(recordedAudioUrls.value[index]);
    audio.onended = () => {};
    audio.onerror = () => {
      toast.error("Không thể phát bản ghi âm");
    };

    audio.play().catch(() => {
      toast.error("Không thể phát bản ghi âm");
    });
  } catch {
    toast.error("Không thể phát bản ghi âm", {
      position: "top",
      duration: 3000,
    });
  }
};

// Hàm markAsComplete cải tiến để tự động phát âm thanh của Nihongo IT khi hiển thị dòng tiếp theo
const markAsComplete = (index: number) => {
  lineCompletionStatus.value[index] = true;

  // Chỉ hiển thị dòng tiếp theo nếu đây là dòng cuối cùng đang hiển thị
  const lastVisibleIndex =
    visibleLineIndices.value[visibleLineIndices.value.length - 1];
  if (index === lastVisibleIndex) {
    // Nếu dòng tiếp theo đã hiển thị mờ, cần bỏ khỏi danh sách dimmedLineIndices và thêm vào visible
    const nextIndex = index + 1;

    // Sử dụng requestAnimationFrame để tránh blocking main thread
    requestAnimationFrame(() => {
      if (dimmedLineIndices.value.includes(nextIndex)) {
        // Xóa khỏi danh sách mờ và thêm vào danh sách hiển thị đầy đủ
        dimmedLineIndices.value = dimmedLineIndices.value.filter(
          (i) => i !== nextIndex,
        );
        visibleLineIndices.value.push(nextIndex);

        // Kiểm tra dòng tiếp theo sau dòng của Nihongo IT
        if (
          conversation.value &&
          nextIndex + 1 < conversation.value.dialogue.length
        ) {
          const lineAfterNext = conversation.value.dialogue[nextIndex + 1];
          // Nếu dòng sau đó là của Nihongo IT, thêm vào danh sách hiển thị mờ
          if (lineAfterNext.speaker !== "user") {
            dimmedLineIndices.value.push(nextIndex + 1);
          }
        }

        // Cuộn xuống dòng mới nhất - không cần đợi animation kết thúc
        scrollToLatestMessage();

        // Tự động phát âm thanh của Nihongo IT sau khi hiển thị
        const nextLine = conversation.value?.dialogue[nextIndex];
        if (nextLine && nextLine.speaker !== "user") {
          // Đợi một chút để hiệu ứng hiển thị hoàn tất
          setTimeout(() => {
            playAudio(nextLine);
          }, 500);
        }

        // Sau khi hiển thị dòng Nihongo IT, tiếp tục hiển thị dòng người dùng kế tiếp (nếu có)
        const nextUserIndex = nextIndex + 1;
        if (
          conversation.value &&
          nextUserIndex < conversation.value.dialogue.length &&
          conversation.value.dialogue[nextUserIndex].speaker === "user"
        ) {
          // Giảm thời gian chờ để cải thiện trải nghiệm
          setTimeout(() => {
            requestAnimationFrame(() => {
              // Bỏ dòng người dùng khỏi danh sách dimmed (nếu có) và thêm vào visible
              if (dimmedLineIndices.value.includes(nextUserIndex)) {
                dimmedLineIndices.value = dimmedLineIndices.value.filter(
                  (i) => i !== nextUserIndex,
                );
              }
              visibleLineIndices.value.push(nextUserIndex);

              // Áp dụng hiệu ứng typing cho dòng người dùng
              isTyping.value = true;
              typedText.value = "";
              currentTypeIndex.value = 0;

              // Kiểm tra lại conversation.value trước khi sử dụng
              if (conversation.value) {
                typeTextEffect(
                  conversation.value.dialogue[nextUserIndex].japanese,
                );
              }

              // Cuộn xuống dòng mới nhất sau một khoảng thời gian nhỏ
              setTimeout(() => scrollToLatestMessage(), 50);
            });
          }, 700); // Giảm từ 1000ms xuống 700ms
        }
      } else {
        // Nếu không có dòng mờ kế tiếp, tiếp tục hiển thị dòng tiếp theo như thông thường
        setTimeout(() => {
          startTypingNextLine();
          // Đảm bảo cuộn xuống sau khi hiển thị dòng mới
          scrollToLatestMessage();
        }, 700); // Giảm từ 1000ms xuống 700ms
      }
    });
  }

  // Check if the conversation is now completed
  if (isConversationCompleted.value) {
    toast.success("Chúc mừng! Bạn đã hoàn thành hội thoại");

    // Add a small delay before showing the feedback summary
    setTimeout(() => {
      fetchFeedbackSummary();
      // Scroll to the feedback summary after it's loaded
      setTimeout(() => {
        scrollToFeedbackSummary();
      }, 500);
    }, 1000);
  }
};

// Add function to scroll to feedback summary
const scrollToFeedbackSummary = () => {
  const feedbackElement = document.querySelector(".feedback-summary-card");
  if (feedbackElement) {
    feedbackElement.scrollIntoView({ behavior: "smooth", block: "start" });
  }
};

// Scroll tới tin nhắn mới nhất với hiệu ứng mượt hơn
const scrollToLatestMessage = () => {
  // Đợi DOM cập nhật
  setTimeout(() => {
    // Lấy dòng hội thoại mới nhất
    const lastVisibleIndex =
      visibleLineIndices.value[visibleLineIndices.value.length - 1];
    if (lastVisibleIndex === undefined) return;

    const lastMessageElement = document.querySelector(
      `.message-row[data-index="${lastVisibleIndex}"]`,
    );
    if (lastMessageElement) {
      // Lấy vị trí của phần tử so với cửa sổ
      const rect = lastMessageElement.getBoundingClientRect();
      const targetPosition = window.scrollY + rect.top - 150;

      // Giảm thiểu giật lag bằng cách sử dụng biến delta nhỏ cho mỗi frame
      const startPosition = window.scrollY;
      const distance = targetPosition - startPosition;

      // Chỉ cuộn khi khoảng cách đủ lớn để tránh cuộn không cần thiết
      if (Math.abs(distance) < 20) return;

      const duration = Math.min(Math.abs(distance), 600); // Thời gian tối đa là 600ms, ngắn hơn cho khoảng cách nhỏ
      let startTime: number | null = null;

      // Hàm animation cuộn với easing function mượt mà hơn
      function animateScroll(timestamp: number) {
        if (!startTime) startTime = timestamp;
        const elapsed = timestamp - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Easing function cải tiến (easeOutQuint)
        const easedProgress = 1 - Math.pow(1 - progress, 3);

        window.scrollTo({
          top: startPosition + distance * easedProgress,
          behavior: "auto", // Sử dụng 'auto' vì chúng ta đang tự xử lý animation
        });

        if (elapsed < duration) {
          requestAnimationFrame(animateScroll);
        }
      }

      // Bắt đầu animation với requestAnimationFrame để đồng bộ với refresh rate màn hình
      requestAnimationFrame(animateScroll);
    }
  }, 50); // Giảm thời gian chờ từ 100ms xuống 50ms
};

// Hiệu ứng typing cải tiến cho văn bản
const typeTextEffect = (text: string) => {
  // Sử dụng requestAnimationFrame để đồng bộ với refresh rate màn hình
  if (!text || text.length === 0) {
    isTyping.value = false;

    // Kiểm tra dòng hiện tại - nếu là của Nihongo IT, phát âm thanh
    const lastVisibleIndex =
      visibleLineIndices.value[visibleLineIndices.value.length - 1];
    if (
      conversation.value &&
      lastVisibleIndex < conversation.value.dialogue.length
    ) {
      const currentLine = conversation.value.dialogue[lastVisibleIndex];
      if (currentLine.speaker !== "user") {
        // Đợi một chút sau khi typing kết thúc
        setTimeout(() => {
          if (!isPlayingAudio.value) {
            playAudio(currentLine);
          }
        }, 200);
      }
    }

    return;
  }

  if (currentTypeIndex.value < text.length) {
    // Sử dụng RAF để đồng bộ với refresh rate của màn hình
    requestAnimationFrame(() => {
      typedText.value = text.substring(0, currentTypeIndex.value + 1);
      currentTypeIndex.value++;

      // Xác định thời gian chờ cho ký tự tiếp theo
      const currentChar = text[currentTypeIndex.value - 1];
      const pauseChars = ["。", "、", "!", "?", "！", "？", "…", ".", ","];

      // Tăng độ biến thiên cho tốc độ typing để tự nhiên hơn
      const randomVariation =
        Math.random() * typingVariation.value * 2 - typingVariation.value;
      let delay = Math.max(20, typeSpeed.value + randomVariation);

      // Thêm thời gian dừng cho dấu câu hoặc khoảng trắng
      if (pauseChars.includes(currentChar)) {
        delay += 250; // Dừng lâu hơn sau dấu câu
      } else if (currentChar === " ") {
        delay += 40; // Dừng nhẹ cho khoảng trắng
      }

      // Giảm thời gian trễ nếu là ký tự cuối để tránh chờ quá lâu
      if (currentTypeIndex.value === text.length - 1) {
        delay = Math.min(delay, 60);
      }

      setTimeout(() => typeTextEffect(text), delay);
    });
  } else {
    isTyping.value = false;

    // Lấy dòng hiện tại đang typing
    const lastVisibleIndex =
      visibleLineIndices.value[visibleLineIndices.value.length - 1];
    if (
      !conversation.value ||
      lastVisibleIndex >= conversation.value.dialogue.length
    )
      return;

    const currentLine = conversation.value.dialogue[lastVisibleIndex];

    // Phát âm thanh ngay sau khi typing xong nếu là câu của Nihongo IT
    if (currentLine.speaker !== "user") {
      setTimeout(() => {
        if (!isPlayingAudio.value) {
          playAudio(currentLine);
        }
      }, 200);
    }

    // Chỉ hiển thị mờ dòng tiếp theo nếu là dòng của Nihongo IT
    const nextIndex = lastVisibleIndex + 1;
    if (conversation.value && nextIndex < conversation.value.dialogue.length) {
      const nextLine = conversation.value.dialogue[nextIndex];

      // Hiển thị mờ dòng tiếp theo nếu là của Nihongo IT
      if (
        nextLine.speaker !== "user" &&
        !dimmedLineIndices.value.includes(nextIndex) &&
        !visibleLineIndices.value.includes(nextIndex)
      ) {
        requestAnimationFrame(() => {
          dimmedLineIndices.value.push(nextIndex);
        });
      }
    }

    // Chỉ tự động hiển thị dòng tiếp theo nếu là dòng đầu tiên của Nihongo IT
    if (currentLine.speaker !== "user" && lastVisibleIndex === 0) {
      setTimeout(() => {
        startTypingNextLine();
      }, 800); // Giảm xuống 800ms thay vì 1000ms
    }
  }
};

// Bắt đầu hiệu ứng typing cho dòng tiếp theo
const startTypingNextLine = () => {
  if (!conversation.value) return;

  const lastVisibleIndex =
    visibleLineIndices.value[visibleLineIndices.value.length - 1];
  const nextIndex = lastVisibleIndex + 1;

  // Kiểm tra xem có dòng tiếp theo không
  if (nextIndex < conversation.value.dialogue.length) {
    const nextLine = conversation.value.dialogue[nextIndex];

    // Nếu dòng tiếp theo đang hiển thị mờ, xóa khỏi danh sách hiển thị mờ
    requestAnimationFrame(() => {
      if (dimmedLineIndices.value.includes(nextIndex)) {
        dimmedLineIndices.value = dimmedLineIndices.value.filter(
          (i) => i !== nextIndex,
        );
      }

      // Thêm dòng vào danh sách hiển thị
      visibleLineIndices.value.push(nextIndex);

      // Áp dụng hiệu ứng typing
      isTyping.value = true;
      typedText.value = "";
      currentTypeIndex.value = 0;

      // Đảm bảo tất cả thay đổi DOM đã được áp dụng trước khi bắt đầu typing
      setTimeout(() => {
        typeTextEffect(nextLine.japanese);
        // Cuộn sau khi bắt đầu hiệu ứng typing nhưng không chờ hoàn thành
        scrollToLatestMessage();

        // Nếu là câu của Nihongo IT, tự động phát âm thanh sau khi hiệu ứng typing hoàn tất
        if (nextLine.speaker !== "user") {
          const typingDuration =
            nextLine.japanese.length * typeSpeed.value + 200;
          setTimeout(() => {
            playAudio(nextLine);
          }, typingDuration);
        }
      }, 50);
    });
  }
};

// Cập nhật hàm stopRecording để xử lý silenceTimeout nếu cần
const stopRecording = async () => {
  // Đảm bảo dừng nhận dạng trước khi dừng ghi âm
  if (speechRecognitionService.isServiceInitialized()) {
    try {
      await speechRecognitionService.stopRecognition();
    } catch (error) {
      console.error("Error stopping speech recognition:", error);
    }
  }

  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop();
    isRecording.value = false;

    // Stop all audio tracks
    if (audioStream) {
      audioStream
        .getTracks()
        .forEach((track: MediaStreamTrack) => track.stop());
    }
  }
};

// Fetch furigana for Japanese text
const fetchFurigana = async (text: string): Promise<FuriganaToken[]> => {
  try {
    // Gọi API với thêm header xác thực
    const response = await api.get("/api/v1/learning/furigana", {
      params: { text },
    });

    // Chuyển đổi dữ liệu API nếu cần
    if (Array.isArray(response.data)) {
      return response.data as FuriganaToken[];
    } else if (response.data.tokens) {
      return response.data.tokens as FuriganaToken[];
    } else {
      // Fallback nếu API trả về định dạng khác
      return generateFallbackFurigana(text);
    }
  } catch {
    // Return fallback formatting if API fails
    return generateFallbackFurigana(text);
  }
};

// Khởi tạo furigana cho tất cả các dòng hội thoại
const initializeFurigana = async () => {
  if (!conversation.value || !conversation.value.dialogue) return;

  for (let i = 0; i < conversation.value.dialogue.length; i++) {
    const line = conversation.value.dialogue[i];
    if (line.japanese) {
      try {
        const tokens = await fetchFurigana(line.japanese);
        // Cập nhật dòng hội thoại với dữ liệu furigana
        // Sử dụng phương thức splice để thay đổi mảng gốc
        const updatedLine = { ...line, furiganaTokens: tokens };
        conversation.value.dialogue.splice(i, 1, updatedLine);
      } catch {}
    }
  }
};

// Tạo dữ liệu furigana giả lập cho trường hợp API không hoạt động
const generateFallbackFurigana = (text: string): FuriganaToken[] => {
  // Danh sách các ký tự kanji phổ biến và cách đọc
  const kanjiMap: Record<string, string> = {
    日: "に",
    本: "ほん",
    語: "ご",
    勉: "べん",
    強: "きょう",
    私: "わたし",
    僕: "ぼく",
    食: "た",
    飲: "の",
    行: "い",
    来: "く",
    帰: "かえ",
    見: "み",
    聞: "き",
    読: "よ",
    書: "か",
    話: "はな",
    会: "あ",
    分: "わ",
    好: "す",
    大: "だい",
    小: "しょう",
    新: "しん",
    古: "ふる",
    高: "たか",
    安: "やす",
    長: "なが",
    短: "みじか",
    多: "おお",
    少: "すく",
    友: "とも",
    達: "たち",
    人: "ひと",
    男: "おとこ",
    女: "おんな",
    子: "こ",
    水: "みず",
    火: "ひ",
    風: "かぜ",
    雨: "あめ",
  };

  // Tách mỗi ký tự trong text
  const result: FuriganaToken[] = [];

  for (let i = 0; i < text.length; i++) {
    const char = text[i];

    // Kiểm tra xem có phải là kanji không dựa vào unicode range
    const isKanji = /[\u4e00-\u9faf\u3400-\u4dbf]/.test(char);

    if (isKanji) {
      result.push({
        text: char,
        reading: kanjiMap[char] || "", // Trả về reading nếu có trong map
        isKanji: true,
      });
    } else {
      result.push({
        text: char,
        isKanji: false,
      });
    }
  }

  return result;
};

// Add new function to fetch feedback summary
const fetchFeedbackSummary = async () => {
  if (!conversation.value) return;

  try {
    // Collect all non-null analysis results
    const analysisResults = lineAnalysisResults.value.filter(
      (result) => result !== null,
    ) as SpeechAnalysisResult[];

    // Store in history for future reference
    feedbackHistory.value = [...analysisResults];

    // Only proceed if we have results to analyze
    if (analysisResults.length === 0) {
      toast.warning("Không có đủ dữ liệu để tạo tổng kết");
      return;
    }

    // Get all user lines as a single string for context
    const userLines = conversation.value.dialogue
      .filter((line) => line.speaker === "user")
      .map((line) => line.japanese)
      .join(" ");

    // Fetch the summary
    feedbackSummary.value = null; // Reset while loading

    const summary = await aiService.getFeedbackSummary(
      analysisResults,
      userLines,
    );
    feedbackSummary.value = summary;

    // Automatically save feedback to the server
    if (conversation.value.id && authStore.user && authStore.user.userId) {
      try {
        const saved = await feedbackService.saveFeedback(
          conversation.value.id,
          summary,
          authStore.user.userId,
        );
        if (saved) {
          console.log("Feedback saved successfully");
        }
      } catch (error) {
        console.error("Error saving feedback:", error);
      }
    }
  } catch (error) {
    console.error("Error fetching feedback summary:", error);
    toast.error("Không thể tạo tổng kết phản hồi");

    // Provide fallback data
    feedbackSummary.value = {
      summary: "Không thể tạo tổng kết phản hồi do lỗi kết nối.",
      common_errors: [],
      improvement_tips: ["Tiếp tục luyện tập để cải thiện phát âm."],
    };
  }
};

// Update the restartConversation function - remove dialog references
const restartConversation = () => {
  // Reset all progress
  recordedAudioUrls.value = new Array(
    conversation.value?.dialogue.length || 0,
  ).fill(null);
  recordedAudioBlobs.value = new Array(
    conversation.value?.dialogue.length || 0,
  ).fill(null);
  pronunciationScores.value = new Array(
    conversation.value?.dialogue.length || 0,
  ).fill(0);
  lineCompletionStatus.value = new Array(
    conversation.value?.dialogue.length || 0,
  ).fill(false);
  lineAnalysisResults.value = new Array(
    conversation.value?.dialogue.length || 0,
  ).fill(null);

  // Reset visible lines
  visibleLineIndices.value = [0];
  dimmedLineIndices.value = [];

  // Reset feedback summary
  feedbackSummary.value = null;

  // Check if second line is from Nihongo IT to show it dimmed
  if (
    conversation.value &&
    conversation.value.dialogue.length > 1 &&
    conversation.value.dialogue[1].speaker !== "user"
  ) {
    dimmedLineIndices.value.push(1);
  }

  // Start typing effect for first message
  isTyping.value = true;
  typedText.value = "";
  currentTypeIndex.value = 0;

  if (conversation.value && conversation.value.dialogue.length > 0) {
    typeTextEffect(conversation.value.dialogue[0].japanese);
  }

  // Scroll to top
  window.scrollTo({ top: 0, behavior: "smooth" });

  toast.info("Bắt đầu luyện tập lại");
};

// Add helper function for score color class
const getScoreColorClass = (score: number): string => {
  if (score >= 90) return "text-success";
  if (score >= 80) return "text-light-green";
  if (score >= 70) return "text-lime";
  if (score >= 60) return "text-warning";
  return "text-error";
};

onUnmounted(() => {
  // Clean up resources when component is destroyed
  if (isRecording.value) {
    stopRecording();
  }

  recordedAudioUrls.value.forEach((url) => {
    if (url) URL.revokeObjectURL(url);
  });

  // Cleanup Azure Speech Recognizer
  if (speechRecognitionService.isServiceInitialized()) {
    speechRecognitionService.stopRecognition().catch(() => {});
  }
});
</script>

<style scoped lang="scss">
.conversation-practice-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px;

  .loading-container,
  .error-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
  }

  .conversation-header {
    margin-bottom: 12px;

    .d-flex {
      // Luôn giữ nút quay lại và tiêu đề trên cùng một hàng
      align-items: center !important;
    }
  }

  .conversation-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    row-gap: 4px;
  }

  .japanese-text {
    font-family: "Noto Sans JP", sans-serif;
    will-change: contents;
    transform: translateZ(0);

    // Style for the highlighted text (can be overridden by inline styles)
    :deep(span) {
      &[style*="color: #4CAF50"] {
        font-weight: 600;
        text-shadow: 0 0 1px rgba(76, 175, 80, 0.3);
      }
    }

    .ruby-text {
      margin: 0 1px;
      text-align: center;
      position: relative;
    }

    ruby {
      position: relative;
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      line-height: 1.8;
    }

    rt {
      position: absolute;
      top: -0.8em;
      font-size: 0.55rem;
      color: #2196f3;
      font-weight: 400;
      text-align: center;
      line-height: 1;
      white-space: nowrap;
      letter-spacing: 0.05em;
      opacity: 0.9;
    }
  }

  .typing-cursor {
    display: inline-block;
    vertical-align: middle;
    transform-origin: bottom;
    will-change: opacity, transform;
    animation:
      cursorBlink 0.7s step-end infinite,
      cursorBounce 3s ease-in-out infinite;
  }

  @keyframes cursorBlink {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0;
    }
  }

  @keyframes cursorBounce {
    0%,
    20%,
    40%,
    60%,
    80%,
    100% {
      transform: translateY(0) translateZ(0);
    }
    50% {
      transform: translateY(-2px) translateZ(0);
    }
  }

  .chat-container {
    display: flex;
    flex-direction: column;
    padding: 4px 0;
    min-height: 50vh;
    position: relative;
    overflow: visible;
    margin-bottom: 16px;
    contain: layout style;
  }

  .message-row {
    display: flex;
    align-items: flex-start;
    width: 100%;
    margin-bottom: 8px;
    animation: fadeInUp 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    opacity: 0;
    will-change: transform, opacity;
    transform: translateZ(0);

    &.dimmed-line {
      opacity: 0.6;
      filter: blur(0.5px);
      pointer-events: none;
      animation: fadeInDimmed 0.5s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;

      .message-container {
        opacity: 0.6;
        border-left: 3px dashed rgba(0, 0, 0, 0.1);
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
        transform: scale(0.98);
      }
    }
  }

  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translate3d(0, 20px, 0);
    }
    to {
      opacity: 1;
      transform: translate3d(0, 0, 0);
    }
  }

  @keyframes fadeInDimmed {
    from {
      opacity: 0;
      transform: translate3d(0, 15px, 0);
    }
    to {
      opacity: 0.6;
      transform: translate3d(0, 0, 0);
    }
  }

  .message-container {
    position: relative;
    border-radius: 18px;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    min-width: 270px;
    transform-origin: bottom center;
    animation: messageScale 0.25s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    will-change: transform, opacity, box-shadow;
    backface-visibility: hidden;

    &:hover {
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
      transform: translateY(-2px) translateZ(0);
    }

    &.user-message {
      background-color: #e3f2fd; // Màu xanh nhạt cho tin nhắn người dùng
      border-top-right-radius: 4px;
      align-self: flex-end;
    }

    &.bot-message {
      background-color: #e8f5e9; // Màu xanh lá nhạt cho tin nhắn của Nihongo IT
      border-top-left-radius: 4px;
      align-self: flex-start;
    }

    &.completed-message {
      border-left-width: 3px;
      border-left-style: solid;
      animation: pulse 0.8s cubic-bezier(0.25, 0.1, 0.25, 1);
    }

    &.score-excellent {
      border-left-color: #4caf50; // success
    }

    &.score-very-good {
      border-left-color: #8bc34a; // light-green
    }

    &.score-good {
      border-left-color: #cddc39; // lime
    }

    &.score-fair {
      border-left-color: #ffc107; // warning
    }

    &.score-poor {
      border-left-color: #f44336; // error
    }

    @keyframes messageScale {
      from {
        transform: scale3d(0.98, 0.98, 1);
        opacity: 0.8;
      }
      to {
        transform: scale3d(1, 1, 1);
        opacity: 1;
      }
    }

    @keyframes pulse {
      0% {
        box-shadow: 0 0 0 0 rgba(76, 175, 80, 0.4);
      }
      70% {
        box-shadow: 0 0 0 6px rgba(76, 175, 80, 0);
      }
      100% {
        box-shadow: 0 0 0 0 rgba(76, 175, 80, 0);
      }
    }
  }

  .message-content {
    position: relative;
    z-index: 0;
    will-change: contents;
    contain: content;
    padding-top: 12px !important;
  }

  .user-controls {
    background-color: transparent;
    padding: 2px 12px;
    margin: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
  }

  .avatar-container {
    margin-top: 0;
  }

  .azure-speech-toggle {
    margin-right: 8px;
  }

  .azure-interim-text {
    color: #666;
    font-style: italic;
    background-color: rgba(0, 0, 0, 0.03);
    padding: 4px 8px;
    border-radius: 4px;
    margin-top: 4px;
  }

  .mini-switch {
    margin-top: 0;
    margin-bottom: 0;

    :deep(.v-switch__track) {
      opacity: 0.5;
      transform: scale(0.75);
    }

    :deep(.v-switch__thumb) {
      transform: scale(0.75);
    }
  }

  // Responsive styles for mobile
  @media (max-width: 960px) {
    padding: 12px;

    .conversation-header {
      .text-subtitle-1 {
        font-size: 0.9rem !important;
      }
    }

    .message-container {
      min-width: 220px;
    }
  }

  @media (max-width: 768px) {
    .mini-switch {
      transform: scale(0.85);
      margin-right: 0;

      :deep(.v-switch__track) {
        opacity: 0.7;
        transform: scale(0.7);
      }

      :deep(.v-switch__thumb) {
        transform: scale(0.7);
      }
    }

    .conversation-info {
      h2 {
        font-size: 0.9rem !important;
        margin-right: 4px !important;
      }

      .v-chip {
        font-size: 0.7rem;
      }
    }

    .conversation-header {
      .text-subtitle-1 {
        font-size: 0.85rem !important;
      }
    }

    .message-container {
      max-width: 85% !important;
      min-width: 180px;

      .japanese-text {
        font-size: 0.95rem !important;
      }

      .text-body-2 {
        font-size: 0.85rem !important;
      }
    }

    .chat-container {
      padding: 2px 0;
    }

    .user-controls {
      padding: 2px 8px;

      .v-btn {
        padding: 0 6px;
        min-width: auto;

        .v-icon {
          font-size: 1rem;
        }
      }
    }

    .avatar-container {
      .v-avatar {
        width: 32px;
        height: 32px;

        .v-icon {
          font-size: 1rem;
        }
      }
    }
  }

  @media (max-width: 480px) {
    padding: 8px;

    .conversation-header {
      .text-subtitle-1 {
        font-size: 0.8rem !important;
      }
    }

    .mini-switch {
      transform: scale(0.75);

      :deep(.v-switch__track) {
        opacity: 0.7;
        transform: scale(0.65);
      }

      :deep(.v-switch__thumb) {
        transform: scale(0.65);
      }
    }

    .message-container {
      min-width: 150px;
      max-width: 90% !important;

      .japanese-text {
        font-size: 0.9rem !important;
        line-height: 1.5;
      }

      .text-body-2 {
        font-size: 0.8rem !important;
        line-height: 1.4;
      }

      .v-btn {
        min-width: 28px;
        width: 28px;
        height: 28px;

        .v-icon {
          font-size: 0.9rem;
        }
      }
    }

    .user-controls {
      .d-flex {
        flex-wrap: wrap;
        justify-content: space-between;
        gap: 4px;
      }

      .v-btn {
        margin: 0;
        padding: 0 4px;
        height: 28px;

        .v-icon {
          margin-right: 0 !important;
        }
      }
    }

    .avatar-container {
      .v-avatar {
        width: 28px;
        height: 28px;
      }
    }

    .typing-cursor {
      height: 14px;
    }

    .azure-interim-text {
      font-size: 0.8rem;
      padding: 3px 6px;
    }
  }

  .feedback-summary-container {
    .stats-container {
      background-color: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 16px;
    }

    .stat-item {
      text-align: center;
      padding: 0 8px;
    }

    .error-list,
    .tip-list {
      background-color: #f5f5f5;
      border-radius: 8px;
      padding: 4px 0;
    }

    .error-item {
      border-left: 2px solid #f44336;
      margin-bottom: 4px;
      background-color: rgba(244, 67, 54, 0.05);
    }

    .tip-item {
      border-left: 2px solid #4caf50;
      margin-bottom: 4px;
      background-color: rgba(76, 175, 80, 0.05);
    }

    .text-success {
      color: #4caf50 !important;
    }

    .text-light-green {
      color: #8bc34a !important;
    }

    .text-lime {
      color: #cddc39 !important;
    }

    .text-warning {
      color: #ffc107 !important;
    }

    .text-error {
      color: #f44336 !important;
    }
  }

  .feedback-summary-card {
    animation: slideUp 0.5s ease-out forwards;
    transform-origin: bottom;
    overflow: hidden;

    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  }

  .gap-4 {
    gap: 1rem;
  }
}
</style>
