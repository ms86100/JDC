import axios from 'axios';

function normalizeApiBase(rawBase?: string): string {
  if (!rawBase || rawBase === '/' || rawBase === '') {
    return '';
  }
  return rawBase.replace(/\/$/, '');
}

const API_BASE = normalizeApiBase(
  import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_GATEWAY_URL
);

// Auth endpoints are always at /auth/* (relative path for proxy)
const AUTH_BASE = '/api';

let refreshToken: string | null = null;

const apiClient = axios.create({
  baseURL: '',  // Empty - use relative paths, proxy handles /api prefix in dev
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
  if (!config.headers['X-User-Id']) {
    const userId = localStorage.getItem('userId');
    if (userId) {
      config.headers['X-User-Id'] = userId;
    }
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const storedRefreshToken = localStorage.getItem('refreshToken');
      if (storedRefreshToken && !error.config._retry) {
        error.config._retry = true;

        if (!refreshToken) {
          refreshToken = axios
            .post(`${AUTH_BASE}/auth/refresh`, { refreshToken: storedRefreshToken })
            .then(({ data }) => {
              localStorage.setItem('accessToken', data.accessToken);
              localStorage.setItem('refreshToken', data.refreshToken);
              return data.accessToken;
            })
            .finally(() => {
              refreshToken = null;
            });
        }

        try {
          const newToken = await refreshToken;
          error.config.headers.Authorization = `Bearer ${newToken}`;
          return apiClient(error.config);
        } catch {
          refreshToken = null;
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