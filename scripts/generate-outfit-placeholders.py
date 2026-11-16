"""Placeholder outfit art (64x64 pixel PNGs). The team replaces these files with final art of the same name."""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
os.makedirs(OUT, exist_ok=True)

def canvas():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def save(img, name):
    img.resize((64, 64), Image.NEAREST).save(os.path.join(OUT, name + '.png'))

def rects(img, items):
    d = ImageDraw.Draw(img)
    for (x0, y0, x1, y1, c) in items:
        d.rectangle([x0, y0, x1, y1], fill=c)
    return img

STRAW, STRAW_D, RED, BLUE, BLUE_D, BLACK, GOLD, GOLD_D, GEM, PINK, LEAF = (
    '#E9C46A', '#B98B2E', '#D62828', '#2F6FB0', '#1E4F82', '#1D1D1D', '#F4C430', '#B8860B', '#E63946', '#F49AC1', '#4E8A2E')
save(rects(canvas(), [(1, 10, 14, 11, STRAW_D), (4, 5, 11, 10, STRAW), (4, 8, 11, 8, RED)]), 'outfit_straw_hat')
save(rects(canvas(), [(3, 6, 11, 10, BLUE), (9, 9, 15, 10, BLUE_D), (6, 5, 8, 5, BLUE_D)]), 'outfit_cap')
save(rects(canvas(), [(1, 7, 14, 7, BLACK), (2, 7, 6, 10, BLACK), (9, 7, 13, 10, BLACK), (3, 8, 4, 8, '#555555'), (10, 8, 11, 8, '#555555')]), 'outfit_sunglasses')
save(rects(canvas(), [(2, 6, 13, 9, RED), (9, 9, 11, 14, RED), (2, 7, 13, 7, '#F77F7F'), (10, 12, 10, 12, '#F77F7F')]), 'outfit_scarf')
save(rects(canvas(), [(3, 8, 12, 11, GOLD), (3, 5, 4, 8, GOLD), (7, 4, 8, 8, GOLD), (11, 5, 12, 8, GOLD), (3, 11, 12, 11, GOLD_D), (7, 9, 8, 10, GEM)]), 'outfit_crown')
save(rects(canvas(), [(2, 9, 13, 10, LEAF), (2, 7, 4, 9, PINK), (6, 6, 9, 8, '#FFD166'), (11, 7, 13, 9, PINK)]), 'outfit_flower_crown')
print('placeholders written to', os.path.abspath(OUT))
