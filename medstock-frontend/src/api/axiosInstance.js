import axios from 'axios';
import { clearTokens, getAccessToken, setTokens } from './authTokenStore';
import { API_BASE_URL } from '../config/env';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

let onUnauthorized = null;

function shouldSkipRefresh(config) {
  const url = config?.url || '';
  return [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/oauth2/exchange',
  ].some((path) => url.includes(path));
}

export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

axiosInstance.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    if (status === 401 && !originalRequest?._retry && !shouldSkipRefresh(originalRequest)) {
      originalRequest._retry = true;

      try {
        const refreshResponse = await axios.post(
          `${API_BASE_URL}/api/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const nextAccessToken = refreshResponse.data?.accessToken;
        setTokens(nextAccessToken);

        originalRequest.headers.Authorization = `Bearer ${nextAccessToken}`;
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        clearTokens();
        if (onUnauthorized) {
          onUnauthorized();
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
