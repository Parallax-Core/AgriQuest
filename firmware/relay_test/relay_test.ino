/*
 * AgriQuest relay + pump test.
 * Wiring: relay VCC -> VIN/5V, GND -> GND, IN -> GPIO 26.
 *         12V+ -> relay COM, relay NO -> pump +, pump - -> 12V-.
 * Open the Serial Monitor at 115200 baud (line ending "Newline") and type:
 *   w  -> pump on for 3 seconds
 *   t  -> relay click test with the 12V adapter UNPLUGGED (on 1 s, off)
 */

const int RELAY_PIN = 26;

// Most blue relay modules switch ON when IN is pulled LOW ("low-level trigger").
// If the relay does the opposite of what you expect, change this to false.
const bool RELAY_ACTIVE_LOW = true;

const int PUMP_SECONDS = 3;

void relayOn() {
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, RELAY_ACTIVE_LOW ? LOW : HIGH);
}

void relayOff() {
  if (RELAY_ACTIVE_LOW) {
    // Let the pin float: the module's own pull-up turns the relay fully off.
    // (A 3.3 V HIGH from the ESP32 can leave some 5 V modules half-on.)
    pinMode(RELAY_PIN, INPUT);
  } else {
    pinMode(RELAY_PIN, OUTPUT);
    digitalWrite(RELAY_PIN, LOW);
  }
}

void setup() {
  relayOff();                       // make sure the pump is off at power-up
  Serial.begin(115200);
  delay(300);
  Serial.println("\nRelay test ready. Type w (pump 3 s) or t (click test), then Enter.");
}

void loop() {
  if (!Serial.available()) return;
  String cmd = Serial.readStringUntil('\n');
  cmd.trim();

  if (cmd.equalsIgnoreCase("t")) {
    Serial.println("Relay ON (you should hear a click and see its LED light)");
    relayOn();
    delay(1000);
    relayOff();
    Serial.println("Relay OFF (click again, LED off)");
  } else if (cmd.equalsIgnoreCase("w")) {
    Serial.printf("Pump ON for %d s\n", PUMP_SECONDS);
    relayOn();
    delay(PUMP_SECONDS * 1000UL);
    relayOff();
    Serial.println("Pump OFF");
  } else if (cmd.length() > 0) {
    Serial.println("Type w (pump 3 s) or t (click test).");
  }
}
