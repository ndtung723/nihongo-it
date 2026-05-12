import { defineStore } from "pinia";
import { ref } from "vue";
import categoryService from "@/services/category.service";
import type { Category } from "@/types/learning.types";

export const useCategoriesStore = defineStore("categories", () => {
  const categories = ref<Category[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchCategories() {
    if (categories.value.length > 0) return;
    loading.value = true;
    error.value = null;
    try {
      categories.value = await categoryService.getAllCategories();
    } catch {
      error.value = "Không thể tải danh sách danh mục";
    } finally {
      loading.value = false;
    }
  }

  function invalidate() {
    categories.value = [];
  }

  return { categories, loading, error, fetchCategories, invalidate };
});
