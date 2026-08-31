# In progress & up next

**Purpose:** Handoff board for new chats — what landed in code/docs recently vs what is **not** done.  
**Shipped receipts:** [`../shipped/README.md`](../shipped/README.md) · **Sprint pointer:** [`focus.md`](./focus.md)

**Updated:** 2026-08-31

---

## Active phase (Repeater UI)

| Phase | Doc | Gate |
|-------|-----|------|
| **Target toolbar** | [`repeater-target-toolbar.md`](./repeater-target-toolbar.md) | Fix wrong-field auto-fill; then target MCP tools + Send re-smoke |
| **East sidebar** (tail) | [`repeater-east-sidebar.md`](./repeater-east-sidebar.md) | Collapse control spike |

---

## Landed in tree (recent)

| Area | What | Notes |
|------|------|--------|
| **Notes** | `get/set_repeater_tab_notes` via **Annotations** | **done** (2026.8; works with inspector rail **left or right**) |
| **East rail** | state + select MCP tools | **done**; visible=collapse **blocked** |
| **Send target gate** | `TargetMissing` when unset | **done** |
| **Send auto-fill** | `ensureTargetUrl` from Host | **blocked** — fills **search**, not Target (Health Check smoke) |
| **Request get/set** | Swing editors | **blocked** with **left** inspector rail (ambiguous editor) |

---

## In progress (implementation)

| Task | Owner | Next action |
|------|-------|-------------|
| **Toolbar Target** spike + fix | — | `~/Java/burp`: index strings → grep `Not specified` / Target UI → decompile candidates → fix or disable `ensureTargetUrl` |
| Request editor + **left rail** | — | Exclude inspector editables from `pickRequestEditor` |
| East sidebar **collapse** | — | `set_repeater_east_sidebar_visible(false)` control discovery |

---

## Local Burp RE workspace (not in git)

| Path | Role |
|------|------|
| `~/Java/burp/` | Scripts, `work/`, `findings/`, targeted `decompiled/` |
| `maintainer/phases/burp-ui-reference.md` | Agent workflow + env vars |

After JAR upgrade: re-run `inventory.sh` + `index-strings.sh --tier burp`. Persist hits with `search.sh --save <slug>`.

---

## Soon (after target toolbar)

| Task | Doc row |
|------|---------|
| `get/set_repeater_tab_target` | [`repeater-target-toolbar.md`](./repeater-target-toolbar.md) |
| Re-smoke Health Check: target → send → request get/set | same |
| `append_repeater_tab_notes` | [`tools/repeater.md`](./tools/repeater.md) |

---

## Queued (explicitly not now)

| Item | Doc |
|------|-----|
| Repeater groups | [`tools/repeater.md`](./tools/repeater.md) |
| Target scope list | [`tools/target.md`](./tools/target.md) |
| `get/set_repeater_tab_target` | [`repeater-target-toolbar.md`](./repeater-target-toolbar.md) |
| MCP tab v2/v3 | [`mcp-tab-ui.md`](./mcp-tab-ui.md) |

---

## Chances (honest)

| Goal | Likelihood | Why |
|------|------------|-----|
| **Correct get/set notes** for selected tab | **High** if Annotations/reflection spike succeeds | Data model is documented in Montoya; jar points at `setNotes` bridges |
| **Rail tab switch + restore** | **Medium–high** | Same Swing session patterns as tab strip; need spike on vertical rail controls |
| **Collapse/expand + restore** | **Medium** | UI control discovery; version-sensitive |
| **Explanations / Custom actions content** | **Medium/low** | Depends what Burp exposes per rail tab; may be read-only v1 |

---

## New chat starter prompt (copy)

```text
burp-mcp handoff (2026-08-31):

Read maintainer/phases/focus.md, in-progress.md, repeater-target-toolbar.md, burp-ui-reference.md.

Active bugs (2026.8 live smoke):
- ensureTargetUrl writes request SEARCH bar, not toolbar Target ("Not specified" still shows).
- get/set_repeater_tab_request fails with LEFT inspector rail (ambiguous editor).

Jar spike (local, outside repo):
  cd ~/Java/burp && ./scripts/init.sh
  ./scripts/index-strings.sh --tier burp   # if work/string-index.tsv missing
  ./scripts/search.sh -m string -p 'Not specified' --save target-not-specified
  ./scripts/candidates.sh --save target-candidates
  ./scripts/decompile.sh <class> --save decompile-<name>

Then fix RepeaterSend.ensureTargetUrl + request editor discovery in burp-mcp; disable auto-fill if spike inconclusive (fail-closed Send gate stays).

Constraints: no Repeater registry; no decompiled Burp in git; ./gradlew test; deploy + live smoke before ledger target rows → done.
Secondary: set_repeater_east_sidebar_visible collapse discovery (repeater-east-sidebar.md tail).
```
