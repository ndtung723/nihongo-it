import api from "../utils/api";
import {
  getAccessToken,
  setAccessToken,
  clearAccessToken,
} from "../utils/tokenStore";
import type {
  UserInfo,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  GetCurrentUserResponse,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UpdateProfileResponse,
} from "@/types/user.types";

// Re-export for backward compatibility
export type {
  UserInfo,
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  GetCurrentUserResponse,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UpdateProfileResponse,
};

export interface GoogleLoginRequest {
  tokenId: string;
}
export interface PasswordResetRequest {
  email: string;
}
export interface PasswordResetResponse {
  message?: string;
}
export interface ResetPasswordRequest {
  token: string;
  password: string;
}
export interface ChangePasswordResponse {
  message?: string;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post("/api/v1/user/auth/login", credentials);
    const result: LoginResponse = response.data;
    if (result.token) {
      this.saveToken(result.token);
      // refresh_token is stored in httpOnly cookie by the server — not exposed in response body
    }
    return result;
  }

  async signup(userData: SignupRequest): Promise<SignupResponse> {
    const response = await api.post("/api/v1/user/auth/signup", userData);
    return response.data;
  }

  async getCurrentUser(): Promise<GetCurrentUserResponse> {
    const response = await api.get("/api/v1/user/auth/current");
    return response.data;
  }

  getToken(): string | null {
    return getAccessToken();
  }

  saveToken(token: string): void {
    setAccessToken(token);
  }

  removeToken(): void {
    clearAccessToken();
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  async logout(): Promise<void> {
    try {
      // Cookie is sent automatically; server clears it via Set-Cookie: refresh_token=; Max-Age=0
      await api.post("/api/v1/user/auth/logout", {});
    } catch {
      // Ignore errors — still clear local state
    }
    this.removeToken();
  }

  async logoutAll(): Promise<void> {
    try {
      await api.post("/api/v1/user/auth/logout-all");
    } catch {
      // Ignore errors — still clear local state
    }
    this.removeToken();
  }

  async loginWithGoogle(tokenId: string): Promise<LoginResponse> {
    const response = await api.post("/api/v1/user/auth/google-login", {
      tokenId,
    });
    const result: LoginResponse = response.data;
    if (result.token) {
      this.saveToken(result.token);
    }
    return result;
  }

  async requestPasswordReset(email: string): Promise<PasswordResetResponse> {
    const response = await api.post("/api/v1/user/auth/forgot-password", {
      email,
    } as PasswordResetRequest);
    return response.data;
  }

  async resetPassword(
    token: string,
    password: string,
  ): Promise<PasswordResetResponse> {
    const response = await api.post("/api/v1/user/auth/set-new-password", {
      token,
      password,
      confirmPassword: password,
    });
    return response.data;
  }

  async changePassword(
    request: ChangePasswordRequest,
  ): Promise<ChangePasswordResponse> {
    const response = await api.post(
      "/api/v1/user/auth/change-password",
      request,
    );
    return response.data;
  }

  async updateProfile(
    request: UpdateProfileRequest,
  ): Promise<UpdateProfileResponse> {
    const response = await api.post(
      "/api/v1/user/auth/update-profile",
      request,
    );
    return response.data;
  }
}

export default new AuthService();
