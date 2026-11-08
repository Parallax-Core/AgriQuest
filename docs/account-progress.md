# Account progress and ESP32 integration

No database schema, policies, functions, or existing rows were changed while
implementing this feature. The schema was inspected read-only. Normal app use
now saves the signed-in user's data through the existing authenticated API.

## Account lifecycle

- A verified password login supplies the Supabase user ID and bearer token.
- Before Play opens, load that user's profile and oldest owned plant using
  explicit user filters plus the database's existing RLS policies.
- Use a separate DataStore file named by the validated user UUID. The former
  shared `agriquest_preferences` file is never assigned to a user or imported.
- A new database profile starts at zero XP, level 0, and zero streak. The app no
  longer seeds earned awards, journal entries, or moisture history.
- Returning accounts restore XP, stage, sowing date, streak, today's quest
  claims, quiz completion, and journal entries. Daily limits remain separate
  from email request limits.
- Log Out cancels the account repository, HTTP requests, simulation/monitoring,
  and dashboard ViewModel. Token expiry does the same and returns to login.

## Persistence

Core progress is saved locally before background cloud synchronization. The
local dirty revision is acknowledged only if no newer edit has appeared. Failed
syncs retain the dirty data and retry. The account header shows pending/offline
sync status; signing out keeps unsent local data for the next login.

The existing fields are used as follows:

| Existing table | App use |
| --- | --- |
| `profiles` | `exp`, `level`, `streak_days`, `last_reset_date`, `quiz_completed_date` |
| `plants` | Owned plant ID, name, `sowing_date`, `growth_stage` |
| `quest_progress` | Daily claims and awarded XP; existing unique user/quest/date key supports retry upserts |
| `journal_entries` | Restore and append care notes; server generates identity IDs |
| `devices` | Resolve the account's paired device for its selected plant |
| `sensor_readings` | Read latest telemetry filtered by plant and device |

Reserved ledger IDs `_daily_quiz` and `_care_bonus` preserve the daily XP total
within the existing `quest_progress` table. Journal insert retries look up the
original creation timestamp; no identity columns are overridden. Photos and
scan metadata remain account-scoped on this device; cloud photo storage is not
added by this change.

The existing API provides separate requests rather than a transactional
progress-save RPC. XP writes never intentionally lower the cloud total, but
concurrent edits from multiple devices are not an exactly-once reward system.
This limitation cannot be fully eliminated without backend changes. Use one
active session/device per account until transactional reward APIs are added.

## Firmware contract

Hardware mode polls `devices` and `sensor_readings` every 10 seconds using the
current user's bearer token. Unpaired accounts show "Awaiting paired ESP32
data". Readings older than two minutes do not indicate a live connection.
Missing sensor values are not filled with simulated values.

The firmware can continue using the existing device-token RPCs:

- `submit_reading`: send sensor measurements for the paired device.
- `get_pending_commands` / `report_command`: consume and acknowledge commands.

The app sends pump requests through the existing `request_command` RPC. Its
reservoir, duration, and cooldown checks remain authoritative. A queued command
does not fabricate a moisture increase. Fan AUTO/ON/OFF commands are not mapped
to the database's timed-command contract yet and are not simulated as successful
hardware commands. Direct unauthenticated BLE packets cannot overwrite an
account's cloud monitoring state.

## Older-plant detection (prepared, not automatically enabled)

`DetectedPlantIdentity` and `AgriQuestRepositoryImpl.applyDetectedPlantIdentity`
are the future integration entry point. A detection must carry the current
account, owned plant ID, paired device ID, and an explicit sowing date or
estimated stage. The repository verifies the binding before applying metadata.
Stage/age adjustments do not award XP. Ordinary moisture/temperature readings
never trigger age detection. Firmware/vision age detection is not implemented.

## Validation

Before cleanup, unit tests covered separate account files, restart restoration, dirty-sync
revisions, fresh defaults, ownership filters, authenticated PATCH/upsert
requests, paired sensor queries, stale/partial packets, and command rejection.
The test-only sources were removed for the runtime-focused handoff. Cross-device and physical
ESP32 end-to-end validation require real account/device sessions.
