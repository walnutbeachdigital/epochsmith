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
# Now made by the newer texture scripts: write_png(f"{root}/item/silicon.png", from_map(SILICON_LUMP, SILICON))
# Now made by the newer texture scripts: write_png(f"{root}/item/transistor.png", from_map(TRANSISTOR, PART))
# Now made by the newer texture scripts: write_png(f"{root}/item/circuit_board.png", from_map(CIRCUIT_BOARD, PART))
# Now made by the newer texture scripts: write_png(f"{root}/block/fabricator_side.png", fabricator_side())
# Now made by the newer texture scripts: write_png(f"{root}/block/fabricator_front.png", fabricator_front(False))
# Now made by the newer texture scripts: write_png(f"{root}/block/fabricator_front_on.png", fabricator_front(True))
# Now made by the newer texture scripts: write_png(f"{root}/block/solar_panel_top.png", solar_top())
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


# Now made by the newer texture scripts: write_png(f"{root}/block/computer_core_side.png", core_side(False))
# Now made by the newer texture scripts: write_png(f"{root}/block/computer_core_side_on.png", core_side(True))
# Now made by the newer texture scripts: write_png(f"{root}/block/computer_core_top.png", core_top())
print("computer core textures written")


def induction_side():
    """Brushed steel with a copper coil band."""
    px = [[(150, 156, 166, 255) if (x * 3 + y) % 7 else (138, 144, 154, 255) for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (190, 196, 206, 255)
        px[15][i] = px[i][15] = (90, 96, 106, 255)
    for y in range(5, 11):
        for x in range(1, 15):
            px[y][x] = (200, 120, 70, 255) if y % 2 else (160, 90, 50, 255)
    return px


def induction_front(on):
    px = induction_side()
    for y in range(3, 13):
        for x in range(4, 12):
            edge = y in (3, 12) or x in (4, 11)
            if edge:
                px[y][x] = (60, 64, 72, 255)
            elif on:
                px[y][x] = (255, 200, 90, 255) if (x + y) % 3 else (255, 140, 50, 255)
            else:
                px[y][x] = (40, 30, 30, 255)
    return px


# Now made by the newer texture scripts: write_png(f"{root}/block/induction_furnace_side.png", induction_side())
# Now made by the newer texture scripts: write_png(f"{root}/block/induction_furnace_front.png", induction_front(False))
# Now made by the newer texture scripts: write_png(f"{root}/block/induction_furnace_front_on.png", induction_front(True))
print("induction furnace textures written")


def vault_side(on):
    """Stacked drive bays, each with an activity light."""
    px = [[(36, 40, 48, 255) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (80, 86, 98, 255)
        px[15][i] = px[i][15] = (20, 22, 28, 255)
    for bay, y in enumerate((2, 6, 10)):
        for yy in range(y, y + 3):
            for x in range(2, 14):
                px[yy][x] = (58, 64, 76, 255) if yy == y else (48, 52, 62, 255)
        for x in range(4, 10):
            px[y + 1][x] = (28, 30, 36, 255)
        lit = (90, 200, 255, 255) if on else (60, 66, 76, 255)
        px[y + 1][12] = lit if (bay != 1 or not on) else (120, 255, 150, 255)
    return px


def vault_top():
    px = [[(70, 76, 88, 255) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = px[15][i] = px[i][15] = (40, 44, 52, 255)
    for y in range(4, 12):
        for x in range(4, 12):
            px[y][x] = (58, 120, 200, 255) if (x + y) % 2 else (50, 100, 170, 255)
    return px


# Now made by the newer texture scripts: write_png(f"{root}/block/data_vault_side.png", vault_side(False))
# Now made by the newer texture scripts: write_png(f"{root}/block/data_vault_side_on.png", vault_side(True))
# Now made by the newer texture scripts: write_png(f"{root}/block/data_vault_top.png", vault_top())
print("data vault textures written")


def assembler_side():
    """Clean-room panels with a yellow-and-black safety stripe."""
    px = fabricator_side()
    for x in range(16):
        for y in (12, 13):
            px[y][x] = (230, 190, 40, 255) if ((x + y) // 2) % 2 else (30, 30, 34, 255)
    return px


def assembler_front(on):
    px = assembler_side()
    for y in range(2, 11):
        for x in range(2, 14):
            edge = y in (2, 10) or x in (2, 13)
            px[y][x] = (110, 116, 126, 255) if edge else (36, 44, 56, 255)
    arm = (255, 170, 60, 255) if on else (150, 150, 158, 255)
    for x in range(4, 9):
        px[5][x] = arm
    for y in range(5, 9):
        px[y][8] = arm
    px[8][7] = px[8][9] = arm
    if on:
        px[9][8] = (140, 220, 255, 255)
    return px


# Now made by the newer texture scripts: write_png(f"{root}/block/assembler_side.png", assembler_side())
# Now made by the newer texture scripts: write_png(f"{root}/block/assembler_front.png", assembler_front(False))
# Now made by the newer texture scripts: write_png(f"{root}/block/assembler_front_on.png", assembler_front(True))
print("assembler textures written")


# ---- Laser cutter, multitool and composite tactical armor
TOOL = {"O": (16, 18, 24, 255), "D": (44, 50, 62, 255), "M": (82, 92, 110, 255), "H": (150, 166, 190, 255),
        "B": (60, 200, 255, 255), "b": (30, 120, 200, 255), "R": (255, 70, 60, 255), "Y": (230, 190, 40, 255)}

LASER_CUTTER = [
    "................",
    "................",
    "...........OO...",
    "..........ObBO..",
    ".........ObBBO..",
    "...OOOOOOOMBO...",
    "..OHHMMMMMDO....",
    "..OHMMMMMDDO....",
    "..ODDDOODDO.....",
    "...OOOODRDO.....",
    "......ODDO......",
    "......ODDO......",
    "......OHDO......",
    ".......OO.......",
]

MULTITOOL = [
    "................",
    "..OOO.....OOOO..",
    ".OHHMO...OHMMDO.",
    ".OMMMDO.OHMDDO..",
    "..ODDMDOMMDOO...",
    "...OODMMMDO.....",
    ".....OMBMO......",
    "....OMDbDMO.....",
    "...OMDO.ODMO....",
    "..OYDO...OO.....",
    ".OYDO...........",
    ".ODO............",
    "..O.............",
]

COMPOSITE = {"O": (14, 16, 20, 255), "D": (40, 46, 54, 255), "M": (64, 72, 84, 255), "H": (110, 124, 140, 255),
             "W": (60, 200, 255, 255), "w": (30, 120, 200, 255)}


def composite_layer(skip_hat):
    """Matte charcoal plates with thin cyan circuit traces."""
    px = []
    for y in range(32):
        row = []
        for x in range(64):
            if skip_hat and x >= 32 and y < 16:
                row.append((0, 0, 0, 0))
            elif (x % 8 == 0 and y % 4 != 3) or (y % 8 == 4 and x % 16 < 9):
                row.append((40, 170, 230, 255))
            elif (x + y) % 5 == 0:
                row.append((52, 58, 68, 255))
            else:
                row.append((40, 46, 54, 255) if (x // 4 + y // 4) % 2 else (46, 52, 62, 255))
        px.append(row)
    return px


import make_bronze_gear_textures as gear

# Now made by the newer texture scripts: write_png(f"{root}/item/laser_cutter.png", from_map(LASER_CUTTER, TOOL))
# Now made by the newer texture scripts: write_png(f"{root}/item/multitool.png", from_map(MULTITOOL, TOOL))
# Now made by tools/recolor_vanilla.py: for piece in ("helmet", "chestplate", "leggings", "boots"):
#     write_png(f"{root}/item/composite_{piece}.png", from_map(gear.ICONS[piece], COMPOSITE))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/composite_layer_1.png", composite_layer(True))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/composite_layer_2.png", composite_layer(False))
print("information gear textures written")
