/**
 * PhoneDeck Stateless WebSocket Relay (Cloudflare Workers + Durable Objects)
 *
 * Security & Hosting Model:
 * - Android remains the authoritative HTTP server, auth verifier, and dashboard host.
 * - This Worker ONLY terminates public HTTPS/TLS and forwards HTTP envelopes over WSS
 *   to the specific Android device matching `deviceId`.
 * - Each `deviceId` gets an isolated Durable Object bound on first connect to a SHA-256 hash
 *   of the device's 256-bit `deviceTunnelSecret`. Other devices cannot hijack another ID.
 */

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    // 1. Android Device Outbound WebSocket Tunnel Connect: /ws/device?deviceId=<id>
    if (path === "/ws/device") {
      const deviceId = (url.searchParams.get("deviceId") || request.headers.get("X-Device-Id") || "").trim();
      if (!/^pd_[a-f0-9]{12,40}$/i.test(deviceId)) {
        return new Response("Invalid deviceId", { status: 400 });
      }
      const id = env.PHONEDECK_TUNNEL.idFromName(deviceId);
      const stub = env.PHONEDECK_TUNNEL.get(id);
      return stub.fetch(request);
    }

    // 2. Desktop Browser Public HTTPS Access: /d/<deviceId>/*
    const match = path.match(/^\/d\/(pd_[a-f0-9]{12,40})(\/.*)?$/i);
    if (match) {
      const deviceId = match[1];
      const subPath = match[2];
      if (!subPath) {
        return Response.redirect(`${url.origin}/d/${deviceId}/${url.search}`, 302);
      }
      const id = env.PHONEDECK_TUNNEL.idFromName(deviceId);
      const stub = env.PHONEDECK_TUNNEL.get(id);
      return stub.fetch(request);
    }

    if (path === "/" || path === "/health") {
      return new Response(
        JSON.stringify({ service: "PhoneDeck Relay", status: "ready" }),
        { status: 200, headers: { "Content-Type": "application/json; charset=utf-8" } }
      );
    }

    return new Response("Not Found", { status: 404 });
  }
};

export class DeviceTunnelDO {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.deviceSocket = null;
    this.pending = new Map();
    this.streams = new Map();
  }

  async fetch(request) {
    const url = new URL(request.url);

    // Handle Android Device WebSocket Connection
    if (url.pathname === "/ws/device") {
      if (request.headers.get("Upgrade") !== "websocket") {
        return new Response("Expected WebSocket Upgrade", { status: 426 });
      }
      const deviceId = (url.searchParams.get("deviceId") || request.headers.get("X-Device-Id") || "").trim();
      const deviceSecret = (request.headers.get("X-Device-Secret") || "").trim();
      if (!deviceId || deviceSecret.length < 32) {
        return new Response("Missing device credentials", { status: 401 });
      }

      const providedHash = await sha256Hex(deviceSecret);
      const storedHash = await this.state.storage.get("secret_hash");
      if (storedHash && storedHash !== providedHash) {
        return new Response("Forbidden: deviceId already bound to another key", { status: 403 });
      }
      if (!storedHash) {
        await this.state.storage.put("secret_hash", providedHash);
      }

      const pair = new WebSocketPair();
      const client = pair[0];
      const server = pair[1];
      server.accept();

      if (this.deviceSocket) {
        try {
          this.deviceSocket.close(1000, "Replaced by newer device session");
        } catch (_) {}
      }
      this.deviceSocket = server;
      const publicUrl = `${url.origin}/d/${deviceId}/`;

      server.addEventListener("message", (event) => {
        this.onDeviceMessage(server, event.data, publicUrl);
      });
      server.addEventListener("close", () => {
        if (this.deviceSocket === server) {
          this.deviceSocket = null;
          this.failAllPending("Device disconnected");
        }
      });
      server.addEventListener("error", () => {
        if (this.deviceSocket === server) {
          this.deviceSocket = null;
          this.failAllPending("Tunnel socket error");
        }
      });

      // Immediately acknowledge tunnel readiness
      server.send(JSON.stringify({ type: "tunnel_ready", deviceId, publicUrl }));
      return new Response(null, { status: 101, webSocket: client });
    }

    // Handle Incoming Desktop HTTPS Request -> Forward to Android Device
    if (!this.deviceSocket) {
      return new Response(offlineHtml(), {
        status: 503,
        headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" }
      });
    }

    const reqId = crypto.randomUUID();
    const prefixMatch = url.pathname.match(/^\/d\/[^/]+(\/.*)$/);
    const targetPath = prefixMatch ? prefixMatch[1] : "/";
    const query = url.search ? url.search.substring(1) : "";

    const headersObj = {};
    for (const [k, v] of request.headers.entries()) {
      headersObj[k.toLowerCase()] = v;
    }
    headersObj["x-forwarded-proto"] = "https";
    headersObj["x-phonedeck-tunnel"] = "1";

    let bodyBase64 = "";
    if (request.method !== "GET" && request.method !== "HEAD") {
      const buf = await request.arrayBuffer();
      if (buf.byteLength > 0) {
        bodyBase64 = arrayBufferToBase64(buf);
      }
    }

    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        if (this.pending.has(reqId)) {
          this.pending.delete(reqId);
          resolve(new Response("Gateway Timeout from Android Host", { status: 504 }));
        }
      }, 45000);

      this.pending.set(reqId, { resolve, timeout });

      try {
        this.deviceSocket.send(JSON.stringify({
          type: "http_req",
          id: reqId,
          method: request.method,
          path: targetPath,
          query,
          headers: headersObj,
          bodyBase64
        }));
      } catch (err) {
        clearTimeout(timeout);
        this.pending.delete(reqId);
        resolve(new Response("Tunnel write failed", { status: 502 }));
      }
    });
  }

  onDeviceMessage(ws, raw, publicUrl) {
    let msg;
    try {
      msg = JSON.parse(typeof raw === "string" ? raw : new TextDecoder().decode(raw));
    } catch (_) {
      return;
    }

    if (msg.type === "device_hello") {
      ws.send(JSON.stringify({ type: "tunnel_ready", publicUrl }));
      return;
    }

    const reqId = msg.id;
    if (!reqId) return;

    if (msg.type === "http_res") {
      const entry = this.pending.get(reqId);
      if (!entry) return;
      clearTimeout(entry.timeout);
      this.pending.delete(reqId);

      const headers = buildResponseHeaders(msg.headers);
      const bytes = msg.bodyBase64 ? base64ToUint8Array(msg.bodyBase64) : new Uint8Array(0);
      entry.resolve(new Response(bytes, { status: msg.status || 200, headers }));
      return;
    }

    if (msg.type === "http_res_start") {
      const entry = this.pending.get(reqId);
      if (!entry) return;
      clearTimeout(entry.timeout);
      this.pending.delete(reqId);

      const headers = buildResponseHeaders(msg.headers);
      const readable = new ReadableStream({
        start: (controller) => {
          this.streams.set(reqId, controller);
        },
        cancel: () => {
          this.streams.delete(reqId);
          try {
            if (this.deviceSocket) {
              this.deviceSocket.send(JSON.stringify({ type: "http_cancel", id: reqId }));
            }
          } catch (_) {}
        }
      });
      entry.resolve(new Response(readable, { status: msg.status || 200, headers }));
      return;
    }

    if (msg.type === "http_res_chunk") {
      const controller = this.streams.get(reqId);
      if (controller && msg.chunkBase64) {
        try {
          controller.enqueue(base64ToUint8Array(msg.chunkBase64));
        } catch (_) {
          this.streams.delete(reqId);
        }
      }
      return;
    }

    if (msg.type === "http_res_end") {
      const controller = this.streams.get(reqId);
      if (controller) {
        try { controller.close(); } catch (_) {}
        this.streams.delete(reqId);
      }
    }
  }

  failAllPending(reason) {
    for (const [reqId, entry] of this.pending.entries()) {
      clearTimeout(entry.timeout);
      entry.resolve(new Response(reason, { status: 503 }));
      this.pending.delete(reqId);
    }
    for (const [reqId, controller] of this.streams.entries()) {
      try { controller.close(); } catch (_) {}
      this.streams.delete(reqId);
    }
  }
}

function buildResponseHeaders(rawHeaders) {
  const headers = new Headers();
  if (rawHeaders && typeof rawHeaders === "object") {
    for (const [k, v] of Object.entries(rawHeaders)) {
      if (k.toLowerCase() === "set-cookie" && String(v).includes("\n")) {
        for (const cookieLine of String(v).split("\n")) {
          headers.append("Set-Cookie", cookieLine);
        }
      } else {
        headers.set(k, String(v));
      }
    }
  }
  return headers;
}

async function sha256Hex(str) {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(str));
  return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function arrayBufferToBase64(buf) {
  const bytes = new Uint8Array(buf);
  let bin = "";
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    bin += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(bin);
}

function base64ToUint8Array(b64) {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i);
  }
  return bytes;
}

function offlineHtml() {
  return `<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>PhoneDeck Offline</title></head><body style="font-family:system-ui,sans-serif;background:#f8fafc;color:#0f172a;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:20px;"><div style="background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:28px;max-width:400px;text-align:center;"><h2 style="margin:0 0 8px;">Android Device Offline</h2><p style="color:#475569;font-size:14px;margin:0;">Ensure PhoneDeck is running on the Android device with Remote Access enabled.</p></div></body></html>`;
}
