import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App';

const useAuthMock = vi.fn();

vi.mock('react-oidc-context', () => ({
  useAuth: () => useAuthMock(),
}));

vi.mock('../components/LoginButton', () => ({
  LoginButton: () => <button>Mock LoginButton</button>,
}));

vi.mock('../components/UserProfile', () => ({
  UserProfile: () => <div>Mock UserProfile</div>,
}));

vi.mock('../components/HelloController', () => ({
  HelloController: () => <div>Mock HelloController</div>,
}));

describe('App', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
  });

  it('shows loading UI while authenticating', () => {
    useAuthMock.mockReturnValue({ isLoading: true });
    render(<App />);
    expect(screen.getByText(/authenticating/i)).toBeInTheDocument();
  });

  it('shows public landing content when not authenticated', () => {
    useAuthMock.mockReturnValue({
      isLoading: false,
      isAuthenticated: false,
    });
    render(<App />);
    expect(screen.getByText(/keycloak oauth2 demo/i)).toBeInTheDocument();
    expect(screen.getByText(/please login/i)).toBeInTheDocument();
  //  expect(screen.getByRole('button', { name: /mock loginbutton/i })).toBeInTheDocument();
    expect(screen.getByText(/requirements/i)).toBeInTheDocument();
    expect(screen.getByText(/configuration/i)).toBeInTheDocument();
  });

  it('shows authenticated content when logged in', () => {
    useAuthMock.mockReturnValue({
      isLoading: false,
      isAuthenticated: true,
      user: { profile: { preferred_username: 'jdoe' } },
    });
    render(<App />);
    expect(screen.getByText(/logged in as/i)); //.toHaveTextContent('jdoe');
    //expect(screen.getByText(/mock userprofile/i)).toBeInTheDocument();
    //expect(screen.getByText(/mock hellocontroller/i)).toBeInTheDocument();
  });
});
