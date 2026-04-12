#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR_DEFAULT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

EMAIL="${EDUSECURE_ADMIN_EMAIL:-admin@example.com}"
PASSWORD="${EDUSECURE_ADMIN_PASSWORD:-AdminPass123!}"
FULL_NAME="${EDUSECURE_ADMIN_FULL_NAME:-System Administrator}"
BACKEND_URL="${EDUSECURE_BACKEND_URL:-http://localhost:8080}"
COMPOSE_PROJECT_DIR="${EDUSECURE_COMPOSE_PROJECT_DIR:-$PROJECT_DIR_DEFAULT}"
POSTGRES_SERVICE="${EDUSECURE_POSTGRES_SERVICE:-postgres}"
DB_NAME="${POSTGRES_DB:-edusecure}"
DB_USER="${POSTGRES_USER:-postgres}"
ADMIN_ONLY=0

usage() {
  cat <<'EOF'
Usage: add-dev-admin.sh [options]

Options:
  --email <value>              Admin email address
  --password <value>           Admin password for new users
  --full-name <value>          Admin full name for new users
  --backend-url <value>        Backend base URL (default: http://localhost:8080)
  --compose-project-dir <dir>  Repository root / Docker Compose project dir
  --postgres-service <name>    Docker Compose Postgres service name (default: postgres)
  --db-name <name>             Database name inside the container (default: edusecure)
  --db-user <name>             Database user inside the container (default: postgres)
  --admin-only                 Remove the STUDENT role after assigning ADMIN
  --help                       Show this help text
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --email)
      EMAIL="$2"
      shift 2
      ;;
    --password)
      PASSWORD="$2"
      shift 2
      ;;
    --full-name)
      FULL_NAME="$2"
      shift 2
      ;;
    --backend-url)
      BACKEND_URL="$2"
      shift 2
      ;;
    --compose-project-dir)
      COMPOSE_PROJECT_DIR="$2"
      shift 2
      ;;
    --postgres-service)
      POSTGRES_SERVICE="$2"
      shift 2
      ;;
    --db-name)
      DB_NAME="$2"
      shift 2
      ;;
    --db-user)
      DB_USER="$2"
      shift 2
      ;;
    --admin-only)
      ADMIN_ONLY=1
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

EMAIL="${EMAIL,,}"
EMAIL_SQL_LITERAL="'${EMAIL//\'/\'\'}'"
BACKEND_URL="${BACKEND_URL%/}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

write_step() {
  printf '==> %s\n' "$1"
}

write_detail() {
  printf '    %s\n' "$1"
}

json_escape() {
  local value="$1"
  value=${value//\\/\\\\}
  value=${value//\"/\\\"}
  value=${value//$'\n'/\\n}
  value=${value//$'\r'/\\r}
  value=${value//$'\t'/\\t}
  printf '%s' "$value"
}

compose_psql() {
  docker compose --project-directory "$COMPOSE_PROJECT_DIR" exec -T "$POSTGRES_SERVICE" \
    psql -P pager=off -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"
}

auth_schema_ready() {
  local table_count

  if ! table_count="$({ compose_psql -t -A <<'SQL'
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('roles', 'users', 'user_roles');
SQL
} | tr -d '[:space:]')"; then
    return 1
  fi

  [[ "$table_count" == '3' ]]
}

wait_for_auth_schema() {
  local timeout_seconds="${1:-120}"
  local poll_interval_seconds="${2:-3}"
  local elapsed=0
  local waited=0

  while (( elapsed < timeout_seconds )); do
    if auth_schema_ready; then
      if (( waited == 1 )); then
        write_detail 'Detected roles/users/user_roles after waiting for Liquibase to finish.'
      fi

      return 0
    fi

    if (( waited == 0 )); then
      write_detail 'Auth tables are not available yet; waiting for the backend to finish applying Liquibase migrations...'
      waited=1
    fi

    sleep "$poll_interval_seconds"
    elapsed=$((elapsed + poll_interval_seconds))
  done

  cat >&2 <<EOF
The PostgreSQL container is reachable, but the auth schema is still missing after waiting ${timeout_seconds} seconds.

Start the dev stack and let the backend finish booting so Liquibase can create:
  - roles
  - users
  - user_roles

Recommended command from the repository root:
  docker compose up -d postgres backend

If those services are already running, check the backend logs for Liquibase/database errors:
  docker compose logs backend --tail=200

If the logs show repeated startup failures or the schema is unexpectedly incomplete, you may be using a stale PostgreSQL dev volume.
EOF

  return 1
}

require_command docker
require_command curl
require_command awk

write_step 'Waiting for the auth schema managed by Liquibase'
wait_for_auth_schema

write_step 'Ensuring auth roles exist in PostgreSQL'
if ! compose_psql <<'SQL'; then
INSERT INTO roles (name)
VALUES ('STUDENT'), ('LECTURER'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;
SQL
  echo 'Unable to insert or verify auth roles after the schema readiness check.' >&2
  exit 1
fi

write_step "Checking whether ${EMAIL} already exists"
user_exists="$({ compose_psql -t -A <<SQL
SELECT 1
FROM users
WHERE email = ${EMAIL_SQL_LITERAL}
LIMIT 1;
SQL
} | tr -d '[:space:]')"

if [[ -z "$user_exists" ]]; then
  write_step 'User not found; creating it through the backend register endpoint'

  cookie_jar="$(mktemp)"
  response_body="$(mktemp)"
  cleanup() {
    rm -f "$cookie_jar" "$response_body"
  }
  trap cleanup EXIT

  if ! curl -fsS -c "$cookie_jar" "$BACKEND_URL/api/auth/csrf" -o /dev/null; then
    echo "Could not reach $BACKEND_URL/api/auth/csrf. Start the backend before running this script for a new user." >&2
    exit 1
  fi

  csrf_token="$(awk '$6 == "XSRF-TOKEN" { print $7 }' "$cookie_jar" | tail -n 1)"
  if [[ -z "$csrf_token" ]]; then
    echo 'Failed to obtain the XSRF-TOKEN cookie from the backend.' >&2
    exit 1
  fi

  payload=$(printf '{"email":"%s","password":"%s","fullName":"%s"}' \
    "$(json_escape "$EMAIL")" \
    "$(json_escape "$PASSWORD")" \
    "$(json_escape "$FULL_NAME")")

  register_status="$(curl -sS -o "$response_body" -w '%{http_code}' \
    -b "$cookie_jar" -c "$cookie_jar" \
    -H 'Content-Type: application/json' \
    -H "X-XSRF-TOKEN: $csrf_token" \
    -X POST "$BACKEND_URL/api/auth/register" \
    --data "$payload")"

  case "$register_status" in
    201)
      printf 'Created user %s\n' "$EMAIL"
      ;;
    409)
      printf 'User %s already exists; continuing with admin role assignment.\n' "$EMAIL" >&2
      ;;
    *)
      printf 'Failed to register %s via the backend (HTTP %s). Response: %s\n' "$EMAIL" "$register_status" "$(cat "$response_body")" >&2
      exit 1
      ;;
  esac
else
  printf 'User %s already exists; skipping registration.\n' "$EMAIL"
fi

write_step 'Assigning the ADMIN role'
compose_psql <<SQL
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = ${EMAIL_SQL_LITERAL}
ON CONFLICT DO NOTHING;
SQL

if [[ "$ADMIN_ONLY" -eq 1 ]]; then
  write_step 'Removing the STUDENT role because --admin-only was specified'
  compose_psql <<SQL
DELETE FROM user_roles
WHERE user_id = (
    SELECT id
    FROM users
    WHERE email = ${EMAIL_SQL_LITERAL}
)
AND role_id = (
    SELECT id
    FROM roles
    WHERE name = 'STUDENT'
);
SQL
fi

write_step 'Reading back the assigned roles'
stored_full_name="$(compose_psql -t -A <<SQL
SELECT full_name
FROM users
WHERE email = ${EMAIL_SQL_LITERAL};
SQL
)"
stored_full_name="$(printf '%s' "$stored_full_name" | sed '/^[[:space:]]*$/d')"

roles_output="$(compose_psql -t -A <<SQL
SELECT r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = ${EMAIL_SQL_LITERAL}
ORDER BY r.name;
SQL
)"
roles_output="$(printf '%s' "$roles_output" | sed '/^[[:space:]]*$/d')"

if [[ -z "$roles_output" ]]; then
  echo "No roles were found for $EMAIL. The admin bootstrap did not complete successfully." >&2
  exit 1
fi

printf '\nAdmin bootstrap complete.\n'
printf 'Email:      %s\n' "$EMAIL"
printf 'Full name:  %s\n' "$stored_full_name"
printf 'Roles:\n%s\n' "$roles_output"
printf 'DB:         %s (%s)\n' "$DB_NAME" "$POSTGRES_SERVICE"
printf 'Backend:    %s\n' "$BACKEND_URL"

if [[ -z "$user_exists" ]]; then
  printf 'Password:   %s\n' "$PASSWORD"
else
  printf 'Password:   unchanged (existing user)\n'
fi


