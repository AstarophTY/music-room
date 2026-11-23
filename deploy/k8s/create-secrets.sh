#!/usr/bin/env bash
# Creates the Kubernetes secrets from an env file. Values never appear in command arguments.
set -euo pipefail

ENV_FILE="${1:-.env}"
NAMESPACE="music-room"
[ -f "$ENV_FILE" ] || { echo "No $ENV_FILE file: run 'make env' first." >&2; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
chmod 700 "$WORK"

value() { grep -E "^$1=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true; }

apply_secret() {
  local name="$1"; shift
  local file="$WORK/$name.env"
  : > "$file"
  for key in "$@"; do printf '%s=%s\n' "$key" "$(value "$key")" >> "$file"; done
  kubectl -n "$NAMESPACE" create secret generic "$name" --from-env-file="$file" \
    --dry-run=client -o yaml | kubectl apply -f -
}

for required in POSTGRES_USER POSTGRES_PASSWORD JWT_SECRET; do
  [ -n "$(value "$required")" ] || { echo "$required is empty in $ENV_FILE." >&2; exit 1; }
done

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

apply_secret music-room-core POSTGRES_USER POSTGRES_PASSWORD JWT_SECRET
apply_secret music-room-identity GOOGLE_CLIENT_ID APPLE_CLIENT_ID SMTP_HOST SMTP_PORT SMTP_USER SMTP_PASSWORD SMTP_FROM
apply_secret music-room-library STRIPE_SECRET_KEY STRIPE_PRICE_ID STRIPE_WEBHOOK_SECRET STRIPE_RETURN_URL STRIPE_AUTOMATIC_TAX

APPLE_KEY="$(value APPLE_MUSIC_PRIVATE_KEY_PATH)"
MUSIC_FILE="$WORK/music-room-music.env"
: > "$MUSIC_FILE"
for key in SPOTIFY_CLIENT_ID SPOTIFY_CLIENT_SECRET SPOTIFY_REDIRECT_URI APPLE_MUSIC_TEAM_ID APPLE_MUSIC_KEY_ID; do
  printf '%s=%s\n' "$key" "$(value "$key")" >> "$MUSIC_FILE"
done
if [ -n "$APPLE_KEY" ]; then
  [ -f "$APPLE_KEY" ] || { echo "Apple Music key not found: $APPLE_KEY" >&2; exit 1; }
  kubectl -n "$NAMESPACE" create secret generic apple-music-key --from-file=AuthKey.p8="$APPLE_KEY" \
    --dry-run=client -o yaml | kubectl apply -f -
  echo "APPLE_MUSIC_PRIVATE_KEY_PATH=/secrets/apple/AuthKey.p8" >> "$MUSIC_FILE"
else
  echo "APPLE_MUSIC_PRIVATE_KEY_PATH=" >> "$MUSIC_FILE"
fi
kubectl -n "$NAMESPACE" create secret generic music-room-music --from-env-file="$MUSIC_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secrets applied to namespace $NAMESPACE."
