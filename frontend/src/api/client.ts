import axios, { AxiosError, AxiosResponse } from 'axios';

export interface ApiErrorResponseData {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export class ApiError extends Error {
  status: number;
  errorTitle: string;
  validationErrors?: Record<string, string>;
  path?: string;

  constructor(
    status: number,
    message: string,
    errorTitle = 'Error',
    validationErrors?: Record<string, string>,
    path?: string
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorTitle = errorTitle;
    this.validationErrors = validationErrors;
    this.path = path;
  }
}

// Compute normalized base URL ensuring /api prefix
function getBaseUrl(): string {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (!envUrl) {
    return '/api';
  }
  const clean = envUrl.trim().replace(/\/+$/, '');
  return clean.endsWith('/api') ? clean : `${clean}/api`;
}

export const apiClient = axios.create({
  baseURL: getBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('stocksense_auth_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<ApiErrorResponseData>) => {
    // 1. Network / Connection errors
    if (!error.response) {
      const isConnectionRefused = error.message.includes('Network Error') || error.code === 'ERR_NETWORK';
      const userMsg = isConnectionRefused
        ? 'Cannot reach the StockSense backend service. Please verify that the server is running on http://localhost:8080.'
        : `Network request failed: ${error.message}`;

      return Promise.reject(new ApiError(0, userMsg, 'Network Error'));
    }

    const { status, data } = error.response;
    const backendMsg = data?.message;
    const validationErrors = data?.validationErrors;
    const errorTitle = data?.error || `HTTP ${status}`;
    const path = data?.path;

    // 2. HTTP 401 Unauthorized (Session expired or unauthenticated)
    if (status === 401) {
      localStorage.removeItem('stocksense_auth_token');
      localStorage.removeItem('stocksense_auth_user');
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      const message = backendMsg || 'Session expired or authentication invalid. Please log in again.';
      return Promise.reject(new ApiError(401, message, 'Unauthorized', undefined, path));
    }

    // 3. HTTP 403 Forbidden (Role access denied)
    if (status === 403) {
      const message = backendMsg || 'Access denied: Insufficient privileges for this action.';
      return Promise.reject(new ApiError(403, message, 'Forbidden', undefined, path));
    }

    // 4. HTTP 400 Validation / Bad Request
    if (status === 400) {
      let message = backendMsg || 'Invalid request parameters or payload.';
      if (validationErrors && Object.keys(validationErrors).length > 0) {
        const details = Object.entries(validationErrors)
          .map(([field, err]) => `${field}: ${err}`)
          .join(', ');
        message = `Validation failed: ${details}`;
      }
      return Promise.reject(new ApiError(400, message, 'Bad Request', validationErrors, path));
    }

    // 3. HTTP 404 Resource Not Found
    if (status === 404) {
      const message = backendMsg || 'The requested resource was not found.';
      return Promise.reject(new ApiError(404, message, 'Not Found', undefined, path));
    }

    // 4. HTTP 409 Conflict (e.g. duplicate SKU, insufficient stock, movement history blocking deletion)
    if (status === 409) {
      const message = backendMsg || 'Action conflict detected. Check current stock or product state.';
      return Promise.reject(new ApiError(409, message, 'Conflict', undefined, path));
    }

    // 5. HTTP 500 Internal Server Error
    if (status >= 500) {
      const message = 'An unexpected internal error occurred on the server. Please try again later.';
      return Promise.reject(new ApiError(status, message, 'Server Error', undefined, path));
    }

    // Generic fallback for other status codes
    const fallbackMessage = backendMsg || error.message || 'An error occurred while processing your request.';
    return Promise.reject(new ApiError(status, fallbackMessage, errorTitle, validationErrors, path));
  }
);

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
}

export default apiClient;
