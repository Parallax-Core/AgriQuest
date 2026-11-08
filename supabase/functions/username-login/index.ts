// Username resolution stays server-side. Never log passwords, emails or sessions.
const reply = (status: number, code: string) => Response.json({ code }, {
  status, headers: { "Cache-Control": "no-store" },
});
Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return reply(405, "method_not_allowed");
  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const publicKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !serviceKey || !publicKey) return reply(503, "unavailable");
  // This is a public password-login endpoint; public API keys are not user identity.
  // Supabase Auth verifies the password below, using the server-configured public key.
  try {
    const raw = await request.text();
    if (raw.length > 8192) return reply(400, "invalid_credentials");
    const data = JSON.parse(raw);
    if (typeof data.username !== "string" || typeof data.password !== "string" ||
        !data.username.trim() || data.username.trim().length > 128 ||
        !data.password || data.password.length > 4096) return reply(400, "invalid_credentials");
    const resolved = await fetch(url + "/rest/v1/rpc/resolve_login_username", {
      method: "POST", headers: { apikey: serviceKey, Authorization: "Bearer " + serviceKey,
        "Content-Type": "application/json" },
      body: JSON.stringify({ p_username: data.username.trim() }), signal: AbortSignal.timeout(5000),
    });
    if (!resolved.ok) return reply(503, "unavailable");
    const email = await resolved.json();
    // Always use the normal Auth password flow, also for unknown usernames.
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(data.username.trim().toLowerCase()));
    const unknown = Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2,"0")).join("");
    const auth = await fetch(url + "/auth/v1/token?grant_type=password", {
      method: "POST", headers: { apikey: publicKey, "Content-Type": "application/json" },
      body: JSON.stringify({ email: typeof email === "string" ? email : unknown + "@username-login.invalid",
        password: data.password }), signal: AbortSignal.timeout(8000),
    });
    const session = await auth.json();
    if (!auth.ok) {
      if (auth.status === 429) return reply(429, "over_request_rate_limit");
      if (auth.status >= 500) return reply(503, "unavailable");
      if (typeof email === "string" && session.error_code === "email_not_confirmed") return reply(400, "email_not_confirmed");
      return reply(400, "invalid_credentials");
    }
    if (typeof email !== "string" || typeof session.access_token !== "string" ||
        !session.user?.email_confirmed_at || session.user?.is_anonymous === true) {
      return reply(400, "invalid_credentials");
    }
    return Response.json({ access_token: session.access_token, expires_in: session.expires_in }, {
      headers: { "Cache-Control": "no-store" },
    });
  } catch {
    return reply(503, "unavailable");
  }
});
