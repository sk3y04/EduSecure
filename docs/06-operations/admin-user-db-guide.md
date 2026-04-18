# How to Insert an Admin User into the Database

This guide shows the safest way to create an `ADMIN` user in `EduSecure`.

## Quick start: use the dev bootstrap scripts

Two helper scripts are now available at the repository root:

- Windows PowerShell: `scripts/add-dev-admin.ps1`
- Linux/macOS Bash: `scripts/add-dev-admin.sh`

What they do:

1. connect to the dev PostgreSQL container from `compose.yaml`
2. wait for Liquibase to create the auth schema (`roles`, `users`, `user_roles`)
3. ensure the auth roles exist in `roles`
4. create the user through `POST /api/auth/register` **only if the email does not already exist**
5. add the `ADMIN` role in `user_roles`
6. optionally remove the original `STUDENT` role

The scripts default to these dev values:

- email: `admin@example.com`
- password: `AdminPass123!`
- full name: `System Administrator`
- backend URL: `http://localhost:8080`
- database: `edusecure`
- database user: `postgres`
- compose service: `postgres`

> Important: these helper scripts are primarily aimed at the local development stack.
>
> For a production deployment, prefer the explicit HTTPS + PostgreSQL promotion workflow documented below so you can:
>
> - register the account through the real public origin
> - avoid relying on dev defaults like `http://localhost:8080`
> - control exactly which Compose project and environment file are used
> - avoid accidentally printing or reusing development credentials
>
> If the user does **not** already exist, the backend must be running so the script can register the account with a valid bcrypt password hash.
>
> The scripts now wait briefly for Liquibase to create the auth tables. If they still report that `roles` / `users` / `user_roles` are missing after waiting, the backend has not finished booting or has failed during startup.

### Windows PowerShell

Run from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\add-dev-admin.ps1
```

Custom values:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\add-dev-admin.ps1 `
  -Email 'dev-admin@example.com' `
  -Password 'AdminPass123!' `
  -FullName 'Development Admin' `
  -AdminOnly
```

### Linux / Bash

Make the script executable once:

```bash
chmod +x ./scripts/add-dev-admin.sh
```

Run it from the repository root:

```bash
./scripts/add-dev-admin.sh
```

Custom values:

```bash
./scripts/add-dev-admin.sh \
  --email dev-admin@example.com \
  --password 'AdminPass123!' \
  --full-name 'Development Admin' \
  --admin-only
```

### Environment variable overrides

Both scripts also support these environment variables:

- `EDUSECURE_ADMIN_EMAIL`
- `EDUSECURE_ADMIN_PASSWORD`
- `EDUSECURE_ADMIN_FULL_NAME`
- `EDUSECURE_BACKEND_URL`
- `EDUSECURE_COMPOSE_PROJECT_DIR`
- `EDUSECURE_POSTGRES_SERVICE`
- `POSTGRES_DB`
- `POSTGRES_USER`

## Why this guide uses a two-step method

From the current backend implementation:

- `POST /api/auth/register` always creates a `STUDENT` user.
- `POST /api/auth/users` **cannot** create `ADMIN` accounts.
- passwords are stored as **bcrypt hashes**, not plaintext.
- roles are seeded at backend startup by `DataInitializer`.

Because of that, the safest method is:

1. create a normal account through the application so the password is hashed correctly
2. add the `ADMIN` role directly in PostgreSQL

---

## Production-safe method

For the deployed home-server stack, the safest admin bootstrap workflow is:

1. register the account over the real HTTPS origin so the normal security flow is exercised
2. promote that user to `ADMIN` directly in PostgreSQL on the home server
3. immediately sign in and enable MFA for that admin account

This avoids changing password hashes manually and keeps the public-origin cookie / CSRF behavior aligned with the deployed Nginx routing.

### Quick start: use the production bootstrap script

A dedicated production helper script is available at:

- Linux/macOS Bash: `scripts/add-prod-admin.sh`

What it does:

1. reads deployment settings from `.env.prod`
2. derives the public origin from `APP_CORS_ALLOWED_ORIGINS` unless you override it
3. waits for Liquibase to finish creating `roles`, `users`, and `user_roles`
4. registers the account through the live `/api/auth/register` endpoint if it does not already exist
5. promotes that user to `ADMIN` in PostgreSQL
6. removes the `STUDENT` role by default for a tighter admin posture

Example:

```bash
chmod +x ./scripts/add-prod-admin.sh

./scripts/add-prod-admin.sh \
  --env-file /srv/edusecure/.env.prod \
  --compose-file /srv/edusecure/compose.yaml \
  --email admin@example.com \
  --full-name 'System Administrator'
```

If you want to keep the `STUDENT` role as well, add:

```bash
--keep-student-role
```

If you omit `--password`, the script prompts for it interactively and does not echo it back to the terminal.

### Production prerequisites

Make sure these are already true:

- the VPS reverse proxy is serving `https://edusecure.skey.ovh`
- the home-server Compose stack is up and healthy
- PostgreSQL is reachable through `docker compose exec`
- the backend has started at least once so `roles`, `users`, and `user_roles` exist

### Step 1: Register the user over HTTPS

Because the backend uses CSRF protection, first obtain the CSRF cookie and then submit the registration request.

Example using `curl` on Linux/macOS:

```bash
curl -sS -c cookies.txt https://edusecure.skey.ovh/api/auth/csrf -o /dev/null

XSRF_TOKEN=$(awk '$6 == "XSRF-TOKEN" { print $7 }' cookies.txt | tail -n 1)

curl -sS \
  -b cookies.txt \
  -c cookies.txt \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $XSRF_TOKEN" \
  -X POST https://edusecure.skey.ovh/api/auth/register \
  --data '{"email":"admin@example.com","password":"REPLACE_WITH_STRONG_PASSWORD","fullName":"System Administrator"}'
```

At this point the user exists with the default `STUDENT` role and a valid bcrypt password hash.

### Step 2: Promote the user to ADMIN on the home server

From the directory that contains `/srv/edusecure/compose.yaml`, run:

```bash
cd /srv/edusecure

docker compose --env-file /srv/edusecure/.env.prod exec -T postgres \
  psql -P pager=off -v ON_ERROR_STOP=1 -U edusecure_app -d edusecure <<'SQL'
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com'
ON CONFLICT DO NOTHING;
SQL
```

Replace `edusecure_app` and `edusecure` if your production `.env.prod` uses different values.

### Step 3: Verify the assigned roles

```bash
cd /srv/edusecure

docker compose --env-file /srv/edusecure/.env.prod exec -T postgres \
  psql -P pager=off -U edusecure_app -d edusecure \
  -c "SELECT u.email, r.name FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = 'admin@example.com' ORDER BY r.name;"
```

Expected result: at least `ADMIN` should be listed for that email address.

### Step 4: Optional — remove the STUDENT role

If you want the seeded production admin to be `ADMIN` only:

```bash
cd /srv/edusecure

docker compose --env-file /srv/edusecure/.env.prod exec -T postgres \
  psql -P pager=off -v ON_ERROR_STOP=1 -U edusecure_app -d edusecure <<'SQL'
DELETE FROM user_roles
WHERE user_id = (
  SELECT id FROM users WHERE email = 'admin@example.com'
)
AND role_id = (
  SELECT id FROM roles WHERE name = 'STUDENT'
);
SQL
```

### Step 5: Immediately enable MFA

After the admin can sign in, enable MFA right away through the deployed application:

1. sign in at `https://edusecure.skey.ovh`
2. open the account security / MFA section
3. complete the MFA setup and verification flow

That keeps the privileged bootstrap account aligned with the production security posture.

---

## Prerequisites

Make sure these services are available:

- PostgreSQL from `compose.yaml`
- backend application running at `http://localhost:8080`

Default local database settings in this repository are:

- database: `edusecure`
- username: `postgres`
- password: `postgres`
- port: `5432`

Start the local stack if needed:

```powershell
docker compose up -d postgres backend
```

> Important: start the backend at least once before editing the database. The backend seeds the `roles` table (`STUDENT`, `LECTURER`, `ADMIN`) on startup.

If the bootstrap script still says the auth schema is missing after waiting, inspect the backend startup logs:

```powershell
docker compose logs backend --tail=200
```

If the backend repeatedly fails to start and you are comfortable resetting local dev data, remove the persisted PostgreSQL volume and start again:

```powershell
docker compose down -v
docker compose up -d postgres backend
```

---

## Recommended method: create a normal user, then promote it to ADMIN

### Step 1: Register the user normally

Run this from the repository root:

```powershell
$body = @{
  email = 'admin@example.com'
  password = 'AdminPass123!'
  fullName = 'System Administrator'
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/auth/register' `
  -ContentType 'application/json' `
  -Body $body
```

This creates the user in the `users` table with a valid bcrypt password hash.

---

### Step 2: Open PostgreSQL

```powershell
docker compose exec postgres psql -U postgres -d edusecure
```

---

### Step 3: Check that the `ADMIN` role exists

Inside `psql`, run:

```sql
SELECT id, name
FROM roles
ORDER BY id;
```

You should see a row with `name = 'ADMIN'`.

If you do **not** see it, stop here and start the backend once. The backend is what seeds the roles.

---

### Step 4: Add the ADMIN role to the user

Still inside `psql`, run:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = 'admin@example.com'
ON CONFLICT DO NOTHING;
```

This keeps the existing user record and adds the `ADMIN` role mapping in `user_roles`.

---

### Step 5: Verify the result

```sql
SELECT u.email, r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'admin@example.com'
ORDER BY r.name;
```

Expected result: the user should now have at least `ADMIN` listed.

---

## Optional: remove the original STUDENT role

If you want the account to be only `ADMIN`, run this:

```sql
DELETE FROM user_roles
WHERE user_id = (
  SELECT id FROM users WHERE email = 'admin@example.com'
)
AND role_id = (
  SELECT id FROM roles WHERE name = 'STUDENT'
);
```

Then verify again:

```sql
SELECT u.email, r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'admin@example.com'
ORDER BY r.name;
```

---

## Quick login check

After promotion, test the account:

```powershell
$body = @{
  email = 'admin@example.com'
  password = 'AdminPass123!'
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/auth/login' `
  -ContentType 'application/json' `
  -Body $body
```

If login succeeds and the response includes `ADMIN` in `roles`, the account is ready.

---

## Database tables involved

This process uses these tables:

- `users`
- `roles`
- `user_roles`

Relevant columns:

- `users.id`
- `users.email`
- `users.password_hash`
- `roles.id`
- `roles.name`
- `user_roles.user_id`
- `user_roles.role_id`

---

## Important notes

- emails are normalized to lowercase by the backend, so use lowercase when querying by email
- do **not** insert plaintext passwords into `users.password_hash`
- if you want to create a user fully by raw SQL, you must first generate a valid **bcrypt** hash yourself
- for this project, promoting a normally registered user is the safest and simplest admin bootstrap path

---

## One-command verification query

If you just want to confirm the admin mapping later:

```powershell
docker compose exec postgres psql -U postgres -d edusecure -c "SELECT u.email, r.name FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id WHERE u.email = 'admin@example.com' ORDER BY r.name;"
```

