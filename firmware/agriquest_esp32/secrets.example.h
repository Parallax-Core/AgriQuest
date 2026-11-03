// Copy this file to secrets.h (same folder) and fill in your values.
// secrets.h is in .gitignore, so your Wi-Fi password and device token are never pushed to GitHub.

#pragma once

// Your Wi-Fi. The ESP32 only connects to 2.4 GHz networks.
#define WIFI_SSID     "YourWiFiName"
#define WIFI_PASSWORD "YourWiFiPassword"

// Same values as agriquest.supabaseUrl / agriquest.supabaseKey in local.properties
// (the publishable key - never the secret key).
#define SUPABASE_URL  "https://xxxx.supabase.co"
#define SUPABASE_KEY  "sb_publishable_..."

// From: python tools/register_device.py --email you@example.com --password ... --device AgriQuest-01
#define DEVICE_ID     "AgriQuest-01"
#define DEVICE_TOKEN  "b0b5dec339b14cb2a9d68c8fe91426a0bfcc4f21a8834510ab3fe6f21b4b05a8"
