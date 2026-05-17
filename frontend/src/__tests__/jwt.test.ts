import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  decodeToken,
  isTokenExpired,
  getStoredPayload,
  isAdmin,
} from "@/utils/jwt";

vi.mock("@/utils/tokenStore", () => ({ getAccessToken: vi.fn() }));
import { getAccessToken } from "@/utils/tokenStore";

// Minimal JWT header.payload.signature structure for testing
// payload: { sub, userId, role, email, fullName, exp, iat }
function makeToken(payload: object): string {
  const header = btoa(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = btoa(JSON.stringify(payload)).replace(/=/g, "");
  return `${header}.${body}.fakesig`;
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 7200; // 2h from now
const PAST_EXP = Math.floor(Date.now() / 1000) - 3600; // 1h ago

const validPayload = {
  sub: "test@example.com",
  userId: "uuid-1234",
  role: 2,
  email: "test@example.com",
  fullName: "Test User",
  exp: FUTURE_EXP,
  iat: Math.floor(Date.now() / 1000),
};

const adminPayload = { ...validPayload, role: 1 };

describe("decodeToken()", () => {
  it("decodes a valid JWT and returns payload", () => {
    const token = makeToken(validPayload);
    const result = decodeToken(token);
    expect(result).not.toBeNull();
    expect(result?.email).toBe("test@example.com");
    expect(result?.role).toBe(2);
  });

  it("returns null for malformed token", () => {
    expect(decodeToken("not.a.jwt")).toBeNull();
    expect(decodeToken("")).toBeNull();
  });
});

describe("isTokenExpired()", () => {
  it("returns false for token with future expiry", () => {
    const token = makeToken(validPayload);
    expect(isTokenExpired(token)).toBe(false);
  });

  it("returns true for token with past expiry", () => {
    const token = makeToken({ ...validPayload, exp: PAST_EXP });
    expect(isTokenExpired(token)).toBe(true);
  });

  it("returns true for malformed token", () => {
    expect(isTokenExpired("garbage")).toBe(true);
  });
});

describe("getStoredPayload()", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns null when no token in store", () => {
    vi.mocked(getAccessToken).mockReturnValue(null);
    expect(getStoredPayload()).toBeNull();
  });

  it("decodes and returns payload when token exists", () => {
    const token = makeToken(validPayload);
    vi.mocked(getAccessToken).mockReturnValue(token);
    const result = getStoredPayload();
    expect(result?.userId).toBe("uuid-1234");
  });
});

describe("isAdmin()", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns true when role is 1 (ADMIN)", () => {
    vi.mocked(getAccessToken).mockReturnValue(makeToken(adminPayload));
    expect(isAdmin()).toBe(true);
  });

  it("returns false when role is 2 (USER)", () => {
    vi.mocked(getAccessToken).mockReturnValue(makeToken(validPayload));
    expect(isAdmin()).toBe(false);
  });

  it("returns false when no token", () => {
    vi.mocked(getAccessToken).mockReturnValue(null);
    expect(isAdmin()).toBe(false);
  });
});
