"""Draws placeholder art for the Industrial era. Run from the project root:
    python3 tools/make_industrial_textures.py"""
import random

from pixelart import write_png, from_map

IRON_DARK = (46, 48, 54, 255)
IRON = (84, 88, 96, 255)
IRON_LIGHT = (124, 128, 136, 255)
RIVET = (170, 174, 180, 255)
BRASS = (196, 156, 72, 255)
BRASS_DARK = (138, 104, 40, 255)
BRICK = [(150, 74, 56, 255), (136, 66, 50, 255)]
MORTAR = (110, 100, 92, 255)


def riveted(seed):
    """Dark iron plates with seams and rivets: the boiler's shell."""
    rnd = random.Random(seed)
    px = [[(IRON if rnd.random() > 0.2 else IRON_LIGHT) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[7][i] = IRON_DARK
        px[i][0] = IRON_DARK
        px[i][15] = IRON_DARK
    for x in (2, 6, 10, 14):
        px[5][x] = RIVET
        px[9][x] = RIVET
    for y in (12, 13):
        for x in range(16):
            px[y][x] = BRASS if y == 12 else BRASS_DARK
    return px


def boiler_top():
    px = riveted(4)
    for y in range(5, 11):
        for x in range(5, 11):
            edge = y in (5, 10) or x in (5, 10)
            px[y][x] = IRON_DARK if edge else (30, 30, 34, 255)
    return px


def bricks():
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            offset = 4 if (y // 4) % 2 else 0
            if y % 4 == 3 or (x + offset) % 8 == 7:
                row.append(MORTAR)
            else:
                row.append(BRICK[((x + offset) // 8 + y // 4) % 2])
        px.append(row)
    return px


def coke_oven_front(lit):
    px = bricks()
    glow = [(255, 170, 60, 255), (240, 110, 30, 255), (190, 60, 20, 255)]
    for y in range(6, 15):
        for x in range(4, 12):
            arch = y == 6 and x in (4, 11)
            if arch:
                continue
            if y == 6 or x in (4, 11):
                px[y][x] = IRON_DARK
            else:
                px[y][x] = glow[(x + y) % 3] if lit else (24, 22, 22, 255)
    return px


def pipe():
    """Copper pipe wall with a highlight stripe."""
    c = [(126, 66, 40, 255), (176, 96, 58, 255), (214, 136, 92, 255), (232, 172, 128, 255)]
    px = []
    for y in range(16):
        band = 3 if y in (5, 6) else 2 if y in (4, 7, 8) else 1 if y < 12 else 0
        px.append([c[band] for _ in range(16)])
    for x in (0, 15):
        for y in range(16):
            px[y][x] = c[0]
    return px


def flywheel():
    """A spoked iron flywheel face."""
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    import math
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6 <= d <= 7.8:
                px[y][x] = IRON_DARK if d > 7 else IRON_LIGHT
            elif d < 1.6:
                px[y][x] = BRASS
            elif d < 6 and (abs(x - 7.5) < 0.8 or abs(y - 7.5) < 0.8):
                px[y][x] = IRON
    return px


COKE = {"O": (22, 22, 24, 255), "D": (54, 54, 60, 255), "M": (82, 82, 90, 255), "H": (124, 124, 134, 255)}
COKE_LUMP = [
    "................",
    "................",
    ".....OOO.OO.....",
    "....OHMOOMHO....",
    "...OHMDMMDMMO...",
    "...OMDOMHMODO...",
    "..OHMMMDMMDMMO..",
    "..OMDOMMHMOMDO..",
    "..OMMMDMMDMMMO..",
    "...OMDMMOMDDO...",
    "...ODMMDMMDO....",
    "....OODDDDOO....",
    "......OOOO......",
]

WROUGHT = {"O": (35, 38, 44, 255), "D": (80, 86, 96, 255), "M": (110, 116, 126, 255), "H": (150, 156, 166, 255)}
STEEL = {"O": (40, 46, 58, 255), "D": (104, 116, 134, 255), "M": (150, 164, 182, 255), "H": (206, 218, 232, 255)}

RIVETS = [
    "................",
    "................",
    "....OO....OO....",
    "...OHMO..OHMO...",
    "...OMDO..OMDO...",
    "....OD....OD....",
    "....OD....OD....",
    ".....O.....O....",
    ".......OO.......",
    "......OHMO......",
    "......OMDO......",
    ".......OD.......",
    ".......OD.......",
    "........O.......",
]

STEAM_PISTON = [
    "................",
    ".....OOOOOO.....",
    "....OHHHHHMO....",
    "....OHMMMMDO....",
    "....OHMMMMDO....",
    "....OHMMMMDO....",
    "....ODDDDDDO....",
    ".....OOOOOO.....",
    ".......OO.......",
    ".......OH.......",
    ".......OM.......",
    ".......OM.......",
    "......OOOOO.....",
    "......OHMDO.....",
    "......OOOOO.....",
]


def pressure_gauge():
    """A brass-rimmed dial with a red needle on a copper stem."""
    import math
    px = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 6.5)
            if d <= 5.0:
                px[y][x] = (236, 232, 220, 255)
            elif d <= 6.2:
                px[y][x] = BRASS if d < 5.7 else BRASS_DARK
    for (x, y) in ((5, 8), (6, 7), (7, 6)):
        px[y][x] = (200, 40, 32, 255)
    px[6][8] = (40, 36, 34, 255)
    for (x, y) in ((4, 4), (11, 4), (7, 2)):
        px[y][x] = (90, 86, 80, 255)
    for y in (13, 14, 15):
        for x in (6, 7, 8, 9):
            px[y][x] = (176, 96, 58, 255) if x in (7, 8) else (126, 66, 40, 255)
    return px


root = "src/main/resources/assets/epochsmith/textures"
write_png(f"{root}/item/coke.png", from_map(COKE_LUMP, COKE))
write_png(f"{root}/block/boiler_side.png", riveted(3))
write_png(f"{root}/block/boiler_top.png", boiler_top())
write_png(f"{root}/block/coke_oven_side.png", bricks())
write_png(f"{root}/block/coke_oven_front.png", coke_oven_front(False))
write_png(f"{root}/block/coke_oven_front_lit.png", coke_oven_front(True))
write_png(f"{root}/block/steam_pipe.png", pipe())
write_png(f"{root}/block/flywheel.png", flywheel())
write_png(f"{root}/item/rivets.png", from_map(RIVETS, WROUGHT))
write_png(f"{root}/item/steam_piston.png", from_map(STEAM_PISTON, STEEL))
write_png(f"{root}/item/pressure_gauge.png", pressure_gauge())
print("industrial textures written")
