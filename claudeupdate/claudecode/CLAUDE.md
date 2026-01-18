# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Keycloak OAuth2/OIDC authentication demo with a Spring Boot backend (Java 17, port 8080) and React/TypeScript frontend (Vite, port 3000). The architecture demonstrates secure JWT-based authentication with Keycloak as the identity provider (port 8180).

## Development Commands

### Infrastructure
```bash
# Start Keycloak using Docker Compose
docker-compose up -d

# Or manually run Keycloak
docker run -d --name keycloak -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

### Backend (Spring Boot)
```bash
cd backend

# Run the application
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build the project
./mvnw clean package

# Run tests
./mvnw test

# Run a specific test
./mvnw test -Dtest=ClassName#methodName
```

### Frontend (React + Vite)
```bash
cd frontend

# Install dependencies
npm install

# Start development server (port 3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

## Architecture & Key Components

### Authentication Flow
1. **React frontend** initiates OIDC Authorization Code + PKCE flow with Keycloak
2. **Keycloak** authenticates the user and issues JWT tokens
3. **Frontend** includes JWT as Bearer token in API requests
4. **Spring Boot backend** validates JWT signature using Keycloak's public key (JWKS)
5. **Backend** extracts roles/groups from JWT and enforces authorization

### Role/Authority Extraction (Critical Pattern)

The `KeycloakJwtAuthenticationConverter` extracts Spring Security authorities from three JWT sources:
- `realm_access.roles` → realm-level roles
- `resource_access.{client_id}.roles` → client-specific roles
- `groups` → Keycloak group memberships (e.g., `/ADMIN` group becomes `ROLE_ADMIN`)

All extracted roles are prefixed with `ROLE_` and uppercased for Spring Security's `hasRole()` checks.

### Security Configuration

**Backend** (`SecurityConfig.java`):
- Public endpoints: `/api/public/**`, `/actuator/health`, `/error`
- Protected endpoints: `/api/hello/**` and `/api/admin/**` require `ROLE_ADMIN`
- Custom `KeycloakAuthenticationEntryPoint` handles 401 responses:
  - Browser requests (Accept: text/html) → redirect to Keycloak login
  - API/AJAX requests → return JSON with `login_url` field
- CORS configured via `app.cors.allowed-origins` in application.yml
- Stateless session (no server-side sessions)

**Frontend** (`react-oidc-context` + `oidc-client-ts`):
- PKCE-enabled Authorization Code flow (mobile/iOS compatible)
- Automatic token refresh via silent iframe (`silent-renew.html`)
- Tokens stored in localStorage for iOS compatibility
- Requests `groups` scope to include group membership in tokens
- Bearer token attached via Axios interceptor (`api.service.ts`)

### Key Files

**Backend:**
- `SecurityConfig.java` - Spring Security rules, CORS, and exception handling
- `KeycloakJwtAuthenticationConverter.java` - Custom role extraction from JWT
- `KeycloakAuthenticationEntryPoint.java` - Browser vs API 401 handling
- `application.yml` - Keycloak issuer URI, CORS origins, client configuration

**Frontend:**
- `auth.config.ts` - OIDC client settings (authority, scopes, redirect URIs)
- `api.service.ts` - Axios instance with Bearer token interceptor
- `useApi.ts` - React hook for authenticated API calls

### Environment Variables

**Backend** (via `application.yml` or env vars):
- `KEYCLOAK_ISSUER_URI` - Full realm issuer URL (default: http://localhost:8180/realms/demo)
- `KEYCLOAK_AUTH_SERVER_URL` - Keycloak base URL for constructing auth URLs
- `KEYCLOAK_REALM` - Realm name (default: demo)
- `KEYCLOAK_CLIENT_ID` - Public client ID (default: react-client)
- `KEYCLOAK_REDIRECT_URI` - Frontend callback URL
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins

**Frontend** (via `.env` or `.env.example`):
- `VITE_KEYCLOAK_URL` - Keycloak base URL (default: http://localhost:8180)
- `VITE_KEYCLOAK_REALM` - Realm name (default: demo)
- `VITE_KEYCLOAK_CLIENT_ID` - Client ID (default: react-client)
- `VITE_API_URL` - Backend API URL (default: http://localhost:8080)

## Keycloak Setup Requirements

For the application to work, Keycloak must be configured with:
1. Realm named `demo` (or matching `KEYCLOAK_REALM`)
2. Public client `react-client` with:
   - Client authentication: OFF
   - Valid redirect URIs: `http://localhost:3000/*`
   - Web origins: `http://localhost:3000`
3. Client scope `groups` with Group Membership mapper:
   - Token claim name: `groups`
   - Add to ID/access/userinfo tokens
4. Group named `ADMIN` (users in this group get `ROLE_ADMIN`)
5. Test user with credentials assigned to `ADMIN` group

**Note:** The README contains detailed step-by-step Keycloak configuration instructions. Refer to it when setting up a new instance.

## Common Modifications

### Adding New Protected Endpoints
1. Add controller method with `@PreAuthorize("hasRole('ADMIN')")` or similar
2. Update `SecurityConfig.authorizeHttpRequests()` if using URL-based rules
3. Consider if new roles require additional JWT claim mappings in Keycloak

### Changing Role Sources
- Edit `KeycloakJwtAuthenticationConverter` methods:
  - `extractRealmRoles()` - for realm_access.roles
  - `extractResourceRoles()` - for resource_access claims
  - `extractGroupRoles()` - for groups claim
- Adjust role prefix/casing logic if needed

### Adjusting CORS
- Backend: Update `app.cors.allowed-origins` in `application.yml`
- Keycloak: Add matching origins to client's Web Origins setting

### Mobile/Native App Integration
- Frontend uses PKCE flow (mobile-compatible by default)
- For React Native: Use `react-native-app-auth` or `expo-auth-session`
- Configure custom URL scheme for redirects (e.g., `myapp://callback`)
- Ensure Keycloak client allows the custom redirect URI
