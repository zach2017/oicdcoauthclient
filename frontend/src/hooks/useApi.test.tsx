import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useIsAdmin, useUserGroups } from './useApi';

const useAuthMock = vi.fn();
vi.mock('react-oidc-context', () => ({
  useAuth: () => useAuthMock(),
}));

describe('useUserGroups', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
  });

  it('returns empty array when no user profile', () => {
    useAuthMock.mockReturnValue({ user: null });
    const { result } = renderHook(() => useUserGroups());
    expect(result.current).toEqual([]);
  });

  it('returns groups from profile', () => {
    useAuthMock.mockReturnValue({
      user: { profile: { groups: ['/ADMIN', '/users'] } },
    });
    const { result } = renderHook(() => useUserGroups());
    expect(result.current).toEqual(['/ADMIN', '/users']);
  });
});

describe('useIsAdmin', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
  });

  it('is true when group contains ADMIN', () => {
    useAuthMock.mockReturnValue({
      user: { profile: { groups: ['/ADMIN'] } },
    });
    const { result } = renderHook(() => useIsAdmin());
    expect(result.current).toBe(true);
  });

  it('is false when no admin group', () => {
    useAuthMock.mockReturnValue({
      user: { profile: { groups: ['/users'] } },
    });
    const { result } = renderHook(() => useIsAdmin());
    expect(result.current).toBe(false);
  });
});
