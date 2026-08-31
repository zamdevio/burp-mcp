# Burp state plane (MCP)

Conceptual layer: MCP tools expose **observations and mutations** on Burp’s live UI/state, not a shadow registry.

**Repeater today:** `RepeaterToolEnvelope` + `RepeaterContextSnapshot`.  
**Other areas:** Montoya-only tools return plain text/JSON for now; adopt the same **envelope + context** pattern when Swing UI matters (Proxy queue, Intruder tabs, …).

---

## Agent contract (enforce in code + docs)

1. **No server-side tab registry** — `repeater-tab-N` is a strip index; re-list or read `context.tabs` after close/reorder.
2. **Authoritative post-mutation context** — every Repeater Swing tool captures `context` **after** the operation completes (same response). Agents should trust `context.selectedTab` / `context.tabs` in that JSON without an extra list call unless the human may have edited Burp in parallel.
3. **Failures keep state** — `ok: false` responses still include full `context` so the agent can recover (wrong id, missing editor, etc.).
4. **No fake success** — if the UI control cannot be found or verified (Notes sidebar, ambiguous editor), return structured error; never report ok when Burp did not update.
5. **`get_repeater_context`** — read-only snapshot; same shape as envelope `context`; use before long workflows or after the user switches suite tabs.

---

## Composition (why 48 tools matter)

Agents chain **read state → mutate → read state** across areas, e.g. Proxy history → create Repeater tab → set request → send → notes handoff → Intruder. The state plane makes each step **self-describing** instead of RPC + guesswork.

---

## Implementation map

| Concern | Location |
|---------|----------|
| Suite tab focus | `ui/SuiteUiSession.kt` |
| Repeater snapshot | `repeater/RepeaterContext.kt` |
| Envelope JSON | `repeater/RepeaterMcpModels.kt`, `RepeaterMcpResponse.kt` |
| Post-tool refresh | `RepeaterToolHandlers.envelope` → `refreshContext` |
| Session rules | [`suite-ui-session.md`](suite-ui-session.md) |

---

## Next (product)

- **Phase:** [`phases/repeater-east-sidebar.md`](../phases/repeater-east-sidebar.md) — Notes **`Annotations`** path first; then **`get_repeater_east_sidebar_state`**, rail visible/tab mutators with **restore** (capture snapshot at entry, restore in `finally` on EDT).
- Do **not** mark Notes MCP tools **`done`** until visible UI verify passes (no fake success from hidden editors or clipboard).
- Envelopes for **create_repeater_tab** (Montoya) with tab list refresh where feasible.
- Per-area context tools: `get_proxy_context`, … when Swing spikes land.
- lmstation/Cursor prompts: always parse JSON envelope; never assume success from `message` alone.

---

## Related

- [`suite-ui-session.md`](suite-ui-session.md) · [`repeater-ui.md`](repeater-ui.md) · [`phases/tools/repeater.md`](../phases/tools/repeater.md)
