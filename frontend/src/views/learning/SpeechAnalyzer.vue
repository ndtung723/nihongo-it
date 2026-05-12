<template>
  <div class="speech-analyzer">
    <h2>Phân Tích Phát Âm Tiếng Nhật</h2>

    <div class="control-panel">
      <div class="sentence-selector">
        <label for="sentence-select">Chọn câu:</label>
        <select id="sentence-select" v-model="sentence">
          <option v-for="s in sentences" :key="s.id" :value="s.text">
            {{ s.text }}
          </option>
        </select>
        <button
          @click="playSampleAudio"
          class="play-btn"
          :disabled="isPlayingSample"
        >
          <span v-if="!isPlayingSample">Nghe mẫu</span>
          <span v-else>Đang phát...</span>
        </button>
      </div>

      <div class="record-buttons">
        <button
          @click="startRecording"
          :disabled="isRecording"
          class="record-btn"
        >
          <span v-if="!isRecording">Bắt đầu ghi âm</span>
          <span v-else>Đang ghi âm...</span>
        </button>
        <button
          @click="stopRecording"
          :disabled="!isRecording"
          class="stop-btn"
        >
          Dừng ghi âm
        </button>
        <button
          v-if="recordedAudioBlob"
          @click="playRecordedAudio"
          :disabled="isPlayingRecording || isRecording"
          class="play-recording-btn"
        >
          <span v-if="!isPlayingRecording">Nghe bản ghi</span>
          <span v-else>Đang phát...</span>
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="loading">
      <p>Đang phân tích...</p>
    </div>

    <div class="sentence-display">
      <div class="sentence-with-audio">
        <p>{{ sentence }}</p>
        <button
          @click="playSampleAudio"
          class="play-btn small"
          :disabled="isPlayingSample"
          title="Nghe mẫu"
        >
          <span>▶</span>
        </button>
      </div>
    </div>

    <div v-if="analysis" class="analysis-results">
      <h3>Kết quả phân tích</h3>

      <!-- Hiển thị từng từ với màu dựa trên độ chính xác và tooltip cho gợi ý -->
      <div
        class="word-analysis"
        v-if="analysis.words && analysis.words.length > 0"
      >
        <p class="original-sentence">
          <strong>Câu gốc đã loại bỏ dấu câu:</strong>
          <span
            v-for="(word, index) in analysis.words"
            :key="index"
            :class="{
              'incorrect-word': !word.isCorrect,
              'correct-word': word.isCorrect,
            }"
            :title="word.suggestion || ''"
            @click="
              word.audioUrl && !word.isCorrect
                ? playWordTTS(word.audioUrl)
                : null
            "
          >
            {{ word.text }}
            <small v-if="!word.isCorrect" class="hint-indicator">?</small>
            <small
              v-if="!word.isCorrect && word.audioUrl"
              class="sound-indicator"
              >♪</small
            >
          </span>
        </p>
        <p class="analysis-note">
          <em
            >Ghi chú: Dấu câu và ký tự đặc biệt đã được loại bỏ để so sánh chính
            xác hơn. Nhấn vào từ đỏ để nghe phát âm chuẩn.</em
          >
        </p>
      </div>

      <!-- Hiển thị phản hồi cá nhân hóa -->
      <div class="personalized-feedback" v-if="analysis.personalizedFeedback">
        <h4>Phản hồi:</h4>
        <p>{{ analysis.personalizedFeedback }}</p>
      </div>

      <div class="score-container">
        <p class="score">
          Điểm tổng: <span>{{ analysis.score }}/100</span>
        </p>
        <div class="score-breakdown">
          <div class="score-component intonation">
            <div class="component-header">
              <div class="score-label">Ngữ điệu:</div>
              <div class="score-value">
                {{ analysis.intonationScore || 0 }}/100
              </div>
            </div>
            <div class="score-progress-container">
              <div
                class="score-progress-bar"
                :style="{
                  width: `${analysis.intonationScore || 0}%`,
                  backgroundColor: getScoreColor('intonation'),
                }"
              ></div>
            </div>
            <div class="score-details">
              <div
                class="score-status"
                :class="getScoreStatusClass('intonation')"
              >
                {{ getScoreDescription("intonation") }}:
                {{ analysis.intonation }}
              </div>
              <div
                v-if="!isPitchValid(analysis.pitchMean)"
                class="pitch-warning"
              >
                {{ getPitchWarningMessage(analysis.pitchMean) }}
              </div>
              <div class="score-metrics" v-else-if="analysis.pitchMean">
                <span>Cao độ trung bình: {{ analysis.pitchMean }}Hz</span>
                <span
                  class="score-tip"
                  v-if="
                    analysis.samplePitchData &&
                    analysis.samplePitchData.length > 0
                  "
                >
                  (So sánh với mẫu)
                </span>
                <span class="score-tip" v-else> (Tối ưu: 150-300Hz) </span>
              </div>
            </div>
          </div>
          <div class="score-component clarity">
            <div class="component-header">
              <div class="score-label">Độ rõ:</div>
              <div class="score-value">
                {{ analysis.clarityScore || 0 }}/100
              </div>
            </div>
            <div class="score-progress-container">
              <div
                class="score-progress-bar"
                :style="{
                  width: `${analysis.clarityScore || 0}%`,
                  backgroundColor: getScoreColor('clarity'),
                }"
              ></div>
            </div>
            <div class="score-details">
              <div class="score-status" :class="getScoreStatusClass('clarity')">
                {{ getScoreDescription("clarity") }}: {{ analysis.clarity }}
              </div>
              <div class="score-metrics" v-if="analysis.f1Mean">
                <span>Formant F1 trung bình: {{ analysis.f1Mean }}Hz</span>
                <span
                  class="score-tip"
                  v-if="
                    analysis.sampleFormantData &&
                    analysis.sampleFormantData.length > 0
                  "
                >
                  (So sánh với mẫu)
                </span>
                <span class="score-tip" v-else> (Tối ưu: 450-650Hz) </span>
              </div>
              <div
                class="formant-tip"
                v-if="analysis.f1Mean && (analysis.clarityScore || 0) < 70"
              >
                <span v-if="analysis.f1Mean < 450"
                  >Thử mở rộng miệng hơn khi phát âm để cải thiện độ rõ</span
                >
                <span v-else-if="analysis.f1Mean > 650"
                  >Thử điều chỉnh vị trí lưỡi khi phát âm để cải thiện độ
                  rõ</span
                >
                <span v-else>Tập trung vào phát âm rõ ràng từng âm tiết</span>
              </div>
            </div>
          </div>
          <div
            class="score-component text-similarity"
            v-if="analysis.textScore !== undefined"
          >
            <div class="component-header">
              <div class="score-label">Độ chính xác từ ngữ:</div>
              <div class="score-value">{{ analysis.textScore }}/100</div>
            </div>
            <div class="score-progress-container">
              <div
                class="score-progress-bar"
                :style="{
                  width: `${analysis.textScore}%`,
                  backgroundColor: getScoreColor('text'),
                }"
              ></div>
            </div>
            <div v-if="analysis.textScore === 0" class="text-match-warning">
              Không nhận diện được từ nào khớp với câu mẫu. Hãy thử:
              <ul>
                <li>Kiểm tra microphone của bạn và phát âm to, rõ ràng hơn</li>
                <li>Mở rộng miệng khi phát âm (F1 tối ưu: 450-650Hz)</li>
                <li>Tập trung vào từng từ, không nói quá nhanh</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="score-formula">
          <p><strong>Công thức tính điểm:</strong></p>
          <p v-html="getScoreFormula()"></p>
        </div>
      </div>

      <div class="details">
        <p>
          <strong>Văn bản được nhận dạng:</strong> {{ analysis.transcription }}
        </p>
      </div>
    </div>

    <div v-if="error" class="error">
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import { useRouter } from "vue-router";
import authService from "@/services/auth.service";

// TypeScript declarations for Recorder.js
declare global {
  interface Window {
    Recorder: {
      new (
        input: MediaStreamAudioSourceNode,
        config: { numChannels: number },
      ): RecorderInstance;
      forceDownload: (blob: Blob, filename: string) => void;
    };
    webkitAudioContext: typeof AudioContext;
  }
}

interface RecorderInstance {
  record: () => void;
  stop: () => void;
  clear: () => void;
  exportWAV: (callback: (blob: Blob) => void) => void;
}

// Word interface for analysis with enhanced properties
interface Word {
  text: string;
  isCorrect: boolean;
  suggestion?: string | null;
  audioUrl?: string | null;
}

// Enhanced analysis interface with personalized feedback
interface Analysis {
  score: number;
  feedback: string;
  personalizedFeedback?: string;
  intonation: string;
  clarity: string;
  rhythm: string;
  transcription: string;
  words: Word[];
  intonationScore?: number;
  clarityScore?: number;
  rhythmScore?: number;
  textScore?: number;
  audioSimilarityScore?: number;
  pitchMean?: number;
  f1Mean?: number;
  onsetCount?: number;
  expectedSyllables?: number;
  formulaWeights?: {
    withoutAudio: { text: number; intonation: number; clarity: number };
  };
  userPitchData?: number[];
  samplePitchData?: number[];
  userFormantData?: number[];
  sampleFormantData?: number[];
}

// Trạng thái
const sentences = [
  { id: "today_library", text: "今日、図書館で本を借りました。" },
  { id: "hello_friend", text: "こんにちは、友達！" },
  { id: "weather_good", text: "今日の天気はとても良いですね。" },
  { id: "japanese_study", text: "日本語を勉強することは楽しいです。" },
];
const sentence = ref(sentences[0].text);
const analysis = ref<Analysis | null>(null);
const isRecording = ref(false);
const isLoading = ref(false);
const error = ref<string | null>(null);
const isPlayingSample = ref(false);
const sampleAudio = ref<HTMLAudioElement | null>(null);

// Thêm biến cho tính năng phát lại bản ghi
const recordedAudioBlob = ref<Blob | null>(null);
const recordedAudioUrl = ref<string | null>(null);
const isPlayingRecording = ref(false);
const recordedAudio = ref<HTMLAudioElement | null>(null);

// Add router for authentication redirects
const router = useRouter();

// Get the sample ID from the sentence text
const getSampleId = (sentenceText: string): string => {
  const foundSentence = sentences.find((s) => s.text === sentenceText);
  return foundSentence?.id || "";
};

// Play sample audio function
const playSampleAudio = async () => {
  if (isPlayingSample.value) return;

  const sampleId = getSampleId(sentence.value);
  if (!sampleId) {
    error.value = "Không thể xác định câu mẫu để phát âm thanh";
    return;
  }

  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      error.value = "Vui lòng đăng nhập để sử dụng tính năng phát âm mẫu";
      setTimeout(() => {
        router.push({
          name: "login",
          query: { redirect: router.currentRoute.value.fullPath },
        });
      }, 1500);
      return;
    }

    // Create audio element if it doesn't exist
    if (!sampleAudio.value) {
      sampleAudio.value = new Audio();

      // Add event listeners
      sampleAudio.value.addEventListener("ended", () => {
        isPlayingSample.value = false;
      });

      sampleAudio.value.addEventListener("error", () => {
        error.value = "Lỗi khi phát âm thanh mẫu";
        isPlayingSample.value = false;
      });
    }

    // Set source to MP3 for smaller file size with authentication header
    // Use Authorization header instead of token parameter
    const audioUrl = `http://localhost:8080/api/v1/speech/sample-audio/${sampleId}?format=mp3`;

    // Create a new XMLHttpRequest to fetch the audio with proper authentication
    const xhr = new XMLHttpRequest();
    xhr.open("GET", audioUrl, true);
    xhr.responseType = "blob";
    xhr.setRequestHeader("Authorization", `Bearer ${authToken}`);

    xhr.onload = async function () {
      if (xhr.status === 200) {
        const blob = xhr.response;
        const objectUrl = URL.createObjectURL(blob);

        // Set the audio source to the blob URL
        if (sampleAudio.value) {
          sampleAudio.value.src = objectUrl;

          // Play audio
          isPlayingSample.value = true;
          try {
            await sampleAudio.value.play();
          } catch {
            isPlayingSample.value = false;
            error.value = "Không thể phát âm thanh mẫu";
          }
        } else {
          error.value = "Lỗi phát âm thanh mẫu: audio element không tồn tại";
          isPlayingSample.value = false;
          URL.revokeObjectURL(objectUrl); // Clean up
        }
      } else {
        error.value = `Lỗi khi tải âm thanh mẫu: ${xhr.status}`;
        isPlayingSample.value = false;
      }
    };

    xhr.onerror = function () {
      error.value = "Lỗi kết nối khi tải âm thanh mẫu";
      isPlayingSample.value = false;
    };

    xhr.send();
  } catch {
    error.value = "Không thể phát âm thanh mẫu";
    isPlayingSample.value = false;
  }
};

// Biến toàn cục
let audioContext: AudioContext | null = null;
let recorder: RecorderInstance | null = null;

// Trạng thái recorder
let stream: MediaStream | null = null;

// Ghi âm với định dạng WAV
const startRecording = async () => {
  error.value = null;
  analysis.value = null;

  try {
    // Yêu cầu quyền truy cập microphone
    stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
    });

    // Khởi tạo AudioContext với sample rate 44.1kHz
    audioContext = new (window.AudioContext || window.webkitAudioContext)({
      sampleRate: 44100,
    });

    // Kiểm tra xem Recorder có tồn tại không
    if (typeof window.Recorder === "undefined") {
      error.value =
        "Thư viện ghi âm không được tải. Vui lòng tải lại trang sau khi kết nối internet.";
      return;
    }

    // Tạo input từ stream
    const input = audioContext.createMediaStreamSource(stream);

    // Khởi tạo Recorder.js
    recorder = new window.Recorder(input, {
      numChannels: 1,
    });

    // Bắt đầu ghi âm
    recorder.record();

    isRecording.value = true;
  } catch {
    error.value =
      "Không thể bắt đầu ghi âm. Vui lòng cấp quyền sử dụng microphone.";
  }
};

// Dừng ghi âm và phân tích
const stopRecording = async () => {
  if (!recorder) {
    error.value = "Không có bộ ghi âm - vui lòng thử lại";
    return;
  }

  try {
    isRecording.value = false;
    isLoading.value = true;

    // Dừng ghi âm và lấy Blob WAV

    // Sử dụng Recorder.js API để dừng và lấy WAV blob
    recorder.stop();

    // Tạo WAV blob từ Recorder.js
    recorder.exportWAV((blob) => {
      // Xử lý blob trong callback
      handleAudioBlob(blob);
    });
  } catch (e) {
    error.value =
      "Lỗi khi xử lý audio: " + (e instanceof Error ? e.message : String(e));
    isLoading.value = false;
  }
};

// Xử lý audio blob sau khi dừng ghi âm
const handleAudioBlob = async (blob: Blob) => {
  try {
    // Lưu blob cho tính năng phát lại
    recordedAudioBlob.value = blob;

    // Xóa URL cũ nếu có
    if (recordedAudioUrl.value) {
      URL.revokeObjectURL(recordedAudioUrl.value);
      recordedAudioUrl.value = null;
    }

    // Dừng tất cả tracks
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      stream = null;
    }

    // Đóng AudioContext
    if (audioContext && audioContext.state !== "closed") {
      await audioContext.close();
      audioContext = null;
    }

    // Reset recorder
    if (recorder) {
      recorder.clear();
      recorder = null;
    }

    // Kiểm tra kích thước blob
    if (!blob || blob.size < 1000) {
      error.value = "Không ghi được âm thanh. Vui lòng thử lại và nói to hơn.";
      isLoading.value = false;
      return;
    }

    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      error.value = "Vui lòng đăng nhập để sử dụng tính năng phân tích phát âm";
      isLoading.value = false;
      setTimeout(() => {
        router.push({
          name: "login",
          query: { redirect: router.currentRoute.value.fullPath },
        });
      }, 1500);
      return;
    }

    // Tạo FormData
    const formData = new FormData();
    formData.append("file", blob, "recording.wav");
    formData.append("reference_text", sentence.value);
    formData.append("sample_id", getSampleId(sentence.value));

    // Gửi đến Java service endpoint thay vì Python trực tiếp
    try {
      const response = await fetch(
        "http://localhost:8080/api/v1/ai/speech/analyze-audio-enhanced",
        {
          method: "POST",
          body: formData,
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        },
      );

      if (!response.ok) {
        const errorData =
          (await response.json().catch(() => null)) ||
          (await response.text().catch(() => null)) ||
          `Error ${response.status}`;

        if (response.status === 401) {
          error.value = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.";
          setTimeout(() => {
            router.push({
              name: "login",
              query: { redirect: router.currentRoute.value.fullPath },
            });
          }, 1500);
        } else if (response.status === 0 || response.status === 404) {
          throw new Error(
            "Không thể kết nối đến server FastAPI. Vui lòng kiểm tra nếu server đang chạy.",
          );
        } else if (response.status === 400) {
          throw new Error(
            `File âm thanh không hợp lệ hoặc tham số sai: ${errorData.detail || errorData}`,
          );
        } else if (response.status === 415 || response.status === 422) {
          throw new Error(
            `Định dạng yêu cầu không hợp lệ: ${errorData.detail || errorData}`,
          );
        } else if (response.status === 405) {
          throw new Error("Phương thức không được hỗ trợ. Vui lòng thử lại.");
        } else if (response.status === 500) {
          throw new Error(
            `Lỗi server FastAPI: ${errorData.detail || errorData}`,
          );
        } else {
          throw new Error(`Lỗi server: ${errorData.detail || errorData}`);
        }
      }

      // Xử lý kết quả
      const result = await response.json();

      // Ensure all scores in the result object are used directly without any fixed values
      analysis.value = result;
    } catch (e) {
      error.value = e instanceof Error ? e.message : String(e);
    } finally {
      isLoading.value = false;
    }
  } catch (e) {
    error.value =
      "Lỗi khi xử lý audio: " + (e instanceof Error ? e.message : String(e));
    isLoading.value = false;
  }
};

// Watch for sentence changes to stop audio playback
watch(sentence, () => {
  if (sampleAudio.value && isPlayingSample.value) {
    sampleAudio.value.pause();
    sampleAudio.value.currentTime = 0;
    isPlayingSample.value = false;
  }

  // Reset analysis when sentence changes
  analysis.value = null;
  error.value = null;
});

onMounted(() => {
  // Kiểm tra nếu Recorder.js đã được tải
  if (typeof window.Recorder === "undefined") {
    error.value = "Thư viện ghi âm chưa sẵn sàng. Vui lòng tải lại trang.";
  }

  // Kiểm tra kết nối tới backend
  checkBackendConnection();
});

// Kiểm tra kết nối tới backend
const checkBackendConnection = async () => {
  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken();

    // Check Java service health
    const response = await fetch("http://localhost:8080/api/v1/speech/health", {
      method: "GET",
      headers: {
        Accept: "application/json",
        Authorization: authToken ? `Bearer ${authToken}` : "",
      },
    });

    if (response.ok) {
      const data = await response.json();
      if (data.status === "healthy") {
      } else {
        error.value =
          "Server phân tích âm thanh có thể không hoạt động bình thường.";
      }
    } else if (response.status === 401) {
      error.value =
        "Vui lòng đăng nhập để sử dụng tính năng phân tích phát âm.";
    } else {
      error.value =
        "Không thể kết nối tới server. Vui lòng kiểm tra nếu server đang chạy.";
    }
  } catch {
    error.value =
      "Không thể kết nối tới server phân tích âm thanh. Vui lòng kiểm tra nếu server đang chạy.";
  }
};

onBeforeUnmount(() => {
  // Dọn dẹp
  if (stream) {
    stream.getTracks().forEach((track) => track.stop());
  }

  if (audioContext && audioContext.state !== "closed") {
    audioContext.close();
  }

  // Clean up audio playback resources
  if (sampleAudio.value) {
    sampleAudio.value.pause();
    sampleAudio.value.src = "";
    sampleAudio.value = null;
  }

  // Clean up recorded audio resources
  if (recordedAudio.value) {
    recordedAudio.value.pause();
    recordedAudio.value.src = "";
    recordedAudio.value = null;
  }

  if (recordedAudioUrl.value) {
    URL.revokeObjectURL(recordedAudioUrl.value);
    recordedAudioUrl.value = null;
  }

  recordedAudioBlob.value = null;
});

// Phát lại bản ghi âm thanh
const playRecordedAudio = async () => {
  if (isPlayingRecording.value || !recordedAudioBlob.value) return;

  try {
    // Tạo URL cho audio blob nếu chưa có
    if (!recordedAudioUrl.value) {
      recordedAudioUrl.value = URL.createObjectURL(recordedAudioBlob.value);
    }

    // Tạo audio element nếu chưa có
    if (!recordedAudio.value) {
      recordedAudio.value = new Audio();

      // Thêm event listeners
      recordedAudio.value.addEventListener("ended", () => {
        isPlayingRecording.value = false;
      });

      recordedAudio.value.addEventListener("error", () => {
        error.value = "Không thể phát bản ghi âm thanh";
        isPlayingRecording.value = false;
      });
    }

    // Đặt nguồn và phát
    recordedAudio.value.src = recordedAudioUrl.value;
    isPlayingRecording.value = true;
    await recordedAudio.value.play();
  } catch {
    error.value = "Không thể phát bản ghi âm thanh";
    isPlayingRecording.value = false;
  }
};

// Play TTS audio for a word
const playWordTTS = async (audioUrl: string | null | undefined) => {
  if (!audioUrl) {
    error.value = "Không có âm thanh mẫu cho từ này";
    return;
  }

  try {
    // Verify authentication before proceeding
    const authToken = authService.getToken();
    if (!authToken) {
      error.value = "Vui lòng đăng nhập để sử dụng tính năng phát âm mẫu";
      setTimeout(() => {
        router.push({
          name: "login",
          query: { redirect: router.currentRoute.value.fullPath },
        });
      }, 1500);
      return;
    }

    // Create a new XMLHttpRequest to fetch the audio with proper authentication
    const xhr = new XMLHttpRequest();
    xhr.open("GET", audioUrl, true);
    xhr.responseType = "blob";
    xhr.setRequestHeader("Authorization", `Bearer ${authToken}`);

    xhr.onload = async function () {
      if (xhr.status === 200) {
        const blob = xhr.response;
        const objectUrl = URL.createObjectURL(blob);

        // Create and play audio element
        const ttsAudio = new Audio(objectUrl);
        await ttsAudio.play();

        // Clean up object URL when audio finishes
        ttsAudio.onended = function () {
          URL.revokeObjectURL(objectUrl);
        };
      } else {
        error.value = `Lỗi khi tải âm thanh mẫu: ${xhr.status}`;
      }
    };

    xhr.onerror = function () {
      error.value = "Lỗi kết nối khi tải âm thanh mẫu";
    };

    xhr.send();
  } catch {
    error.value = "Không thể phát âm thanh mẫu";
  }
};

// Function to get score color based on the score value
const getScoreColor = (component: "intonation" | "clarity" | "text") => {
  if (!analysis.value) return "#ccc";

  let score = 0;

  switch (component) {
    case "intonation":
      score = analysis.value.intonationScore || 0;
      break;
    case "clarity":
      score = analysis.value.clarityScore || 0;
      break;
    case "text":
      score = analysis.value.textScore || 0;
      break;
  }

  // Color spectrum with more granular ranges
  if (score >= 95) {
    return "#1E88E5"; // Bright blue for excellent
  } else if (score >= 85) {
    return "#2ecc71"; // Green for very good
  } else if (score >= 70) {
    return "#27ae60"; // Darker green for good
  } else if (score >= 60) {
    return "#f39c12"; // Yellow for average
  } else if (score >= 40) {
    return "#e67e22"; // Orange for below average
  } else {
    return "#e74c3c"; // Red for poor
  }
};

// Hàm để lấy class CSS dựa trên trạng thái
const getScoreStatusClass = (component: "intonation" | "clarity") => {
  if (!analysis.value) return "";

  let score = 0;

  switch (component) {
    case "intonation":
      score = analysis.value.intonationScore || 0;
      break;
    case "clarity":
      score = analysis.value.clarityScore || 0;
      break;
  }

  if (score >= 90) {
    return "status-excellent";
  } else if (score >= 70) {
    return "status-good";
  } else if (score >= 50) {
    return "status-average";
  } else {
    return "status-need-improvement";
  }
};

// Get status description based on score
const getScoreDescription = (component: "intonation" | "clarity") => {
  if (!analysis.value) return "";

  let score = 0;

  switch (component) {
    case "intonation":
      score = analysis.value.intonationScore || 0;
      break;
    case "clarity":
      score = analysis.value.clarityScore || 0;
      break;
  }

  if (score >= 90) {
    return "Xuất sắc";
  } else if (score >= 80) {
    return "Rất tốt";
  } else if (score >= 70) {
    return "Tốt";
  } else if (score >= 60) {
    return "Khá";
  } else if (score >= 50) {
    return "Trung bình";
  } else {
    return "Cần cải thiện";
  }
};

// Hàm để hiển thị công thức tính điểm
const getScoreFormula = () => {
  if (!analysis.value) return "";

  const textScore = analysis.value.textScore || 0;
  const intonationScore = analysis.value.intonationScore || 0;
  const clarityScore = analysis.value.clarityScore || 0;

  let formula = "";

  // Special handling for text score of 0
  if (textScore === 0) {
    formula = `<strong>Không có khớp văn bản:</strong> Khi độ chính xác từ ngữ = 0%, điểm tổng cũng sẽ là 0%.<br>
              Điểm cuối cùng = 0%`;
    return formula;
  }

  // Use the weights directly from the backend if available
  const weights = analysis.value.formulaWeights || {
    withoutAudio: { text: 0.7, intonation: 0.15, clarity: 0.15 },
  };

  const textWeight = weights.withoutAudio.text;
  const intonationWeight = weights.withoutAudio.intonation;
  const clarityWeight = weights.withoutAudio.clarity;

  if (textScore >= 95 && intonationScore >= 95 && clarityScore >= 95) {
    formula = `<strong>Công thức thông thường:</strong> Độ chính xác từ ngữ (${textScore}%) × ${textWeight} + Ngữ điệu (${intonationScore}%) × ${intonationWeight} + Độ rõ (${clarityScore}%) × ${clarityWeight}<br>
              <strong>Tăng điểm xuất sắc:</strong> Khi các thành phần đều ≥ 95%, điểm số được nâng cao đến ${analysis.value.score}%`;
  } else {
    formula = `Độ chính xác từ ngữ (${textScore}%) × ${textWeight} + Ngữ điệu (${intonationScore}%) × ${intonationWeight} + Độ rõ (${clarityScore}%) × ${clarityWeight}`;
  }

  return formula;
};

// Function to check if pitch analysis is valid
const isPitchValid = (pitchMean?: number) => {
  if (pitchMean === undefined || pitchMean === null) return false;
  return !isNaN(pitchMean) && pitchMean >= 150 && pitchMean <= 300;
};

// Function to get pitch warning message based on pitch analysis
const getPitchWarningMessage = (pitchMean?: number) => {
  if (pitchMean === undefined || pitchMean === null)
    return "Không có dữ liệu phân tích cao độ";
  if (pitchMean < 150) return "Cao độ quá thấp";
  if (pitchMean > 300) return "Cao độ quá cao";
  return "Cao độ hợp lệ";
};
</script>

<style scoped>
.speech-analyzer {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h2 {
  color: #2c3e50;
  margin-bottom: 30px;
  text-align: center;
}

.control-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.sentence-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.record-buttons {
  display: flex;
  gap: 10px;
}

button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
}

.record-btn {
  background-color: #e74c3c;
  color: white;
}

.stop-btn {
  background-color: #3498db;
  color: white;
}

.play-btn {
  background-color: #27ae60;
  color: white;
  align-self: flex-start;
  margin-top: 8px;
}

.play-recording-btn {
  background-color: #9b59b6;
  color: white;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  margin: 20px 0;
  font-style: italic;
}

.sentence-display {
  margin: 20px 0;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border-left: 4px solid #2c3e50;
}

.sentence-with-audio {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sentence-with-audio p {
  margin: 0;
  flex-grow: 1;
}

.play-btn.small {
  padding: 5px 10px;
  font-size: 14px;
  min-width: 40px;
}

.analysis-results {
  margin-top: 30px;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.personalized-feedback {
  margin: 20px 0;
  padding: 15px;
  background-color: #e8f4fc;
  border-radius: 6px;
  border-left: 4px solid #3498db;
}

.personalized-feedback h4 {
  margin-top: 0;
  color: #2980b9;
}

.score-container {
  margin: 25px 0;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.score {
  font-size: 28px;
  text-align: center;
  margin-bottom: 20px;
  color: #2c3e50;
}

.score span {
  font-weight: bold;
  color: #27ae60;
  background: #e8f5e9;
  padding: 5px 15px;
  border-radius: 20px;
}

.score-breakdown {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.score-component {
  display: flex;
  flex-direction: column;
  padding: 15px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.component-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.score-label {
  font-weight: 600;
  color: #333;
}

.score-value {
  font-weight: 700;
  color: #1a73e8;
}

.score-progress-container {
  height: 10px;
  background-color: #e9ecef;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 8px;
}

.score-progress-bar {
  height: 100%;
  transition: width 0.5s ease;
}

.score-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-top: 5px;
}

.score-status {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 5px;
}

.score-metrics {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
}

.score-tip {
  font-style: italic;
  color: #999;
}

.status-excellent {
  background-color: #e3f2fd;
  color: #1565c0;
  border-left: 4px solid #1565c0;
}

.status-good {
  background-color: #e8f5e9;
  color: #2e7d32;
  border-left: 4px solid #2e7d32;
}

.status-average {
  background-color: #fff8e1;
  color: #f57c00;
  border-left: 4px solid #f57c00;
}

.status-need-improvement {
  background-color: #ffebee;
  color: #c62828;
  border-left: 4px solid #c62828;
}

.score-formula {
  margin-top: 20px;
  padding: 10px;
  background-color: #e3f2fd;
  border-radius: 6px;
  font-size: 14px;
}

.details {
  margin-top: 30px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.error {
  margin-top: 20px;
  padding: 10px;
  background-color: #f8d7da;
  color: #721c24;
  border-radius: 4px;
}

.word-analysis {
  margin: 20px 0;
  padding: 15px;
  background-color: #f0f8ff;
  border-radius: 6px;
  border-left: 4px solid #3498db;
}

.original-sentence {
  font-size: 18px;
  line-height: 1.6;
}

.original-sentence span {
  display: inline-block;
  margin-right: 5px;
  padding: 3px 6px;
  border-radius: 4px;
  transition: all 0.2s;
  cursor: help;
}

.correct-word {
  background-color: transparent;
}

.incorrect-word {
  color: white;
  background-color: #e74c3c;
  font-weight: bold;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.incorrect-word:hover {
  background-color: #c0392b;
  transform: scale(1.05);
}

.analysis-note {
  font-size: 14px;
  color: #777;
  margin-top: 5px;
}

.hint-indicator {
  font-size: 10px;
  position: absolute;
  top: -8px;
  right: -5px;
  background-color: #2c3e50;
  color: white;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  font-weight: bold;
}

.sound-indicator {
  font-size: 10px;
  position: absolute;
  bottom: -8px;
  right: -5px;
  background-color: #ffd700;
  color: #2c3e50;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  font-weight: bold;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.pitch-warning {
  color: #e74c3c;
  font-size: 13px;
  margin-top: 5px;
  padding: 5px 8px;
  background-color: #ffebee;
  border-radius: 4px;
  border-left: 3px solid #e74c3c;
  display: block;
}

.text-match-warning {
  color: #e74c3c;
  font-size: 13px;
  margin-top: 5px;
  padding: 5px 8px;
  background-color: #ffebee;
  border-radius: 4px;
  border-left: 3px solid #e74c3c;
  display: block;
}

.text-match-warning ul {
  margin-top: 5px;
  margin-bottom: 0;
  padding-left: 20px;
}

.text-match-warning li {
  margin-bottom: 3px;
}

.formant-tip {
  font-size: 13px;
  color: #999;
  margin-top: 5px;
  padding: 5px 8px;
  background-color: #f8f9fa;
  border-radius: 4px;
  display: block;
}
</style>
