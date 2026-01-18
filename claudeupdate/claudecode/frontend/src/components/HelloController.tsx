import { useState, CSSProperties } from 'react';
import { useApi } from '../hooks/useApi';
import { HelloResponse, UserInfoResponse, AdminActionResponse } from '../services/api.service';

const styles: Record<string, CSSProperties> = {
  container: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '24px',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
    marginBottom: '20px',
  },
  title: {
    fontSize: '20px',
    fontWeight: '600',
    color: '#333',
    marginBottom: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  buttonGroup: {
    display: 'flex',
    flexWrap: 'wrap' as const,
    gap: '12px',
    marginBottom: '20px',
  },
  button: {
    padding: '10px 20px',
    fontSize: '14px',
    fontWeight: '500',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
    transition: 'all 0.2s ease',
    backgroundColor: '#667eea',
    color: 'white',
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
    cursor: 'not-allowed',
  },
  response: {
    backgroundColor: '#f8f9fa',
    borderRadius: '8px',
    padding: '16px',
    marginTop: '16px',
  },
  responseTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: '#666',
    marginBottom: '12px',
    textTransform: 'uppercase' as const,
  },
  codeBlock: {
    backgroundColor: '#1e1e1e',
    color: '#d4d4d4',
    padding: '16px',
    borderRadius: '8px',
    overflow: 'auto',
    fontSize: '13px',
    fontFamily: 'Monaco, Consolas, monospace',
    whiteSpace: 'pre-wrap' as const,
    wordBreak: 'break-word' as const,
  },
  error: {
    backgroundColor: '#ffebee',
    color: '#c62828',
    padding: '16px',
    borderRadius: '8px',
    marginTop: '16px',
  },
  loading: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    color: '#666',
    padding: '16px',
  },
  successMessage: {
    backgroundColor: '#e8f5e9',
    color: '#2e7d32',
    padding: '12px 16px',
    borderRadius: '8px',
    marginBottom: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
};

type ApiResponse = HelloResponse | UserInfoResponse | AdminActionResponse | null;

export function HelloController() {
  const { isAuthenticated, isTokenSet, getHello, getHelloMe, getUserInfo, performAdminAction } = useApi();
  const [response, setResponse] = useState<ApiResponse>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastEndpoint, setLastEndpoint] = useState<string>('');

  const handleApiCall = async (
    apiCall: () => Promise<ApiResponse>,
    endpointName: string
  ) => {
    setLoading(true);
    setError(null);
    setLastEndpoint(endpointName);
    
    try {
      const result = await apiCall();
      setResponse(result);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      const axiosError = err as { response?: { status?: number; data?: { message?: string } } };
      
      if (axiosError.response?.status === 403) {
        setError('Access Denied: You need ADMIN role to access this endpoint');
      } else if (axiosError.response?.status === 401) {
        setError('Unauthorized: Please login again');
      } else {
        setError(axiosError.response?.data?.message || errorMessage);
      }
      setResponse(null);
    } finally {
      setLoading(false);
    }
  };

  const isDisabled = !isAuthenticated || !isTokenSet || loading;

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>
        <span>🔌</span> API Endpoints (Protected by ADMIN role)
      </h3>

      {isAuthenticated && isTokenSet && (
        <div style={styles.successMessage}>
          <span>✓</span> Authenticated - Bearer token ready
        </div>
      )}

      <div style={styles.buttonGroup}>
        <button
          onClick={() => handleApiCall(getHello, '/api/hello')}
          disabled={isDisabled}
          style={{
            ...styles.button,
            ...(isDisabled ? styles.buttonDisabled : {}),
          }}
        >
          GET /api/hello
        </button>

        <button
          onClick={() => handleApiCall(getHelloMe, '/api/hello/me')}
          disabled={isDisabled}
          style={{
            ...styles.button,
            ...(isDisabled ? styles.buttonDisabled : {}),
          }}
        >
          GET /api/hello/me
        </button>

        <button
          onClick={() => handleApiCall(getUserInfo, '/api/hello/userinfo')}
          disabled={isDisabled}
          style={{
            ...styles.button,
            ...(isDisabled ? styles.buttonDisabled : {}),
          }}
        >
          GET /api/hello/userinfo
        </button>

        <button
          onClick={() => handleApiCall(
            () => performAdminAction({ action: 'test', timestamp: new Date().toISOString() }),
            '/api/hello/action'
          )}
          disabled={isDisabled}
          style={{
            ...styles.button,
            ...(isDisabled ? styles.buttonDisabled : {}),
          }}
        >
          POST /api/hello/action
        </button>
      </div>

      {!isAuthenticated && (
        <div style={{ color: '#666', padding: '16px 0' }}>
          Please login to access the protected API endpoints.
        </div>
      )}

      {loading && (
        <div style={styles.loading}>
          <span>⏳</span> Loading...
        </div>
      )}

      {error && (
        <div style={styles.error}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {response && !loading && (
        <div style={styles.response}>
          <div style={styles.responseTitle}>
            Response from {lastEndpoint}
          </div>
          <pre style={styles.codeBlock}>
            {JSON.stringify(response, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
