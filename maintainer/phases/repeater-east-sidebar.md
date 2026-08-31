# Phase — Repeater east sidebar (Notes + rail)

**Status:** **Active — ship this slice before marking Notes `done`.**  
**Not end-user docs.** User-facing limits stay in [`docs/repeater.md`](../../docs/repeater.md) until tools are verified.

**Ledgers:** [`tools/repeater.md`](./tools/repeater.md) (tool rows) · **UI rules:** [`repeater-ui.md`](./repeater-ui.md) · **State:** [`../systems/state-plane.md`](../systems/state-plane.md) · **Jar spike (local):** `maintainer/temp/jar-spike/` (gitignored class extracts only)

---

## Scope (what “Notes done” actually means)

We are **not** only fixing two MCP tools. The product slice is **full east-rail behavior** for agents:

| Layer | Human UI (Burp 2026.x) | MCP goal |
|-------|------------------------|----------|
| **Data** | Per-tab notes (rich text in east panel) | Read/write **correct** plain text for **selected message tab** |
| **Rail tab** | Vertical tabs: **Notes**, **Explanations**, **Custom act…** | Select rail tab; **restore** previous rail tab after tool |
| **Rail visibility** | Collapse / expand east sidebar | Set visible/collapsed; **restore** prior visibility after tool |
| **Session** | User may be on Proxy, another Repeater tab, another rail tab | Same as strip: **select → settle → act → restore** (see [`repeater-ui.md`](./repeater-ui.md)) |

**Out of this phase (later):** Repeater **groups**, gear **per-tab settings**, Target toolbar — see [`tools/repeater.md`](./tools/repeater.md).

---

## Why Notes are still `blocked`

| Attempt | Result |
|---------|--------|
| Swing walk (`JTextComponent`, `JEditorPane`, reflective scrolls) | MCP sometimes reads **wrong** hidden editors; **visible** east Notes text often **not** in walked tree (`burp.Zwlc` HTML pane). |
| `scan_repeater_notes_ui` + needle | **No** `matchesNeedle` on components; **`clipboardPreview`** often null or wrong focus. |
| Clipboard / Robot fallback | **Unreliable** — can copy **request** editor; not acceptable for production. |

**Good path (2026.8 jar spike, read-only):**

- Tab notes are **`burp.api.montoya.core.Annotations`**: `notes()`, `setNotes()`, `hasNotes()` (same model family as Proxy history — we already serialize annotations elsewhere).
- Montoya **`Repeater`** public API is still **`sendToRepeater` only** — no tab annotations API.
- Internal hints (class names only, do **not** commit decompiled sources): mutable bridge e.g. **`Zveg`** (`Supplier`/`Consumer` for notes); tab bundle e.g. **`Zemd`** (`HttpRequestResponse` + `HttpEditor` suppliers); UI renderer **`Zwlc`** extends `JTextPane` (not trustworthy per-instance `getText()` for 2026.x).

**Implementation direction:** bind selected Repeater tab → **`Annotations`** (Montoya if added, else **in-process reflection** on Burp types from jar spike — **no** JAR patching, **no** checked-in decompile).

---

## Ship order (do not skip)

Ship and live-smoke **in this order**. Do **not** mark `get/set_repeater_tab_notes` **`done`** until step 1 passes visual + MCP verify on Burp 2026.x.

### Step 1 — Notes data path (P0)

| Item | Status |
|------|--------|
| `get_repeater_tab_notes` | **blocked** — must match **visible** east Notes |
| `set_repeater_tab_notes` | **blocked** — fail-closed verify |
| `scan_repeater_notes_ui` | **done** (debug only; keep for regression) |
| Remove or gate **Robot/clipboard** behind explicit debug flag once Annotations path works | todo |

**Done criteria:** User types unique string in Notes → MCP get returns it → set replaces → **Burp UI** shows new text → post-mutation **`context`** in envelope.

### Step 2 — East rail **observation** (P0 for agents)

| MCP tool (proposed) | Capability | Status |
|---------------------|------------|--------|
| `get_repeater_east_sidebar_state` | `{ visible, selectedRailTab, … }` | todo |

Agents need read before write. Optional fields discovered during spike (width collapsed, which tabs exist).

### Step 3 — East rail **mutation + restore** (P0)

| MCP tool | Capability | Restore |
|----------|------------|---------|
| `set_repeater_east_sidebar_visible` | Collapse / expand rail | Prior visibility |
| `select_repeater_east_sidebar_tab` | Notes / Explanations / Custom… | Prior rail tab |

**Rules:** Capture **full east-sidebar snapshot** at tool entry; restore in `finally` (EDT), same pattern as `RepeaterTabSession` for message tabs. Document snapshot shape in [`state-plane.md`](../systems/state-plane.md).

### Step 4 — Notes siblings (P1, same phase)

| MCP tool | Capability | Status |
|----------|------------|--------|
| `append_repeater_tab_notes` | Append to notes | todo |
| Explanations / Custom actions | Read-only or tool-specific | todo after spike |

Only after Steps 1–3 are green.

---

## Code map (today)

| Area | Files |
|------|--------|
| Notes Swing (legacy path) | `repeater/RepeaterNotes.kt`, `RepeaterNotesSidebar.kt`, `RepeaterNotesReflective.kt`, `RepeaterNotesScanner.kt` |
| Clipboard fallback (demote when Step 1 lands) | `repeater/RepeaterNotesClipboard.kt` |
| Context + envelope | `RepeaterContext.kt`, `RepeaterMcpModels.kt`, `RepeaterToolHandlers.kt` |
| Next spike module (name TBD) | e.g. `repeater/RepeaterTabAnnotations.kt` — Montoya/reflection only |

---

## Montoya ask (upstream)

Document in [`montoya-gaps.md`](./montoya-gaps.md): **Repeater tab → `HttpRequestResponse` / `Annotations`** (or dedicated notes API) so extensions avoid obfuscated reflection.

---

## Live smoke checklist (this phase)

1. `./gradlew test` → `./scripts/deploy-extension.sh` → reload MCP `burp`.
2. Repeater focused; one tab with known Notes string.
3. `get_repeater_context` → `scan_repeater_notes_ui` (needle) → `get/set_repeater_tab_notes`.
4. After rail tools exist: mutate notes **without** leaving user on wrong rail tab or collapsed state they didn’t have.

---

## Related queued work (not this phase)

- Repeater **groups** — [`tools/repeater.md`](./tools/repeater.md) (was “Now” in focus; **after** east sidebar).
- CepatEdge tab recreate / gitignored plans — user project only.
