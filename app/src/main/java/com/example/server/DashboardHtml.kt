package com.example.server

object DashboardHtml {

    fun get(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PhoneDeck &bull; Device Console</title>
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
  --sidebar-w: 244px;
  --radius-lg: 12px;
  --radius-md: 8px;
  --radius-sm: 6px;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 10px 28px rgba(15, 23, 42, 0.10);
  --font-sans: -apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", Roboto, sans-serif;
  --font-mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  background: var(--bg-page);
  color: var(--text-primary);
  font-family: var(--font-sans);
  font-size: 13.5px;
  line-height: 1.45;
  min-height: 100vh;
  font-variant-numeric: tabular-nums;
  -webkit-font-smoothing: antialiased;
}

.mono { font-family: var(--font-mono); font-size: 12px; }
svg.ic { width: 15px; height: 15px; flex-shrink: 0; display: inline-block; vertical-align: middle; }
svg.ic-sm { width: 13px; height: 13px; flex-shrink: 0; display: inline-block; vertical-align: middle; }

/* APP SHELL WITH GROUPED LEFT SIDEBAR */
.app-shell {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-w);
  background: var(--bg-surface);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  z-index: 200;
  transition: transform 0.18s ease;
}

.sidebar-brand {
  height: 54px;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 0 3px var(--green-soft);
  flex-shrink: 0;
}
.status-dot.offline {
  background: var(--rose);
  box-shadow: 0 0 0 3px var(--rose-soft);
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.sidebar-device {
  font-size: 11.5px;
  color: var(--text-secondary);
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-color);
  background: #fafbfd;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px 10px 16px;
}

.nav-group {
  margin-bottom: 14px;
}
.nav-group:last-child {
  margin-bottom: 0;
}

.nav-group-label {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  padding: 4px 10px 6px;
}

.nav-item {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  padding: 7px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 9px;
  text-align: left;
  transition: background 0.12s, color 0.12s;
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

.sidebar-footer {
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-size: 11.5px;
  color: var(--text-secondary);
  background: #fafbfd;
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  z-index: 190;
  display: none;
}
.sidebar-backdrop.open { display: block; }

/* MAIN AREA & STICKY HEADER */
.main-wrap {
  flex: 1;
  margin-left: var(--sidebar-w);
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 120;
  height: 54px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.menu-btn {
  display: none;
  border: 1px solid var(--border-color);
  background: var(--bg-surface);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  padding: 5px 8px;
  cursor: pointer;
}

.page-title-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.page-crumb {
  font-size: 11.5px;
  color: var(--text-muted);
  font-weight: 500;
}
.page-heading {
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.pill {
  background: var(--bg-subtle);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.pill.live { background: var(--green-soft); border-color: #a7f3d0; color: #065f46; }
.pill.warn { background: var(--rose-soft); border-color: #fecdd3; color: #9f1239; }

.container {
  max-width: 1220px;
  width: 100%;
  margin: 0 auto;
  padding: 18px 20px 36px;
}

.page { display: none; animation: fadeIn 0.12s ease; }
.page.active { display: block; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(2px); } to { opacity: 1; transform: translateY(0); } }

/* GRIDS & CARDS */
.grid-2 { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.grid-3 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.grid-4 { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.grid-screen { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.9fr); gap: 16px; align-items: start; }

.card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 14px;
}
.card:last-child { margin-bottom: 0; }
.card-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.card-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 7px;
}

.metric-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 13px 15px;
  box-shadow: var(--shadow-sm);
}
.metric-lbl { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-muted); }
.metric-val { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-top: 2px; letter-spacing: -0.02em; }
.metric-sub { font-size: 11.5px; color: var(--text-secondary); margin-top: 2px; }

/* BUTTONS, INPUTS, TAGS */
.btn {
  border: 1px solid var(--border-color);
  background: var(--bg-surface);
  color: var(--text-primary);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 500;
  padding: 7px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-decoration: none;
  transition: background 0.14s, border-color 0.14s, transform 0.06s;
  white-space: nowrap;
}
.btn:hover { background: var(--bg-subtle); border-color: var(--border-strong); }
.btn:active { transform: scale(0.98); }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-primary { background: var(--accent); border-color: var(--accent); color: #ffffff; }
.btn-primary:hover { background: var(--accent-hover); border-color: var(--accent-hover); }
.btn-danger { background: var(--rose-soft); border-color: #fecdd3; color: var(--rose); }
.btn-danger:hover { background: #ffe4e6; }
.btn-success { background: var(--green-soft); border-color: #a7f3d0; color: #065f46; }
.btn-sm { padding: 5px 9px; font-size: 12px; border-radius: var(--radius-sm); }

.input, select.input {
  width: 100%;
  background: var(--bg-surface);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  padding: 7px 11px;
  font-family: inherit;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.14s, box-shadow 0.14s;
}
.input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12); }

.row { display: flex; gap: 8px; align-items: center; }
.row-wrap { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

.tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.tag-green { background: var(--green-soft); color: #065f46; }
.tag-rose { background: var(--rose-soft); color: #9f1239; }
.tag-blue { background: var(--accent-soft); color: var(--accent); }
.tag-amber { background: var(--amber-soft); color: #92400e; }
.tag-gray { background: var(--bg-subtle); color: var(--text-secondary); }

.chip {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--bg-surface);
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.chip:hover { border-color: var(--border-strong); color: var(--text-primary); }
.chip.active { background: var(--accent); color: #ffffff; border-color: var(--accent); }

/* TABLES */
.table-wrap { overflow-x: auto; border: 1px solid var(--border-color); border-radius: var(--radius-md); }
table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
th {
  text-align: left;
  padding: 9px 12px;
  background: var(--bg-subtle);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border-color);
}
td { padding: 9px 12px; border-bottom: 1px solid var(--border-color); vertical-align: middle; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: #fafbfd; }

/* HARDWARE & AUDIO CONTROLS */
.action-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.media-bar { display: flex; gap: 6px; flex-wrap: wrap; }
.slider-row { margin-bottom: 11px; }
.slider-row:last-child { margin-bottom: 0; }
.slider-hd { display: flex; justify-content: space-between; font-size: 12px; font-weight: 500; margin-bottom: 4px; color: var(--text-secondary); }
input[type="range"] { width: 100%; accent-color: var(--accent); cursor: pointer; }

/* SCREEN CAST STAGE */
.cast-stage {
  background: #0f172a;
  border-radius: var(--radius-md);
  min-height: 440px;
  max-height: 620px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 8px;
}
.cast-img-wrap {
  position: relative;
  display: inline-block;
  line-height: 0;
  max-width: 100%;
  max-height: 600px;
}
.cast-img {
  max-width: 100%;
  max-height: 580px;
  border-radius: 6px;
  display: none;
  cursor: crosshair;
  user-select: none;
  -webkit-user-drag: none;
  touch-action: none;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}
.tap-ripple {
  position: absolute;
  width: 24px;
  height: 24px;
  margin: -12px 0 0 -12px;
  border-radius: 50%;
  border: 2px solid #60a5fa;
  background: rgba(37, 99, 235, 0.25);
  pointer-events: none;
  animation: rippleOut 0.4s ease-out forwards;
}
@keyframes rippleOut {
  from { transform: scale(0.5); opacity: 1; }
  to { transform: scale(1.6); opacity: 0; }
}
.nav-bar-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

/* CAMERA PREVIEW */
.cam-box {
  background: var(--bg-subtle);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-top: 10px;
}
.cam-box img {
  width: 100%;
  max-height: 300px;
  object-fit: contain;
  display: none;
}

/* THREADED SMS MASTER-DETAIL */
.sms-split {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  overflow: hidden;
  min-height: 500px;
  max-height: 640px;
}
.sms-threads {
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
  background: #fbfcfe;
}
.sms-thread-item {
  padding: 11px 13px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background 0.12s;
}
.sms-thread-item:hover { background: var(--bg-subtle); }
.sms-thread-item.active { background: var(--accent-soft); border-left: 3px solid var(--accent); padding-left: 10px; }
.sms-thread-top { display: flex; justify-content: space-between; align-items: baseline; gap: 6px; }
.sms-thread-name { font-weight: 600; font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sms-thread-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }
.sms-thread-snip { font-size: 12px; color: var(--text-secondary); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.sms-detail { display: flex; flex-direction: column; min-height: 0; height: 100%; }
.sms-detail-hd {
  padding: 11px 15px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-surface);
}
.sms-bubbles {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--bg-page);
}
.bubble {
  max-width: 78%;
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.4;
  word-break: break-word;
}
.bubble.in { align-self: flex-start; background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-primary); border-bottom-left-radius: 3px; }
.bubble.out { align-self: flex-end; background: var(--accent); color: #ffffff; border-bottom-right-radius: 3px; }
.bubble-ts { font-size: 10.5px; opacity: 0.72; margin-top: 3px; }
.sms-reply-bar {
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-surface);
  display: flex;
  gap: 8px;
}

/* DROPZONES & MEDIA */
.dropzone {
  border: 1.5px dashed var(--border-strong);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  background: var(--bg-page);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  transition: border-color 0.14s, background 0.14s;
}
.dropzone.dragover { border-color: var(--accent); background: var(--accent-soft); }
.progress-track { height: 6px; background: var(--bg-subtle); border-radius: 999px; overflow: hidden; margin-top: 6px; }
.progress-fill { height: 100%; background: var(--accent); width: 0%; transition: width 0.12s; }

.media-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 12px; }
.media-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-surface);
  cursor: pointer;
  transition: border-color 0.14s, transform 0.1s;
}
.media-card:hover { border-color: var(--accent); transform: translateY(-1px); }
.media-thumb {
  width: 100%;
  height: 124px;
  object-fit: cover;
  background: var(--bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}
.media-info { padding: 8px 10px; font-size: 12px; }
.media-name { font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* TERMINAL */
.term-out {
  background: #0f172a;
  color: #e2e8f0;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.5;
  border-radius: var(--radius-md);
  padding: 13px;
  height: 380px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

/* MODAL (FILE EDITOR & MEDIA PREVIEW) */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: none;
  align-items: center;
  justify-content: center;
  z-index: 400;
  padding: 18px;
}
.modal-backdrop.open { display: flex; }
.modal-box {
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 880px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-md);
  overflow: hidden;
}
.modal-hd {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.modal-body {
  padding: 14px 16px;
  overflow: auto;
  flex: 1;
}
.code-editor {
  width: 100%;
  min-height: 390px;
  font-family: var(--font-mono);
  font-size: 12.5px;
  line-height: 1.5;
  padding: 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: #fcfdff;
  color: var(--text-primary);
  resize: vertical;
  outline: none;
}
.code-editor:focus { border-color: var(--accent); }

/* TOAST & WARN */
.warn-bar {
  background: var(--amber-soft);
  border: 1px solid #fde68a;
  color: #92400e;
  border-radius: var(--radius-md);
  padding: 9px 12px;
  margin-bottom: 12px;
  font-size: 12.5px;
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
#toast-wrap { position: fixed; bottom: 18px; right: 18px; z-index: 500; display: flex; flex-direction: column; gap: 6px; }
.toast {
  background: var(--text-primary);
  color: #ffffff;
  padding: 9px 14px;
  border-radius: var(--radius-md);
  font-size: 12.5px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.22);
}

@media (max-width: 960px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar.open { transform: translateX(0); box-shadow: var(--shadow-md); }
  .main-wrap { margin-left: 0; }
  .menu-btn { display: inline-flex; align-items: center; justify-content: center; }
  .grid-2, .grid-3, .grid-4, .grid-screen { grid-template-columns: 1fr; }
  .sms-split { grid-template-columns: 1fr; max-height: none; }
  .sms-threads { max-height: 210px; border-right: none; border-bottom: 1px solid var(--border-color); }
}
</style>
</head>
<body>

<div class="sidebar-backdrop" id="sidebar-backdrop" onclick="toggleSidebar(false)"></div>

<div class="app-shell">
  <!-- GROUPED LEFT SIDEBAR NAVIGATION -->
  <aside class="sidebar" id="sidebar-nav">
    <div class="sidebar-brand">
      <div class="brand-left">
        <span class="status-dot" id="conn-dot"></span>
        <span class="brand-name">PhoneDeck</span>
      </div>
      <span class="pill mono" id="ping-label" style="font-size:11px;">--ms</span>
    </div>
    <div class="sidebar-device" id="top-device-name">Connecting...</div>

    <div class="sidebar-scroll">
      <div class="nav-group">
        <div class="nav-group-label">Control</div>
        <button class="nav-item active" data-page="overview" onclick="openPage('overview')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
          Dashboard
        </button>
        <button class="nav-item" data-page="screen" onclick="openPage('screen')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="6" y="2" width="12" height="20" rx="2"/><line x1="12" y1="18" x2="12" y2="18.01"/></svg>
          Live Screen &amp; Cam
        </button>
      </div>

      <div class="nav-group">
        <div class="nav-group-label">Communications</div>
        <button class="nav-item" data-page="sms" onclick="openPage('sms')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          Messages
        </button>
        <button class="nav-item" data-page="contacts" onclick="openPage('contacts')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          Contacts
        </button>
        <button class="nav-item" data-page="calls" onclick="openPage('calls')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
          Calls
        </button>
        <button class="nav-item" data-page="calendar" onclick="openPage('calendar')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          Calendar
        </button>
      </div>

      <div class="nav-group">
        <div class="nav-group-label">Storage</div>
        <button class="nav-item" data-page="files" onclick="openPage('files')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          File Explorer
        </button>
        <button class="nav-item" data-page="media" onclick="openPage('media')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          Media Gallery
        </button>
      </div>

      <div class="nav-group">
        <div class="nav-group-label">System &amp; Agent</div>
        <button class="nav-item" data-page="apps" onclick="openPage('apps')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="8" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/><rect x="13" y="13" width="8" height="8" rx="1.5"/></svg>
          Apps &amp; APK
        </button>
        <button class="nav-item" data-page="terminal" onclick="openPage('terminal')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/></svg>
          Shell Terminal
        </button>
        <button class="nav-item" data-page="notifications" onclick="openPage('notifications')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
          Notifications
        </button>
        <button class="nav-item" data-page="hardware" onclick="openPage('hardware')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
          Location &amp; Radio
        </button>
        <button class="nav-item" data-page="mcp" onclick="openPage('mcp')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          MCP Agent
        </button>
        <button class="nav-item" data-page="permissions" onclick="openPage('permissions')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          Permissions
        </button>
        <button class="nav-item" data-page="settings" onclick="openPage('settings')">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          Security
        </button>
      </div>
    </div>

    <div class="sidebar-footer">
      <span>Background Bridge</span>
      <span class="tag tag-green">Always Active</span>
    </div>
  </aside>

  <div class="main-wrap">
    <header class="topbar">
      <div class="topbar-left">
        <button class="menu-btn" onclick="toggleSidebar()" aria-label="Menu">
          <svg class="ic" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
        </button>
        <div class="page-title-bar">
          <span class="page-crumb" id="active-group-lbl">Control</span>
          <span class="page-crumb">/</span>
          <span class="page-heading" id="active-page-lbl">Dashboard</span>
        </div>
      </div>

      <div class="topbar-right">
        <span class="pill live" id="top-conn-pill">Online</span>
        <span class="pill" id="top-bat">--%</span>
        <span class="pill" id="top-wakelock">Eco Idle</span>
        <button class="btn btn-sm" onclick="syncAllNow()">Sync</button>
        <button class="btn btn-sm btn-danger" onclick="logoutSession(false)">Lock</button>
      </div>
    </header>

    <main class="container">

      <!-- 1. DASHBOARD OVERVIEW -->
      <section id="page-overview" class="page active">
        <div class="grid-4" style="margin-bottom:14px;">
          <div class="metric-card">
            <div class="metric-lbl">Battery</div>
            <div class="metric-val" id="t-bat">--%</div>
            <div class="metric-sub" id="t-bat-sub">--</div>
          </div>
          <div class="metric-card">
            <div class="metric-lbl">Thermal / Voltage</div>
            <div class="metric-val" id="t-temp">--&deg;C</div>
            <div class="metric-sub" id="t-volt">-- mV</div>
          </div>
          <div class="metric-card">
            <div class="metric-lbl">Memory Used</div>
            <div class="metric-val" id="t-ram">-- MB</div>
            <div class="metric-sub" id="t-ram-sub">Total: -- MB</div>
          </div>
          <div class="metric-card">
            <div class="metric-lbl">Storage Free</div>
            <div class="metric-val" id="t-stor">-- GB</div>
            <div class="metric-sub" id="t-stor-sub">Total: -- GB</div>
          </div>
        </div>

        <div class="grid-2">
          <div>
            <div class="card">
              <div class="card-hd"><span class="card-title">Hardware Actions</span></div>
              <div class="action-grid">
                <button class="btn" onclick="runAction('torch_toggle')">Torch Toggle</button>
                <button class="btn" onclick="runAction('wake_screen')">Wake Screen</button>
                <button class="btn" onclick="runAction('play_sound',{type:'ringtone'})">Ring Phone</button>
                <button class="btn" onclick="runAction('play_sound',{type:'beep'})">Beep Tone</button>
                <button class="btn" onclick="runAction('vibrate',{pattern:'short'})">Vibrate Short</button>
                <button class="btn" onclick="runAction('vibrate',{pattern:'pulse'})">Vibrate Pulse</button>
              </div>
            </div>

            <div class="card">
              <div class="card-hd"><span class="card-title">Media Remote</span></div>
              <div class="media-bar">
                <button class="btn" onclick="sendMediaKey('prev')">Prev</button>
                <button class="btn btn-primary" onclick="sendMediaKey('play_pause')">Play / Pause</button>
                <button class="btn" onclick="sendMediaKey('next')">Next</button>
                <button class="btn" onclick="sendMediaKey('stop')">Stop</button>
                <button class="btn" onclick="sendMediaKey('vol_down')">Vol -</button>
                <button class="btn" onclick="sendMediaKey('vol_up')">Vol +</button>
                <button class="btn" onclick="sendMediaKey('mute')">Mute</button>
              </div>
            </div>

            <div class="card">
              <div class="card-hd"><span class="card-title">Audio Volumes</span></div>
              <div class="slider-row">
                <div class="slider-hd"><span>Media</span><span class="mono" id="lbl-media">--%</span></div>
                <input type="range" id="rng-media" min="0" max="100" oninput="setVol('media', this.value)">
              </div>
              <div class="slider-row">
                <div class="slider-hd"><span>Ringtone</span><span class="mono" id="lbl-ring">--%</span></div>
                <input type="range" id="rng-ring" min="0" max="100" oninput="setVol('ring', this.value)">
              </div>
              <div class="slider-row">
                <div class="slider-hd"><span>Alarm</span><span class="mono" id="lbl-alarm">--%</span></div>
                <input type="range" id="rng-alarm" min="0" max="100" oninput="setVol('alarm', this.value)">
              </div>
            </div>
          </div>

          <div>
            <div class="card">
              <div class="card-hd">
                <span class="card-title">Two-Way Clipboard</span>
                <div class="row">
                  <button class="btn btn-sm" onclick="refreshClip()">Read Phone</button>
                  <button class="btn btn-sm" onclick="copyPhoneClipToPc()">Copy to PC</button>
                </div>
              </div>
              <div class="mono" id="clip-box" style="background:var(--bg-subtle); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:9px 11px; min-height:46px; max-height:110px; overflow:auto; margin-bottom:10px; color:var(--text-secondary);">--</div>
              <div class="row">
                <input type="text" id="clip-input" class="input" placeholder="Text to push to phone clipboard..." onkeydown="if(event.key==='Enter')pushClip()">
                <button class="btn btn-primary" onclick="pushClip()">Push</button>
              </div>
            </div>

            <div class="card">
              <div class="card-hd"><span class="card-title">Speech &amp; Browser Launch</span></div>
              <div class="row" style="margin-bottom:10px;">
                <input type="text" id="tts-input" class="input" placeholder="Text to speak via TTS..." onkeydown="if(event.key==='Enter')speakText()">
                <button class="btn btn-primary" onclick="speakText()">Speak</button>
              </div>
              <div class="row">
                <input type="url" id="url-input" class="input" placeholder="https://example.com" onkeydown="if(event.key==='Enter')openUrl()">
                <button class="btn" onclick="openUrl()">Open URL</button>
              </div>
            </div>

            <div class="card">
              <div class="card-hd"><span class="card-title">Device Profile</span></div>
              <div class="table-wrap">
                <table>
                  <tbody>
                    <tr><td style="color:var(--text-secondary); width:36%;">Model</td><td id="d-model">--</td></tr>
                    <tr><td style="color:var(--text-secondary);">Android OS</td><td id="d-os">--</td></tr>
                    <tr><td style="color:var(--text-secondary);">Local Endpoint</td><td class="mono" id="d-ip">--</td></tr>
                    <tr><td style="color:var(--text-secondary);">Wi-Fi SSID</td><td id="d-wifi">--</td></tr>
                    <tr><td style="color:var(--text-secondary);">Screen / Uptime</td><td><span id="d-screen">--</span> &middot; <span class="mono" id="d-uptime">--</span></td></tr>
                    <tr><td style="color:var(--text-secondary);">API Requests</td><td class="mono" id="d-reqs">0</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 2. LIVE SCREEN & CAMERA -->
      <section id="page-screen" class="page">
        <div class="grid-screen">
          <div class="card">
            <div class="card-hd">
              <div class="row">
                <span class="card-title">Interactive Screen Cast</span>
                <span class="tag tag-amber" id="cast-res-tag">Offline</span>
                <span class="tag tag-gray" id="cast-acc-tag">Touch: Checking</span>
              </div>
              <div class="row">
                <button class="btn btn-sm btn-primary" onclick="requestScreenCast()">Start Cast</button>
                <button class="btn btn-sm btn-danger" onclick="stopScreenCast()">Stop</button>
                <button class="btn btn-sm" onclick="toggleCastFullscreen()">Fullscreen</button>
              </div>
            </div>

            <div class="cast-stage" id="cast-stage">
              <div id="cast-placeholder" style="text-align:center; color:#94a3b8; padding:24px;">
                <div style="font-size:14px; font-weight:600; color:#e2e8f0; margin-bottom:6px;">Screen Cast Inactive</div>
                <div style="font-size:12px; margin-bottom:14px;">Start casting to mirror display and send remote taps/swipes. Auto-pauses when tab is idle to save battery.</div>
                <button class="btn btn-primary" onclick="requestScreenCast()">Request Cast Session</button>
              </div>
              <div class="cast-img-wrap" id="cast-img-wrap">
                <img id="cast-img" class="cast-img" draggable="false" alt="Live phone screen">
              </div>
            </div>

            <div class="nav-bar-grid">
              <button class="btn btn-sm" onclick="sendNav('back')">Back</button>
              <button class="btn btn-sm" onclick="sendNav('home')">Home</button>
              <button class="btn btn-sm" onclick="sendNav('recents')">Recents</button>
              <button class="btn btn-sm" onclick="sendNav('notifications')">Notifs</button>
              <button class="btn btn-sm" onclick="sendNav('quick_settings')">Quick</button>
              <button class="btn btn-sm" onclick="sendNav('power')">Power</button>
              <button class="btn btn-sm" onclick="sendNav('lock')">Lock</button>
              <a class="btn btn-sm" id="cast-snapshot-link" href="/api/screen/frame" target="_blank">Snapshot</a>
            </div>

            <div class="row" style="margin-top:10px;">
              <input type="text" id="screen-text-input" class="input" placeholder="Type text into focused phone input..." onkeydown="if(event.key==='Enter')sendScreenText()">
              <button class="btn btn-primary" onclick="sendScreenText()">Send Text</button>
            </div>
          </div>

          <div>
            <div class="card">
              <div class="card-hd">
                <span class="card-title">Stream Quality</span>
                <span class="mono" style="color:var(--text-secondary);">Frames: <b id="cast-frames">0</b></span>
              </div>
              <div class="grid-3" style="margin-bottom:10px;">
                <div>
                  <label style="font-size:11px; color:var(--text-secondary); display:block; margin-bottom:3px;">FPS (1-20)</label>
                  <input type="number" id="cast-fps" class="input" value="8" min="1" max="20">
                </div>
                <div>
                  <label style="font-size:11px; color:var(--text-secondary); display:block; margin-bottom:3px;">Quality</label>
                  <input type="number" id="cast-qual" class="input" value="65" min="20" max="95">
                </div>
                <div>
                  <label style="font-size:11px; color:var(--text-secondary); display:block; margin-bottom:3px;">Scale %</label>
                  <input type="number" id="cast-scale" class="input" value="65" min="25" max="100">
                </div>
              </div>
              <button class="btn btn-primary" style="width:100%;" onclick="updateCastConfig()">Apply Stream Settings</button>
            </div>

            <div class="card">
              <div class="card-hd">
                <span class="card-title">Remote Camera Capture</span>
                <span class="mono" id="cam-ts" style="color:var(--text-muted); font-size:11px;">Ready</span>
              </div>
              <div class="row-wrap">
                <button class="chip active" id="cam-lens-back" onclick="setCamLens('back')">Back</button>
                <button class="chip" id="cam-lens-front" onclick="setCamLens('front')">Front</button>
                <label class="row" style="font-size:12px; cursor:pointer; margin-left:4px;">
                  <input type="checkbox" id="cam-flash" style="accent-color:var(--accent);"> Flash
                </label>
                <button class="btn btn-primary btn-sm" id="cam-cap-btn" style="margin-left:auto;" onclick="captureCamera()">Capture</button>
              </div>
              <div class="cam-box">
                <span id="cam-empty" style="font-size:12px; color:var(--text-muted);">No photo captured yet</span>
                <img id="cam-preview" alt="Remote camera snapshot">
              </div>
              <div class="row" style="justify-content:flex-end; margin-top:8px;">
                <a class="btn btn-sm" id="cam-download" href="#" download="phonedeck_capture.jpg" style="display:none;">Download JPEG</a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 3. MESSAGES (THREADED SMS) -->
      <section id="page-sms" class="page">
        <div class="row-wrap" style="margin-bottom:10px;">
          <input type="search" id="sms-search" class="input" style="max-width:300px;" placeholder="Search threads or messages..." oninput="renderSmsThreads()">
          <button class="btn btn-primary btn-sm" onclick="toggleNewSmsPanel()">+ New Message</button>
          <button class="btn btn-sm" onclick="loadSms()">Refresh</button>
        </div>

        <div id="sms-new-panel" class="card" style="display:none;">
          <div class="card-hd"><span class="card-title">New SMS</span></div>
          <div class="grid-2" style="margin-bottom:8px;">
            <input type="text" id="sms-to" class="input" placeholder="Phone number">
            <input type="text" id="sms-body" class="input" placeholder="Message text..." onkeydown="if(event.key==='Enter')sendSms()">
          </div>
          <div class="row" style="justify-content:flex-end;">
            <button class="btn btn-sm" onclick="toggleNewSmsPanel(false)">Cancel</button>
            <button class="btn btn-primary btn-sm" onclick="sendSms()">Send SMS</button>
          </div>
        </div>

        <div class="sms-split">
          <div class="sms-threads" id="sms-thread-list">
            <div style="padding:16px; color:var(--text-muted);">Loading conversations...</div>
          </div>
          <div class="sms-detail">
            <div class="sms-detail-hd">
              <div>
                <div style="font-weight:600;" id="sms-active-title">Select a conversation</div>
                <div class="mono" style="font-size:11px; color:var(--text-muted);" id="sms-active-sub">--</div>
              </div>
              <button class="btn btn-sm" id="sms-active-dial" style="display:none;" onclick="dialActiveThread()">Call</button>
            </div>
            <div class="sms-bubbles" id="sms-bubbles">
              <div style="margin:auto; color:var(--text-muted); font-size:12.5px;">Choose a thread on the left to read and reply.</div>
            </div>
            <div class="sms-reply-bar">
              <input type="text" id="sms-reply-input" class="input" placeholder="Type reply..." disabled onkeydown="if(event.key==='Enter')sendThreadReply()">
              <button class="btn btn-primary" id="sms-reply-btn" disabled onclick="sendThreadReply()">Send</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 4. CONTACTS -->
      <section id="page-contacts" class="page">
        <div class="card">
          <div class="row-wrap" style="margin-bottom:10px;">
            <input type="text" id="c-name" class="input" style="max-width:190px;" placeholder="Name">
            <input type="text" id="c-phone" class="input" style="max-width:170px;" placeholder="Phone">
            <input type="email" id="c-email" class="input" style="max-width:210px;" placeholder="Email">
            <button class="btn btn-primary" onclick="addContact()">Add Contact</button>
            <input type="search" id="contact-search" class="input" style="max-width:220px; margin-left:auto;" placeholder="Search contacts..." oninput="filterContacts()">
            <button class="btn btn-sm" onclick="loadContacts()">Refresh</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Name</th><th>Phone</th><th>Email</th><th style="width:140px;">Actions</th></tr></thead>
              <tbody id="contacts-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 5. CALLS -->
      <section id="page-calls" class="page">
        <div class="card">
          <div class="row-wrap" style="margin-bottom:12px;">
            <span class="pill">Carrier: <b id="p-op">--</b></span>
            <span class="pill">Network: <b id="p-net">--</b></span>
            <span class="pill">SIM: <b id="p-sim">--</b></span>
            <div class="row" style="margin-left:auto;">
              <input type="text" id="dial-num" class="input" style="max-width:200px;" placeholder="Number to dial..." onkeydown="if(event.key==='Enter')dialNumber()">
              <button class="btn btn-primary" onclick="dialNumber()">Dial</button>
              <button class="btn btn-sm" onclick="loadCalls()">Refresh</button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Type</th><th>Contact / Number</th><th>Duration</th><th>Date</th><th>Quick</th></tr></thead>
              <tbody id="calls-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 6. CALENDAR -->
      <section id="page-calendar" class="page">
        <div class="card">
          <div class="row-wrap" style="margin-bottom:12px;">
            <input type="text" id="cal-title" class="input" style="max-width:230px;" placeholder="Event title">
            <input type="text" id="cal-loc" class="input" style="max-width:180px;" placeholder="Location">
            <input type="text" id="cal-desc" class="input" style="max-width:230px;" placeholder="Notes">
            <button class="btn btn-primary" onclick="addCalendarEvent()">Add Event (+1h)</button>
            <button class="btn btn-sm" style="margin-left:auto;" onclick="loadCalendar()">Refresh</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Title</th><th>Start</th><th>Location</th></tr></thead>
              <tbody id="cal-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 7. FILE EXPLORER -->
      <section id="page-files" class="page">
        <div class="warn-bar" id="perm-warn-all_files">
          <span>All Files Access is not granted on the phone; browsing app storage fallback.</span>
        </div>

        <div class="card">
          <div class="row-wrap" style="margin-bottom:10px;">
            <button class="btn btn-sm" onclick="goUpDir()">Up</button>
            <input type="text" id="file-path-box" class="input mono" style="flex:1; min-width:200px;" onkeydown="if(event.key==='Enter')loadFiles(this.value)">
            <button class="btn btn-sm" onclick="loadFiles(document.getElementById('file-path-box').value)">Go</button>
            <button class="btn btn-sm" onclick="loadFiles('')">Home</button>
            <button class="btn btn-sm" onclick="createFolder()">+ Folder</button>
            <input type="search" id="file-filter" class="input" style="max-width:190px;" placeholder="Filter folder..." oninput="renderFiles()">
            <button class="btn btn-sm" onclick="loadFiles(currentDir)">Refresh</button>
          </div>

          <div class="dropzone" id="file-dropzone">
            <div>
              <div style="font-weight:600;">Upload Files</div>
              <div style="font-size:12px; color:var(--text-secondary);">Drag &amp; drop files here or pick files to upload to current folder.</div>
            </div>
            <div class="row">
              <input type="file" id="upload-file-input" multiple style="font-size:12px; max-width:220px;">
              <button class="btn btn-primary btn-sm" id="upload-btn" onclick="uploadSelectedFiles()">Upload</button>
            </div>
            <div id="upload-progress-wrap" style="display:none; width:100%;">
              <div class="row" style="justify-content:space-between; font-size:11.5px;">
                <span id="upload-status-lbl">Uploading...</span>
                <span class="mono" id="upload-pct-lbl">0%</span>
              </div>
              <div class="progress-track"><div class="progress-fill" id="upload-progress-bar"></div></div>
            </div>
          </div>

          <div class="table-wrap">
            <table>
              <thead><tr><th>Name</th><th style="width:100px;">Size</th><th style="width:240px;">Actions</th></tr></thead>
              <tbody id="files-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 8. MEDIA GALLERY -->
      <section id="page-media" class="page">
        <div class="card">
          <div class="row-wrap" style="margin-bottom:12px;">
            <button class="btn btn-sm btn-primary" id="m-btn-images" onclick="loadMedia('images')">Images</button>
            <button class="btn btn-sm" id="m-btn-videos" onclick="loadMedia('videos')">Videos</button>
            <button class="btn btn-sm" id="m-btn-audio" onclick="loadMedia('audio')">Audio</button>
            <input type="search" id="media-filter" class="input" style="max-width:240px; margin-left:auto;" placeholder="Filter media..." oninput="renderMediaGrid()">
          </div>
          <div class="media-grid" id="media-grid"></div>
        </div>
      </section>

      <!-- 9. APPS & APK INSTALLER -->
      <section id="page-apps" class="page">
        <div class="card">
          <div class="card-hd">
            <span class="card-title">Remote APK Installer</span>
            <div class="row">
              <span class="tag tag-amber" id="apk-perm-tag">Unknown Apps: Checking</span>
              <span class="tag tag-blue" id="acc-perm-tag">Accessibility: Checking</span>
            </div>
          </div>
          <div id="app-op-banner" style="display:none; background:var(--bg-subtle); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:7px 11px; margin-bottom:10px; font-size:12px;" class="mono"></div>

          <div class="dropzone" id="apk-dropzone">
            <div>
              <div style="font-weight:600;">Upload &amp; Install APK</div>
              <div style="font-size:12px; color:var(--text-secondary);">Drop an .apk file or select from desktop.</div>
            </div>
            <div class="row">
              <input type="file" id="apk-file-input" accept=".apk" style="font-size:12px; max-width:220px;">
              <button class="btn btn-primary btn-sm" id="apk-upload-btn" onclick="uploadAndInstallApk()">Install APK</button>
            </div>
            <div id="apk-progress-wrap" style="display:none; width:100%;">
              <div class="row" style="justify-content:space-between; font-size:11.5px;">
                <span id="apk-upload-lbl">Uploading APK...</span>
                <span class="mono" id="apk-pct-lbl">0%</span>
              </div>
              <div class="progress-track"><div class="progress-fill" id="apk-progress-bar"></div></div>
            </div>
          </div>

          <div class="row">
            <input type="text" id="apk-path-input" class="input mono" placeholder="/sdcard/Download/app.apk">
            <button class="btn" onclick="installApkFromPath()">Install Path</button>
          </div>
        </div>

        <div class="card">
          <div class="row-wrap" style="margin-bottom:12px;">
            <button class="chip active" id="app-tab-user" onclick="setAppTab('user')">User Apps</button>
            <button class="chip" id="app-tab-all" onclick="setAppTab('all')">All Apps</button>
            <button class="chip" id="app-tab-sys" onclick="setAppTab('sys')">System</button>
            <input type="search" id="app-filter" class="input" style="max-width:260px; margin-left:auto;" placeholder="Filter installed apps..." oninput="renderFilteredApps()">
            <button class="btn btn-sm" onclick="loadApps(); loadUsage();">Refresh</button>
          </div>
          <div class="grid-3" id="apps-grid"></div>
        </div>

        <div class="card">
          <div class="card-hd"><span class="card-title">24h App Usage</span></div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>App</th><th>Package</th><th>Foreground</th></tr></thead>
              <tbody id="usage-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 10. SHELL TERMINAL -->
      <section id="page-terminal" class="page">
        <div class="card">
          <div class="card-hd">
            <span class="card-title">Android Shell</span>
            <span class="pill mono" id="term-cwd-badge">/</span>
          </div>
          <div class="row-wrap" style="margin-bottom:10px;">
            <button class="chip" onclick="runTerm('ls -la')">ls -la</button>
            <button class="chip" onclick="runTerm('pwd')">pwd</button>
            <button class="chip" onclick="runTerm('df -h')">df -h</button>
            <button class="chip" onclick="runTerm('uptime')">uptime</button>
            <button class="chip" onclick="runTerm('getprop ro.product.model')">model</button>
            <button class="chip" onclick="runTerm('ip addr show')">ip addr</button>
            <button class="btn btn-sm" style="margin-left:auto;" onclick="clearTerm()">Clear</button>
          </div>
          <pre class="term-out" id="term-out">PhoneDeck Shell Ready.</pre>
          <div class="row" style="margin-top:10px;">
            <input type="text" id="term-input" class="input mono" placeholder="Enter command (ArrowUp/Down for history)..." onkeydown="onTermKey(event)">
            <button class="btn btn-primary" onclick="runTermInput()">Run</button>
          </div>
        </div>
      </section>

      <!-- 11. NOTIFICATIONS (CAPTURED FEED ONLY) -->
      <section id="page-notifications" class="page">
        <div class="card">
          <div class="card-hd">
            <span class="card-title">Captured Device Notifications</span>
            <div class="row">
              <button class="btn btn-sm" onclick="loadNotifications()">Refresh</button>
              <button class="btn btn-sm btn-danger" onclick="clearNotifs()">Clear</button>
            </div>
          </div>
          <div id="notif-list"></div>
        </div>
      </section>

      <!-- 12. LOCATION & RADIO -->
      <section id="page-hardware" class="page">
        <div class="grid-2">
          <div class="card">
            <div class="card-hd">
              <span class="card-title">GPS Location</span>
              <button class="btn btn-sm" onclick="loadLocation()">Refresh</button>
            </div>
            <div class="table-wrap" style="margin-bottom:10px;">
              <table>
                <tbody>
                  <tr><td style="color:var(--text-secondary);">Latitude</td><td class="mono" id="loc-lat">--</td></tr>
                  <tr><td style="color:var(--text-secondary);">Longitude</td><td class="mono" id="loc-lng">--</td></tr>
                  <tr><td style="color:var(--text-secondary);">Accuracy</td><td class="mono" id="loc-acc">--</td></tr>
                  <tr><td style="color:var(--text-secondary);">Provider</td><td class="mono" id="loc-prov">--</td></tr>
                </tbody>
              </table>
            </div>
            <a class="btn btn-primary btn-sm" id="loc-map-link" href="#" target="_blank" style="display:none;">Open in Maps</a>
          </div>

          <div class="card">
            <div class="card-hd">
              <span class="card-title">Bluetooth &amp; Wi-Fi</span>
              <button class="btn btn-sm" onclick="loadBluetooth()">Refresh</button>
            </div>
            <div class="row-wrap" style="margin-bottom:10px;">
              <span class="pill">Bluetooth: <b id="bt-state">--</b></span>
              <span class="pill">Adapter: <b id="bt-name">--</b></span>
              <span class="pill">Wi-Fi: <b id="net-ssid">--</b></span>
              <span class="pill mono" id="net-ip">--</span>
            </div>
            <div class="table-wrap">
              <table>
                <thead><tr><th>Device</th><th>Address</th><th>State</th></tr></thead>
                <tbody id="bt-tbody"></tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- 13. MCP AGENT -->
      <section id="page-mcp" class="page">
        <div class="card">
          <div class="card-hd">
            <div class="row">
              <span class="card-title">MCP Agent Server</span>
              <span class="tag tag-green" id="mcp-status-pill">Active</span>
              <span class="tag tag-blue" id="mcp-tools-pill">0 Tools</span>
            </div>
            <label class="row" style="font-size:12.5px; font-weight:600; cursor:pointer;">
              <input type="checkbox" id="mcp-master-toggle" checked onchange="toggleMcpMaster(this.checked)" style="width:16px; height:16px; accent-color:var(--accent);">
              Enable MCP
            </label>
          </div>

          <div class="grid-2" style="margin-bottom:12px;">
            <div>
              <label style="display:block; font-size:11.5px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">MCP Bearer Token</label>
              <div class="row">
                <div class="mono" id="mcp-token-box" style="flex:1; background:var(--bg-subtle); border:1px solid var(--border-color); border-radius:var(--radius-md); padding:7px 10px; overflow:hidden; text-overflow:ellipsis;">••••••••</div>
                <button class="btn btn-sm" id="btn-toggle-token" onclick="toggleMcpTokenVisibility()">Show</button>
                <button class="btn btn-sm" onclick="copyMcpToken()">Copy</button>
                <button class="btn btn-sm btn-danger" onclick="regenerateMcpToken()">Rotate</button>
              </div>
            </div>
            <div>
              <label style="display:block; font-size:11.5px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Tunnel Endpoint Override</label>
              <div class="row">
                <input type="url" id="mcp-tunnel-input" class="input mono" placeholder="https://your-tunnel-url">
                <button class="btn btn-primary btn-sm" onclick="saveMcpTunnelUrl()">Save</button>
              </div>
            </div>
          </div>

          <div class="row-wrap" style="margin-bottom:8px;">
            <button class="chip active" id="tab-cfg-http" onclick="setCfgTab('http')">Streamable HTTP</button>
            <button class="chip" id="tab-cfg-sse" onclick="setCfgTab('sse')">SSE</button>
            <button class="chip" id="tab-cfg-claude" onclick="setCfgTab('claude')">Claude / Cursor</button>
            <button class="chip" id="tab-cfg-curl" onclick="setCfgTab('curl')">cURL</button>
            <button class="btn btn-sm" style="margin-left:auto;" onclick="copyConfigSnippet()">Copy Snippet</button>
          </div>
          <pre class="mono" id="mcp-config-snippet" style="background:#0f172a; color:#e2e8f0; border-radius:var(--radius-md); padding:12px; font-size:12px; overflow-x:auto;"></pre>
        </div>

        <div class="card">
          <div class="card-hd"><span class="card-title">MCP Category Permissions</span></div>
          <div class="grid-3" id="mcp-category-grid"></div>
        </div>

        <div class="card">
          <div class="card-hd">
            <span class="card-title">JSON-RPC Tester</span>
            <div class="row">
              <button class="btn btn-sm btn-primary" onclick="testMcpToolsList()">tools/list</button>
              <button class="btn btn-sm" onclick="testMcpPing()">ping</button>
            </div>
          </div>
          <div class="grid-2" style="margin-bottom:8px;">
            <select id="mcp-test-select" class="input" onchange="onSelectTestTool(this.value)"><option value="">Select tool...</option></select>
            <div class="row">
              <input type="text" id="mcp-test-args" class="input mono" placeholder="{}">
              <button class="btn btn-primary btn-sm" onclick="runMcpToolTest()">Execute</button>
            </div>
          </div>
          <pre class="mono" id="mcp-test-result" style="background:#0f172a; color:#e2e8f0; border-radius:var(--radius-md); padding:12px; font-size:12px; min-height:70px; max-height:240px; overflow:auto;">// Ready</pre>
        </div>
      </section>

      <!-- 14. PERMISSIONS -->
      <section id="page-permissions" class="page">
        <div class="card">
          <div class="card-hd">
            <span class="card-title">Android Runtime &amp; Special Permissions</span>
            <button class="btn btn-sm" onclick="fetchPermissions()">Refresh</button>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Permission</th><th>Scope</th><th>Status</th></tr></thead>
              <tbody id="perm-matrix-tbody"></tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 15. SECURITY & IDENTITY -->
      <section id="page-settings" class="page">
        <div class="grid-2">
          <div class="card">
            <div class="card-hd">
              <span class="card-title">Device Identity &amp; Sessions</span>
              <span class="tag tag-green" id="auth-status-tag">Device Bound</span>
            </div>
            <div class="table-wrap" style="margin-bottom:12px;">
              <table>
                <tbody>
                  <tr><td style="color:var(--text-secondary); width:38%;">Owner User ID</td><td class="mono" id="owner-user-id">--</td></tr>
                  <tr><td style="color:var(--text-secondary);">Device ID</td><td class="mono" id="owner-device-id">--</td></tr>
                  <tr><td style="color:var(--text-secondary);">OTP Channel</td><td><span class="tag tag-blue">On-Device OTP</span></td></tr>
                  <tr><td style="color:var(--text-secondary);">Public Tunnel</td><td class="mono" id="owner-remote-status">--</td></tr>
                </tbody>
              </table>
            </div>
            <div class="row-wrap">
              <button class="btn" onclick="logoutSession(false)">Sign Out This Browser</button>
              <button class="btn btn-danger" onclick="logoutSession(true)">Revoke All Sessions</button>
            </div>
          </div>

          <div class="card">
            <div class="card-hd"><span class="card-title">Change Access Password</span></div>
            <form onsubmit="changePassword(event)">
              <div style="margin-bottom:9px;">
                <label style="display:block; font-size:12px; font-weight:500; margin-bottom:3px;">Current Password</label>
                <input type="password" id="set-cur-pwd" class="input" required>
              </div>
              <div style="margin-bottom:9px;">
                <label style="display:block; font-size:12px; font-weight:500; margin-bottom:3px;">New Password (8+ chars)</label>
                <input type="password" id="set-new-pwd" class="input" minlength="8" required>
              </div>
              <div style="margin-bottom:10px;">
                <label style="display:block; font-size:12px; font-weight:500; margin-bottom:3px;">Confirm New Password</label>
                <input type="password" id="set-conf-pwd" class="input" minlength="8" required>
              </div>
              <div id="pwd-change-msg" style="display:none; padding:8px 10px; border-radius:var(--radius-md); font-size:12px; margin-bottom:9px;"></div>
              <button type="submit" class="btn btn-primary" id="save-pwd-btn">Update Password</button>
            </form>
          </div>
        </div>
      </section>

    </main>
  </div>
</div>

<!-- SHARED FILE / MEDIA PREVIEW & CODE EDITOR MODAL -->
<div class="modal-backdrop" id="preview-modal" onclick="if(event.target===this)closeModal()">
  <div class="modal-box">
    <div class="modal-hd">
      <div>
        <div style="font-weight:600;" id="modal-title">Preview</div>
        <div class="mono" style="font-size:11px; color:var(--text-muted);" id="modal-sub">--</div>
      </div>
      <div class="row">
        <button class="btn btn-primary btn-sm" id="modal-save-btn" style="display:none;" onclick="saveEditedFile()">Save</button>
        <a class="btn btn-sm" id="modal-dl-btn" href="#" download>Download</a>
        <button class="btn btn-sm" onclick="closeModal()">Close</button>
      </div>
    </div>
    <div class="modal-body" id="modal-body"></div>
  </div>
</div>

<div id="toast-wrap"></div>

<script>
var PD_BASE = (function() {
  var m = window.location.pathname.match(/^(\/d\/[^\/]+)/);
  return m ? m[1] : '';
})();
function apiUrl(p) {
  if (PD_BASE && typeof p === 'string' && p.charAt(0) === '/' && p.indexOf(PD_BASE + '/') !== 0) {
    return PD_BASE + p;
  }
  return p;
}
function homeUrl() { return (PD_BASE || '') + '/'; }
(function() {
  var origFetch = window.fetch;
  if (origFetch && PD_BASE) {
    window.fetch = function(input, init) {
      if (typeof input === 'string' && input.charAt(0) === '/') input = apiUrl(input);
      return origFetch.call(window, input, init);
    };
  }
  var snapLink = document.getElementById('cast-snapshot-link');
  if (snapLink && PD_BASE) snapLink.setAttribute('href', apiUrl('/api/screen/frame'));
})();

var PAGE_META = {
  overview: ['Control', 'Dashboard'],
  screen: ['Control', 'Live Screen & Cam'],
  sms: ['Communications', 'Messages'],
  contacts: ['Communications', 'Contacts'],
  calls: ['Communications', 'Calls'],
  calendar: ['Communications', 'Calendar'],
  files: ['Storage', 'File Explorer'],
  media: ['Storage', 'Media Gallery'],
  apps: ['System & Agent', 'Apps & APK'],
  terminal: ['System & Agent', 'Shell Terminal'],
  notifications: ['System & Agent', 'Notifications'],
  hardware: ['System & Agent', 'Location & Radio'],
  mcp: ['System & Agent', 'MCP Agent'],
  permissions: ['System & Agent', 'Permissions'],
  settings: ['System & Agent', 'Security']
};

var currentTab = 'overview';
var loadedPages = {};
var permMap = {};
var currentDir = '';
var parentDir = '';
var filesCache = [];
var mediaCache = [];
var mediaType = 'images';
var contactsCache = [];
var smsRaw = [];
var smsThreads = [];
var activeThreadKey = '';
var appsCache = [];
var appTab = 'user';
var inFlightStatus = false;
var inFlightPerms = false;
var castActive = false;
var accessibilityOk = false;
var camLens = 'back';
var termCwd = '';
var termHistory = [];
var termHistIdx = -1;
var editingFilePath = '';

function setElText(id, val) {
  var el = document.getElementById(id);
  if (el) el.textContent = val;
}
function toast(msg) {
  var wrap = document.getElementById('toast-wrap');
  if (!wrap) return;
  var el = document.createElement('div');
  el.className = 'toast';
  el.textContent = msg;
  wrap.appendChild(el);
  setTimeout(function() { if (el.parentNode) el.remove(); }, 3000);
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
function fmtShortTime(ms) {
  if (!ms) return '';
  var d = new Date(ms);
  var now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

/* SIDEBAR & LAZY PAGE NAVIGATION */
function toggleSidebar(force) {
  var sb = document.getElementById('sidebar-nav');
  var bd = document.getElementById('sidebar-backdrop');
  if (!sb || !bd) return;
  var open = (typeof force === 'boolean') ? force : !sb.classList.contains('open');
  sb.classList.toggle('open', open);
  bd.classList.toggle('open', open);
}

function openPage(pageId) {
  currentTab = pageId;
  var pages = document.querySelectorAll('.page');
  for (var i = 0; i < pages.length; i++) pages[i].classList.remove('active');
  var target = document.getElementById('page-' + pageId);
  if (target) target.classList.add('active');

  var items = document.querySelectorAll('.nav-item');
  for (var j = 0; j < items.length; j++) {
    items[j].classList.toggle('active', items[j].getAttribute('data-page') === pageId);
  }

  var meta = PAGE_META[pageId] || ['PhoneDeck', pageId];
  setElText('active-group-lbl', meta[0]);
  setElText('active-page-lbl', meta[1]);
  toggleSidebar(false);

  var img = document.getElementById('cast-img');
  if (pageId !== 'screen' && img) {
    img.src = '';
  } else if (pageId === 'screen') {
    refreshCastStatus();
  }
  ensurePageLoaded(pageId);
}

function ensurePageLoaded(pageId) {
  if (loadedPages[pageId]) return;
  loadedPages[pageId] = true;
  if (pageId === 'sms') loadSms();
  else if (pageId === 'contacts') loadContacts();
  else if (pageId === 'calls') loadCalls();
  else if (pageId === 'calendar') loadCalendar();
  else if (pageId === 'files') loadFiles(currentDir);
  else if (pageId === 'media') loadMedia(mediaType);
  else if (pageId === 'apps') { loadAppOpStatus(); loadApps(); loadUsage(); }
  else if (pageId === 'notifications') loadNotifications();
  else if (pageId === 'hardware') { loadLocation(); loadBluetooth(); }
  else if (pageId === 'mcp') loadMcpConfig();
  else if (pageId === 'permissions') fetchPermissions();
  else if (pageId === 'settings') refreshAuthInfo();
}

function syncAllNow() {
  fetchStatus();
  fetchPermissions();
  delete loadedPages[currentTab];
  if (currentTab === 'screen') refreshCastStatus();
  ensurePageLoaded(currentTab);
  toast('Synced');
}

/* CORE TELEMETRY (VISIBILITY-AWARE 5s POLLING) */
function fetchStatus() {
  if (inFlightStatus) return;
  inFlightStatus = true;
  var t0 = performance.now();
  fetch('/api/status')
    .then(function(r) {
      if (r.status === 401) { window.location.replace(homeUrl()); throw new Error('unauthorized'); }
      return r.json();
    })
    .then(function(s) {
      var ping = Math.round(performance.now() - t0);
      setElText('ping-label', ping + 'ms');
      var dot = document.getElementById('conn-dot');
      if (dot) dot.className = 'status-dot';
      var cp = document.getElementById('top-conn-pill');
      if (cp) { cp.className = 'pill live'; cp.textContent = 'Online'; }

      setElText('top-bat', s.batteryLevel + '%' + (s.isCharging ? ' +' : ''));
      setElText('top-wakelock', s.isWakeLockActive ? 'Awake' : 'Eco Idle');
      setElText('top-device-name', s.manufacturer + ' ' + s.model);

      var rm = document.getElementById('rng-media');
      if (rm && !rm.matches(':active')) { rm.value = s.mediaVolumePercent; setElText('lbl-media', s.mediaVolumePercent + '%'); }
      var rr = document.getElementById('rng-ring');
      if (rr && !rr.matches(':active')) { rr.value = s.ringVolumePercent; setElText('lbl-ring', s.ringVolumePercent + '%'); }
      var ra = document.getElementById('rng-alarm');
      if (ra && !ra.matches(':active')) { ra.value = s.alarmVolumePercent; setElText('lbl-alarm', s.alarmVolumePercent + '%'); }

      setElText('t-bat', s.batteryLevel + '%');
      setElText('t-bat-sub', s.isCharging ? (s.pluggedType || 'Charging') : 'Discharging');
      setElText('t-temp', Number(s.batteryTempC || 0).toFixed(1) + '\u00B0C');
      setElText('t-volt', (s.batteryVoltageMv || 0) + ' mV');
      var usedRam = (s.totalRamMb || 0) - (s.availRamMb || 0);
      setElText('t-ram', usedRam + ' MB');
      setElText('t-ram-sub', 'Total: ' + (s.totalRamMb || 0) + ' MB');
      setElText('t-stor', s.freeStorageGb + ' GB');
      setElText('t-stor-sub', 'Total: ' + s.totalStorageGb + ' GB');

      setElText('d-model', s.manufacturer + ' ' + s.model);
      setElText('d-os', 'Android ' + s.androidVersion + ' (API ' + s.sdkInt + ')');
      setElText('d-ip', s.localIp + ':' + s.serverPort);
      setElText('d-wifi', s.wifiSsid || '--');
      setElText('d-screen', s.isScreenOn ? 'ON' : 'OFF');
      setElText('d-uptime', s.uptimeSeconds + 's');
      setElText('d-reqs', s.totalRequests);
      setElText('net-ssid', s.wifiSsid || '--');
      setElText('net-ip', s.localIp || '--');
      if (s.clipboardPreview) {
        var cb = document.getElementById('clip-box');
        if (cb && cb.textContent === '--') cb.textContent = s.clipboardPreview;
      }
    })
    .catch(function() {
      var dot = document.getElementById('conn-dot');
      if (dot) dot.className = 'status-dot offline';
      var cp = document.getElementById('top-conn-pill');
      if (cp) { cp.className = 'pill warn'; cp.textContent = 'Offline'; }
    })
    .finally(function() { inFlightStatus = false; });
}

function fetchPermissions() {
  if (inFlightPerms) return;
  inFlightPerms = true;
  fetch('/api/permissions')
    .then(function(r) {
      if (r.status === 401) { window.location.replace(homeUrl()); throw new Error('unauthorized'); }
      return r.json();
    })
    .then(function(data) {
      permMap = data.map || {};
      var list = data.permissions || [];
      var tbody = document.getElementById('perm-matrix-tbody');
      if (!tbody) return;
      var html = '';
      for (var i = 0; i < list.length; i++) {
        var p = list[i];
        var warn = document.getElementById('perm-warn-' + p.id);
        if (warn) warn.style.display = p.isGranted ? 'none' : 'flex';
        html += '<tr><td style="font-weight:500;">' + esc(p.title) + '</td><td class="mono">' + esc(p.webPageId) + '</td>' +
          '<td><span class="tag ' + (p.isGranted ? 'tag-green' : 'tag-rose') + '">' + (p.isGranted ? 'Granted' : 'Denied') + '</span></td></tr>';
      }
      tbody.innerHTML = html;
    })
    .catch(function() {})
    .finally(function() { inFlightPerms = false; });
}

/* HARDWARE, MEDIA REMOTE, CLIPBOARD */
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
  .catch(function() { toast('Request failed'); });
}
function sendMediaKey(key) { runAction('media_key', { key: key }); }
function setVol(stream, val) {
  setElText('lbl-' + stream, val + '%');
  runAction('set_volume', { stream: stream, percent: parseInt(val, 10) });
}
function refreshClip() {
  runAction('get_clipboard').then(function(res) {
    setElText('clip-box', (res && res.clipboard) ? res.clipboard : '(Clipboard empty)');
  });
}
function copyPhoneClipToPc() {
  var txt = (document.getElementById('clip-box') || {}).textContent || '';
  if (!txt || txt === '--' || txt === '(Clipboard empty)') return toast('Nothing to copy');
  navigator.clipboard.writeText(txt).then(function() { toast('Copied to desktop clipboard'); });
}
function pushClip() {
  var inp = document.getElementById('clip-input');
  var txt = inp ? inp.value : '';
  if (!txt) return;
  runAction('set_clipboard', { text: txt }).then(function() {
    setElText('clip-box', txt);
    inp.value = '';
  });
}
function speakText() {
  var txt = (document.getElementById('tts-input') || {}).value || '';
  if (txt.trim()) runAction('speak', { text: txt.trim() });
}
function openUrl() {
  var url = (document.getElementById('url-input') || {}).value || '';
  if (url.trim()) runAction('open_url', { url: url.trim() });
}

/* INTERACTIVE LIVE SCREEN & REMOTE CAMERA */
function refreshCastStatus() {
  fetch('/api/screen/status')
    .then(function(r) { return r.json(); })
    .then(function(st) {
      castActive = !!st.isCasting;
      accessibilityOk = !!st.accessibilityEnabled;
      setElText('cast-frames', st.frameCount || 0);
      var accTag = document.getElementById('cast-acc-tag');
      if (accTag) {
        accTag.className = 'tag ' + (accessibilityOk ? 'tag-green' : 'tag-amber');
        accTag.textContent = accessibilityOk ? 'Touch Ready' : 'Enable Accessibility for Touch';
      }
      var tag = document.getElementById('cast-res-tag');
      var img = document.getElementById('cast-img');
      var ph = document.getElementById('cast-placeholder');
      if (st.isCasting) {
        if (tag) { tag.className = 'tag tag-green'; tag.textContent = 'LIVE ' + (st.width || '') + 'x' + (st.height || ''); }
        if (ph) ph.style.display = 'none';
        if (img) {
          img.style.display = 'block';
          if (currentTab === 'screen' && (!img.src || img.src.indexOf('/api/screen/stream') === -1)) {
            img.src = apiUrl('/api/screen/stream?t=' + Date.now());
          }
        }
      } else {
        if (tag) { tag.className = 'tag tag-amber'; tag.textContent = 'Offline'; }
        if (img) { img.style.display = 'none'; img.src = ''; }
        if (ph) ph.style.display = 'block';
      }
    })
    .catch(function() {});
}

function requestScreenCast() {
  fetch('/api/screen/request', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function(res) {
      toast(res.message || 'Confirm on phone');
      var n = 0;
      var t = setInterval(function() {
        n++;
        refreshCastStatus();
        if (castActive || n > 15) clearInterval(t);
      }, 1000);
    });
}
function stopScreenCast() {
  var img = document.getElementById('cast-img');
  if (img) img.src = '';
  fetch('/api/screen/stop', { method: 'POST' }).then(function() {
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
  }).then(function() { toast('Stream updated'); refreshCastStatus(); });
}
function toggleCastFullscreen() {
  var el = document.getElementById('cast-stage');
  if (!el) return;
  if (!document.fullscreenElement) { if (el.requestFullscreen) el.requestFullscreen(); }
  else { if (document.exitFullscreen) document.exitFullscreen(); }
}

(function bindScreenTouch() {
  var img = document.getElementById('cast-img');
  var wrap = document.getElementById('cast-img-wrap');
  if (!img || !wrap) return;
  var down = null;

  function normCoords(ev) {
    var rect = img.getBoundingClientRect();
    var x = Math.max(0, Math.min(1, (ev.clientX - rect.left) / Math.max(1, rect.width)));
    var y = Math.max(0, Math.min(1, (ev.clientY - rect.top) / Math.max(1, rect.height)));
    return { x: x, y: y, px: ev.clientX - rect.left, py: ev.clientY - rect.top };
  }
  function showRipple(px, py) {
    var r = document.createElement('div');
    r.className = 'tap-ripple';
    r.style.left = px + 'px';
    r.style.top = py + 'px';
    wrap.appendChild(r);
    setTimeout(function() { if (r.parentNode) r.remove(); }, 420);
  }
  img.addEventListener('pointerdown', function(ev) {
    if (ev.button !== 0) return;
    ev.preventDefault();
    var c = normCoords(ev);
    down = { x: c.x, y: c.y, clientX: ev.clientX, clientY: ev.clientY, t: Date.now() };
  });
  img.addEventListener('pointerup', function(ev) {
    if (!down) return;
    var c = normCoords(ev);
    var dist = Math.hypot(ev.clientX - down.clientX, ev.clientY - down.clientY);
    var elapsed = Date.now() - down.t;
    showRipple(c.px, c.py);
    if (dist < 12 && elapsed < 350) {
      sendScreenInput({ type: 'tap', x: Number(c.x.toFixed(4)), y: Number(c.y.toFixed(4)) });
    } else {
      sendScreenInput({
        type: 'swipe',
        x1: Number(down.x.toFixed(4)), y1: Number(down.y.toFixed(4)),
        x2: Number(c.x.toFixed(4)), y2: Number(c.y.toFixed(4)),
        duration: Math.max(140, Math.min(900, elapsed))
      });
    }
    down = null;
  });
  img.addEventListener('pointercancel', function() { down = null; });
})();

function sendScreenInput(payload) {
  return fetch('/api/screen/input', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
  .then(function(r) { return r.json(); })
  .then(function(res) {
    if (!res.success && res.message) toast(res.message);
    return res;
  })
  .catch(function() { toast('Touch dispatch failed'); });
}
function sendNav(action) { sendScreenInput({ type: 'nav', action: action }); }
function sendScreenText() {
  var inp = document.getElementById('screen-text-input');
  var txt = inp ? inp.value : '';
  if (!txt) return;
  sendScreenInput({ type: 'text', text: txt }).then(function(res) {
    if (res && res.success) { inp.value = ''; toast('Text sent'); }
  });
}

function setCamLens(lens) {
  camLens = lens;
  document.getElementById('cam-lens-back').className = 'chip' + (lens === 'back' ? ' active' : '');
  document.getElementById('cam-lens-front').className = 'chip' + (lens === 'front' ? ' active' : '');
}
function captureCamera() {
  var btn = document.getElementById('cam-cap-btn');
  var flash = document.getElementById('cam-flash') && document.getElementById('cam-flash').checked ? '1' : '0';
  if (btn) { btn.disabled = true; btn.textContent = 'Capturing...'; }
  fetch('/api/camera/capture?lens=' + camLens + '&flash=' + flash)
    .then(function(r) {
      if (!r.ok) return r.json().then(function(e) { throw new Error(e.message || 'Camera unavailable'); });
      return r.blob();
    })
    .then(function(blob) {
      var url = URL.createObjectURL(blob);
      var img = document.getElementById('cam-preview');
      var empty = document.getElementById('cam-empty');
      var dl = document.getElementById('cam-download');
      if (empty) empty.style.display = 'none';
      if (img) {
        if (img.dataset.objUrl) URL.revokeObjectURL(img.dataset.objUrl);
        img.dataset.objUrl = url;
        img.src = url;
        img.style.display = 'block';
      }
      if (dl) { dl.href = url; dl.style.display = 'inline-flex'; }
      setElText('cam-ts', new Date().toLocaleTimeString());
      toast('Photo captured');
    })
    .catch(function(e) { toast(e.message || 'Camera capture failed'); })
    .finally(function() { if (btn) { btn.disabled = false; btn.textContent = 'Capture'; } });
}

/* COMMS: THREADED SMS, CONTACTS, CALLS, CALENDAR */
function loadSms() {
  fetch('/api/sms')
    .then(function(r) { return r.json(); })
    .then(function(data) {
      smsRaw = data.items || [];
      buildSmsThreads();
      renderSmsThreads();
      if (activeThreadKey) openSmsThread(activeThreadKey);
    });
}

function buildSmsThreads() {
  var map = {};
  for (var i = 0; i < smsRaw.length; i++) {
    var m = smsRaw[i];
    var addr = (m.address || 'Unknown').trim();
    var norm = addr.replace(/\s+/g, '');
    var key = norm || ('t_' + (m.threadId || i));
    if (!map[key]) {
      map[key] = { key: key, address: addr, name: m.contactName || '', messages: [], latestDate: 0 };
    }
    if (m.contactName && !map[key].name) map[key].name = m.contactName;
    map[key].messages.push(m);
    if ((m.date || 0) > map[key].latestDate) map[key].latestDate = m.date || 0;
  }
  smsThreads = Object.keys(map).map(function(k) {
    var th = map[k];
    th.messages.sort(function(a, b) { return (a.date || 0) - (b.date || 0); });
    return th;
  });
  smsThreads.sort(function(a, b) { return b.latestDate - a.latestDate; });
  if (!activeThreadKey && smsThreads.length) activeThreadKey = smsThreads[0].key;
}

function renderSmsThreads() {
  var box = document.getElementById('sms-thread-list');
  if (!box) return;
  var q = ((document.getElementById('sms-search') || {}).value || '').toLowerCase().trim();
  var filtered = smsThreads.filter(function(th) {
    if (!q) return true;
    if ((th.name || '').toLowerCase().indexOf(q) !== -1) return true;
    if (th.address.toLowerCase().indexOf(q) !== -1) return true;
    for (var i = 0; i < th.messages.length; i++) {
      if ((th.messages[i].body || '').toLowerCase().indexOf(q) !== -1) return true;
    }
    return false;
  });
  if (!filtered.length) {
    box.innerHTML = '<div style="padding:16px; color:var(--text-muted);">No conversations found.</div>';
    return;
  }
  var html = '';
  for (var i = 0; i < filtered.length; i++) {
    var th = filtered[i];
    var last = th.messages[th.messages.length - 1] || {};
    var title = th.name ? th.name : th.address;
    var encKey = encodeURIComponent(th.key);
    html += '<div class="sms-thread-item' + (th.key === activeThreadKey ? ' active' : '') + '" onclick="openSmsThread(decodeURIComponent(\'' + encKey + '\'))">' +
      '<div class="sms-thread-top"><span class="sms-thread-name">' + esc(title) + '</span><span class="sms-thread-time">' + fmtShortTime(th.latestDate) + '</span></div>' +
      '<div class="sms-thread-snip">' + esc(last.body || '') + '</div>' +
    '</div>';
  }
  box.innerHTML = html;
}

function openSmsThread(key) {
  activeThreadKey = key;
  renderSmsThreads();
  var th = smsThreads.find(function(t) { return t.key === key; });
  var bubbles = document.getElementById('sms-bubbles');
  var repInp = document.getElementById('sms-reply-input');
  var repBtn = document.getElementById('sms-reply-btn');
  var dialBtn = document.getElementById('sms-active-dial');
  if (!th || !bubbles) return;

  setElText('sms-active-title', th.name || th.address);
  setElText('sms-active-sub', th.address + ' (' + th.messages.length + ' messages)');
  if (dialBtn) dialBtn.style.display = th.address ? 'inline-flex' : 'none';
  if (repInp) repInp.disabled = false;
  if (repBtn) repBtn.disabled = false;

  var html = '';
  for (var i = 0; i < th.messages.length; i++) {
    var m = th.messages[i];
    var isOut = m.type === 'Sent';
    html += '<div class="bubble ' + (isOut ? 'out' : 'in') + '">' +
      '<div>' + esc(m.body) + '</div>' +
      '<div class="bubble-ts">' + esc(m.type) + ' &middot; ' + new Date(m.date).toLocaleString([], { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' }) + '</div>' +
    '</div>';
  }
  bubbles.innerHTML = html;
  bubbles.scrollTop = bubbles.scrollHeight;
}

function toggleNewSmsPanel(force) {
  var p = document.getElementById('sms-new-panel');
  if (!p) return;
  p.style.display = (force === false || p.style.display === 'block') ? 'none' : 'block';
}
function sendThreadReply() {
  var th = smsThreads.find(function(t) { return t.key === activeThreadKey; });
  var inp = document.getElementById('sms-reply-input');
  if (!th || !inp) return;
  var msg = inp.value.trim();
  if (!msg) return;
  fetch('/api/sms/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone: th.address, message: msg })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Reply sent' : 'Failed to send');
    if (res.success) {
      inp.value = '';
      setTimeout(loadSms, 900);
    }
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
      toggleNewSmsPanel(false);
      setTimeout(loadSms, 900);
    }
  });
}
function dialActiveThread() {
  var th = smsThreads.find(function(t) { return t.key === activeThreadKey; });
  if (th && th.address) quickDial(th.address);
}

function loadContacts() {
  fetch('/api/contacts').then(function(r) { return r.json(); }).then(function(data) {
    contactsCache = data.items || [];
    filterContacts();
  });
}
function filterContacts() {
  var q = ((document.getElementById('contact-search') || {}).value || '').toLowerCase();
  var list = contactsCache.filter(function(c) {
    return !q || c.name.toLowerCase().indexOf(q) !== -1 || c.phone.toLowerCase().indexOf(q) !== -1;
  });
  var tbody = document.getElementById('contacts-tbody');
  if (!tbody) return;
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="4" style="color:var(--text-muted);">No contacts found.</td></tr>';
    return;
  }
  var html = '';
  for (var i = 0; i < list.length; i++) {
    var c = list[i];
    var encPh = encodeURIComponent(c.phone || '');
    html += '<tr><td style="font-weight:500;">' + esc(c.name) + '</td><td class="mono">' + esc(c.phone) + '</td><td class="mono">' + esc(c.email) + '</td>' +
      '<td>' + (c.phone ? '<button class="btn btn-sm" onclick="quickDial(decodeURIComponent(\'' + encPh + '\'))">Call</button> <button class="btn btn-sm" onclick="composeSmsTo(decodeURIComponent(\'' + encPh + '\'))">SMS</button>' : '') + '</td></tr>';
  }
  tbody.innerHTML = html;
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
function composeSmsTo(phone) {
  openPage('sms');
  var p = document.getElementById('sms-new-panel');
  if (p) p.style.display = 'block';
  var to = document.getElementById('sms-to');
  if (to) to.value = phone;
}
function quickDial(num) {
  fetch('/api/calls/dial', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ number: num })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Dialing ' + num : 'Call permission required');
  });
}

function loadCalls() {
  fetch('/api/calls').then(function(r) { return r.json(); }).then(function(data) {
    var p = data.phone || {};
    setElText('p-op', p.operatorName || '--');
    setElText('p-net', p.networkType || '--');
    setElText('p-sim', p.simState || '--');
    var list = data.items || [];
    var tbody = document.getElementById('calls-tbody');
    if (!tbody) return;
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:var(--text-muted);">No call logs.</td></tr>';
      return;
    }
    var html = '';
    for (var i = 0; i < list.length; i++) {
      var c = list[i];
      var cls = c.type === 'Missed' ? 'tag-rose' : (c.type === 'Incoming' ? 'tag-green' : 'tag-blue');
      var encNum = encodeURIComponent(c.number || '');
      html += '<tr><td><span class="tag ' + cls + '">' + esc(c.type) + '</span></td>' +
        '<td><b>' + esc(c.name || c.number) + '</b>' + (c.name ? ' <span class="mono">(' + esc(c.number) + ')</span>' : '') + '</td>' +
        '<td class="mono">' + c.durationSec + 's</td><td class="mono">' + new Date(c.date).toLocaleString() + '</td>' +
        '<td><button class="btn btn-sm" onclick="quickDial(decodeURIComponent(\'' + encNum + '\'))">Dial</button></td></tr>';
    }
    tbody.innerHTML = html;
  });
}
function dialNumber() {
  var num = document.getElementById('dial-num').value.trim();
  if (num) quickDial(num);
}

function loadCalendar() {
  fetch('/api/calendar').then(function(r) { return r.json(); }).then(function(data) {
    var list = data.items || [];
    var tbody = document.getElementById('cal-tbody');
    if (!tbody) return;
    if (!list.length) {
      tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No upcoming events.</td></tr>';
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
  if (!title) return toast('Enter event title');
  fetch('/api/calendar/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: title,
      location: document.getElementById('cal-loc').value.trim(),
      description: document.getElementById('cal-desc').value.trim()
    })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Event added' : 'Failed (check permission)');
    if (res.success) { document.getElementById('cal-title').value = ''; loadCalendar(); }
  });
}

/* FILES EXPLORER, IN-BROWSER PREVIEW/EDITOR & MEDIA GALLERY */
var TEXT_EXTS = { txt:1, json:1, md:1, xml:1, kt:1, java:1, js:1, ts:1, tsx:1, jsx:1, css:1, html:1, htm:1, py:1, sh:1, log:1, csv:1, yml:1, yaml:1, ini:1, conf:1, properties:1, gradle:1, kts:1, sql:1 };
var PREVIEW_EXTS = { jpg:'img', jpeg:'img', png:'img', gif:'img', webp:'img', bmp:'img', svg:'img', mp4:'vid', webm:'vid', mkv:'vid', mov:'vid', mp3:'aud', wav:'aud', ogg:'aud', m4a:'aud', flac:'aud', pdf:'pdf' };

function getExt(name) {
  var idx = name.lastIndexOf('.');
  return idx !== -1 ? name.slice(idx + 1).toLowerCase() : '';
}

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
      filesCache = data.items || [];
      renderFiles();
    })
    .catch(function() { toast('Could not load directory'); });
}

function renderFiles() {
  var q = ((document.getElementById('file-filter') || {}).value || '').toLowerCase();
  var items = filesCache.filter(function(f) { return !q || f.name.toLowerCase().indexOf(q) !== -1; });
  var tbody = document.getElementById('files-tbody');
  if (!tbody) return;
  if (!items.length) {
    tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">Empty folder</td></tr>';
    return;
  }
  var html = '';
  for (var i = 0; i < items.length; i++) {
    var f = items[i];
    var encPath = encodeURIComponent(f.path);
    var encName = encodeURIComponent(f.name);
    var ext = getExt(f.name);
    var nameCell = f.isDirectory
      ? '<a href="javascript:void(0)" onclick="loadFiles(decodeURIComponent(\'' + encPath + '\'))" style="color:var(--accent); font-weight:600; text-decoration:none;">[DIR] ' + esc(f.name) + '</a>'
      : '<span>' + esc(f.name) + '</span>';

    var acts = '';
    if (!f.isDirectory) {
      if (TEXT_EXTS[ext]) {
        acts += '<button class="btn btn-sm btn-primary" onclick="openFileEditor(decodeURIComponent(\'' + encPath + '\'))">Edit</button> ';
      } else if (PREVIEW_EXTS[ext]) {
        acts += '<button class="btn btn-sm btn-primary" onclick="openFilePreview(decodeURIComponent(\'' + encPath + '\'), decodeURIComponent(\'' + encName + '\'), \'' + PREVIEW_EXTS[ext] + '\')">Preview</button> ';
      }
      acts += '<a class="btn btn-sm" href="' + apiUrl('/api/download?path=' + encPath) + '" download>DL</a> ';
    }
    acts += '<button class="btn btn-sm" onclick="renameFile(decodeURIComponent(\'' + encPath + '\'), decodeURIComponent(\'' + encName + '\'))">Rename</button> ';
    acts += '<button class="btn btn-sm btn-danger" onclick="deleteFile(decodeURIComponent(\'' + encPath + '\'))">Del</button>';
    html += '<tr><td>' + nameCell + '</td><td class="mono">' + (f.isDirectory ? 'DIR' : fmtBytes(f.sizeBytes)) + '</td><td>' + acts + '</td></tr>';
  }
  tbody.innerHTML = html;
}

function goUpDir() { if (parentDir) loadFiles(parentDir); }
function renameFile(path, oldName) {
  var next = prompt('Rename "' + oldName + '" to:', oldName);
  if (!next || next === oldName) return;
  fetch('/api/files/rename', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: path, newName: next })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'Renamed' : (res.message || 'Rename failed'));
    if (res.success) loadFiles(currentDir);
  });
}
function deleteFile(path) {
  if (!confirm('Delete "' + path.split('/').pop() + '"?')) return;
  fetch('/api/files/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: path })
  }).then(function() { toast('Deleted'); loadFiles(currentDir); });
}
function createFolder() {
  var name = prompt('New folder name:');
  if (!name) return;
  fetch('/api/files/mkdir', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parentPath: currentDir, name: name })
  }).then(function() { toast('Folder created'); loadFiles(currentDir); });
}

function openFileEditor(path) {
  fetch('/api/files/read?path=' + encodeURIComponent(path))
    .then(function(r) { return r.json(); })
    .then(function(res) {
      if (!res.success) return toast(res.message || 'Cannot open file');
      editingFilePath = path;
      setElText('modal-title', res.name || path.split('/').pop());
      setElText('modal-sub', path);
      var saveBtn = document.getElementById('modal-save-btn');
      if (saveBtn) saveBtn.style.display = 'inline-flex';
      var dl = document.getElementById('modal-dl-btn');
      if (dl) { dl.href = apiUrl('/api/download?path=' + encodeURIComponent(path)); dl.style.display = 'inline-flex'; }
      var body = document.getElementById('modal-body');
      body.innerHTML = '<textarea id="modal-code-editor" class="code-editor" spellcheck="false"></textarea>';
      document.getElementById('modal-code-editor').value = res.content || '';
      document.getElementById('preview-modal').classList.add('open');
    });
}
function saveEditedFile() {
  var ed = document.getElementById('modal-code-editor');
  if (!ed || !editingFilePath) return;
  fetch('/api/files/write', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: editingFilePath, content: ed.value })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.success ? 'File saved' : (res.message || 'Save failed'));
    if (res.success) loadFiles(currentDir);
  });
}

function openFilePreview(path, name, kind) {
  editingFilePath = '';
  var url = apiUrl('/api/download?inline=1&path=' + encodeURIComponent(path));
  openMediaModal(name, path, apiUrl('/api/download?path=' + encodeURIComponent(path)), url, kind);
}
function openMediaModal(title, sub, dlUrl, rawUrl, kind) {
  editingFilePath = '';
  setElText('modal-title', title);
  setElText('modal-sub', sub);
  var saveBtn = document.getElementById('modal-save-btn');
  if (saveBtn) saveBtn.style.display = 'none';
  var dl = document.getElementById('modal-dl-btn');
  if (dl) { dl.href = dlUrl; dl.style.display = 'inline-flex'; }

  var body = document.getElementById('modal-body');
  if (kind === 'img') {
    body.innerHTML = '<img src="' + rawUrl + '" style="max-width:100%; max-height:70vh; display:block; margin:0 auto; border-radius:8px;">';
  } else if (kind === 'vid') {
    body.innerHTML = '<video src="' + rawUrl + '" controls autoplay style="width:100%; max-height:70vh; border-radius:8px; background:#000;"></video>';
  } else if (kind === 'aud') {
    body.innerHTML = '<div style="padding:28px; text-align:center;"><audio src="' + rawUrl + '" controls autoplay style="width:100%;"></audio></div>';
  } else if (kind === 'pdf') {
    body.innerHTML = '<iframe src="' + rawUrl + '" style="width:100%; height:70vh; border:1px solid var(--border-color); border-radius:8px;"></iframe>';
  }
  document.getElementById('preview-modal').classList.add('open');
}
function closeModal() {
  var m = document.getElementById('preview-modal');
  if (m) m.classList.remove('open');
  var body = document.getElementById('modal-body');
  if (body) body.innerHTML = '';
  editingFilePath = '';
}

function xhrUpload(url, file, onProgress) {
  return new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', apiUrl(url), true);
    var startTs = Date.now();
    xhr.upload.onprogress = function(ev) {
      if (ev.lengthComputable && onProgress) {
        var pct = Math.round((ev.loaded / ev.total) * 100);
        var sec = Math.max(0.1, (Date.now() - startTs) / 1000);
        onProgress(pct, fmtBytes(ev.loaded / sec) + '/s');
      }
    };
    xhr.onload = function() {
      try { resolve(JSON.parse(xhr.responseText)); }
      catch (e) { resolve({ success: xhr.status >= 200 && xhr.status < 300 }); }
    };
    xhr.onerror = function() { reject(new Error('Network error')); };
    xhr.send(file);
  });
}
function uploadSelectedFiles() {
  var inp = document.getElementById('upload-file-input');
  if (!inp || !inp.files || !inp.files.length) return toast('Select file(s) first');
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
      setElText('upload-status-lbl', 'Complete');
      setElText('upload-pct-lbl', '100%');
      if (bar) bar.style.width = '100%';
      toast('Uploaded ' + files.length + ' file(s)');
      loadFiles(currentDir);
      setTimeout(function() { if (wrap) wrap.style.display = 'none'; }, 2000);
      return;
    }
    var f = files[idx];
    setElText('upload-status-lbl', '(' + (idx + 1) + '/' + files.length + ') ' + f.name);
    xhrUpload('/api/upload?name=' + encodeURIComponent(f.name) + '&dir=' + encodeURIComponent(currentDir), f, function(pct, spd) {
      setElText('upload-status-lbl', f.name + ' - ' + spd);
      setElText('upload-pct-lbl', pct + '%');
      if (bar) bar.style.width = pct + '%';
    }).then(function() { idx++; next(); }).catch(function() { toast('Failed: ' + f.name); idx++; next(); });
  }
  next();
}

function loadMedia(type) {
  mediaType = type;
  ['images', 'videos', 'audio'].forEach(function(t) {
    var b = document.getElementById('m-btn-' + t);
    if (b) b.className = 'btn btn-sm ' + (t === type ? 'btn-primary' : '');
  });
  fetch('/api/media?type=' + type).then(function(r) { return r.json(); }).then(function(data) {
    mediaCache = data.items || [];
    renderMediaGrid();
  });
}
function renderMediaGrid() {
  var q = ((document.getElementById('media-filter') || {}).value || '').toLowerCase();
  var items = mediaCache.filter(function(m) { return !q || m.name.toLowerCase().indexOf(q) !== -1; });
  var grid = document.getElementById('media-grid');
  if (!grid) return;
  if (!items.length) {
    grid.innerHTML = '<div style="color:var(--text-muted); padding:12px;">No items found.</div>';
    return;
  }
  var kind = mediaType === 'images' ? 'img' : (mediaType === 'videos' ? 'vid' : 'aud');
  var html = '';
  for (var i = 0; i < items.length; i++) {
    var m = items[i];
    var rawUrl = apiUrl('/api/media/raw?id=' + m.id + '&type=' + mediaType);
    var encName = encodeURIComponent(m.name);
    var encRaw = encodeURIComponent(rawUrl);
    var thumb = mediaType === 'images'
      ? '<img src="' + rawUrl + '" class="media-thumb" loading="lazy">'
      : '<div class="media-thumb mono">' + (mediaType === 'videos' ? 'VIDEO' : 'AUDIO') + '</div>';
    html += '<div class="media-card" onclick="openMediaModal(decodeURIComponent(\'' + encName + '\'), \'' + fmtBytes(m.sizeBytes) + '\', decodeURIComponent(\'' + encRaw + '\'), decodeURIComponent(\'' + encRaw + '\'), \'' + kind + '\')">' +
      thumb +
      '<div class="media-info"><div class="media-name">' + esc(m.name) + '</div>' +
      '<div style="color:var(--text-muted); font-size:11px; margin-top:3px;">' + fmtBytes(m.sizeBytes) + '</div></div></div>';
  }
  grid.innerHTML = html;
}

/* APPS, REMOTE APK INSTALLER, TERMINAL, NOTIFICATIONS */
function loadAppOpStatus() {
  fetch('/api/apps/status').then(function(r) { return r.json(); }).then(function(st) {
    var apkTag = document.getElementById('apk-perm-tag');
    if (apkTag) {
      apkTag.className = 'tag ' + (st.canInstallUnknownApps ? 'tag-green' : 'tag-amber');
      apkTag.textContent = 'Unknown Apps: ' + (st.canInstallUnknownApps ? 'Ready' : 'Grant on Phone');
    }
    var accTag = document.getElementById('acc-perm-tag');
    if (accTag) {
      accTag.className = 'tag ' + (st.accessibilityEnabled ? 'tag-green' : 'tag-blue');
      accTag.textContent = 'Accessibility: ' + (st.accessibilityEnabled ? 'Active' : 'Optional');
    }
    var b = document.getElementById('app-op-banner');
    if (b && st.message) { b.style.display = 'block'; b.textContent = '[' + (st.state || 'info').toUpperCase() + '] ' + st.message; }
  }).catch(function() {});
}
function uploadAndInstallApk(dropFile) {
  var inp = document.getElementById('apk-file-input');
  var f = dropFile || (inp && inp.files && inp.files[0] ? inp.files[0] : null);
  if (!f) return toast('Select an .apk file first');
  var wrap = document.getElementById('apk-progress-wrap');
  var bar = document.getElementById('apk-progress-bar');
  var btn = document.getElementById('apk-upload-btn');
  if (wrap) wrap.style.display = 'block';
  if (btn) btn.disabled = true;
  xhrUpload('/api/apps/upload-install?name=' + encodeURIComponent(f.name), f, function(pct, spd) {
    setElText('apk-upload-lbl', f.name + ' - ' + spd);
    setElText('apk-pct-lbl', pct + '%');
    if (bar) bar.style.width = pct + '%';
  }).then(function(res) {
    if (btn) btn.disabled = false;
    toast((res && res.message) || 'Install dispatched');
    loadAppOpStatus();
    setTimeout(function() { loadApps(); loadAppOpStatus(); }, 2500);
  }).catch(function() { if (btn) btn.disabled = false; toast('APK upload failed'); });
}
function installApkFromPath() {
  var path = document.getElementById('apk-path-input').value.trim();
  if (!path) return toast('Enter APK path');
  fetch('/api/apps/install', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ path: path })
  }).then(function(r) { return r.json(); }).then(function(res) {
    toast(res.message || 'Install dispatched');
    loadAppOpStatus();
    setTimeout(function() { loadApps(); loadAppOpStatus(); }, 2500);
  });
}
function uninstallApp(pkg, name) {
  if (!confirm('Uninstall "' + name + '"?')) return;
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
  fetch('/api/apps').then(function(r) { return r.json(); }).then(function(apps) {
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
  var q = ((document.getElementById('app-filter') || {}).value || '').toLowerCase();
  var list = appsCache.filter(function(a) {
    if (appTab === 'user' && a.isSystem) return false;
    if (appTab === 'sys' && !a.isSystem) return false;
    return !q || a.name.toLowerCase().indexOf(q) !== -1 || a.packageName.toLowerCase().indexOf(q) !== -1;
  });
  var grid = document.getElementById('apps-grid');
  if (!grid) return;
  if (!list.length) { grid.innerHTML = '<div style="color:var(--text-muted);">No matching apps.</div>'; return; }
  var html = '';
  for (var i = 0; i < list.length; i++) {
    var a = list[i];
    var encPkg = encodeURIComponent(a.packageName);
    var encName = encodeURIComponent(a.name);
    html += '<div style="border:1px solid var(--border-color); border-radius:var(--radius-md); padding:10px 12px; display:flex; align-items:center; justify-content:space-between; gap:8px;">' +
      '<div style="min-width:0;"><div style="font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + esc(a.name) + '</div>' +
      '<div class="mono" style="color:var(--text-muted); font-size:11px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">' + esc(a.packageName) + ' v' + esc(a.versionName) + '</div></div>' +
      '<div class="row"><button class="btn btn-sm btn-primary" onclick="runAction(\'launch_app\',{packageName:decodeURIComponent(\'' + encPkg + '\')})">Open</button>' +
      (!a.isSystem ? '<button class="btn btn-sm btn-danger" onclick="uninstallApp(decodeURIComponent(\'' + encPkg + '\'),decodeURIComponent(\'' + encName + '\'))">Del</button>' : '') +
      '</div></div>';
  }
  grid.innerHTML = html;
}
function loadUsage() {
  fetch('/api/usage').then(function(r) { return r.json(); }).then(function(data) {
    var list = data.items || [];
    var tbody = document.getElementById('usage-tbody');
    if (!tbody) return;
    if (!list.length) { tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">Usage access unavailable.</td></tr>'; return; }
    var html = '';
    for (var i = 0; i < list.length; i++) {
      var u = list[i];
      html += '<tr><td style="font-weight:500;">' + esc(u.appName) + '</td><td class="mono">' + esc(u.packageName) + '</td><td class="mono">' + u.totalMinutes + ' min</td></tr>';
    }
    tbody.innerHTML = html;
  });
}

function onTermKey(ev) {
  var inp = document.getElementById('term-input');
  if (ev.key === 'Enter') { runTermInput(); }
  else if (ev.key === 'ArrowUp' && termHistory.length) {
    ev.preventDefault();
    termHistIdx = termHistIdx < 0 ? termHistory.length - 1 : Math.max(0, termHistIdx - 1);
    inp.value = termHistory[termHistIdx] || '';
  } else if (ev.key === 'ArrowDown' && termHistory.length) {
    ev.preventDefault();
    if (termHistIdx < termHistory.length - 1) { termHistIdx++; inp.value = termHistory[termHistIdx]; }
    else { termHistIdx = -1; inp.value = ''; }
  }
}
function runTermInput() {
  var inp = document.getElementById('term-input');
  var cmd = inp ? inp.value.trim() : '';
  if (cmd) { runTerm(cmd); inp.value = ''; }
}
function runTerm(cmd) {
  if (termHistory[termHistory.length - 1] !== cmd) termHistory.push(cmd);
  termHistIdx = -1;
  var out = document.getElementById('term-out');
  var promptStr = (termCwd || '/') + ' $ ' + cmd;
  out.textContent += '\n' + promptStr + '\n';
  out.scrollTop = out.scrollHeight;
  fetch('/api/shell', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command: cmd, cwd: termCwd })
  }).then(function(r) { return r.json(); }).then(function(res) {
    if (res.cwd) { termCwd = res.cwd; setElText('term-cwd-badge', res.cwd); }
    var body = (res.stdout || '') + (res.stderr ? ('\nERR: ' + res.stderr) : '');
    out.textContent += (body ? body + '\n' : '') + '[exit ' + res.exitCode + ' | ' + res.executionTimeMs + 'ms]\n';
    out.scrollTop = out.scrollHeight;
  });
}
function clearTerm() { setElText('term-out', 'PhoneDeck Shell Ready.'); }

function loadNotifications() {
  fetch('/api/notifications').then(function(r) { return r.json(); }).then(function(list) {
    var box = document.getElementById('notif-list');
    if (!box) return;
    if (!list.length) { box.innerHTML = '<div style="color:var(--text-muted); padding:10px;">No notifications captured.</div>'; return; }
    var html = '';
    for (var i = 0; i < list.length; i++) {
      var n = list[i];
      html += '<div style="border:1px solid var(--border-color); border-left:3px solid var(--accent); border-radius:var(--radius-md); padding:9px 12px; margin-bottom:8px;">' +
        '<div style="display:flex; justify-content:space-between; font-size:11px; color:var(--text-muted);"><span>' + esc(n.appName) + '</span><span>' + new Date(n.timestamp).toLocaleTimeString() + '</span></div>' +
        '<div style="font-weight:600; margin-top:2px;">' + esc(n.title) + '</div><div style="color:var(--text-secondary); font-size:12.5px;">' + esc(n.text) + '</div></div>';
    }
    box.innerHTML = html;
  });
}
function clearNotifs() { runAction('clear_notifications').then(loadNotifications); }

/* SYSTEM: SECURITY, LOCATION, BLUETOOTH, MCP */
function loadLocation() {
  fetch('/api/location').then(function(r) { return r.json(); }).then(function(l) {
    setElText('loc-lat', l.available ? l.latitude.toFixed(6) : '--');
    setElText('loc-lng', l.available ? l.longitude.toFixed(6) : '--');
    setElText('loc-acc', l.available ? (l.accuracyMeters.toFixed(1) + ' m') : '--');
    setElText('loc-prov', l.provider || '--');
    var link = document.getElementById('loc-map-link');
    if (link) {
      if (l.available) { link.href = 'https://maps.google.com/?q=' + l.latitude + ',' + l.longitude; link.style.display = 'inline-flex'; }
      else link.style.display = 'none';
    }
  });
}
function loadBluetooth() {
  fetch('/api/bluetooth').then(function(r) { return r.json(); }).then(function(bt) {
    setElText('bt-state', bt.isSupported ? (bt.isEnabled ? 'Enabled' : 'Disabled') : 'Unavailable');
    setElText('bt-name', bt.adapterName || '--');
    var list = bt.devices || [];
    var tbody = document.getElementById('bt-tbody');
    if (!tbody) return;
    if (!list.length) { tbody.innerHTML = '<tr><td colspan="3" style="color:var(--text-muted);">No paired devices.</td></tr>'; return; }
    var html = '';
    for (var i = 0; i < list.length; i++) {
      var d = list[i];
      html += '<tr><td style="font-weight:500;">' + esc(d.name) + '</td><td class="mono">' + esc(d.address) + '</td><td><span class="tag tag-green">' + esc(d.bondState) + '</span></td></tr>';
    }
    tbody.innerHTML = html;
  });
}
function refreshAuthInfo() {
  fetch('/api/auth/status').then(function(r) { return r.json(); }).then(function(st) {
    if (!st.authenticated) { window.location.replace(homeUrl()); return; }
    setElText('owner-user-id', st.userId || '--');
    setElText('owner-device-id', st.deviceId || '--');
    setElText('owner-remote-status', st.relayConnected && st.publicUrl ? st.publicUrl : (st.relayConnected ? 'Connected' : 'Local Only'));
  }).catch(function() {});
}
function changePassword(ev) {
  ev.preventDefault();
  var cur = document.getElementById('set-cur-pwd').value;
  var np = document.getElementById('set-new-pwd').value;
  var cp = document.getElementById('set-conf-pwd').value;
  var msgBox = document.getElementById('pwd-change-msg');
  var btn = document.getElementById('save-pwd-btn');
  function showMsg(ok, text) {
    msgBox.style.display = 'block';
    msgBox.style.background = ok ? 'var(--green-soft)' : 'var(--rose-soft)';
    msgBox.style.color = ok ? '#065f46' : 'var(--rose)';
    msgBox.style.border = ok ? '1px solid #a7f3d0' : '1px solid #fecdd3';
    msgBox.textContent = text;
  }
  if (!np || np.length < 8) return showMsg(false, 'New password must be 8+ characters');
  if (np !== cp) return showMsg(false, 'Passwords do not match');
  btn.disabled = true;
  fetch('/api/auth/change-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ currentPassword: cur, newPassword: np })
  }).then(function(r) { return r.json().then(function(d) { return { ok: r.ok, data: d }; }); }).then(function(res) {
    btn.disabled = false;
    if (res.ok && res.data && res.data.success) {
      showMsg(true, res.data.message || 'Updated');
      document.getElementById('set-cur-pwd').value = '';
      document.getElementById('set-new-pwd').value = '';
      document.getElementById('set-conf-pwd').value = '';
    } else {
      showMsg(false, (res.data && res.data.message) ? res.data.message : 'Failed');
    }
  }).catch(function() { btn.disabled = false; showMsg(false, 'Request failed'); });
}
function logoutSession(allDevices) {
  fetch('/api/auth/logout', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ allDevices: !!allDevices })
  }).finally(function() { window.location.replace(homeUrl()); });
}

/* MCP AGENT */
var mcpConfigCache = null;
var mcpToolsCache = [];
var mcpTokenVisible = false;
var activeCfgTab = 'http';
var MCP_CAT_DEFS = [
  { id: 'device', title: 'Device & Telemetry', desc: 'Battery, RAM, storage, GPS, Bluetooth, usage' },
  { id: 'controls', title: 'Hardware Controls', desc: 'Torch, volume, vibrate, sound, TTS, clipboard' },
  { id: 'files', title: 'Files & Media', desc: 'Browse, read, write, rename, delete, MediaStore' },
  { id: 'comms', title: 'Contacts, SMS & Calls', desc: 'Contacts, SMS, dialing, calls, calendar' },
  { id: 'apps', title: 'Apps & Live Screen', desc: 'Installed apps, launch, APK install, screen' },
  { id: 'shell', title: 'Shell Terminal', desc: 'Direct Android shell command execution', warn: true }
];
function loadMcpConfig() {
  fetch('/api/mcp/config').then(function(r) { return r.json(); }).then(function(cfg) {
    mcpConfigCache = cfg;
    var enabled = !!cfg.enabled;
    var toggle = document.getElementById('mcp-master-toggle');
    if (toggle) toggle.checked = enabled;
    var pill = document.getElementById('mcp-status-pill');
    if (pill) { pill.className = 'tag ' + (enabled ? 'tag-green' : 'tag-rose'); pill.textContent = enabled ? 'Active' : 'Disabled'; }
    setElText('mcp-tools-pill', (cfg.activeToolsCount || 0) + ' Tools');
    var tb = document.getElementById('mcp-token-box');
    if (tb) tb.textContent = mcpTokenVisible ? (cfg.token || '') : (cfg.maskedToken || '••••••••');
    var ti = document.getElementById('mcp-tunnel-input');
    if (ti && document.activeElement !== ti) ti.value = cfg.tunnelUrl || '';
    renderMcpCategoryGrid(cfg.categories || {});
    renderConfigSnippet();
    populateMcpTestTools();
  }).catch(function() {});
}
function renderMcpCategoryGrid(cats) {
  var grid = document.getElementById('mcp-category-grid');
  if (!grid) return;
  var html = '';
  for (var i = 0; i < MCP_CAT_DEFS.length; i++) {
    var c = MCP_CAT_DEFS[i];
    var checked = cats[c.id] !== false;
    html += '<div style="border:1px solid var(--border-color); border-radius:var(--radius-md); padding:11px;">' +
      '<div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">' +
        '<span style="font-weight:600;">' + esc(c.title) + '</span>' +
        '<input type="checkbox" ' + (checked ? 'checked' : '') + ' onchange="toggleMcpCategory(\'' + c.id + '\', this.checked)" style="width:15px; height:15px; accent-color:var(--accent);">' +
      '</div><div style="font-size:12px; color:var(--text-secondary);">' + esc(c.desc) + '</div>' +
      (c.warn ? '<div style="font-size:11px; color:var(--amber); font-weight:600; margin-top:4px;">High privilege access</div>' : '') + '</div>';
  }
  grid.innerHTML = html;
}
function toggleMcpMaster(checked) {
  fetch('/api/mcp/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ enabled: checked }) }).then(function() { toast('MCP updated'); loadMcpConfig(); });
}
function toggleMcpCategory(cat, checked) {
  var patch = { categories: {} }; patch.categories[cat] = checked;
  fetch('/api/mcp/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(patch) }).then(function() { toast('Updated'); loadMcpConfig(); });
}
function saveMcpTunnelUrl() {
  var url = (document.getElementById('mcp-tunnel-input') || {}).value || '';
  fetch('/api/mcp/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ tunnelUrl: url.trim() }) }).then(function() { toast('Saved'); loadMcpConfig(); });
}
function toggleMcpTokenVisibility() {
  mcpTokenVisible = !mcpTokenVisible;
  setElText('btn-toggle-token', mcpTokenVisible ? 'Hide' : 'Show');
  if (mcpConfigCache) setElText('mcp-token-box', mcpTokenVisible ? (mcpConfigCache.token || '') : (mcpConfigCache.maskedToken || '••••••••'));
}
function copyMcpToken() {
  if (mcpConfigCache && mcpConfigCache.token) navigator.clipboard.writeText(mcpConfigCache.token).then(function() { toast('Token copied'); });
}
function regenerateMcpToken() {
  if (!confirm('Regenerate MCP Bearer Token?')) return;
  fetch('/api/mcp/regenerate-token', { method: 'POST' }).then(function() { toast('Token rotated'); loadMcpConfig(); });
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
  var token = (mcpConfigCache && mcpConfigCache.token) ? mcpConfigCache.token : '<TOKEN>';
  var base = (mcpConfigCache && mcpConfigCache.tunnelUrl) ? mcpConfigCache.tunnelUrl : window.location.origin;
  var code = '';
  if (activeCfgTab === 'http') {
    code = '{\n  "mcpServers": {\n    "phonedeck": {\n      "url": "' + base + '/mcp",\n      "headers": { "Authorization": "Bearer ' + token + '" }\n    }\n  }\n}';
  } else if (activeCfgTab === 'sse') {
    code = '{\n  "mcpServers": {\n    "phonedeck-sse": {\n      "url": "' + base + '/mcp/sse",\n      "headers": { "Authorization": "Bearer ' + token + '" }\n    }\n  }\n}';
  } else if (activeCfgTab === 'claude') {
    code = '{\n  "mcpServers": {\n    "phonedeck": {\n      "command": "npx",\n      "args": ["-y", "mcp-remote", "' + base + '/mcp", "--header", "Authorization: Bearer ' + token + '"]\n    }\n  }\n}';
  } else {
    code = 'curl -X POST "' + base + '/mcp" -H "Authorization: Bearer ' + token + '" -H "Content-Type: application/json" -d \'{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}\'';
  }
  pre.textContent = code;
}
function copyConfigSnippet() {
  var pre = document.getElementById('mcp-config-snippet');
  if (pre) navigator.clipboard.writeText(pre.textContent).then(function() { toast('Snippet copied'); });
}
function populateMcpTestTools() {
  fetch('/api/mcp/tools').then(function(r) { return r.json(); }).then(function(data) {
    mcpToolsCache = data.tools || [];
    var sel = document.getElementById('mcp-test-select');
    if (!sel) return;
    var cur = sel.value;
    sel.innerHTML = '<option value="">Select tool...</option>';
    for (var i = 0; i < mcpToolsCache.length; i++) {
      var t = mcpToolsCache[i];
      var opt = document.createElement('option');
      opt.value = t.name;
      opt.textContent = '[' + t.category + '] ' + t.name;
      sel.appendChild(opt);
    }
    if (cur) sel.value = cur;
  }).catch(function() {});
}
function onSelectTestTool(name) {
  var tool = mcpToolsCache.find(function(t) { return t.name === name; });
  var inp = document.getElementById('mcp-test-args');
  if (!inp) return;
  if (tool && tool.inputSchema && tool.inputSchema.properties) {
    var sample = {};
    for (var k in tool.inputSchema.properties) {
      var p = tool.inputSchema.properties[k];
      sample[k] = p.type === 'integer' ? 50 : (p.type === 'string' ? '' : true);
    }
    inp.value = JSON.stringify(sample);
  } else inp.value = '{}';
}
function testMcpToolsList() {
  fetch('/api/mcp/test', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ method: 'tools/list', params: {} }) })
    .then(function(r) { return r.json(); }).then(function(res) { setElText('mcp-test-result', JSON.stringify(res, null, 2)); });
}
function testMcpPing() {
  fetch('/api/mcp/test', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ method: 'ping', params: {} }) })
    .then(function(r) { return r.json(); }).then(function(res) { setElText('mcp-test-result', JSON.stringify(res, null, 2)); });
}
function runMcpToolTest() {
  var name = (document.getElementById('mcp-test-select') || {}).value || '';
  if (!name) return toast('Select a tool');
  var args = {};
  try { args = JSON.parse((document.getElementById('mcp-test-args') || {}).value || '{}'); }
  catch (e) { return toast('Invalid JSON'); }
  fetch('/api/mcp/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ method: 'tools/call', params: { name: name, arguments: args } })
  }).then(function(r) { return r.json(); }).then(function(res) {
    setElText('mcp-test-result', JSON.stringify(res, null, 2));
  });
}

function setupDropzone(elId, onFiles) {
  var dz = document.getElementById(elId);
  if (!dz) return;
  dz.addEventListener('dragover', function(e) { e.preventDefault(); dz.classList.add('dragover'); });
  dz.addEventListener('dragleave', function() { dz.classList.remove('dragover'); });
  dz.addEventListener('drop', function(e) {
    e.preventDefault();
    dz.classList.remove('dragover');
    if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length) onFiles(e.dataTransfer.files);
  });
}
setupDropzone('file-dropzone', function(files) { uploadFileList(files); });
setupDropzone('apk-dropzone', function(files) { if (files[0]) uploadAndInstallApk(files[0]); });

document.addEventListener('visibilitychange', function() {
  if (!document.hidden) fetchStatus();
});
setInterval(function() { if (!document.hidden) fetchStatus(); }, 5000);

refreshAuthInfo();
fetchStatus();
</script>
</body>
</html>
        """.trimIndent()
    }
}
