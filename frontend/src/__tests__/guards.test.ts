import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  requireAuth,
  requireAdmin,
  redirectIfAuthenticated,
} from "@/router/guards";
import type { NavigationGuardNext, RouteLocationNormalized } from "vue-router";

// Module-level mocks
vi.mock("@/utils/tokenStore", () => ({
  getAccessToken: vi.fn(),
  clearAccessToken: vi.fn(),
}));
vi.mock("@/utils/jwt", () => ({
  isTokenExpired: vi.fn(),
  decodeToken: vi.fn(),
}));
vi.mock("@/types/roles", () => ({
  ROLES: { ADMIN: 1, USER: 2 },
}));

import { getAccessToken, clearAccessToken } from "@/utils/tokenStore";
import { isTokenExpired, decodeToken } from "@/utils/jwt";

function makeTo(fullPath = "/protected"): RouteLocationNormalized {
  return { fullPath } as RouteLocationNormalized;
}

function makeFrom(): RouteLocationNormalized {
  return {} as RouteLocationNormalized;
}

describe("requireAuth()", () => {
  let next: NavigationGuardNext;

  beforeEach(() => {
    next = vi.fn() as unknown as NavigationGuardNext;
    vi.clearAllMocks();
  });

  it("no token → redirects to login with redirect query", () => {
    vi.mocked(getAccessToken).mockReturnValue(null);

    requireAuth(makeTo("/dashboard"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith({
      name: "login",
      query: { redirect: "/dashboard" },
    });
  });

  it("expired token → clears token and redirects to login", () => {
    vi.mocked(getAccessToken).mockReturnValue("expired_token");
    vi.mocked(isTokenExpired).mockReturnValue(true);

    requireAuth(makeTo("/dashboard"), makeFrom(), next);

    expect(clearAccessToken).toHaveBeenCalled();
    expect(next).toHaveBeenCalledWith({
      name: "login",
      query: { redirect: "/dashboard" },
    });
  });

  it("valid token → calls next() without arguments", () => {
    vi.mocked(getAccessToken).mockReturnValue("valid_token");
    vi.mocked(isTokenExpired).mockReturnValue(false);

    requireAuth(makeTo(), makeFrom(), next);

    expect(next).toHaveBeenCalledWith();
    expect(clearAccessToken).not.toHaveBeenCalled();
  });
});

describe("requireAdmin()", () => {
  let next: NavigationGuardNext;

  beforeEach(() => {
    next = vi.fn() as unknown as NavigationGuardNext;
    vi.clearAllMocks();
  });

  it("no token → redirects to login", () => {
    vi.mocked(getAccessToken).mockReturnValue(null);

    requireAdmin(makeTo("/admin"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith({
      name: "login",
      query: { redirect: "/admin" },
    });
  });

  it("valid admin token (role=1) → calls next()", () => {
    vi.mocked(getAccessToken).mockReturnValue("admin_token");
    vi.mocked(isTokenExpired).mockReturnValue(false);
    vi.mocked(decodeToken).mockReturnValue({ role: 1 } as any);

    requireAdmin(makeTo("/admin"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith();
  });

  it("valid user token (role=2) → redirects to home (not login)", () => {
    vi.mocked(getAccessToken).mockReturnValue("user_token");
    vi.mocked(isTokenExpired).mockReturnValue(false);
    vi.mocked(decodeToken).mockReturnValue({ role: 2 } as any);

    requireAdmin(makeTo("/admin"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith({ name: "home" });
  });

  it("token decodes to null → redirects to home", () => {
    vi.mocked(getAccessToken).mockReturnValue("bad_token");
    vi.mocked(isTokenExpired).mockReturnValue(false);
    vi.mocked(decodeToken).mockReturnValue(null);

    requireAdmin(makeTo("/admin"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith({ name: "home" });
  });

  it("expired admin token → clears token and redirects to login", () => {
    vi.mocked(getAccessToken).mockReturnValue("expired_admin");
    vi.mocked(isTokenExpired).mockReturnValue(true);

    requireAdmin(makeTo("/admin"), makeFrom(), next);

    expect(clearAccessToken).toHaveBeenCalled();
    expect(next).toHaveBeenCalledWith({
      name: "login",
      query: { redirect: "/admin" },
    });
  });
});

describe("redirectIfAuthenticated()", () => {
  let next: NavigationGuardNext;

  beforeEach(() => {
    next = vi.fn() as unknown as NavigationGuardNext;
    vi.clearAllMocks();
  });

  it("valid token → redirects to home", () => {
    vi.mocked(getAccessToken).mockReturnValue("valid_token");
    vi.mocked(isTokenExpired).mockReturnValue(false);

    redirectIfAuthenticated(makeTo("/login"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith({ name: "home" });
  });

  it("no token → calls next() (allows access to login page)", () => {
    vi.mocked(getAccessToken).mockReturnValue(null);

    redirectIfAuthenticated(makeTo("/login"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith();
  });

  it("expired token → calls next() (allows access to login page)", () => {
    vi.mocked(getAccessToken).mockReturnValue("expired_token");
    vi.mocked(isTokenExpired).mockReturnValue(true);

    redirectIfAuthenticated(makeTo("/login"), makeFrom(), next);

    expect(next).toHaveBeenCalledWith();
  });
});
