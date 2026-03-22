# API Auth Contract

This document now records the **currently implemented** authentication contract in `backend/`.

The backend implements:
- password-based authentication with bcrypt-hashed passwords
- JWT-based stateless API authentication transported in an HttpOnly cookie after successful login
- optional TOTP-based MFA with recovery codes

Status markers used below:
- **Implemented** = behaviour already present in the codebase today
- **Deferred** = intentionally out of scope for the current implementation

## 1. Auth scope and chosen MFA approach

Base path: `/api/auth`

The current auth-hardening approach uses:
- factor 1: email + password
- factor 2: TOTP code from an authenticator app
- enrollment model: user opts in after account creation/login
- session model: JWT is issued **only after full authentication is complete** and is returned to the browser in an `HttpOnly` auth cookie

Deferred from the current MFA phase:
- SMS OTP
- email OTP
- WebAuthn/passkeys
- organisation-wide mandatory MFA rollout rules

Those may be considered later, but the first implementation should stay small and align with the current Spring/JWT architecture.

## 2. Endpoint inventory

| Endpoint | Method | Authentication required | Status | Purpose |
|---|---|---|---|---|
| `/register` | POST | No | Implemented | Create a new user with default `STUDENT` role |
| `/login` | POST | No | Implemented | Authenticate password; either return JWT immediately or return MFA challenge |
| `/logout` | POST | No | Implemented | Clear the auth cookie in the browser |
| `/me` | GET | Yes | Implemented | Return current authenticated user details |
| `/mfa/status` | GET | Yes | Implemented | Return MFA status for the current authenticated user |
| `/mfa/setup` | POST | Yes | Implemented | Generate TOTP enrollment material for the current user |
| `/mfa/enable` | POST | Yes | Implemented | Verify first TOTP code and enable MFA |
| `/mfa/verify` | POST | No | Implemented | Complete login for users who received an MFA challenge |
| `/mfa/disable` | POST | Yes | Implemented | Disable MFA after password + MFA confirmation |

Additional system endpoint already implemented:

| Endpoint | Method | Authentication required | Purpose |
|---|---|---|---|
| `/api/system/health` | GET | No | Return service status |

## 3. Current implemented baseline

## A. `POST /api/auth/register` — Implemented

### Request body
```json
{
  "email": "student@example.com",
  "password": "StrongPass123!",
  "fullName": "Student Example"
}
```

### Validation rules
- `email` must be present and valid
- `password` must be present, at least 8 characters, and include at least one uppercase letter, one lowercase letter, one number, and one special character
- `fullName` must be present

### Success response
Status: `201 Created`

```json
{
  "authStatus": "AUTHENTICATED",
  "userId": "uuid-value",
  "email": "student@example.com",
  "fullName": "Student Example",
  "roles": ["STUDENT"],
  "token": null,
  "mfaEnabled": false,
  "amr": ["pwd"],
  "challengeId": null,
  "mfaMethod": null,
  "expiresAt": null,
  "remainingAttempts": null
}
```

### Failure cases
- `409 Conflict` if the email is already registered, using the shared auth error envelope
- `400 Bad Request` for validation failure, with field-level error details
- `500 Internal Server Error` if the default role is unexpectedly missing

### Auth cookie side effect
The response also sets `Set-Cookie` for the auth JWT with:
- `HttpOnly`
- `Path=/`
- configurable `SameSite`
- configurable `Secure`
- optional configured `Domain`

### Example validation failure response
```json
{
  "message": "Validation failed",
  "errors": {
    "password": [
      "Password must be at least 8 characters",
      "Password must include at least one uppercase letter",
      "Password must include at least one number",
      "Password must include at least one special character"
    ]
  }
}
```

## B. `POST /api/auth/login` — Implemented current behaviour

### Request body
```json
{
  "email": "student@example.com",
  "password": "StrongPass123!"
}
```

### Success response when MFA is not enabled
Status: `200 OK`

```json
{
  "authStatus": "AUTHENTICATED",
  "userId": "uuid-value",
  "email": "student@example.com",
  "fullName": "Student Example",
  "roles": ["STUDENT"],
  "token": null,
  "mfaEnabled": false,
  "amr": ["pwd"],
  "challengeId": null,
  "mfaMethod": null,
  "expiresAt": null,
  "remainingAttempts": null
}
```

### Success response when MFA is enabled
Status: `200 OK`

```json
{
  "authStatus": "MFA_REQUIRED",
  "userId": null,
  "email": null,
  "fullName": null,
  "roles": null,
  "token": null,
  "mfaEnabled": true,
  "amr": null,
  "challengeId": "uuid-value",
  "mfaMethod": "TOTP",
  "expiresAt": "2026-03-15T12:00:00Z",
  "remainingAttempts": 5
}
```

### Failure cases
- `400 Bad Request` for validation failure, with field-level error details
- authentication failure produces a `401 Unauthorized` response using the shared auth error envelope

### Auth cookie side effects
- password-only success sets the auth JWT in an `HttpOnly` cookie
- MFA challenge responses clear any previous auth cookie

## C. `GET /api/auth/me` — Implemented

### Authentication transport
Browser requests are authenticated by the `HttpOnly` auth cookie. The frontend sends credentialed
requests and does not read the JWT directly.

### Success response
Status: `200 OK`

```json
{
  "userId": "uuid-value",
  "email": "student@example.com",
  "fullName": "Student Example",
  "roles": ["STUDENT"]
}
```

### Failure cases
- `401 Unauthorized` if no valid auth cookie is supplied
- `404 Not Found` if the JWT subject no longer exists in persistence

## 4. Implemented MFA contract

### Shared validation failure response for auth request bodies

The following auth endpoints now use the same structured validation response pattern when request-body validation fails:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/mfa/enable`
- `POST /api/auth/mfa/verify`
- `POST /api/auth/mfa/disable`

Standard shape:

```json
{
  "message": "Validation failed",
  "errors": {
    "fieldName": ["validation message"],
    "_global": ["Request body is malformed or contains invalid values"]
  }
}
```

Notes:
- field-keyed entries are returned for bean-validation failures such as blank or missing values
- `_global` is used when the request body is malformed or contains values that cannot be parsed correctly, for example an invalid UUID string

### Shared non-validation auth failure response

Non-validation auth/business failures now use the same top-level envelope, but place their detail in `_global`.

```json
{
  "message": "Invalid credentials",
  "errors": {
    "_global": ["Invalid credentials"]
  }
}
```

This applies to auth-domain outcomes such as:
- duplicate registration conflict
- invalid credentials
- invalid MFA verification code
- invalid or expired MFA challenge
- MFA conflict states such as trying to set up MFA when it is already enabled
- MFA attempt-limit failures

## A. Login response branching

`POST /api/auth/login` will keep the same request body, but its success response becomes one of two valid shapes.

### A1. Password-only success path

Used when MFA is not enabled for the user.

```json
{
  "authStatus": "AUTHENTICATED",
  "userId": "uuid-value",
  "email": "student@example.com",
  "fullName": "Student Example",
  "roles": ["STUDENT"],
  "token": null,
  "mfaEnabled": false,
  "amr": ["pwd"]
}
```

The auth JWT is set in the response `Set-Cookie` header rather than exposed to frontend JavaScript.

### A2. MFA challenge path

Used when password verification succeeds and the user has MFA enabled.

```json
{
  "authStatus": "MFA_REQUIRED",
  "challengeId": "uuid-value",
  "mfaMethod": "TOTP",
  "expiresAt": "2026-03-15T12:00:00Z",
  "remainingAttempts": 5
}
```

Rules:
- no bearer token is returned in the `MFA_REQUIRED` branch
- the challenge must be short-lived
- password failure and unknown-user failure should remain indistinguishable to the client

## B. `GET /api/auth/mfa/status` — Implemented

### Authentication transport
Requires the authenticated `HttpOnly` auth cookie.

### Success response
```json
{
  "mfaEnabled": true,
  "mfaMethod": "TOTP",
  "recoveryCodesRemaining": 8,
  "enabledAt": "2026-03-15T11:20:00Z"
}
```

Purpose:
- support account-settings UI
- allow frontend to display whether MFA is configured
- avoid overloading `/me` with setup-specific detail

## C. `POST /api/auth/mfa/setup` — Implemented

### Authentication transport
Requires the authenticated `HttpOnly` auth cookie.

### Request body
No body required for the first version.

### Success response
```json
{
  "mfaMethod": "TOTP",
  "manualEntryKey": "JBSWY3DPEHPK3PXP",
  "otpauthUri": "otpauth://totp/EduSecure:student%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=EduSecure&digits=6&period=30"
}
```

Rules:
- setup should be idempotent only until MFA is enabled or setup is restarted
- the server must not expose the decrypted secret again after MFA is fully enabled unless setup is explicitly reset

## D. `POST /api/auth/mfa/enable` — Implemented

### Authentication transport
Requires the authenticated `HttpOnly` auth cookie.

### Request body
```json
{
  "verificationCode": "123456"
}
```

### Success response
```json
{
  "mfaEnabled": true,
  "mfaMethod": "TOTP",
  "recoveryCodes": [
    "A1B2-C3D4",
    "E5F6-G7H8",
    "J9K0-L1M2"
  ]
}
```

Rules:
- MFA becomes active only after a valid first TOTP verification
- recovery codes are returned only once in plaintext
- stored recovery codes must be hashed, not stored in plaintext

## E. `POST /api/auth/mfa/verify` — Implemented

### Request body
```json
{
  "challengeId": "uuid-value",
  "verificationCode": "123456"
}
```

### Success response
Status: `200 OK`

```json
{
  "authStatus": "AUTHENTICATED",
  "userId": "uuid-value",
  "email": "student@example.com",
  "fullName": "Student Example",
  "roles": ["STUDENT"],
  "token": null,
  "mfaEnabled": true,
  "amr": ["pwd", "otp"]
}
```

### Auth cookie side effect
Successful verification sets the auth JWT in an `HttpOnly` auth cookie.

### Failure cases
- `400 Bad Request` for validation failure or malformed request body, using the shared structured validation response
- `401 Unauthorized` for invalid code, using the shared auth error envelope
- `410 Gone` for expired or already consumed challenge, using the shared auth error envelope
- `429 Too Many Requests` if the challenge exceeds the allowed verification attempts, using the shared auth error envelope

## F. `POST /api/auth/mfa/disable` — Implemented

### Authentication transport
Requires the authenticated `HttpOnly` auth cookie.

### Request body
```json
{
  "password": "StrongPass123!",
  "verificationCode": "123456"
}
```

### Success response
Status: `204 No Content`

Rules:
- disabling MFA should require the current password plus a current MFA code or valid recovery code
- disabling MFA should invalidate any pending MFA setup/challenge state for that user

### Failure cases
- `400 Bad Request` for validation failure or malformed request body, using the shared structured validation response
- `401 Unauthorized` if the supplied password or verification code is not valid, using the shared auth error envelope

## G. `POST /api/auth/logout` — Implemented

### Request body
No body required.

### Success response
Status: `204 No Content`

### Auth cookie side effect
The response clears the auth cookie by returning `Set-Cookie` with `Max-Age=0`.

## 5. Security expectations for the MFA phase

### Password storage
Implemented baseline:
- passwords are hashed using bcrypt before persistence
- plaintext passwords are not stored in the `User` entity

### MFA secret handling
Current rule:
- the TOTP secret must not be stored in plaintext
- the stored secret should be encrypted at rest using an application-managed symmetric key kept outside source control
- MFA setup material must be visible to the user only during enrollment

### Recovery code handling
Current rule:
- recovery codes are one-time backup secrets
- stored recovery codes must be hashed
- the plaintext set is shown only once after generation

### Token handling
Current rule:
- JWTs are issued at registration and at login completion
- JWTs are transported in an `HttpOnly` auth cookie for protected browser requests
- JWTs support stateless authentication only
- for MFA-enabled users, JWTs are issued only after `/mfa/verify` succeeds
- the final JWT should carry authentication-context claims such as `amr` and/or `mfa=true`
- no pre-auth bearer token should be issued just to carry the MFA challenge state

Deployment note:
- development defaults use `SameSite=Lax` and `Secure=false` for localhost
- production should enable the `prod` profile and run with `Secure=true`
- cross-site SPA/API deployments should use `SameSite=None` together with `Secure=true`
- `Domain` should remain unset unless a specific shared domain scope is required

Important report note:
- JWT must not be described as encryption of user data
- transport confidentiality still depends on HTTPS/TLS in the secure system design

## 6. Frontend implications

Frontend behaviour should be designed around two login outcomes:

1. **Immediate auth success**
   - let the browser store the returned `HttpOnly` auth cookie
   - call `/api/auth/me` to restore session on refresh

2. **MFA required**
   - do not store an auth token in JavaScript
   - move the user to a second-factor screen
   - submit the code to `/api/auth/mfa/verify`
   - rely on the browser-set auth cookie only after successful MFA verification

Additional UI requirements for the MFA phase:
- account settings screen for MFA status/enrollment
- QR/manual-code presentation for setup
- one-time display and download/copy flow for recovery codes
- clear expiry/error handling for invalid or expired MFA challenges

## 7. Scope note

This document is now aligned with the implemented backend auth and MFA behaviour. It should still avoid speculative changes to unrelated submission, grading, or signature APIs until those phases are separately documented.

