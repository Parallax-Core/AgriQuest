"""Placeholder outfit art, made to match the reference plant art (res/drawable/tomato_stages_*.png):
same pixel size (the outfit is drawn at 64x64 art pixels, 30% of the 210-px-tall stage images, never upscaled),
1-pixel dark outline, flat fills with a lighter highlight band. The team replaces these with final art of the same name.
Run: python3 scripts/generate-outfit-placeholders.py"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
S = 64

def new():
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)

def outline(img, color):
    """1-px outline around every filled shape, like the stage art."""
    px = img.load(); edge = []
    for y in range(S):
        for x in range(S):
            if px[x, y][3] == 0 and any(0 <= x + dx < S and 0 <= y + dy < S and px[x + dx, y + dy][3] > 0
                                         and px[x + dx, y + dy] != color for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for p in edge: px[p] = color
    return img

def save(img, name):
    img.save(os.path.join(OUT, name + '.png'))

os.makedirs(OUT, exist_ok=True)

# Straw hat: wide brim, crown, red band
img, d = new()
d.ellipse([4, 36, 59, 50], fill='#D9A441'); d.ellipse([8, 38, 55, 46], fill='#E9C46A')
d.rectangle([18, 18, 45, 41], fill='#E9C46A'); d.ellipse([18, 12, 45, 26], fill='#E9C46A')
d.rectangle([18, 32, 45, 37], fill='#C0392B'); d.line([22, 20, 22, 30], fill='#F6DE9A', width=2)
save(outline(img, (92, 60, 20, 255)), 'outfit_straw_hat')

# Garden cap
img, d = new()
d.pieslice([12, 16, 52, 56], 180, 360, fill='#3C7DC4'); d.rectangle([12, 36, 52, 40], fill='#3C7DC4')
d.ellipse([34, 36, 62, 46], fill='#2C5F96'); d.ellipse([29, 14, 35, 20], fill='#2C5F96')
d.line([20, 26, 26, 22], fill='#7FB1E6', width=2)
save(outline(img, (20, 40, 70, 255)), 'outfit_cap')

# Flower crown: leafy band with flowers like the stage art's yellow/pink blooms
img, d = new()
d.ellipse([6, 36, 58, 48], fill='#5C9E31'); d.ellipse([10, 39, 54, 45], fill=(0, 0, 0, 0))
for cx, c in ((14, '#F49AC1'), (32, '#F7D85A'), (50, '#F49AC1')):
    for ox, oy in ((-4, 0), (4, 0), (0, -4), (0, 4)):
        d.ellipse([cx + ox - 3, 36 + oy - 3, cx + ox + 3, 36 + oy + 3], fill=c)
    d.ellipse([cx - 2, 34, cx + 2, 38], fill='#E08A1E')
save(outline(img, (40, 70, 25, 255)), 'outfit_flower_crown')

# Harvest crown
img, d = new()
d.polygon([(10, 48), (10, 22), (20, 34), (32, 16), (44, 34), (54, 22), (54, 48)], fill='#F4C430')
d.rectangle([10, 42, 54, 48], fill='#D9A21E'); d.ellipse([28, 36, 36, 44], fill='#E63946')
d.ellipse([15, 38, 20, 43], fill='#3C7DC4'); d.ellipse([44, 38, 49, 43], fill='#3C7DC4')
d.line([14, 26, 14, 40], fill='#FBE58A', width=2)
save(outline(img, (110, 75, 10, 255)), 'outfit_crown')

# Sunglasses
img, d = new()
d.rectangle([4, 26, 60, 29], fill='#2B2B2B')
d.rounded_rectangle([6, 26, 29, 42], 5, fill='#2B2B2B'); d.rounded_rectangle([35, 26, 58, 42], 5, fill='#2B2B2B')
d.line([10, 30, 16, 30], fill='#8A8A8A', width=2); d.line([39, 30, 45, 30], fill='#8A8A8A', width=2)
save(outline(img, (10, 10, 10, 255)), 'outfit_sunglasses')

# Cozy scarf
img, d = new()
d.rounded_rectangle([6, 22, 58, 36], 6, fill='#D64541'); d.rectangle([36, 34, 46, 58], fill='#D64541')
for x in range(12, 58, 10): d.line([x, 24, x, 34], fill='#F28B82', width=3)
for y in range(40, 58, 8): d.line([37, y, 45, y], fill='#F28B82', width=3)
save(outline(img, (90, 20, 20, 255)), 'outfit_scarf')
print('placeholders written to', os.path.abspath(OUT))
