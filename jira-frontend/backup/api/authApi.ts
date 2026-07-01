import apiClient from './axiosClient';

export interface LoginRequest { username: string; password: string }
export interface RegisterRequest { username: string; email: string; password: string }
export interface AuthResponse {
  accessToken: string; refreshToken: string; tokenType: string;
  expiresIn: number; userId: string; username: string; email: string; roles: string[];
}
export interface UserDto { id: string; username: string; email: string; active: boolean; roles: string[] }

export const authApi = {
  login: (data: LoginRequest) => apiClient.post<AuthResponse>('/api/auth/login', data),
  register: (data: RegisterRequest) => apiClient.post<UserDto>('/api/auth/register', data),
  refresh: (refreshToken: string) => apiClient.post<AuthResponse>('/api/auth/refresh', { refreshToken }),
};
