# Burp UI reference — local decompile (agents)

**Purpose:** Where to read **real** Burp Repeater/Swing structure instead of guessing with blind `JTextField` / tree walks.  
**Never commit** decompiled Burp sources into this repo (see [`focus.md`](./focus.md) · Not now).

---

## Prefer this over blind Swing heuristics

| Approach | When it wins | Cost |
|----------|----------------|------|
| **Montoya / Annotations** | API exists for the data | Low — use first |
| **Local full JAR decompile + grep** | Toolbar Target, rail collapse, editor boundaries, new 2026.x widgets | One-time ~700MB extract; then fast targeted reads |
| **Runtime tree scan + heuristics** | Last resort or debug (`scan_repeater_notes_ui`) | High — wrong-field bugs (e.g. Target → request **search**), long smoke loops |

Same playbook as **Notes**: jar spike → class names (`Zemd`, etc.) → narrow reflection or Swing path in extension code. Do **not** ship “pick any blank text field” logic for toolbar controls.

**Small extracts:** optional class-only copies under `maintainer/temp/jar-spike/` (gitignored) for quick `javap` — not a substitute for searchable decompile when designing a new tool.

---

## Maintainer workstation (WSL)

| Item | Path |
|------|------|
| **RE workspace** | `~/Java/burp/` — read **`README.md`** + **`scripts/README.md`** |
| **Burp Pro JAR** | `~/Java/burp/original/burpsuite_pro_v2026.8.jar` (or `BURP_JAR_PATH`) |
| **Targeted decompile** | `~/Java/burp/decompiled/` via `./scripts/decompile.sh` |

Agents: start with **`cd ~/Java/burp && ./scripts/init.sh`**, then **`search.sh` / `classes.sh` / `candidates.sh` with `--save`** — do **not** full-JAR CFR by default.

**Note:** `~/Java/burp` is **outside** this git repo (local workstation only). Scripts live on disk there; this doc is the agent pointer.

## Workflow (preferred)

1. `./scripts/inventory.sh` (once per JAR version)
2. **String index (once per JAR / tier):** `./scripts/index-strings.sh --tier burp` → `work/string-index.tsv` (~21k `burp/*` classes). Without this, `search.sh -m string` is slow (parallel zipgrep).
3. `./scripts/search.sh -m string -p 'Not specified' --save target-not-specified` (uses index in ms) or `./scripts/find-string.sh -p '…' --save adhoc` (no index, slow)
4. `./scripts/candidates.sh --save candidates`
5. `./scripts/decompile.sh burp.Class --save decompile-<name>`
6. Class names only back into phase docs — no proprietary source in git

Legacy one-shot full CFR is discouraged; use indexed search + targeted decompile.

### Env (optional)

Copy from [`.env.example`](../../.env.example): `BURP_JAR_PATH`, `BURP_DECOMPILE_ROOT=~/Java/burp`

---

## Linked phases

- **Target toolbar:** [`repeater-target-toolbar.md`](./repeater-target-toolbar.md)
- **East / inspector rail:** [`repeater-east-sidebar.md`](./repeater-east-sidebar.md)
- **Swing rules:** [`repeater-ui.md`](./repeater-ui.md)
