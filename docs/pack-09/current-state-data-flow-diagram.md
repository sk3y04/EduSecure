# Current-State Data Flow Diagram

This note adds a **proper data flow diagram (DFD)** for the implemented EduSecure repository state.

It is intended to complement the earlier UML packs rather than replace them.

## 1. Why this note exists

The repository already contained:
- UML deployment diagrams
- UML sequence diagrams
- implementation evidence notes for auth, submissions, grades, and audit integrity

What it did **not** contain was a dedicated DFD showing:
- external entities
- major trust boundaries
- the main application processes
- persistent data stores
- the movement of sensitive data between them

That gap matters because the final report may benefit from one diagram that explains the implemented security boundary in a simpler way than the sequence diagrams.

## 2. Included DFD artefacts

The current Pack 09 DFD set is:
- `docs/pack-09/uml/dfd-context-current-state.puml`
- `docs/pack-09/uml/dfd-level-1-current-state.puml`

### A. Context DFD
Shows EduSecure as one system process interacting with:
- student browser clients
- lecturer browser clients for owned submissions, grades, and spaces
- admin browser clients for global oversight
- a TOTP authenticator app

### B. Level-1 DFD
Breaks the implemented system into the main backend-facing responsibilities:
- auth and session establishment
- MFA challenge handling
- submission protection and retrieval
- grade integrity handling
- space ownership and membership handling
- append-oriented audit recording

## 3. Scope and trust-boundary rules

This DFD is intentionally aligned to the **implemented** repository state and current safe claims.

### What it shows
- browser clients communicating with the application over the deployment transport boundary
- backend-issued `HttpOnly` cookie session transport for browser authentication
- optional MFA with encrypted-at-rest TOTP secrets, challenge records, and hashed recovery codes
- student-uploaded UTF-8 text submission content entering the backend, then being hashed, signature-processed, encrypted at rest, and split between metadata storage and ciphertext storage
- owner-scoped lecturer access to submissions, grades, and spaces with admin override
- grade creation/update constrained by previously verified submissions
- audit events written through an HMAC-backed append-oriented integrity chain

### What it does not claim
- client-side signing by end users
- browser-side JWT storage
- enterprise KMS or HSM integration
- end-to-end encryption between browser clients and lecturers
- revival of the removed Pack 08 standalone symmetric-transport flow

## 4. Current implementation evidence this DFD maps to

### Runtime topology
- `compose.yaml`

### Frontend browser/session behavior
- `frontend/src/services/http.ts`
- `frontend/src/stores/auth.ts`

### Auth and MFA
- `backend/src/main/java/edusecure/edusecure/controller/auth/AuthController.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`

### Submission integrity and confidentiality
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `docs/pack-06/submission-content-protection-and-retrieval.md`

### Grade integrity
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`
- `docs/pack-07/grade-phase-status-and-evidence.md`

### Audit integrity
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`

## 5. Recommended report wording

Safe wording:

> The DFD represents the implemented EduSecure data movement and trust boundaries at a high level. It supplements the earlier UML diagrams by showing where sensitive data enters the system, where it is transformed by cryptographic controls, and where metadata, ciphertext, grades, and audit records are stored.

Unsafe wording to avoid:
- "The DFD proves end-to-end encryption between all actors."
- "The DFD shows a client-side digital-signature PKI deployment."
- "The browser stores the JWT and replays it manually."
- "The old AES transmission demo is still the live secure-transport mechanism."

## 6. Practical use in the report

Use the DFD when you want to explain:
- where authentication data moves
- where MFA data is stored and checked
- where uploaded submission text becomes integrity metadata and ciphertext
- why the submission ciphertext store is separate from PostgreSQL metadata
- how grade operations and audit integrity fit into the same system boundary

Use the existing sequence diagrams when you need step-by-step interaction detail.

