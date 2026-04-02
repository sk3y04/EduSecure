# Implementation Plan and Considerations

This document describes how the EduSecure design is being implemented in a Java/Spring Boot study-project artefact, while also recording the remaining planned cryptography phases.

## Current implementation status

The backend has already completed an initial realignment, auth-foundation phase, and MFA hardening phase. At the time of writing, the implemented baseline includes:

- simplified Spring Boot dependencies for REST, Security, Validation, JPA, and PostgreSQL
- Liquibase-backed schema definition and runtime validation against PostgreSQL mappings
- a public `/api/system/health` endpoint
- `User`, `Role`, and `RoleName` identity types
- MFA-related identity state, challenge, and recovery-code types
- repository-backed user lookup
- `BCryptPasswordEncoder`-based password hashing
- `AuthController`, `AuthService`, `MfaService`, `AuthTokenService`, `TotpService`, `CustomUserDetailsService`, `JwtService`, and `JwtAuthenticationFilter`
- public `/api/auth/register` and `/api/auth/login`
- `/api/auth/mfa/status`, `/api/auth/mfa/setup`, `/api/auth/mfa/enable`, `/api/auth/mfa/verify`, and `/api/auth/mfa/disable`
- protected `/api/auth/me`
- integration tests for health, auth, and MFA flow
- a dedicated PostgreSQL/Testcontainers smoke test for Liquibase-backed schema delivery

For authentication specifically, the current code now supports both:
- password-only login for users who have not enabled MFA
- TOTP-based MFA with recovery codes for users who have enabled MFA

The remaining sections describe the design direction that extends this implemented baseline into the later cryptography artefact phases.

Important process note:
- further feature coding should now pause until the Pack 03 documentation catch-up and phase gate are reviewed

## 1. Intended implementation style

EduSecure should be implemented as a simple layered application:

- Vue.js frontend
- Spring Boot REST backend
- PostgreSQL database
- dedicated cryptographic service layer

This keeps the implementation explainable and compatible with the structural patterns observed in `Christialattion/`.

## 2. Planned backend module structure

Suggested packages:

- `config`
- `controller`
- `service`
- `service.crypto`
- `security`
- `entity`
- `dto`
- `repository`
- `audit`

This separation helps document where cryptographic code lives and avoids mixing crypto operations directly into controllers.

Implemented so far:
- `config`
- `controller`
- `dto`
- `entity`
- `repository`
- `security`
- `service.auth`
- implemented `audit`, `service.grade`, `service.submission`, and `service.crypto`

## 3. Library and framework choices

## Core Spring stack

Planned essentials:
- Spring Boot Web / REST
- Spring Security
- Spring Validation
- Spring Data JPA
- PostgreSQL driver

## Password hashing

Use:
- Spring Security `PasswordEncoder`
- `BCryptPasswordEncoder`

## Java cryptography APIs

Use standard Java cryptography architecture classes where possible:
- `SecureRandom`
- `MessageDigest`
- `Mac`
- `Cipher`
- `KeyGenerator`
- `KeyPairGenerator`
- `Signature`
- key specifications from `java.security` and `javax.crypto`

## Session handling

Optional, if retained in final architecture:
- JWT handling library for stateless REST authentication

Important note:
- JWT is an application-session convenience, not the core cryptographic artefact focus

Current status:
- JWT handling is already implemented for the auth baseline
- it is used only for stateless API authentication support
- it must still be described in the report as a signed token mechanism, not as encryption

## MFA handling in the current auth phase

Implemented choice:
- optional TOTP-based MFA using authenticator apps

Why this is the preferred first MFA step:
- it fits a study-project scope better than SMS or email delivery infrastructure
- it strengthens account authentication without replacing the current Spring Security password flow
- it avoids the much larger implementation and UX surface of WebAuthn/passkeys

Implemented shape:
- keep email + password as factor 1
- if MFA is disabled, establish the authenticated browser session immediately after successful login
- if MFA is enabled, return a short-lived challenge instead of an authenticated session cookie
- issue the JWT only after successful TOTP verification and transport it to browser clients in the auth cookie
- generate one-time recovery codes during MFA enablement

Current technical notes:
- prefer a small, focused implementation rather than introducing a large identity framework
- TOTP secret generation should use secure randomness
- the TOTP secret should be encrypted at rest under a separate application-managed key
- recovery codes should be hashed because they are backup secrets, not reusable seeds
- the final JWT should include auth-context data such as `amr` and/or `mfa=true`
- browser-facing frontend code should not persist the JWT in JavaScript-accessible storage

## 4. Planned cryptographic implementation mapping

| Requirement | Implemented / selected implementation |
|---|---|
| secure password storage | `BCryptPasswordEncoder` |
| MFA hardening for login | RFC 6238-style `TOTP` verifier with short-lived server-side challenge state |
| hashing for integrity | `MessageDigest` with `SHA-256` |
| HMAC integrity | `Mac` with `HmacSHA256` |
| symmetric encryption demo | `Cipher` with AES-GCM |
| digital signature generation/verification | `Signature` with ECC + SHA-256 (`SHA256withECDSA`) |

## 5. Randomness and key generation

### Random number generation
Use `SecureRandom` for:
- AES key generation
- IV/nonce generation
- RSA keypair generation support
- any token/entropy-related security inputs not handled by framework defaults

### Why this matters
Weak randomness can undermine otherwise strong algorithms. This must be stated clearly in the report.

## 6. Nonce / IV handling

For `AES-GCM`:
- generate a fresh nonce/IV for every encryption operation
- never reuse a nonce with the same key
- store or transmit the nonce alongside the ciphertext as needed
- document the format clearly in the artefact

Why this matters:
- nonce reuse in GCM can catastrophically weaken confidentiality and integrity

## 7. Key handling assumptions

## Password hashes
- stored in the database
- no plaintext password storage
- separate application-managed salt field is not required when using bcrypt in its standard form

## TOTP secret, if MFA is used
- store encrypted at rest rather than plaintext
- do not reuse the AES demo key for MFA secret protection
- keep the MFA secret-encryption key externalised in environment configuration
- treat recovery codes as separate secrets and store only their hashes

## ECC keypairs for digital signatures
- private key should remain server-side for system-controlled demonstrations unless a user-specific signing flow is explicitly simulated
- if student-side authorship is simulated, document that the private key represents the student's controlled signing identity within the study-project model
- public verification key may be stored and associated with the signer identity

## HMAC secret
- should be externalised via environment configuration for development
- should not be hardcoded into source code

## JWT secret, if JWT is used
- externalise via environment variables or secure config
- keep separate from HMAC audit secrets and asymmetric signing material
- for browser clients, prefer backend-issued `HttpOnly` cookie transport rather than frontend-managed bearer-token storage

## 8. Error handling and security hygiene

The implementation should:
- validate requests with DTO constraints
- avoid leaking stack traces or cryptographic internals in API responses
- return clear but non-sensitive error messages
- separate authentication failures from internal server errors
- avoid logging plaintext secrets, passwords, keys, or sensitive payloads
- avoid establishing an authenticated session before a required MFA step is completed

## 9. Database and secure coding considerations

Cryptography alone does not prevent SQL injection. Therefore:
- prefer Spring Data JPA repositories
- use validated DTOs
- minimise custom dynamic query building
- enforce RBAC at controller/service layers
- keep grade-changing operations auditable

Current implementation note:
- runtime schema delivery is now represented through Liquibase changelogs rather than relying on `spring.jpa.hibernate.ddl-auto=update`
- Hibernate is now used to validate mappings against the migrated schema
- the default fast test path still uses H2 for speed, while a dedicated PostgreSQL/Testcontainers smoke test provides real-database verification for the Liquibase baseline
- this improves reproducibility, but it should not be described as full production database hardening

## 10. Artefact flow recommendation

The first demonstration-friendly implementation path should be:

1. user registration/login with bcrypt-protected passwords
2. MFA hardening for login using TOTP and recovery codes
3. secure message/file encryption demo using AES-GCM
4. assignment signature generation and verification using ECC + SHA-256
5. grade-change audit entry protected by SHA-256/HMAC integrity support

Current progress against this sequence:
- step 1 is now implemented as the foundation phase
- step 2 is now implemented and integration-tested
- steps 3 to 5 are now implemented in the current backend artefact

## 11. Build-direction note

`backend/build.gradle` currently includes advanced scaffold features such as LDAP, SAML2, OAuth2 server/client/resource-server support, WebAuthn, GraphQL, and Modulith.

Before implementation accelerates, the backend stack should be simplified so the code matches this study-project plan rather than an unrelated advanced scaffold.

## 12. Implementation-quality checklist

Before a cryptographic feature is considered complete, confirm:

- the control maps to a documented risk
- the control maps to a CIA objective
- algorithm choice is justified in the report
- key/nonce handling is documented
- the code path is testable and demonstrable
- the output can be explained with screenshots or sample runs if needed

