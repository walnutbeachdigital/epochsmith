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

base = "src/main/resources/assets/epochsmith/textures/item"
write_png(f"{base}/bronze_ingot.png", from_map(INGOT, BRONZE))
write_png(f"{base}/iron_bloom.png", from_map(IRON_BLOOM, BLOOM))
print("bronze and iron textures written")
