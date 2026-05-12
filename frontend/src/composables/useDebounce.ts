import { ref, watch, type Ref } from "vue";

export function useDebounce<T>(source: Ref<T>, delay = 300): Ref<T> {
  const debounced = ref<T>(source.value) as Ref<T>;
  let timer: ReturnType<typeof setTimeout>;

  watch(source, (value) => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      debounced.value = value;
    }, delay);
  });

  return debounced;
}
