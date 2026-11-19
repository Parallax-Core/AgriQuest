// Copy this file to secrets.h (same folder) and fill in your values.
// secrets.h is in .gitignore, so your Wi-Fi password and device token are never pushed to GitHub.

#pragma once

// Your Wi-Fi. The ESP32 only connects to 2.4 GHz networks.
#define WIFI_SSID     "YourWiFiName"
#define WIFI_PASSWORD "YourWiFiPassword"

// Same values as agriquest.supabaseUrl / agriquest.supabaseAnonKey in local.properties
// (the publishable key - never the secret key).
#define SUPABASE_URL  "https://xxxx.supabase.co"
#define SUPABASE_KEY  "sb_publishable_..."

// From: python tools/new_device_token.py --email you@example.com --device AgriQuest-01
// (it also prints the SQL that registers the token in Supabase)
#define DEVICE_ID     "AgriQuest-01"
#define DEVICE_TOKEN  "paste-the-token-here"
