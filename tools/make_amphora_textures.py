"""Draws placeholder art for the clay amphora. Run from the project root:
    python3 tools/make_amphora_textures.py"""
import random

from pixelart import write_png, from_map

# Fired terracotta with a black band and an orange key pattern, like Greek pottery
TERRA = [(196, 104, 58), (184, 96, 52), (204, 112, 64), (190, 100, 55)]
BLACK = (38, 30, 28, 255)
ORANGE = (214, 124, 70, 255)
KEY = [  # the band's pattern, repeated across the face ('#' = orange)
    "################",
    "................",
    ".###..###..###..",
    ".#.#..#.#..#.#..",
    ".#....#....#....",
    "................",
]


def amphora_face():
    rnd = random.Random(7)
    px = []
    for y in range(16):
        if 4 <= y <= 11:
            band_row = y - 5
            row = []
            for x in range(16):
                on = 0 <= band_row < len(KEY) and KEY[band_row][x] == "#"
                row.append(ORANGE if on else BLACK)
            # thin black rules above and below the pattern
            px.append(row if y not in (4, 11) else [BLACK] * 16)
        else:
            px.append([TERRA[rnd.randrange(len(TERRA))] + (255,) for _ in range(16)])
    return px


# Raw grey clay, shaped but not yet fired
CLAY = {"O": (90, 92, 104, 255), "D": (120, 126, 140, 255), "C": (158, 164, 178, 255), "H": (190, 196, 208, 255)}
UNFIRED = [
    "................",
    "......OOOO......",
    "......OCCO......",
    ".....OOCCOO.....",
    "....OCCCCCCO....",
    "...OCCHCCCCDO...",
    "...OCHCCCCCDO...",
    "...OCHCCCCCDO...",
    "...OCCCCCCCDO...",
    "....OCCCCCDO....",
    ".....OCCCDO.....",
    "......OCDO......",
    ".....OOOOOO.....",
]

root = "src/main/resources/assets/epochsmith/textures"
# Now made by the newer texture scripts: write_png(f"{root}/block/amphora.png", amphora_face())
# Now made by the newer texture scripts: write_png(f"{root}/item/unfired_amphora.png", from_map(UNFIRED, CLAY))
print("amphora textures written")
