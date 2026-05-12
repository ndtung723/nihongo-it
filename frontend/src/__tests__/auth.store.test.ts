import { describe, it, expect, vi, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useAuthStore } from "@/stores/modules/auth";

// Mock auth service
vi.mock("@/services/auth.service.ts", () => ({
  default: {
    login: vi.fn(),
    signup: vi.fn(),
    getCurrentUser: vi.fn(),
    logout: vi.fn(),
    getToken: vi.fn(),
    removeToken: vi.fn(),
    requestPasswordReset: vi.fn(),
    resetPassword: vi.fn(),
    changePassword: vi.fn(),
    loginWithGoogle: vi.fn(),
  },
}));

// Mock jwt utils
vi.mock("@/utils/jwt", () => ({
  isTokenExpired: vi.fn(),
}));

import authService from "@/services/auth.service.ts";
import { isTokenExpired } from "@/utils/jwt";

describe("auth store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe("isAuthenticated", () => {
    it("returns true when token exists and is not expired", () => {
      vi.mocked(authService.getToken).mockReturnValue("valid_token");
      vi.mocked(isTokenExpired).mockReturnValue(false);

      const store = useAuthStore();
      expect(store.isAuthenticated).toBe(true);
    });

    it("returns false when no token", () => {
      vi.mocked(authService.getToken).mockReturnValue(null);

      const store = useAuthStore();
      expect(store.isAuthenticated).toBe(false);
    });

    it("returns false when token is expired", () => {
      vi.mocked(authService.getToken).mockReturnValue("expired_token");
      vi.mocked(isTokenExpired).mockReturnValue(true);

      const store = useAuthStore();
      expect(store.isAuthenticated).toBe(false);
    });
  });

  describe("login()", () => {
    it("returns true and fetches user on success", async () => {
      vi.mocked(authService.login).mockResolvedValue({
        token: "jwt",
        refreshToken: "rt",
      });
      vi.mocked(authService.getCurrentUser).mockResolvedValue({
        status: { status: "OK" },
        userInfo: { userId: "1", email: "a@b.com", fullName: "A", roleId: 2 },
      });

      const store = useAuthStore();
      const result = await store.login({ email: "a@b.com", password: "pw" });

      expect(result).toBe(true);
      expect(store.user?.email).toBe("a@b.com");
    });

    it("returns false and sets error on failure", async () => {
      vi.mocked(authService.login).mockRejectedValue({
        response: { data: { message: "Invalid credentials" } },
      });

      const store = useAuthStore();
      const result = await store.login({ email: "a@b.com", password: "wrong" });

      expect(result).toBe(false);
      expect(store.error).toBe("Invalid credentials");
    });

    it("sets loading to false after login regardless of outcome", async () => {
      vi.mocked(authService.login).mockRejectedValue(new Error("network"));

      const store = useAuthStore();
      await store.login({ email: "x@x.com", password: "pw" });

      expect(store.loading).toBe(false);
    });
  });

  describe("initializeAuth()", () => {
    it("calls fetchCurrentUser when token is valid", () => {
      vi.mocked(authService.getToken).mockReturnValue("valid_token");
      vi.mocked(isTokenExpired).mockReturnValue(false);
      vi.mocked(authService.getCurrentUser).mockResolvedValue({
        status: { status: "OK" },
        userInfo: null,
      });

      const store = useAuthStore();
      store.initializeAuth();

      expect(authService.getCurrentUser).toHaveBeenCalled();
    });

    it("removes token when expired", () => {
      vi.mocked(authService.getToken).mockReturnValue("expired_token");
      vi.mocked(isTokenExpired).mockReturnValue(true);

      const store = useAuthStore();
      store.initializeAuth();

      expect(authService.removeToken).toHaveBeenCalled();
      expect(authService.getCurrentUser).not.toHaveBeenCalled();
    });

    it("does nothing when no token", () => {
      vi.mocked(authService.getToken).mockReturnValue(null);

      const store = useAuthStore();
      store.initializeAuth();

      expect(authService.getCurrentUser).not.toHaveBeenCalled();
      expect(authService.removeToken).not.toHaveBeenCalled();
    });
  });

  describe("logout()", () => {
    it("clears user state", async () => {
      vi.mocked(authService.logout).mockResolvedValue(undefined);
      vi.mocked(authService.getCurrentUser).mockResolvedValue({
        status: { status: "OK" },
        userInfo: { userId: "1", email: "a@b.com", fullName: "A", roleId: 2 },
      });

      const store = useAuthStore();
      store.user = { userId: "1", email: "a@b.com", fullName: "A", roleId: 2 };
      await store.logout();

      expect(store.user).toBeNull();
    });
  });
});
