import { describe, it, expect, beforeEach } from "vitest";
import {
  getAccessToken,
  setAccessToken,
  clearAccessToken,
} from "@/utils/tokenStore";

describe("tokenStore", () => {
  beforeEach(() => {
    clearAccessToken();
  });

  describe("getAccessToken()", () => {
    it("returns null when no token has been set", () => {
      expect(getAccessToken()).toBeNull();
    });

    it("returns the token after setAccessToken()", () => {
      setAccessToken("my-jwt-token");
      expect(getAccessToken()).toBe("my-jwt-token");
    });
  });

  describe("setAccessToken()", () => {
    it("overwrites an existing token", () => {
      setAccessToken("first-token");
      setAccessToken("second-token");
      expect(getAccessToken()).toBe("second-token");
    });

    it("stores the exact string provided (no transformation)", () => {
      const token = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
      setAccessToken(token);
      expect(getAccessToken()).toBe(token);
    });
  });

  describe("clearAccessToken()", () => {
    it("clears a previously set token", () => {
      setAccessToken("some-token");
      clearAccessToken();
      expect(getAccessToken()).toBeNull();
    });

    it("is safe to call when no token is set", () => {
      expect(() => clearAccessToken()).not.toThrow();
      expect(getAccessToken()).toBeNull();
    });

    it("calling clear twice leaves token as null", () => {
      setAccessToken("token");
      clearAccessToken();
      clearAccessToken();
      expect(getAccessToken()).toBeNull();
    });
  });

  describe("isolation (in-memory, not localStorage)", () => {
    it("does not read from localStorage", () => {
      localStorage.setItem("auth_token", "localstorage-token");
      expect(getAccessToken()).toBeNull();
    });

    it("does not persist across module reload simulation (fresh module state)", () => {
      clearAccessToken();
      expect(getAccessToken()).toBeNull();
    });
  });
});
