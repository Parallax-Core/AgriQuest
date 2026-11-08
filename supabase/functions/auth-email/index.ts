import { Webhook } from "npm:standardwebhooks@1.0.0";

type AuthEmail = {
  user: { id: string; email: string };
  email_data: { token: string; email_action_type: string };
};

function required(name: string): string {
  const value = Deno.env.get(name)?.trim();
  if (!value) {
    console.error(JSON.stringify({ stage: "configuration", missing: name }));
    throw new Error("Missing server configuration");
  }
  return value;
}

function failure(status: number, message: string): Response {
  return Response.json({ error: { http_code: status, message } }, { status });
}

Deno.serve(async (request) => {
  const startedAt = Date.now();
  let stage = "signature";
  if (request.method !== "POST") return failure(405, "Method not allowed");
  // JWT verification is disabled only because Supabase Auth signs these HTTP hooks.
  // Verify the signature and timestamp before reading recipients or sending email.
  let event: AuthEmail;
  try {
    const secret = required("SEND_EMAIL_HOOK_SECRET").replace(/^v1,whsec_/, "");
    event = new Webhook(secret).verify(await request.text(),
      Object.fromEntries(request.headers)) as AuthEmail;
  } catch {
    console.error(JSON.stringify({ stage, error: "signature_or_hook_secret_invalid" }));
    return failure(401, "Invalid hook signature");
  }

  try {
    const action = event.email_data.email_action_type;
    if (action !== "signup" && action !== "recovery") {
      // Do not silently deliver an inappropriate token for unsupported account actions.
      return failure(400, "This email action is not configured");
    }
    if (!event.user.id || !event.user.email || !event.email_data.token) {
      return failure(400, "Invalid email event");
    }
    // Read provider configuration only after verifying the signed Auth request.
    const apiKey = required("BREVO_API_KEY");
    const senderEmail = required("AUTH_SENDER_EMAIL");
    const senderName = Deno.env.get("AUTH_SENDER_NAME") || "AgriQuest";
    const subject = action === "recovery" ? "Reset your AgriQuest password" : "Confirm your AgriQuest email";
    const text = action === "recovery"
      ? `Your AgriQuest password reset code is: ${event.email_data.token}\n\nEnter it in the app to choose a new password. If you did not request this, ignore this email.`
      : `Your AgriQuest email confirmation code is: ${event.email_data.token}\n\nEnter it in the app, then log in with your email and password.`;
    stage = "brevo_delivery";
    const sent = await fetch("https://api.brevo.com/v3/smtp/email", {
      method: "POST",
      headers: { "api-key": apiKey, "Content-Type": "application/json", accept: "application/json" },
      body: JSON.stringify({ sender: { email: senderEmail, name: senderName },
        to: [{ email: event.user.email }], subject, textContent: text }),
      signal: AbortSignal.timeout(Math.max(1, Math.min(2500, 4000 - (Date.now() - startedAt)))),
    });
    // Never log the hook payload, tokens, provider response, or credentials.
    if (!sent.ok) {
      const providerError = await sent.json().catch(() => ({}));
      const knownCodes = ["unauthorized", "permission_denied", "invalid_parameter", "missing_parameter",
        "not_enough_credits", "account_under_validation", "too_many_requests", "method_not_allowed"];
      const detail = String(providerError.message ?? "");
      const reason = /unrecogni[sz]ed.*ip|ip address|unauthori[sz]ed.*ip/i.test(detail) ? "ip_restriction"
        : /key.*not found|invalid.*key|key.*invalid/i.test(detail) ? "invalid_api_key" : "provider_rejection";
      console.error(JSON.stringify({ stage, status: sent.status, reason,
        code: knownCodes.includes(providerError.code) ? providerError.code : "provider_error" }));
      return failure(502, "Email delivery unavailable. Please try again later.");
    }
    console.info(JSON.stringify({ stage, status: "accepted", action, elapsedMs: Date.now() - startedAt }));
    return Response.json({});
  } catch {
    console.error(JSON.stringify({ stage, error: "request_failed_or_timed_out", elapsedMs: Date.now() - startedAt }));
    return failure(503, "Email service unavailable");
  }
});
