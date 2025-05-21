import api from '../utils/api';

export interface FlashcardDTO {
  id: string;
  frontText: string;
  backText: string;
  vocabularyId?: string;
  due: string;
  reps: number;
  lapses: number;
  state: string;
  difficulty: number;
  stability: number;
  interval: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReviewResponse {
  result: {
    status: string;
    message: string;
  };
  data: FlashcardDTO;
}

class FlashcardService {
  // Get flashcards for a vocabulary item
  async getFlashcardsByVocabulary(vocabId: string): Promise<FlashcardDTO[]> {
    try {
      const response = await api.get(`/learning-service-api/v1/flashcards/vocabulary/${vocabId}`);

      if (response.data?.data) {
        return response.data.data;
      }

      return [];
    } catch (error) {
      console.error('Error fetching flashcards for vocabulary:', error);
      throw error;
    }
  }

  // Review a flashcard with a rating
  async reviewFlashcard(flashcardId: string, rating: number): Promise<ReviewResponse> {
    try {
      const response = await api.post(`/learning-service-api/v1/flashcards/${flashcardId}/review`, { rating });
      return response.data;
    } catch (error) {
      console.error('Error reviewing flashcard:', error);
      throw error;
    }
  }

  // Create a flashcard from a vocabulary item
  async createFlashcardFromVocabulary(vocabId: string): Promise<FlashcardDTO> {
    try {
      const response = await api.post(`/learning-service-api/v1/flashcards/vocabulary/${vocabId}`, {});

      if (response.data?.data) {
        return response.data.data;
      }

      throw new Error('Failed to create flashcard');
    } catch (error) {
      console.error('Error creating flashcard from vocabulary:', error);
      throw error;
    }
  }

  // Get due cards for studying
  async getDueCards(): Promise<FlashcardDTO[]> {
    try {
      const response = await api.get('/learning-service-api/v1/flashcards/due');

      if (response.data?.data) {
        return response.data.data;
      }

      return [];
    } catch (error) {
      console.error('Error fetching due cards:', error);
      throw error;
    }
  }

  // Get study statistics
  async getStudyStatistics(): Promise<any> {
    try {
      const response = await api.get('/learning-service-api/v1/flashcards/statistics');

      if (response.data?.data) {
        return response.data.data;
      }

      return {};
    } catch (error) {
      console.error('Error fetching study statistics:', error);
      throw error;
    }
  }
}

export default new FlashcardService();
