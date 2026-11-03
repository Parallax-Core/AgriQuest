/*
 * AgriQuest ESP32 firmware - step 1: send soil-moisture readings to Supabase.
 *
 * What it does
 *   - Joins Wi-Fi (name and password in secrets.h; Bluetooth pairing comes later).
 *   - Reads the capacitive soil-moisture sensor on GPIO 34 every READ_INTERVAL_MS
 *     and sends it to Supabase through the submit_reading database function
 *     (POST /rest/v1/rpc/submit_reading), the same call the virtual ESP32 uses.
 *   - Shows the moisture and the last result on the 16x2 I2C LCD.
 *
 * Until the soil sensor arrives, leave TEST_MODE at 1: it sends made-up values that
 * slowly dry out, so the whole path (ESP32 -> Wi-Fi -> Supabase -> app) can be tested.
 *
 * Security: uses only the publishable key plus this device's own token (never the
 * secret key), as the paper requires. HTTPS certificate checking is skipped
 * (setInsecure) to keep the prototype simple; list this as a limitation.
 *
 * Board: "ESP32 Dev Module" (Espressif ESP32 board package).
 * Library: "LiquidCrystal I2C" by Frank de Brabander (Arduino Library Manager).
 */

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "secrets.h"   // copy secrets.example.h to secrets.h and fill it in

// ----------------------------------------------------------------- settings
#define TEST_MODE 1                         // 1 = fake readings (no sensor yet), 0 = real sensor

const int SOIL_PIN = 34;                    // ADC1 pin: still works while Wi-Fi is on
const int LCD_SDA = 21;
const int LCD_SCL = 22;
const unsigned long READ_INTERVAL_MS = 10000;   // one reading every 10 s while testing

// Calibration for the real sensor (raw 0-4095 readings). Replace with your own:
// hold the sensor in dry air -> SOIL_DRY_RAW; dip it in water up to the line -> SOIL_WET_RAW.
const int SOIL_DRY_RAW = 3000;
const int SOIL_WET_RAW = 1300;

// ----------------------------------------------------------------- state
LiquidCrystal_I2C *lcd = nullptr;
unsigned long lastReadingAt = 0;
float fakeMoisture = 62.0;
bool autoPaused = false;   // 'p' in the Serial Monitor pauses / resumes automatic readings

// ----------------------------------------------------------------- LCD helpers
void showLine(int row, const String &text) {
  if (!lcd) return;
  String padded = text.substring(0, 16);
  while (padded.length() < 16) padded += ' ';
  lcd->setCursor(0, row);
  lcd->print(padded);
}

// Finds the LCD at 0x27 or 0x3F (the two usual backpack addresses).
void setupLcd() {
  Wire.begin(LCD_SDA, LCD_SCL);
  const uint8_t addresses[] = {0x27, 0x3F};
  for (uint8_t address : addresses) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {
      lcd = new LiquidCrystal_I2C(address, 16, 2);
      lcd->init();
      lcd->backlight();
      Serial.printf("LCD found at 0x%02X\n", address);
      return;
    }
  }
  Serial.println("No LCD found (check wiring); continuing without it.");
}

// ----------------------------------------------------------------- Wi-Fi
void connectWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  showLine(0, "WiFi connecting");
  showLine(1, WIFI_SSID);
  Serial.printf("Connecting to Wi-Fi \"%s\"", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long started = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - started < 20000) {
    delay(500);
    Serial.print('.');
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nWi-Fi connected, IP %s\n", WiFi.localIP().toString().c_str());
    showLine(0, "WiFi connected");
    showLine(1, WiFi.localIP().toString());
  } else {
    Serial.println("\nWi-Fi failed (wrong password, or not a 2.4 GHz network?). Retrying later.");
    showLine(0, "WiFi failed");
    showLine(1, "Retrying...");
  }
}

// ----------------------------------------------------------------- soil sensor
int readSoilRaw() {
  long total = 0;
  for (int i = 0; i < 10; i++) {           // average 10 samples to smooth noise
    total += analogRead(SOIL_PIN);
    delay(5);
  }
  return total / 10;
}

float readMoisturePercent() {
#if TEST_MODE
  fakeMoisture -= 0.6 + random(-20, 21) / 100.0;   // dries a little each reading
  if (fakeMoisture < 20) fakeMoisture = 70;        // "watered" again so the test keeps going
  return fakeMoisture;
#else
  int raw = readSoilRaw();
  Serial.printf("Soil raw value: %d\n", raw);      // use these to calibrate SOIL_DRY_RAW / SOIL_WET_RAW
  float percent = (float)(SOIL_DRY_RAW - raw) * 100.0 / (SOIL_DRY_RAW - SOIL_WET_RAW);
  return constrain(percent, 0.0, 100.0);
#endif
}

// ----------------------------------------------------------------- Supabase
// Sends one reading. Returns the HTTP status (200 = saved), or a negative number for a connection error.
int sendReading(float moisture) {
  WiFiClientSecure client;
  client.setInsecure();
  HTTPClient http;
  String url = String(SUPABASE_URL) + "/rest/v1/rpc/submit_reading";
  if (!http.begin(client, url)) return -1;
  http.setTimeout(15000);
  http.addHeader("apikey", SUPABASE_KEY);
  http.addHeader("Content-Type", "application/json");

  String body = String("{\"p_device_id\":\"") + DEVICE_ID +
                "\",\"p_token\":\"" + DEVICE_TOKEN +
                "\",\"p_soil_moisture\":" + String(moisture, 1) + "}";
  int status = http.POST(body);
  String reply = http.getString();
  http.end();

  if (status == 200) {
    Serial.printf("Saved reading #%s: soil %.1f%%\n", reply.c_str(), moisture);
  } else {
    // 401/403 with "Unknown device or wrong token" = DEVICE_ID or DEVICE_TOKEN in secrets.h is wrong
    Serial.printf("Supabase error %d: %s\n", status, reply.c_str());
  }
  return status;
}

// Sends one reading and shows the result on the LCD.
void sendAndShow(float moisture) {
  showLine(0, String("Soil ") + String(moisture, 1) + "%" + (TEST_MODE ? " TEST" : ""));
  int status = sendReading(moisture);
  if (status == 200) {
    showLine(1, autoPaused ? "Sent OK (paused)" : "Sent OK");
  } else if (status < 0) {
    showLine(1, "No server conn.");
  } else {
    showLine(1, String("Server err ") + status);
  }
}

// Serial Monitor commands (set the line ending to "Newline"):
//   a number such as 35  -> sends that soil moisture right away
//   p                    -> pauses / resumes the automatic readings
void handleSerialCommand() {
  if (!Serial.available()) return;
  String input = Serial.readStringUntil('\n');
  input.trim();
  if (input.length() == 0) return;

  if (input.equalsIgnoreCase("p")) {
    autoPaused = !autoPaused;
    Serial.println(autoPaused ? "Automatic readings paused. Type a number to send one." : "Automatic readings resumed.");
    showLine(1, autoPaused ? "Paused" : "Resumed");
    return;
  }

  float value = input.toFloat();
  bool isNumber = value != 0 || input == "0" || input.startsWith("0.");
  if (!isNumber || value < 0 || value > 100) {
    Serial.println("Type a moisture from 0 to 100, or p to pause/resume.");
    return;
  }
  Serial.printf("Sending custom reading: %.1f%%\n", value);
  fakeMoisture = value;          // in test mode, the automatic values continue from here
  lastReadingAt = millis();      // restart the 10 s timer
  sendAndShow(value);
}

// ----------------------------------------------------------------- main
void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println("\nAgriQuest ESP32 starting");
  analogReadResolution(12);
  randomSeed(esp_random());
  setupLcd();
  showLine(0, "AgriQuest");
  showLine(1, TEST_MODE ? "Test mode" : DEVICE_ID);
  delay(1500);
  connectWifi();
  Serial.println("Type a number (0-100) to send a custom reading, or p to pause automatic readings.");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWifi();
    if (WiFi.status() != WL_CONNECTED) {
      delay(5000);
      return;
    }
  }

  handleSerialCommand();

  if (!autoPaused && (lastReadingAt == 0 || millis() - lastReadingAt >= READ_INTERVAL_MS)) {
    lastReadingAt = millis();
    sendAndShow(readMoisturePercent());
  }
  delay(100);
}
