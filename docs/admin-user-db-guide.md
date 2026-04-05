# How to Insert an Admin User into the Database

This guide shows the safest way to create an `ADMIN` user in `EduSecure`.

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

