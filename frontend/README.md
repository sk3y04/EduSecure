# EduSecure Frontend MVP

Small Vue 3 frontend for the EduSecure cryptography artefact.

## Stack

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios
- Tailwind CSS

## Current scope

This MVP focuses on the backend flows already implemented in `backend/`:

- password login with backend-issued HttpOnly auth cookie
- MFA challenge completion
- current-user bootstrap and role-aware navigation
- admin/lecturer account creation UI for managed user onboarding
- assignment listing
- lecturer/admin assignment creation
- student submission creation
- submission integrity evidence view
- MFA status, setup, enable, and disable

## Environment

Copy `.env.example` to `.env` if you need to override the API base URL.

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

For cookie auth to work in development, the backend must allow the frontend origin and send
credentialed responses. The backend defaults already allow `http://localhost:5173`. If you change
the frontend origin, also update the backend `APP_CORS_ALLOWED_ORIGINS` setting.

The browser client now also uses Spring's CSRF protection for unsafe requests:
- the frontend first bootstraps `GET /api/auth/csrf` when needed
- the backend returns a readable `XSRF-TOKEN` cookie
- Axios mirrors that cookie value into the `X-XSRF-TOKEN` header for `POST`, `PUT`, `PATCH`, and `DELETE`
- the auth JWT remains separate in the `HttpOnly` `EDUSECURE_AUTH` cookie

For production, run the backend with the `prod` profile so the cookie defaults become secure:

```bash
SPRING_PROFILES_ACTIVE=prod
AUTH_COOKIE_SECURE=true
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
```

Recommended production cookie settings:

- same-site deployment on one site or subpath: keep `AUTH_COOKIE_SAME_SITE=Lax`
- cross-site SPA/API deployment: use `AUTH_COOKIE_SAME_SITE=None` and keep `AUTH_COOKIE_SECURE=true`
- shared subdomain deployments: optionally set `AUTH_COOKIE_DOMAIN=example.com`

The backend now validates these combinations at startup and fails fast for unsafe production
settings such as `AUTH_COOKIE_SAME_SITE=None` with `AUTH_COOKIE_SECURE=false`.

## Install

```bash
npm install
```

## Run in development

```bash
npm run dev
```

## Validate

```bash
npm run type-check
npm run build
```

## Notes

- `Christialattion/frontend/` is used only as a structural reference.
- The UI is intentionally evidence-oriented rather than product-polished.
- Authentication is now cookie-based: the browser stores the session JWT in an HttpOnly cookie,
  while the frontend only keeps the MFA challenge state in `sessionStorage`.
- Unsafe browser requests are CSRF-protected through the `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN`
  header pattern implemented in `frontend/src/services/http.ts`.
- MFA is standard TOTP: users can enroll with a normal smartphone authenticator app by scanning a QR code derived from the backend `otpauth://` URI or by entering the returned manual key.
- Admins and lecturers now have a user-management screen; admins can create lecturer and student accounts, while lecturers are limited to student account creation.
- Symmetric-encryption evidence now comes from the backend AES-GCM-at-rest flows for MFA secrets and submission storage rather than from a separate frontend AES demo screen.
- Grade screens can still be expanded in the frontend if additional screenshots/report evidence are useful.
