# ESP32 heartbeat and disconnect rule

The app decides whether the ESP32 is connected only from the readings it sends. There is no separate "ping".

## Firmware requirement

- Call `submit_reading(p_device_id, p_token, ...)` **at least every 15 seconds**, even when no value has changed. Each reading is the heartbeat.
- If a call fails (Wi-Fi or server problem), retry on the next 15-second tick. Do not stop sending.

## What the app does

| Situation | Status shown |
|---|---|
| A new reading arrived in the last 45 s | **Connected** |
| Paired, but no new reading seen yet | **Waiting for the first ESP32 signal** |
| No new reading for more than 45 s (3 missed heartbeats) | **Disconnected (no signal for N s)** |
| Phone can't reach the server for more than 45 s | **Can't reach the server** |

- The app checks for a new reading every 5 seconds, so "Disconnected" appears 45 to 50 seconds after the last signal.
- Time is measured on the phone when a *new* reading appears, so a wrong clock on the ESP32 or the phone doesn't cause false disconnects.
- The constants live in `ConnectionState` (`HEARTBEAT_INTERVAL_MS`, `HEARTBEAT_TIMEOUT_MS`, `POLL_INTERVAL_MS`).
