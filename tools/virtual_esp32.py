"""
Virtual ESP32 for AgriQuest: acts like the real soil-moisture + pump unit, so the app and
database can be tested before the hardware is ready.

It uses exactly the calls the real firmware will use (paper section 3.8.2):
  * submit_reading        - sends a soil-moisture reading every few seconds
  * get_pending_commands  - checks for pump commands every 5 seconds
  * report_command        - reports that the pump ran
Those three calls use only the public (publishable) key plus the device token,
like the real ESP32. Logging in is needed once, only to pair this virtual device
with your plant (the app's Bluetooth pairing will do this step for the real ESP32).

Needs Python 3.8+ and nothing else (standard library only).

Run from the project folder:
    python tools/virtual_esp32.py --email you@example.com --password yourpassword

Options:
    --interval 10        seconds between readings (default 10)
    --moisture 55        starting soil moisture in % (default 55)
    --dry-rate 0.8       how much moisture drops per reading, in % (default 0.8)
    --device NAME        device id (default AgriQuest-VIRTUAL)
The Supabase URL and key are read from local.properties (agriquest.supabaseUrl / agriquest.supabaseKey).
Stop with Ctrl+C.
"""
import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

POLL_SECONDS = 5           # how often the real firmware will check for commands
PUMP_GAIN_PER_SECOND = 4.0  # % of moisture added per second of pumping (simulated)


def read_local_properties():
    here = os.path.dirname(os.path.abspath(__file__))
    path = os.path.join(here, "..", "local.properties")
    values = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, value = line.split("=", 1)
                    values[key.strip()] = value.strip()
    return values


class Supabase:
    def __init__(self, url, key):
        self.url = url.rstrip("/")
        self.key = key

    def call(self, method, path, body=None, token=None):
        headers = {"apikey": self.key, "Accept": "application/json", "Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(self.url + path, data=data, headers=headers, method=method)
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
            raise RuntimeError(f"{e.code}: {detail}") from None

    def rpc(self, function, args, token=None):
        return self.call("POST", "/rest/v1/rpc/" + function, args, token)


def pair(api, email, password, device_id):
    """Logs in once and links this virtual device to the account's first plant."""
    session = api.call("POST", "/auth/v1/token?grant_type=password", {"email": email, "password": password})
    access = session["access_token"]
    plants = api.call("GET", "/rest/v1/plants?select=id,name&order=created_at.asc&limit=1", token=access)
    if not plants:
        sys.exit("This account has no plant yet. Log in once in the app first.")
    plant = plants[0]
    device_token = api.rpc("pair_device", {"p_plant_id": plant["id"], "p_device_id": device_id}, token=access)
    print(f"Paired {device_id} with plant '{plant['name']}'.")
    return device_token


def main():
    props = read_local_properties()
    parser = argparse.ArgumentParser(description="Virtual ESP32 (soil moisture + pump) for AgriQuest")
    parser.add_argument("--email", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--device", default="AgriQuest-VIRTUAL")
    parser.add_argument("--interval", type=float, default=10)
    parser.add_argument("--moisture", type=float, default=55)
    parser.add_argument("--dry-rate", type=float, default=0.8)
    parser.add_argument("--url", default=props.get("agriquest.supabaseUrl", ""))
    parser.add_argument("--key", default=props.get("agriquest.supabaseKey", ""))
    args = parser.parse_args()
    if not args.url.startswith(("https://", "http://localhost", "http://127.0.0.1")) or not args.key:
        sys.exit("Supabase URL/key not found. Add them to local.properties or pass --url and --key.")

    api = Supabase(args.url, args.key)
    token = pair(api, args.email, args.password, args.device)
    moisture = args.moisture
    next_reading = 0.0
    print(f"Sending a reading every {args.interval:g} s and checking for pump commands every {POLL_SECONDS} s. Ctrl+C to stop.\n")

    while True:
        now = time.time()
        if now >= next_reading:
            moisture = max(0.0, moisture - args.dry_rate + random.uniform(-0.2, 0.2))
            try:
                api.rpc("submit_reading", {"p_device_id": args.device, "p_token": token,
                                           "p_soil_moisture": round(moisture, 1)})
                print(f"{time.strftime('%H:%M:%S')}  reading sent: soil moisture {moisture:.1f}%")
            except Exception as e:  # keep running through network blips, like the firmware should
                print(f"{time.strftime('%H:%M:%S')}  could not send reading ({e})")
            next_reading = now + args.interval

        try:
            commands = api.rpc("get_pending_commands", {"p_device_id": args.device, "p_token": token}) or []
        except Exception as e:
            print(f"{time.strftime('%H:%M:%S')}  could not check commands ({e})")
            commands = []

        for command in commands:
            seconds = int(command["duration_s"])
            print(f"{time.strftime('%H:%M:%S')}  >>> {command['actuator'].upper()} ON for {seconds} s (command #{command['id']})")
            time.sleep(seconds)
            if command["actuator"] == "pump":
                moisture = min(100.0, moisture + PUMP_GAIN_PER_SECOND * seconds)
            api.rpc("report_command", {"p_device_id": args.device, "p_token": token,
                                       "p_command_id": command["id"], "p_success": True})
            print(f"{time.strftime('%H:%M:%S')}  <<< {command['actuator']} off, reported done")
            next_reading = 0  # send a fresh reading right away so the app shows the change

        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped.")
    except RuntimeError as e:
        sys.exit(f"Error from Supabase: {e}")
