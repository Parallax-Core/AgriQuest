# Two plants per account (Plant A and Plant B)

A player can grow up to **2 plants**: a tomato or an eggplant each. Both use the same growth stages,
care settings and quests. The top bar shows `A · <name>` and `B · <name>` chips to switch between them,
or `+ Add plant` while the account has only one.

## What each plant has, and what the account shares

| Per plant | Shared by the account |
|---|---|
| Type (tomato / eggplant), name, age and stage | XP and level |
| Soil reading, Water / Fan / Light buttons | Streak and daily quests (completed by the plant on screen) |
| Leaf scans, journal, moisture history | Awards |
| ESP32 slot (see below) | Outfits owned and worn |

On the phone, Plant A keeps the original account file. Plant B gets its own file (`AccountStorage.fileName`).
Before switching, the app sends any unsaved progress (`flushSync`).

## One ESP32 for both plants

A board has two sets of sensors and actuators:

| | Plant A (slot 1) | Plant B (slot 2) |
|---|---|---|
| Soil sensor | GPIO 34 | GPIO 35 |
| Pump | relay IN1, GPIO 26 | relay IN2, GPIO 27 |
| Grow lamp | relay IN3, GPIO 25 | relay IN4, GPIO 33 |

- **Database:** `devices.plant_id` is slot 1 (paired with the physical pairing proof, as before).
  `devices.plant_b_id` is slot 2. Readings are sent with `p_slot`, and commands carry a `slot`.
  A Plant A command never runs Plant B's pump or lamp.
- **App:** select Plant B, then pair the same board. Because the board already belongs to the account,
  the app links Plant B to slot 2 (`link_plant_to_device`) instead of re-pairing it.
- **Firmware:** Plant B is off until `PLANT_B_FITTED` is set to `1` in `agriquest_esp32.ino`.
  Until then, Plant B commands are reported as "not fitted".
- **Testing without hardware:** `tools/virtual_esp32.py --plant-b` also simulates slot 2.

## Deploy

1. Run `supabase/migrations/20261124120000_two_plants_shared_device.sql` in the Supabase SQL editor.
   It's needed before **Add plant** works, and before Plant B can use the board.
   Older firmware keeps working: readings default to slot 1.
2. Flash the updated firmware once the second sensor, pump and lamp are wired, with `PLANT_B_FITTED 1`.

## Placeholder art

`eggplant_stages_<stage>.png` are placeholders made from the tomato reference art:
- The early stages are the same plant.
- The flowers are recolored lavender.
- The fruit and ripening stages have long, hanging pixel eggplants drawn on the leafy plant.

Replace them with final art using the same names and 166×210 size (see `art-guidelines.md`).

## Known limits

- The AI leaf scan model is trained on tomato leaves only.
- The two plants share the outfits, so a hat worn by Plant A is also worn by Plant B.
