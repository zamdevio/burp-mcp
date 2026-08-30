#!/usr/bin/env bash
# TCP + SSE connectivity smoke for the Burp MCP HTTP endpoint.
set -u

_MCP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/connect.sh
source "$_MCP_DIR/lib/connect.sh"

if bmcp_wants_help "${1:-}"; then
  cat <<'EOF'
check.sh — TCP + SSE smoke test to Burp MCP

Usage:
  ./scripts/mcp/check.sh [--host IP] [--port N]

Environment:
  BURP_MCP_HOST           TCP connect host (optional; auto-detected)
  BURP_MCP_WINDOWS_HOST   Alias if BURP_MCP_HOST unset
  BURP_MCP_PORT           Port (default 9876)
  BURP_MCP_TIMEOUT        Seconds (default 5)

HTTP Host/Origin always use 127.0.0.1:$PORT (required by the server).
EOF
  exit 0
fi

bmcp_parse_connect_args "$@" || exit 1

bmcp_export_mcp_connect_env || {
  bmcp_err "Could not detect MCP host — pass --host IP or set BURP_MCP_HOST"
  exit 1
}

PORT="${BURP_MCP_PORT}"
TIMEOUT="${BURP_MCP_TIMEOUT:-5}"
HOST_HEADER="${BURP_MCP_HOST_HEADER}"
CONNECT_HOST="${BURP_MCP_HOST}"
MCP_URL="http://${CONNECT_HOST}:${PORT}/"
RESPONSE_FILE="$(mktemp)"

cleanup() { rm -f "$RESPONSE_FILE"; }
trap cleanup EXIT

bmcp_require_cmd curl
bmcp_require_cmd grep

bmcp_heading "burp-mcp connectivity check"
bmcp_kv "platform" "$(bmcp_host_platform)"
bmcp_kv "connect host" "$CONNECT_HOST"
bmcp_kv "MCP port" "$PORT"
bmcp_kv "Network URL" "$MCP_URL"
bmcp_kv "HTTP Host header" "$HOST_HEADER"
if bmcp_is_wsl; then
  bmcp_kv "WSL mode" "$(bmcp_wsl_networking_mode)"
  bmcp_kv "WSL gateway" "$(bmcp_detect_gateway_ip 2>/dev/null || echo "(none)")"
fi

bmcp_heading "1. TCP connectivity"
if bmcp_tcp_open "$CONNECT_HOST" "$PORT" "$TIMEOUT"; then
  bmcp_ok "Reachable ${CONNECT_HOST}:${PORT}"
else
  bmcp_err "Cannot reach ${CONNECT_HOST}:${PORT}"
  printf '\nHints:\n'
  printf '  - Enable MCP in the Burp MCP tab (listen on 0.0.0.0 or 127.0.0.1)\n'
  printf '  - From WSL to Windows Burp, try: ./scripts/mcp/check.sh --host <windows-ip>\n'
  printf '  - Confirm port under Advanced in the MCP tab (default 9876)\n'
  exit 2
fi

bmcp_heading "2. Direct HTTP (no Host override)"
DIRECT_STATUS="$(
  curl --silent --output /dev/null --write-out '%{http_code}' --max-time "$TIMEOUT" \
    "$MCP_URL" 2>/dev/null || true
)"
case "$DIRECT_STATUS" in
  200) bmcp_ok "Endpoint accepted Host ${CONNECT_HOST}:${PORT}" ;;
  403) bmcp_warn "Reachable but rejects Host ${CONNECT_HOST}:${PORT} (expected if only 127.0.0.1 Host is allowed)" ;;
  000) bmcp_warn "No complete HTTP status from direct request" ;;
  *) bmcp_warn "Direct request returned HTTP ${DIRECT_STATUS}" ;;
esac

bmcp_heading "3. SSE with Host ${HOST_HEADER}"
CURL_EXIT=0
curl --silent --show-error --include --no-buffer --max-time "$TIMEOUT" \
  --header "Host: ${HOST_HEADER}" \
  "$MCP_URL" >"$RESPONSE_FILE" 2>&1 || CURL_EXIT=$?

cat "$RESPONSE_FILE"
echo

if grep -qE '^HTTP/[0-9.]+ 200' "$RESPONSE_FILE"; then
  bmcp_ok "MCP returned HTTP 200"
else
  bmcp_err "MCP did not return HTTP 200 with Host ${HOST_HEADER}"
  exit 3
fi

if grep -qi '^Content-Type: text/event-stream' "$RESPONSE_FILE"; then
  bmcp_ok "Endpoint is serving SSE"
else
  bmcp_err "Response is not an SSE stream"
  exit 4
fi

if grep -q '^event: endpoint' "$RESPONSE_FILE" &&
   grep -qE '^data: .*[?&]?sessionId=' "$RESPONSE_FILE"; then
  bmcp_ok "MCP session endpoint present"
else
  bmcp_err "No MCP session endpoint in SSE response"
  exit 5
fi

if [[ "$CURL_EXIT" -eq 28 ]]; then
  bmcp_ok "curl timeout expected (SSE stays open)"
elif [[ "$CURL_EXIT" -ne 0 ]]; then
  bmcp_warn "curl exited ${CURL_EXIT} after receiving the response"
fi

bmcp_heading "Result"
bmcp_ok "burp-mcp MCP endpoint is reachable"
printf 'URL:  %s\n' "$MCP_URL"
printf 'Host: %s\n' "$HOST_HEADER"
printf '\nNext: ./scripts/mcp/test.sh [--host …] [--port …]\n'
