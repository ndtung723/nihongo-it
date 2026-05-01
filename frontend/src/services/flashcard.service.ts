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
			const response = await api.get(`/api/v1/learning/flashcards/vocabulary/${vocabId}`);

			if (response.data?.data) {
				return response.data.data;
			}

			return [];
		} catch (error) {
			throw error;
		}
	}

	// Review a flashcard with a rating
	async reviewFlashcard(flashcardId: string, rating: number): Promise<ReviewResponse> {
		try {
			const response = await api.post(`/api/v1/learning/flashcards/${flashcardId}/review`, {
				rating,
			});
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	// Create a flashcard from a vocabulary item
	async createFlashcardFromVocabulary(vocabId: string): Promise<FlashcardDTO> {
		try {
			const response = await api.post(`/api/v1/learning/flashcards/vocabulary/${vocabId}`, {});

			if (response.data?.data) {
				return response.data.data;
			}

			throw new Error('Failed to create flashcard');
		} catch (error) {
			throw error;
		}
	}

	// Get due cards for studying
	async getDueCards(): Promise<FlashcardDTO[]> {
		try {
			const response = await api.get('/api/v1/learning/flashcards/due');

			if (response.data?.data) {
				return response.data.data;
			}

			return [];
		} catch (error) {
			throw error;
		}
	}

	// Get study statistics
	async getStudyStatistics(): Promise<any> {
		try {
			const response = await api.get('/api/v1/learning/flashcards/statistics');

			if (response.data?.data) {
				return response.data.data;
			}

			return {};
		} catch (error) {
			throw error;
		}
	}
}

export default new FlashcardService();
