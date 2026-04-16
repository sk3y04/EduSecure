#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR_DEFAULT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
DEFAULT_ENV_FILE="/srv/edusecure/.env.prod"
if [[ ! -f "$DEFAULT_ENV_FILE" && -f "$PROJECT_DIR_DEFAULT/deploy/home-server/.env.prod" ]]; then
  DEFAULT_ENV_FILE="$PROJECT_DIR_DEFAULT/deploy/home-server/.env.prod"
fi

ENV_FILE="${EDUSECURE_ENV_FILE:-$DEFAULT_ENV_FILE}"
COMPOSE_FILE="${EDUSECURE_COMPOSE_FILE:-}"
POSTGRES_SERVICE="${EDUSECURE_POSTGRES_SERVICE:-postgres}"
EMAIL="${EDUSECURE_ADMIN_EMAIL:-}"
PASSWORD="${EDUSECURE_ADMIN_PASSWORD:-}"
FULL_NAME="${EDUSECURE_ADMIN_FULL_NAME:-System Administrator}"
PUBLIC_BASE_URL="${EDUSECURE_PUBLIC_BASE_URL:-}"
KEEP_STUDENT_ROLE=0

usage() {
  cat <<'EOF'
Usage: add-prod-admin.sh [options]

Seeds a production admin account by:
  1. reading deployment settings from .env.prod
  2. registering the user through the live HTTPS endpoint
  3. promoting that user to ADMIN in PostgreSQL
  4. optionally removing the STUDENT role

Options:
  --env-file <path>         Path to .env.prod (default: /srv/edusecure/.env.prod)
  --compose-file <path>     Path to compose.yaml / compose.prod.yaml
  --postgres-service <name> Compose service name for PostgreSQL (default: postgres)
  --email <value>           Admin email address (required unless prompted)
  --password <value>        Admin password (prompted if omitted)
  --full-name <value>       Admin full name (default: System Administrator)
  --public-base-url <url>   Public base URL such as https://edusecure.skey.ovh
  --keep-student-role       Keep the default STUDENT role instead of removing it
  --help                    Show this help text

Environment variables:
  EDUSECURE_ENV_FILE
  EDUSECURE_COMPOSE_FILE
  EDUSECURE_POSTGRES_SERVICE
  EDUSECURE_ADMIN_EMAIL
  EDUSECURE_ADMIN_PASSWORD
  EDUSECURE_ADMIN_FULL_NAME
  EDUSECURE_PUBLIC_BASE_URL
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --compose-file)
      COMPOSE_FILE="$2"
      shift 2
      ;;
    --postgres-service)
      POSTGRES_SERVICE="$2"
      shift 2
      ;;
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
    --public-base-url)
      PUBLIC_BASE_URL="$2"
      shift 2
      ;;
    --keep-student-role)
      KEEP_STUDENT_ROLE=1
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

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
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

load_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Env file not found: $ENV_FILE" >&2
    exit 1
  fi

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

resolve_compose_file() {
  if [[ -n "$COMPOSE_FILE" ]]; then
    return
  fi

  local env_dir
  env_dir="$(cd -- "$(dirname -- "$ENV_FILE")" && pwd)"

  if [[ -f "$env_dir/compose.yaml" ]]; then
    COMPOSE_FILE="$env_dir/compose.yaml"
  elif [[ -f "$env_dir/compose.prod.yaml" ]]; then
    COMPOSE_FILE="$env_dir/compose.prod.yaml"
  else
    echo "Could not find compose.yaml or compose.prod.yaml next to $ENV_FILE" >&2
    exit 1
  fi
}

compose_psql() {
  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    exec -T "$POSTGRES_SERVICE" \
    psql -P pager=off -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"
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

Check the deployed backend logs:
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs backend --tail=200
EOF

  return 1
}

prompt_if_missing() {
  if [[ -z "$EMAIL" ]]; then
    read -r -p 'Admin email: ' EMAIL
  fi

  if [[ -z "$FULL_NAME" ]]; then
    read -r -p 'Full name: ' FULL_NAME
  fi

  if [[ -z "$PASSWORD" ]]; then
    local password_confirm=''
    read -r -s -p 'Admin password: ' PASSWORD
    printf '\n'
    read -r -s -p 'Confirm password: ' password_confirm
    printf '\n'

    if [[ "$PASSWORD" != "$password_confirm" ]]; then
      echo 'Passwords do not match.' >&2
      exit 1
    fi
  fi
}

derive_public_base_url() {
  if [[ -n "$PUBLIC_BASE_URL" ]]; then
    PUBLIC_BASE_URL="$(trim "$PUBLIC_BASE_URL")"
    return
  fi

  local first_origin=''
  IFS=',' read -r first_origin _ <<< "${APP_CORS_ALLOWED_ORIGINS:-}"
  first_origin="$(trim "$first_origin")"

  if [[ -n "$first_origin" ]]; then
    PUBLIC_BASE_URL="$first_origin"
    return
  fi

  echo 'Could not derive the public base URL from APP_CORS_ALLOWED_ORIGINS. Pass --public-base-url explicitly.' >&2
  exit 1
}

register_user_if_needed() {
  local normalized_email="$1"
  local email_sql_literal="'$normalized_email'"
  local user_exists=''

  write_step "Checking whether $normalized_email already exists"
  user_exists="$({ compose_psql -t -A <<SQL
SELECT 1
FROM users
WHERE email = ${email_sql_literal}
LIMIT 1;
SQL
} | tr -d '[:space:]')"

  if [[ -n "$user_exists" ]]; then
    write_detail 'User already exists; skipping registration and continuing with role promotion.'
    return 0
  fi

  write_step 'User not found; creating it through the live register endpoint'

  local cookie_jar response_body csrf_token register_status payload
  cookie_jar="$(mktemp)"
  response_body="$(mktemp)"
  trap 'rm -f "$cookie_jar" "$response_body"' RETURN

  if ! curl -fsS -c "$cookie_jar" "$PUBLIC_BASE_URL/api/auth/csrf" -o /dev/null; then
    echo "Could not reach $PUBLIC_BASE_URL/api/auth/csrf" >&2
    exit 1
  fi

  csrf_token="$(awk '$6 == "XSRF-TOKEN" { print $7 }' "$cookie_jar" | tail -n 1)"
  if [[ -z "$csrf_token" ]]; then
    echo 'Failed to obtain the XSRF-TOKEN cookie from the backend.' >&2
    exit 1
  fi

  payload=$(printf '{"email":"%s","password":"%s","fullName":"%s"}' \
    "$(json_escape "$normalized_email")" \
    "$(json_escape "$PASSWORD")" \
    "$(json_escape "$FULL_NAME")")

  register_status="$(curl -sS -o "$response_body" -w '%{http_code}' \
    -b "$cookie_jar" -c "$cookie_jar" \
    -H 'Content-Type: application/json' \
    -H "X-XSRF-TOKEN: $csrf_token" \
    -X POST "$PUBLIC_BASE_URL/api/auth/register" \
    --data "$payload")"

  case "$register_status" in
    201)
      write_detail "Created user $normalized_email"
      ;;
    409)
      write_detail "User $normalized_email already exists according to the backend; continuing with admin role assignment."
      ;;
    *)
      printf 'Failed to register %s via the backend (HTTP %s). Response: %s\n' "$normalized_email" "$register_status" "$(cat "$response_body")" >&2
      exit 1
      ;;
  esac

  rm -f "$cookie_jar" "$response_body"
  trap - RETURN
}

promote_user() {
  local normalized_email="$1"
  local email_sql_literal="'$normalized_email'"

  write_step 'Ensuring auth roles exist in PostgreSQL'
  compose_psql <<'SQL'
INSERT INTO roles (name)
VALUES ('STUDENT'), ('LECTURER'), ('ADMIN')
ON CONFLICT (name) DO NOTHING;
SQL

  write_step 'Assigning the ADMIN role'
  compose_psql <<SQL
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.email = ${email_sql_literal}
ON CONFLICT DO NOTHING;
SQL

  if (( KEEP_STUDENT_ROLE == 0 )); then
    write_step 'Removing the STUDENT role for a tighter production admin posture'
    compose_psql <<SQL
DELETE FROM user_roles
WHERE user_id = (
    SELECT id
    FROM users
    WHERE email = ${email_sql_literal}
)
AND role_id = (
    SELECT id
    FROM roles
    WHERE name = 'STUDENT'
);
SQL
  fi
}

print_verification() {
  local normalized_email="$1"
  local email_sql_literal="'$normalized_email'"
  local stored_full_name roles_output

  write_step 'Reading back the assigned roles'
  stored_full_name="$(compose_psql -t -A <<SQL
SELECT full_name
FROM users
WHERE email = ${email_sql_literal};
SQL
)"
  stored_full_name="$(trim "$stored_full_name")"

  roles_output="$(compose_psql -t -A <<SQL
SELECT r.name
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = ${email_sql_literal}
ORDER BY r.name;
SQL
)"
  roles_output="$(printf '%s' "$roles_output" | sed '/^[[:space:]]*$/d')"

  if [[ -z "$roles_output" ]]; then
    echo "No roles were found for $normalized_email. The admin bootstrap did not complete successfully." >&2
    exit 1
  fi

  printf '\nProduction admin bootstrap complete.\n'
  printf 'Email:      %s\n' "$normalized_email"
  printf 'Full name:  %s\n' "$stored_full_name"
  printf 'Roles:\n%s\n' "$roles_output"
  printf 'Origin:     %s\n' "$PUBLIC_BASE_URL"
  printf 'DB:         %s (%s)\n' "$POSTGRES_DB" "$POSTGRES_SERVICE"
  printf '\nNext step: sign in at %s and enable MFA immediately.\n' "$PUBLIC_BASE_URL"
}

require_command docker
require_command curl
require_command awk
require_command sed
require_command tr

load_env_file
resolve_compose_file
prompt_if_missing
derive_public_base_url

EMAIL="$(trim "$EMAIL")"
EMAIL="${EMAIL,,}"
FULL_NAME="$(trim "$FULL_NAME")"
PUBLIC_BASE_URL="${PUBLIC_BASE_URL%/}"

if [[ -z "$EMAIL" || -z "$PASSWORD" || -z "$FULL_NAME" ]]; then
  echo 'Email, password, and full name must all be set.' >&2
  exit 1
fi

if [[ -z "${POSTGRES_DB:-}" || -z "${POSTGRES_USER:-}" ]]; then
  echo 'POSTGRES_DB and POSTGRES_USER must be set in the env file.' >&2
  exit 1
fi

write_step 'Waiting for the auth schema managed by Liquibase'
wait_for_auth_schema
register_user_if_needed "$EMAIL"
promote_user "$EMAIL"
print_verification "$EMAIL"

