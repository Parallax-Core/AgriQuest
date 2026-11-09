// Production release publisher. Publishing credentials never go into the Android build.
import { readFileSync, appendFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

async function main() {
  const required = ['SUPABASE_URL', 'SUPABASE_SERVICE_ROLE_KEY', 'AGRIQUEST_VERSION_CODE',
    'AGRIQUEST_VERSION_NAME', 'GITHUB_SHA'];
  const missing = required.filter(name => !process.env[name]);
  if (missing.length) throw new Error('Missing release configuration: ' + missing.join(', '));
  const base = process.env.SUPABASE_URL.trim().replace(/\/$/, '');
  const parsed = new URL(base);
  if (parsed.protocol !== 'https:' || parsed.origin !== base) throw new Error('SUPABASE_URL must be the HTTPS project origin');
  const code = Number(process.env.AGRIQUEST_VERSION_CODE);
  if (!Number.isInteger(code) || code <= 1 || code >= 2100000000 || !/^[0-9a-f]{40}$/.test(process.env.GITHUB_SHA)) {
    throw new Error('Invalid version or source commit');
  }
  const apk = readFileSync(process.argv[2]);
  if (!apk.length || apk.length > 52428800 || apk.subarray(0, 2).toString() !== 'PK') {
    throw new Error('APK is empty, invalid, or exceeds the 50 MiB update limit');
  }
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY.trim();
  async function request(path, method = 'GET', body, type = 'application/json') {
    const response = await fetch(base + path, {
      method, body, redirect: 'error', signal: AbortSignal.timeout(180000),
      headers: { apikey: key, Authorization: 'Bearer ' + key, 'Content-Type': type, 'x-upsert': 'false' },
    });
    if (!response.ok) throw new Error(`Supabase release request failed (HTTP ${response.status}); check the service-role secret and release migration`);
    return response;
  }
  const latest = await (await request('/rest/v1/app_releases?select=version_code&order=version_code.desc&limit=1')).json();
  if (latest.length && latest[0].version_code >= code) throw new Error('Refusing to replace a newer or existing release');
  const path = `releases/${code}/AgriQuest.apk`;
  await request('/storage/v1/object/app-updates/' + path, 'POST', apk, 'application/vnd.android.package-archive');
  // Publish the pointer only AFTER the complete signed APK was uploaded successfully.
  const release = { version_code: code, version_name: process.env.AGRIQUEST_VERSION_NAME,
    apk_path: path, sha256: createHash('sha256').update(apk).digest('hex'),
    size_bytes: apk.length, commit_sha: process.env.GITHUB_SHA };
  await request('/rest/v1/app_releases', 'POST', JSON.stringify(release));
  console.log(`Published AgriQuest ${release.version_name} (${code})`);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY,
    `### AgriQuest ${release.version_name}\n\n[Download the signed APK](${base}/storage/v1/object/public/app-updates/${path})\n\nInstall this release once to enable future in-app update prompts.\n`);
}

try { await main(); }
catch (error) { console.error(error.message); process.exitCode = 1; }
