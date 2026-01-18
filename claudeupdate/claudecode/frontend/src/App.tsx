import { useAuth } from 'react-oidc-context';
import { LoginButton } from './components/LoginButton';
import { UserProfile } from './components/UserProfile';
import { HelloController } from './components/HelloController';
import { CSSProperties } from 'react';

const styles: Record<string, CSSProperties> = {
  container: {
    minHeight: '100vh',
    padding: '20px',
    paddingTop: 'env(safe-area-inset-top, 20px)',
    paddingBottom: 'env(safe-area-inset-bottom, 20px)',
  },
  header: {
    textAlign: 'center' as const,
    marginBottom: '32px',
    color: 'white',
  },
  title: {
    fontSize: '32px',
    fontWeight: '700',
    marginBottom: '8px',
    textShadow: '0 2px 4px rgba(0,0,0,0.2)',
  },
  subtitle: {
    fontSize: '16px',
    opacity: 0.9,
  },
  main: {
    maxWidth: '800px',
    margin: '0 auto',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    marginBottom: '20px',
    textAlign: 'center' as const,
  },
  welcomeText: {
    fontSize: '18px',
    color: '#333',
    marginBottom: '20px',
  },
  infoCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    marginBottom: '20px',
  },
  infoTitle: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#333',
    marginBottom: '16px',
  },
  infoList: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
  },
  infoItem: {
    padding: '8px 0',
    borderBottom: '1px solid #eee',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '50vh',
    color: 'white',
  },
  spinner: {
    width: '48px',
    height: '48px',
    border: '4px solid rgba(255,255,255,0.3)',
    borderTopColor: 'white',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
};

function App() {
  const auth = useAuth();

  // Handle OAuth callback
  if (auth.isLoading) {
    return (
      <div style={styles.container}>
        <div style={styles.loadingContainer}>
          <div style={styles.spinner}></div>
          <p style={{ marginTop: '16px' }}>Authenticating...</p>
        </div>
        <style>{`
          @keyframes spin {
            to { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <h1 style={styles.title}>🔐 Keycloak OAuth2 Demo</h1>
        <p style={styles.subtitle}>
          Spring Boot Resource Server + React Client
        </p>
      </header>

      <main style={styles.main}>
        {!auth.isAuthenticated ? (
          <>
            <div style={styles.card}>
              <p style={styles.welcomeText}>
                Welcome! Please login with your Keycloak account to access the protected API.
              </p>
              <LoginButton />
            </div>

            <div style={styles.infoCard}>
              <h3 style={styles.infoTitle}>📋 Requirements</h3>
              <ul style={styles.infoList}>
                <li style={styles.infoItem}>
                  <span>✓</span> User must be a member of the <strong>ADMIN</strong> group in Keycloak
                </li>
                <li style={styles.infoItem}>
                  <span>✓</span> Keycloak client must include <strong>groups</strong> scope
                </li>
                <li style={styles.infoItem}>
                  <span>✓</span> Group mapper must be configured to include groups in token
                </li>
              </ul>
            </div>

            <div style={styles.infoCard}>
              <h3 style={styles.infoTitle}>🔧 Configuration</h3>
              <ul style={styles.infoList}>
                <li style={styles.infoItem}>
                  <span>🖥️</span> Backend: http://localhost:8080
                </li>
                <li style={styles.infoItem}>
                  <span>🌐</span> Frontend: http://localhost:3000
                </li>
                <li style={styles.infoItem}>
                  <span>🔑</span> Keycloak: http://localhost:8180
                </li>
              </ul>
            </div>
          </>
        ) : (
          <>
            <div style={{ ...styles.card, display: 'flex', justifyContent: 'space-between', alignItems: 'center', textAlign: 'left' as const }}>
              <div>
                <strong>Logged in as:</strong> {auth.user?.profile.preferred_username}
              </div>
              <LoginButton />
            </div>

            <UserProfile />
            <HelloController />
          </>
        )}
      </main>
    </div>
  );
}

export default App;
