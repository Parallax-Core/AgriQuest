"""Generates AgriQuest's original game sounds (no third-party audio). Run: python3 scripts/generate-sounds.py"""
import math, random, struct, wave, os

RATE = 22050
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'raw')
random.seed(7)

def save(name, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = 0.85 / peak
    with wave.open(os.path.join(OUT, name + '.wav'), 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, s * scale)) * 32767)) for s in samples))

def env(i, n, attack=0.01, release=0.2):
    t = i / RATE; dur = n / RATE
    a = min(1, t / attack) if attack else 1
    r = min(1, (dur - t) / release) if release else 1
    return max(0, a * r)

def tone(freq, dur, vol=1.0, shape='sine', attack=0.005, release=0.1):
    n = int(dur * RATE); out = []
    for i in range(n):
        ph = 2 * math.pi * freq * i / RATE
        v = math.sin(ph) if shape == 'sine' else (1 if math.sin(ph) > 0 else -1) * 0.5
        out.append(v * vol * env(i, n, attack, release))
    return out

def mix(*tracks):
    n = max(len(t) for t in tracks); out = [0.0] * n
    for t in tracks:
        for i, v in enumerate(t): out[i] += v
    return out

def concat(*parts):
    out = []
    for p in parts: out += p
    return out

def lowpass(samples, k):
    out, y = [], 0.0
    for s in samples:
        y += k * (s - y); out.append(y)
    return out

# Water: soft splashing noise with random droplet blips
n = int(1.6 * RATE)
noise = lowpass([random.uniform(-1, 1) for _ in range(n)], 0.25)
water = [noise[i] * 0.5 * env(i, n, 0.08, 0.5) for i in range(n)]
for _ in range(14):
    start = random.randint(0, n - 3000); f = random.uniform(700, 1400)
    for i in range(1800):
        water[start + i] += math.sin(2 * math.pi * (f + i * 0.6) * i / RATE) * 0.35 * math.exp(-i / 300)
save('sfx_water', water)

# Fan: rising and falling whoosh
n = int(1.4 * RATE)
raw = [random.uniform(-1, 1) for _ in range(n)]
fan = []
y = 0.0
for i, s in enumerate(raw):
    k = 0.05 + 0.25 * math.sin(math.pi * i / n)
    y += k * (s - y)
    fan.append(y * math.sin(math.pi * i / n) * (0.8 + 0.2 * math.sin(2 * math.pi * 9 * i / RATE)))
save('sfx_fan', fan)

# Light: click plus a bright two-note chime
click = [random.uniform(-1, 1) * math.exp(-i / 60) for i in range(800)]
chime = concat(tone(1046.5, 0.12, 0.5), tone(1568, 0.35, 0.5, release=0.3))
save('sfx_light', mix(click, [0.0] * 600 + chime))

# Success: happy arpeggio C-E-G-C
success = concat(*[tone(f, 0.11, 0.6, 'square', release=0.05) for f in (523.25, 659.25, 783.99)],
                 tone(1046.5, 0.45, 0.6, 'square', release=0.35))
save('sfx_success', success)

# Tap: tiny blip
save('sfx_tap', tone(880, 0.06, 0.5, 'square', release=0.04))

# Ambient garden loop (8 s): soft pad + occasional bird chirps, fades at the ends so it loops cleanly
n = 8 * RATE
pad = [0.12 * (math.sin(2 * math.pi * 196 * i / RATE) + 0.6 * math.sin(2 * math.pi * 293.66 * i / RATE)
       + 0.4 * math.sin(2 * math.pi * 392 * i / RATE)) * (0.6 + 0.4 * math.sin(2 * math.pi * i / n)) for i in range(n)]
for start in (int(1.2 * RATE), int(3.9 * RATE), int(6.1 * RATE)):
    for c in range(3):
        s0 = start + c * 2600
        for i in range(2000):
            f = 2400 + 900 * math.sin(math.pi * i / 2000)
            pad[s0 + i] += 0.18 * math.sin(2 * math.pi * f * i / RATE) * math.sin(math.pi * i / 2000)
fade = int(0.3 * RATE)
for i in range(fade):
    pad[i] *= i / fade; pad[-1 - i] *= i / fade
save('ambient_garden', pad)
print('sounds written to', os.path.abspath(OUT))
