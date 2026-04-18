# Phase Gate Before Next Coding

This checklist covered the MFA implementation phase and now records which parts are complete after the backend MFA delivery.

It should also continue to be used before larger later phases such as:
- assignment submission features
- hashing/signature workflows
- transport-security design and AES-based at-rest protection work
- grade-integrity logic
- audit/tamper-evidence features

## 1. Documentation gate

The next coding phase should not start until all of the following are available and reviewed:

- [x] `docs/04-evidence-testing/authentication/implementation-status-and-evidence.md`
- [x] `docs/03-features/authentication/api-auth-contract.md`
- [x] `docs/02-architecture-crypto/data-model-rationale.md`
- [x] `docs/02-architecture-crypto/uml/foundation/class-diagram.puml`
- [x] `docs/02-architecture-crypto/uml/foundation/sequence-login-secure.puml`
- [x] this phase gate document itself
- [x] relevant governance and architecture references have been checked for consistency after MFA updates

## 2. Traceability gate

Before coding MFA, document:

- [x] which asset it primarily protects: user accounts and downstream access tokens
- [x] which specific risks it mitigates: password-only compromise, credential stuffing impact, replay of single-factor login success
- [x] which cryptographic primitive(s) it uses: TOTP with HMAC, plus symmetric secret protection at rest
- [x] which CIA property it primarily supports: authentication integrity with secondary confidentiality benefit for account access
- [x] which report section and UML artefact it belongs to: auth contract, data model rationale, secure login sequence, class diagram

## 3. MFA design gate

Before implementing MFA, the following must be explicitly fixed:

- [x] factor choice for phase 1 is `TOTP` authenticator app, not SMS/email/WebAuthn
- [x] login response may branch between `AUTHENTICATED` and `MFA_REQUIRED`
- [x] JWT must be issued only after MFA verification for MFA-enabled users
- [x] TOTP secret must be protected at rest, not stored in plaintext
- [x] recovery codes are in scope and must be one-time use
- [x] a short-lived server-side `MfaChallenge` model is part of the design
- [x] challenge expiry duration is fixed in implementation notes (`300` seconds in backend properties)
- [x] allowed verification-attempt count is fixed in implementation notes (`5` attempts in backend properties)
- [x] MFA disable flow confirmation requirements are fixed in code tasks (password + valid TOTP or recovery code)

## 4. Frontend-flow gate

Before MFA coding starts, confirm:

- [x] frontend login must support a second-step challenge state
- [x] authenticated browser-session establishment must happen only after full authentication
- [x] an account-settings flow is needed for setup/enable/disable/status
- [ ] exact screen/state naming for the MFA challenge UI is agreed
- [ ] recovery-code copy/download UX is agreed

Backend status note:
- the backend contract for these frontend states is now implemented and tested

## 5. Testing gate

Implemented backend evidence now covers:

- [x] success test for login without MFA enabled
- [x] success test for login with MFA enabled and valid TOTP
- [x] test that login returns `MFA_REQUIRED` before token issuance
- [x] invalid-code negative test
- [x] expired-challenge negative test
- [x] recovery-code one-time-use test
- [x] disable-MFA verification test
- [ ] report-reusable evidence to capture (responses, screenshots, sample logs)

## 6. Scope-control gate

Confirm before coding:

- [x] the next phase is still small enough for a study project
- [x] the chosen MFA approach fits the existing Spring/JWT architecture
- [x] no new factor has been added only because it is technically interesting
- [x] the auth change has a documented security reason rather than being a generic feature add-on
- [x] unrelated auth redesigns are excluded from the first MFA implementation

## 7. Go / No-Go rule

### Go
The backend MFA phase is ready to build on because the flow can now be explained clearly from the contract, data model, diagrams, code, and integration tests.

### No-Go
If the MFA implementation still depends on unresolved challenge lifecycle rules, secret-protection rules, or frontend state ambiguity, stop and document those details first.

