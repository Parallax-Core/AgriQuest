// assess-leaf: AI leaf assessment (paper FR-10 to FR-12).
//
//   app --photo--> this function --photo--> EfficientNet-B0 model on Render
//                       |--leaf condition only--> Gemini (care guidance, FR-11)
//                       '--result--> public.leaf_scans (the player's AI scan history)
//
// GET  = wake the Render model (its free plan sleeps after 15 idle minutes).
// POST = multipart form: "file" (JPEG leaf photo) and "plant_id".
//
// Secrets (Supabase > Edge Functions > Secrets): AI_SERVER_URL, AI_API_KEY (Render model key, optional),
// GEMINI_API_KEY, and optionally GEMINI_MODEL (default gemini-3.5-flash-lite). They never reach the app.
// Never log photos, tokens or keys.

const GEMINI_MODEL_DEFAULT = "gemini-3.5-flash-lite";
const MAX_PHOTO_BYTES = 6 * 1024 * 1024;
const REQUIRED_LOGIN_DAYS = 7; // same rule as the app (AiUnlock.REQUIRED_LOGIN_DAYS)

// Paper, "Choice of generative model": leaf condition only, short practical actions, no chemical
// products, doses or medical claims, a fixed format of three to four actions.
const GUIDANCE_RULES = `You give care guidance for a home-grown tomato plant in a gardening game.
You receive only the result of an image classifier for one leaf. Reply with 3 or 4 short, practical care actions.
Rules:
- Each action is one plain sentence of at most 20 words, written for a beginner.
- For a healthy leaf, suggest practices that keep the plant healthy.
- For a possible disease or pest, suggest actions such as monitoring the affected area, removing the affected
  leaf or plant part when appropriate, improving airflow or watering at the soil, and seeking further
  assessment (for example from an agriculturist) if it spreads.
- Never name or recommend chemical products, pesticides, fungicides or doses. Make no medical or health claims.
- The classifier can be wrong: describe the condition as "possible", never as a confirmed diagnosis.
- If the classifier was not confident, make the first action: retake a clear, well-lit photo of a single leaf.`;

const json = (status: number, body: unknown) => Response.json(body, {
  status, headers: { "Cache-Control": "no-store" },
});

Deno.serve(async (request: Request) => {
  const url = Deno.env.get("SUPABASE_URL");
  const publicKey = Deno.env.get("SUPABASE_ANON_KEY");
  const modelUrl = Deno.env.get("AI_SERVER_URL")?.replace(/\/+$/, "");
  if (!url || !publicKey || !modelUrl) return json(503, { code: "unavailable", message: "The AI service is not configured." });

  // Who is asking: the player's own session, verified with Supabase Auth below (verify_jwt is off).
  const auth = request.headers.get("Authorization") ?? "";
  if (!auth.startsWith("Bearer ")) return json(401, { code: "not_logged_in", message: "Log in to scan leaves." });
  const asPlayer = { apikey: publicKey, Authorization: auth, "Content-Type": "application/json" };

  try {
    const user = await fetch(url + "/auth/v1/user", { headers: asPlayer, signal: AbortSignal.timeout(8000) });
    if (!user.ok) return json(401, { code: "not_logged_in", message: "Your session expired. Log in again." });

    if (request.method === "GET") {
      const health = await fetch(modelUrl + "/health", { signal: AbortSignal.timeout(90_000) }).catch(() => null);
      return json(health?.ok ? 200 : 503, { awake: !!health?.ok });
    }
    if (request.method !== "POST") return json(405, { code: "method_not_allowed" });

    // Same unlock rule as the app, enforced here too so it can't be skipped.
    const days = await fetch(url + "/rest/v1/rpc/ai_login_days", {
      method: "POST", headers: asPlayer, body: "{}", signal: AbortSignal.timeout(8000),
    });
    if (!days.ok) return json(503, { code: "unavailable", message: "Couldn't check the AI unlock. Try again." });
    if (Number(await days.text()) < REQUIRED_LOGIN_DAYS) {
      return json(403, { code: "locked", message: `Log in for ${REQUIRED_LOGIN_DAYS} days to unlock` });
    }

    const form = await request.formData();
    const photo = form.get("file");
    const plantId = String(form.get("plant_id") ?? "");
    if (!(photo instanceof File) || photo.size === 0) return json(400, { code: "no_photo", message: "No leaf photo was sent." });
    if (photo.size > MAX_PHOTO_BYTES) return json(413, { code: "photo_too_big", message: "The photo is too big." });
    if (!/^[0-9a-f-]{36}$/i.test(plantId)) return json(400, { code: "no_plant", message: "No plant was given." });

    // 1. Classify the leaf on Render (retry once while Render is still waking up).
    const prediction = await classify(modelUrl, photo);
    if ("error" in prediction) return json(prediction.status, { code: "model_error", message: prediction.error });

    // 2. Care guidance from Gemini: only the assessed condition is sent, never the photo or sensor data.
    const guidance = await careGuidance(prediction);

    // 3. Save to the player's AI scan history (row-level security: own plant only).
    const saved = await fetch(url + "/rest/v1/leaf_scans", {
      method: "POST", headers: { ...asPlayer, Prefer: "return=representation" },
      body: JSON.stringify({
        plant_id: plantId,
        label: prediction.label,
        class_id: prediction.classId ?? null,
        confidence: prediction.confidence,
        is_healthy: prediction.isHealthy,
        low_confidence: prediction.lowConfidence,
        care_guidance: guidance ? guidance.join("\n") : null,
      }),
      signal: AbortSignal.timeout(8000),
    }).then(async (r) => r.ok ? (await r.json())[0]?.id ?? null : null).catch(() => null);

    return json(200, {
      ...prediction,
      careGuidance: guidance,                       // 3-4 actions, or null when unavailable
      guidanceSource: guidance ? "gemini" : "unavailable",
      scanId: saved,                                // null if the history could not be saved
    });
  } catch {
    return json(503, { code: "unavailable", message: "The AI service is unavailable. Try again." });
  }
});

type Prediction = {
  label: string; classId?: string; pathogen?: string | null; confidence: number; confidenceText?: string;
  isHealthy: boolean; lowConfidence: boolean; heatmapPng?: string | null;
};

async function classify(modelUrl: string, photo: File): Promise<Prediction | { error: string; status: number }> {
  const headers: Record<string, string> = { Accept: "application/json" };
  const modelKey = Deno.env.get("AI_API_KEY");
  if (modelKey) headers["X-API-Key"] = modelKey;
  for (let attempt = 1; attempt <= 2; attempt++) {
    const body = new FormData();
    body.append("file", photo, "leaf.jpg");
    const response = await fetch(modelUrl + "/predict", {
      method: "POST", headers, body, signal: AbortSignal.timeout(100_000), // covers Render's ~1 min cold start
    }).catch(() => null);
    if (response?.ok) {
      const p = await response.json();
      const confidence = Number(p.confidence);
      return {
        label: String(p.label), classId: p.classId ?? undefined, pathogen: p.pathogen ?? null,
        confidence, confidenceText: p.confidenceText ?? `${Math.round(confidence * 100)}%`,
        isHealthy: Boolean(p.isHealthy), lowConfidence: p.lowConfidence ?? confidence < 0.5,
        heatmapPng: p.heatmapPng ?? null,
      };
    }
    const status = response?.status ?? 503;
    if (attempt === 1 && (status === 502 || status === 503 || status === 504 || !response)) {
      await new Promise((r) => setTimeout(r, 3000));
      continue;
    }
    const detail = await response?.json().then((b) => typeof b?.detail === "string" ? b.detail : null).catch(() => null);
    return { status: status >= 500 ? 502 : status, error: detail ?? `The leaf model answered with error ${status}.` };
  }
  return { status: 502, error: "The leaf model is not answering. Try again in a minute." };
}

/** 3-4 care actions from Gemini, or null if Gemini is unavailable, over its limit, or answered badly. */
async function careGuidance(p: Prediction): Promise<string[] | null> {
  const key = Deno.env.get("GEMINI_API_KEY");
  if (!key) return null;
  const model = Deno.env.get("GEMINI_MODEL") || GEMINI_MODEL_DEFAULT;
  const condition = p.isHealthy ? "healthy leaf (no disease detected)" : `possible ${p.label}`;
  const prompt = `Classifier result: ${condition}. Confidence: ${Math.round(p.confidence * 100)}%` +
    (p.lowConfidence ? " (low, the classifier is not sure)." : ".");
  try {
    const response = await fetch(
      `${Deno.env.get("GEMINI_API_BASE") || "https://generativelanguage.googleapis.com"}/v1beta/models/${encodeURIComponent(model)}:generateContent`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": key },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: GUIDANCE_RULES }] },
          contents: [{ role: "user", parts: [{ text: prompt }] }],
          generationConfig: {
            responseMimeType: "application/json",
            responseSchema: {
              type: "OBJECT",
              properties: { actions: { type: "ARRAY", items: { type: "STRING" } } },
              required: ["actions"],
            },
          },
        }),
        signal: AbortSignal.timeout(25_000),
      });
    if (!response.ok) return null; // 429 = daily free limit reached; the app shows "guidance unavailable"
    const data = await response.json();
    const text = (data?.candidates?.[0]?.content?.parts ?? []).map((part: { text?: string }) => part.text ?? "").join("");
    const actions = (JSON.parse(text)?.actions ?? [])
      .filter((a: unknown) => typeof a === "string")
      .map((a: string) => a.trim().replace(/^[-*•\d.)\s]+/, ""))
      .filter((a: string) => a.length > 0 && a.length <= 200);
    return actions.length >= 3 ? actions.slice(0, 4) : null;
  } catch {
    return null;
  }
}
