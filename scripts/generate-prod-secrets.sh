#!/usr/bin/env bash
set -euo pipefail

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

write_secret() {
  local name="$1"
  local mode="$2"
  local length="$3"

  case "$mode" in
    hex)
      printf '%s=%s\n' "$name" "$(openssl rand -hex "$length")"
      ;;
    base64)
      printf '%s=%s\n' "$name" "$(openssl rand -base64 "$length" | tr -d '\n')"
      ;;
    *)
      echo "Unsupported mode: $mode" >&2
      exit 1
      ;;
  esac
}

require_command openssl
require_command tr

cat <<'EOF'
# Paste these values into deploy/home-server/.env.prod after reviewing them.
# AES-backed settings are generated as 32 random bytes encoded in Base64.
EOF

write_secret POSTGRES_PASSWORD hex 24
write_secret MONGODB_ROOT_PASSWORD hex 24
write_secret MONGODB_APP_PASSWORD hex 24
write_secret JWT_SECRET base64 48
write_secret MFA_SECRET_ENCRYPTION_KEY base64 32
printf 'MFA_SECRET_KEY_VERSION=v1\n'
printf 'MFA_ISSUER=EduSecure\n'
write_secret AUDIT_HMAC_SECRET base64 48
write_secret SUBMISSION_STORAGE_MASTER_KEY base64 32
printf 'SUBMISSION_STORAGE_MASTER_KEY_VERSION=v1\n'

