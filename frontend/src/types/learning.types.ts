import type { JlptLevel, DateString, UUID } from "./common.types";

export interface Category {
  categoryId: UUID;
  name: string;
  meaning?: string;
  displayOrder: number;
  topicCount?: number;
  isActive: boolean;
  createdAt?: DateString;
  updatedAt?: DateString;
}

export interface CreateCategoryRequest {
  name: string;
  meaning: string;
  description?: string;
  displayOrder?: number;
}
export interface UpdateCategoryRequest {
  name?: string;
  meaning?: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
}

export interface Topic {
  topicId: UUID;
  name: string;
  meaning?: string;
  displayOrder: number;
  categoryId: UUID;
  categoryName?: string;
  vocabularyCount?: number;
  isActive: boolean;
  createdAt?: DateString;
  updatedAt?: DateString;
}

export interface CreateTopicRequest {
  name: string;
  meaning: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
  categoryId: UUID;
}
export interface UpdateTopicRequest {
  name?: string;
  meaning?: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
  categoryId?: UUID;
}

export interface ExampleSentence {
  japanese: string;
  vietnamese: string;
}
export interface ChatMessage {
  role: string;
  content: string;
}

export interface VocabularyItem {
  vocabId: UUID;
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  jlptLevel: JlptLevel;
  topicId?: UUID;
  topicName?: string;
  createdAt?: DateString;
  isSaved: boolean;
  // AI chat fields (client-side only)
  aiExplanation?: string;
  aiExamples?: ExampleSentence[];
  chatHistory?: ChatMessage[];
}

export interface VocabularyFilter {
  keyword: string | null;
  jlptLevel: JlptLevel | null;
  topicName: string | null;
  page: number;
  size: number;
  sort?: string | null;
}

export interface CreateVocabularyRequest {
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  topicName: string;
  jlptLevel: JlptLevel;
}
export interface UpdateVocabularyRequest {
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  topicName?: string;
  jlptLevel?: JlptLevel;
}

export interface FlashcardDTO {
  id: UUID;
  frontText: string;
  backText: string;
  vocabularyId?: UUID;
  due: DateString;
  reps: number;
  lapses: number;
  state: string; // backend returns "new"/"learning"/"review"/"relearning" or numeric string
  difficulty: number;
  stability: number;
  interval: number;
  createdAt?: DateString;
  updatedAt?: DateString;
}

export interface FlashcardStats {
  totalCards: number;
  dueToday: number;
  newCards: number;
  learningCards: number;
  masteredCards: number;
  averageRetention?: number;
}

export interface ReviewResponse {
  result: { status: string; message: string };
  data: FlashcardDTO;
}
