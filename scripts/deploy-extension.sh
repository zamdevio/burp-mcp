#!/usr/bin/env bash
# Build burp-mcp-all.jar and copy it to the Burp Extensions folder for live smoke.
#
# Destination (first match wins):
#   1. CLI arg: ./scripts/deploy-extension.sh /path/to/Extensions[/burp-mcp-all.jar]
#   2. Env:     BURP_EXTENSION_JAR_DEST from repo-root .env (or already exported)
#
# Usage:
#   ./scripts/deploy-extension.sh                  # test + embed + cp (needs .env)
#   ./scripts/deploy-extension.sh --skip-tests     # embed + cp only
#   ./scripts/deploy-extension.sh /mnt/c/.../Extensions
#   ./scripts/deploy-extension.sh --skip-tests /mnt/c/.../Extensions/burp-mcp-all.jar
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SKIP_TESTS=0
DEST_ARG=""

usage() {
  cat <<'EOF'
Build burp-mcp-all.jar and copy it to the Burp Extensions folder for live smoke.

Destination (first match wins):
  1. CLI arg: ./scripts/deploy-extension.sh /path/to/Extensions[/burp-mcp-all.jar]
  2. Env:     BURP_EXTENSION_JAR_DEST from repo-root .env (or already exported)

Usage:
  ./scripts/deploy-extension.sh                  # test + embed + cp (needs .env)
  ./scripts/deploy-extension.sh --skip-tests     # embed + cp only
  ./scripts/deploy-extension.sh /mnt/c/.../Extensions
  ./scripts/deploy-extension.sh --skip-tests /mnt/c/.../Extensions/burp-mcp-all.jar
EOF
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage 0 ;;
    --skip-tests) SKIP_TESTS=1; shift ;;
    -*)
      echo "error: unknown option: $1" >&2
      usage 1
      ;;
    *)
      if [[ -n "$DEST_ARG" ]]; then
        echo "error: unexpected extra argument: $1" >&2
        usage 1
      fi
      DEST_ARG="$1"
      shift
      ;;
  esac
done

load_dotenv() {
  local env_file="$ROOT/.env"
  [[ -f "$env_file" ]] || return 0
  # shellcheck disable=SC1090
  set -a
  # Only export simple KEY=VALUE lines (ignore comments / blanks)
  # Avoid sourcing arbitrary shell from .env
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      export "${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
    fi
  done < "$env_file"
  set +a
}

load_dotenv

resolve_dest() {
  local raw="${1:-${BURP_EXTENSION_JAR_DEST:-}}"
  if [[ -z "$raw" ]]; then
    cat >&2 <<EOF
error: no deploy destination.

Set BURP_EXTENSION_JAR_DEST in repo-root .env (see .env.example), or pass a path:

  ./scripts/deploy-extension.sh /mnt/c/Users/<you>/BurpSuite/Extensions
EOF
    exit 1
  fi

  # Expand leading ~ if present
  if [[ "$raw" == ~* ]]; then
    raw="${raw/#\~/$HOME}"
  fi

  if [[ -d "$raw" ]]; then
    printf '%s' "$raw/burp-mcp-all.jar"
  elif [[ "$raw" == *.jar ]]; then
    printf '%s' "$raw"
  else
    # Treat as directory path even if not created yet
    printf '%s' "$raw/burp-mcp-all.jar"
  fi
}

DEST="$(resolve_dest "$DEST_ARG")"
DEST_DIR="$(dirname "$DEST")"

if [[ ! -d "$DEST_DIR" ]]; then
  echo "error: destination directory does not exist: $DEST_DIR" >&2
  exit 1
fi

if [[ "$SKIP_TESTS" -eq 0 ]]; then
  echo "==> ./gradlew test"
  ./gradlew test
fi

echo "==> ./gradlew embedProxyJar"
./gradlew embedProxyJar

SRC="$ROOT/build/libs/burp-mcp-all.jar"
if [[ ! -f "$SRC" ]]; then
  echo "error: built JAR missing: $SRC" >&2
  exit 1
fi

echo "==> cp $SRC -> $DEST"
cp -f "$SRC" "$DEST"

echo "OK: deployed $(basename "$DEST") ($(wc -c < "$SRC" | tr -d ' ') bytes)"
echo
echo "Next (human only):"
echo "  1. Confirm Burp Extensions → Burp MCP Server has Auto-reload enabled (file change should reload)."
echo "  2. Cursor Settings → MCP → reload \`burp\` (agents cannot do this)."
echo "  3. Tell the agent to smoke via user-burp."
