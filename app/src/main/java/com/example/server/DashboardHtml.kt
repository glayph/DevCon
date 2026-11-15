package com.example.server

object DashboardHtml {

    fun get(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PhoneDeck &bull; 24/7 Device Console</title>
<style>
:root {
  --bg-page: #f8fafc;
  --bg-surface: #ffffff;
  --bg-subtle: #f1f5f9;
  --border-color: #e2e8f0;
  --border-strong: #cbd5e1;
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-muted: #94a3b8;
  --accent: #2563eb;
  --accent-soft: #eff6ff;
  --accent-hover: #1d4ed8;
  --green: #10b981;
  --green-soft: #ecfdf5;
  --rose: #e11d48;
  --rose-soft: #fff1f2;
  --amber: #d97706;
  --amber-soft: #fffbeb;
  --sidebar-w: 252px;
  --radius-lg: 12px;
  --radius-md: 8px;
  --radius-sm: 6px;
  --font-sans: -apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", Roboto, sans-serif;
  --font-mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  background: var(--bg-page);
  color: var(--text-primary);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.5;
  min-height: 100vh;
  display: flex;
  font-variant-numeric: tabular-nums;
  -webkit-font-smoothing: antialiased;
}

/* SIDEBAR */
.sidebar {
  width: var(--sidebar-w);
  background: var(--bg-surface);
  border-right: 1px solid var(--border-color);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  display: flex;
  flex-direction: column;
  z-index: 200;
  transition: transform 0.2s ease;
}

.sidebar-brand {
  padding: 16px 18px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 0 3px var(--green-soft);
  transition: background 0.2s, box-shadow 0.2s;
}
.status-dot.offline {
  background: var(--rose);
  box-shadow: 0 0 0 3px var(--rose-soft);
}

.brand-title {
  font-weight: 700;
  font-size: 16px;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.nav-section {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  padding: 12px 10px 6px;
}

.nav-item {
  width: 100%;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  padding: 8px 10px;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  text-align: left;
  transition: background 0.15s ease, color 0.15s ease;
  margin-bottom: 2px;
}

.nav-item:hover {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.nav-item-label {
  display: flex;
  align-items: center;
  gap: 9px;
}

.perm-badge {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--border-strong);
  flex-shrink: 0;
}
.perm-badge.ok { background: var(--green); }
.perm-badge.missing { background: var(--rose); }

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
}

/* MAIN SHELL */
.main-wrap {
  margin-left: var(--sidebar-w);
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.menu-btn {
  display: none;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  cursor: pointer;
  font-size: 16px;
}

.page-heading {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-pills {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  background: var(--bg-subtle);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 4px 11px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.pill.live { background: var(--green-soft); border-color: #a7f3d0; color: #065f46; }
.pill.warn { background: var(--rose-soft); border-color: #fecdd3; color: #9f1239; }

.content {
  padding: 22px 24px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

.page { display: none; animation: fadeIn 0.14s ease; }
.page.active { display: block; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(2px); } to { opacity: 1; transform: translateY(0); } }

/* CARDS & LAYOUT */
.grid-2 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}
.grid-4 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.card-title {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-sub {
  font-size: 12px;
  color: var(--text-muted);
}

/* BUTTONS & INPUTS */
.btn {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s ease;
  text-decoration: none;
  white-space: nowrap;
}
.btn:hover {
  background: var(--bg-subtle);
  border-color: var(--border-strong);
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--accent);
  color: #ffffff;
  border-color: var(--accent);
}
.btn-primary:hover {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
}

.btn-danger {
  background: var(--rose-soft);
  color: var(--rose);
  border-color: #fecdd3;
}
.btn-danger:hover {
  background: var(--rose);
  color: #fff;
}

.btn-sm {
  padding: 5px 10px;
  font-size: 12px;
}

.btn-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.input {
  background: var(--bg-page);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  width: 100%;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--bg-surface);
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.chip {
  background: var(--bg-subtle);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 4px 11px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
}
.chip:hover, .chip.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

/* SLIDERS */
.slider-row { margin-bottom: 12px; }
.slider-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.slider {
  width: 100%;
  accent-color: var(--accent);
  cursor: pointer;
}

/* DROPZONE & PROGRESS */
.dropzone {
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius-lg);
  background: var(--bg-page);
  padding: 18px;
  text-align: center;
  transition: border-color 0.15s, background 0.15s;
  margin-bottom: 14px;
}
.dropzone.dragover {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.progress-track {
  width: 100%;
  height: 8px;
  background: var(--bg-subtle);
  border-radius: 99px;
  overflow: hidden;
  margin-top: 8px;
}
.progress-fill {
  height: 100%;
  width: 0%;
  background: var(--accent);
  border-radius: 99px;
  transition: width 0.12s linear;
}

/* SCREEN CAST STAGE */
.cast-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(260px, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 960px) {
  .cast-layout { grid-template-columns: 1fr; }
}
.cast-stage {
  background: #0f172a;
  border-radius: var(--radius-lg);
  border: 1px solid #1e293b;
  min-height: 440px;
  max-height: 75vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
.cast-stage img {
  max-width: 100%;
  max-height: 74vh;
  object-fit: contain;
  display: none;
  border-radius: 6px;
}
.cast-placeholder {
  color: #94a3b8;
  text-align: center;
  padding: 28px;
  max-width: 340px;
}

/* TABLES & LISTS */
.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
th {
  background: var(--bg-subtle);
  text-align: left;
  padding: 10px 14px;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
}
td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}
tr:last-child td { border-bottom: none; }
tr:hover td { background: var(--bg-page); }

.tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.tag-green { background: var(--green-soft); color: var(--green); }
.tag-rose { background: var(--rose-soft); color: var(--rose); }
.tag-blue { background: var(--accent-soft); color: var(--accent); }
.tag-amber { background: var(--amber-soft); color: var(--amber); }

.mono { font-family: var(--font-mono); font-size: 12px; }

/* MEDIA GALLERY */
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}
.media-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-surface);
  display: flex;
  flex-direction: column;
}
.media-thumb {
  width: 100%;
  height: 130px;
  object-fit: cover;
  background: var(--bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--text-muted);
}
.media-info { padding: 8px 10px; font-size: 12px; }
.media-name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* TERMINAL */
.terminal-box {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: var(--radius-md);
  padding: 14px;
  font-family: var(--font-mono);
  font-size: 12.5px;
  height: 360px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

/* PERMISSION BANNER */
.perm-alert {
  background: var(--amber-soft);
  border: 1px solid #fde68a;
  color: #92400e;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

/* STAT BOX */
.stat-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 16px;
}
.stat-label { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; }
.stat-value { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-top: 4px; }
.stat-sub { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

#toast-wrap {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 999;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.toast {
  background: var(--text-primary);
  color: #fff;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.18);
}

@media (max-width: 860px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar.open { transform: translateX(0); box-shadow: 0 10px 30px rgba(0,0,0,0.15); }
  .main-wrap { margin-left: 0; }
  .menu-btn { display: inline-flex; }
}
</style>
</head>
<body>

<aside class="sidebar" id="sidebar">
  <div class="sidebar-brand">
    <div class="brand-left">
      <div class="status-dot" id="conn-dot"></div>
      <span class="brand-title">PhoneDeck</span>
    </div>
    <span class="mono" style="color:var(--text-muted);" id="ping-label">--ms</span>
  </div>

  <nav class="sidebar-nav">
    <div class="nav-section">Workspace</div>
    <button class="nav-item active" onclick="openPage('controls', 'Controls & Overview')">
      <span class="nav-item-label"><span>🎛️</span> Controls</span>
    </button>
    <button class="nav-item" onclick="openPage('screen', 'Live Screen Cast'); refreshCastStatus();">
      <span class="nav-item-label"><span>📺</span> Live Screen Cast</span>
      <span class="perm-badge" id="badge-screen_cast"></span>
    </button>
    <button class="nav-item" onclick="openPage('telemetry', 'Telemetry & Hardware'); fetchStatus();">
      <span class="nav-item-label"><span>📊</span> Telemetry</span>
    </button>
    <button class="nav-item" onclick="openPage('mcp', 'MCP Agent & Remote Control'); loadMcpConfig();">
      <span class="nav-item-label"><span>🤖</span> MCP Agent</span>
      <span class="tag tag-blue" id="side-mcp-badge" style="font-size:10px; padding:1px 6px;">MCP</span>
    </button>

    <div class="nav-section">Files &amp; Content</div>
    <button class="nav-item" onclick="openPage('files', 'Files & Storage'); loadFiles(currentDir);">
      <span class="nav-item-label"><span>📁</span> Files &amp; Storage</span>
      <span class="perm-badge" id="badge-all_files"></span>
    </button>
    <button class="nav-item" onclick="openPage('media', 'Media Gallery'); loadMedia('images');">
      <span class="nav-item-label"><span>🖼️</span> Media Gallery</span>
      <span class="perm-badge" id="badge-media"></span>
    </button>
    <button class="nav-item" onclick="openPage('contacts', 'Contacts'); loadContacts();">
      <span class="nav-item-label"><span>👤</span> Contacts</span>
      <span class="perm-badge" id="badge-contacts"></span>
    </button>
    <button class="nav-item" onclick="openPage('sms', 'SMS & Messages'); loadSms();">
      <span class="nav-item-label"><span>💬</span> SMS &amp; Messages</span>
      <span class="perm-badge" id="badge-sms"></span>
    </button>
    <button class="nav-item" onclick="openPage('calls', 'Call Logs & Phone'); loadCalls();">
      <span class="nav-item-label"><span>📞</span> Call Logs &amp; Phone</span>
      <span class="perm-badge" id="badge-calls_phone"></span>
    </button>
    <button class="nav-item" onclick="openPage('calendar', 'Calendar Events'); loadCalendar();">
      <span class="nav-item-label"><span>📅</span> Calendar</span>
      <span class="perm-badge" id="badge-calendar"></span>
    </button>

    <div class="nav-section">Device &amp; Apps</div>
    <button class="nav-item" onclick="openPage('apps', 'Apps & Installer'); loadApps(); loadUsage(); loadAppOpStatus();">
      <span class="nav-item-label"><span>📦</span> Apps &amp; Installer</span>
      <span class="perm-badge" id="badge-install_packages"></span>
    </button>
    <button class="nav-item" onclick="openPage('location', 'GPS Location'); loadLocation();">
      <span class="nav-item-label"><span>📍</span> Location</span>
      <span class="perm-badge" id="badge-location"></span>
    </button>
    <button class="nav-item" onclick="openPage('notifications', 'Notifications'); loadNotifications();">
      <span class="nav-item-label"><span>🔔</span> Notifications</span>
      <span class="perm-badge" id="badge-notification_listener"></span>
    </button>
    <button class="nav-item" onclick="openPage('bluetooth', 'Bluetooth & Network'); loadBluetooth();">
      <span class="nav-item-label"><span>📶</span> Bluetooth &amp; Network</span>
      <span class="perm-badge" id="badge-bluetooth"></span>
    </button>
    <button class="nav-item" onclick="openPage('terminal', 'Terminal & API')">
      <span class="nav-item-label"><span>💻</span> Terminal &amp; API</span>
    </button>
    <button class="nav-item" onclick="openPage('permissions', 'Permissions Status'); fetchPermissions();">
      <span class="nav-item-label"><span>🔐</span> Permissions</span>
    </button>
    <button class="nav-item" onclick="openPage('settings', 'Settings & Security'); refreshAuthInfo();">
      <span class="nav-item-label"><span>⚙️</span> Settings</span>
    </button>
  </nav>

  <div class="sidebar-footer">
    <div style="font-weight:600; color:var(--text-primary);" id="side-device-name">Connecting...</div>
    <div id="side-android-ver" style="font-size:11px; color:var(--text-muted);">24/7 Service</div>
  </div>
</aside>

<div class="main-wrap">
  <header class="topbar">
    <div class="topbar-left">
      <button class="menu-btn" onclick="toggleSidebar()">☰</button>
      <h1 class="page-heading" id="page-title">Controls &amp; Overview</h1>
    </div>
    <div class="topbar-pills">
      <div class="pill live" id="top-conn-pill">● 24/7 Online</div>
      <div class="pill"><span>🔋</span><span id="top-bat">--%</span></div>
      <div class="pill" id="top-wakelock">⚡ Awake</div>
      <button class="btn btn-sm" onclick="fetchPermissions(); fetchStatus();">↻ Sync</button>
      <button class="btn btn-sm" onclick="logoutSession()" title="Lock console session">🔒 Lock</button>
    </div>
  </header>

  <main class="content">

    <!-- PAGE: CONTROLS -->
    <section id="page-controls" class="page active">
      <div class="grid-2">
        <div class="card">
          <div class="card-header">
            <span class="card-title">Hardware Actions</span>
            <span class="card-sub">Direct control</span>
          </div>
          <div class="btn-grid">
            <button class="btn" onclick="runAction('torch_toggle')">💡 Torch</button>
            <button class="btn" onclick="runAction('wake_screen')">⚡ Wake</button>
            <button class="btn" onclick="runAction('play_sound', {type:'beep'})">🔔 Beep</button>
            <button class="btn" onclick="runAction('play_sound', {type:'ring'})">📞 Ring</button>
            <button class="btn" onclick="runAction('vibrate', {pattern:'pulse', duration:300})">📳 Vibrate</button>
            <button class="btn" onclick="runAction('set_volume', {stream:'media', percent:0})">🔇 Mute</button>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <span class="card-title">Audio Volumes</span>
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>Media</span><span id="lbl-media">--%</span></div>
            <input type="range" class="slider" id="rng-media" min="0" max="100" onchange="setVol('media', this.value)">
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>Ring</span><span id="lbl-ring">--%</span></div>
            <input type="range" class="slider" id="rng-ring" min="0" max="100" onchange="setVol('ring', this.value)">
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>Alarm</span><span id="lbl-alarm">--%</span></div>
            <input type="range" class="slider" id="rng-alarm" min="0" max="100" onchange="setVol('alarm', this.value)">
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <span class="card-title">Two-Way Clipboard</span>
            <button class="btn btn-sm" onclick="refreshClip()">Refresh</button>
          </div>
          <div id="clip-box" class="mono" style="background:var(--bg-page); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:10px; min-height:42px; margin-bottom:10px; word-break:break-all;">
            (Click Refresh to read phone clipboard)
          </div>
          <div style="display:flex; gap:8px;">
            <input type="text" id="clip-input" class="input" placeholder="Send text to phone clipboard...">
            <button class="btn btn-primary" onclick="pushClip()">Send</button>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <span class="card-title">Text-to-Speech &amp; URL</span>
          </div>
          <div style="display:flex; gap:8px; margin-bottom:10px;">
            <input type="text" id="tts-input" class="input" placeholder="Speak text on phone...">
            <button class="btn btn-primary" onclick="speakText()">Speak</button>
          </div>
          <div style="display:flex; gap:8px;">
            <input type="text" id="url-input" class="input" placeholder="Open URL on phone (https://...)">
            <button class="btn" onclick="openUrl()">Open</button>
          </div>
        </div>
      </div>
    </section>

    <!-- PAGE: LIVE SCREEN CAST -->
    <section id="page-screen" class="page">
      <div class="cast-layout">
        <div class="card" style="margin-bottom:0;">
          <div class="card-header">
            <div>
              <span class="card-title">Live Device Screen</span>
              <span class="tag tag-blue" id="cast-res-tag" style="margin-left:8px;">Offline</span>
            </div>
            <div style="display:flex; gap:6px; flex-wrap:wrap;">
              <button class="btn btn-primary btn-sm" onclick="requestScreenCast()">▶ Start Cast</button>
              <button class="btn btn-danger btn-sm" onclick="stopScreenCast()">■ Stop</button>
              <a class="btn btn-sm" href="/api/screen/frame" download="phonedeck_screen.jpg">📷 Snapshot</a>
              <button class="btn btn-sm" onclick="toggleCastFullscreen()">⛶ Fullscreen</button>
            </div>
          </div>
          <div class="cast-stage" id="cast-stage">
            <img id="cast-img" alt="Live Phone Screen" />
            <div class="cast-placeholder" id="cast-placeholder">
              <div style="font-size:32px; margin-bottom:10px;">📺</div>
              <div style="font-weight:600; color:#e2e8f0; margin-bottom:6px;">Live Screen Cast Ready</div>
              <div style="font-size:12.5px; margin-bottom:14px;">Click <b>Start Cast</b> to trigger screen capture consent on your phone and begin live MJPEG streaming.</div>
              <button class="btn btn-primary" onclick="requestScreenCast()">Start Live Cast</button>
            </div>
          </div>
        </div>

        <div class="card" style="margin-bottom:0;">
          <div class="card-header">
            <span class="card-title">Stream Optimization</span>
            <button class="btn btn-sm" onclick="refreshCastStatus()">↻ Check</button>
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>Frame Rate (FPS)</span><span class="mono" id="cast-fps-lbl">8 fps</span></div>
            <input type="range" class="slider" id="cast-fps" min="2" max="15" value="8" oninput="document.getElementById('cast-fps-lbl').textContent=this.value+' fps'" onchange="updateCastConfig()">
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>JPEG Quality</span><span class="mono" id="cast-qual-lbl">65%</span></div>
            <input type="range" class="slider" id="cast-qual" min="30" max="90" value="65" oninput="document.getElementById('cast-qual-lbl').textContent=this.value+'%'" onchange="updateCastConfig()">
          </div>
          <div class="slider-row">
            <div class="slider-meta"><span>Resolution Scale</span><span class="mono" id="cast-scale-lbl">65%</span></div>
            <input type="range" class="slider" id="cast-scale" min="35" max="100" step="5" value="65" oninput="document.getElementById('cast-scale-lbl').textContent=this.value+'%'" onchange="updateCastConfig()">
          </div>
          <div style="background:var(--bg-subtle); border-radius:var(--radius-md); padding:12px; font-size:12px; color:var(--text-secondary);">
            <div><b>Frames streamed:</b> <span class="mono" id="cast-frames">0</span></div>
            <div style="margin-top:4px;"><b>Tip:</b> Lower resolution scale (50–65%) gives the smoothest framerate over Wi-Fi and minimizes battery usage.</div>
          </div>
        </div>
      </div>
    </section>

    <!-- PAGE: TELEMETRY -->
    <section id="page-telemetry" class="page">
      <div class="grid-4" style="margin-bottom:16px;">
        <div class="stat-card">
          <div class="stat-label">Battery</div>
          <div class="stat-value" id="t-bat">--%</div>
          <div class="stat-sub" id="t-bat-sub">--</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Battery Temp</div>
          <div class="stat-value" id="t-temp">--&deg;C</div>
          <div class="stat-sub" id="t-volt">-- mV</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Memory Used</div>
          <div class="stat-value" id="t-ram">-- MB</div>
          <div class="stat-sub" id="t-ram-sub">Total: -- MB</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Free Storage</div>
          <div class="stat-value" id="t-stor">-- GB</div>
          <div class="stat-sub" id="t-stor-sub">Total: -- GB</div>
        </div>
      </div>

      <div class="card">
        <div class="card-header"><span class="card-title">System Details</span></div>
        <div class="table-wrap">
          <table>
            <tbody>
              <tr><td>Device Model</td><td id="d-model" class="mono">--</td></tr>
              <tr><td>Android Version</td><td id="d-os" class="mono">--</td></tr>
              <tr><td>Local IP &amp; Port</td><td id="d-ip" class="mono">--</td></tr>
              <tr><td>Wi-Fi Network</td><td id="d-wifi" class="mono">--</td></tr>
              <tr><td>Screen State</td><td id="d-screen" class="mono">--</td></tr>
              <tr><td>Server Uptime</td><td id="d-uptime" class="mono">--</td></tr>
              <tr><td>Total API Requests</td><td id="d-reqs" class="mono">--</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: FILES & STORAGE -->
    <section id="page-files" class="page">
      <div id="perm-warn-all_files" class="perm-alert" style="display:none;">
        <span>⚠️ <b>All Files Access</b> is not granted on the phone yet. Enable it in the PhoneDeck app to browse all folders.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(function(){loadFiles(currentDir);})">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">File Explorer &amp; Upload</span>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-sm" onclick="goUpDir()">⬆ Up</button>
            <button class="btn btn-sm" onclick="createFolder()">+ Folder</button>
            <button class="btn btn-sm" onclick="loadFiles(currentDir)">↻ Refresh</button>
          </div>
        </div>

        <div style="display:flex; gap:8px; margin-bottom:14px;">
          <input type="text" id="file-path-box" class="input mono" placeholder="/storage/emulated/0" onkeydown="if(event.key==='Enter') loadFiles(this.value)">
          <button class="btn" onclick="loadFiles(document.getElementById('file-path-box').value)">Go</button>
        </div>

        <div id="file-dropzone" class="dropzone">
          <div style="font-weight:600; color:var(--text-primary); margin-bottom:4px;">Drag &amp; drop files here to upload to current folder</div>
          <div style="font-size:12px; color:var(--text-muted); margin-bottom:10px;">Supports large &amp; multiple files with real-time streaming</div>
          <div style="display:flex; gap:8px; justify-content:center; align-items:center; flex-wrap:wrap;">
            <input type="file" id="upload-file-input" multiple style="font-size:12.5px;">
            <button class="btn btn-primary btn-sm" id="upload-btn" onclick="uploadSelectedFiles()">⬆ Upload Selected</button>
          </div>
          <div id="upload-progress-wrap" style="display:none; margin-top:12px; text-align:left;">
            <div style="display:flex; justify-content:space-between; font-size:12px;">
              <span id="upload-status-lbl">Uploading...</span>
              <span class="mono" id="upload-pct-lbl">0%</span>
            </div>
            <div class="progress-track"><div class="progress-fill" id="upload-progress-bar"></div></div>
          </div>
        </div>

        <div class="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Size</th><th>Actions</th></tr></thead>
            <tbody id="files-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: MEDIA GALLERY -->
    <section id="page-media" class="page">
      <div id="perm-warn-media" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Photos, Video &amp; Audio</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(function(){loadMedia('images');})">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Device Media</span>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-sm btn-primary" id="m-btn-images" onclick="loadMedia('images')">Photos</button>
            <button class="btn btn-sm" id="m-btn-videos" onclick="loadMedia('videos')">Videos</button>
            <button class="btn btn-sm" id="m-btn-audio" onclick="loadMedia('audio')">Audio</button>
          </div>
        </div>
        <div id="media-grid" class="media-grid"></div>
      </div>
    </section>

    <!-- PAGE: CONTACTS -->
    <section id="page-contacts" class="page">
      <div id="perm-warn-contacts" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Contacts</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadContacts)">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header"><span class="card-title">Add New Contact</span></div>
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap:8px;">
          <input type="text" id="c-name" class="input" placeholder="Full Name">
          <input type="text" id="c-phone" class="input" placeholder="Phone Number">
          <input type="text" id="c-email" class="input" placeholder="Email (optional)">
          <button class="btn btn-primary" onclick="addContact()">Save Contact</button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Phonebook</span>
          <button class="btn btn-sm" onclick="loadContacts()">↻ Refresh</button>
        </div>
        <input type="text" id="contact-search" class="input" placeholder="Search contacts..." oninput="filterContacts()" style="margin-bottom:12px;">
        <div class="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Phone</th><th>Email</th></tr></thead>
            <tbody id="contacts-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: SMS -->
    <section id="page-sms" class="page">
      <div id="perm-warn-sms" class="perm-alert" style="display:none;">
        <span>⚠️ <b>SMS &amp; Messages</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadSms)">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header"><span class="card-title">Send SMS</span></div>
        <div style="display:grid; grid-template-columns: 1fr 2fr auto; gap:8px;">
          <input type="text" id="sms-to" class="input" placeholder="Phone number">
          <input type="text" id="sms-body" class="input" placeholder="Message text...">
          <button class="btn btn-primary" onclick="sendSms()">Send SMS</button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">SMS History</span>
          <button class="btn btn-sm" onclick="loadSms()">↻ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Type</th><th>Number</th><th>Message</th><th>Time</th></tr></thead>
            <tbody id="sms-tbody"><tr><td colspan="4">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: CALLS & PHONE -->
    <section id="page-calls" class="page">
      <div id="perm-warn-calls_phone" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Call Logs &amp; Phone</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadCalls)">Recheck</button>
      </div>

      <div class="grid-4" style="margin-bottom:16px;">
        <div class="stat-card"><div class="stat-label">Carrier</div><div class="stat-value" id="p-op" style="font-size:16px;">--</div></div>
        <div class="stat-card"><div class="stat-label">Network</div><div class="stat-value" id="p-net" style="font-size:16px;">--</div></div>
        <div class="stat-card"><div class="stat-label">SIM State</div><div class="stat-value" id="p-sim" style="font-size:16px;">--</div></div>
        <div class="stat-card">
          <div class="stat-label">Quick Dial</div>
          <div style="display:flex; gap:6px; margin-top:4px;">
            <input type="text" id="dial-num" class="input" placeholder="Number" style="padding:4px 8px;">
            <button class="btn btn-primary btn-sm" onclick="dialNumber()">Call</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Recent Call Logs</span>
          <button class="btn btn-sm" onclick="loadCalls()">↻ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Type</th><th>Name / Number</th><th>Duration</th><th>Date</th></tr></thead>
            <tbody id="calls-tbody"><tr><td colspan="4">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: CALENDAR -->
    <section id="page-calendar" class="page">
      <div id="perm-warn-calendar" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Calendar Events</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadCalendar)">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header"><span class="card-title">Create Calendar Event (Next Hour)</span></div>
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap:8px;">
          <input type="text" id="cal-title" class="input" placeholder="Event Title">
          <input type="text" id="cal-loc" class="input" placeholder="Location (optional)">
          <input type="text" id="cal-desc" class="input" placeholder="Description">
          <button class="btn btn-primary" onclick="addCalendarEvent()">Add Event</button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Calendar Events</span>
          <button class="btn btn-sm" onclick="loadCalendar()">↻ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Title</th><th>Start</th><th>Location</th></tr></thead>
            <tbody id="cal-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: APPS & REMOTE INSTALLER -->
    <section id="page-apps" class="page">
      <div class="card">
        <div class="card-header">
          <div>
            <span class="card-title">Remote APK Installer (Hands-Free)</span>
          </div>
          <div style="display:flex; gap:6px; flex-wrap:wrap;">
            <span class="tag tag-amber" id="apk-perm-tag">Install Source: --</span>
            <span class="tag tag-amber" id="acc-perm-tag">Hands-Free Assistant: --</span>
          </div>
        </div>

        <div id="apk-dropzone" class="dropzone" style="margin-bottom:12px;">
          <div style="font-weight:600; margin-bottom:4px;">Drop an .apk file here to install onto phone remotely</div>
          <div style="font-size:12px; color:var(--text-muted); margin-bottom:10px;">Uses silent shell if available, or PackageInstaller + Hands-Free Assistant</div>
          <div style="display:flex; gap:8px; justify-content:center; align-items:center; flex-wrap:wrap;">
            <input type="file" id="apk-file-input" accept=".apk" style="font-size:12.5px;">
            <button class="btn btn-primary btn-sm" id="apk-upload-btn" onclick="uploadAndInstallApk()">📦 Upload &amp; Install APK</button>
          </div>
          <div id="apk-progress-wrap" style="display:none; margin-top:10px; text-align:left;">
            <div style="display:flex; justify-content:space-between; font-size:12px;">
              <span id="apk-upload-lbl">Uploading APK...</span>
              <span class="mono" id="apk-pct-lbl">0%</span>
            </div>
            <div class="progress-track"><div class="progress-fill" id="apk-progress-bar"></div></div>
          </div>
        </div>

        <div style="display:flex; gap:8px; margin-bottom:8px;">
          <input type="text" id="apk-path-input" class="input mono" placeholder="Or enter APK path on phone (e.g. /storage/emulated/0/Download/app.apk)">
          <button class="btn" onclick="installApkFromPath()">Install Path</button>
        </div>
        <div id="app-op-banner" style="font-size:12.5px; color:var(--text-secondary); padding:6px 10px; background:var(--bg-subtle); border-radius:var(--radius-sm); display:none;"></div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Installed Applications</span>
          <div style="display:flex; gap:6px; align-items:center;">
            <span class="chip active" id="app-tab-user" onclick="setAppTab('user')">User Apps</span>
            <span class="chip" id="app-tab-all" onclick="setAppTab('all')">All</span>
            <span class="chip" id="app-tab-sys" onclick="setAppTab('sys')">System</span>
            <button class="btn btn-sm" onclick="loadApps()">↻ Reload</button>
          </div>
        </div>
        <input type="text" id="app-filter" class="input" placeholder="Search apps or package ID..." oninput="renderFilteredApps()" style="margin-bottom:12px;">
        <div id="apps-grid" style="display:grid; grid-template-columns: repeat(auto-fill, minmax(270px, 1fr)); gap:10px;"></div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">24h App Usage Stats</span>
          <button class="btn btn-sm" onclick="loadUsage()">↻ Refresh</button>
        </div>
        <div id="perm-warn-usage_stats" class="perm-alert" style="display:none;">
          <span>⚠️ <b>App Usage Access</b> is not enabled yet. Enable it in PhoneDeck app.</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>App</th><th>Package</th><th>Time Used</th></tr></thead>
            <tbody id="usage-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: LOCATION -->
    <section id="page-location" class="page">
      <div id="perm-warn-location" class="perm-alert" style="display:none;">
        <span>⚠️ <b>GPS Location</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadLocation)">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Live GPS Coordinates</span>
          <button class="btn btn-sm" onclick="loadLocation()">↻ Refresh</button>
        </div>
        <div class="grid-4" style="margin-bottom:14px;">
          <div class="stat-card"><div class="stat-label">Latitude</div><div class="stat-value mono" id="loc-lat" style="font-size:17px;">--</div></div>
          <div class="stat-card"><div class="stat-label">Longitude</div><div class="stat-value mono" id="loc-lng" style="font-size:17px;">--</div></div>
          <div class="stat-card"><div class="stat-label">Accuracy</div><div class="stat-value" id="loc-acc" style="font-size:17px;">-- m</div></div>
          <div class="stat-card"><div class="stat-label">Provider</div><div class="stat-value" id="loc-prov" style="font-size:17px;">--</div></div>
        </div>
        <a id="loc-map-link" href="#" target="_blank" class="btn btn-primary" style="display:none;">Open Coordinates in Maps</a>
      </div>
    </section>

    <!-- PAGE: NOTIFICATIONS -->
    <section id="page-notifications" class="page">
      <div id="perm-warn-notification_listener" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Notification Listener</b> is not enabled yet. Enable it in PhoneDeck app to intercept incoming notifications.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadNotifications)">Recheck</button>
      </div>

      <div class="card">
        <div class="card-header"><span class="card-title">Send Notification to Phone</span></div>
        <div style="display:grid; grid-template-columns: 1fr 2fr auto; gap:8px;">
          <input type="text" id="n-title" class="input" placeholder="Title">
          <input type="text" id="n-msg" class="input" placeholder="Message">
          <button class="btn btn-primary" onclick="pushNotif()">Send</button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Notification Stream</span>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-sm" onclick="loadNotifications()">↻ Refresh</button>
            <button class="btn btn-sm btn-danger" onclick="clearNotifs()">Clear</button>
          </div>
        </div>
        <div id="notif-list"></div>
      </div>
    </section>

    <!-- PAGE: BLUETOOTH & NETWORK -->
    <section id="page-bluetooth" class="page">
      <div id="perm-warn-bluetooth" class="perm-alert" style="display:none;">
        <span>⚠️ <b>Bluetooth &amp; Nearby</b> permission is not granted yet. Enable it in PhoneDeck app.</span>
        <button class="btn btn-sm" onclick="fetchPermissions().then(loadBluetooth)">Recheck</button>
      </div>

      <div class="grid-4" style="margin-bottom:16px;">
        <div class="stat-card"><div class="stat-label">Bluetooth Status</div><div class="stat-value" id="bt-state" style="font-size:17px;">--</div></div>
        <div class="stat-card"><div class="stat-label">Adapter Name</div><div class="stat-value" id="bt-name" style="font-size:17px;">--</div></div>
        <div class="stat-card"><div class="stat-label">Wi-Fi SSID</div><div class="stat-value" id="net-ssid" style="font-size:17px;">--</div></div>
        <div class="stat-card"><div class="stat-label">Local IP</div><div class="stat-value mono" id="net-ip" style="font-size:17px;">--</div></div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-title">Paired Bluetooth Devices</span>
          <button class="btn btn-sm" onclick="loadBluetooth()">↻ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Device Name</th><th>MAC Address</th><th>Status</th></tr></thead>
            <tbody id="bt-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: TERMINAL & API -->
    <section id="page-terminal" class="page">
      <div class="card">
        <div class="card-header">
          <span class="card-title">Remote Shell Terminal</span>
          <button class="btn btn-sm" onclick="clearTerm()">Clear</button>
        </div>
        <div class="chip-row" style="margin-bottom:10px; margin-top:0;">
          <span class="chip" onclick="runTerm('dumpsys battery')">dumpsys battery</span>
          <span class="chip" onclick="runTerm('uptime')">uptime</span>
          <span class="chip" onclick="runTerm('df -h')">df -h</span>
          <span class="chip" onclick="runTerm('pm list packages -3')">3rd-party packages</span>
          <span class="chip" onclick="runTerm('ip addr show')">ip addr</span>
        </div>
        <div style="display:flex; gap:8px; margin-bottom:10px;">
          <input type="text" id="term-input" class="input mono" placeholder="Enter shell command..." onkeydown="if(event.key==='Enter') runTermInput()">
          <button class="btn btn-primary" onclick="runTermInput()">Run</button>
        </div>
        <div id="term-out" class="terminal-box">$ PhoneDeck Shell Ready.
</div>
      </div>
    </section>

    <!-- PAGE: PERMISSIONS MATRIX -->
    <section id="page-permissions" class="page">
      <div class="card">
        <div class="card-header">
          <span class="card-title">Android Permissions Matrix</span>
          <button class="btn btn-sm" onclick="fetchPermissions()">↻ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead><tr><th>Permission</th><th>Web Page</th><th>Status</th></tr></thead>
            <tbody id="perm-matrix-tbody"><tr><td colspan="3">Loading...</td></tr></tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- PAGE: SETTINGS & SECURITY -->
    <section id="page-settings" class="page">
      <div id="default-pwd-warn" class="perm-alert" style="display:none;">
        <span>⚠️ You are currently using the default access password. Change it below to protect your phone on local networks.</span>
      </div>

      <div class="grid-2">
        <div class="card">
          <div class="card-header">
            <span class="card-title">Change Access Password</span>
            <span class="tag tag-green" id="auth-status-tag">Protected</span>
          </div>
          <form id="change-pwd-form" onsubmit="changePassword(event)">
            <div style="margin-bottom:10px;">
              <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Current Password</label>
              <input type="password" id="set-cur-pwd" class="input" placeholder="Enter current password" autocomplete="current-password" required>
            </div>
            <div style="margin-bottom:10px;">
              <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">New Password</label>
              <input type="password" id="set-new-pwd" class="input" placeholder="Enter new password" autocomplete="new-password" required>
            </div>
            <div style="margin-bottom:12px;">
              <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Confirm New Password</label>
              <input type="password" id="set-conf-pwd" class="input" placeholder="Re-enter new password" autocomplete="new-password" required>
            </div>
            <div id="pwd-change-msg" style="display:none; font-size:12.5px; padding:8px 10px; border-radius:6px; margin-bottom:12px;"></div>
            <button type="submit" class="btn btn-primary" id="save-pwd-btn">Update Password</button>
          </form>
        </div>

        <div class="card">
          <div class="card-header">
            <span class="card-title">Browser Session &amp; Cookie</span>
            <span class="tag tag-blue">Cookie Saved</span>
          </div>
          <p style="font-size:13px; color:var(--text-secondary); margin-bottom:14px;">
            Your browser stores a signed HTTP-only session cookie so you stay signed in automatically across visits and phone restarts.
          </p>
          <p style="font-size:12.5px; color:var(--text-muted); margin-bottom:16px;">
            Updating your password automatically keeps this browser signed in while invalidating sessions on all other browsers.
          </p>
          <button class="btn btn-danger" onclick="logoutSession()">🔒 Lock &amp; Sign Out This Browser</button>
        </div>
      </div>
    </section>

    <!-- PAGE: MCP AGENT & PROTOCOL -->
    <section id="page-mcp" class="page">
      <div class="card" style="margin-bottom:16px;">
        <div class="card-header">
          <div>
            <span class="card-title">Model Context Protocol (MCP) Server</span>
            <span class="tag tag-green" id="mcp-status-pill" style="margin-left:8px;">Active</span>
            <span class="tag tag-blue" id="mcp-tools-pill">-- Tools</span>
          </div>
          <div style="display:flex; gap:8px; align-items:center;">
            <label style="display:flex; align-items:center; gap:6px; font-size:13px; font-weight:600; cursor:pointer;">
              <input type="checkbox" id="mcp-master-toggle" onchange="toggleMcpMaster(this.checked)" style="width:16px; height:16px; accent-color:var(--accent);">
              <span>Enable MCP Server</span>
            </label>
            <button class="btn btn-sm" onclick="loadMcpConfig()">↻ Refresh</button>
          </div>
        </div>
        <p style="font-size:13px; color:var(--text-secondary); margin-bottom:14px;">
          Connect your local AI agent (Claude Desktop, Cursor, Gemini CLI) or remote cloud agent (via Tunnel / Tailscale / Cloudflare) to control and automate your phone via standard Model Context Protocol (MCP) transports.
        </p>

        <!-- Bearer Token Box -->
        <div style="background:var(--bg-page); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:12px 14px; margin-bottom:14px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; flex-wrap:wrap; gap:8px;">
            <span style="font-size:12px; font-weight:600; color:var(--text-secondary);">Bearer API Token (Keep secret)</span>
            <div style="display:flex; gap:6px;">
              <button class="btn btn-sm" id="btn-toggle-token" onclick="toggleMcpTokenVisibility()">Show Token</button>
              <button class="btn btn-sm btn-primary" onclick="copyMcpToken()">Copy Token</button>
              <button class="btn btn-sm btn-danger" onclick="regenerateMcpToken()">Regenerate</button>
            </div>
          </div>
          <div class="mono" id="mcp-token-box" style="font-size:13px; font-weight:600; color:var(--accent); word-break:break-all;">••••••••••••••••••••</div>
        </div>

        <!-- Optional Public / Cloud Tunnel URL -->
        <div style="display:grid; grid-template-columns: 1fr auto; gap:8px;">
          <div>
            <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Cloud / Public Tunnel URL (Optional for remote cloud agents)</label>
            <input type="text" id="mcp-tunnel-input" class="input mono" placeholder="e.g. https://phone.yourdomain.com or https://abc.ngrok-free.app" onchange="saveMcpTunnelUrl()">
          </div>
          <div style="align-self:end;">
            <button class="btn" onclick="saveMcpTunnelUrl()">Save URL</button>
          </div>
        </div>
      </div>

      <!-- Client Configuration Tabs -->
      <div class="card" style="margin-bottom:16px;">
        <div class="card-header">
          <span class="card-title">Agent Client Configuration</span>
          <div style="display:flex; gap:6px; flex-wrap:wrap;">
            <span class="chip active" id="tab-cfg-http" onclick="setCfgTab('http')">Streamable HTTP</span>
            <span class="chip" id="tab-cfg-sse" onclick="setCfgTab('sse')">Legacy SSE</span>
            <span class="chip" id="tab-cfg-claude" onclick="setCfgTab('claude')">Cursor / Claude Desktop</span>
            <span class="chip" id="tab-cfg-curl" onclick="setCfgTab('curl')">cURL Test</span>
          </div>
        </div>
        <div style="position:relative;">
          <pre class="mono" id="mcp-config-snippet" style="background:#0f172a; color:#f8fafc; border-radius:var(--radius-md); padding:14px; font-size:12px; overflow-x:auto; min-height:110px;"></pre>
          <button class="btn btn-sm" onclick="copyConfigSnippet()" style="position:absolute; top:8px; right:8px; background:rgba(255,255,255,0.15); color:#fff; border:none; padding:4px 8px;">Copy</button>
        </div>
      </div>

      <!-- Per-Category Permission Toggles -->
      <div class="card" style="margin-bottom:16px;">
        <div class="card-header">
          <div>
            <span class="card-title">Tool Category Permissions</span>
            <span class="card-sub">Enable or disable phone capability groups for MCP agents</span>
          </div>
        </div>
        <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); gap:12px;" id="mcp-category-grid">
          <!-- Populated dynamically -->
        </div>
      </div>

      <!-- Built-in MCP Inspector & Tester -->
      <div class="card">
        <div class="card-header">
          <div>
            <span class="card-title">MCP Inspector &amp; Test Console</span>
            <span class="card-sub">Test tools directly or inspect active JSON-RPC schemas</span>
          </div>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-sm btn-primary" onclick="testMcpToolsList()">Inspect `tools/list`</button>
            <button class="btn btn-sm" onclick="testMcpPing()">Ping Server</button>
          </div>
        </div>
        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px; margin-bottom:12px;">
          <div>
            <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Test Calling Tool</label>
            <select id="mcp-test-select" class="input" onchange="onSelectTestTool(this.value)">
              <option value="">Select an active tool to test...</option>
            </select>
          </div>
          <div>
            <label style="display:block; font-size:12px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Tool Arguments (JSON)</label>
            <input type="text" id="mcp-test-args" class="input mono" placeholder="{}">
          </div>
        </div>
        <div style="display:flex; justify-content:flex-end; margin-bottom:10px;">
          <button class="btn btn-primary btn-sm" onclick="runMcpToolTest()">▶ Execute Tool</button>
        </div>
        <pre class="mono" id="mcp-test-result" style="background:#0f172a; color:#e2e8f0; border-radius:var(--radius-md); padding:12px; font-size:12px; min-height:80px; max-height:260px; overflow-y:auto;">// Click "Inspect tools/list" or execute a tool to see MCP JSON-RPC output...</pre>
      </div>
    </section>

  </main>
</div>

<div id="toast-wrap"></div>

<script>
var permMap = {};
var currentDir = '';
var parentDir = '';
var currentPage = 'controls';
var contactsCache = [];
var appsCache = [];
var appTab = 'user';
var inFlightStatus = false;
var inFlightPerms = false;
var castActive = false;

function setElText(id, val) {
  var el = document.getElementById(id);
  if (el) el.textContent = val;
}

function toggleSidebar() {
  var sb = document.getElementById('sidebar');
  if (sb) sb.classList.toggle('open');
}

function openPage(id, title) {
  currentPage = id;
  var pages = document.querySelectorAll('.page');
  for (var i = 0; i < pages.length; i++) pages[i].classList.remove('active');
  var navs = document.querySelectorAll('.nav-item');
  for (var j = 0; j < navs.length; j++) navs[j].classList.remove('active');

  var target = document.getElementById('page-' + id);
  if (target) target.classList.add('active');
  setElText('page-title', title);

  for (var k = 0; k < navs.length; k++) {
    var oc = navs[k].getAttribute('onclick') || '';
    if (oc.indexOf("'" + id + "'") !== -1) {
      navs[k].classList.add('active');
      break;
    }
  }
  var sb = document.getElementById('sidebar');
  if (sb) sb.classList.remove('open');

  // Manage MJPEG stream lifecycle so it only streams when screen page is open
  var img = document.getElementById('cast-img');
  if (id !== 'screen' && img) {
    img.src = '';
  } else if (id === 'screen') {
    refreshCastStatus();
  }
}

function toast(msg) {
  var wrap = document.getElementById('toast-wrap');
  if (!wrap) return;
  var el = document.createElement('div');
  el.className = 'toast';
  el.textContent = msg;
  wrap.appendChild(el);
  setTimeout(function() { if (el.parentNode) el.remove(); }, 3200);
}

function esc(s) {
  if (s === null || s === undefined) return '';
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

function fmtBytes(b) {
  if (!b) return '0 B';
  var u = ['B', 'KB', 'MB', 'GB'];
  var i = Math.floor(Math.log(b) / Math.log(1024));
  if (i >= u.length) i = u.length - 1;
  return (b / Math.pow(1024, i)).toFixed(1) + ' ' + u[i];
}

function fetchPermissions() {
  if (inFlightPerms) return Promise.resolve();
  inFlightPerms = true;
  return fetch('/api/permissions')
    .then(function(r) {
      if (r.status === 401) { window.location.replace('/'); throw new Error('unauthorized'); }
      return r.json();
    })
    .then(function(data) {
      permMap = data.map || {};
      var list = data.permissions || [];
      var tbody = document.getElementById('perm-matrix-tbody');
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var p = list[i];
        var badge = document.getElementById('badge-' + p.id);
        if (badge) {
          badge.className = 'perm-badge ' + (p.isGranted ? 'ok' : 'missing');
        }
        var warn = document.getElementById('perm-warn-' + p.id);
        if (warn) {
          warn.style.display = p.isGranted ? 'none' : 'flex';
        }
        html += '<tr>' +
          '<td style="font-weight:500;">' + esc(p.title) + '</td>' +
          '<td class="mono">' + esc(p.webPageId) + '</td>' +
          '<td><span class="tag ' + (p.isGranted ? 'tag-green' : 'tag-rose') + '">' + (p.isGranted ? 'Granted' : 'Not Granted') + '</span></td>' +
        '</tr>';
      }
      if (tbody) tbody.innerHTML = html;
    })
    .catch(function() {})
    .finally(function() { inFlightPerms = false; });
}

function fetchStatus() {
  if (inFlightStatus) return;
  inFlightStatus = true;
  var t0 = performance.now();
  fetch('/api/status')
    .then(function(r) {
      if (r.status === 401) { window.location.replace('/'); throw new Error('unauthorized'); }
      return r.json();
    })
    .then(function(s) {
      var ping = Math.round(performance.now() - t0);
      setElText('ping-label', ping + 'ms');
      var dot = document.getElementById('conn-dot');
      if (dot) dot.className = 'status-dot';
      var cp = document.getElementById('top-conn-pill');
      if (cp) {
        cp.className = 'pill live';
        cp.textContent = '● 24/7 Online';
      }
      setElText('top-bat', s.batteryLevel + '%' + (s.isCharging ? ' ⚡' : ''));
      setElText('top-wakelock', s.isWakeLockActive ? '⚡ Awake' : 'Sleep');
      setElText('side-device-name', s.manufacturer + ' ' + s.model);
      setElText('side-android-ver', 'Android ' + s.androidVersion + ' • ' + s.localIp);

      var rm = document.getElementById('rng-media');
      if (rm && !rm.matches(':active')) {
        rm.value = s.mediaVolumePercent;
        setElText('lbl-media', s.mediaVolumePercent + '%');
      }
      var rr = document.getElementById('rng-ring');
      if (rr && !rr.matches(':active')) {
        rr.value = s.ringVolumePercent;
        setElText('lbl-ring', s.ringVolumePercent + '%');
      }
      var ra = document.getElementById('rng-alarm');
      if (ra && !ra.matches(':active')) {
        ra.value = s.alarmVolumePercent;
        setElText('lbl-alarm', s.alarmVolumePercent + '%');
      }

      setElText('t-bat', s.batteryLevel + '%');
      setElText('t-bat-sub', s.isCharging ? s.pluggedType : 'Discharging');
      setElText('t-temp', Number(s.batteryTempC || 0).toFixed(1) + '\u00B0C');
      setElText('t-volt', s.batteryVoltageMv + ' mV');
      var usedRam = (s.totalRamMb || 0) - (s.availRamMb || 0);
      setElText('t-ram', usedRam + ' MB');
      setElText('t-ram-sub', 'Total: ' + s.totalRamMb + ' MB');
      setElText('t-stor', s.freeStorageGb + ' GB');
      setElText('t-stor-sub', 'Total: ' + s.totalStorageGb + ' GB');

      setElText('d-model', s.manufacturer + ' ' + s.model);
      setElText('d-os', 'Android ' + s.androidVersion + ' (API ' + s.sdkInt + ')');
      setElText('d-ip', s.localIp + ':' + s.serverPort);
      setElText('d-wifi', s.wifiSsid);
      setElText('d-screen', s.isScreenOn ? 'ON' : 'OFF');
      setElText('d-uptime', s.uptimeSeconds + 's');
      setElText('d-reqs', s.totalRequests);
      setElText('net-ssid', s.wifiSsid);
      setElText('net-ip', s.localIp);
    })
    .catch(function() {
      var dot = document.getElementById('conn-dot');
      if (dot) dot.className = 'status-dot offline';
      var cp = document.getElementById('top-conn-pill');
      if (cp) {
        cp.className = 'pill warn';
        cp.textContent = '● Reconnecting...';
      }
    })
    .finally(function() { inFlightStatus = false; });
}

/* LIVE SCREEN CAST */
function refreshCastStatus() {
  fetch('/api/screen/status')
    .then(function(r) { return r.json(); })
    .then(function(st) {
      castActive = !!st.isCasting;
      setElText('cast-frames', st.frameCount || 0);
      var tag = document.getElementById('cast-res-tag');
      var img = document.getElementById('cast-img');
      var ph = document.getElementById('cast-placeholder');
      if (st.isCasting) {
        if (tag) {
          tag.className = 'tag tag-green';
          tag.textContent = 'LIVE ' + (st.width || '') + 'x' + (st.height || '');
        }
        if (ph) ph.style.display = 'none';
        if (img) {
          img.style.display = 'block';
          if (currentPage === 'screen' && (!img.src || img.src.indexOf('/api/screen/stream') === -1)) {
            img.src = '/api/screen/stream?t=' + Date.now();
          }
        }
      } else {
        if (tag) {
          tag.className = 'tag tag-amber';
          tag.textContent = 'Offline';
        }
        if (img) {
          img.style.display = 'none';
          img.src = '';
        }
        if (ph) ph.style.display = 'block';
      }
    })
    .catch(function() {});
}

function requestScreenCast() {
  fetch('/api/screen/request', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function(res) {
      toast(res.message || 'Check phone screen to confirm cast');
      var attempts = 0;
      var timer = setInterval(function() {
        attempts++;
        refreshCastStatus();
        if (castActive || attempts > 15) clearInterval(timer);
      }, 1000);
    })
    .catch(function() { toast('Failed to request cast'); });
}

function stopScreenCast() {
  var img = document.getElementById('cast-img');
  if (img) img.src = '';
  fetch('/api/screen/stop', { method: 'POST' })
    .then(function() {
      toast('Screen cast stopped');
      refreshCastStatus();
    });
}

function updateCastConfig() {
  var fps = parseInt(document.getElementById('cast-fps').value || '8', 10);
  var quality = parseInt(document.getElementById('cast-qual').value || '65', 10);
  var scale = parseInt(document.getElementById('cast-scale').value || '65', 10);
  fetch('/api/screen/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fps: fps, quality: quality, scale: scale })
  }).then(function() {
    toast('Stream settings updated');
    setTimeout(refreshCastStatus, 400);
  });
}

function toggleCastFullscreen() {
  var el = document.getElementById('cast-stage');
  if (!el) return;
  if (!document.fullscreenElement) {
    if (el.requestFullscreen) el.requestFullscreen();
  } else {
    if (document.exitFullscreen) document.exitFullscreen();
  }
}

/* CONTROLS */
function runAction(action, params) {
  return fetch('/api/action', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(Object.assign({ action: action }, params || {}))
  })
  .then(function(r) { return r.json(); })
  .then(function(res) {
    toast(res.message || 'Done');
    return res;
  })
  .catch(function() { toast('Action failed'); });
}

function setVol(stream, val) {
  setElText('lbl-' + stream, val + '%');
  runAction('set_volume', { stream: stream, percent: parseInt(val, 10) });
}

function refreshClip() {
  runAction('get_clipboard').then(function(res) {
    setElText('clip-box', (res && res.clipboard) ? res.clipboard : '(Clipboard empty)');
  });
}

function pushClip() {
  var txt = document.getElementById('clip-input').value;
  if (!txt) return;
  runAction('set_clipboard', { text: txt }).then(function() {
    setElText('clip-box', txt);
    document.getElementById('clip-input').value = '';
  });
}

function speakText() {
  var txt = document.getElementById('tts-input').value.trim();
  if (txt) runAction('speak', { text: txt });
}

function openUrl() {
  var url = document.getElementById('url-input').value.trim();
  if (url) runAction('open_url', { url: url });
}

/* FILES & STREAMING MULTI-UPLOAD */
function loadFiles(path) {
  path = path || '';
  fetch('/api/files?path=' + encodeURIComponent(path))
    .then(function(r) { return r.json(); })
    .then(function(data) {
      currentDir = data.currentPath || '';
      parentDir = data.parentPath || '';
      var box = document.getElementById('file-path-box');
      if (box) box.value = currentDir;
      var warn = document.getElementById('perm-warn-all_files');
      if (warn) warn.style.display = data.hasAllFilesAccess ? 'none' : 'flex';

      var items = data.items || [];
      var tbody = document.getElementById('files-tbody');
      if (!items.length) {
        tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">Folder is empty or inaccessible</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < items.length; i++) {
        var f = items[i];
        var encPath = encodeURIComponent(f.path);
        var nameCell = f.isDirectory
          ? '<a href="javascript:void(0)" onclick="loadFiles(decodeURIComponent(\'' + encPath + '\'))" style="color:var(--accent); font-weight:600; text-decoration:none;">📁 ' + esc(f.name) + '</a>'
          : '<span>📄 ' + esc(f.name) + '</span>';
        var acts = (!f.isDirectory ? '<a class="btn btn-sm" href="/api/download?path=' + encPath + '" download>Download</a> ' : '') +
          '<button class="btn btn-sm btn-danger" onclick="deleteFile(decodeURIComponent(\'' + encPath + '\'))">Delete</button>';
        html += '<tr><td>' + nameCell + '</td><td class="mono">' + (f.isDirectory ? 'DIR' : fmtBytes(f.sizeBytes)) + '</td><td>' + acts + '</td></tr>';
      }
      tbody.innerHTML = html;
    })
    .catch(function() { toast('Could not load directory'); });
}

function goUpDir() {
  if (parentDir) loadFiles(parentDir);
}

function deleteFile(path) {
  if (!confirm('Delete "' + path.split('/').pop() + '"?')) return;
  fetch('/api/files/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: path })
  }).then(function() {
    toast('Deleted');
    loadFiles(currentDir);
  });
}

function createFolder() {
  var name = prompt('New folder name:');
  if (!name) return;
  fetch('/api/files/mkdir', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parentPath: currentDir, name: name })
  }).then(function() {
    toast('Folder created');
    loadFiles(currentDir);
  });
}

function xhrUpload(url, file, onProgress) {
  return new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', url, true);
    var startTs = Date.now();
    xhr.upload.onprogress = function(ev) {
      if (ev.lengthComputable && onProgress) {
        var pct = Math.round((ev.loaded / ev.total) * 100);
        var elapsedSec = Math.max(0.1, (Date.now() - startTs) / 1000);
        var speed = fmtBytes(ev.loaded / elapsedSec) + '/s';
        onProgress(pct, speed);
      }
    };
    xhr.onload = function() {
      try {
        resolve(JSON.parse(xhr.responseText));
      } catch (e) {
        resolve({ success: xhr.status >= 200 && xhr.status < 300 });
      }
    };
    xhr.onerror = function() { reject(new Error('Network error')); };
    xhr.send(file);
  });
}

function uploadSelectedFiles() {
  var inp = document.getElementById('upload-file-input');
  if (!inp || !inp.files || !inp.files.length) return toast('Select one or more files first');
  uploadFileList(inp.files);
}

function uploadFileList(fileList) {
  var files = Array.prototype.slice.call(fileList);
  if (!files.length) return;
  var wrap = document.getElementById('upload-progress-wrap');
  var bar = document.getElementById('upload-progress-bar');
  var btn = document.getElementById('upload-btn');
  if (wrap) wrap.style.display = 'block';
  if (btn) btn.disabled = true;

  var idx = 0;
  function next() {
    if (idx >= files.length) {
      if (btn) btn.disabled = false;
      setElText('upload-status-lbl', 'All ' + files.length + ' file(s) uploaded!');
      setElText('upload-pct-lbl', '100%');
      if (bar) bar.style.width = '100%';
      toast('Upload complete');
      loadFiles(currentDir);
      setTimeout(function() { if (wrap) wrap.style.display = 'none'; }, 2500);
      return;
    }
    var f = files[idx];
    var labelPrefix = '(' + (idx + 1) + '/' + files.length + ') ' + f.name;
    setElText('upload-status-lbl', 'Uploading ' + labelPrefix);
    var url = '/api/upload?name=' + encodeURIComponent(f.name) + '&dir=' + encodeURIComponent(currentDir);
    xhrUpload(url, f, function(pct, speed) {
      setElText('upload-status-lbl', 'Uploading ' + labelPrefix + ' • ' + speed);
      setElText('upload-pct-lbl', pct + '%');
      if (bar) bar.style.width = pct + '%';
    }).then(function(res) {
      if (!res || !res.success) toast('Failed: ' + f.name);
      idx++;
      next();
    }).catch(function() {
      toast('Error uploading: ' + f.name);
      idx++;
      next();
    });
  }
  next();
}

/* MEDIA */
function loadMedia(type) {
  ['images', 'videos', 'audio'].forEach(function(t) {
    var b = document.getElementById('m-btn-' + t);
    if (b) b.className = 'btn btn-sm ' + (t === type ? 'btn-primary' : '');
  });
  fetch('/api/media?type=' + type)
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var items = data.items || [];
      var grid = document.getElementById('media-grid');
      if (!items.length) {
        grid.innerHTML = '<div style="color:var(--text-muted); padding:16px;">No ' + type + ' found or permission missing.</div>';
        return;
      }
      var html = '';
      for (var i = 0; i < items.length; i++) {
        var m = items[i];
        var rawUrl = '/api/media/raw?id=' + m.id + '&type=' + type;
        var preview = type === 'images'
          ? '<img src="' + rawUrl + '" class="media-thumb" loading="lazy">'
          : '<div class="media-thumb">' + (type === 'videos' ? '🎬' : '🎵') + '</div>';
        html += '<div class="media-card">' + preview +
          '<div class="media-info">' +
            '<div class="media-name">' + esc(m.name) + '</div>' +
            '<div style="display:flex; justify-content:space-between; align-items:center; margin-top:6px;">' +
              '<span style="color:var(--text-muted);">' + fmtBytes(m.sizeBytes) + '</span>' +
              '<a href="' + rawUrl + '" target="_blank" class="btn btn-sm" style="padding:2px 8px;">Open</a>' +
            '</div>' +
          '</div>' +
        '</div>';
      }
      grid.innerHTML = html;
    });
}

/* CONTACTS */
function loadContacts() {
  fetch('/api/contacts')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      contactsCache = data.items || [];
      renderContacts(contactsCache);
    });
}

function renderContacts(list) {
  var tbody = document.getElementById('contacts-tbody');
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No contacts found or permission not granted.</td></tr>';
    return;
  }
  var html = '';
  for (var i = 0; i < list.length; i++) {
    var c = list[i];
    html += '<tr><td style="font-weight:500;">' + esc(c.name) + '</td><td class="mono">' + esc(c.phone) + '</td><td class="mono">' + esc(c.email) + '</td></tr>';
  }
  tbody.innerHTML = html;
}

function filterContacts() {
  var q = document.getElementById('contact-search').value.toLowerCase();
  renderContacts(contactsCache.filter(function(c) {
    return c.name.toLowerCase().indexOf(q) !== -1 || c.phone.toLowerCase().indexOf(q) !== -1;
  }));
}

function addContact() {
  var name = document.getElementById('c-name').value.trim();
  var phone = document.getElementById('c-phone').value.trim();
  var email = document.getElementById('c-email').value.trim();
  if (!name) return toast('Enter a name');
  fetch('/api/contacts/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: name, phone: phone, email: email })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Contact saved' : 'Failed (check permission)');
    if (res.success) {
      document.getElementById('c-name').value = '';
      document.getElementById('c-phone').value = '';
      document.getElementById('c-email').value = '';
      loadContacts();
    }
  });
}

/* SMS */
function loadSms() {
  fetch('/api/sms')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var list = data.items || [];
      var tbody = document.getElementById('sms-tbody');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="4" style="color:var(--text-muted);">No SMS messages found or permission not granted.</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var m = list[i];
        html += '<tr>' +
          '<td><span class="tag ' + (m.type === 'Inbox' ? 'tag-blue' : 'tag-green') + '">' + esc(m.type) + '</span></td>' +
          '<td class="mono">' + esc(m.address) + '</td>' +
          '<td>' + esc(m.body) + '</td>' +
          '<td class="mono">' + new Date(m.date).toLocaleString() + '</td>' +
        '</tr>';
      }
      tbody.innerHTML = html;
    });
}

function sendSms() {
  var phone = document.getElementById('sms-to').value.trim();
  var message = document.getElementById('sms-body').value.trim();
  if (!phone || !message) return toast('Enter phone and message');
  fetch('/api/sms/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone: phone, message: message })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'SMS sent' : 'Failed to send SMS');
    if (res.success) {
      document.getElementById('sms-body').value = '';
      setTimeout(loadSms, 1000);
    }
  });
}

/* CALLS */
function loadCalls() {
  fetch('/api/calls')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var p = data.phone || {};
      setElText('p-op', p.operatorName || '--');
      setElText('p-net', p.networkType || '--');
      setElText('p-sim', p.simState || '--');

      var list = data.items || [];
      var tbody = document.getElementById('calls-tbody');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="4" style="color:var(--text-muted);">No call logs or permission not granted.</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var c = list[i];
        var cls = c.type === 'Missed' ? 'tag-rose' : (c.type === 'Incoming' ? 'tag-green' : 'tag-blue');
        html += '<tr>' +
          '<td><span class="tag ' + cls + '">' + esc(c.type) + '</span></td>' +
          '<td><b>' + esc(c.name || c.number) + '</b>' + (c.name ? ' <span class="mono">(' + esc(c.number) + ')</span>' : '') + '</td>' +
          '<td class="mono">' + c.durationSec + 's</td>' +
          '<td class="mono">' + new Date(c.date).toLocaleString() + '</td>' +
        '</tr>';
      }
      tbody.innerHTML = html;
    });
}

function dialNumber() {
  var num = document.getElementById('dial-num').value.trim();
  if (!num) return;
  fetch('/api/calls/dial', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ number: num })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Dialing ' + num : 'Call permission required');
  });
}

/* CALENDAR */
function loadCalendar() {
  fetch('/api/calendar')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var list = data.items || [];
      var tbody = document.getElementById('cal-tbody');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No calendar events or permission not granted.</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var e = list[i];
        html += '<tr><td style="font-weight:500;">' + esc(e.title) + '</td><td class="mono">' + new Date(e.startMs).toLocaleString() + '</td><td>' + esc(e.location || '--') + '</td></tr>';
      }
      tbody.innerHTML = html;
    });
}

function addCalendarEvent() {
  var title = document.getElementById('cal-title').value.trim();
  if (!title) return toast('Enter title');
  fetch('/api/calendar/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: title,
      location: document.getElementById('cal-loc').value.trim(),
      description: document.getElementById('cal-desc').value.trim()
    })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Event added' : 'Failed (check Calendar permission)');
    if (res.success) {
      document.getElementById('cal-title').value = '';
      loadCalendar();
    }
  });
}

/* APPS, REMOTE INSTALLER & UNINSTALLER */
function loadAppOpStatus() {
  fetch('/api/apps/status')
    .then(function(r) { return r.json(); })
    .then(function(st) {
      var apkTag = document.getElementById('apk-perm-tag');
      if (apkTag) {
        apkTag.className = 'tag ' + (st.canInstallUnknownApps ? 'tag-green' : 'tag-amber');
        apkTag.textContent = 'Install Unknown Apps: ' + (st.canInstallUnknownApps ? 'Ready' : 'Enable on Phone');
      }
      var accTag = document.getElementById('acc-perm-tag');
      if (accTag) {
        accTag.className = 'tag ' + (st.accessibilityEnabled ? 'tag-green' : 'tag-blue');
        accTag.textContent = 'Hands-Free Assistant: ' + (st.accessibilityEnabled ? 'Active' : 'Optional');
      }
      var banner = document.getElementById('app-op-banner');
      if (banner && st.message) {
        banner.style.display = 'block';
        banner.textContent = '[' + (st.state || 'info').toUpperCase() + '] ' + st.message;
      }
    })
    .catch(function() {});
}

function uploadAndInstallApk(dropFile) {
  var inp = document.getElementById('apk-file-input');
  var f = dropFile || (inp && inp.files && inp.files[0] ? inp.files[0] : null);
  if (!f) return toast('Select an .apk file first');
  if (f.name.toLowerCase().indexOf('.apk') === -1) return toast('Please choose a valid .apk file');

  var wrap = document.getElementById('apk-progress-wrap');
  var bar = document.getElementById('apk-progress-bar');
  var btn = document.getElementById('apk-upload-btn');
  if (wrap) wrap.style.display = 'block';
  if (btn) btn.disabled = true;
  setElText('apk-upload-lbl', 'Uploading ' + f.name + '...');

  xhrUpload('/api/apps/upload-install?name=' + encodeURIComponent(f.name), f, function(pct, speed) {
    setElText('apk-upload-lbl', 'Uploading ' + f.name + ' • ' + speed);
    setElText('apk-pct-lbl', pct + '%');
    if (bar) bar.style.width = pct + '%';
  }).then(function(res) {
    if (btn) btn.disabled = false;
    setElText('apk-upload-lbl', res.message || 'Done');
    toast(res.message || 'Install triggered');
    loadAppOpStatus();
    setTimeout(function() { loadApps(); loadAppOpStatus(); }, 2500);
  }).catch(function() {
    if (btn) btn.disabled = false;
    toast('APK upload failed');
  });
}

function installApkFromPath() {
  var path = document.getElementById('apk-path-input').value.trim();
  if (!path) return toast('Enter APK file path');
  fetch('/api/apps/install', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: path })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.message || 'Install started');
    loadAppOpStatus();
    setTimeout(function() { loadApps(); loadAppOpStatus(); }, 2500);
  });
}

function uninstallApp(pkg, name) {
  if (!confirm('Uninstall "' + name + '" (' + pkg + ')?')) return;
  fetch('/api/apps/uninstall', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ packageName: pkg })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.message || 'Uninstall triggered');
    loadAppOpStatus();
    setTimeout(function() { loadApps(); loadAppOpStatus(); }, 2000);
  });
}

function loadApps() {
  fetch('/api/apps')
    .then(function(r) { return r.json(); })
    .then(function(apps) {
      appsCache = apps || [];
      renderFilteredApps();
    });
}

function setAppTab(tab) {
  appTab = tab;
  ['user', 'all', 'sys'].forEach(function(t) {
    var el = document.getElementById('app-tab-' + t);
    if (el) el.className = 'chip' + (t === tab ? ' active' : '');
  });
  renderFilteredApps();
}

function renderFilteredApps() {
  var q = (document.getElementById('app-filter') ? document.getElementById('app-filter').value : '').toLowerCase();
  var filtered = appsCache.filter(function(a) {
    if (appTab === 'user' && a.isSystem) return false;
    if (appTab === 'sys' && !a.isSystem) return false;
    return !q || a.name.toLowerCase().indexOf(q) !== -1 || a.packageName.toLowerCase().indexOf(q) !== -1;
  });
  var grid = document.getElementById('apps-grid');
  if (!grid) return;
  if (!filtered.length) {
    grid.innerHTML = '<div style="color:var(--text-muted); padding:10px;">No matching apps found.</div>';
    return;
  }
  var html = '';
  for (var i = 0; i < filtered.length; i++) {
    var a = filtered[i];
    var encPkg = encodeURIComponent(a.packageName);
    var encName = encodeURIComponent(a.name);
    html += '<div style="border:1px solid var(--border-color); border-radius:var(--radius-md); padding:10px 12px; display:flex; align-items:center; justify-content:space-between; gap:8px; background:var(--bg-surface);">' +
      '<div style="overflow:hidden; min-width:0;">' +
        '<div style="font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + esc(a.name) + '</div>' +
        '<div class="mono" style="color:var(--text-muted); font-size:11px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + esc(a.packageName) + ' &bull; v' + esc(a.versionName) + '</div>' +
      '</div>' +
      '<div style="display:flex; gap:4px; flex-shrink:0;">' +
        '<button class="btn btn-primary btn-sm" onclick="runAction(\'launch_app\', {packageName: decodeURIComponent(\'' + encPkg + '\')})">Open</button>' +
        (!a.isSystem ? '<button class="btn btn-danger btn-sm" onclick="uninstallApp(decodeURIComponent(\'' + encPkg + '\'), decodeURIComponent(\'' + encName + '\'))">Uninstall</button>' : '') +
      '</div>' +
    '</div>';
  }
  grid.innerHTML = html;
}

function loadUsage() {
  fetch('/api/usage')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      var list = data.items || [];
      var tbody = document.getElementById('usage-tbody');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No usage data or Usage Access not granted.</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var u = list[i];
        html += '<tr><td style="font-weight:500;">' + esc(u.appName) + '</td><td class="mono">' + esc(u.packageName) + '</td><td class="mono">' + u.totalMinutes + ' min</td></tr>';
      }
      tbody.innerHTML = html;
    });
}

/* LOCATION */
function loadLocation() {
  fetch('/api/location')
    .then(function(r) { return r.json(); })
    .then(function(l) {
      setElText('loc-lat', l.available ? l.latitude.toFixed(6) : '--');
      setElText('loc-lng', l.available ? l.longitude.toFixed(6) : '--');
      setElText('loc-acc', l.available ? (l.accuracyMeters.toFixed(1) + ' m') : '--');
      setElText('loc-prov', l.provider || '--');
      var link = document.getElementById('loc-map-link');
      if (link) {
        if (l.available) {
          link.href = 'https://maps.google.com/?q=' + l.latitude + ',' + l.longitude;
          link.style.display = 'inline-flex';
        } else {
          link.style.display = 'none';
        }
      }
    });
}

/* NOTIFICATIONS */
function loadNotifications() {
  fetch('/api/notifications')
    .then(function(r) { return r.json(); })
    .then(function(list) {
      var box = document.getElementById('notif-list');
      if (!list.length) {
        box.innerHTML = '<div style="color:var(--text-muted); padding:12px;">No notifications captured yet.</div>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var n = list[i];
        html += '<div style="border:1px solid var(--border-color); border-left:3px solid var(--accent); border-radius:var(--radius-md); padding:10px 14px; margin-bottom:8px;">' +
          '<div style="display:flex; justify-content:space-between; font-size:11px; color:var(--text-muted);">' +
            '<span>' + esc(n.appName) + '</span><span>' + new Date(n.timestamp).toLocaleTimeString() + '</span>' +
          '</div>' +
          '<div style="font-weight:600; margin-top:2px;">' + esc(n.title) + '</div>' +
          '<div style="color:var(--text-secondary); font-size:13px;">' + esc(n.text) + '</div>' +
        '</div>';
      }
      box.innerHTML = html;
    });
}

function pushNotif() {
  var title = document.getElementById('n-title').value.trim() || 'PhoneDeck';
  var msg = document.getElementById('n-msg').value.trim() || 'Alert from Desktop';
  runAction('show_notification', { title: title, message: msg });
}

function clearNotifs() {
  runAction('clear_notifications').then(loadNotifications);
}

/* BLUETOOTH */
function loadBluetooth() {
  fetch('/api/bluetooth')
    .then(function(r) { return r.json(); })
    .then(function(bt) {
      setElText('bt-state', bt.isSupported ? (bt.isEnabled ? 'Enabled' : 'Disabled') : 'Unavailable');
      setElText('bt-name', bt.adapterName || '--');
      var list = bt.devices || [];
      var tbody = document.getElementById('bt-tbody');
      if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No paired Bluetooth devices or permission not granted.</td></tr>';
        return;
      }
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var d = list[i];
        html += '<tr><td style="font-weight:500;">' + esc(d.name) + '</td><td class="mono">' + esc(d.address) + '</td><td><span class="tag tag-green">' + esc(d.bondState) + '</span></td></tr>';
      }
      tbody.innerHTML = html;
    });
}

/* TERMINAL */
function runTermInput() {
  var cmd = document.getElementById('term-input').value.trim();
  if (cmd) runTerm(cmd);
}

function runTerm(cmd) {
  var out = document.getElementById('term-out');
  document.getElementById('term-input').value = cmd;
  out.textContent += '\n$ ' + cmd + '\n...';
  out.scrollTop = out.scrollHeight;
  fetch('/api/shell', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command: cmd })
  }).then(function(r) { return r.json(); }).then(function(res) {
    out.textContent += '\n' + (res.stdout || '') + (res.stderr ? ('\nERR: ' + res.stderr) : '') + '\n[exit ' + res.exitCode + ' in ' + res.executionTimeMs + 'ms]\n';
    out.scrollTop = out.scrollHeight;
  });
}

function clearTerm() {
  setElText('term-out', '$ PhoneDeck Shell Ready.\n');
}

function setupDropzone(elId, onFiles) {
  var dz = document.getElementById(elId);
  if (!dz) return;
  dz.addEventListener('dragover', function(e) { e.preventDefault(); dz.classList.add('dragover'); });
  dz.addEventListener('dragleave', function() { dz.classList.remove('dragover'); });
  dz.addEventListener('drop', function(e) {
    e.preventDefault();
    dz.classList.remove('dragover');
    if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length) {
      onFiles(e.dataTransfer.files);
    }
  });
}

setupDropzone('file-dropzone', function(files) { uploadFileList(files); });
setupDropzone('apk-dropzone', function(files) { if (files[0]) uploadAndInstallApk(files[0]); });

/* SETTINGS & AUTHENTICATION */
function refreshAuthInfo() {
  fetch('/api/auth/status')
    .then(function(r) { return r.json(); })
    .then(function(st) {
      if (!st.authenticated) {
        window.location.replace('/');
        return;
      }
      var warn = document.getElementById('default-pwd-warn');
      if (warn) warn.style.display = st.isDefaultPassword ? 'flex' : 'none';
      var tag = document.getElementById('auth-status-tag');
      if (tag) {
        tag.className = 'tag ' + (st.isDefaultPassword ? 'tag-amber' : 'tag-green');
        tag.textContent = st.isDefaultPassword ? 'Default Password' : 'Custom Password';
      }
    })
    .catch(function() {});
}

function changePassword(ev) {
  ev.preventDefault();
  var cur = document.getElementById('set-cur-pwd').value;
  var np = document.getElementById('set-new-pwd').value;
  var cp = document.getElementById('set-conf-pwd').value;
  var msgBox = document.getElementById('pwd-change-msg');
  var btn = document.getElementById('save-pwd-btn');

  function showMsg(ok, text) {
    if (!msgBox) return;
    msgBox.style.display = 'block';
    msgBox.style.background = ok ? 'var(--green-soft)' : 'var(--rose-soft)';
    msgBox.style.color = ok ? '#065f46' : 'var(--rose)';
    msgBox.style.border = ok ? '1px solid #a7f3d0' : '1px solid #fecdd3';
    msgBox.textContent = text;
  }

  if (!np) {
    showMsg(false, 'New password cannot be empty');
    return;
  }
  if (np !== cp) {
    showMsg(false, 'New password and confirmation do not match');
    return;
  }

  btn.disabled = true;
  fetch('/api/auth/change-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ currentPassword: cur, newPassword: np })
  })
  .then(function(r) { return r.json().then(function(d) { return { ok: r.ok, data: d }; }); })
  .then(function(res) {
    btn.disabled = false;
    if (res.ok && res.data && res.data.success) {
      showMsg(true, res.data.message || 'Password updated');
      toast('Access password updated');
      document.getElementById('set-cur-pwd').value = '';
      document.getElementById('set-new-pwd').value = '';
      document.getElementById('set-conf-pwd').value = '';
      refreshAuthInfo();
    } else {
      showMsg(false, (res.data && res.data.message) ? res.data.message : 'Failed to update password');
    }
  })
  .catch(function() {
    btn.disabled = false;
    showMsg(false, 'Request failed');
  });
}

function logoutSession() {
  fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' })
    .finally(function() { window.location.replace('/'); });
}

refreshAuthInfo();
fetchPermissions();
fetchStatus();
loadMcpConfig();
setInterval(fetchStatus, 3000);
setInterval(fetchPermissions, 5000);

/* MCP AGENT PROTOCOL HANDLERS */
var mcpConfigCache = null;
var mcpToolsCache = [];
var mcpTokenVisible = false;
var activeCfgTab = 'http';

var MCP_CAT_DEFS = [
  { id: 'device', title: 'Device & Telemetry', desc: 'Battery, RAM, storage, GPS location, bluetooth, and app usage', icon: '📱' },
  { id: 'controls', title: 'Hardware Controls', desc: 'Torch, volume, vibration, audio tones, TTS speak, and clipboard', icon: '🎛️' },
  { id: 'files', title: 'Files & Media', desc: 'Read/write files, folder creation, file deletion, and MediaStore', icon: '📁' },
  { id: 'comms', title: 'Contacts, SMS & Calls', desc: 'Address book, SMS messages, phone dialing, call logs, calendar, alerts', icon: '💬' },
  { id: 'apps', title: 'Apps & Live Screen', desc: 'List installed apps, launch, APK install, uninstall, screen snapshots', icon: '📦' },
  { id: 'shell', title: 'Shell Terminal', desc: 'Execute system commands in Android shell (High Privilege)', icon: '💻', warn: true }
];

function loadMcpConfig() {
  fetch('/api/mcp/config')
    .then(function(r) { return r.json(); })
    .then(function(cfg) {
      mcpConfigCache = cfg;
      var enabled = !!cfg.enabled;
      var toggle = document.getElementById('mcp-master-toggle');
      if (toggle) toggle.checked = enabled;

      var pill = document.getElementById('mcp-status-pill');
      if (pill) {
        pill.className = 'tag ' + (enabled ? 'tag-green' : 'tag-rose');
        pill.textContent = enabled ? 'Active' : 'Disabled';
      }

      var toolsPill = document.getElementById('mcp-tools-pill');
      if (toolsPill) {
        toolsPill.textContent = (cfg.activeToolsCount || 0) + ' Tools Active';
      }

      var sideBadge = document.getElementById('side-mcp-badge');
      if (sideBadge) {
        sideBadge.className = 'tag ' + (enabled ? 'tag-blue' : 'tag-rose');
        sideBadge.textContent = enabled ? 'MCP' : 'OFF';
      }

      var tokenBox = document.getElementById('mcp-token-box');
      if (tokenBox) {
        tokenBox.textContent = mcpTokenVisible ? (cfg.token || '') : (cfg.maskedToken || '••••••••');
      }

      var tunnelInput = document.getElementById('mcp-tunnel-input');
      if (tunnelInput && document.activeElement !== tunnelInput) {
        tunnelInput.value = cfg.tunnelUrl || '';
      }

      renderMcpCategoryGrid(cfg.categories || {});
      renderConfigSnippet();
      populateMcpTestTools();
    })
    .catch(function() {});
}

function renderMcpCategoryGrid(cats) {
  var grid = document.getElementById('mcp-category-grid');
  if (!grid) return;
  var html = '';
  for (var i = 0; i < MCP_CAT_DEFS.length; i++) {
    var c = MCP_CAT_DEFS[i];
    var isChecked = cats[c.id] !== false;
    html += '<div style="border:1px solid var(--border-color); border-radius:var(--radius-md); padding:12px; background:var(--bg-surface); display:flex; flex-direction:column; justify-content:space-between;">' +
      '<div>' +
        '<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">' +
          '<div style="font-weight:600; display:flex; align-items:center; gap:6px;"><span>' + c.icon + '</span> ' + esc(c.title) + '</div>' +
          '<input type="checkbox" ' + (isChecked ? 'checked' : '') + ' onchange="toggleMcpCategory(\'' + c.id + '\', this.checked)" style="width:16px; height:16px; accent-color:var(--accent); cursor:pointer;">' +
        '</div>' +
        '<div style="font-size:12px; color:var(--text-secondary);">' + esc(c.desc) + '</div>' +
        (c.warn ? '<div style="font-size:11px; color:var(--amber); font-weight:600; margin-top:4px;">⚠️ Gives agent command execution access</div>' : '') +
      '</div>' +
    '</div>';
  }
  grid.innerHTML = html;
}

function toggleMcpMaster(checked) {
  fetch('/api/mcp/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled: checked })
  }).then(function() {
    toast(checked ? 'MCP server enabled' : 'MCP server disabled');
    loadMcpConfig();
  });
}

function toggleMcpCategory(cat, checked) {
  var patch = { categories: {} };
  patch.categories[cat] = checked;
  fetch('/api/mcp/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(patch)
  }).then(function() {
    toast('Category updated');
    loadMcpConfig();
  });
}

function saveMcpTunnelUrl() {
  var inp = document.getElementById('mcp-tunnel-input');
  var url = inp ? inp.value.trim() : '';
  fetch('/api/mcp/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tunnelUrl: url })
  }).then(function() {
    toast('Tunnel URL saved');
    loadMcpConfig();
  });
}

function toggleMcpTokenVisibility() {
  mcpTokenVisible = !mcpTokenVisible;
  var btn = document.getElementById('btn-toggle-token');
  if (btn) btn.textContent = mcpTokenVisible ? 'Hide Token' : 'Show Token';
  var box = document.getElementById('mcp-token-box');
  if (box && mcpConfigCache) {
    box.textContent = mcpTokenVisible ? (mcpConfigCache.token || '') : (mcpConfigCache.maskedToken || '••••••••');
  }
}

function copyMcpToken() {
  if (!mcpConfigCache || !mcpConfigCache.token) return;
  navigator.clipboard.writeText(mcpConfigCache.token).then(function() {
    toast('MCP Bearer token copied to clipboard');
  }).catch(function() {
    toast('Failed to copy');
  });
}

function regenerateMcpToken() {
  if (!confirm('Regenerate MCP Bearer Token? All existing agent connections must be updated with the new token.')) return;
  fetch('/api/mcp/regenerate-token', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function(res) {
      toast('New token generated');
      loadMcpConfig();
    });
}

function setCfgTab(tab) {
  activeCfgTab = tab;
  ['http', 'sse', 'claude', 'curl'].forEach(function(t) {
    var el = document.getElementById('tab-cfg-' + t);
    if (el) el.className = 'chip' + (t === tab ? ' active' : '');
  });
  renderConfigSnippet();
}

function renderConfigSnippet() {
  var pre = document.getElementById('mcp-config-snippet');
  if (!pre) return;
  var token = (mcpConfigCache && mcpConfigCache.token) ? mcpConfigCache.token : '<YOUR_MCP_TOKEN>';
  var base = (mcpConfigCache && mcpConfigCache.tunnelUrl) ? mcpConfigCache.tunnelUrl : window.location.origin;

  var code = '';
  if (activeCfgTab === 'http') {
    code = '// Streamable HTTP Transport (Modern MCP Specification)\n' +
      '{\n' +
      '  "mcpServers": {\n' +
      '    "phonedeck": {\n' +
      '      "url": "' + base + '/mcp",\n' +
      '      "headers": {\n' +
      '        "Authorization": "Bearer ' + token + '"\n' +
      '      }\n' +
      '    }\n' +
      '  }\n' +
      '}';
  } else if (activeCfgTab === 'sse') {
    code = '// Legacy HTTP + SSE Transport\n' +
      '{\n' +
      '  "mcpServers": {\n' +
      '    "phonedeck-sse": {\n' +
      '      "url": "' + base + '/mcp/sse",\n' +
      '      "headers": {\n' +
      '        "Authorization": "Bearer ' + token + '"\n' +
      '      }\n' +
      '    }\n' +
      '  }\n' +
      '}';
  } else if (activeCfgTab === 'claude') {
    code = '// Cursor / Claude Desktop (claude_desktop_config.json)\n' +
      '{\n' +
      '  "mcpServers": {\n' +
      '    "phonedeck": {\n' +
      '      "command": "npx",\n' +
      '      "args": [\n' +
      '        "-y",\n' +
      '        "mcp-remote",\n' +
      '        "' + base + '/mcp",\n' +
      '        "--header",\n' +
      '        "Authorization: Bearer ' + token + '"\n' +
      '      ]\n' +
      '    }\n' +
      '  }\n' +
      '}';
  } else if (activeCfgTab === 'curl') {
    code = '# Test MCP Tools List via cURL:\n' +
      'curl -X POST "' + base + '/mcp" \\\n' +
      '  -H "Authorization: Bearer ' + token + '" \\\n' +
      '  -H "Content-Type: application/json" \\\n' +
      '  -d \'{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}\'';
  }
  pre.textContent = code;
}

function copyConfigSnippet() {
  var pre = document.getElementById('mcp-config-snippet');
  if (!pre) return;
  navigator.clipboard.writeText(pre.textContent).then(function() {
    toast('Config snippet copied to clipboard');
  });
}

function populateMcpTestTools() {
  fetch('/api/mcp/tools')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      mcpToolsCache = data.tools || [];
      var sel = document.getElementById('mcp-test-select');
      if (!sel) return;
      var curVal = sel.value;
      sel.innerHTML = '<option value="">Select an active tool to test...</option>';
      for (var i = 0; i < mcpToolsCache.length; i++) {
        var t = mcpToolsCache[i];
        var opt = document.createElement('option');
        opt.value = t.name;
        opt.textContent = '[' + t.category + '] ' + t.name;
        sel.appendChild(opt);
      }
      if (curVal) sel.value = curVal;
    })
    .catch(function() {});
}

function onSelectTestTool(name) {
  var tool = mcpToolsCache.find(function(t) { return t.name === name; });
  var argsInp = document.getElementById('mcp-test-args');
  if (!argsInp) return;
  if (tool && tool.inputSchema && tool.inputSchema.properties) {
    var sample = {};
    for (var k in tool.inputSchema.properties) {
      var p = tool.inputSchema.properties[k];
      sample[k] = p.type === 'integer' ? 50 : (p.type === 'string' ? '' : true);
    }
    argsInp.value = JSON.stringify(sample);
  } else {
    argsInp.value = '{}';
  }
}

function testMcpToolsList() {
  var out = document.getElementById('mcp-test-result');
  if (out) out.textContent = '// Querying tools/list...';
  fetch('/api/mcp/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ method: 'tools/list', params: {} })
  })
  .then(function(r) { return r.json(); })
  .then(function(res) {
    if (out) out.textContent = JSON.stringify(res, null, 2);
  })
  .catch(function(e) {
    if (out) out.textContent = '// Error: ' + e.message;
  });
}

function testMcpPing() {
  var out = document.getElementById('mcp-test-result');
  if (out) out.textContent = '// Sending ping...';
  fetch('/api/mcp/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ method: 'ping', params: {} })
  })
  .then(function(r) { return r.json(); })
  .then(function(res) {
    if (out) out.textContent = JSON.stringify(res, null, 2);
  })
  .catch(function(e) {
    if (out) out.textContent = '// Error: ' + e.message;
  });
}

function runMcpToolTest() {
  var sel = document.getElementById('mcp-test-select');
  var toolName = sel ? sel.value : '';
  if (!toolName) return toast('Select a tool to test');
  var argsStr = document.getElementById('mcp-test-args').value.trim() || '{}';
  var parsedArgs = {};
  try {
    parsedArgs = JSON.parse(argsStr);
  } catch (e) {
    return toast('Invalid JSON in arguments');
  }

  var out = document.getElementById('mcp-test-result');
  if (out) out.textContent = '// Executing tools/call: ' + toolName + '...';

  fetch('/api/mcp/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      method: 'tools/call',
      params: { name: toolName, arguments: parsedArgs }
    })
  })
  .then(function(r) { return r.json(); })
  .then(function(res) {
    if (out) out.textContent = JSON.stringify(res, null, 2);
    toast('Tool executed');
  })
  .catch(function(e) {
    if (out) out.textContent = '// Error: ' + e.message;
  });
}
</script>
</body>
</html>
        """.trimIndent()
    }
}
