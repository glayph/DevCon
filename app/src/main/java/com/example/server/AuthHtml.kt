package com.example.server

object AuthHtml {

    fun get(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PhoneDeck &bull; Sign In</title>
<style>
:root {
  --bg-page: #f8fafc;
  --bg-surface: #ffffff;
  --bg-subtle: #f1f5f9;
  --border-color: #e2e8f0;
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-muted: #94a3b8;
  --accent: #2563eb;
  --accent-soft: #eff6ff;
  --accent-hover: #1d4ed8;
  --rose: #e11d48;
  --rose-soft: #fff1f2;
  --font-sans: -apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", Roboto, sans-serif;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  min-height: 100vh;
  background: var(--bg-page);
  color: var(--text-primary);
  font-family: var(--font-sans);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  -webkit-font-smoothing: antialiased;
}
.auth-card {
  width: 100%;
  max-width: 390px;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 28px 26px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}
.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}
.lock-badge {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: var(--accent-soft);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}
.brand-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.brand-sub {
  font-size: 12.5px;
  color: var(--text-secondary);
}
.field-label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}
.input {
  width: 100%;
  background: var(--bg-page);
  border: 1px solid var(--border-color);
  border-radius: 9px;
  padding: 11px 64px 11px 13px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
  background: var(--bg-surface);
}
.toggle-btn {
  position: absolute;
  right: 8px;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.toggle-btn:hover { background: var(--bg-subtle); color: var(--text-primary); }
.submit-btn {
  width: 100%;
  background: var(--accent);
  color: #ffffff;
  border: none;
  border-radius: 9px;
  padding: 11px 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.submit-btn:hover { background: var(--accent-hover); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.err-box {
  display: none;
  background: var(--rose-soft);
  border: 1px solid #fecdd3;
  color: var(--rose);
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 12.5px;
  margin-bottom: 14px;
}
.foot-note {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
</head>
<body>
<div class="auth-card">
  <div class="brand-row">
    <div class="lock-badge">🔒</div>
    <div>
      <div class="brand-title">PhoneDeck Console</div>
      <div class="brand-sub">Authentication required to access device</div>
    </div>
  </div>

  <div id="err-box" class="err-box"></div>

  <form id="login-form" onsubmit="submitLogin(event)">
    <label class="field-label" for="pwd-input">Access Password</label>
    <div class="input-wrap">
      <input type="password" id="pwd-input" class="input" placeholder="Enter password..." autocomplete="current-password" autofocus required>
      <button type="button" class="toggle-btn" id="toggle-pwd" onclick="togglePwd()">Show</button>
    </div>
    <button type="submit" class="submit-btn" id="login-btn">Unlock Console</button>
  </form>

  <div class="foot-note">Session cookie is saved in this browser automatically.</div>
</div>

<script>
function togglePwd() {
  var inp = document.getElementById('pwd-input');
  var btn = document.getElementById('toggle-pwd');
  if (!inp) return;
  if (inp.type === 'password') {
    inp.type = 'text';
    btn.textContent = 'Hide';
  } else {
    inp.type = 'password';
    btn.textContent = 'Show';
  }
}

function submitLogin(ev) {
  ev.preventDefault();
  var pwd = document.getElementById('pwd-input').value;
  var err = document.getElementById('err-box');
  var btn = document.getElementById('login-btn');
  err.style.display = 'none';
  btn.disabled = true;
  btn.textContent = 'Verifying...';

  fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ password: pwd })
  })
  .then(function(r) { return r.json().then(function(d) { return { ok: r.ok, data: d }; }); })
  .then(function(res) {
    if (res.ok && res.data && res.data.success) {
      window.location.replace('/');
    } else {
      err.textContent = (res.data && res.data.message) ? res.data.message : 'Incorrect password';
      err.style.display = 'block';
      btn.disabled = false;
      btn.textContent = 'Unlock Console';
      document.getElementById('pwd-input').select();
    }
  })
  .catch(function() {
    err.textContent = 'Connection error. Make sure PhoneDeck is running.';
    err.style.display = 'block';
    btn.disabled = false;
    btn.textContent = 'Unlock Console';
  });
}
</script>
</body>
</html>
        """.trimIndent()
    }
}
