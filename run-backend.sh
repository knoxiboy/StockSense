#!/usr/bin/env bash
set -e

# Load .env if present
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
if [ -f "$DIR/.env" ]; then
  set -a
  source "$DIR/.env"
  set +a
fi

cd "$DIR/backend"
fuser -k 8080/tcp 2>/dev/null || true
exec ./mvnw spring-boot:run
