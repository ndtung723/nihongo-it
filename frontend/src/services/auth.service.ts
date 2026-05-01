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
	token?: string;
	refreshToken?: string;
	message?: string;
}

export interface SignupResponse {
	message?: string;
}

export interface UserInfo {
	userId: string;
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
		const response = await api.post('/api/v1/user/auth/login', credentials);
		const result: LoginResponse = response.data;
		if (result.token) {
			this.saveToken(result.token);
			if (result.refreshToken) this.saveRefreshToken(result.refreshToken);
		}
		return result;
	}

	async signup(userData: SignupRequest): Promise<SignupResponse> {
		try {
			const response = await api.post('/api/v1/user/auth/signup', userData);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async getCurrentUser(): Promise<GetCurrentUserResponse> {
		const response = await api.get('/api/v1/user/auth/current');
		return response.data;
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

	getRefreshToken(): string | null {
		return localStorage.getItem('refresh_token');
	}

	saveRefreshToken(token: string): void {
		localStorage.setItem('refresh_token', token);
	}

	removeRefreshToken(): void {
		localStorage.removeItem('refresh_token');
	}

	isLoggedIn(): boolean {
		return !!this.getToken();
	}

	async logout(): Promise<void> {
		const refreshToken = this.getRefreshToken();
		if (refreshToken) {
			try {
				await api.post('/api/v1/user/auth/logout', { refreshToken });
			} catch {
				// Ignore errors — still clear local tokens
			}
		}
		this.removeToken();
		this.removeRefreshToken();
	}

	async loginWithGoogle(tokenId: string): Promise<LoginResponse> {
		const response = await api.post('/api/v1/user/auth/google-login', { tokenId });
		const result: LoginResponse = response.data;
		if (result.token) {
			this.saveToken(result.token);
			if (result.refreshToken) this.saveRefreshToken(result.refreshToken);
		}
		return result;
	}

	async requestPasswordReset(email: string): Promise<PasswordResetResponse> {
		try {
			const response = await api.post('/api/v1/user/auth/forgot-password', {
				email,
			} as PasswordResetRequest);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async resetPassword(token: string, password: string): Promise<PasswordResetResponse> {
		try {
			const response = await api.post('/api/v1/user/auth/set-new-password', {
				token,
				password,
				confirmPassword: password,
			});
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async changePassword(request: ChangePasswordRequest): Promise<ChangePasswordResponse> {
		try {
			const response = await api.post('/api/v1/user/auth/change-password', request);
			return response.data;
		} catch (error) {
			throw error;
		}
	}

	async updateProfile(request: UpdateProfileRequest): Promise<UpdateProfileResponse> {
		try {
			const response = await api.post('/api/v1/user/auth/update-profile', request);
			return response.data;
		} catch (error) {
			throw error;
		}
	}
}

export default new AuthService();
