import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginButton } from './LoginButton';

const useAuthMock = vi.fn();

vi.mock('react-oidc-context', () => ({
  useAuth: () => useAuthMock(),
}));

describe('LoginButton', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
  });

  it('shows a disabled loading button when auth is loading', () => {
    useAuthMock.mockReturnValue({
      isLoading: true,
      isAuthenticated: false,
      error: null,
    });
    render(<LoginButton />);
    expect(screen.getByRole('button', { name: /loading/i })).toBeDisabled();
  });

  it('renders an error state and lets user retry', async () => {
    const signinRedirect = vi.fn();
    useAuthMock.mockReturnValue({
      isLoading: false,
      isAuthenticated: false,
      error: new Error('Boom'),
      signinRedirect,
    });
    render(<LoginButton />);
    expect(screen.getByText(/error:/i)).toHaveTextContent('Boom');
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(signinRedirect).toHaveBeenCalledTimes(1);
  });

  it('shows logout when authenticated and calls signoutRedirect', async () => {
    const signoutRedirect = vi.fn();
    useAuthMock.mockReturnValue({
      isLoading: false,
      isAuthenticated: true,
      error: null,
      signoutRedirect,
    });
    render(<LoginButton />);
    await userEvent.click(screen.getByRole('button', { name: /logout/i }));
    expect(signoutRedirect).toHaveBeenCalledTimes(1);
  });

  it('shows login when unauthenticated and calls signinRedirect', async () => {
    const signinRedirect = vi.fn();
    useAuthMock.mockReturnValue({
      isLoading: false,
      isAuthenticated: false,
      error: null,
      signinRedirect,
    });
    render(<LoginButton />);
    await userEvent.click(screen.getByRole('button', { name: /login with keycloak/i }));
    expect(signinRedirect).toHaveBeenCalledTimes(1);
  });
});
