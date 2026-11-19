/*
 * AgriQuest ESP32 firmware: soil-moisture readings + pump commands through Supabase.
 *
 * What it does
 *   - Joins Wi-Fi (name and password in secrets.h; Bluetooth pairing comes later).
 *   - Every READ_INTERVAL_MS: reads the capacitive soil-moisture sensor on GPIO 34 and sends it
 *     with submit_reading (POST /rest/v1/rpc/submit_reading).
 *   - Every COMMAND_POLL_MS: asks Supabase for pending commands (get_pending_commands). A "pump"
 *     command switches the relay on GPIO 26 for the requested seconds, then the ESP32 reports the
 *     result (report_command). This is what the app's Water Plant button triggers.
 *     A "light" command switches the grow lamp (relay 3, GPIO 25) on for the requested seconds,
 *     or off when the duration is 1 s. "fan" commands are reported as not fitted (no fan yet).
 *   - Shows moisture, pump state and errors on the 16x2 I2C LCD.
 *
 * Safety on the ESP32 itself (the database checks run time and cooldown too, but hardware must not
 * rely on that alone):
 *   - The relay is switched OFF first thing at power-up.
 *   - A pump run is capped at MAX_PUMP_SECONDS even if a larger value arrives.
 *   - The relay always switches off when a run ends, before anything else happens.
 *
 * Until the soil sensor arrives, leave TEST_MODE at 1: it sends made-up values that slowly dry
 * out, and a pump run raises them, so the full app <-> ESP32 loop can be demonstrated.
 *
 * Security: uses only the publishable key plus this device's own token (never the secret key).
 * HTTPS certificate checking is skipped (setInsecure) to keep the prototype simple; list this as
 * a limitation (docs/esp32-pairing.md asks for verified certificates in production).
 *
 * Board: "ESP32 Dev Module" (Espressif ESP32 board package).
 * Library: "LiquidCrystal I2C" by Frank de Brabander (Arduino Library Manager).
 *
 * Serial Monitor (115200 baud, line ending "Newline"):
 *   a number (0-100)  send that soil moisture right away (testing)
 *   p                 pause / resume automatic readings
 *   w                 run the pump for 3 s locally (wiring test, no server)
 *   c                 calibration reading: average of 20 raw readings over 10 s (see the calibration guide)
 *   l or lamp         grow lamp on / off (local test, stays as set until you type l again)
 */

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <esp_task_wdt.h>
#include <Preferences.h>
#include "secrets.h"   // copy secrets.example.h to secrets.h and fill it in

// ----------------------------------------------------------------- settings
#define TEST_MODE 0                        // 1 = fake readings (no sensor yet), 0 = real sensor

const int SOIL_PIN = 34;                    // ADC1 pin: still works while Wi-Fi is on
const int RELAY_PIN = 26;                   // relay module IN1: water pump
const int LIGHT_PIN = 25;                   // relay module IN3: grow lamp
const int LCD_SDA = 21;
const int LCD_SCL = 22;

// Most blue relay modules switch ON when IN is LOW ("low-level trigger"). Use the value that
// worked in firmware/relay_test/relay_test.ino.
const bool RELAY_ACTIVE_LOW = true;

const unsigned long READ_INTERVAL_MS = 10000;   // one reading every 10 s
const unsigned long COMMAND_POLL_MS = 5000;     // check for pump commands every 5 s
const int MAX_PUMP_SECONDS = 10;                // hard local limit per run
const float TEST_PUMP_GAIN_PER_SECOND = 4.0;
const uint32_t WATCHDOG_MS = 40000;             // restart automatically if the program freezes this long    // test mode: % moisture added per pump second

// Calibration for the real sensor (raw 0-4095 readings). Replace with your own:
// hold the sensor in dry air -> SOIL_DRY_RAW; dip it in water up to the line -> SOIL_WET_RAW.
   const int SOIL_DRY_RAW = 820; // dry
const int SOIL_WET_RAW = 0;   // your in-water number

// ----------------------------------------------------------------- state
LiquidCrystal_I2C *lcd = nullptr;
Preferences memory;                        // survives restarts: remembers a pump run not yet reported
unsigned long lastReadingAt = 0;
unsigned long lastCommandPollAt = 0;
float fakeMoisture = 62.0;
bool autoPaused = false;

// ----------------------------------------------------------------- relay (pump)
void switchOff(int pin) {
  if (RELAY_ACTIVE_LOW) {
    // Let the pin float: the module's own pull-up turns the relay fully off.
    // (A 3.3 V HIGH from the ESP32 can leave some 5 V modules half-on.)
    pinMode(pin, INPUT);
  } else {
    pinMode(pin, OUTPUT);
    digitalWrite(pin, LOW);
  }
}

void switchOn(int pin) {
  pinMode(pin, OUTPUT);
  digitalWrite(pin, RELAY_ACTIVE_LOW ? LOW : HIGH);
}

void relayOff() { switchOff(RELAY_PIN); }

// ----------------------------------------------------------------- grow lamp (relay 3)
bool lampOn = false;
unsigned long lampOffAt = 0;               // millis() when a timed lamp command ends; 0 = stays as set

void setLamp(bool on, unsigned long seconds = 0) {
  lampOn = on;
  if (on) switchOn(LIGHT_PIN); else switchOff(LIGHT_PIN);
  lampOffAt = (on && seconds > 0) ? millis() + seconds * 1000UL : 0;
  if (on && seconds > 0) Serial.printf("LAMP ON for %lu s\n", seconds);
  else Serial.println(on ? "LAMP ON" : "LAMP OFF");
}

// Switches a timed lamp off when its time is up (called from loop, so nothing blocks).
void checkLampTimer() {
  if (lampOn && lampOffAt != 0 && (long)(millis() - lampOffAt) >= 0) setLamp(false);
}

void relayOn() { switchOn(RELAY_PIN); }

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
  Wire.setTimeOut(50);                     // never wait forever on the LCD (motor noise can disturb the bus)
  const uint8_t addresses[] = {0x27, 0x3F, 0x27, 0x3F, 0x27, 0x3F};   // a few tries
  for (uint8_t address : addresses) {
    delay(50);
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
    esp_task_wdt_reset();
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
  if (fakeMoisture < 20) fakeMoisture = 20;        // stays dry until watered
  return fakeMoisture;
#else
  int raw = readSoilRaw();
  Serial.printf("Soil raw value: %d\n", raw);      // use these to calibrate SOIL_DRY_RAW / SOIL_WET_RAW
  float percent = (float)(SOIL_DRY_RAW - raw) * 100.0 / (SOIL_DRY_RAW - SOIL_WET_RAW);
  return constrain(percent, 0.0, 100.0);
#endif
}

// ----------------------------------------------------------------- Supabase
// Calls a database function (POST /rest/v1/rpc/<function>) with a JSON body.
// Returns the HTTP status (200/204 = OK) or a negative number for a connection error.
int callRpc(const char *function, const String &body, String &reply) {
  WiFiClientSecure client;
  client.setInsecure();
  client.setHandshakeTimeout(10);          // seconds; the default can block for 2 minutes
  HTTPClient http;
  String url = String(SUPABASE_URL) + "/rest/v1/rpc/" + function;
  if (!http.begin(client, url)) return -1;
  http.setConnectTimeout(8000);
  http.setTimeout(10000);
  http.setReuse(false);                    // "Connection: close", so the server ends the reply cleanly
  http.addHeader("apikey", SUPABASE_KEY);
  http.addHeader("Content-Type", "application/json");
  int status = http.POST(body);
  // report_command answers "204 No Content". Reading a body that never comes used to freeze the ESP32 here.
  reply = (status > 0 && status != 204) ? http.getString() : String("");
  http.end();
  return status;
}

String deviceJson() {
  return String("\"p_device_id\":\"") + DEVICE_ID + "\",\"p_token\":\"" + DEVICE_TOKEN + "\"";
}

// Sends one reading. Returns the HTTP status (200 = saved).
int sendReading(float moisture) {
  String reply;
  int status = callRpc("submit_reading", String("{") + deviceJson() +
                       ",\"p_soil_moisture\":" + String(moisture, 1) + "}", reply);
  if (status == 200) {
    Serial.printf("Saved reading #%s: soil %.1f%%\n", reply.c_str(), moisture);
  } else {
    // 400/401/403 with "Unknown device or wrong token" = DEVICE_ID or DEVICE_TOKEN in secrets.h is wrong;
    // "Device is unassigned" = the device isn't linked to a plant any more.
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

bool reportCommand(long id, bool success, const char *message) {
  String reply;
  String body = String("{") + deviceJson() + ",\"p_command_id\":" + String(id) +
                ",\"p_success\":" + (success ? "true" : "false");
  if (message) body += String(",\"p_message\":\"") + message + "\"";
  body += "}";
  for (int attempt = 1; attempt <= 3; attempt++) {   // the app waits for this, so retry if Wi-Fi blipped
    esp_task_wdt_reset();
    Serial.printf("Reporting command #%ld to Supabase (try %d)...\n", id, attempt);
    int status = callRpc("report_command", body, reply);
    if (status == 200 || status == 204) {
      Serial.printf("Reported command #%ld as %s\n", id, success ? "done" : "failed");
      memory.remove("unreported");
      return true;
    }
    if (reply.indexOf("No running command") >= 0) {   // already recorded (or timed out) on the server
      Serial.printf("Command #%ld was already closed in Supabase\n", id);
      memory.remove("unreported");
      return true;
    }
    Serial.printf("Could not report command #%ld (error %d): %s\n", id, status, reply.c_str());
    delay(1000);
  }
  return false;
}

// After a restart: report a pump run that finished but whose "done" never reached Supabase.
void reportLeftoverCommand() {
  long id = memory.getLong("unreported", -1);
  if (id < 0) return;
  Serial.printf("Command #%ld ran before the restart; reporting it now\n", id);
  reportCommand(id, true, "Reported after the ESP32 restarted.");
  memory.remove("unreported");             // report once; Supabase times it out otherwise
}

// Runs the pump for the given seconds (capped), with the relay off again afterwards.
void runPump(int seconds) {
  seconds = constrain(seconds, 1, MAX_PUMP_SECONDS);
  Serial.printf("PUMP ON for %d s\n", seconds);
  showLine(0, String("PUMP ON ") + seconds + "s");
  relayOn();
  for (int left = seconds; left > 0; left--) {
    esp_task_wdt_reset();
    showLine(1, String("Watering... ") + left);
    delay(1000);
  }
  relayOff();
  Serial.println("PUMP OFF");
  delay(300);                              // let the motor's switch-off spike settle
  if (lcd) { lcd->init(); lcd->backlight(); }   // redraw cleanly in case the spike garbled the LCD
  showLine(0, "Pump off");
  showLine(1, "");
  Serial.println("LCD OK after pump");
#if TEST_MODE
  fakeMoisture = min(100.0f, fakeMoisture + TEST_PUMP_GAIN_PER_SECOND * seconds);
#endif
}

// Reads a number field such as "duration_s":5 from one command object.
long jsonNumber(const String &object, const char *key) {
  String pattern = String("\"") + key + "\":";
  int at = object.indexOf(pattern);
  if (at < 0) return -1;
  return object.substring(at + pattern.length()).toInt();
}

// Reads a text field such as "actuator":"pump" from one command object.
String jsonText(const String &object, const char *key) {
  String pattern = String("\"") + key + "\":\"";
  int at = object.indexOf(pattern);
  if (at < 0) return "";
  int start = at + pattern.length();
  int end = object.indexOf('"', start);
  return end < 0 ? "" : object.substring(start, end);
}

// Asks Supabase for commands waiting for this device and carries them out.
void checkCommands() {
  String reply;
  int status = callRpc("get_pending_commands", String("{") + deviceJson() + "}", reply);
  if (status != 200) {
    if (status > 0) Serial.printf("Command check error %d: %s\n", status, reply.c_str());
    return;
  }
  // The reply is a JSON list of flat objects: [{"id":12,"actuator":"pump","duration_s":5,...}]
  int from = 0;
  while (true) {
    int open = reply.indexOf('{', from);
    if (open < 0) break;
    int close = reply.indexOf('}', open);
    if (close < 0) break;
    String command = reply.substring(open, close + 1);
    from = close + 1;

    long id = jsonNumber(command, "id");
    String actuator = jsonText(command, "actuator");
    int seconds = (int)jsonNumber(command, "duration_s");
    if (id < 0) continue;

    if (actuator == "pump") {
      memory.putLong("unreported", id);    // if the pump's spike freezes the ESP32, report it after the restart
      runPump(seconds);
      reportCommand(id, true, nullptr);
      lastReadingAt = 0;   // send a fresh reading right away so the app shows the change
    } else if (actuator == "light") {
      // The app's Light button: on for duration_s seconds, or OFF when duration_s is 1 (the database
      // needs at least 1 s). Reporting "done" right after switching is the app's confirmation.
      if (seconds <= 1) {
        setLamp(false);
        showLine(1, "Lamp OFF");
      } else {
        setLamp(true, seconds);              // runs in the background; loop() switches it off
        showLine(1, String("Lamp on ") + seconds + "s");
      }
      reportCommand(id, true, nullptr);
    } else {
      Serial.printf("Command #%ld for \"%s\" ignored: not fitted on this device\n", id, actuator.c_str());
      reportCommand(id, false, "This actuator is not fitted on the device yet.");
    }
  }
}

// ----------------------------------------------------------------- Serial Monitor commands
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
  if (input.equalsIgnoreCase("c")) {     // calibration: 20 readings over ~10 s, printed as one line
    const int n = 20;
    long sum = 0, sumSq = 0;
    int lowest = 4095, highest = 0;
    Serial.println("Calibration reading: keep the sensor still for 10 s...");
    for (int i = 0; i < n; i++) {
      esp_task_wdt_reset();
      int raw = readSoilRaw();
      sum += raw; sumSq += (long)raw * raw;
      lowest = min(lowest, raw); highest = max(highest, raw);
      delay(500);
    }
    float mean = (float)sum / n;
    float sd = sqrt(max(0.0f, (float)sumSq / n - mean * mean));
    Serial.printf("CAL  mean=%.1f  sd=%.1f  min=%d  max=%d  (n=%d)\n", mean, sd, lowest, highest, n);
    showLine(0, String("CAL raw ") + String(mean, 0));
    showLine(1, String("sd ") + String(sd, 1));
    return;
  }
  if (input.equalsIgnoreCase("l") || input.equalsIgnoreCase("lamp")) {   // letter L, not the number 1
    setLamp(!lampOn);
    showLine(1, lampOn ? "Lamp ON" : "Lamp OFF");
    return;
  }
  if (input.equalsIgnoreCase("w")) {
    Serial.println("Local pump test (not reported to the server)");
    runPump(3);
    return;
  }

  float value = input.toFloat();
  bool isNumber = value != 0 || input == "0" || input.startsWith("0.");
  if (!isNumber || value < 0 || value > 100) {
    Serial.println("Type a moisture from 0 to 100, p to pause/resume, w for a 3 s pump test, lamp to switch the lamp, or c to take a calibration reading.");
    return;
  }
  Serial.printf("Sending custom reading: %.1f%%\n", value);
  fakeMoisture = value;          // in test mode, the automatic values continue from here
  lastReadingAt = millis();      // restart the reading timer
  sendAndShow(value);
}

// ----------------------------------------------------------------- main
// Shows why the ESP32 last restarted: a "brownout" or "watchdog" right after the pump points to motor noise.
void printResetReason() {
  switch (esp_reset_reason()) {
    case ESP_RST_BROWNOUT: Serial.println("Restarted after a BROWNOUT (power dipped - check pump wiring/diode)"); break;
    case ESP_RST_TASK_WDT:
    case ESP_RST_INT_WDT:
    case ESP_RST_WDT:      Serial.println("Restarted by the WATCHDOG (the program had frozen)"); break;
    case ESP_RST_PANIC:    Serial.println("Restarted after a crash"); break;
    default: break;
  }
}

void setup() {
  relayOff();                    // pump off before anything else
  switchOff(LIGHT_PIN);          // lamp off too
  Serial.begin(115200);
  delay(300);
  Serial.println("\nAgriQuest ESP32 starting");
  printResetReason();

  // Watchdog: if loop() stops running for WATCHDOG_MS, the ESP32 restarts by itself (pump off at boot).
  esp_task_wdt_config_t watchdog = {.timeout_ms = WATCHDOG_MS, .idle_core_mask = 0, .trigger_panic = true};
  if (esp_task_wdt_reconfigure(&watchdog) != ESP_OK) esp_task_wdt_init(&watchdog);
  esp_task_wdt_add(NULL);
  analogReadResolution(12);
  randomSeed(esp_random());
  setupLcd();
  showLine(0, "AgriQuest");
  showLine(1, TEST_MODE ? "Test mode" : DEVICE_ID);
  delay(1500);
  memory.begin("agriquest", false);
  connectWifi();
  if (WiFi.status() == WL_CONNECTED) reportLeftoverCommand();
  Serial.println("Type a number (0-100) to send a reading, p to pause, w to test the pump.");
}

void loop() {
  esp_task_wdt_reset();
  if (WiFi.status() != WL_CONNECTED) {
    relayOff();
    connectWifi();
    if (WiFi.status() != WL_CONNECTED) {
      delay(5000);
      return;
    }
  }

  handleSerialCommand();
  checkLampTimer();

  if (millis() - lastCommandPollAt >= COMMAND_POLL_MS || lastCommandPollAt == 0) {
    lastCommandPollAt = millis();
    checkCommands();
  }

  if (!autoPaused && (lastReadingAt == 0 || millis() - lastReadingAt >= READ_INTERVAL_MS)) {
    lastReadingAt = millis();
    sendAndShow(readMoisturePercent());
  }
  delay(100);
}
