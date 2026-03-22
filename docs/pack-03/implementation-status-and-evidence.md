# Implementation Status and Evidence

This document is now **auth-focused** for the implemented MFA phase. Its goal is to keep the authentication documentation aligned with the real backend code.

## 1. Current authentication status in code

The current authentication implementation in `backend/` is now a **password + JWT baseline extended with optional TOTP MFA**, with the JWT transported to browser clients in an `HttpOnly` authentication cookie.

Implemented auth areas:
- public registration endpoint
- public login endpoint
- protected current-user endpoint
- `bcrypt` password hashing through Spring Security
- JWT issuance at registration and login for non-MFA users
- JWT validation through `JwtAuthenticationFilter`
- HttpOnly auth-cookie issuance and clearing
- production-safe cookie configuration and startup validation
- RBAC-backed authenticated access for the rest of the API
- optional TOTP MFA enrollment and enablement
- MFA challenge verification endpoint
- one-time recovery codes
- MFA-specific JWT auth-context claims (`mfa`, `amr`)
- login branching between `AUTHENTICATED` and `MFA_REQUIRED`

Important clarification:
- the wider codebase already contains features beyond the original auth foundation
- this document remains intentionally limited to authentication state so the MFA implementation can be evidenced clearly

## 2. Auth-related backend structure currently in use

### Config and security
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/java/edusecure/edusecure/config/AuthCookieProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/CorsProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/AuthCookieConfigurationValidator.java`

### Controllers
- `backend/src/main/java/edusecure/edusecure/controller/AuthController.java`
- `backend/src/main/java/edusecure/edusecure/controller/SystemController.java`

### Auth and security services
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthTokenService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`
- `backend/src/main/java/edusecure/edusecure/security/CustomUserDetailsService.java`
- `backend/src/main/java/edusecure/edusecure/security/JwtService.java`
- `backend/src/main/java/edusecure/edusecure/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`

### Auth DTOs
- `backend/src/main/java/edusecure/edusecure/dto/RegisterRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/LoginRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/AuthResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/AuthStatus.java`
- `backend/src/main/java/edusecure/edusecure/dto/CurrentUserResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaStatusResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaSetupResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaEnableRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaEnableResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaVerifyRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/MfaDisableRequest.java`

### Identity model relevant to auth
- `backend/src/main/java/edusecure/edusecure/entity/User.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaMethod.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaChallenge.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaRecoveryCode.java`
- `backend/src/main/java/edusecure/edusecure/entity/Role.java`
- `backend/src/main/java/edusecure/edusecure/entity/RoleName.java`

### Persistence relevant to auth
- `backend/src/main/java/edusecure/edusecure/repository/UserRepository.java`
- `backend/src/main/java/edusecure/edusecure/repository/RoleRepository.java`
- `backend/src/main/java/edusecure/edusecure/repository/MfaChallengeRepository.java`
- `backend/src/main/java/edusecure/edusecure/repository/MfaRecoveryCodeRepository.java`

## 3. Auth endpoint status

| Endpoint | Method | Status | Notes |
|---|---|---|---|
| `/api/system/health` | GET | Implemented | Public health/status endpoint |
| `/api/auth/register` | POST | Implemented | Public registration; assigns default `STUDENT` role and sets auth cookie on success |
| `/api/auth/login` | POST | Implemented | Public login; sets auth cookie for non-MFA users or returns `MFA_REQUIRED` challenge for MFA-enabled users |
| `/api/auth/logout` | POST | Implemented | Clears the auth cookie |
| `/api/auth/me` | GET | Implemented | Requires authenticated cookie-backed session |
| `/api/auth/mfa/status` | GET | Implemented | Return current user MFA state |
| `/api/auth/mfa/setup` | POST | Implemented | Begin TOTP enrollment |
| `/api/auth/mfa/enable` | POST | Implemented | Confirm first TOTP code and enable MFA |
| `/api/auth/mfa/verify` | POST | Implemented | Finish login for MFA-enabled users and set auth cookie |
| `/api/auth/mfa/disable` | POST | Implemented | Disable MFA after password + second-factor re-verification |

## 4. Security behaviour currently implemented

### Password handling
- passwords are not stored in plaintext
- `BCryptPasswordEncoder` is used for password hashing
- the `User` entity stores `passwordHash`
- registration currently requires passwords to be at least 8 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character
- invalid auth request payloads now return a structured validation error body rather than only a bare `400` status

### Session/authentication model
- the API uses stateless request handling after authentication
- registration and completed login flows mint a JWT with auth-context claims
- browser clients receive that JWT in an `HttpOnly` auth cookie rather than reading it in JavaScript
- login returns an authenticated cookie only when authentication is fully complete
- MFA-enabled accounts receive a short-lived server-side challenge instead of an immediate authenticated cookie
- protected endpoints are primarily reached through the cookie-backed session established by the browser
- logout clears the cookie with `Set-Cookie`/`Max-Age=0`
- the JWT mechanism is implemented as an auth/session layer, not as a confidentiality mechanism

### Cookie-hardening behaviour
- auth cookies are always `HttpOnly`
- default development cookie settings are `SameSite=Lax`, `Path=/`, and `Secure=false`
- production deployments can enable `Secure=true`, optional `Domain`, and explicit allowed frontend origins via configuration
- startup validation rejects unsafe combinations such as `SameSite=None` with `Secure=false`
- startup validation also rejects running the `prod` profile with `auth.cookie.secure=false`

### MFA-specific security behaviour
- TOTP is the only MFA method currently implemented
- MFA secrets are encrypted at rest under a dedicated symmetric key
- recovery codes are stored hashed and consumed once
- successful MFA verification issues a JWT with `mfa=true` and `amr=[pwd, otp]`
- disabling MFA requires the current password plus a valid TOTP or recovery code

### Access control baseline
- `/api/system/health` is public
- `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, and `/api/auth/mfa/verify` are public
- other endpoints are authenticated by default in the current security config

### Validation-response consistency
- `register`, `login`, `mfa/enable`, `mfa/verify`, and `mfa/disable` all use the same `ValidationErrorResponse` shape for request-body input errors
- malformed auth request bodies are also normalised into the same top-level response pattern with `_global` error entries
- auth-domain business failures such as invalid credentials, duplicate registration, invalid MFA codes, expired challenges, and conflict states also use the same top-level envelope with `_global` error entries

## 5. MFA design now implemented in code

The current auth implementation now includes:
- optional MFA per user account
- TOTP authenticator app as the first and only MFA method in phase 1
- no JWT issuance before MFA verification for MFA-enabled users
- server-side short-lived MFA challenge records
- hashed one-time recovery codes
- encrypted-at-rest TOTP secret storage

## 6. Current test evidence for the implemented auth baseline

### Health and foundation test
- `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`

Evidence currently covered:
- application context loads
- public health endpoint returns success

### Auth integration tests
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`

Evidence currently covered:
- registration succeeds
- registration rejects passwords that are too short
- registration rejects passwords that do not include an uppercase letter
- registration rejects passwords that do not include a lowercase letter
- registration rejects passwords that do not include a number
- registration rejects passwords that do not include a special character
- registration returns structured field-level validation messages for weak passwords
- login returns structured field-level validation messages for invalid request bodies
- login returns a structured auth error envelope for invalid credentials
- registration sets the auth cookie
- `/api/auth/me` works with the issued auth cookie
- login succeeds
- login sets `HttpOnly`, `Path=/`, and `SameSite=Lax` cookie attributes under development defaults
- logout clears the auth cookie
- `/api/auth/me` rejects unauthenticated access
- duplicate registration returns conflict

### MFA integration tests
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

Evidence currently covered:
- MFA status is disabled by default for new users
- MFA setup returns TOTP enrollment material
- MFA enablement succeeds with a valid TOTP code
- login returns `MFA_REQUIRED` for MFA-enabled accounts
- `/api/auth/mfa/verify` sets the authenticated cookie only after valid TOTP verification
- consumed challenge reuse is rejected
- recovery code can be used exactly once
- invalid TOTP is rejected while the challenge remains usable within limits
- expired challenge is rejected
- disabling MFA requires password + valid second factor
- `mfa/enable`, `mfa/verify`, and `mfa/disable` return the same structured validation format for request-body input errors
- malformed `mfa/verify` request bodies are normalised into the same response envelope
- MFA business failures such as invalid codes, expired challenges, and MFA conflict states return the same shared auth error envelope

### Cookie-configuration validation tests
- `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationValidatorTests.java`
- `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationStartupValidationTests.java`

Evidence currently covered:
- local-development cookie defaults are accepted
- `SameSite=None` with `Secure=false` is rejected
- valid secure production cookie combinations are accepted
- the Spring context fails fast when the `prod` profile is combined with `auth.cookie.secure=false`

## 7. Report value of the current auth implementation

The current implemented auth slice already supports:
- the plaintext-password mitigation narrative
- the secure login design discussion
- the class diagram and secure login sequence baseline
- the implementation-plan section for auth, bcrypt, JWT-based stateless security, and hardened browser cookie transport

The authentication slice now supports both the original password-security narrative and a stronger login-assurance narrative through optional MFA, while still preserving the existing password-only flow for users who have not enabled MFA.

