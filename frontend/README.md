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
- The AES demo remains a separate artefact-evidence slice; the real submission confidentiality control is the AES-at-rest storage flow already present in the backend.
- Grade and optional AES demo screens can still be expanded in the frontend if additional screenshots/report evidence are useful.
