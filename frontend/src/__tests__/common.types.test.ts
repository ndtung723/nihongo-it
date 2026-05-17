import { describe, it, expect } from "vitest";
import { extractApiError } from "@/types/common.types";

describe("extractApiError()", () => {
  it("returns message from response.data.message", () => {
    const err = { response: { data: { message: "Không tìm thấy từ vựng" } } };
    expect(extractApiError(err)).toBe("Không tìm thấy từ vựng");
  });

  it("returns fallback when response is missing", () => {
    expect(extractApiError({})).toBe("Đã xảy ra lỗi");
    expect(extractApiError(null)).toBe("Đã xảy ra lỗi");
    expect(extractApiError(undefined)).toBe("Đã xảy ra lỗi");
  });

  it("returns fallback when response.data is missing", () => {
    const err = { response: {} };
    expect(extractApiError(err)).toBe("Đã xảy ra lỗi");
  });

  it("returns fallback when response.data.message is missing", () => {
    const err = { response: { data: {} } };
    expect(extractApiError(err)).toBe("Đã xảy ra lỗi");
  });

  it("returns fallback when response.data.message is null", () => {
    const err = { response: { data: { message: null } } };
    expect(extractApiError(err)).toBe("Đã xảy ra lỗi");
  });

  it("respects custom fallback parameter", () => {
    const err = {};
    expect(extractApiError(err, "Custom fallback")).toBe("Custom fallback");
  });

  it("returns message over custom fallback when message present", () => {
    const err = { response: { data: { message: "Server error" } } };
    expect(extractApiError(err, "Custom fallback")).toBe("Server error");
  });

  it("handles Error objects (no response property)", () => {
    const err = new Error("network error");
    expect(extractApiError(err)).toBe("Đã xảy ra lỗi");
  });

  it("handles string errors", () => {
    expect(extractApiError("raw string error")).toBe("Đã xảy ra lỗi");
  });
});
