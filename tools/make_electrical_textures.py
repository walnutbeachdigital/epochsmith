"""Draws placeholder art for the Electrical era. Run from the project root:
    python3 tools/make_electrical_textures.py"""
import math

from pixelart import write_png, from_map

COPPER = [(126, 66, 40, 255), (176, 96, 58, 255), (214, 136, 92, 255), (232, 172, 128, 255)]
IRON_DARK = (46, 48, 54, 255)
IRON = (84, 88, 96, 255)
IRON_LIGHT = (124, 128, 136, 255)
BAKELITE = (52, 34, 26, 255)
BAKELITE_LIGHT = (86, 58, 42, 255)


def wire():
    """Copper wire wrapped in dark cloth insulation, with a bare copper stripe."""
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            if 6 <= y <= 9:
                row.append(COPPER[3] if y == 7 else COPPER[2])
            else:
                row.append(BAKELITE_LIGHT if (x + y) % 4 == 0 else BAKELITE)
        px.append(row)
    return px


def dynamo_side():
    """An iron frame with a copper coil showing through."""
    px = [[IRON for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = IRON_LIGHT
        px[15][i] = px[i][15] = IRON_DARK
    for y in range(3, 13):
        for x in range(3, 13):
            px[y][x] = COPPER[1 + (y % 2)] if (x + y // 2) % 3 else COPPER[0]
    return px


def dynamo_top():
    px = [[IRON for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2:
                px[y][x] = (190, 194, 200, 255)
            elif d < 5:
                px[y][x] = COPPER[2] if int(d * 2) % 2 else COPPER[1]
            elif x in (0, 15) or y in (0, 15):
                px[y][x] = IRON_DARK
    return px


def battery_side(level):
    """A dark cell with four charge bars on the front; 'level' of them are lit (0-4)."""
    px = [[BAKELITE for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[15][i] = IRON_LIGHT
        px[i][0] = px[i][15] = IRON_DARK
    for bar in range(4):
        y0 = 12 - bar * 3
        on = bar < level
        colour = [(80, 220, 110, 255), (60, 180, 90, 255)] if on else [(40, 44, 40, 255), (30, 32, 30, 255)]
        for y in (y0, y0 + 1):
            for x in range(4, 12):
                px[y][x] = colour[0] if y == y0 else colour[1]
    return px


def battery_top():
    px = [[IRON for _ in range(16)] for _ in range(16)]
    for (cx, cy, colour) in ((4, 5, (200, 60, 50, 255)), (11, 5, (50, 50, 60, 255))):
        for y in range(cy - 1, cy + 2):
            for x in range(cx - 1, cx + 2):
                px[y][x] = colour
    for i in range(16):
        px[0][i] = px[15][i] = px[i][0] = px[i][15] = IRON_DARK
    return px


def lamp(lit):
    """A glass globe in an iron cage."""
    glow = (255, 236, 160, 255) if lit else (150, 150, 140, 255)
    centre = (255, 252, 230, 255) if lit else (110, 110, 104, 255)
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if x in (0, 15) or y in (0, 15) or x == 7 or y == 7:
                row.append(IRON_DARK)
            elif d < 3:
                row.append(centre)
            else:
                row.append(glow)
        px.append(row)
    return px


BULB = {"O": (60, 60, 64, 255), "G": (220, 230, 236, 255), "g": (180, 196, 204, 255),
        "F": (255, 200, 90, 255), "C": (176, 96, 58, 255), "c": (126, 66, 40, 255)}
LIGHT_BULB = [
    "................",
    ".....OOOOOO.....",
    "....OGGGGGgO....",
    "...OGGGFFGGgO...",
    "...OGGF..FGgO...",
    "...OGGF..FGgO...",
    "...OGGGFFGGgO...",
    "....OGGFFGgO....",
    ".....OGFFgO.....",
    "......OFFO......",
    "......OCCO......",
    "......OccO......",
    "......OCCO......",
    "......OccO......",
    ".......OO.......",
]

root = "src/main/resources/assets/epochsmith/textures"
write_png(f"{root}/block/copper_wire.png", wire())
write_png(f"{root}/block/dynamo_side.png", dynamo_side())
write_png(f"{root}/block/dynamo_top.png", dynamo_top())
for level in range(5):
    write_png(f"{root}/block/battery_side_{level}.png", battery_side(level))
write_png(f"{root}/block/battery_top.png", battery_top())
write_png(f"{root}/block/electric_lamp.png", lamp(False))
write_png(f"{root}/block/electric_lamp_on.png", lamp(True))
write_png(f"{root}/item/light_bulb.png", from_map(LIGHT_BULB, BULB))
print("electrical textures written")
