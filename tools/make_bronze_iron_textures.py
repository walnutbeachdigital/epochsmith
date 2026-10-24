"""Draws placeholder textures for the Bronze and Iron era. Run from the project root:
    python3 tools/make_bronze_iron_textures.py"""
from pixelart import write_png, from_map, INGOT

BRONZE = {"O": (90, 52, 24, 255), "D": (160, 98, 40, 255), "M": (205, 133, 63, 255), "H": (240, 190, 120, 255)}
BLOOM = {"O": (40, 34, 32, 255), "D": (85, 72, 66, 255), "M": (120, 104, 96, 255),
         "H": (165, 148, 138, 255), "S": (60, 50, 46, 255)}

# A spongy, pitted lump: what really came out of a bloomery before hammering
IRON_BLOOM = [
    "................",
    "................",
    "....OOOO.OO.....",
    "...OHMMSOMMO....",
    "..OHMSMMMMSMO...",
    "..OMMMMSMHMMDO..",
    ".OHMSMMMMMSMDO..",
    ".OMMMMHMSMMMDO..",
    ".OMSMMMMMMSDDO..",
    "..OMMMSMMMMDO...",
    "..ODMMMMSMDDO...",
    "...ODDSDDDDO....",
    "....OOOOOOO.....",
]

WROUGHT = {"O": (35, 38, 44, 255), "D": (80, 86, 96, 255), "M": (110, 116, 126, 255), "H": (150, 156, 166, 255)}
HAMMER = dict(BRONZE, W=(120, 86, 48, 255), w=(84, 58, 32, 255))

# A flat hammered sheet, used for bronze tool heads and armor
PLATE = [
    "................",
    "................",
    "................",
    "................",
    "....OOOOOOOO....",
    "...OHHHHHHHHO...",
    "...OHMMMMMMDO...",
    "...OHMMHMMMDO...",
    "...OHMMMMMMDO...",
    "...OHMMMMMMDO...",
    "...ODDDDDDDDO...",
    "....OOOOOOOO....",
]

# Bronze head on a wooden handle, angled like vanilla tools
SMITHS_HAMMER = [
    "................",
    "........OOO.....",
    ".......OHMDO....",
    "......OHMMMDO...",
    ".....OHMMMDO....",
    "....OHMMMDOw....",
    ".....OMMDOW.....",
    "......OOOW......",
    ".......Ww.......",
    "......Ww........",
    ".....Ww.........",
    "....Ww..........",
    "...Ww...........",
    "..ww............",
]

base = "src/main/resources/assets/epochsmith/textures/item"
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/bronze_ingot.png", from_map(INGOT, BRONZE))
write_png(f"{base}/iron_bloom.png", from_map(IRON_BLOOM, BLOOM))
write_png(f"{base}/bronze_plate.png", from_map(PLATE, BRONZE))
# Now made by tools/recolor_vanilla.py (recolored from vanilla iron/coal): write_png(f"{base}/wrought_iron_ingot.png", from_map(INGOT, WROUGHT))
write_png(f"{base}/smiths_hammer.png", from_map(SMITHS_HAMMER, HAMMER))
print("bronze and iron textures written")
