# AI leaf assessment (Edge Function + Gemini)

The leaf scan follows the paper's design (FR-10 to FR-12):

```
app --photo + plant_id--> assess-leaf (Supabase Edge Function)
                            |--photo--> EfficientNet-B0 model on Render  -> label, confidence, heatmap
                            |--leaf condition only--> Gemini 3.5 Flash-Lite -> 3-4 care actions
                            '--result--> public.leaf_scans (the player's AI scan history)
```

- Only the assessed condition (for example "possible Early Blight, 91%") goes to Gemini: never the photo and never sensor data.
- The Render model key and the Gemini key are Supabase secrets. They are not in the app or on GitHub.
- The prompt asks for 3 or 4 short, practical actions, with no chemical products, doses or medical claims, worded as "possible", not a confirmed diagnosis. The function keeps only answers with 3 or 4 actions.
- If Gemini fails or reaches its daily free limit, the app still shows the model's result and says the AI care guidance is unavailable.
- The function also enforces the 7-login-day AI unlock, so it can't be skipped.
- Until the function is deployed, the app falls back to calling Render directly and shows its built-in care tips.

Code: `supabase/functions/assess-leaf/index.ts` (server) and `app/.../data/remote/AssessLeafApi.kt` (app).

## Deploy (once)

1. **Get a Gemini API key.** Open Google AI Studio, sign in, choose **Get API key → Create API key**. Copy it. Don't paste it anywhere else.
2. **Add the secrets.** Supabase dashboard → **Edge Functions → Secrets**, add:

   | Name | Value |
   |---|---|
   | `GEMINI_API_KEY` | the key from step 1 |
   | `AI_SERVER_URL` | the Render model address, for example `https://<your-service>.onrender.com` |
   | `AI_API_KEY` | the Render model key (the value of `agriquest.aiApiKey` in `local.properties`); leave out if the model has no key |
   | `GEMINI_MODEL` | optional; default `gemini-3.5-flash-lite` |

3. **Deploy the function**, either way:
   - **Supabase CLI** (in the project folder):
     ```
     npx supabase login
     npx supabase link --project-ref <your project ref>
     npx supabase functions deploy assess-leaf
     ```
   - **Dashboard:** Edge Functions → **Deploy a new function → Via Editor**, name it `assess-leaf`, paste the contents of `supabase/functions/assess-leaf/index.ts`, deploy. Then in its settings turn **Verify JWT** off: the function checks the player's login itself.

## Test

1. In the app, log in with an account that has the AI unlocked, and scan a leaf.
2. **What To Do** should show 3-4 actions with the note "Care guidance by AI (Gemini)".
3. Supabase → Table Editor → `leaf_scans`: a new row with the label, confidence and `care_guidance`.
4. Function errors show under Edge Functions → `assess-leaf` → **Logs**.

## After it works

Remove `agriquest.aiApiKey` from `local.properties` and rebuild, so the app no longer carries the Render key. (The direct-Render fallback then only works if the model has no key.)
