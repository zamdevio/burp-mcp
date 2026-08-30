#!/usr/bin/env bash
# Resolve MCP TCP host/port and export connect env for check/test.
# shellcheck shell=bash

_BMCP_LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$_BMCP_LIB_DIR/common.sh"
# shellcheck source=platform.sh
source "$_BMCP_LIB_DIR/platform.sh"

bmcp_load_repo_env() {
  local root env_file
  root="$(bmcp_repo_root)"
  env_file="$root/.env"
  [[ -f "$env_file" ]] || return 0
  set -a
  # shellcheck disable=SC1090
  source "$env_file"
  set +a
}

bmcp_detect_mcp_port() {
  printf '%s' "${BURP_MCP_PORT:-9876}"
}

# Explicit host from CLI/env (BURP_MCP_HOST, else BURP_MCP_WINDOWS_HOST alias).
bmcp_explicit_host() {
  if [[ -n "${BURP_MCP_HOST:-}" ]]; then
    printf '%s' "$BURP_MCP_HOST"
    return 0
  fi
  if [[ -n "${BURP_MCP_WINDOWS_HOST:-}" ]]; then
    printf '%s' "$BURP_MCP_WINDOWS_HOST"
    return 0
  fi
  return 1
}

# Best TCP connect host: explicit → 127.0.0.1 → (WSL) gateway → fall back.
bmcp_detect_mcp_host() {
  local port explicit gateway plat
  plat="$(bmcp_host_platform)"
  port="$(bmcp_detect_mcp_port)"
  explicit="$(bmcp_explicit_host || true)"

  if [[ "$plat" == "macos" || "$plat" == "linux" || "$plat" == "windows" ]]; then
    if [[ -n "$explicit" ]] && bmcp_tcp_open "$explicit" "$port"; then
      printf '%s' "$explicit"
      return 0
    fi
    if bmcp_tcp_open "127.0.0.1" "$port"; then
      printf '%s' "127.0.0.1"
      return 0
    fi
    if [[ -n "$explicit" ]]; then
      printf '%s' "$explicit"
      return 0
    fi
    printf '%s' "127.0.0.1"
    return 0
  fi

  # WSL: Burp often on Windows
  gateway="$(bmcp_detect_gateway_ip || true)"

  if [[ -n "$explicit" ]] && bmcp_tcp_open "$explicit" "$port"; then
    printf '%s' "$explicit"
    return 0
  fi
  if bmcp_tcp_open "127.0.0.1" "$port"; then
    printf '%s' "127.0.0.1"
    return 0
  fi
  if [[ -n "$gateway" ]] && bmcp_tcp_open "$gateway" "$port"; then
    printf '%s' "$gateway"
    return 0
  fi

  if [[ -n "$explicit" ]]; then
    printf '%s' "$explicit"
    return 0
  fi
  case "$(bmcp_wsl_networking_mode)" in
    mirrored)
      printf '%s' "127.0.0.1"
      return 0
      ;;
  esac
  if [[ -n "$gateway" ]]; then
    printf '%s' "$gateway"
    return 0
  fi
  return 1
}

# Parse --host / --port from "$@"; remaining args left in BMCP_ARGV.
bmcp_parse_connect_args() {
  BMCP_ARGV=()
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --host)
        [[ $# -ge 2 ]] || { bmcp_err "--host requires a value"; return 1; }
        export BURP_MCP_HOST="$2"
        shift 2
        ;;
      --port)
        [[ $# -ge 2 ]] || { bmcp_err "--port requires a value"; return 1; }
        export BURP_MCP_PORT="$2"
        shift 2
        ;;
      --host=*)
        export BURP_MCP_HOST="${1#--host=}"
        shift
        ;;
      --port=*)
        export BURP_MCP_PORT="${1#--port=}"
        shift
        ;;
      *)
        BMCP_ARGV+=("$1")
        shift
        ;;
    esac
  done
}

# Load .env, resolve host/port, export connect vars. Returns 1 if host unknown.
bmcp_export_mcp_connect_env() {
  local host port
  bmcp_load_repo_env
  port="$(bmcp_detect_mcp_port)"
  host="$(bmcp_detect_mcp_host)" || host=""
  [[ -n "$host" ]] || return 1
  export BURP_MCP_HOST="$host"
  export BURP_MCP_PORT="$port"
  export BURP_MCP_HOST_HEADER="127.0.0.1:${port}"
  export BURP_MCP_ORIGIN="http://127.0.0.1:${port}"
}
