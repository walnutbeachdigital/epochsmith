"""Draws placeholder art for bronze tools and bronze lamellar armor. Run from the project root:
    python3 tools/make_bronze_gear_textures.py"""
import os

from pixelart import write_png, from_map, TRANSPARENT

BRONZE = {"O": (90, 52, 24, 255), "D": (160, 98, 40, 255), "M": (205, 133, 63, 255), "H": (240, 190, 120, 255),
          "W": (120, 86, 48, 255), "w": (84, 58, 32, 255)}

ICONS = {
    "bronze_sword": [
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
    "bronze_pickaxe": [
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
    "bronze_axe": [
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
    "bronze_shovel": [
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
    "bronze_hoe": [
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
    "bronze_helmet": [
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
    "bronze_chestplate": [
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
    "bronze_leggings": [
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
    "bronze_boots": [
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


root = "src/main/resources/assets/epochsmith/textures"
for name, rows in ICONS.items():
    write_png(f"{root}/item/{name}.png", from_map(rows, BRONZE))
os.makedirs(f"{root}/models/armor", exist_ok=True)
write_png(f"{root}/models/armor/bronze_layer_1.png", lamellar(64, 32, skip_hat=True))
write_png(f"{root}/models/armor/bronze_layer_2.png", lamellar(64, 32, skip_hat=False))
print("bronze gear textures written")
