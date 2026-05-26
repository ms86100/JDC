import axios from 'axios';

// Build-time env var VITE_API_GATEWAY_URL is baked in by Docker
// In Docker: VITE_API_GATEWAY_URL=/api (nginx proxy handles it)
// In dev: falls back to '' (relative path, Vite proxy handles it)
const API_BASE = import.meta.env.VITE_API_GATEWAY_URL ?? '';

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  try {
    const stored = localStorage.getItem('user');
    if (stored) {
      const user = JSON.parse(stored) as { userId?: string };
      if (user.userId) {
        config.headers['X-User-Id'] = user.userId;
      }
    }
  } catch {
    /* ignore */
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken && !error.config._retry) {
        error.config._retry = true;
        try {
          const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, {
            refreshToken,
          });
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);
          error.config.headers.Authorization = `Bearer ${data.accessToken}`;
          return apiClient(error.config);
        } catch {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
