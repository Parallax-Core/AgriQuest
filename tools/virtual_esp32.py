"""
Virtual ESP32 for AgriQuest: acts like the real soil-moisture + pump unit, so the app and the
database can be tested without the hardware.

It makes exactly the calls the real firmware makes, with only the publishable key and the
device's own token (no login, no secret key):
  * submit_reading        - sends a soil-moisture reading every few seconds
  * get_pending_commands  - checks for pump commands every 5 seconds
  * report_command        - reports that the pump ran

Setup (once): give the virtual device its own token, the same way as the real ESP32:
    python tools/new_device_token.py --email you@example.com --device AgriQuest-VIRTUAL
Run the printed SQL in Supabase > SQL Editor, then start it with the printed token:
    python tools/virtual_esp32.py --device AgriQuest-VIRTUAL --token <token>
(or set the token in the AGRIQUEST_DEVICE_TOKEN environment variable instead of --token).

The app shows the first device linked to the plant. To see the virtual one, run it while the real
ESP32's row is unlinked, or give the virtual device the real one's id (and switch the real one off).

Options:
    --interval 10        seconds between readings (default 10)
    --moisture 55        starting soil moisture in % (default 55)
    --dry-rate 0.8       how much moisture drops per reading, in % (default 0.8)
The Supabase URL and key are read from local.properties. Needs Python 3.8+ only. Stop with Ctrl+C.
"""
import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.request

POLL_SECONDS = 5            # how often the real firmware checks for commands
PUMP_GAIN_PER_SECOND = 4.0  # % of moisture added per second of pumping (simulated)
MAX_PUMP_SECONDS = 10       # same local safety cap as the firmware


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


class Device:
    def __init__(self, url, key, device_id, token):
        self.url, self.key = url.rstrip("/"), key
        self.device = {"p_device_id": device_id, "p_token": token}

    def rpc(self, function, **args):
        body = json.dumps({**self.device, **args}).encode("utf-8")
        request = urllib.request.Request(self.url + "/rest/v1/rpc/" + function, data=body, method="POST",
                                         headers={"apikey": self.key, "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                text = response.read().decode("utf-8")
                return json.loads(text) if text else None
        except urllib.error.HTTPError as e:
            detail = e.read().decode("utf-8", "replace")
            try:
                detail = json.loads(detail).get("message") or detail
            except ValueError:
                pass
            raise RuntimeError(f"{e.code}: {detail}") from None


def now():
    return time.strftime("%H:%M:%S")


def main():
    props = read_local_properties()
    parser = argparse.ArgumentParser(description="Virtual ESP32 (soil moisture + pump) for AgriQuest")
    parser.add_argument("--device", default="AgriQuest-VIRTUAL")
    parser.add_argument("--token", default=os.environ.get("AGRIQUEST_DEVICE_TOKEN", ""),
                        help="device token from tools/new_device_token.py")
    parser.add_argument("--interval", type=float, default=10)
    parser.add_argument("--moisture", type=float, default=55)
    parser.add_argument("--dry-rate", type=float, default=0.8)
    parser.add_argument("--url", default=props.get("agriquest.supabaseUrl", ""))
    parser.add_argument("--key", default=props.get("agriquest.supabaseAnonKey") or props.get("agriquest.supabaseKey", ""))
    parser.add_argument("--plant-b", action="store_true",
                        help="also simulate Plant B's soil sensor and pump on slot 2 (link Plant B to this device in the app first)")
    parser.add_argument("--once", action="store_true", help=argparse.SUPPRESS)  # testing: stop after one pump run
    args = parser.parse_args()
    if not args.url.startswith(("https://", "http://localhost", "http://127.0.0.1")) or not args.key:
        sys.exit("Supabase URL/key not found. Add them to local.properties or pass --url and --key.")
    if not args.token:
        sys.exit("No device token. Run tools/new_device_token.py first, then pass --token.")

    esp = Device(args.url, args.key, args.device, args.token)
    # slot 1 = Plant A, slot 2 = Plant B (each has its own soil sensor and pump)
    slots = [1, 2] if args.plant_b else [1]
    moisture = {slot: args.moisture for slot in slots}
    label = {1: "", 2: "Plant B "}
    next_reading = 0.0
    print(f"{args.device}: a reading every {args.interval:g} s, pump check every {POLL_SECONDS} s. Ctrl+C to stop.\n")

    while True:
        if time.time() >= next_reading:
            for slot in slots:
                moisture[slot] = max(0.0, moisture[slot] - args.dry_rate + random.uniform(-0.2, 0.2))
                try:
                    extra = {"p_slot": 2} if slot == 2 else {}
                    reading_id = esp.rpc("submit_reading", p_soil_moisture=round(moisture[slot], 1), **extra)
                    print(f"{now()}  {label[slot]}reading #{reading_id} sent: soil moisture {moisture[slot]:.1f}%")
                except RuntimeError as e:
                    if "wrong token" in str(e) or "unassigned" in str(e).lower():
                        sys.exit(f"Supabase refused the device ({e}). Check --device/--token and that the SQL was run.")
                    print(f"{now()}  could not send {label[slot]}reading ({e})")
                except urllib.error.URLError as e:  # keep running through network blips, like the firmware
                    print(f"{now()}  could not send reading ({e.reason})")
            next_reading = time.time() + args.interval

        try:
            commands = esp.rpc("get_pending_commands") or []
        except (RuntimeError, urllib.error.URLError) as e:
            print(f"{now()}  could not check commands ({e})")
            commands = []

        for command in commands:
            slot = 2 if command.get("slot") == 2 else 1
            if command["actuator"] != "pump" or slot not in slots:
                esp.rpc("report_command", p_command_id=command["id"], p_success=False,
                        p_message="This actuator is not fitted on the device yet.")
                continue
            seconds = max(1, min(int(command["duration_s"]), MAX_PUMP_SECONDS))
            print(f"{now()}  >>> {label[slot]}PUMP ON for {seconds} s (command #{command['id']})")
            time.sleep(seconds)
            moisture[slot] = min(100.0, moisture[slot] + PUMP_GAIN_PER_SECOND * seconds)
            esp.rpc("report_command", p_command_id=command["id"], p_success=True)
            print(f"{now()}  <<< {label[slot]}pump off, reported done")
            next_reading = 0  # send a fresh reading right away so the app shows the change
            if args.once:
                esp.rpc("submit_reading", p_soil_moisture=round(moisture[slot], 1), **({"p_slot": 2} if slot == 2 else {}))
                print(f"{now()}  reading sent: soil moisture {moisture[slot]:.1f}%")
                return

        time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nStopped.")
