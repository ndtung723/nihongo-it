export type JlptLevel = "N1" | "N2" | "N3" | "N4" | "N5";
export type DateString = string; // ISO 8601: "2025-05-11T10:30:00"
export type UUID = string;

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number; // 0-based
  size: number;
  lastPage?: boolean;
}

export function extractApiError(
  err: unknown,
  fallback = "Đã xảy ra lỗi",
): string {
  const e = err as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}
