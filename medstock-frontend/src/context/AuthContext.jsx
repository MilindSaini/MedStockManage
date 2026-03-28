import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import axiosInstance, { registerUnauthorizedHandler } from '../api/axiosInstance';
import { clearTokens, setTokens } from '../api/authTokenStore';

const AuthContext = createContext(null);
const ACTIVE_ROLE_STORAGE_KEY = 'medstock.activeRole';
const HAS_SESSION_STORAGE_KEY = 'medstock.hasSession';
const ACCESS_TOKEN_STORAGE_KEY = 'medstock.accessToken';
const USER_STORAGE_KEY = 'medstock.user';

function readSessionValue(key) {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.sessionStorage.getItem(key);
}

function readStoredUser() {
  const rawUser = readSessionValue(USER_STORAGE_KEY);
  if (!rawUser) {
    return null;
  }

  try {
    return JSON.parse(rawUser);
  } catch {
    return null;
  }
}

function normalizeRoles(user) {
  if (Array.isArray(user?.roles) && user.roles.length > 0) {
    return user.roles;
  }
  if (user?.role) {
    return [user.role];
  }
  return [];
}

function resolveDefaultRole(roles) {
  if (roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('EMPLOYEE')) return 'EMPLOYEE';
  if (roles.includes('OWNER')) return 'OWNER';
  return null;
}

export function AuthProvider({ children }) {
  const [accessToken, setAccessTokenState] = useState(() => readSessionValue(ACCESS_TOKEN_STORAGE_KEY));
  const [user, setUserState] = useState(() => readStoredUser());
  const [activeRole, setActiveRole] = useState(() => localStorage.getItem(ACTIVE_ROLE_STORAGE_KEY));
  const [isAuthReady, setIsAuthReady] = useState(false);

  function setAccessToken(nextAccessToken) {
    setAccessTokenState(nextAccessToken);
    if (typeof window !== 'undefined') {
      if (nextAccessToken) {
        window.sessionStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, nextAccessToken);
      } else {
        window.sessionStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
      }
    }
  }

  function setUser(nextUser) {
    setUserState(nextUser);
    if (typeof window !== 'undefined') {
      if (nextUser) {
        window.sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser));
      } else {
        window.sessionStorage.removeItem(USER_STORAGE_KEY);
      }
    }
  }

  useEffect(() => {
    setTokens(accessToken);
  }, [accessToken]);

  useEffect(() => {
    registerUnauthorizedHandler(() => {
      setAccessToken(null);
      setUser(null);
      setActiveRole(null);
      localStorage.removeItem(ACTIVE_ROLE_STORAGE_KEY);
      localStorage.removeItem(HAS_SESSION_STORAGE_KEY);
      clearTokens();
    });
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function bootstrapSession() {
      const isOauthCallback = typeof window !== 'undefined'
        && new URLSearchParams(window.location.search).has('code');
      const hasSession = localStorage.getItem(HAS_SESSION_STORAGE_KEY) === 'true';

      if (isOauthCallback || !hasSession) {
        if (!cancelled) {
          setIsAuthReady(true);
        }
        return;
      }

      try {
        const response = await axiosInstance.post('/api/auth/refresh', {});
        if (!cancelled) {
          applyAuthResponse(response.data);
        }
      } catch {
        if (!cancelled) {
          setAccessToken(null);
          setUser(null);
          setActiveRole(null);
          localStorage.removeItem(ACTIVE_ROLE_STORAGE_KEY);
          localStorage.removeItem(HAS_SESSION_STORAGE_KEY);
          clearTokens();
        }
      } finally {
        if (!cancelled) {
          setIsAuthReady(true);
        }
      }
    }

    bootstrapSession();
    return () => {
      cancelled = true;
    };
  }, []);

  async function login(identifier, password) {
    const response = await axiosInstance.post('/api/auth/login', { identifier, password });
    applyAuthResponse(response.data);
    return response.data;
  }

  async function register(payload) {
    const response = await axiosInstance.post('/api/auth/register', payload);
    applyAuthResponse(response.data);
    return response.data;
  }

  async function completeOAuthLogin(code) {
    const response = await axiosInstance.get('/api/auth/oauth2/exchange', {
      params: { code },
    });
    applyAuthResponse(response.data);
    return response.data;
  }

  async function logout() {
    try {
      await axiosInstance.post('/api/auth/logout');
    } finally {
      setAccessToken(null);
      setUser(null);
      setActiveRole(null);
      localStorage.removeItem(ACTIVE_ROLE_STORAGE_KEY);
      localStorage.removeItem(HAS_SESSION_STORAGE_KEY);
      clearTokens();
    }
  }

  async function completeOwnerProfile(payload) {
    const response = await axiosInstance.post('/api/auth/owner-profile', payload);
    const nextUser = response.data;
    setUser(nextUser);
    const nextRoles = normalizeRoles(nextUser);
    const nextRole = nextRoles.includes('OWNER') ? 'OWNER' : resolveDefaultRole(nextRoles);
    setActiveRole(nextRole);
    if (nextRole) {
      localStorage.setItem(ACTIVE_ROLE_STORAGE_KEY, nextRole);
    } else {
      localStorage.removeItem(ACTIVE_ROLE_STORAGE_KEY);
    }
    return response.data;
  }

  async function refreshMe() {
    const response = await axiosInstance.get('/api/auth/me');
    setUser(response.data);
    return response.data;
  }

  async function updateProfile(payload) {
    const response = await axiosInstance.put('/api/auth/profile', payload);
    setUser(response.data);
    return response.data;
  }

  function switchRole(nextRole) {
    const roles = normalizeRoles(user);
    if (!roles.includes(nextRole)) {
      return false;
    }
    setActiveRole(nextRole);
    localStorage.setItem(ACTIVE_ROLE_STORAGE_KEY, nextRole);
    return true;
  }

  function applyAuthResponse(data) {
    setAccessToken(data.accessToken);
    setUser(data.user);
    localStorage.setItem(HAS_SESSION_STORAGE_KEY, 'true');
    const roles = normalizeRoles(data.user);
    const persistedRole = localStorage.getItem(ACTIVE_ROLE_STORAGE_KEY);
    const nextRole = persistedRole && roles.includes(persistedRole)
      ? persistedRole
      : resolveDefaultRole(roles);

    setActiveRole(nextRole);
    if (nextRole) {
      localStorage.setItem(ACTIVE_ROLE_STORAGE_KEY, nextRole);
    } else {
      localStorage.removeItem(ACTIVE_ROLE_STORAGE_KEY);
    }
  }

  const roles = normalizeRoles(user);

  const value = useMemo(
    () => ({
      accessToken,
      user,
      role: activeRole,
      roles,
      isAuthReady,
      isAuthenticated: Boolean(accessToken),
      login,
      register,
      logout,
      completeOAuthLogin,
      completeOwnerProfile,
      refreshMe,
      updateProfile,
      switchRole,
    }),
    [accessToken, user, activeRole, roles, isAuthReady]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
