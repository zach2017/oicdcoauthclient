# Keycloak OAuth2 Demo

A complete OAuth2/OIDC authentication setup with:
- **Spring Boot Resource Server** - Protected by Keycloak JWT tokens
- **React Client** - OIDC authentication with bearer token support
- **iOS Compatible** - Uses PKCE flow for mobile-friendly authentication

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React App     │────▶│    Keycloak     │────▶│  Spring Boot    │
│   (Frontend)    │◀────│  (Auth Server)  │◀────│  (Resource API) │
│   Port: 3000    │     │   Port: 8180    │     │   Port: 8080    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │   1. Login Request    │                       │
        │──────────────────────▶│                       │
        │   2. Auth Code + PKCE │                       │
        │◀──────────────────────│                       │
        │   3. Exchange Token   │                       │
        │──────────────────────▶│                       │
        │   4. JWT Access Token │                       │
        │◀──────────────────────│                       │
        │                       │                       │
        │   5. API Request with Bearer Token            │
        │──────────────────────────────────────────────▶│
        │   6. Validate JWT (with Keycloak public key)  │
        │                       │◀──────────────────────│
        │   7. API Response                             │
        │◀──────────────────────────────────────────────│
```

## Prerequisites

- Java 17+
- Node.js 18+
- Docker (for running Keycloak)
- Maven

## Quick Start

### 1. Start Keycloak

```bash
# Run Keycloak in development mode
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:23.0 \
  start-dev
```

### 2. Configure Keycloak

Access Keycloak Admin Console at http://localhost:8180 (admin/admin)

#### Create Realm
1. Click "Create Realm"
2. Name: `demo`
3. Click "Create"

#### Create Client
1. Go to Clients → Create client
2. Client ID: `react-client`
3. Client type: `OpenID Connect`
4. Click "Next"
5. Client authentication: `OFF` (public client for SPA)
6. Authorization: `OFF`
7. Click "Next"
8. Root URL: `http://localhost:3000`
9. Valid redirect URIs: `http://localhost:3000/*`
10. Valid post logout redirect URIs: `http://localhost:3000/*`
11. Web origins: `http://localhost:3000`
12. Click "Save"

#### Configure Client Scopes (for groups)
1. Go to Client Scopes → Create client scope
2. Name: `groups`
3. Type: `Default`
4. Protocol: `OpenID Connect`
5. Click "Save"
6. Go to Mappers → Add mapper → By configuration → Group Membership
7. Name: `groups`
8. Token Claim Name: `groups`
9. Full group path: `OFF`
10. Add to ID token: `ON`
11. Add to access token: `ON`
12. Add to userinfo: `ON`
13. Click "Save"

#### Add Scope to Client
1. Go to Clients → react-client → Client scopes
2. Click "Add client scope"
3. Select `groups` → Add as "Default"

#### Create ADMIN Group
1. Go to Groups → Create group
2. Name: `ADMIN`
3. Click "Create"

#### Create Test User
1. Go to Users → Add user
2. Username: `testadmin`
3. Email: `admin@example.com`
4. First name: `Test`
5. Last name: `Admin`
6. Email verified: `ON`
7. Click "Create"
8. Go to Credentials → Set password
9. Password: `password`
10. Temporary: `OFF`
11. Click "Save"
12. Go to Groups → Join group → Select `ADMIN`

### 3. Start Backend

```bash
cd backend

# Set environment variables (optional, defaults to localhost:8180)
export KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/demo

# Run with Maven
./mvnw spring-boot:run
```

The API will be available at http://localhost:8080

### 4. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at http://localhost:3000

### 5. Test the Application

1. Open http://localhost:3000
2. Click "Login with Keycloak"
3. Login with `testadmin` / `password`
4. Click the API buttons to test protected endpoints
5. Observe the JWT token claims and API responses

## API Endpoints

### Public Endpoints (No Auth)
- `GET /api/public/health` - Health check
- `GET /api/public/info` - Service info

### Protected Endpoints (Require ADMIN role)
- `GET /api/hello` - Basic hello with user info
- `GET /api/hello/me` - Detailed user information
- `GET /api/hello/userinfo` - Full JWT claims and token info
- `POST /api/hello/action` - Admin action endpoint

## Configuration

### Backend (application.yml)

```yaml
spring.security.oauth2.resourceserver:
  jwt:
    issuer-uri: http://localhost:8180/realms/demo

app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
```

### Frontend (.env)

```env
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=demo
VITE_KEYCLOAK_CLIENT_ID=react-client
VITE_API_URL=http://localhost:8080
```

## iOS / Mobile Support

The React client uses PKCE (Proof Key for Code Exchange) flow which is:
- Recommended for native mobile apps and SPAs
- More secure than implicit flow
- Supported by iOS Safari and WKWebView

For React Native / iOS apps, you can use the same OIDC configuration with libraries like:
- `react-native-app-auth`
- `expo-auth-session`

### WebView Considerations
- Ensure cookies are enabled for session management
- Use `SameSite=None; Secure` cookies for cross-origin requests
- Consider using a custom URL scheme for redirects in native apps

## Security Features

### Login Redirect for Anonymous Users
When an unauthenticated user accesses a protected endpoint:

| Client Type | Behavior |
|-------------|----------|
| Browser (Accept: text/html) | Redirects to Keycloak login page |
| API Client (Accept: application/json) | Returns 401 JSON with login_url |
| AJAX (X-Requested-With: XMLHttpRequest) | Returns 401 JSON with login_url |

**Example 401 Response for API clients:**
```json
{
  "error": "unauthorized",
  "message": "Authentication required",
  "login_url": "http://localhost:8180/realms/demo/protocol/openid-connect/auth?...",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Configuration Options:**
```yaml
keycloak:
  enable-redirect: true  # Set to false to always return 401
  redirect-uri: http://localhost:3000/callback
```

### JWT Token Validation
- Tokens are validated against Keycloak's public key (JWKS)
- Issuer, audience, and expiration are verified
- Token signature is cryptographically verified

### Role Extraction
The `KeycloakJwtAuthenticationConverter` extracts roles from:
1. `realm_access.roles` - Realm-level roles
2. `resource_access.{client}.roles` - Client-level roles
3. `groups` - Group memberships (converted to `ROLE_` format)

### CORS Configuration
- Only allowed origins can access the API
- Credentials (cookies, auth headers) are allowed
- Preflight requests are cached for 1 hour

## Troubleshooting

### "Access Denied" Error
- Ensure user is a member of the ADMIN group
- Verify the `groups` scope is included in the client
- Check that the group mapper is configured correctly

### "Invalid Token" Error
- Verify Keycloak is running at the configured URL
- Check that the realm name matches
- Ensure the issuer URI matches exactly

### CORS Errors
- Add your frontend URL to `app.cors.allowed-origins`
- Ensure the Keycloak client has the correct Web Origins

### Token Not Refreshing
- Check that `automaticSilentRenew` is enabled
- Verify the silent-renew.html is accessible
- Check browser console for iframe/postMessage errors

## Project Structure

```
keycloak-demo/
├── backend/
│   ├── src/main/java/com/example/demo/
│   │   ├── KeycloakResourceServerApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── controller/
│   │   │   ├── HelloController.java
│   │   │   └── PublicController.java
│   │   └── security/
│   │       └── KeycloakJwtAuthenticationConverter.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── HelloController.tsx
│   │   │   ├── LoginButton.tsx
│   │   │   └── UserProfile.tsx
│   │   ├── config/
│   │   │   └── auth.config.ts
│   │   ├── hooks/
│   │   │   └── useApi.ts
│   │   ├── services/
│   │   │   └── api.service.ts
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── public/
│   │   └── silent-renew.html
│   ├── package.json
│   └── vite.config.ts
└── README.md
```

## License

MIT
