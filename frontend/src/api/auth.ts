import http from './http';
import { User, AuthResponse, LoginDto, RegisterDto, ResetPasswordDto } from '../types/auth';

const TOKEN_KEY = 'stocksense_auth_token';
const USER_KEY = 'stocksense_auth_user';

export const authApi = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  getCurrentUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  },

  saveSession(response: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
  },

  clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },

  async register(data: RegisterDto): Promise<AuthResponse> {
    const res = await http.post<AuthResponse>('/auth/register', data);
    this.saveSession(res.data);
    return res.data;
  },

  async login(data: LoginDto): Promise<AuthResponse> {
    const res = await http.post<AuthResponse>('/auth/login', data);
    this.saveSession(res.data);
    return res.data;
  },

  async logout(): Promise<void> {
    try {
      await http.post('/auth/logout');
    } finally {
      this.clearSession();
    }
  },

  async getProfile(): Promise<User> {
    const res = await http.get<User>('/auth/me');
    localStorage.setItem(USER_KEY, JSON.stringify(res.data));
    return res.data;
  },

  async updateProfile(fullName: string): Promise<User> {
    const res = await http.put<User>('/auth/profile', { fullName });
    localStorage.setItem(USER_KEY, JSON.stringify(res.data));
    return res.data;
  },

  async forgotPassword(email: string): Promise<{ message: string }> {
    const res = await http.post<{ message: string }>('/auth/forgot-password', { email });
    return res.data;
  },

  async verifyOtp(email: string, otp: string): Promise<{ message: string }> {
    const res = await http.post<{ message: string }>('/auth/verify-otp', { email, otp });
    return res.data;
  },

  async resetPassword(data: ResetPasswordDto): Promise<{ message: string }> {
    const res = await http.post<{ message: string }>('/auth/reset-password', data);
    return res.data;
  },
};
