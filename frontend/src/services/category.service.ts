import api from "@/utils/api";
import type {
  Category,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from "@/types/learning.types";

const categoryService = {
  getAllCategories: (): Promise<Category[]> =>
    api.get("/api/v1/learning/categories").then((r) => r.data),

  getCategoryById: (id: string): Promise<Category> =>
    api.get(`/api/v1/learning/categories/${id}`).then((r) => r.data),

  searchCategories: (query: string): Promise<Category[]> =>
    api
      .get("/api/v1/learning/categories/search", { params: { query } })
      .then((r) => r.data),

  adminGetAllCategories: (): Promise<Category[]> =>
    api.get("/api/v1/learning/admin/categories").then((r) => r.data),

  adminGetCategoryById: (id: string): Promise<Category> =>
    api.get(`/api/v1/learning/admin/categories/${id}`).then((r) => r.data),

  adminCreateCategory: (data: CreateCategoryRequest): Promise<Category> =>
    api.post("/api/v1/learning/admin/categories", data).then((r) => r.data),

  adminUpdateCategory: (
    id: string,
    data: UpdateCategoryRequest,
  ): Promise<Category> =>
    api
      .put(`/api/v1/learning/admin/categories/${id}`, data)
      .then((r) => r.data),

  adminDeleteCategory: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/admin/categories/${id}`).then(() => undefined),

  adminToggleCategoryStatus: (id: string): Promise<Category> =>
    api
      .patch(`/api/v1/learning/admin/categories/${id}/toggle-status`)
      .then((r) => r.data),

  adminSearchCategories: (
    nameQuery?: string,
    meaningQuery?: string,
  ): Promise<Category[]> => {
    const params = new URLSearchParams();
    if (nameQuery) params.append("query", nameQuery);
    if (meaningQuery) params.append("meaningQuery", meaningQuery);
    return api
      .get(`/api/v1/learning/admin/categories/search?${params}`)
      .then((r) => r.data);
  },
};

export default categoryService;
