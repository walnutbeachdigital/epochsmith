"""Draws placeholder art for the Information era. Run from the project root:
    python3 tools/make_information_textures.py"""
from pixelart import write_png, from_map

SILICON = {"O": (30, 34, 44, 255), "D": (70, 78, 96, 255), "M": (104, 116, 140, 255), "H": (170, 186, 214, 255)}
SILICON_LUMP = [
    "................",
    "........OO......",
    ".......OHMO.....",
    "......OHMMDO....",
    ".....OHMMHMDO...",
    "....OHMMMMMDO...",
    "...OHMMHMMDDO...",
    "...OMMMMMMDO....",
    "..OHMMMMMDDO....",
    "..OMMMMHMDO.....",
    "..ODMMMMDO......",
    "...ODDDDO.......",
    "....OOOO........",
]

PART = {"O": (16, 16, 18, 255), "B": (40, 40, 44, 255), "b": (64, 64, 70, 255),
        "L": (190, 196, 206, 255), "G": (40, 120, 60, 255), "g": (28, 90, 44, 255),
        "C": (214, 136, 92, 255), "Y": (220, 190, 90, 255)}
TRANSISTOR = [
    "................",
    "................",
    ".....OOOOOO.....",
    "....ObbbbbbO....",
    "....ObBBBBbO....",
    "....OBBBBBBO....",
    "....OBBBBBBO....",
    ".....OOOOOO.....",
    ".....L..L..L....",
    ".....L..L..L....",
    ".....L..L..L....",
    ".....L..L..L....",
    ".....L..L..L....",
]
CIRCUIT_BOARD = [
    "................",
    ".OOOOOOOOOOOOOO.",
    ".OGGCCCCGGGgGGO.",
    ".OGgGGGCGOOGgGO.",
    ".OGGGOOCGObOGGO.",
    ".OCCCObCCOOGGCO.",
    ".OGGGOOGGGGGGCO.",
    ".OGgGGGGOOOOGCO.",
    ".OGGCCCCObbOGCO.",
    ".OGGCGGGOOOOGGO.",
    ".OYGCGgGGGGGGYO.",
    ".OOOOOOOOOOOOOO.",
]


def fabricator_side():
    """Clean-room white panels with a blue status stripe."""
    px = [[(222, 226, 232, 255) if (x + y) % 5 else (210, 214, 222, 255) for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (240, 242, 246, 255)
        px[15][i] = px[i][15] = (150, 156, 166, 255)
        px[12][i] = (70, 130, 210, 255)
    return px


def fabricator_front(on):
    px = fabricator_side()
    glass = [(120, 200, 255, 255), (80, 160, 230, 255)] if on else [(60, 70, 84, 255), (44, 52, 64, 255)]
    for y in range(3, 10):
        for x in range(3, 13):
            edge = y in (3, 9) or x in (3, 12)
            px[y][x] = (110, 116, 126, 255) if edge else glass[(x + y) % 2]
    return px


def solar_top():
    """Blue photovoltaic cells in a silver grid."""
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            if x % 5 == 0 or y % 5 == 0:
                row.append((200, 206, 214, 255))
            else:
                row.append((40, 70, 140, 255) if (x + y) % 3 else (60, 100, 180, 255))
        px.append(row)
    return px


root = "src/main/resources/assets/epochsmith/textures"
write_png(f"{root}/item/silicon.png", from_map(SILICON_LUMP, SILICON))
write_png(f"{root}/item/transistor.png", from_map(TRANSISTOR, PART))
write_png(f"{root}/item/circuit_board.png", from_map(CIRCUIT_BOARD, PART))
write_png(f"{root}/block/fabricator_side.png", fabricator_side())
write_png(f"{root}/block/fabricator_front.png", fabricator_front(False))
write_png(f"{root}/block/fabricator_front_on.png", fabricator_front(True))
write_png(f"{root}/block/solar_panel_top.png", solar_top())
print("information textures written")


def core_side(on):
    """A server rack: dark steel with rows of status lights."""
    px = [[(48, 52, 60, 255) if (x + y) % 4 else (40, 44, 52, 255) for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (90, 96, 108, 255)
        px[15][i] = px[i][15] = (24, 26, 32, 255)
    colors = [(80, 230, 120, 255), (90, 170, 255, 255), (255, 190, 70, 255)]
    for row, y in enumerate((3, 7, 11)):
        for x in range(2, 14):
            px[y][x] = (28, 30, 36, 255)
            px[y + 1][x] = (60, 64, 74, 255)
        for x in (3, 5, 7):
            lit = on and (x + row) % 3 != 0
            px[y][x] = colors[(x + row) % 3] if lit else (70, 74, 82, 255)
    return px


def core_top():
    px = [[(60, 64, 74, 255) for _ in range(16)] for _ in range(16)]
    for y in range(2, 14, 3):
        for x in range(2, 14):
            px[y][x] = (24, 26, 32, 255)
    return px


write_png(f"{root}/block/computer_core_side.png", core_side(False))
write_png(f"{root}/block/computer_core_side_on.png", core_side(True))
write_png(f"{root}/block/computer_core_top.png", core_top())
print("computer core textures written")
