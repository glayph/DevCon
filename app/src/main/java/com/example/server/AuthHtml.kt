package com.example.server

object AuthHtml {

    fun get(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>PhoneDeck</title>
<style>
:root {
  --bg: #f8fafc;
  --surface: #ffffff;
  --subtle: #f1f5f9;
  --border: #e2e8f0;
  --border-strong: #cbd5e1;
  --ink: #0f172a;
  --ink-secondary: #475569;
  --ink-muted: #94a3b8;
  --accent: #2563eb;
  --accent-hover: #1d4ed8;
  --ring: rgba(37, 99, 235, 0.14);
  --rose: #e11d48;
  --rose-bg: #fff1f2;
  --rose-border: #fecdd3;
  --ok: #047857;
  --ok-bg: #ecfdf5;
  --ok-border: #a7f3d0;
  --font: -apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", Roboto, sans-serif;
  --mono: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  font-family: var(--font);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  -webkit-font-smoothing: antialiased;
}
.card {
  width: 100%;
  max-width: 368px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 32px 28px 28px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}
.brand-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}
.brand-mark {
  width: 18px;
  height: 18px;
  color: var(--accent);
  flex-shrink: 0;
}
.brand {
  font-size: 13.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--ink-secondary);
}
.title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.025em;
  color: var(--ink);
}
.step-count {
  display: none;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ink-muted);
}
.steps {
  display: flex;
  gap: 6px;
  margin: -8px 0 20px;
}
.step-bar {
  flex: 1;
  height: 2px;
  border-radius: 99px;
  background: var(--border);
  transition: background 0.2s ease;
}
.step-bar.active { background: var(--accent); }
.step-bar.done { background: var(--ok); }
.group {
  margin-bottom: 14px;
}
.label-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
}
.label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
}
.otp-badge {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--ink-secondary);
}
.field {
  position: relative;
  display: flex;
  align-items: center;
}
.input {
  width: 100%;
  height: 42px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--ink);
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.input::placeholder { color: var(--ink-muted); }
.input.pwd { padding-right: 54px; }
.input.otp {
  height: 50px;
  font-family: var(--mono);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.55em;
  text-align: center;
  padding-left: 0.55em;
}
.input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--ring);
}
.toggle {
  position: absolute;
  right: 6px;
  background: transparent;
  border: none;
  color: var(--ink-secondary);
  font-size: 11.5px;
  font-weight: 600;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: pointer;
}
.toggle:hover { color: var(--ink); background: var(--subtle); }
.btn {
  width: 100%;
  height: 42px;
  background: var(--accent);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 6px;
  transition: background 0.15s ease, opacity 0.15s ease;
}
.btn:hover { background: var(--accent-hover); }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.actions-center {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.link {
  background: none;
  border: none;
  color: var(--ink-secondary);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  padding: 2px 0;
  transition: color 0.15s ease;
}
.link:hover { color: var(--accent); }
.link:disabled { opacity: 0.45; cursor: not-allowed; }
.alert {
  display: none;
  border-radius: 8px;
  padding: 9px 11px;
  font-size: 12.5px;
  margin-bottom: 14px;
  line-height: 1.4;
}
.alert.error { background: var(--rose-bg); border: 1px solid var(--rose-border); color: var(--rose); }
.alert.success { background: var(--ok-bg); border: 1px solid var(--ok-border); color: var(--ok); }
.view { display: none; }
.view.active { display: block; }
@media (max-width: 420px) {
  .card { padding: 28px 20px 24px; }
}
</style>
</head>
<body>
<div class="card">
  <div class="brand-row">
    <svg class="brand-mark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <rect x="6" y="2.5" width="12" height="19" rx="2.5"></rect>
      <line x1="10" y1="18.5" x2="14" y2="18.5"></line>
    </svg>
    <span class="brand">PhoneDeck</span>
  </div>

  <div class="title-row">
    <h1 class="title" id="page-title">Sign in</h1>
    <span class="step-count" id="step-count"></span>
  </div>

  <div id="alert-box" class="alert error" role="alert"></div>

  <!-- SIGN UP (FIRST-TIME REGISTRATION) -->
  <div id="view-register" class="view">
    <div class="steps" aria-hidden="true">
      <div class="step-bar active" id="bar-1"></div>
      <div class="step-bar" id="bar-2"></div>
    </div>

    <form id="reg-step-1" onsubmit="startReg(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="reg-userid">User</label>
        </div>
        <div class="field">
          <input type="text" id="reg-userid" class="input" placeholder="username" minlength="3" maxlength="32" autocomplete="username" required>
        </div>
      </div>
      <div class="group">
        <div class="label-row">
          <label class="label" for="reg-pwd">Password</label>
        </div>
        <div class="field">
          <input type="password" id="reg-pwd" class="input pwd" placeholder="8+ characters" minlength="8" autocomplete="new-password" required>
          <button type="button" class="toggle" onclick="toggleInput('reg-pwd', this)">Show</button>
        </div>
      </div>
      <div class="group">
        <div class="label-row">
          <label class="label" for="reg-pwd2">Confirm Password</label>
        </div>
        <div class="field">
          <input type="password" id="reg-pwd2" class="input pwd" placeholder="Repeat password" minlength="8" autocomplete="new-password" required>
          <button type="button" class="toggle" onclick="toggleInput('reg-pwd2', this)">Show</button>
        </div>
      </div>
      <button type="submit" class="btn" id="reg-next-btn">Next</button>
    </form>

    <form id="reg-step-2" style="display:none;" onsubmit="verifyRegOtp(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="reg-otp">OTP</label>
          <span class="otp-badge">Sent to phone</span>
        </div>
        <div class="field">
          <input type="text" id="reg-otp" class="input otp" inputmode="numeric" pattern="\d{4}" maxlength="4" placeholder="••••" autocomplete="one-time-code" oninput="cleanDigits(this)" required>
        </div>
      </div>
      <button type="submit" class="btn" id="reg-verify-btn">Confirm</button>
      <div class="actions-row">
        <button type="button" class="link" onclick="goRegStep(1)">Back</button>
        <button type="button" class="link" id="reg-resend-btn" onclick="resendRegOtp()">Resend</button>
      </div>
    </form>
    <div class="actions-center">
      <button type="button" class="link" onclick="showLoginView()">Already have an account? Sign in</button>
    </div>
  </div>

  <!-- SIGN IN -->
  <div id="view-login" class="view active">
    <form id="login-form" onsubmit="submitLogin(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="login-userid">User</label>
        </div>
        <div class="field">
          <input type="text" id="login-userid" class="input" autocomplete="username" required>
        </div>
      </div>
      <div class="group">
        <div class="label-row">
          <label class="label" for="login-pwd">Password</label>
        </div>
        <div class="field">
          <input type="password" id="login-pwd" class="input pwd" autocomplete="current-password" required>
          <button type="button" class="toggle" onclick="toggleInput('login-pwd', this)">Show</button>
        </div>
      </div>
      <button type="submit" class="btn" id="login-btn">Login</button>
    </form>
    <div class="actions-center">
      <button type="button" class="link" onclick="showRecoveryView()">Forgot password</button>
    </div>
    <div class="actions-center">
      <button type="button" class="link" id="create-account-link" onclick="showRegisterView()">Create an account</button>
    </div>
  </div>

  <!-- PASSWORD RESET -->
  <div id="view-recover" class="view">
    <form id="rec-step-1" onsubmit="startRecovery(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="rec-userid">User</label>
        </div>
        <div class="field">
          <input type="text" id="rec-userid" class="input" autocomplete="username" required>
        </div>
      </div>
      <button type="submit" class="btn" id="rec-send-btn">Continue</button>
      <div class="actions-center">
        <button type="button" class="link" onclick="showLoginView()">Back to Sign In</button>
      </div>
    </form>

    <form id="rec-step-2" style="display:none;" onsubmit="verifyRecOtp(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="rec-otp">OTP</label>
          <span class="otp-badge">Sent to phone</span>
        </div>
        <div class="field">
          <input type="text" id="rec-otp" class="input otp" inputmode="numeric" pattern="\d{4}" maxlength="4" placeholder="••••" autocomplete="one-time-code" oninput="cleanDigits(this)" required>
        </div>
      </div>
      <button type="submit" class="btn" id="rec-verify-btn">Verify</button>
      <div class="actions-center">
        <button type="button" class="link" onclick="showLoginView()">Cancel</button>
      </div>
    </form>

    <form id="rec-step-3" style="display:none;" onsubmit="completeRecReset(event)">
      <div class="group">
        <div class="label-row">
          <label class="label" for="rec-pwd">New Password</label>
        </div>
        <div class="field">
          <input type="password" id="rec-pwd" class="input pwd" placeholder="8+ characters" minlength="8" autocomplete="new-password" required>
          <button type="button" class="toggle" onclick="toggleInput('rec-pwd', this)">Show</button>
        </div>
      </div>
      <div class="group">
        <div class="label-row">
          <label class="label" for="rec-pwd2">Confirm Password</label>
        </div>
        <div class="field">
          <input type="password" id="rec-pwd2" class="input pwd" placeholder="Repeat password" minlength="8" autocomplete="new-password" required>
          <button type="button" class="toggle" onclick="toggleInput('rec-pwd2', this)">Show</button>
        </div>
      </div>
      <button type="submit" class="btn" id="rec-reset-btn">Update Password</button>
    </form>
  </div>
</div>

<script>
var PD_BASE = (function() {
  var m = window.location.pathname.match(/^(\/d\/[^\/]+)/);
  return m ? m[1] : '';
})();
function apiUrl(p) { return PD_BASE + p; }
function homeUrl() { return (PD_BASE || '') + '/'; }

var regPendingId = '';
var recId = '';
var recTicket = '';
var resendTimer = null;
var accountRegistered = false;

function cleanDigits(el) {
  el.value = (el.value || '').replace(/\D+/g, '').slice(0, 4);
}

function setAlert(type, msg) {
  var box = document.getElementById('alert-box');
  if (!box) return;
  if (!msg) {
    box.style.display = 'none';
    box.textContent = '';
    return;
  }
  box.className = 'alert ' + (type || 'error');
  box.textContent = msg;
  box.style.display = 'block';
}

function toggleInput(id, btn) {
  var inp = document.getElementById(id);
  if (!inp) return;
  if (inp.type === 'password') {
    inp.type = 'text';
    btn.textContent = 'Hide';
  } else {
    inp.type = 'password';
    btn.textContent = 'Show';
  }
}

function setActiveView(viewId) {
  var views = document.querySelectorAll('.view');
  for (var i = 0; i < views.length; i++) views[i].classList.remove('active');
  var target = document.getElementById(viewId);
  if (target) target.classList.add('active');
}

function showLoginView() {
  setAlert(null, '');
  document.getElementById('page-title').textContent = 'Sign in';
  document.getElementById('step-count').style.display = 'none';
  setActiveView('view-login');
  var uid = document.getElementById('login-userid');
  if (uid) uid.focus();
}

function showRegisterView() {
  if (accountRegistered) {
    showLoginView();
    setAlert('error', 'This device already has an account. Please sign in.');
    return;
  }
  setAlert(null, '');
  document.getElementById('page-title').textContent = 'Sign up';
  document.getElementById('step-count').style.display = 'inline';
  setActiveView('view-register');
  goRegStep(1);
}

function showRecoveryView() {
  setAlert(null, '');
  document.getElementById('page-title').textContent = 'Reset password';
  document.getElementById('step-count').style.display = 'none';
  setActiveView('view-recover');
  document.getElementById('rec-step-1').style.display = 'block';
  document.getElementById('rec-step-2').style.display = 'none';
  document.getElementById('rec-step-3').style.display = 'none';
}

function goRegStep(step) {
  setAlert(null, '');
  var sc = document.getElementById('step-count');
  if (sc) sc.textContent = step + ' / 2';
  for (var i = 1; i <= 2; i++) {
    var form = document.getElementById('reg-step-' + i);
    var bar = document.getElementById('bar-' + i);
    if (form) form.style.display = (i === step) ? 'block' : 'none';
    if (bar) {
      bar.className = 'step-bar' + (i < step ? ' done' : (i === step ? ' active' : ''));
    }
  }
  if (step === 1) {
    var u = document.getElementById('reg-userid');
    if (u) u.focus();
  } else {
    var otp = document.getElementById('reg-otp');
    if (otp) otp.focus();
  }
}

function startResendCooldown(sec) {
  var btn = document.getElementById('reg-resend-btn');
  if (!btn) return;
  if (resendTimer) clearInterval(resendTimer);
  var left = sec || 60;
  btn.disabled = true;
  btn.textContent = 'Resend (' + left + 's)';
  resendTimer = setInterval(function() {
    left--;
    if (left <= 0) {
      clearInterval(resendTimer);
      resendTimer = null;
      btn.disabled = false;
      btn.textContent = 'Resend';
    } else {
      btn.textContent = 'Resend (' + left + 's)';
    }
  }, 1000);
}

function postJson(path, payload) {
  return fetch(apiUrl(path), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify(payload || {})
  }).then(function(r) {
    return r.json().then(function(d) { return { ok: r.ok, status: r.status, data: d }; });
  });
}

function startReg(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var userId = document.getElementById('reg-userid').value.trim();
  var pwd = document.getElementById('reg-pwd').value;
  var pwd2 = document.getElementById('reg-pwd2').value;
  if (pwd !== pwd2) {
    setAlert('error', 'Passwords do not match.');
    return;
  }
  var btn = document.getElementById('reg-next-btn');
  btn.disabled = true;
  btn.textContent = 'Sending...';
  postJson('/api/auth/register/start', { userId: userId, password: pwd, confirmPassword: pwd2 })
    .then(function(res) {
      btn.disabled = false;
      btn.textContent = 'Next';
      if (res.ok && res.data && res.data.success) {
        regPendingId = res.data.pendingId || '';
        document.getElementById('reg-otp').value = '';
        goRegStep(2);
        startResendCooldown(60);
      } else {
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Registration error.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Continue';
      setAlert('error', 'Connection error.');
    });
}

function resendRegOtp() {
  if (!regPendingId) return goRegStep(1);
  setAlert(null, '');
  var btn = document.getElementById('reg-resend-btn');
  btn.disabled = true;
  postJson('/api/auth/register/resend-otp', { pendingId: regPendingId })
    .then(function(res) {
      if (res.ok && res.data && res.data.success) {
        startResendCooldown(60);
      } else {
        btn.disabled = false;
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Unable to resend.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      setAlert('error', 'Connection error.');
    });
}

function verifyRegOtp(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var code = document.getElementById('reg-otp').value.trim();
  var btn = document.getElementById('reg-verify-btn');
  btn.disabled = true;
  btn.textContent = 'Confirming...';
  postJson('/api/auth/register/verify-otp', { pendingId: regPendingId, code: code })
    .then(function(res) {
      if (res.ok && res.data && res.data.success) {
        window.location.replace(homeUrl());
      } else {
        btn.disabled = false;
        btn.textContent = 'Confirm';
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Invalid code.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Confirm';
      setAlert('error', 'Connection error.');
    });
}

function submitLogin(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var userId = document.getElementById('login-userid').value.trim();
  var pwd = document.getElementById('login-pwd').value;
  var btn = document.getElementById('login-btn');
  btn.disabled = true;
  btn.textContent = 'Signing in...';
  postJson('/api/auth/login', { userId: userId, password: pwd })
    .then(function(res) {
      if (res.ok && res.data && res.data.success) {
        window.location.replace(homeUrl());
      } else {
        btn.disabled = false;
        btn.textContent = 'Login';
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Invalid credentials.');
        document.getElementById('login-pwd').select();
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Login';
      setAlert('error', 'Could not reach PhoneDeck. Check the connection and try again.');
    });
}

function startRecovery(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var userId = document.getElementById('rec-userid').value.trim();
  var btn = document.getElementById('rec-send-btn');
  btn.disabled = true;
  btn.textContent = 'Sending...';
  postJson('/api/auth/recover/start', { userId: userId })
    .then(function(res) {
      btn.disabled = false;
      btn.textContent = 'Continue';
      if (res.ok && res.data && res.data.success) {
        recId = res.data.recoveryId || '';
        document.getElementById('rec-step-1').style.display = 'none';
        document.getElementById('rec-step-2').style.display = 'block';
        document.getElementById('rec-otp').focus();
      } else {
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Verification failed.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Continue';
      setAlert('error', 'Connection error.');
    });
}

function verifyRecOtp(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var code = document.getElementById('rec-otp').value.trim();
  var btn = document.getElementById('rec-verify-btn');
  btn.disabled = true;
  btn.textContent = 'Verifying...';
  postJson('/api/auth/recover/verify-otp', { recoveryId: recId, code: code })
    .then(function(res) {
      btn.disabled = false;
      btn.textContent = 'Verify';
      if (res.ok && res.data && res.data.success) {
        recTicket = res.data.recoveryTicket || '';
        document.getElementById('rec-step-2').style.display = 'none';
        document.getElementById('rec-step-3').style.display = 'block';
        document.getElementById('rec-pwd').focus();
      } else {
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Invalid code.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Verify';
      setAlert('error', 'Connection error.');
    });
}

function completeRecReset(ev) {
  ev.preventDefault();
  setAlert(null, '');
  var pwd = document.getElementById('rec-pwd').value;
  var pwd2 = document.getElementById('rec-pwd2').value;
  if (pwd !== pwd2) {
    setAlert('error', 'Passwords do not match.');
    return;
  }
  var btn = document.getElementById('rec-reset-btn');
  btn.disabled = true;
  btn.textContent = 'Updating...';
  postJson('/api/auth/recover/reset-password', { recoveryTicket: recTicket, newPassword: pwd })
    .then(function(res) {
      if (res.ok && res.data && res.data.success) {
        window.location.replace(homeUrl());
      } else {
        btn.disabled = false;
        btn.textContent = 'Update Password';
        setAlert('error', (res.data && res.data.message) ? res.data.message : 'Reset failed.');
      }
    })
    .catch(function() {
      btn.disabled = false;
      btn.textContent = 'Update Password';
      setAlert('error', 'Connection error.');
    });
}

(function initAuth() {
  fetch(apiUrl('/api/auth/status'), { credentials: 'same-origin' })
    .then(function(r) { return r.json(); })
    .then(function(st) {
      accountRegistered = !!st.registered;
      var createAccountLink = document.getElementById('create-account-link');
      if (createAccountLink) createAccountLink.style.display = accountRegistered ? 'none' : '';
      if (st.authenticated) {
        window.location.replace(homeUrl());
        return;
      }
      if (st.registered) {
        showLoginView();
      } else {
        showRegisterView();
      }
    })
    .catch(function() {
      showLoginView();
      setAlert('error', 'Could not check account status. Sign in or create an account.');
    });
})();
</script>
</body>
</html>
        """.trimIndent()
    }
}
