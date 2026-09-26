export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'WORKER' | 'MANAGER' | string;
  requestedRole?: string;
  approvalStatus?: string;
  emailVerified?: boolean;
  enabled?: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface RegisterDto {
  email: string;
  password: string;
  fullName: string;
  requestedRole?: 'WORKER' | 'MANAGER';
}

export interface RegisterResponse {
  message: string;
  email: string;
  role: string;
  requestedRole: string;
  approvalStatus: string;
  requiresEmailVerification: boolean;
}

export interface LoginDto {
  email: string;
  password: string;
}

export interface VerifyOtpDto {
  email: string;
  otp: string;
}

export interface ResetPasswordDto {
  email: string;
  otp: string;
  newPassword: string;
}
