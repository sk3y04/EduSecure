# Vue Frontend MVP and API Integration Plan

This document now records the **implemented frontend MVP baseline** together with the remaining optional Vue.js work, so the frontend stays a small support layer rather than a second large project.

## 1. Current situation

The main implemented artefact is still backend-heavy, but the workspace root now **does** contain a dedicated EduSecure frontend project under `frontend/`.

That frontend should still remain carefully scoped so it supports the report and evidence pack without turning into a second large product.

## 2. Frontend goal

The Vue frontend exists to:
- demonstrate the backend workflows clearly
- provide screenshots/evidence for the report
- avoid unnecessary UI complexity

It should not become a large polished product.

## 3. Recommended MVP scope

### Phase A: auth baseline
- login page
- cookie-backed auth state handling with `/api/auth/me` bootstrap
- role-aware route guard
- current-user fetch on app load
- MFA challenge handling

### Phase B: assignment and submission flow
- assignment list view
- student submission form
- submission detail view showing:
  - digest
  - signature algorithm
  - verification status
  - verification message

### Phase C: grade visibility
- student grade view if additional screenshots/evidence are needed
- lecturer/admin grade create/update view if time permits

> **Note (updated):** Phase D (the retired standalone symmetric-crypto view) has been removed. Secure transmission is handled by TLS 1.3 via Certbot/Let's Encrypt at the infrastructure level.

## 4. API integration targets

The frontend should integrate against these implemented endpoints first:

### Auth
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/mfa/verify`
- `GET /api/auth/mfa/status`
- `POST /api/auth/mfa/setup`
- `POST /api/auth/mfa/enable`
- `POST /api/auth/mfa/disable`

### Assignments
- `POST /api/assignments`
- `GET /api/assignments`

### Submissions
- `POST /api/assignments/{assignmentId}/submissions`
- `GET /api/submissions/{submissionId}`

### Grades
- `POST /api/submissions/{submissionId}/grade`
- `PUT /api/grades/{gradeId}`
- `GET /api/grades/{gradeId}`
- `GET /api/my/grades/{gradeId}`


## 5. Recommended Vue stack

Keep the frontend small and conventional:
- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios

This is consistent with the implemented EduSecure frontend under `frontend/` and still compatible with the reference style visible in `Christialattion/frontend/`.

## 6. Auth transport rule

The browser-facing frontend now uses the implemented auth model from Pack 03:
- the backend issues a JWT-backed authenticated session in an `HttpOnly` cookie
- frontend requests are sent with credentials enabled
- the frontend does not persist the auth JWT in `localStorage`
- `POST /api/auth/logout` clears the session cookie server-side

## 7. Suggested UI roles and views

### Student
- login
- assignment list filtered by current space memberships
- submit work for assignments visible through current space membership
- view submission verification result
- view own grades while the related assignment remains visible through current space membership

### Lecturer
- login
- create assignment
- review submission metadata
- create/update grades
- optional view for audit evidence if later exposed

### Admin
- login
- limited oversight pages only if needed for screenshots/report support

## 8. UX rule

Every page should support report evidence.

That means each screen should make at least one security-relevant fact visible, for example:
- role-aware access
- submission verification status
- grade ownership restrictions
- submission integrity metadata (hashDigest, verificationStatus)

## 9. Scope-control rule

Do not implement frontend features just because the backend supports them.

A frontend page is worth building only if it contributes to one of these:
- clearer demonstration of the cryptographic artefact
- stronger report screenshots/evidence
- clearer user-role flow explanation

## 10. Remaining implementation order

Implemented baseline already present:
1. auth shell and route guard
2. assignment list and submission form
3. submission detail view
4. MFA account-security flow

Remaining optional order if more UI evidence is useful:
5. student grade view
6. lecturer/admin grade management screens if still useful and time allows

