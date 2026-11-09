# Supabase app updates

The app checks for a newer release when opened or brought to the foreground. It shows an optional Update/Later dialog without changing existing screens. It downloads the APK into private cache, verifies its SHA-256, size, package name, version, and signing certificate, and opens Android's installer. The user must approve installation and, if prompted, allow installations from AgriQuest. This is not a background push notification service.

## Activate the GitHub workflow

1. Commit and push `.github/workflows/android-release.yml` and the accompanying source changes to `main`.
2. Open GitHub **Actions → Publish AgriQuest update**. The push starts a build automatically. **Run workflow** can also start a build on `main`.
3. Wait for every step to succeed, including **Publish to Supabase**. A failed build or failed upload does not announce a new release.
4. Open the successful run's **Summary** for the signed APK download link. The run also has an APK artifact. Download the artifact ZIP and extract `AgriQuest.apk` if using that route.
5. Install this release on each phone once. Existing debug-signed installations normally need to be uninstalled first because the new permanent release signing certificate differs. Sync account progress before uninstalling; local-only files are removed by Android. Log back into the same account afterward.
6. Later successful app changes pushed to `main` produce higher-version signed APKs. Phones running the release will prompt on their next app open/resume. Updates signed with this same key install over the release and retain app data. Documentation-only and IDE-only pushes do not create an unnecessary release.

Repository secrets required (already provided by the owner): `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Add optional `AI_API_KEY` only if the existing AI server requires a key. Android build configuration uses the public URL/key; the service-role key is scoped to the publisher step only. Signing files stay in the runner's temporary directory, are never uploaded as artifacts, and are removed after the job. Keep the keystore and password backup private and outside the repository. No actual GitHub secret values are committed; the workflow contains references to secret names only.

No workflow or initial release is published merely by adding GitHub secrets. The workflow must be pushed and a successful run completed. Existing unrelated IDE changes are preserved and excluded from the update-system commit.

## Backend

Migration `20261109020330_app_release_updates.sql` is applied to the connected project. It adds only:

- `public.app_releases`: public read-only version, immutable APK path, hash, byte length, source commit, and creation time. Client writes are revoked; RLS permits reads for anon/authenticated users.
- Storage bucket `app-updates`: public APK downloads, limited to Android APK MIME type and 50 MiB. No client upload/update/delete policies are added. Only trusted publishing credentials upload releases.

Existing account, plant, ESP32, progress, and authentication structures are untouched. No service-role key is included in the application. Public release metadata and APK files are intentionally available before login. Users' private records remain under their existing policies.

The production publisher `scripts/publish-app-release.mjs` first checks that the version is newer, uploads to `releases/<version_code>/AgriQuest.apk`, then inserts the release record. Upload failure leaves the previous release active. Versions use elapsed UTC seconds since January 1, 2024, so newer builds and workflow reruns receive higher Android version codes. Publishing jobs are serialized. Never recreate or rotate the signing key for routine updates. Do not edit a published APK in place. Retain the latest APK and remove older obsolete release objects through Supabase Storage when needed to manage storage usage; deleting an old file does not erase account progress.

## Verified and remaining checks

Debug and signed-release builds and lint passed. `apksigner` verified the local release certificate against the generated keystore. The workflow passed actionlint. Publisher checks covered success, failed upload (no metadata announcement), refusal to replace a newer release, missing credentials, and invalid non-HTTPS origin. Live rollback SQL checks confirmed public metadata access and denied client release writes/deletion and APK uploads. No audit records remain.

The GitHub-hosted workflow has not run until these changes are pushed, so repository secret contents and hosted SDK availability are not yet proven. Physical-phone unknown-source permissions, installer UI, and upgrade data retention require the first two signed releases on hardware. Optional update checks fail silently when offline and never block login or plant care. Once the user chooses Download, failures are shown in the update dialog.

References: [Supabase public buckets](https://supabase.com/docs/guides/storage/buckets/fundamentals), [Android signing and updates](https://developer.android.com/studio/publish/app-signing), [GitHub workflow triggers](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows).
