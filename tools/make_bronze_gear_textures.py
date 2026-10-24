"""Draws tool and armor art for each metal: bronze (lamellar) and steel (knight's plate).
Run from the project root:
    python3 tools/make_bronze_gear_textures.py"""
import os

from pixelart import write_png, from_map, TRANSPARENT

WOOD = {"W": (120, 86, 48, 255), "w": (84, 58, 32, 255)}
BRONZE = dict({"O": (90, 52, 24, 255), "D": (160, 98, 40, 255), "M": (205, 133, 63, 255), "H": (240, 190, 120, 255)}, **WOOD)
STEEL = dict({"O": (40, 46, 58, 255), "D": (104, 116, 134, 255), "M": (150, 164, 182, 255), "H": (206, 218, 232, 255)}, **WOOD)

ICONS = {
    "sword": [
        ".......OO.......",
        "......OHMO......",
        "......OHMO......",
        "......OHMO......",
        "......OHMO......",
        "......OHMO......",
        "......OHMO......",
        "......OHMO......",
        "....OOOHMDOO....",
        "....ODDDDDDO....",
        ".....OOWWOO.....",
        ".......WW.......",
        "......OwwO......",
        ".......OO.......",
    ],
    "pickaxe": [
        "................",
        ".....OOOOOO.....",
        "...OOHHMMMDOO...",
        "..OHMDOOOOODMO..",
        "..OMO..OWO..OMO.",
        "..OO...OWO...OO.",
        ".......OWO......",
        ".......OWO......",
        ".......OWO......",
        ".......OWO......",
        ".......OWO......",
        ".......OwO......",
        "........O.......",
    ],
    "axe": [
        "................",
        ".........OO.....",
        ".......OOHMO....",
        "......OHMMMDO...",
        ".....OOHMMMDO...",
        ".....OWOMMDO....",
        ".....OWOOOO.....",
        ".....OWO........",
        ".....OWO........",
        ".....OWO........",
        ".....OWO........",
        ".....OwO........",
        "......O.........",
    ],
    "shovel": [
        "................",
        "......OOOO......",
        ".....OHMMDO.....",
        ".....OHMMDO.....",
        ".....OHMMDO.....",
        "......ODDO......",
        ".......OWO......",
        ".......OWO......",
        ".......OWO......",
        ".......OWO......",
        "......OOwOO.....",
        "......OwwwO.....",
        ".......OOO......",
    ],
    "hoe": [
        "................",
        "....OOOOOO......",
        "...OHMMMDWO.....",
        "....OOOOOWO.....",
        "........OWO.....",
        "........OWO.....",
        "........OWO.....",
        "........OWO.....",
        "........OWO.....",
        "........OWO.....",
        "........OwO.....",
        ".........O......",
    ],
    "helmet": [
        "................",
        "................",
        "................",
        "....OOOOOOOO....",
        "...OHHMMMMMDO...",
        "...OHMDMDMDDO...",
        "...OMO....ODO...",
        "...OMO....ODO...",
        "...OOO....OOO...",
    ],
    "chestplate": [
        "................",
        "...OOO....OOO...",
        "..OHMDOOOOOHDO..",
        "..OHMMMMMMMMDO..",
        "..OOOHMDMDMOOO..",
        "....OHMMMMDO....",
        "....OHMDMDDO....",
        "....OHMMMMDO....",
        "....OHMDMDDO....",
        "....ODDDDDDO....",
        "....OOOOOOOO....",
    ],
    "leggings": [
        "................",
        "....OOOOOOOO....",
        "....OHMMMMDO....",
        "....OHMDMDDO....",
        "....OHMOOMDO....",
        "....OHMO.ODO....",
        "....OHMO.ODO....",
        "....OHMO.ODO....",
        "....ODDO.ODO....",
        "....OOOO.OOO....",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "...OOO....OOO...",
        "...OHO....OHO...",
        "...OMO....OMO...",
        "..OHMO...OHMO...",
        "..ODDO...ODDO...",
        "..OOOO...OOOO...",
    ],
}


# A brace drill: crank handle above, steel bit below
HAND_DRILL = [
    "................",
    ".......OO.......",
    "......OHMO......",
    "......OMDO......",
    ".......OO.......",
    "........OW......",
    "....OOOOOW......",
    "....OHMMMO......",
    "...OWO..OW......",
    "...OWO..OW......",
    "....O...OW......",
    "........OHO.....",
    "........OMO.....",
    "........OMO.....",
    ".........O......",
]


def lamellar(width, height, skip_hat):
    """Rows of small overlapping plates, the look of lamellar armor.
    The armor model reads different regions of this sheet for each body part, so a
    pattern that covers the whole sheet gives a consistent look everywhere."""
    px = []
    for y in range(height):
        row = []
        band, line = divmod(y, 4)
        for x in range(width):
            if skip_hat and x >= 32 and y < 16:
                row.append(TRANSPARENT)  # the helmet's outer layer: leave empty so it isn't bulky
                continue
            col = (x + (2 if band % 2 else 0)) % 4
            if line == 3 or col == 3:
                c = "O"
            elif line == 0:
                c = "H"
            elif line == 2:
                c = "D"
            else:
                c = "M"
            row.append(BRONZE[c])
        px.append(row)
    return px


def plate(width, height, skip_hat):
    """Large polished plates with dark seams and rivets: the look of knight's armor."""
    px = []
    for y in range(height):
        row = []
        for x in range(width):
            if skip_hat and x >= 32 and y < 16:
                row.append(TRANSPARENT)
                continue
            px_in, py_in = x % 8, y % 8
            if px_in == 7 or py_in == 7:
                c = "O"
            elif (px_in, py_in) in ((1, 1), (5, 1), (1, 5), (5, 5)):
                c = "H"
            elif py_in <= 1:
                c = "H" if px_in < 6 else "M"
            elif py_in >= 5:
                c = "D"
            else:
                c = "M"
            row.append(STEEL[c])
        px.append(row)
    return px


root = "src/main/resources/assets/epochsmith/textures"
# Now made by tools/recolor_vanilla.py: for metal, palette in (("bronze", BRONZE), ("steel", STEEL)):
#     for name, rows in ICONS.items():
#         write_png(f"{root}/item/{metal}_{name}.png", from_map(rows, palette))
# Now made by the newer texture scripts: write_png(f"{root}/item/hand_drill.png", from_map(HAND_DRILL, STEEL))
os.makedirs(f"{root}/models/armor", exist_ok=True)
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/bronze_layer_1.png", lamellar(64, 32, skip_hat=True))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/bronze_layer_2.png", lamellar(64, 32, skip_hat=False))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/knight_layer_1.png", plate(64, 32, skip_hat=True))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/knight_layer_2.png", plate(64, 32, skip_hat=False))
print("bronze and steel gear textures written")
