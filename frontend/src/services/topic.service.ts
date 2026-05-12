import api from "@/utils/api";
import type {
  Topic,
  CreateTopicRequest,
  UpdateTopicRequest,
} from "@/types/learning.types";

const topicService = {
  getAllTopics: (): Promise<Topic[]> =>
    api.get("/api/v1/learning/topics").then((r) => r.data),

  getTopicById: (id: string): Promise<Topic> =>
    api.get(`/api/v1/learning/topics/${id}`).then((r) => r.data),

  getTopicsByCategoryId: (categoryId: string): Promise<Topic[]> =>
    api
      .get(`/api/v1/learning/topics/category/${categoryId}`)
      .then((r) => r.data),

  adminGetAllTopics: (): Promise<Topic[]> =>
    api.get("/api/v1/learning/admin/topics").then((r) => r.data),

  adminGetTopicById: (id: string): Promise<Topic> =>
    api.get(`/api/v1/learning/admin/topics/${id}`).then((r) => r.data),

  adminGetTopicsByCategoryId: (categoryId: string): Promise<Topic[]> =>
    api
      .get(`/api/v1/learning/admin/topics/category/${categoryId}`)
      .then((r) => r.data),

  adminCreateTopic: (data: CreateTopicRequest): Promise<Topic> =>
    api.post("/api/v1/learning/admin/topics", data).then((r) => r.data),

  adminUpdateTopic: (id: string, data: UpdateTopicRequest): Promise<Topic> =>
    api.put(`/api/v1/learning/admin/topics/${id}`, data).then((r) => r.data),

  adminDeleteTopic: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/admin/topics/${id}`).then(() => undefined),

  adminToggleTopicStatus: (id: string): Promise<Topic> =>
    api
      .patch(`/api/v1/learning/admin/topics/${id}/toggle-status`)
      .then((r) => r.data),

  adminSearchTopics: (categoryId: string, query: string): Promise<Topic[]> =>
    api
      .get("/api/v1/learning/admin/topics/search", {
        params: { categoryId, query },
      })
      .then((r) => r.data),
};

export default topicService;
