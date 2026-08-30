#!/usr/bin/env bash
# Shared logging / help for scripts/mcp.
# shellcheck shell=bash

bmcp_info()  { printf '\033[1;34m[INFO]\033[0m %s\n' "$*"; }
bmcp_ok()    { printf '\033[1;32m[PASS]\033[0m %s\n' "$*"; }
bmcp_warn()  { printf '\033[1;33m[WARN]\033[0m %s\n' "$*"; }
bmcp_err()   { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; }
bmcp_kv()    { printf '  %-22s %s\n' "$1" "$2"; }

bmcp_heading() {
  printf '\n\033[1;36m%s\033[0m\n' "$1"
}

bmcp_require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    bmcp_err "Required command not found: $1"
    exit 1
  fi
}

bmcp_wants_help() {
  case "${1:-}" in
    -h|--help|help) return 0 ;;
    *) return 1 ;;
  esac
}

bmcp_repo_root() {
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
  printf '%s' "$here"
}
