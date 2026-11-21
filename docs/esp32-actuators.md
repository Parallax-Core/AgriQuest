# ESP32 actuators: Water, Fan and Light buttons

On ESP32 hardware, each in-game button sends a command to the ESP32 through Supabase. The animation and sound play only after the ESP32 confirms. In emulated mode the buttons act at once.

## How a tap works

1. The app calls `request_command(plant, actuator, duration_s)`. The database checks its rules (one command at a time per actuator; the pump also has a maximum run time and a cooldown).
2. The ESP32 checks `get_pending_commands` every 5 s, switches its relay, and reports back with `report_command`.
3. The app follows the command's status and changes the button and animation only on confirmation:

| Button | Confirmed when | Animation |
|---|---|---|
| Water | status becomes `running` (the ESP32 picked it up) | while the pump runs |
| Light | status becomes `done` (the ESP32 switched the lamp) | until switched off, or after 4 h |
| Fan | status becomes `done` | until switched off, or after 4 h |

While waiting, Fan and Light show "Waiting...". A failed or expired command shows its reason and the animation doesn't change. No answer within 70 s counts as offline.

## Command values

| Actuator | `duration_s` | ESP32 action |
|---|---|---|
| `pump` | 1 to 10 | pump on for that long (relay 1, GPIO 26) |
| `light` | 2 or more | lamp on for that long, then off by itself (relay 3, GPIO 25) |
| `light` | 1 | lamp off |
| `fan` | any | reported as failed: "not fitted" (no fan yet) |

"Off" is sent as a 1-second command because `actuator_commands.duration_s` must be at least 1. The app sends 14400 s (4 h) for "on" (`ACTUATOR_ON_SECONDS`) and turns its own animation off when that time is up, matching the ESP32.

## Known limits

- If the ESP32 restarts, the lamp starts off, but the app keeps showing it on until it is switched or the 4 h pass.
- Typing `lamp` in the Serial Monitor switches the lamp locally; the app is not told.
