#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR_DEFAULT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="${EDUSECURE_CRYPTO_DIR:-$PROJECT_DIR_DEFAULT/deploy/home-server/crypto}"
PRIVATE_KEY_PATH="${EDUSECURE_SIGNING_PRIVATE_KEY_PATH:-$OUTPUT_DIR/signing-private.pem}"
PUBLIC_KEY_PATH="${EDUSECURE_SIGNING_PUBLIC_KEY_PATH:-$OUTPUT_DIR/signing-public.pem}"
CURVE="${EDUSECURE_SIGNING_CURVE:-prime256v1}"
FORCE=0

usage() {
  cat <<'EOF'
Usage: generate-prod-signing-keypair.sh [options]

Options:
  --output-dir <dir>     Directory where signing-private.pem and signing-public.pem are written
  --private-key <path>   Full path to the private key output file
  --public-key <path>    Full path to the public key output file
  --curve <name>         OpenSSL EC curve name (default: prime256v1)
  --force                Overwrite existing output files
  --help                 Show this help text

Environment variables:
  EDUSECURE_CRYPTO_DIR
  EDUSECURE_SIGNING_PRIVATE_KEY_PATH
  EDUSECURE_SIGNING_PUBLIC_KEY_PATH
  EDUSECURE_SIGNING_CURVE
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output-dir)
      OUTPUT_DIR="$2"
      PRIVATE_KEY_PATH="$OUTPUT_DIR/signing-private.pem"
      PUBLIC_KEY_PATH="$OUTPUT_DIR/signing-public.pem"
      shift 2
      ;;
    --private-key)
      PRIVATE_KEY_PATH="$2"
      shift 2
      ;;
    --public-key)
      PUBLIC_KEY_PATH="$2"
      shift 2
      ;;
    --curve)
      CURVE="$2"
      shift 2
      ;;
    --force)
      FORCE=1
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

abort_if_exists() {
  local path="$1"

  if [[ -e "$path" && "$FORCE" -ne 1 ]]; then
    echo "Refusing to overwrite existing file: $path" >&2
    echo "Re-run with --force if you intend to replace it." >&2
    exit 1
  fi
}

require_command openssl
require_command mkdir
require_command chmod

mkdir -p "$(dirname "$PRIVATE_KEY_PATH")"
mkdir -p "$(dirname "$PUBLIC_KEY_PATH")"

abort_if_exists "$PRIVATE_KEY_PATH"
abort_if_exists "$PUBLIC_KEY_PATH"

write_step "Generating EC private key ($CURVE)"
openssl genpkey \
  -algorithm EC \
  -pkeyopt "ec_paramgen_curve:$CURVE" \
  -out "$PRIVATE_KEY_PATH"

write_step "Deriving public key"
openssl pkey \
  -in "$PRIVATE_KEY_PATH" \
  -pubout \
  -out "$PUBLIC_KEY_PATH"

chmod 600 "$PRIVATE_KEY_PATH"
chmod 644 "$PUBLIC_KEY_PATH"

printf '\nGenerated signing key pair:\n'
printf '  Private: %s\n' "$PRIVATE_KEY_PATH"
printf '  Public:  %s\n' "$PUBLIC_KEY_PATH"
printf '\nMount or copy them to /srv/edusecure/crypto/ for the production Compose stack.\n'

