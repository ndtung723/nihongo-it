import api from '../utils/api';

// Types
export interface VocabularyItem {
	vocabId: string;
	term: string;
	meaning: string;
	pronunciation?: string;
	example?: string;
	exampleMeaning?: string;
	audioPath?: string;
	jlptLevel: string;
	topicId?: string;
	topicName?: string;
	createdAt?: string;
	isSaved: boolean;

	// For chat interface
	aiExplanation?: string;
	aiExamples?: ExampleSentence[];
	chatHistory?: ChatMessage[];
}

export interface VocabularyFilter {
	keyword: string | null;
	jlptLevel: string | null;
	topicName: string | null;
	page: number;
	size: number;
	sort?: string | null;
}

export interface PagedResponse<T> {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
	lastPage: boolean;
}

export interface ExampleSentence {
	japanese: string;
	vietnamese: string;
}

export interface ChatMessage {
	role: string;
	content: string;
}

class VocabularyService {
	// Get vocabulary with filters
	async getVocabulary(filter: VocabularyFilter): Promise<PagedResponse<VocabularyItem>> {
		try {
			const response = await api.get('/api/v1/learning/vocabulary', {
				params: {
					keyword: filter.keyword || undefined,
					jlptLevel: filter.jlptLevel || undefined,
					topicName: filter.topicName || undefined,
					page: filter.page,
					size: filter.size,
					sort: filter.sort || undefined,
				},
			});
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	// Get vocabulary by ID
	async getVocabularyById(id: string): Promise<VocabularyItem> {
		try {
			const response = await api.get(`/api/v1/learning/vocabulary/${id}`);
			return response.data.data;
		} catch (error) {
			throw error;
		}
	}

	// Get vocabulary by term
	async getVocabularyByTerm(term: string): Promise<VocabularyItem> {
		try {
			const response = await api.get(`/api/v1/learning/vocabulary/term/${term}`);
			return response.data.data;
		} catch (error) {
			throw error;
		}
	}

	// Save vocabulary to user's notebook
	async saveVocabulary(id: string): Promise<VocabularyItem> {
		try {
			const response = await api.post(`/api/v1/learning/vocabulary/${id}/save`, {});
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	// Remove vocabulary from user's notebook
	async removeSavedVocabulary(id: string): Promise<VocabularyItem> {
		try {
			const response = await api.delete(`/api/v1/learning/vocabulary/${id}/save`);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	// Get saved vocabulary
	async getSavedVocabulary(filter: VocabularyFilter): Promise<PagedResponse<VocabularyItem>> {
		try {
			const response = await api.get('/api/v1/learning/vocabulary/saved', {
				params: {
					keyword: filter.keyword || undefined,
					page: filter.page,
					size: filter.size,
					sort: filter.sort || undefined,
				},
			});
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async getCategories(): Promise<any> {
		try {
			const response = await api.get('/api/v1/learning/vocabulary/categories');
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async getJlptLevels(): Promise<any> {
		try {
			const response = await api.get('/api/v1/learning/vocabulary/jlpt-levels');
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async getTopics(): Promise<any> {
		try {
			const response = await api.get('/api/v1/learning/topics');
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async getTopicsByCategory(categoryId: string): Promise<any> {
		try {
			const response = await api.get(`/api/v1/learning/vocabulary/categories/${categoryId}/topics`);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async createVocabulary(vocabulary: any): Promise<any> {
		try {
			const response = await api.post('/api/v1/learning/vocabulary', vocabulary);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async updateVocabulary(id: string, vocabulary: any): Promise<any> {
		try {
			const response = await api.put(`/api/v1/learning/vocabulary/${id}`, vocabulary);
			return response.data;
		} catch (error) {
			throw error;
		}
	}
}

export default new VocabularyService();
