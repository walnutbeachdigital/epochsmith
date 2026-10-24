"""Draws Epochsmith's placeholder tin textures (16x16 PNGs). Run from the project root:
    python3 tools/make_tin_textures.py
Uses only the Python standard library."""
import random

from pixelart import write_png, from_map as _from_map, INGOT

TIN = {"O": (70, 74, 82, 255), "D": (140, 146, 156, 255), "M": (185, 190, 198, 255), "H": (232, 236, 240, 255)}

def stone(seed, shades):
    rnd = random.Random(seed)
    return [[shades[rnd.randrange(len(shades))] + (255,) for _ in range(16)] for _ in range(16)]

# Speck clusters: (top-left x, y) and a small shape drawn in tin colors
CLUSTERS = [(2, 2), (9, 3), (4, 8), (11, 10), (1, 12)]
SHAPE = ["DM.", "MHD", ".DD"]

def ore(seed, shades):
    px = stone(seed, shades)
    for cx, cy in CLUSTERS:
        for dy, line in enumerate(SHAPE):
            for dx, ch in enumerate(line):
                if ch != ".":
                    px[cy + dy][cx + dx] = TIN[ch]
    return px

def from_map(rows):
    return _from_map(rows, TIN)

STONE = [(112, 112, 112), (125, 125, 125), (134, 134, 134), (118, 118, 118)]
DEEPSLATE = [(70, 70, 76), (78, 78, 84), (62, 62, 68), (86, 86, 92)]

RAW_TIN = [
    "................",
    "................",
    ".....OOOO.......",
    "....OHHMMO......",
    "...OHMMMMDOO....",
    "...OMMHMMMMDO...",
    "..OHMMMMMMMMDO..",
    "..OMMMMHMMMMDO..",
    "..OMMMMMMMMDDO..",
    "...ODMMMMMDDO...",
    "....ODDDDDDO....",
    ".....OOOOOO.....",
]
TIN_INGOT = INGOT

base = "src/main/resources/assets/epochsmith/textures"
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/block/tin_ore.png", ore(1, STONE))
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/block/deepslate_tin_ore.png", ore(2, DEEPSLATE))
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/item/raw_tin.png", from_map(RAW_TIN))
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/item/tin_ingot.png", from_map(TIN_INGOT))
print("tin textures written")
