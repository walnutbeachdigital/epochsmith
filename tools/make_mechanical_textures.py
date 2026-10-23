"""Draws placeholder art for the Mechanical era. Run from the project root:
    python3 tools/make_mechanical_textures.py"""
from pixelart import write_png, from_map, INGOT

WOOD = {"O": (62, 40, 20, 255), "D": (120, 84, 48, 255), "M": (160, 118, 70, 255), "H": (196, 156, 102, 255)}
STEEL = {"O": (40, 46, 58, 255), "D": (104, 116, 134, 255), "M": (150, 164, 182, 255), "H": (206, 218, 232, 255)}
IRON = {"O": (35, 38, 44, 255), "D": (80, 86, 96, 255), "M": (110, 116, 126, 255), "H": (150, 156, 166, 255)}

# A cog with eight teeth and a hole in the middle
GEAR = [
    "................",
    "......OOOO......",
    "...OO.OHHO.OO...",
    "...OHOOMMOOHO...",
    "....OMMMMMMO....",
    "..OOMMDOODMMOO..",
    "..OHMMO..OMMHO..",
    "..OHMMO..OMMDO..",
    "..OOMMDOODMMOO..",
    "....OMMMMMMO....",
    "...ODOOMMOODO...",
    "...OO.ODDO.OO...",
    "......OOOO......",
]


def gearbox_face():
    """Dark wooden frame with a gear showing through the middle."""
    plank = [(150, 108, 64, 255), (138, 98, 58, 255)]
    frame = (78, 54, 30, 255)
    px = [[plank[(y // 4) % 2] for x in range(16)] for y in range(16)]
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[y][x] = frame
    cog = from_map(GEAR, {"O": (52, 34, 18, 255), "D": (96, 66, 36, 255), "M": (120, 84, 48, 255), "H": (150, 112, 66, 255)})
    for y in range(16):
        for x in range(16):
            if cog[y][x][3] and 1 <= x <= 14 and 1 <= y <= 14:
                px[y][x] = cog[y][x]
    return px


def crate_face():
    plank = [(160, 118, 70, 255), (146, 106, 62, 255)]
    band = (62, 66, 74, 255)
    band_hi = (104, 110, 120, 255)
    px = [[plank[(x // 5) % 2] for x in range(16)] for y in range(16)]
    for i in range(16):
        for y in (2, 13):
            px[y][i] = band_hi
            px[y + 1][i] = band
    for (x, y) in ((1, 2), (14, 2), (1, 13), (14, 13)):
        px[y][x] = (200, 204, 210, 255)
    return px


root = "src/main/resources/assets/epochsmith/textures"
write_png(f"{root}/item/wooden_gear.png", from_map(GEAR, WOOD))
write_png(f"{root}/item/iron_gear.png", from_map(GEAR, IRON))
write_png(f"{root}/block/gearbox.png", gearbox_face())
write_png(f"{root}/item/steel_ingot.png", from_map(INGOT, STEEL))
write_png(f"{root}/block/banded_crate.png", crate_face())
print("mechanical textures written")
