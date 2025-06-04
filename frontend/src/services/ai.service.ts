import axios from 'axios';
import type { AxiosResponse } from 'axios';
import api from '../utils/api';

// Định nghĩa types cho các tham số request
export type ContentType = 'vocabulary' | 'example' | 'conversation';

export interface TTSCheckResponse {
  exists: boolean;
  url?: string;
}

export interface SpeechAnalysisResult {
  score: number;
  feedback?: string;
  personalizedFeedback?: string;
  transcription?: string;
  intonationScore?: number;
  clarityScore?: number;
  textScore?: number;
  words?: WordAnalysis[];
}

export interface WordAnalysis {
  text: string;
  isCorrect: boolean;
  suggestion?: string;
}

export interface VocabularyExplanation {
  explanation: string;
  examples?: {
    japanese: string;
    vietnamese: string;
  }[];
}

export interface ChatResponse {
  message: string;
}

export interface VocabularyExplanationRequest {
  term: string;
  reading?: string;
  meaning?: string;
  partOfSpeech?: string;
  explanation?: string;
  exampleSentences?: string[];
  language?: string;
}

export interface AIExplanationResponse {
  content: string;
}

export interface FeedbackSummary {
  summary: string;
  common_errors: string[];
  improvement_tips: string[];
  attempts?: number;
  avg_score?: number;
  max_score?: number;
}

class AIService {
  private baseUrl: string;

  constructor() {
    this.baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  }

  /**
   * Kiểm tra xem đã có audio TTS được tạo trước đó chưa
   */
  async checkTTSExists(text: string, contentType: ContentType): Promise<TTSCheckResponse> {
    try {
      const response = await api.get('/ai-service-api/v1/tts/check', {
        params: {
          text,
          contentType
        }
      });
      return response.data;
    } catch (error) {
      return { exists: false };
    }
  }

  /**
   * Lấy audio đã được tạo trước đó
   */
  async getExistingTTSAudio(text: string, contentType: ContentType): Promise<Blob> {
    const response = await api.get('/ai-service-api/v1/tts/audio', {
      params: {
        text,
        contentType
      },
      responseType: 'blob'
    });
    return response.data;
  }

  /**
   * Tạo và phát audio TTS
   */
  async generateTTS(
    text: string,
    contentType: ContentType,
    speechSpeed: number = 1.0,
    saveAudio: boolean = true
  ): Promise<Blob> {
    // Kiểm tra xem audio đã tồn tại chưa
    try {
      const checkResult = await this.checkTTSExists(text, contentType);
      if (checkResult.exists) {
        return await this.getExistingTTSAudio(text, contentType);
      }
    } catch (error) {
    }

    // Tạo audio mới
    const response = await api.post('/ai-service-api/v1/tts/generate', text, {
      headers: {
        'Content-Type': 'text/plain; charset=UTF-8',
        'Accept-Language': 'ja-JP',
        'X-Speech-Speed': speechSpeed.toString(),
        'X-Content-Language': 'ja',
        'X-Content-Type': contentType,
        'X-Save-Audio': saveAudio.toString(),
        'Accept': 'audio/mpeg'
      },
      responseType: 'arraybuffer'
    });

    return new Blob([response.data], { type: 'audio/mpeg' });
  }

  /**
   * Phát audio từ Blob hoặc URL
   */
  async playAudio(audioData: Blob | string): Promise<void> {
    let audioUrl: string;

    if (typeof audioData === 'string') {
      audioUrl = audioData;
    } else {
      audioUrl = URL.createObjectURL(audioData);
    }

    const audio = new Audio(audioUrl);

    try {
      await audio.play();
      return new Promise((resolve) => {
        audio.onended = () => {
          if (typeof audioData !== 'string') {
            URL.revokeObjectURL(audioUrl);
          }
          resolve();
        };
      });
    } catch (error) {
      if (typeof audioData !== 'string') {
        URL.revokeObjectURL(audioUrl);
      }
      throw error;
    }
  }

  /**
   * Phân tích phát âm từ file audio
   */
  async analyzeSpeech(audioBlob: Blob, referenceText: string, type: string): Promise<SpeechAnalysisResult> {
    const formData = new FormData();
    formData.append('file', audioBlob, 'recording.mp3');
    formData.append('reference_text', referenceText);

    formData.append('type', type);

    const response = await api.post('/ai-service-api/v1/speech/analyze-audio-enhanced', formData);
    return response.data;
  }

  /**
   * Giải thích từ vựng bằng AI
   */
  async explainVocabulary(
    term: string,
    pronunciation?: string,
    meaning?: string,
    topicName?: string,
    example?: string
  ): Promise<VocabularyExplanation> {
    const response = await api.post('/ai-service-api/v1/chat/vocabulary/explain', null, {
      params: {
        term,
        pronunciation,
        meaning,
        topicName,
        example
      }
    });

    let data = response.data;
    // Xử lý dữ liệu trả về nếu là string
    if (typeof data === 'string') {
      try {
        const cleanedString = data
          .replace(/```json/g, '')
          .replace(/```/g, '')
          .trim();
        data = JSON.parse(cleanedString);
      } catch (error) {
        return {
          explanation: data,
          examples: []
        };
      }
    }

    return data;
  }

  /**
   * Chat với AI về một từ vựng
   */
  async chatAboutVocabulary(vocabWord: string, userMessage: string): Promise<ChatResponse> {
    const response = await api.post('/ai-service-api/v1/vocabulary/chat', null, {
      params: {
        vocabWord,
        userMessage
      }
    });

    let data = response.data;
    // Xử lý dữ liệu trả về nếu là string
    if (typeof data === 'string') {
      try {
        const cleanedString = data
          .replace(/```json/g, '')
          .replace(/```/g, '')
          .trim();
        data = JSON.parse(cleanedString);
      } catch (error) {
        return { message: data };
      }
    }

    return data;
  }

  /**
   * Giải thích từ vựng bằng AI với định dạng mới
   */
  async getVocabularyExplanation(request: VocabularyExplanationRequest): Promise<AIExplanationResponse> {
    try {
      // Ensure term parameter is explicitly in the params
      const params: Record<string, string> = {
        term: request.term,
        reading: request.reading || '',
        meaning: request.meaning || '',
        partOfSpeech: request.partOfSpeech || '',
        explanation: request.explanation || '',
        language: request.language || 'vi'
      };

      // Add example sentences if available
      if (request.exampleSentences && request.exampleSentences.length > 0) {
        params['exampleSentences'] = request.exampleSentences[0];
      }

      const response = await api.post('/ai-service-api/v1/chat/vocabulary/explain', null, {
        params
      });

      // Xử lý phản hồi
      let data = response.data;

      // Nếu dữ liệu trả về là string, chuyển thành object
      if (typeof data === 'string') {
        return { content: data };
      }

      // Nếu dữ liệu trả về là object, kiểm tra định dạng
      if (data && data.content) {
        return data;
      } else if (data && data.explanation) {
        // Hỗ trợ định dạng cũ
        return { content: data.explanation };
      }

      // Mặc định trả về nội dung
      return { content: JSON.stringify(data) };
    } catch (error) {
      throw error;
    }
  }

  /**
   * Get summary of feedback from multiple attempts
   */
  async getFeedbackSummary(feedbackList: SpeechAnalysisResult[], conversationText: string): Promise<FeedbackSummary> {
    try {
      const response = await api.post('/ai-service-api/v1/speech/summarize-feedback', {
        feedback_list: feedbackList,
        conversation_text: conversationText
      });

      return response.data;
    } catch (error) {
      console.error('Error getting feedback summary:', error);
      return {
        summary: 'Không thể tạo tóm tắt phản hồi.',
        common_errors: [],
        improvement_tips: []
      };
    }
  }
}

export default new AIService();
