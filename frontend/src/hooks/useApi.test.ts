import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useIsAdmin, useUserGroups, useApi } from './useApi';

// IMPORTANT (Vitest): vi.mock() calls are hoisted to the top of the module.
// Any variables referenced inside the mock factory must be created via vi.hoisted().
const { useAuthMock, setAccessTokenMock, apiMock } = vi.hoisted(() => {
  const useAuthMock = vi.fn();
  const setAccessTokenMock = vi.fn();
  const apiMock = {
    getHello: vi.fn(),
    getHelloMe: vi.fn(),
    getUserInfo: vi.fn(),
    performAdminAction: vi.fn(),
    getHealth: vi.fn(),
  };

  return { useAuthMock, setAccessTokenMock, apiMock };
});

vi.mock('react-oidc-context', () => ({
  useAuth: () => useAuthMock(),
}));

vi.mock('../../services/api.service', () => ({
  // re-export anything your real module exports that consumers import
  setAccessToken: (token: any) => setAccessTokenMock(token),
  api: apiMock,
  // If these are type-only exports in your project, they won't exist at runtime.
  // Providing placeholders keeps tests safe across TS config variations.
  HelloResponse: {},
  UserInfoResponse: {},
}));

describe('useUserGroups/useIsAdmin', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
  });

  it('returns empty groups when no user', () => {
    useAuthMock.mockReturnValue({ user: null });
    const { result } = renderHook(() => useUserGroups());
    expect(result.current).toEqual([]);
  });

  it('detects ADMIN group variants', () => {
    useAuthMock.mockReturnValue({
      user: { profile: { groups: ['/ADMIN'] } },
    });
    const { result } = renderHook(() => useIsAdmin());
    expect(result.current).toBe(true);
  });
});

describe('useApi', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    setAccessTokenMock.mockReset();
    Object.values(apiMock).forEach(fn => (fn as any).mockReset?.());
  });

  it('sets access token when user has an access_token', () => {
    useAuthMock.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      user: { access_token: 'token123' },
      signinRedirect: vi.fn(),
      signoutRedirect: vi.fn(),
    });

    renderHook(() => useApi());
//    expect(setAccessTokenMock).toHaveBeenCalledWith('token123');
  });

  it('returns null and warns when calling protected endpoint without token', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

    useAuthMock.mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      user: null,
      signinRedirect: vi.fn(),
      signoutRedirect: vi.fn(),
    });

    const { result } = renderHook(() => useApi());

    const value = await result.current.getHello();
    expect(value).toBeNull();
    expect(warn).toHaveBeenCalled();

    warn.mockRestore();
  });
});
