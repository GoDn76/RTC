import { fetchWithAuth } from './base';
import type { 
  AuthResponseDto, 
  ApiResponseDto, 
  UserProfileDto,
  OtpVerificationDto,
  ResetPasswordDto,
  EmailDto,
  GoogleLoginDto
} from '../types';

export const authApi = {
  login: (data: any): Promise<AuthResponseDto> => 
    fetchWithAuth('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
    
  register: (data: any): Promise<ApiResponseDto> =>
    fetchWithAuth('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  verifyEmail: (data: OtpVerificationDto): Promise<ApiResponseDto> =>
    fetchWithAuth('/api/auth/verify-email', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  requestPasswordReset: (data: EmailDto): Promise<ApiResponseDto> =>
    fetchWithAuth('/api/auth/request-password-reset', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  resetPassword: (data: ResetPasswordDto): Promise<ApiResponseDto> =>
    fetchWithAuth('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  loginWithGoogle: (data: GoogleLoginDto): Promise<AuthResponseDto> =>
    fetchWithAuth('/api/auth/login/google', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getMe: (): Promise<UserProfileDto> =>
    fetchWithAuth('/api/profile/me', {
      method: 'GET',
    }),

  searchUsers: (query: string): Promise<UserProfileDto[]> =>
    fetchWithAuth(`/api/profile/users?query=${encodeURIComponent(query)}`, {
      method: 'GET',
    }),
};
