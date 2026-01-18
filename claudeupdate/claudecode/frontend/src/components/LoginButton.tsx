import { useAuth } from '../contexts/AuthContext';
import { CSSProperties } from 'react';

const styles: Record<string, CSSProperties> = {
  button: {
    padding: '12px 32px',
    fontSize: '16px',
    fontWeight: '600',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  loginButton: {
    backgroundColor: '#4CAF50',
    color: 'white',
  },
  logoutButton: {
    backgroundColor: '#f44336',
    color: 'white',
  },
  loadingButton: {
    backgroundColor: '#9e9e9e',
    color: 'white',
    cursor: 'not-allowed',
  },
};

export function LoginButton() {
  const { isAuthenticated, isLoading, login, logout } = useAuth();

  if (isLoading) {
    return (
      <button style={{ ...styles.button, ...styles.loadingButton }} disabled>
        <span>⏳</span> Loading...
      </button>
    );
  }

  if (isAuthenticated) {
    return (
      <button
        onClick={logout}
        style={{ ...styles.button, ...styles.logoutButton }}
      >
        <span>🚪</span> Logout
      </button>
    );
  }

  return (
    <button
      onClick={login}
      style={{ ...styles.button, ...styles.loginButton }}
    >
      <span>🔐</span> Login with Keycloak
    </button>
  );
}
