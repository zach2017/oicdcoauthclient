import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosError } from 'axios';

/**
 * API Service for BFF (Backend-for-Frontend) Pattern
 * Uses session cookies for authentication instead of Bearer tokens
 * Automatically handles CSRF tokens for state-changing requests
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Create axios instance with credentials enabled
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
  withCredentials: true,  // Send session cookies with every request
});

// CSRF token management
let csrfToken: string | null = null;

/**
 * Fetch CSRF token from backend
 */
const fetchCsrfToken = async (): Promise<string> => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/auth/csrf`, {
      withCredentials: true
    });
    return response.data.token;
  } catch (error) {
    console.error('Failed to fetch CSRF token:', error);
    throw error;
  }
};

// Request interceptor - add CSRF token for state-changing requests
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const method = config.method?.toUpperCase();

    // For state-changing methods, include CSRF token
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method || '')) {
      if (!csrfToken) {
        // Fetch CSRF token if not available
        try {
          csrfToken = await fetchCsrfToken();
        } catch (error) {
          console.error('Failed to get CSRF token, continuing without it');
        }
      }

      if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
      }
    }

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle errors and refresh CSRF token on 403
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      console.error('Unauthorized - session expired or not authenticated');
      // Redirect to login if not authenticated
      window.location.href = '/';
    }

    if (error.response?.status === 403) {
      console.error('Forbidden - either insufficient permissions or invalid CSRF token');
      // Clear CSRF token to force refresh on next request
      csrfToken = null;
    }

    return Promise.reject(error);
  }
);

// API Response types
export interface HelloResponse {
  message: string;
  user: string;
  timestamp: string;
  roles: string[];
}

export interface UserInfoResponse {
  principal: string;
  authorities: string[];
  userInfo: {
    sub: string;
    preferred_username: string;
    email: string;
    email_verified: boolean;
    name: string;
    given_name: string;
    family_name: string;
    groups: string[];
  };
  sessionInfo: {
    authenticationType: string;
    provider: string;
    timestamp: string;
  };
  allAttributes: Record<string, unknown>;
}

export interface HealthResponse {
  status: string;
  timestamp?: string;
  service?: string;
}

export interface AdminActionResponse {
  status: string;
  message: string;
  performedBy: string;
  timestamp: string;
  receivedPayload?: Record<string, unknown>;
}

/**
 * API Methods
 */
export const api = {
  // Public endpoints (no auth required)
  getHealth: async (): Promise<HealthResponse> => {
    const response = await apiClient.get('/api/public/health');
    return response.data;
  },

  getInfo: async () => {
    const response = await apiClient.get('/api/public/info');
    return response.data;
  },

  // Protected endpoints (require ADMIN role)
  getHello: async (): Promise<HelloResponse> => {
    const response = await apiClient.get('/api/hello');
    return response.data;
  },

  getHelloMe: async (): Promise<HelloResponse> => {
    const response = await apiClient.get('/api/hello/me');
    return response.data;
  },

  getUserInfo: async (): Promise<UserInfoResponse> => {
    const response = await apiClient.get('/api/hello/userinfo');
    return response.data;
  },

  performAdminAction: async (payload?: Record<string, unknown>): Promise<AdminActionResponse> => {
    const response = await apiClient.post('/api/hello/action', payload || {});
    return response.data;
  },
};

export default apiClient;
