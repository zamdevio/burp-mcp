#!/usr/bin/env bash
# Full MCP handshake: SSE session → initialize → tools/list → url_encode.
set -Eeuo pipefail

_MCP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/connect.sh
source "$_MCP_DIR/lib/connect.sh"

if bmcp_wants_help "${1:-}"; then
  cat <<'EOF'
test.sh — end-to-end Burp MCP diagnostic

Usage:
  ./scripts/mcp/test.sh [--host IP] [--port N]

Tests: SSE session, initialize, tools/list, url_encode tool call.

Environment:
  BURP_MCP_HOST, BURP_MCP_WINDOWS_HOST, BURP_MCP_PORT, BURP_MCP_TIMEOUT
EOF
  exit 0
fi

bmcp_parse_connect_args "$@" || exit 1

bmcp_export_mcp_connect_env || {
  bmcp_err "Could not detect MCP host — pass --host IP or set BURP_MCP_HOST"
  exit 1
}

PORT="${BURP_MCP_PORT}"
TIMEOUT="${BURP_MCP_TIMEOUT:-10}"
HOST_HEADER="${BURP_MCP_HOST_HEADER}"
ORIGIN="${BURP_MCP_ORIGIN}"
CONNECT_HOST="${BURP_MCP_HOST}"
BASE_URL="http://${CONNECT_HOST}:${PORT}/"

WORK_DIR="$(mktemp -d)"
SSE_FILE="${WORK_DIR}/sse.log"
POST_BODY_FILE="${WORK_DIR}/post-body.log"
SSE_PID=""
SESSION_ID=""
POST_URL=""

cleanup() {
  local exit_code=$?
  if [[ -n "$SSE_PID" ]] && kill -0 "$SSE_PID" 2>/dev/null; then
    kill "$SSE_PID" 2>/dev/null || true
    wait "$SSE_PID" 2>/dev/null || true
  fi
  rm -rf "$WORK_DIR"
  exit "$exit_code"
}
trap cleanup EXIT INT TERM

wait_for_pattern() {
  local pattern="$1"
  local description="$2"
  local attempts=$((TIMEOUT * 10))
  local i
  for ((i = 1; i <= attempts; i++)); do
    if grep -qE "$pattern" "$SSE_FILE" 2>/dev/null; then
      return 0
    fi
    if [[ -n "$SSE_PID" ]] && ! kill -0 "$SSE_PID" 2>/dev/null; then
      bmcp_err "SSE connection died while waiting for ${description}"
      bmcp_heading "SSE log"
      [[ -s "$SSE_FILE" ]] && cat "$SSE_FILE" || bmcp_warn "empty"
      return 1
    fi
    sleep 0.1
  done
  bmcp_err "Timed out after ${TIMEOUT}s waiting for ${description}"
  bmcp_heading "SSE log"
  [[ -s "$SSE_FILE" ]] && cat "$SSE_FILE" || bmcp_warn "empty"
  return 1
}

post_json() {
  local label="$1"
  local json_payload="$2"
  local status
  : >"$POST_BODY_FILE"
  status="$(
    curl --silent --show-error --output "$POST_BODY_FILE" --write-out '%{http_code}' \
      --max-time "$TIMEOUT" \
      --header "Host: ${HOST_HEADER}" \
      --header "Origin: ${ORIGIN}" \
      --header 'Content-Type: application/json' \
      --header 'Accept: application/json, text/event-stream' \
      --data "$json_payload" \
      "$POST_URL"
  )"
  if [[ "$status" != "202" && "$status" != "200" ]]; then
    bmcp_err "${label} returned HTTP ${status}"
    [[ -s "$POST_BODY_FILE" ]] && { printf '\nResponse:\n'; cat "$POST_BODY_FILE"; printf '\n'; }
    return 1
  fi
  bmcp_ok "${label} accepted (HTTP ${status})"
}

bmcp_require_cmd curl
bmcp_require_cmd grep
bmcp_require_cmd sed

bmcp_heading "burp-mcp end-to-end MCP test"
printf 'Connect host:     %s\n' "$CONNECT_HOST"
printf 'MCP port:         %s\n' "$PORT"
printf 'Base URL:         %s\n' "$BASE_URL"
printf 'HTTP Host header: %s\n' "$HOST_HEADER"
printf 'Origin:           %s\n' "$ORIGIN"
printf 'Timeout:          %ss\n' "$TIMEOUT"

bmcp_heading "1. Open SSE connection"
curl --silent --show-error --no-buffer \
  --header "Host: ${HOST_HEADER}" \
  --header "Origin: ${ORIGIN}" \
  --header 'Accept: text/event-stream' \
  "$BASE_URL" >"$SSE_FILE" 2>&1 &
SSE_PID=$!
bmcp_info "SSE PID ${SSE_PID}"

wait_for_pattern '^data: .*[?&]sessionId=[^[:space:]]+' 'MCP session endpoint'

SESSION_ID="$(
  sed -nE 's/^data: .*[?&]sessionId=([^[:space:]&]+).*$/\1/p' "$SSE_FILE" | head -n 1
)"
if [[ -z "$SESSION_ID" ]]; then
  bmcp_err "No session ID in SSE stream"
  exit 1
fi
POST_URL="${BASE_URL}?sessionId=${SESSION_ID}"
bmcp_ok "SSE session created"
printf 'Session ID: %s\n' "$SESSION_ID"

bmcp_heading "2. Initialize"
post_json 'initialize' '{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": { "name": "burp-mcp-diagnostic", "version": "1.0.0" }
  }
}'
wait_for_pattern '"id"[[:space:]]*:[[:space:]]*1.*"result"' 'initialize response'
if grep -qE '"name"[[:space:]]*:[[:space:]]*"(burp-suite|burp-mcp)"' "$SSE_FILE"; then
  bmcp_ok "Server identified in initialize result"
else
  bmcp_warn "Initialize OK; expected server name not found in stream (may still be fine)"
fi

bmcp_heading "3. notifications/initialized"
post_json 'notifications/initialized' '{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
}'

bmcp_heading "4. tools/list"
post_json 'tools/list' '{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}'
wait_for_pattern '"id"[[:space:]]*:[[:space:]]*2.*"tools"' 'tools/list response'
TOOL_COUNT="$(
  grep '"id"[[:space:]]*:[[:space:]]*2' "$SSE_FILE" |
    grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' |
    wc -l | tr -d ' '
)"
bmcp_ok "tools/list returned"
printf 'Detected tools: %s\n' "$TOOL_COUNT"

bmcp_heading "5. tools/call url_encode"
post_json 'tools/call: url_encode' '{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "url_encode",
    "arguments": { "content": "hello world" }
  }
}'
wait_for_pattern '"id"[[:space:]]*:[[:space:]]*3.*("result"|"error")' 'url_encode response'
TOOL_RESPONSE="$(grep '"id"[[:space:]]*:[[:space:]]*3' "$SSE_FILE" | tail -n 1)"
if printf '%s' "$TOOL_RESPONSE" | grep -q '"error"[[:space:]]*:'; then
  bmcp_err "url_encode returned an error"
  printf '%s\n' "$TOOL_RESPONSE"
  exit 1
fi
if printf '%s' "$TOOL_RESPONSE" | grep -q 'hello%20world'; then
  bmcp_ok "url_encode → hello%20world"
else
  bmcp_ok "url_encode returned a successful result"
  printf 'Response: %s\n' "$TOOL_RESPONSE"
fi

bmcp_heading "Result"
bmcp_ok "burp-mcp MCP is fully usable"
printf '\nVerified: SSE, session, initialize, tools/list, url_encode\n'
printf 'URL:   %s\n' "$BASE_URL"
printf 'Tools: %s\n' "$TOOL_COUNT"
