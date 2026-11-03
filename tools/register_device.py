"""
Registers a real ESP32 with your AgriQuest account and prints its device token.

This stands in for Bluetooth pairing until it is built: it logs in as you, finds your
plant, and calls the same pair_device database function the app will use.
Running it again for the same device gives a NEW token (the old one stops working).

Needs Python 3.8+ only. Run from the project folder:
    python tools/register_device.py --email you@example.com --password yourpassword --device AgriQuest-01

Then paste the two printed lines into firmware/agriquest_esp32/secrets.h.
The Supabase URL and key are read from local.properties.
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request


def read_local_properties():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "local.properties")
    values = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, value = line.split("=", 1)
                    values[key.strip()] = value.strip()
    return values


def call(url, key, method, path, body=None, token=None):
    headers = {"apikey": key, "Accept": "application/json", "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url.rstrip("/") + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            text = response.read().decode("utf-8")
            return json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        try:
            parsed = json.loads(detail)
            detail = parsed.get("msg") or parsed.get("message") or parsed.get("error_description") or detail
        except ValueError:
            pass
        sys.exit(f"Supabase error {e.code}: {detail}")
    except urllib.error.URLError as e:
        sys.exit(f"Could not reach Supabase: {e.reason}")


def main():
    props = read_local_properties()
    parser = argparse.ArgumentParser(description="Register an ESP32 with your AgriQuest plant")
    parser.add_argument("--email", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--device", default="AgriQuest-01", help="device id, e.g. AgriQuest-01")
    parser.add_argument("--url", default=props.get("agriquest.supabaseUrl", ""))
    parser.add_argument("--key", default=props.get("agriquest.supabaseKey", ""))
    args = parser.parse_args()
    if not args.url.startswith(("https://", "http://localhost", "http://127.0.0.1")) or not args.key:
        sys.exit("Supabase URL/key not found. Add them to local.properties or pass --url and --key.")

    session = call(args.url, args.key, "POST", "/auth/v1/token?grant_type=password",
                   {"email": args.email, "password": args.password})
    access = session["access_token"]
    plants = call(args.url, args.key, "GET", "/rest/v1/plants?select=id,name&order=created_at.asc&limit=1",
                  token=access)
    if not plants:
        sys.exit("This account has no plant yet. Log in once in the app first.")
    plant = plants[0]
    token = call(args.url, args.key, "POST", "/rest/v1/rpc/pair_device",
                 {"p_plant_id": plant["id"], "p_device_id": args.device}, token=access)

    print(f"Registered {args.device} to plant '{plant['name']}'.\n")
    print("Paste these two lines into firmware/agriquest_esp32/secrets.h:\n")
    print(f'#define DEVICE_ID     "{args.device}"')
    print(f'#define DEVICE_TOKEN  "{token}"')
    print("\nKeep the token private. Running this again replaces it.")


if __name__ == "__main__":
    main()
