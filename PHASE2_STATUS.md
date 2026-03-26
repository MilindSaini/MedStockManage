# MedStock Phase 2 Build Status

Phase 2 has been implemented in code for both backend and frontend.

## Backend completed
- JWT RS256 utility with access token (15 min) and refresh token (7 days)
- Security filter chain with JWT filter and stateless auth
- `UserDetailsService` loading by username or email
- `AuthService` methods: register, login, refreshToken, logout, me, google login flow
- `AuthController` with 7 endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
  - `GET /api/auth/oauth2/google-url`
  - `GET /api/auth/health`
- Google OAuth2 success handler redirects to frontend with `token` and `refreshToken`

## Frontend completed
- `AuthContext` with in-memory token handling (no localStorage)
- Axios interceptor attaches bearer token and refreshes once on 401
- Login page with two tabs: Password and Google
- Register page with required fields and "30 days free" badge
- Protected route with role-based redirect to the correct dashboard

## Validation
- Backend `mvn -DskipTests compile` passed
- Frontend `npm run build` passed

## Configure before running auth in real mode
- Set `.env` values for:
  - `MEDSTOCK_JWT_PRIVATE_KEY`, `MEDSTOCK_JWT_PUBLIC_KEY`
  - `MEDSTOCK_GOOGLE_CLIENT_ID`, `MEDSTOCK_GOOGLE_CLIENT_SECRET`
  - `MEDSTOCK_OAUTH2_SUCCESS_REDIRECT`
- Google Console redirect URI:
  - `http://localhost:8080/login/oauth2/code/google`

## RSA key generation reference
```
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```
Put PEM values into `.env` (escape newlines if needed).
