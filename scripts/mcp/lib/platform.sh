#!/usr/bin/env bash
# Platform + WSL gateway helpers for scripts/mcp.
# shellcheck shell=bash

bmcp_host_platform() {
  local uname_s
  uname_s="$(uname -s 2>/dev/null || echo unknown)"
  case "$uname_s" in
    Darwin) printf 'macos'; return 0 ;;
    MINGW*|MSYS*|CYGWIN*) printf 'windows'; return 0 ;;
    Linux)
      if [[ -n "${WSL_DISTRO_NAME:-}" ]] ||
         grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null ||
         [[ -d /mnt/c/Windows ]]; then
        printf 'wsl'
      else
        printf 'linux'
      fi
      return 0
      ;;
    *) printf 'unknown'; return 0 ;;
  esac
}

bmcp_is_wsl() {
  [[ "$(bmcp_host_platform)" == "wsl" ]]
}

# Default gateway IP on WSL (Windows host on NAT). Empty off WSL / if unavailable.
bmcp_detect_gateway_ip() {
  bmcp_is_wsl || return 1
  ip route show default 2>/dev/null | awk '/default/ {print $3; exit}'
}

# Mirrored | NAT | unknown (best-effort from .wslconfig when readable).
bmcp_wsl_networking_mode() {
  bmcp_is_wsl || { printf 'n/a'; return 0; }
  local cfg
  for cfg in /mnt/c/Users/*/.wslconfig "$HOME/.wslconfig"; do
    [[ -f "$cfg" ]] || continue
    if grep -qiE '^\s*networkingMode\s*=\s*mirrored' "$cfg" 2>/dev/null; then
      printf 'mirrored'
      return 0
    fi
  done
  printf 'nat'
}

bmcp_tcp_open() {
  local host=$1 port=$2 timeout=${3:-2}
  [[ -n "$host" && -n "$port" ]] || return 1
  if command -v nc >/dev/null 2>&1; then
    nc -z -w "$timeout" "$host" "$port" 2>/dev/null
    return $?
  fi
  if command -v timeout >/dev/null 2>&1; then
    timeout "$timeout" bash -c "echo >/dev/tcp/${host}/${port}" 2>/dev/null
    return $?
  fi
  (echo >/dev/tcp/"${host}"/"${port}") >/dev/null 2>&1
}
