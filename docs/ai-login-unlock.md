# AI access and silent progress saving

AI access is independent of plant care streaks. Each authenticated account must log in on seven distinct calendar days (Asia/Manila, UTC+8). Days need not be consecutive; repeat logins on the same day count once. Existing accounts receive no invented historical credit. Unlock does not reset when a plant's care streak resets.

`agriquest_private.account_login_days` stores a unique user/date pair, with RLS and no client table access. `record_account_login()` derives the user from `auth.uid()` and the day from the database clock. `ai_login_days()` returns only that account's count, capped at seven. Android records a day when the authenticated account repository is created. Counts are not shared between accounts or taken from the phone clock.

The AI screen displays **Log in for 7 days to unlock** and the day count while locked. It does not construct the photo controls or wake the AI server. The navigation lock badge uses the same account count. Repository diagnostic/scan entry points recheck the count with Supabase before running; failures deny access. Account login and progress loading still work if the login-day request fails, with AI remaining locked.

The separate Render AI server source is not in this workspace. Its existing API-key authentication is unchanged. Enforcing this entitlement against custom clients calling that server directly requires the server developer to verify Supabase JWTs and query `ai_login_days()` as that user before `/predict`; the Android gate alone cannot secure an external endpoint. No collaborator server code was overwritten.

Save-status text was removed from the account header, along with the dashboard's offline save-status snackbar. Log Out remains. DataStore persistence, dirty-state tracking, cloud synchronization/retries, and account progress behavior are unchanged.

Verification before cleanup: live rollback SQL tests passed for zero initial days, same-day deduplication, six/seven-day thresholds, account isolation, and denied direct writes/anonymous calls. Android unit tests and debug APK build passed. Test-only sources and temporary audit reports were subsequently removed for the runtime-focused handoff.
