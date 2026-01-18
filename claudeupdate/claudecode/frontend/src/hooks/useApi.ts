import { useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { api, HelloResponse, UserInfoResponse } from '../services/api.service';

/**
 * Custom hook for making authenticated API calls
 * Uses session-based authentication (no token management needed)
 */
export function useApi() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuth();

  // Hello endpoint
  const getHello = useCallback(async (): Promise<HelloResponse | null> => {
    if (!isAuthenticated) {
      console.warn('Not authenticated');
      return null;
    }
    try {
      return await api.getHello();
    } catch (error) {
      console.error('Failed to call hello endpoint:', error);
      throw error;
    }
  }, [isAuthenticated]);

  // Hello/me endpoint
  const getHelloMe = useCallback(async (): Promise<HelloResponse | null> => {
    if (!isAuthenticated) {
      console.warn('Not authenticated');
      return null;
    }
    try {
      return await api.getHelloMe();
    } catch (error) {
      console.error('Failed to call hello/me endpoint:', error);
      throw error;
    }
  }, [isAuthenticated]);

  // User info endpoint
  const getUserInfo = useCallback(async (): Promise<UserInfoResponse | null> => {
    if (!isAuthenticated) {
      console.warn('Not authenticated');
      return null;
    }
    try {
      return await api.getUserInfo();
    } catch (error) {
      console.error('Failed to get user info:', error);
      throw error;
    }
  }, [isAuthenticated]);

  // Admin action
  const performAdminAction = useCallback(async (payload?: Record<string, unknown>) => {
    if (!isAuthenticated) {
      console.warn('Not authenticated');
      return null;
    }
    try {
      return await api.performAdminAction(payload);
    } catch (error) {
      console.error('Failed to perform admin action:', error);
      throw error;
    }
  }, [isAuthenticated]);

  // Public health check (doesn't require auth)
  const getHealth = useCallback(async () => {
    try {
      return await api.getHealth();
    } catch (error) {
      console.error('Health check failed:', error);
      throw error;
    }
  }, []);

  // Public info (doesn't require auth)
  const getInfo = useCallback(async () => {
    try {
      return await api.getInfo();
    } catch (error) {
      console.error('Info check failed:', error);
      throw error;
    }
  }, []);

  return {
    isAuthenticated,
    isLoading,
    user,
    getHello,
    getHelloMe,
    getUserInfo,
    performAdminAction,
    getHealth,
    getInfo,
    // Auth methods
    login,
    logout,
  };
}

/**
 * Hook to extract user groups
 */
export function useUserGroups(): string[] {
  const { user } = useAuth();

  if (!user || !user.groups) {
    return [];
  }

  return user.groups;
}

/**
 * Hook to check if user has admin role/group
 */
export function useIsAdmin(): boolean {
  const groups = useUserGroups();

  // Check for ADMIN group (with or without leading slash)
  return groups.some(group =>
    group === 'ADMIN' ||
    group === '/ADMIN' ||
    group.endsWith('/ADMIN')
  );
}
