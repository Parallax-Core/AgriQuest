# AgriQuest authentication setup

The Android app uses Supabase Auth's email/password accounts (`auth.users`). An
unrelated public users table does not authenticate anyone. Only a successful
password login followed by `/auth/v1/user` verification unlocks Play. Signup,
email confirmation, and recovery never unlock it. Tokens remain in memory;
activity recreation retains the ViewModel, but restarting the process requires
another login. Sessions expire back to login rather than refreshing silently.

Signup opens a dedicated email-verification step. An incorrect or expired code
keeps the account unverified; a valid code returns the user to login. The client
also rejects login sessions whose server-validated user has no
`email_confirmed_at`. Supabase keeps a pending `auth.users` record before code
verification; verification activates that account rather than creating its first
database record. Keep Confirm email enabled on the server as well.

## Brevo sender

The requested sender is `AgriQuest <agriquestqcu@gmail.com>`. Verify this address
in Brevo. Gmail's domain cannot be authenticated by you; Brevo may replace the
From address with a compliant Brevo address. A domain you own is needed to keep
your own authenticated sender domain. See
[Brevo's free-domain guidance](https://help.brevo.com/hc/en-us/articles/16045394674066-Troubleshooting-Domain-authentication).

1. Create/enable transactional email in Brevo. Verify your sender address and
   authenticate its sending domain using Brevo's DNS instructions.
2. Create a Brevo **API key**, not an SMTP key. Store it in Supabase Dashboard →
   Edge Functions → Secrets as `BREVO_API_KEY`.
3. Add `AUTH_SENDER_EMAIL` (the verified sender) and `AUTH_SENDER_NAME` (`AgriQuest`).
   Do not put these server credentials in Android resources, BuildConfig, git,
   screenshots, or chat.

This implementation uses Brevo's transactional HTTP API through a signed Supabase
Send Email hook. It replaces Supabase's SMTP sender; do not also configure SMTP
as an alternative path around the hook.

## Deploy

The project already supplies `agriquest.supabaseUrl` and
`agriquest.supabaseAnonKey` in untracked `local.properties`. Keep using the public
anon/publishable key here, never the service-role key.

1. Apply all migrations in `supabase/migrations/` in filename order in the target
   project's SQL editor, or link the Supabase CLI and run `supabase db push`.
2. Deploy `supabase/functions/auth-email/index.ts` as the `auth-email` Edge Function
   (`supabase functions deploy auth-email --no-verify-jwt`). The function verifies
   Auth's webhook signature instead of requiring a user's JWT. Supabase injects
   `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` on the server.
3. In Authentication → Hooks, create a **Send Email** HTTP hook pointing at
   `https://YOUR_PROJECT.supabase.co/functions/v1/auth-email`. Generate its signing
   secret, store that secret as `SEND_EMAIL_HOOK_SECRET` in Edge Function secrets,
   then enable the hook. Keep signature verification enabled in the code.
4. Enable email/password signups and **Confirm email** in Supabase Auth. Set the
   minimum password length to at least 8. This app confirms signup with the email
   code, so no app deep link is required.
5. Supabase and Brevo apply their own provider limits. The app reports actual
   server rejections; changing our code does not disable provider limits.

The hook supports signup and recovery emails. Other email actions (invitations,
magic links, email changes) fail closed until explicitly implemented.

## Code-only emails

The Brevo hook sends the Supabase-generated code as plain text and never includes
a confirmation link. Supabase's default SMTP templates are a separate sending
path: seeing an email from Supabase Auth with a confirmation link means the hook
was not used for that request.

If using Supabase's template-based sender, open Authentication → Email Templates
(or Authentication → Email → Templates), choose Confirm signup, and replace the
body with `supabase/templates/confirmation.html`. For Reset password, use
`supabase/templates/recovery.html`. Both use `{{ .Token }}` and omit
`{{ .ConfirmationURL }}`. These are also configured for local development in
`supabase/config.toml`; editing that file alone does not update hosted templates.
Templates do not configure Brevo delivery: enable the signed Brevo hook from the
deployment steps above for the intended production sending path.

## Resend policy

Custom signup and recovery cooldowns have been removed. Resend is available as
soon as the current request finishes; concurrent taps are still blocked while
the request is running. Old device counters are no longer read.

For existing deployments, run
`supabase/migrations/202611070003_remove_custom_email_limits.sql` in the SQL editor
and redeploy the latest email hook. The migration disables the earlier SQL
reservation functions without deleting their tables. The new hook sends directly
through Brevo without consulting the database counters. Supabase/Brevo quotas
and rate limits still apply and can still produce HTTP 429 responses.

## Verify after deployment

- Sign up using an inbox you control; receive and verify the confirmation code.
- Confirm Play stays locked after signup and after email confirmation.
- Log in with the username and correct password: Play opens. Wrong password, unknown username,
  unconfirmed account, and network failures must stay on login.
- Request recovery, reject an incorrect/expired code, verify the real code, set
  a new password, and return to login. Only the new password should work.
- Confirm resend is available after the preceding request completes and no
  custom countdown survives an app restart. Provider rate limits may still reject requests.
- Check Brevo delivery logs using your test inbox. Never log auth tokens or
  complete hook payloads.

Local validation: `./gradlew :app:assembleDebug :app:lintDebug`.
The backend migration and live email delivery must be checked in a configured
Supabase project; compiling Android does not validate those remote services.

References: [Supabase Auth](https://supabase.com/docs/guides/auth/passwords),
[Send Email hooks](https://supabase.com/docs/guides/auth/auth-hooks/send-email-hook),
[Brevo transactional email](https://developers.brevo.com/docs/send-a-transactional-email).
