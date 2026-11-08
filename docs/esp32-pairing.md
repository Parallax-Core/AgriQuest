# ESP32 pairing and monitoring contract

The Android/backend integration is ready for firmware implementation. No ESP32 firmware is present in this repository. Physical button access and Bluetooth security must be implemented and tested by the firmware developer before production pairing is usable.

## Provisioning

Give each ESP32 a unique existing-format `device_id` (3–40 ASCII letters, digits, `_`, `-`) and an independently generated, cryptographically random 256-bit device token. Store the token in protected device storage. Never ship a shared fleet token, Supabase service-role key, or user ID in firmware. Configure the Supabase project URL and public API key separately.

For a new device, a trusted administrator inserts only its token's SHA-256 hex digest into `agriquest_private.device_pairing`. This table is not exposed to Android. Provision through a trusted administrative process, not a public registration endpoint:

```sql
-- Replace placeholders with the hardware ID and an OFFLINE-computed digest.
insert into agriquest_private.device_pairing(device_id, token_hash)
values ('DEVICE_ID', '64_lowercase_hex_characters_of_sha256_device_token');
```

The placeholder digest is deliberately invalid until replaced. Do not place actual credentials in source control or SQL editor history. Existing registered devices retain their existing `devices.token_hash` and token; opening a pairing window imports that hash into the registry without rotating the credential.

## Physical pairing and BLE

1. Firmware opens a short pairing window only after the physical pairing button is pressed. Rebooting, knowing a MAC address, or receiving a remote request must not open the window.
2. Require Bluetooth LE Secure Connections with authenticated MITM protection (passkey or numeric comparison) before exposing proof. **Just Works bonding is insufficient.** Android's bonded state alone does not establish this security level; firmware must enforce it on the characteristic and reject weaker bonds, including old insecure bonds.
3. Generate a fresh random 256-bit pairing code, encoded as 64 lowercase hex characters. Call `POST /rest/v1/rpc/open_device_pairing` over verified HTTPS with the project's public API key and JSON:

```json
{"p_device_id":"DEVICE_ID","p_token":"DEVICE_SECRET","p_pairing_code_hash":"SHA256_HEX_OF_PAIRING_CODE"}
```

4. Wait for a successful response before returning proof. The server controls expiry (two minutes); never reuse a code, log it, advertise it, or send it over an unauthenticated link. A fresh window invalidates the old code. Rate-limit button windows and pairing requests on the device.
5. Preserve the existing BLE UUIDs: service `0000ffe0-0000-1000-8000-00805f9b34fb`, characteristic `0000ffe1-0000-1000-8000-00805f9b34fb`, notification CCCD `00002902-0000-1000-8000-00805f9b34fb`. Advertise the service and an ESP32/AgriQuest device name. Support negotiated MTU 247 and notifications; send the compact proof in one notification (under 197 bytes with the maximum supported ID). The Android path requires MTU >= 200.
6. Android writes `{"type":"pairing_request","version":1}`. During the physical window, on the authenticated encrypted connection only, firmware notifies:

```json
{"type":"pairing_proof","version":1,"deviceId":"DEVICE_ID","pairingCode":"64_LOWERCASE_HEX_RANDOM_CHARACTERS"}
```

Return `{"type":"pairing_error","version":1}` when not authorized or the window is closed. Do not include the device token, owner ID, or service credentials. Android waits up to 90 seconds for bonding/connection and 20 seconds for the proof response.

7. Android sends the proof to `pair_device(p_plant_id,p_device_id,p_pairing_code)` using the logged-in user's access token. It never receives the long-lived device token. The backend derives ownership from `auth.uid()`, verifies the plant owner, consumes the proof once, and serializes claims for the same device. Even a valid proof cannot steal a device from another account. Firmware does not need to persist the account ID.

The two-argument legacy `pair_device` is retained for backend compatibility but revoked from anonymous and authenticated clients. Firmware must not use it for registration. Unsupported firmware fails closed; there is no ID/MAC-only fallback.

## Ownership, multiple devices, and assignment

`devices` remains the sole ownership/assignment table. An account can own multiple rows; each row has one immutable `owner_id` and one nullable `plant_id`. A plant can exist with no device. There is no public ownership-transfer operation. `unpair_device(p_device_id)` clears the assignment but retains ownership, credentials, and historical readings. A different account cannot claim the unassigned device.

The existing Android layout has no new plant selector. `SupabasePlantApi.listPlants()`, `loadProgress(plantId)`, `listDevices()`, `pairDevice(plantId, proof)`, and `monitoringBinding(plantId, deviceId)` support explicit selection. The existing screen pairs to its active plant; reconnecting a device assigned elsewhere is rejected rather than silently moving it. An explicit future plant-management flow can reuse these methods. Multiple devices can independently send readings now; no firmware account-switching logic is needed.

## Monitoring and commands

Use the existing RPC signatures and measurement units:

- `submit_reading(p_device_id,p_token,p_soil_moisture,p_air_temp,p_humidity,p_light_lux,p_water_level_cm)`; measurement parameters default to null and retain their existing units.
- `get_pending_commands(p_device_id,p_token)`.
- `report_command(p_device_id,p_token,p_command_id,p_success,p_message)`.

Always verify HTTPS certificates. Never log tokens or put them in URL query parameters. Implement retry/backoff and reject unauthorized/unassigned responses. Never supply an owner or plant ID to route telemetry: the server derives the plant from the authenticated device row and locks the assignment through the write. Unregistered, unassigned, wrong-token, or ownership-inconsistent devices are rejected.

Each reading retains its original `plant_id` and `device_id`. Reassignment affects new readings only. Pending/running commands for the old plant expire on reassignment/unassignment, and command retrieval/reporting are restricted to the current plant. Firmware must also stop active actuators safely on disconnect/unassignment and enforce local pump safety limits; a database cannot retract a command already executing on hardware.

## Hardware acceptance checks still required

Test two accounts and two ESP32s, button-only window activation, authenticated MITM bonding, rejection of Just Works/expired/replayed proof, Android Bluetooth permission denial/revocation, reconnect/bond removal, Wi-Fi loss, telemetry routing, reassignment, unassignment, and actuator fail-safe behavior. Software tests do not prove physical-access security or hardware behavior.
