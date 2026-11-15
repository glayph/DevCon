# PhoneDeck MCP Server & Agent Control Console

Add a built-in Model Context Protocol (MCP) server to PhoneDeck supporting both **Streamable HTTP** and **Legacy HTTP+SSE** transports, protected by a **Web UI–managed Bearer API token**, with a **full suite of phone control tools** and per-category permission toggles configurable directly from the localhost Web UI.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural choices were confirmed during clarification and govern the implementation:

- **Dual MCP Transports**: Support both modern **Streamable HTTP** (`/mcp`) and **Legacy HTTP+SSE** (`/mcp/sse` + `/mcp/messages`) concurrently so desktop/local clients (Cursor, Claude Desktop, Gemini CLI) and cloud agent orchestrators (n8n, OpenAI/Anthropic remote MCP connectors via tunnel) connect without adapters.
- **Bearer API Token Authentication**: Protect all MCP endpoints using a dedicated Bearer token (`Authorization: Bearer <token>`) generated, masked, copied, and regenerated from the Web UI—kept independent from the browser's cookie login password.
- **Full Tool Suite with Per-Category Toggles**: Expose PhoneDeck's full device, file, communications, app, screen, and shell capabilities as MCP tools, with live per-category ON/OFF switches in the Web UI so you can grant or restrict agent capabilities at any time.

---

## 1. Overview & Core Concept

- **What It Does**: Turns the 24/7 PhoneDeck bridge into a standard MCP Server that AI agents can query and invoke via JSON-RPC 2.0 to inspect device state, read/write files, manage contacts/SMS/calls/calendar, launch/install/uninstall apps, capture screen snapshots, and optionally run shell commands.
- **Target Audience / Persona**: Developers and power users connecting local coding/desktop AI assistants (over USB `adb reverse` or LAN Wi-Fi) or remote cloud agents (over Tailscale, Cloudflare Tunnel, or ngrok) to automate their Android phone safely.
- **Key Value**: Zero external bridge scripts or desktop wrappers needed—agents talk directly to the phone over standard MCP transports, while the Web UI acts as the single control plane for tokens, client config snippets, tool permissions, and live call logs.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Configure & Copy**: User opens the localhost Web UI, navigates to the new **MCP Agent** sidebar section, verifies the MCP server toggle is active, reveals/copies (or regenerates) the Bearer API token, optionally enters a public tunnel base URL for cloud agents, and copies a ready-to-paste client configuration snippet (`Streamable HTTP`, `Legacy SSE`, `Cursor / Claude`, or `cURL`).
  2. **Scope Capabilities**: User toggles individual tool categories (`Device & Telemetry`, `Hardware Controls`, `Files & Media`, `Contacts, SMS & Calls`, `Apps & Live Screen`, `Shell Terminal`) on or off; disabled categories immediately disappear from `tools/list` and return an authorization error if invoked.
  3. **Verify & Monitor**: User clicks **Test Connection (`tools/list`)** inside the Web UI's built-in MCP Inspector to preview active tools, and watches live incoming MCP agent tool calls in the activity feed.
- **Visual Identity & Theme**:
  - Follows PhoneDeck's existing **ultra-clean light-mode SaaS console** palette (`--bg-page: #f8fafc`, `--bg-surface: #ffffff`, `--bg-subtle: #f1f5f9`, `--accent: #2563eb`, crisp `#e2e8f0` hairline borders, `tabular-nums` monospace fields).
  - *Typography & Hierarchy*: Clear card headers, compact status pills (`Streamable HTTP Active`, `SSE Ready`, active tool count badge), masked monospace token bar with quick actions, and clean toggle rows per tool category.
- **Interactive Feedback & Motion**:
  - Instant toast notifications on token copy/regeneration, category toggle updates, and configuration checks; immediate `401 Unauthorized` recovery if the browser cookie expires; live status tag showing total active MCP tools and connected SSE sessions.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Transport Architecture (`Streamable HTTP` + `Legacy SSE`)**
  - *Chosen Approach*: Serve unified stateless/session-aware `POST/GET /mcp` alongside persistent `GET /mcp/sse` + `POST /mcp/messages?sessionId=...` on the same embedded server port.
  - *Why*: Cloud agent frameworks increasingly expect Streamable HTTP (`/mcp`), while many existing local MCP clients still expect Legacy SSE (`/mcp/sse`). Supporting both avoids compatibility gaps and uses the same underlying JSON-RPC dispatcher.
  - *Alternatives Considered*: Streamable HTTP only (would break older SSE-only MCP clients).

- **Decision 2: Dedicated Bearer Token vs. Browser Password Cookie**
  - *Chosen Approach*: Generate a cryptographically random `pd_mcp_...` Bearer token stored in private app preferences, configurable and revocable independently from the Web UI login password (`admin` / custom).
  - *Why*: Sharing your Web UI login password inside external agent JSON files creates unnecessary risk; a dedicated token can be rotated with one click without locking out your browser session. Also supports `?token=` fallback on `/mcp/sse` for clients that cannot inject custom headers on `EventSource`.
  - *Alternatives Considered*: Reusing the Web UI cookie or plaintext password (poor isolation and harder to rotate per-agent).

- **Decision 3: Live Per-Category Tool Filtering & Schema Enforcement**
  - *Chosen Approach*: Persist 6 boolean category flags (`device_status`, `device_controls`, `files_media`, `comms_pim`, `apps_screen`, `shell`) and filter both `tools/list` discovery and `tools/call` execution at runtime.
  - *Why*: Gives immediate zero-restart control over riskier tools (like `execute_shell` or `send_sms`) directly from the Web UI without disabling harmless read-only telemetry tools.
  - *Alternatives Considered*: Static all-or-nothing tool list (unsafe for untrusted cloud agents).

---

## 4. Technical Architecture & Data Strategy

- **Architecture & Component Diagram**:

```
  ┌────────────────────────┐      ┌──────────────────────────────────┐
  │  Local / Cloud Agent   │      │    PhoneDeck Browser Web UI      │
  │ (Cursor, Claude, Cloud)│      │  (Cookie Auth + Settings / MCP)  │
  └───────────┬────────────┘      └────────────────┬─────────────────┘
              │ Bearer Token                       │ Cookie Session
              ▼                                    ▼
  ┌──────────────────────────────────────────────────────────────────┐
  │                Embedded HTTP Bridge (Port 8080)                  │
  │  ┌─────────────────────────┐    ┌─────────────────────────────┐  │
  │  │  /mcp  (Streamable HTTP)│    │ /api/mcp/config & /tools    │  │
  │  │  /mcp/sse + /messages   │    │ /api/mcp/test               │  │
  │  └────────────┬────────────┘    └──────────────┬──────────────┘  │
  │               └──────────────┬─────────────────┘                 │
  │                              ▼                                   │
  │              ┌───────────────────────────────┐                   │
  │              │   MCP Protocol Engine &       │                   │
  │              │   Config / Token Store        │                   │
  │              └───────────────┬───────────────┘                   │
  │                              ▼                                   │
  │     ┌──────────────┬─────────────────┬───────────────────┐       │
  │     │ DeviceCtrl   │ ContentProvider │ AppMgr & Screen   │       │
  │     └──────────────┴─────────────────┴───────────────────┘       │
  └──────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - **MCP Configuration Store** (private `SharedPreferences`): Stores `mcp_enabled` (default `true`), persistent `mcp_token` (`pd_mcp_<random>`), optional `public_base_url` (for cloud tunnel snippet generation), and the 6 category enable flags (all enabled by default, toggleable anytime).
  - **SSE Session Registry**: Thread-safe map of active SSE client streams (`sessionId` -> output writer / keepalive loop) so `POST /mcp/messages` pushes JSON-RPC responses over the matching `GET /mcp/sse` stream and cleans up cleanly on disconnect.
  - **MCP Activity Feed**: Reuses the in-memory command log ring buffer to record agent tool calls (`tools/list`, `tools/call:<name>`, auth failures) for visibility in the Web UI.

- **Interactive Component & State Mapping**:
  - **Master MCP Toggle & Token Controls**: Calling the Web UI config update endpoint persists state immediately; clicking **Regenerate Token** swaps the stored token and updates all generated JSON snippets on screen.
  - **Client Config Tabs**: Switching between `Streamable HTTP`, `Legacy SSE`, `Claude / Cursor`, and `cURL` dynamically formats the URL (using `localhost`, detected LAN IP, or user-entered Tunnel URL) and active Bearer token.
  - **Category Toggle Grid**: Toggling any category sends an atomic preference update and refreshes the visible active tool count and Inspector preview.
  - **MCP JSON-RPC Dispatcher**: Handles `initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`, `resources/list`, and `resources/read`. Returns standard MCP `content` blocks (`text` JSON payloads and base64 `image` content blocks for screen cast snapshots) with `isError: true` on tool-level failures.

- **Exposed MCP Tool Suite (Across 6 Categories)**:
  1. **Device & Telemetry**: `get_device_status`, `get_permissions_status`, `get_location`, `get_bluetooth_info`, `get_usage_stats`
  2. **Hardware Controls**: `toggle_flashlight`, `set_volume`, `vibrate_device`, `play_sound`, `speak_tts`, `wake_screen`, `open_url`, `get_clipboard`, `set_clipboard`
  3. **Files & Media**: `list_files`, `read_text_file`, `write_text_file`, `create_folder`, `delete_file`, `list_media`
  4. **Comms & PIM**: `list_contacts`, `add_contact`, `list_sms`, `send_sms`, `list_calls`, `dial_number`, `list_calendar`, `add_calendar_event`, `get_notifications`
  5. **Apps & Live Screen**: `list_installed_apps`, `launch_app`, `install_apk`, `uninstall_app`, `get_screen_snapshot`
  6. **Shell Terminal**: `execute_shell`
