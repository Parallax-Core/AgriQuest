# Username/password login

Login now accepts the signup username and password. Matching ignores surrounding whitespace and letter case. Signup still asks for an email, and email verification and password recovery remain email-based. Android still validates the returned bearer token against Supabase Auth and requires a confirmed email before opening the account.

The deployed `username-login` Edge Function privately resolves the username, then uses Supabase Auth's normal password token endpoint. It does not implement password hashing or expose an anonymous username-to-email API. Unknown usernames and wrong passwords receive the same generic invalid-credentials response. Supabase Auth rate-limit responses remain enforced. No passwords, sessions, or resolved emails are logged. The endpoint is intentionally public because a user does not have a session before signing in; password verification is its authentication step.

Migration `20261108133351_username_password_login.sql` adds the private, RLS-protected `account_usernames` registry and service-role-only resolver. Existing signup usernames are backfilled unchanged apart from case/whitespace normalization. The one older account without a stored signup username uses its existing profile display name. The table is private because `profiles` has no existing canonical username field; no existing table/column was renamed or duplicated. A separate Auth insert trigger reserves unique usernames atomically without replacing the existing profile/plant trigger. Later user-metadata changes cannot change or steal a canonical login name. Username renaming requires a future controlled flow.

For trusted administration only, inspect a particular account's canonical login name in SQL Editor:

```sql
select n.username
from agriquest_private.account_usernames n
join auth.users u on u.id=n.user_id
where lower(u.email)=lower('user@example.com');
```

Existing account IDs, passwords, emails, progress, AI login-day records, and ESP32 ownership are unchanged. No collaborator features or UI styling were replaced. The Android modifications are limited to the login field/validation, AuthViewModel forwarding, SupabaseAuthApi endpoint/payload, and corresponding tests. The existing email hook was not modified or redeployed.

Verification: Android tests and debug APK build passed. Rollback SQL tests passed for case-insensitive matching, duplicate rejection, metadata isolation, and denied anonymous/authenticated email lookup. A live temporary-account test passed for successful case-insensitive login, wrong-password rejection, unknown-username rejection, and server-validated identity. The session was logged out and the temporary account and related rows removed; original counts remained 4 accounts, 2 devices, and 866 sensor readings.
