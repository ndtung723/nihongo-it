import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ref, nextTick } from "vue";
import { useDebounce } from "@/composables/useDebounce";

describe("useDebounce()", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("initializes with the source ref's current value", () => {
    const source = ref("initial");
    const debounced = useDebounce(source, 300);
    expect(debounced.value).toBe("initial");
  });

  it("does not update before the delay has passed", async () => {
    const source = ref("first");
    const debounced = useDebounce(source, 300);

    source.value = "second";
    await nextTick();
    vi.advanceTimersByTime(200);

    expect(debounced.value).toBe("first");
  });

  it("updates after the full delay", async () => {
    const source = ref("first");
    const debounced = useDebounce(source, 300);

    source.value = "second";
    await nextTick();
    vi.advanceTimersByTime(300);
    await nextTick();

    expect(debounced.value).toBe("second");
  });

  it("resets the timer on rapid successive changes (trailing debounce)", async () => {
    const source = ref("a");
    const debounced = useDebounce(source, 300);

    source.value = "b";
    await nextTick();
    vi.advanceTimersByTime(200);

    source.value = "c";
    await nextTick();
    vi.advanceTimersByTime(200);

    // Only 200ms since last change — should not have updated yet
    expect(debounced.value).toBe("a");

    vi.advanceTimersByTime(100);
    await nextTick();

    expect(debounced.value).toBe("c");
  });

  it("uses 300ms default delay when not specified", async () => {
    const source = ref("hello");
    const debounced = useDebounce(source); // default delay

    source.value = "world";
    await nextTick();
    vi.advanceTimersByTime(299);
    expect(debounced.value).toBe("hello");

    vi.advanceTimersByTime(1);
    await nextTick();
    expect(debounced.value).toBe("world");
  });

  it("works with numeric ref values", async () => {
    const source = ref(0);
    const debounced = useDebounce(source, 100);

    source.value = 42;
    await nextTick();
    vi.advanceTimersByTime(100);
    await nextTick();

    expect(debounced.value).toBe(42);
  });
});
