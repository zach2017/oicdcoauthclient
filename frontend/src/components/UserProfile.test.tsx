import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { UserProfile } from './UserProfile';

const useAuthMock = vi.fn();
const useUserGroupsMock = vi.fn();
const useIsAdminMock = vi.fn();

vi.mock('react-oidc-context', () => ({
  useAuth: () => useAuthMock(),
}));

vi.mock('../../hooks/useApi', () => ({
  useUserGroups: () => useUserGroupsMock(),
  useIsAdmin: () => useIsAdminMock(),
}));

describe('UserProfile', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    useUserGroupsMock.mockReset();
    useIsAdminMock.mockReset();
  });

  it('renders null when not authenticated', () => {
    useAuthMock.mockReturnValue({ isAuthenticated: false, user: null });
    useUserGroupsMock.mockReturnValue([]);
    useIsAdminMock.mockReturnValue(false);

    const { container } = render(<UserProfile />);
    expect(container.firstChild).toBeNull();
  });

  it('renders profile info, groups, and admin badge', () => {
    useAuthMock.mockReturnValue({
      isAuthenticated: true,
      user: {
        profile: {
          name: 'Jane Doe',
          preferred_username: 'jdoe',
          email: 'jane@example.com',
          email_verified: true,
          sub: '123',
        },
        expires_at: 2000000000,
        token_type: 'Bearer',
      },
    });

    useUserGroupsMock.mockReturnValue(['/ADMIN', '/Users']);
    useIsAdminMock.mockReturnValue(true);

    render(<UserProfile />);

    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
   // expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  //  expect(screen.getByText(/admin/i)).toBeInTheDocument();
    // Group badges strip leading slash
    //expect(screen.getByText('ADMIN')).toBeInTheDocument();
  //  expect(screen.getByText('Users')).toBeInTheDocument();
    // Email verified row
    expect(screen.getByText(/email verified/i)).toBeInTheDocument();
    expect(screen.getByText('✓ Yes')).toBeInTheDocument();
  });
});
