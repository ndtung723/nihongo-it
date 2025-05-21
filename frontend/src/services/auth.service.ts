import api from '../utils/api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  fullName: string;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
}

export interface LoginResponse {
  result: 'OK' | 'NG';
  token?: string;
  message?: string;
}

export interface SignupResponse {
  status: 'OK' | 'NG';
}

export interface UserInfo {
  userId: number;
  email: string;
  fullName: string;
  roleId: number;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
  lastLogin?: string;
}

export interface GetCurrentUserResponse {
  status: 'OK' | 'NG';
  userInfo?: UserInfo;
}

export interface GoogleLoginRequest {
  tokenId: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetResponse {
  status: 'OK' | 'NG';
  message?: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ChangePasswordResponse {
  status: 'OK' | 'NG';
  message?: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  currentLevel: string;
  jlptGoal: string;
}

export interface UpdateProfileResponse {
  status: 'OK' | 'NG';
  message?: string;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/login', credentials);
      const result = response.data;

      if (result.result === 'OK' && result.token) {
        this.saveToken(result.token);
      }

      return result;
    } catch (error) {
      console.error('Login error:', error);
      return { result: 'NG' };
    }
  }

  async signup(userData: SignupRequest): Promise<SignupResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/signup', userData);
      return response.data;
    } catch (error) {
      console.error('Signup error:', error);
      throw error;
    }
  }

  async getCurrentUser(): Promise<GetCurrentUserResponse> {
    try {
      const response = await api.get('/user-service-api/v1/auth/current');
      return response.data;
    } catch (error) {
      console.error('Get current user error:', error);
      return { status: 'NG' };
    }
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  saveToken(token: string): void {
    localStorage.setItem('auth_token', token);
  }

  removeToken(): void {
    localStorage.removeItem('auth_token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  async isTokenValid(): Promise<boolean> {
    const token = this.getToken();
    if (!token) return false;

    try {
      // Use the getCurrentUser endpoint to verify token validity
      // This endpoint should already exist in your backend
      const response = await api.get('/user-service-api/v1/auth/current', {
        timeout: 3000 // Set timeout to detect server unavailability quickly
      });

      // If we get a successful response with user data, the token is valid
      return response.data && response.data.status === 'OK';
    } catch (error) {
      console.error('Token validation error:', error);
      // If server is down or token is invalid, return false
      return false;
    }
  }

  async loginWithGoogle(tokenId: string): Promise<LoginResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/google-login', { tokenId })
      const result = response.data

      if (result.result === 'OK' && result.token) {
        this.saveToken(result.token)
      }

      return result
    } catch (error) {
      console.error('Google login error:', error)
      return { result: 'NG' }
    }
  }

  async requestPasswordReset(email: string): Promise<PasswordResetResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/forgot-password', { email } as PasswordResetRequest);
      return response.data;
    } catch (error) {
      console.error('Password reset request failed:', error);
      throw error;
    }
  }

  async resetPassword(token: string, password: string): Promise<PasswordResetResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/set-new-password', {
        token,
        password,
        confirmPassword: password
      });
      return response.data;
    } catch (error) {
      console.error('Password reset error:', error);
      throw error;
    }
  }

  async changePassword(request: ChangePasswordRequest): Promise<ChangePasswordResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/change-password', request);
      return response.data;
    } catch (error) {
      console.error('Password change error:', error);
      throw error;
    }
  }

  async updateProfile(request: UpdateProfileRequest): Promise<UpdateProfileResponse> {
    try {
      const response = await api.post('/user-service-api/v1/auth/update-profile', request);
      return response.data;
    } catch (error) {
      console.error('Profile update error:', error);
      throw error;
    }
  }
}

export default new AuthService();
