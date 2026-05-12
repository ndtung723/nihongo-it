import api from "../utils/api";
import type { AxiosResponse } from "axios";

export interface ConversationLine {
  lineId?: string;
  speaker: string;
  japaneseText: string;
  vietnameseTranslation?: string;
  notes?: string;
  importantVocab?: string;
  orderIndex: number;
  tempId?: string; // Temp ID for drag and drop functionality
}

export interface Conversation {
  conversationId?: string;
  title: string;
  description?: string;
  jlptLevel?: string;
  unit?: number;
  lines?: ConversationLine[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  size: number;
}

const conversationService = {
  // Public API endpoints
  getConversations(
    page: number = 0,
    size: number = 10,
    search: string = "",
    sortBy: string = "title",
    sortDir: string = "asc",
  ): Promise<AxiosResponse<PagedResponse<Conversation>>> {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());
    params.append("sortBy", sortBy);
    params.append("sortDir", sortDir);

    if (search && search.trim() !== "") {
      params.append("search", search);
    }

    return api.get(`/api/v1/learning/conversations?${params.toString()}`);
  },

  getConversationById(id: string): Promise<AxiosResponse<Conversation>> {
    return api.get(`/api/v1/learning/conversations/${id}`);
  },

  getConversationsByJlptLevel(
    level: string,
    page: number = 0,
    size: number = 10,
  ): Promise<AxiosResponse<PagedResponse<Conversation>>> {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());

    return api.get(
      `/api/v1/learning/conversations/jlpt/${level}?${params.toString()}`,
    );
  },

  // Admin API endpoints
  adminGetConversations(
    page: number = 0,
    size: number = 10,
    search: string = "",
    sortBy: string = "title",
    sortDir: string = "asc",
  ): Promise<AxiosResponse<PagedResponse<Conversation>>> {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());
    params.append("sortBy", sortBy);
    params.append("sortDir", sortDir);

    if (search && search.trim() !== "") {
      params.append("search", search);
    }

    return api.get(`/api/v1/learning/admin/conversations?${params.toString()}`);
  },

  adminGetConversationById(id: string): Promise<AxiosResponse<Conversation>> {
    return api.get(`/api/v1/learning/admin/conversations/${id}`);
  },

  adminGetConversationsByJlptLevel(
    level: string,
    page: number = 0,
    size: number = 10,
  ): Promise<AxiosResponse<PagedResponse<Conversation>>> {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());

    return api.get(
      `/api/v1/learning/admin/conversations/jlpt/${level}?${params.toString()}`,
    );
  },

  adminCreateConversation(
    conversation: Conversation,
  ): Promise<AxiosResponse<Conversation>> {
    return api.post("/api/v1/learning/admin/conversations", conversation);
  },

  adminUpdateConversation(
    id: string,
    conversation: Conversation,
  ): Promise<AxiosResponse<Conversation>> {
    return api.put(`/api/v1/learning/admin/conversations/${id}`, conversation);
  },

  adminDeleteConversation(id: string): Promise<AxiosResponse<unknown>> {
    return api.delete(`/api/v1/learning/admin/conversations/${id}`);
  },

  // Utility functions
  getJlptLevels() {
    return [
      { text: "N5 - Sơ cấp", value: "N5" },
      { text: "N4 - Sơ trung cấp", value: "N4" },
      { text: "N3 - Trung cấp", value: "N3" },
      { text: "N2 - Nâng cao", value: "N2" },
      { text: "N1 - Cao cấp", value: "N1" },
    ];
  },

  getSpeakerOptions() {
    return [
      { text: "Nihongo IT", value: "bot" },
      { text: "Người dùng", value: "user" },
    ];
  },

  // Lưu hội thoại vào danh sách của người dùng
  saveConversation(conversationId: string): Promise<AxiosResponse<unknown>> {
    return api.post(`/api/v1/user/user/saved-conversations/${conversationId}`);
  },

  // Bỏ lưu hội thoại
  unsaveConversation(conversationId: string): Promise<AxiosResponse<unknown>> {
    return api.delete(
      `/api/v1/user/user/saved-conversations/${conversationId}`,
    );
  },

  // Kiểm tra hội thoại đã được lưu hay chưa
  checkSavedConversation(
    conversationId: string,
  ): Promise<AxiosResponse<{ saved: boolean }>> {
    return api.get(
      `/api/v1/user/user/saved-conversations/check/${conversationId}`,
    );
  },

  // Lấy danh sách hội thoại đã lưu
  getSavedConversations(
    page: number = 0,
    size: number = 10,
  ): Promise<AxiosResponse<PagedResponse<Conversation>>> {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());

    return api.get(
      `/api/v1/user/user/saved-conversations?${params.toString()}`,
    );
  },
};

export default conversationService;
