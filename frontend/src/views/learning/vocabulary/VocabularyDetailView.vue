<template>
  <div class="pronunciation-view-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      <span class="mt-4 text-body-1">Đang tải dữ liệu từ vựng...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <v-alert type="error" class="mb-4">{{ error }}</v-alert>
      <v-btn @click="navigateBack" color="primary" variant="outlined">
        <v-icon start>mdi-arrow-left</v-icon>
        Quay lại danh sách từ vựng
      </v-btn>
    </div>

    <!-- Main Content -->
    <template v-else-if="vocabulary">
      <div class="d-flex align-center mb-6">
        <v-btn icon @click="navigateBack" class="mr-3" color="secondary" variant="text">
          <v-icon>mdi-arrow-left</v-icon>
        </v-btn>
        <h1 class="text-h5 font-weight-bold">Luyện phát âm</h1>
      </div>

      <!-- Vocabulary Card -->
      <v-card class="main-vocab-card mb-6" variant="elevated">
        <div class="vocab-header d-flex align-center px-6 py-4">
          <div class="word-container">
            <div class="d-flex align-center mb-1">
              <h2 class="text-h4 japanese-text">
                {{ vocabulary.term || '' }}
              </h2>
              <v-chip :color="getJlptColor(vocabulary.jlptLevel)" class="ml-4">
                {{ vocabulary.jlptLevel }}
              </v-chip>
            </div>
            <div v-if="vocabulary.pronunciation" class="reading-container text-h6 text-medium-emphasis">
              {{ vocabulary.pronunciation }}
            </div>
          </div>
          <v-spacer></v-spacer>
          <div class="action-buttons">
            <v-btn
              :icon="vocabulary.isSaved ? 'mdi-bookmark' : 'mdi-bookmark-outline'"
              size="small"
              variant="text"
              :color="vocabulary.isSaved ? 'warning' : undefined"
              @click="toggleSave"
              class="mr-2 action-btn"
              :title="vocabulary.isSaved ? 'Bỏ lưu' : 'Lưu từ vựng'"
            ></v-btn>
            <v-btn icon class="action-btn mr-2" @click="playAudio" color="primary" variant="text" title="Nghe phát âm">
              <v-icon>mdi-volume-high</v-icon>
            </v-btn>
          </div>
        </div>

        <v-divider></v-divider>

        <!-- Pronunciation Practice Section -->
        <v-card-text class="py-5">
          <section class="pronunciation-section mb-6">
            <h3 class="section-title d-flex align-center">
              <v-icon color="primary" class="mr-2">mdi-microphone</v-icon>
              Luyện phát âm
              <v-tooltip location="top">
                <template v-slot:activator="{ props }">
                  <v-icon v-bind="props" class="ml-2" size="small">mdi-information</v-icon>
                </template>
                Nhấn vào nút ghi âm và đọc từ vựng để kiểm tra độ chính xác phát âm của bạn.
              </v-tooltip>
            </h3>

            <v-card class="pronunciation-controls-card pa-4 mb-4" color="background">
              <div class="d-flex flex-column align-center">
                <div class="recording-controls mb-4">
                  <div class="d-flex gap-3">
                    <v-btn
                      color="error"
                      :disabled="isRecording"
                      @click="startRecording"
                      prepend-icon="mdi-microphone"
                      :loading="isProcessing"
                    >
                      <span v-if="!isRecording">Bắt đầu ghi âm</span>
                      <span v-else>Đang ghi âm...</span>
                    </v-btn>

                    <v-btn
                      color="primary"
                      :disabled="!isRecording"
                      @click="stopRecording"
                      prepend-icon="mdi-stop"
                    >
                      Dừng ghi âm
                    </v-btn>

                    <v-btn
                      v-if="recordedAudioBlob"
                      color="secondary"
                      :disabled="isPlayingRecording || isRecording"
                      @click="playRecordedAudio"
                      prepend-icon="mdi-play"
                    >
                      <span v-if="!isPlayingRecording">Nghe bản ghi</span>
                      <span v-else>Đang phát...</span>
                    </v-btn>
                  </div>

                  <div v-if="isRecording" class="mt-2 text-center text-body-2">
                    <v-chip color="error" size="small">Đang ghi âm...</v-chip>
                  </div>
                </div>

                <v-progress-linear
                  v-if="isProcessing"
                  indeterminate
                  color="primary"
                  class="mb-4"
                ></v-progress-linear>
              </div>
            </v-card>

            <!-- Results Section -->
            <div v-if="hasScore" class="results-container">
              <!-- Circular Score Display -->
              <div class="score-circle-container">
                <div class="score-circle-wrapper">
                  <div class="score-circle" :class="scoreColorClass">
                    <svg class="circle-progress" width="150" height="150" viewBox="0 0 150 150">
                      <circle class="circle-bg" cx="75" cy="75" r="60" stroke-width="10" />
                      <circle
                        class="circle-progress-bar"
                        cx="75"
                        cy="75"
                        r="60"
                        stroke-width="10"
                        :stroke-dasharray="circumference"
                        :stroke-dashoffset="dashOffset"
                      />
                    </svg>
                    <div class="score-number">{{ pronounciationScore }}</div>
                  </div>
                </div>
                <div class="text-body-1 text-center mt-2">Điểm tổng</div>
              </div>

              <!-- Recognized Text Section -->
              <div v-if="recognizedText" class="recognized-text-section mb-4">
                <h4 class="text-h6 mb-2">Văn bản được nhận dạng:</h4>
                <p class="pa-2 bg-surface rounded">{{ recognizedText }}</p>
              </div>

              <!-- Feedback Section -->
              <v-alert
                :type="scoreToAlertType"
                variant="tonal"
                class="mb-4"
              >
                {{ getVietnameseFeedback(pronounciationScore) }}
              </v-alert>

              <v-btn
                color="primary"
                block
                @click="resetRecording"
                prepend-icon="mdi-refresh"
              >
                Thử lại
              </v-btn>
            </div>
          </section>

          <!-- Example Section -->
          <section v-if="vocabulary.example" class="example-section mb-6">
            <h3 class="section-title">
              <v-icon color="primary" class="mr-2">mdi-format-quote-open</v-icon>
              Ví dụ
            </h3>
            <v-card flat color="background" class="example-card pa-4">
              <p class="japanese-text mb-2">{{ vocabulary.example }}</p>
              <p class="text-body-2 text-medium-emphasis">{{ vocabulary.exampleMeaning }}</p>

              <div class="d-flex justify-end">
                <v-btn icon size="small" @click="playExampleAudio" color="primary" variant="text" title="Nghe câu ví dụ">
                  <v-icon>mdi-volume-high</v-icon>
                </v-btn>
              </div>
            </v-card>
          </section>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'vue-toast-notification'
import { useVocabularyStore } from '@/stores'
import axios from 'axios'
import vocabularyService from '@/services/vocabulary.service'
import authService from '@/services/auth.service'
import aiService from '@/services/ai.service'

// Define types
interface Vocabulary {
  vocabId: string;
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  jlptLevel?: string;
  topicName?: string;
  categoryName?: string;
  isSaved?: boolean;
}

// Interface for speech analysis response
interface SpeechAnalysis {
  score: number;
  feedback: string;
  personalizedFeedback?: string;
  transcription: string;
  intonationScore?: number;
  clarityScore?: number;
  textScore?: number;
}

// Router and stores
const route = useRoute()
const router = useRouter()
const vocabularyStore = useVocabularyStore()
const toast = useToast()

// State
const loading = ref(true)
const error = ref('')
const vocabulary = ref<Vocabulary | null>(null)
const isRecording = ref(false)
const isProcessing = ref(false)
const recordedAudio = ref<string | null>(null)
const mediaRecorder = ref<MediaRecorder | null>(null)
const audioChunks = ref<Blob[]>([])
const pronounciationScore = ref(0)
const pronunciationFeedback = ref('')
const hasScore = ref(false)
const recognizedText = ref('')
const isPlayingRecording = ref(false)
const recordedAudioBlob = ref<Blob | null>(null)

// Audio context for recording
let audioContext: AudioContext | null = null
let audioStream: MediaStream | null = null

// Constants
const CIRCUMFERENCE = 2 * Math.PI * 60; // 2πr where r=60

// Computed
const circumference = computed(() => CIRCUMFERENCE)
const dashOffset = computed(() => {
  return CIRCUMFERENCE - (pronounciationScore.value / 100) * CIRCUMFERENCE
})

const scoreColorClass = computed(() => {
  const score = pronounciationScore.value
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-verygood'
  if (score >= 70) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
})

const scoreToAlertType = computed(() => {
  const score = pronounciationScore.value
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'error'
})

// Methods
const getJlptColor = (level: string | undefined): string => {
  if (!level) return 'grey';
  const colors: Record<string, string> = {
    'N1': 'red',
    'N2': 'orange',
    'N3': 'amber',
    'N4': 'light-green',
    'N5': 'green'
  }
  return colors[level] || 'grey'
}

const getVietnameseFeedback = (score: number): string => {
  if (score >= 90) {
    return 'Phát âm xuất sắc! Giọng của bạn rất tự nhiên.'
  } else if (score >= 80) {
    return 'Phát âm rất tốt. Chỉ cần điều chỉnh nhỏ để hoàn hảo.'
  } else if (score >= 70) {
    return 'Phát âm tốt. Hãy tập trung vào ngữ điệu và nhịp điệu.'
  } else if (score >= 60) {
    return 'Phát âm khá dễ hiểu nhưng cần cải thiện về âm điệu và nhấn mạnh.'
  } else {
    return 'Tiếp tục luyện tập! Hãy nghe người bản xứ nói và cố gắng bắt chước cách phát âm của họ.'
  }
}

const toggleSave = async () => {
  if (!vocabulary.value) return;

  try {
    if (vocabulary.value.isSaved) {
      await vocabularyService.removeSavedVocabulary(vocabulary.value.vocabId);
      toast.success('Đã bỏ lưu từ vựng', {
        position: 'top',
        duration: 2000
      });
    } else {
      await vocabularyService.saveVocabulary(vocabulary.value.vocabId);
      toast.success('Đã lưu từ vựng', {
        position: 'top',
        duration: 2000
      });
    }

    // Toggle the state locally
    vocabulary.value.isSaved = !vocabulary.value.isSaved;

  } catch (error) {
    console.error('Error toggling save status:', error);
    toast.error('Không thể cập nhật trạng thái lưu', {
      position: 'top',
      duration: 3000
    });
  }
}

const playAudio = async () => {
  if (!vocabulary.value) return;

  if (vocabulary.value?.audioPath) {
    try {
      const audio = new Audio(vocabulary.value.audioPath)
      await audio.play()
    } catch (err) {
      console.error('Error playing audio:', err)
      toast.error('Không thể phát âm thanh', {
        position: 'top',
        duration: 3000
      })
    }
    return;
  }

  // TTS implementation if no audio path is available
  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken()
    if (!authToken) {
      toast.error('Vui lòng đăng nhập để sử dụng tính năng đọc văn bản', {
        position: 'top',
        duration: 4000
      })
      // Redirect to login page after a short delay
      setTimeout(() => {
        router.push({
          name: 'login',
          query: { redirect: router.currentRoute.value.fullPath }
        })
      }, 1500)
      return
    }

    // Text to speak
    const textToSpeak = vocabulary.value.pronunciation ? vocabulary.value.pronunciation : vocabulary.value.term;

    // Show loading indicator
    toast.info('Đang tạo âm thanh...', {
      position: 'top',
      duration: 2000
    })

    // Use AI service to generate and play audio
    const audioBlob = await aiService.generateTTS(textToSpeak, 'vocabulary', 0.9, true);
    await aiService.playAudio(audioBlob);
  } catch (error) {
    console.error('Error generating or playing TTS audio:', error)

    // Special handling for 401 errors
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      toast.error('Dịch vụ TTS yêu cầu xác thực. Vui lòng đăng nhập lại khi có thể.', {
        position: 'top',
        duration: 3000
      })
    } else {
      toast.error(error instanceof Error ? error.message : 'Không thể tạo giọng nói', {
        position: 'top',
        duration: 3000
      })
    }
  }
}

const playExampleAudio = async () => {
  if (!vocabulary.value || !vocabulary.value.example) {
    toast.error('Không có câu ví dụ cho từ vựng này', {
      position: 'top',
      duration: 3000
    })
    return;
  }

  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken()
    if (!authToken) {
      toast.error('Vui lòng đăng nhập để sử dụng tính năng đọc văn bản', {
        position: 'top',
        duration: 4000
      })
      setTimeout(() => {
        router.push({
          name: 'login',
          query: { redirect: router.currentRoute.value.fullPath }
        })
      }, 1500)
      return
    }

    // Text to speak
    const textToSpeak = vocabulary.value.example;

    // Show loading indicator
    toast.info('Đang tạo âm thanh...',{
      position: 'top',
      duration: 2000
    })

    // Use AI service to generate and play audio
    const audioBlob = await aiService.generateTTS(textToSpeak, 'example', 1.0, true);
    await aiService.playAudio(audioBlob);
  } catch (error) {
    console.error('Error generating or playing TTS audio:', error)

    // Special handling for 401 errors
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      toast.error('Dịch vụ TTS yêu cầu xác thực. Vui lòng đăng nhập lại khi có thể.', {
        position: 'top',
        duration: 3000
      })
    } else {
      toast.error(error instanceof Error ? error.message : 'Không thể tạo giọng nói', {
        position: 'top',
        duration: 3000
      })
    }
  }
}

const startRecording = async () => {
  try {
    audioChunks.value = []
    audioStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })

    if (audioStream) {
      mediaRecorder.value = new MediaRecorder(audioStream)

      mediaRecorder.value.ondataavailable = (event: BlobEvent) => {
        if (event.data.size > 0) {
          audioChunks.value.push(event.data)
        }
      }

      mediaRecorder.value.onstop = () => {
        const audioBlob = new Blob(audioChunks.value, { type: 'audio/wav' })
        recordedAudioBlob.value = audioBlob
        recordedAudio.value = URL.createObjectURL(audioBlob)
        processRecording(audioBlob)
      }

      mediaRecorder.value.start()
      isRecording.value = true
      toast.info('Bắt đầu ghi âm - Hãy phát âm từ vựng', {
        position: 'top',
        duration: 2000
      })
    }
  } catch (err) {
    console.error('Error starting recording:', err)
    toast.error('Không thể truy cập microphone', {
      position: 'top',
      duration: 3000
    })
  }
}

const stopRecording = () => {
  if (mediaRecorder.value && isRecording.value) {
    mediaRecorder.value.stop()
    isRecording.value = false

    // Stop all audio tracks
    if (audioStream) {
      audioStream.getTracks().forEach(track => track.stop())
    }
  }
}

const playRecordedAudio = () => {
  if (!recordedAudio.value) return

  try {
    isPlayingRecording.value = true
    const audio = new Audio(recordedAudio.value)

    audio.onended = () => {
      isPlayingRecording.value = false
    }

    audio.onerror = () => {
      console.error('Error playing recorded audio')
      isPlayingRecording.value = false
      toast.error('Không thể phát bản ghi âm', {
        position: 'top',
        duration: 3000
      })
    }

    audio.play().catch(err => {
      console.error('Error playing recorded audio:', err)
      isPlayingRecording.value = false
      toast.error('Không thể phát bản ghi âm', {
        position: 'top',
        duration: 3000
      })
    })
  } catch (err) {
    console.error('Error playing recorded audio:', err)
    isPlayingRecording.value = false
    toast.error('Không thể phát bản ghi âm', {
      position: 'top',
      duration: 3000
    })
  }
}

const processRecording = async (audioBlob: Blob) => {
  isProcessing.value = true;

  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      toast.error('Vui lòng đăng nhập để sử dụng tính năng phân tích phát âm', {
        position: 'top',
        duration: 4000
      });
      isProcessing.value = false;
      return;
    }

    if (vocabulary.value?.term) {
      const term = vocabulary.value.term;

      // Use AI service to analyze speech
      const analysis = await aiService.analyzeSpeech(audioBlob, term, term);

      // Process response
      pronounciationScore.value = Math.round(analysis.score);
      recognizedText.value = analysis.transcription || '';

      if (analysis.personalizedFeedback) {
        pronunciationFeedback.value = analysis.personalizedFeedback;
      } else {
        pronunciationFeedback.value = analysis.feedback || getVietnameseFeedback(pronounciationScore.value);
      }

      hasScore.value = true;
    }
  } catch (err) {
    console.error('Error processing recording:', err);

    // Fallback to mock data for testing if API fails
    console.log('Using mock data due to API error');
    await new Promise(resolve => setTimeout(resolve, 1500));

    // Mock analysis with random score
    const mockScore = Math.floor(Math.random() * 40) + 60;
    pronounciationScore.value = mockScore;
    recognizedText.value = vocabulary.value?.term || '';
    pronunciationFeedback.value = getVietnameseFeedback(mockScore);
    hasScore.value = true;

    toast.warning('Đang sử dụng dữ liệu mẫu do lỗi kết nối API', {
      position: 'top',
      duration: 3000
    });
  } finally {
    isProcessing.value = false;
  }
}

const resetRecording = () => {
  if (recordedAudio.value) {
    URL.revokeObjectURL(recordedAudio.value)
  }

  recordedAudio.value = null
  recordedAudioBlob.value = null
  pronounciationScore.value = 0
  pronunciationFeedback.value = ''
  recognizedText.value = ''
  hasScore.value = false
  isPlayingRecording.value = false

  // Clear any existing media resources
  if (audioStream) {
    audioStream.getTracks().forEach(track => track.stop())
  }

  toast.info('Đã đặt lại bản ghi. Bạn có thể thử lại.', {
    position: 'top',
    duration: 2000
  })
}

// Add navigateBack method
const navigateBack = () => {
  // Check if we can go back in the browser history
  if (window.history.length > 2) {
    // When we use the browser back button, the previous page's state should be preserved
    router.back()
  } else {
    // If there's no history or we came directly to this page,
    // navigate to vocabulary learning view preserving any query params
    // Get any search parameters from the URL query
    const params = { ...route.query };

    // If we came from vocabulary learning view with a term parameter,
    // try to keep track of the search/filter state in localStorage
    // This ensures that even if we navigate directly, we can attempt to restore the previous state
    const fromRoute = router.currentRoute.value.query.from;
    if (fromRoute === 'vocabularyLearning' || params.from === 'vocabularyLearning') {
      // Check if we have any saved state from VocabularyLearningView
      const savedSearchState = localStorage.getItem('vocabularyLearningSearchState');
      if (savedSearchState) {
        try {
          // Parse the saved state
          const searchState = JSON.parse(savedSearchState);
          // Apply it to our query params
          Object.assign(params, searchState);
        } catch (e) {
          console.error('Error parsing saved search state:', e);
        }
      }
    }

    router.push({
      name: 'vocabularyLearning',
      query: params
    });
  }
}

// Lifecycle hooks
onMounted(async () => {
  const term = route.params.term
  if (term && typeof term === 'string') {
    loading.value = true
    error.value = ''

    try {
      const response = await vocabularyService.getVocabularyByTerm(term)
      vocabulary.value = response
    } catch (err) {
      console.error('Error loading vocabulary:', err)
      error.value = 'Không thể tải thông tin từ vựng'
    } finally {
      loading.value = false
    }
  } else {
    error.value = 'Không có từ vựng được cung cấp'
    loading.value = false
  }
})

onUnmounted(() => {
  // Clean up resources when component is destroyed
  if (isRecording.value) {
    stopRecording()
  }

  if (isPlayingRecording.value) {
    isPlayingRecording.value = false
  }

  if (recordedAudio.value) {
    URL.revokeObjectURL(recordedAudio.value)
  }
})
</script>

<style scoped lang="scss">
.pronunciation-view-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;

  .loading-container,
  .error-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
  }

  .japanese-text {
    font-family: 'Noto Sans JP', sans-serif;
  }

  .section-title {
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--v-primary-base);
  }

  .recording-controls {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;

    .gap-3 {
      gap: 12px;
    }
  }

  .results-container {
    padding: 20px;
    border-radius: 8px;
    background-color: rgba(var(--v-theme-surface-variant), 0.05);
  }

  /* Score Circle Styles */
  .score-circle-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;
  }

  .score-circle-wrapper {
    position: relative;
    width: 150px;
    height: 150px;
    perspective: 1000px;
  }

  .score-circle {
    width: 150px;
    height: 150px;
    position: relative;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: all 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);

    .score-number {
      position: absolute;
      font-size: 2.8rem;
      font-weight: bold;
      z-index: 2;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      color: white;
      animation: fadeIn 0.8s ease-out forwards;
    }

    .circle-progress {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 1;
      transform: rotate(-90deg);
      transition: all 1s ease;
    }

    .circle-bg {
      fill: none;
      stroke: rgba(255, 255, 255, 0.2);
    }

    .circle-progress-bar {
      fill: none;
      transition: stroke-dashoffset 1s ease;
      stroke-linecap: round;
    }
  }

  .score-excellent {
    background: radial-gradient(circle at center, #4CAF50 0%, #2E7D32 80%);
    box-shadow: 0 0 20px rgba(76, 175, 80, 0.5);

    .circle-progress-bar {
      stroke: #A5D6A7;
    }
  }

  .score-verygood {
    background: radial-gradient(circle at center, #8BC34A 0%, #558B2F 80%);
    box-shadow: 0 0 20px rgba(139, 195, 74, 0.5);

    .circle-progress-bar {
      stroke: #C5E1A5;
    }
  }

  .score-good {
    background: radial-gradient(circle at center, #FDD835 0%, #F9A825 80%);
    box-shadow: 0 0 20px rgba(255, 235, 59, 0.5);

    .circle-progress-bar {
      stroke: #FFF59D;
    }
  }

  .score-average {
    background: radial-gradient(circle at center, #FF9800 0%, #E65100 80%);
    box-shadow: 0 0 20px rgba(255, 152, 0, 0.5);

    .circle-progress-bar {
      stroke: #FFCC80;
    }
  }

  .score-poor {
    background: radial-gradient(circle at center, #F44336 0%, #B71C1C 80%);
    box-shadow: 0 0 20px rgba(244, 67, 54, 0.5);

    .circle-progress-bar {
      stroke: #EF9A9A;
    }
  }

  @keyframes fadeIn {
    0% { opacity: 0; transform: scale(0.8); }
    100% { opacity: 1; transform: scale(1); }
  }

  .recognized-text-section {
    margin-top: 16px;
    padding: 8px;
    border-radius: 8px;
    background-color: rgba(var(--v-theme-surface), 0.7);
  }

  .example-card {
    border-radius: 8px;
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(var(--v-theme-on-surface), 0.05);
    }
  }

  .pronunciation-controls-card {
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(var(--v-theme-on-surface), 0.05);
    }
  }
}
</style>
