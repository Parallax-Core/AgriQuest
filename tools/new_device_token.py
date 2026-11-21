"""
Makes a new secret token for an ESP32 and prints the SQL that registers it.

Use this when an ESP32 needs a (new) token: a first-time device, or one whose secrets.h was lost.
It replaces the old tools/register_device.py, which used the old pair_device call that the
secure-pairing migration switched off.

The token itself never goes into the database or the SQL: only its SHA-256 hash does
(docs/esp32-pairing.md: keep real credentials out of the SQL editor history).

Needs Python 3.8+ only. Run from the project folder:
    python tools/new_device_token.py --email you@example.com
    python tools/new_device_token.py --email you@example.com --device AgriQuest-VIRTUAL

Then:
  1. Paste the printed SQL into Supabase > SQL Editor and click Run.
  2. Paste the two printed #define lines into firmware/agriquest_esp32/secrets.h
     (or pass --token to tools/virtual_esp32.py for the virtual device).
Running it again makes another new token; the previous one stops working once you run the new SQL.
"""
import argparse
import hashlib
import re
import secrets


def main():
    parser = argparse.ArgumentParser(description="Make a new ESP32 device token and its registration SQL")
    parser.add_argument("--email", required=True, help="the AgriQuest account that owns the plant")
    parser.add_argument("--device", default="AgriQuest-01", help="device id, e.g. AgriQuest-01")
    args = parser.parse_args()

    if not re.fullmatch(r"[A-Za-z0-9_-]{3,40}", args.device):
        raise SystemExit("Device id: 3-40 letters, digits, - or _ only.")
    email = args.email.strip().lower().replace("'", "''")

    token = secrets.token_hex(32)                      # 64 hex characters
    token_hash = hashlib.sha256(token.encode("utf-8")).hexdigest()

    print("=" * 70)
    print("1) Run this in Supabase > SQL Editor:")
    print("=" * 70)
    print(f"""
-- Gives {args.device} a new token and links it to the account's first plant.
-- Contains only the token's hash, never the token.
with target as (
    select p.id as plant_id, p.owner_id
    from public.plants p
    join auth.users u on u.id = p.owner_id
    where lower(u.email) = '{email}'
    order by p.created_at
    limit 1
)
insert into public.devices (id, plant_id, owner_id, token_hash)
select '{args.device}', plant_id, owner_id, '{token_hash}' from target
on conflict (id) do update
    set token_hash = excluded.token_hash,
        plant_id = coalesce(public.devices.plant_id, excluded.plant_id)
returning id, plant_id, owner_id;

-- Keep the pairing registry on the same token (used by Bluetooth pairing later).
insert into agriquest_private.device_pairing (device_id, token_hash)
values ('{args.device}', '{token_hash}')
on conflict (device_id) do update
    set token_hash = excluded.token_hash, pairing_code_hash = null, expires_at = '-infinity';
""")
    print("If the first query returns no row, the email has no account or plant yet:")
    print("log in once in the app, then run it again.\n")
    print("=" * 70)
    print("2) Put these two lines in firmware/agriquest_esp32/secrets.h:")
    print("=" * 70)
    print(f'#define DEVICE_ID     "{args.device}"')
    print(f'#define DEVICE_TOKEN  "{token}"')
    print("\nKeep the token private: don't commit it, paste it in chats, or put it in SQL.")


if __name__ == "__main__":
    main()
