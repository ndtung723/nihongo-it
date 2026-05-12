import { ref, computed } from "vue";

export function usePagination(defaultPageSize = 10) {
  const page = ref(1);
  const pageSize = ref(defaultPageSize);
  const total = ref(0);

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value));
  const hasNextPage = computed(() => page.value < totalPages.value);
  const hasPrevPage = computed(() => page.value > 1);

  function reset() {
    page.value = 1;
  }

  return { page, pageSize, total, totalPages, hasNextPage, hasPrevPage, reset };
}
