# BFF Pattern Migration Complete

## Summary

Successfully migrated from frontend OAuth2 (tokens in localStorage) to Backend-for-Frontend (BFF) pattern with secure server-side session management.

## Key Changes

### Backend Changes
1. **Dependencies Added**:
   - `spring-boot-starter-oauth2-client` (handles OAuth2 flow)
   - `spring-boot-starter-data-redis` (session storage)
   - `spring-session-data-redis` (Redis session integration)

2. **SecurityConfig.java**: Completely rewritten
   - Changed from Resource Server (JWT validation) to OAuth2 Client (session-based)
   - Session creation policy: `IF_REQUIRED` (was `STATELESS`)
   - Added CSRF protection with cookie-based tokens
   - OAuth2 login configuration
   - Session cookies: HttpOnly, Secure (production), SameSite=Lax

3. **New Files**:
   - `AuthController.java` - BFF auth endpoints (`/api/auth/user`, `/api/auth/login`, `/api/auth/logout`, `/api/auth/csrf`)

4. **Updated Controllers**:
   - `HelloController.java` - Now uses `OAuth2User` instead of `Jwt`

5. **Deleted Files** (no longer needed):
   - `KeycloakJwtAuthenticationConverter.java`
   - `KeycloakAuthenticationEntryPoint.java`
   - `KeycloakAccessDeniedHandler.java`

### Frontend Changes
1. **Dependencies Removed**:
   - `oidc-client-ts`
   - `react-oidc-context`

2. **New Files**:
   - `contexts/AuthContext.tsx` - Simple session-based auth context

3. **Updated Files**:
   - `api.service.ts` - Removed token management, added CSRF handling, enabled withCredentials
   - `useApi.ts` - Simplified to use new auth context
   - `main.tsx` - Uses new AuthProvider
   - `LoginButton.tsx` - Uses new auth hooks
   - `UserProfile.tsx` - Uses new user data structure

4. **Deleted Files**:
   - `config/auth.config.ts`
   - `public/silent-renew.html`

---

## Required Setup Steps

### 1. Start Redis

Redis is required for session storage.

```bash
# Start Redis with Docker
docker run -d -p 6379:6379 --name redis redis:latest

# Or using docker-compose
docker-compose up -d
```

Verify Redis is running:
```bash
docker ps | grep redis
# Or
redis-cli ping
# Should respond: PONG
```

### 2. Update Keycloak Client Configuration

**CRITICAL:** The Keycloak client must be changed from PUBLIC to CONFIDENTIAL.

Access Keycloak Admin Console: http://localhost:8180 (admin/admin)

#### Navigate to: Clients → react-client

Make the following changes:

**General Settings:**
- **Client authentication**: Turn **ON** (changes from public to confidential client)

**Access Settings:**
- **Valid redirect URIs**: Change from `http://localhost:3000/*` to:
  ```
  http://localhost:8080/login/oauth2/code/keycloak
  ```
- **Valid post logout redirect URIs**:
  ```
  http://localhost:8080/*
  ```
- **Web origins**: Keep as is:
  ```
  http://localhost:3000
  ```

**Save the changes.**

#### Get Client Secret

After enabling Client authentication:
1. Go to **Credentials** tab
2. Copy the **Client secret**
3. You'll need this for the backend configuration

### 3. Configure Backend with Client Secret

Set the environment variable:

```bash
# Linux/Mac
export KEYCLOAK_CLIENT_SECRET='<your-secret-from-keycloak>'

# Windows PowerShell
$env:KEYCLOAK_CLIENT_SECRET='<your-secret-from-keycloak>'

# Windows CMD
set KEYCLOAK_CLIENT_SECRET=<your-secret-from-keycloak>
```

**Or** add it directly to `backend/src/main/resources/application.yml`:

```yaml
spring.security.oauth2.client:
  registration:
    keycloak:
      client-secret: <your-secret-here>  # NOT RECOMMENDED FOR PRODUCTION
```

**Note**: For production, always use environment variables, not hardcoded values.

### 4. Start the Services

```bash
# Terminal 1: Start Backend
cd backend
# Set the client secret environment variable first!
mvn spring-boot:run
# Or if you have mvnw: ./mvnw spring-boot:run

# Terminal 2: Start Frontend
cd frontend
npm install  # Install dependencies (oidc packages removed)
npm run dev
```

Backend should start on: http://localhost:8080
Frontend should start on: http://localhost:3000

---

## Testing the BFF Flow

### 1. Test Authentication Flow

1. **Open browser**: http://localhost:3000
2. **Click "Login with Keycloak"**
3. **Browser redirects**: Frontend → Backend → Keycloak
   - URL should change to: `http://localhost:8080/api/auth/login`
   - Then to: `http://localhost:8180/realms/demo/protocol/openid-connect/auth?...`
4. **Login with Keycloak**:
   - Username: `testadmin`
   - Password: `password`
5. **After authentication**: Keycloak → Backend → Frontend
   - Backend exchanges code for tokens (stored server-side)
   - Backend creates session and sets `BFFSESSION` cookie
   - Redirects back to: `http://localhost:3000`

### 2. Verify Session Cookie

Open Browser DevTools (F12) → Application → Cookies → `http://localhost:3000`

You should see:
- **BFFSESSION** cookie
  - HttpOnly: Yes
  - Secure: No (development) / Yes (production)
  - SameSite: Lax
- **XSRF-TOKEN** cookie (for CSRF protection)
  - HttpOnly: No (frontend needs to read it)
  - SameSite: Lax

### 3. Verify No Tokens in LocalStorage

Go to Application → Local Storage → `http://localhost:3000`

**Should be empty** (no `oidc.user` or access tokens stored)

### 4. Test API Calls

Click the API test buttons in the UI:
- **Get Hello**: `GET /api/hello`
- **Get Hello Me**: `GET /api/hello/me`
- **Get User Info**: `GET /api/hello/userinfo`
- **Perform Action**: `POST /api/hello/action`

All should work **without** seeing Authorization headers in Network tab.

### 5. Verify CSRF Protection

Open DevTools → Network → Click "Perform Action"

**Request Headers** should include:
```
X-XSRF-TOKEN: <csrf-token-value>
Cookie: BFFSESSION=...; XSRF-TOKEN=...
```

**No** `Authorization: Bearer` header!

### 6. Test Session Persistence

1. Refresh the page (F5)
2. **Should remain logged in** (session persists in Redis)
3. Check Redis:
   ```bash
   redis-cli KEYS "*"
   # Should show: "spring:session:sessions:<session-id>"
   ```

### 7. Test Logout

1. Click "Logout" button
2. Session should be invalidated
3. Cookies should be deleted
4. Redis session should be removed:
   ```bash
   redis-cli KEYS "*"
   # Should be empty or not show your session
   ```

---

## Security Improvements

### Before (Frontend OAuth2)
- ❌ Access tokens stored in localStorage (XSS risk)
- ❌ Tokens exposed to JavaScript
- ❌ Tokens visible in network tab
- ❌ Manual token refresh required
- ❌ Complex OIDC client library

### After (BFF Pattern)
- ✅ Tokens stored server-side in Redis
- ✅ HttpOnly cookies (not accessible to JavaScript)
- ✅ No tokens in localStorage
- ✅ No Authorization headers visible
- ✅ CSRF protection enabled
- ✅ Backend handles token refresh automatically
- ✅ Simple authentication context

---

## Troubleshooting

### Issue: Backend fails to start

**Error**: `client_secret is required for confidential clients`

**Solution**: Set the `KEYCLOAK_CLIENT_SECRET` environment variable.

### Issue: Login redirects to 404

**Error**: `404 Not Found` after Keycloak login

**Solution**: Check Keycloak redirect URI is set to:
```
http://localhost:8080/login/oauth2/code/keycloak
```

### Issue: CORS errors

**Error**: `Access-Control-Allow-Origin` header missing

**Solution**:
1. Check `application.yml`: `app.cors.allowed-origins` includes `http://localhost:3000`
2. Verify SecurityConfig CORS configuration has `allowCredentials: true`

### Issue: Session not persisting

**Error**: Session lost after page refresh

**Solution**:
1. Verify Redis is running: `docker ps | grep redis`
2. Check `application.yml`: `spring.session.store-type: redis`
3. Verify Redis connection: `redis-cli ping`

### Issue: CSRF token errors (403)

**Error**: `403 Forbidden` on POST requests

**Solution**:
1. Check `X-XSRF-TOKEN` header is being sent
2. Verify CSRF token cookie exists
3. Check api.service.ts CSRF handling code

### Issue: Role/Group not working

**Error**: 403 Access Denied for admin endpoints

**Solution**:
1. Verify user is in ADMIN group in Keycloak
2. Check `groups` scope is included in OAuth2 configuration
3. Verify SecurityConfig authority mapper extracts groups correctly

---

## Production Deployment Checklist

### Backend
- [ ] Set `server.servlet.session.cookie.secure=true` (requires HTTPS)
- [ ] Set `server.servlet.session.cookie.same-site=strict`
- [ ] Configure Redis with authentication: `spring.redis.password`
- [ ] Enable Redis TLS/SSL connection
- [ ] Use environment variables for all secrets
- [ ] Update CORS origins to production domain
- [ ] Set up Redis backup/replication
- [ ] Configure session timeout appropriately
- [ ] Enable Redis connection pooling

### Frontend
- [ ] Update `VITE_API_URL` to production backend URL
- [ ] Build with `npm run build`
- [ ] Serve over HTTPS
- [ ] Ensure same-origin or proper CORS configuration

### Keycloak
- [ ] Update redirect URIs to production URLs
- [ ] Use strong client secret (rotate periodically)
- [ ] Enable MFA for admin accounts
- [ ] Configure appropriate session/token timeouts
- [ ] Set up Keycloak high availability if needed

---

## Architecture Comparison

### Before
```
React App (Port 3000)
    ↓ Direct OIDC
Keycloak (Port 8180)
    ↓ JWT Token in localStorage
React App
    ↓ Bearer Token in Authorization header
Spring Boot (Port 8080)
    ↓ Validates JWT signature
API Response
```

### After (BFF)
```
React App (Port 3000)
    ↓ Session Cookie (HttpOnly)
Spring Boot BFF (Port 8080)
    ↓ OAuth2 Authorization Code Flow
Keycloak (Port 8180)
    ↓ Returns Authorization Code
Spring Boot BFF
    ↓ Exchanges Code for Token
    ↓ Stores Token in Redis Session
    ↓ Returns Session Cookie
React App
    ↓ Subsequent requests with Session Cookie
Spring Boot BFF
    ↓ Retrieves Token from Session
    ↓ Calls downstream APIs with Token
API Response
```

---

## Next Steps

1. ✅ **Test the complete flow** with the instructions above
2. ✅ **Verify security**: Check cookies, no tokens in localStorage
3. ⬜ **Update CLAUDE.md** with new BFF architecture
4. ⬜ **Update README.md** with new setup instructions
5. ⬜ **Test with multiple users** and different roles
6. ⬜ **Performance test** session management under load
7. ⬜ **Set up production environment** with proper secrets management

---

## Files Modified Summary

### Backend
- **Modified**: `pom.xml`, `application.yml`, `SecurityConfig.java`, `HelloController.java`
- **Created**: `AuthController.java`
- **Deleted**: `KeycloakJwtAuthenticationConverter.java`, `KeycloakAuthenticationEntryPoint.java`, `KeycloakAccessDeniedHandler.java`

### Frontend
- **Modified**: `package.json`, `api.service.ts`, `useApi.ts`, `main.tsx`, `LoginButton.tsx`, `UserProfile.tsx`
- **Created**: `contexts/AuthContext.tsx`
- **Deleted**: `config/auth.config.ts`, `public/silent-renew.html`

---

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Verify all setup steps were completed
3. Check logs:
   - Backend: Look for OAuth2/Spring Security logs
   - Frontend: Check browser console
   - Redis: `redis-cli MONITOR`
