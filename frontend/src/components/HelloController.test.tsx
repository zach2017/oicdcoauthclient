import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HelloController } from './HelloController';

const useApiMock = vi.fn();

vi.mock('../hooks/useApi', () => ({
  useApi: () => useApiMock(),
}));

describe('HelloController', () => {
  beforeEach(() => {
    useApiMock.mockReset();
  });

  it('shows login prompt and disables buttons when unauthenticated', () => {
    useApiMock.mockReturnValue({
      isAuthenticated: false,
      isTokenSet: false,
      getHello: vi.fn(),
      getHelloMe: vi.fn(),
      getUserInfo: vi.fn(),
      performAdminAction: vi.fn(),
    });

    render(<HelloController />);

    expect(screen.getByText(/please login/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /get \/api\/hello$/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /get \/api\/hello\/me/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /get \/api\/hello\/userinfo/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /post \/api\/hello\/action/i })).toBeDisabled();
  });

  it('shows bearer-token-ready message when authenticated and token is set', () => {
    useApiMock.mockReturnValue({
      isAuthenticated: true,
      isTokenSet: true,
      getHello: vi.fn(),
      getHelloMe: vi.fn(),
      getUserInfo: vi.fn(),
      performAdminAction: vi.fn(),
    });

    render(<HelloController />);
    expect(screen.getByText(/bearer token ready/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /get \/api\/hello$/i })).toBeEnabled();
  });

  it('renders a JSON response after a successful call', async () => {
    const getHello = vi.fn().mockResolvedValue({
      message: 'Hello',
      user: 'jdoe',
      timestamp: 'now',
      roles: ['ADMIN'],
    });

    useApiMock.mockReturnValue({
      isAuthenticated: true,
      isTokenSet: true,
      getHello,
      getHelloMe: vi.fn(),
      getUserInfo: vi.fn(),
      performAdminAction: vi.fn(),
    });

    render(<HelloController />);

    await userEvent.click(screen.getByRole('button', { name: /get \/api\/hello$/i }));

    await waitFor(() => {
      expect(screen.getByText(/response from \/api\/hello/i)).toBeInTheDocument();
    });

    expect(getHello).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/\"message\":\s*\"hello\"/i)).toBeInTheDocument();
  });

  it('shows a friendly message for 403 errors', async () => {
    const err: any = new Error('Forbidden');
    err.response = { status: 403, data: { message: 'nope' } };

    const getHello = vi.fn().mockRejectedValue(err);

    useApiMock.mockReturnValue({
      isAuthenticated: true,
      isTokenSet: true,
      getHello,
      getHelloMe: vi.fn(),
      getUserInfo: vi.fn(),
      performAdminAction: vi.fn(),
    });

    render(<HelloController />);

    await userEvent.click(screen.getByRole('button', { name: /get \/api\/hello$/i }));

    await waitFor(() => {
      expect(screen.getByText(/access denied/i)).toBeInTheDocument();
    });
  });
});
