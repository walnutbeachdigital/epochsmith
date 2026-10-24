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

REFRACTORY = [(196, 192, 184, 255), (180, 176, 168, 255)]


def arc_side():
    """Pale refractory brick with a copper bus bar across the middle."""
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            offset = 4 if (y // 4) % 2 else 0
            if 6 <= y <= 8:
                row.append(COPPER[3] if y == 6 else COPPER[1])
            elif y % 4 == 3 or (x + offset) % 8 == 7:
                row.append((140, 136, 130, 255))
            else:
                row.append(REFRACTORY[((x + offset) // 8 + y // 4) % 2])
        px.append(row)
    return px


def arc_front(lit):
    """An iron door with a viewing slot; white-blue arc light when running."""
    px = [[IRON for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = IRON_LIGHT
        px[15][i] = px[i][15] = IRON_DARK
    hot = [(255, 255, 255, 255), (200, 230, 255, 255), (140, 190, 255, 255)]
    for y in range(5, 11):
        for x in range(3, 13):
            if y in (5, 10) or x in (3, 12):
                px[y][x] = IRON_DARK
            else:
                px[y][x] = hot[(x * 3 + y) % 3] if lit else (24, 24, 30, 255)
    return px


def arc_top():
    px = arc_side()
    for (cx, cy) in ((4, 8), (8, 4), (12, 8)):
        for y in range(cy - 1, cy + 2):
            for x in range(cx - 1, cx + 2):
                px[y][x] = (40, 40, 44, 255) if (x, y) != (cx, cy) else (90, 90, 96, 255)
    return px


ELEC = {"O": (30, 22, 18, 255), "D": (60, 40, 30, 255), "M": (96, 64, 46, 255), "H": (214, 136, 92, 255),
        "S": (190, 196, 206, 255), "s": (120, 128, 140, 255), "R": (170, 40, 34, 255), "r": (120, 28, 24, 255),
        "W": (120, 86, 48, 255), "w": (84, 58, 32, 255), "G": (176, 96, 58, 255)}

ELECTRIC_DRILL = [
    "................",
    "..OOOOOO........",
    "..OHHMMDO.......",
    "..OHMMMDOOO.....",
    "..OMMMMDOSsO....",
    "..ODDDDDOsSsO...",
    "...OOODOOOsSsO..",
    ".....ODO...OsSO.",
    ".....ODO....OsO.",
    ".....ODO.....O..",
    "....OOHOO.......",
    "....ODDDO.......",
    ".....OOO........",
]

CHAINSAW = [
    "................",
    "........OOOOOOO.",
    ".......OsSsSsSsO",
    "..OOOOOOSOOOOOsO",
    ".ORRRRROsO....SO",
    ".ORrRrROSOOOOOsO",
    ".ORRRRROsSsSsSsO",
    ".OrrrrrOOOOOOOO.",
    "..OOODO.........",
    "....ODO.........",
    "....OOO.........",
]


def coil_layer(skip_hat):
    """Dark plates wound with bands of copper coil."""
    px = []
    for y in range(32):
        row = []
        for x in range(64):
            if skip_hat and x >= 32 and y < 16:
                row.append((0, 0, 0, 0))
            elif y % 6 in (0, 1, 2):
                row.append(COPPER[3] if (x + y) % 3 == 0 else COPPER[1 + (y % 2)])
            elif y % 6 == 3:
                row.append((24, 18, 16, 255))
            else:
                row.append(BAKELITE_LIGHT if x % 8 else BAKELITE)
        px.append(row)
    return px


STEEL_SHEET = [(150, 164, 182, 255), (138, 152, 170, 255)]


def cabinet_side():
    px = [[STEEL_SHEET[(x + y) % 2] for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (206, 218, 232, 255)
        px[15][i] = px[i][15] = (40, 46, 58, 255)
    return px


def cabinet_front():
    """Nine drawer fronts in a 3x3 grid, each with a small brass label plate at the bottom."""
    px = cabinet_side()
    for row in range(3):
        for col in range(3):
            x0, y0 = 1 + col * 5, 1 + row * 5
            for y in range(y0, y0 + 4):
                for x in range(x0, x0 + 4):
                    edge = x in (x0, x0 + 3) or y in (y0, y0 + 3)
                    px[y][x] = (60, 66, 78, 255) if edge else (92, 100, 114, 255)
            px[y0 + 3][x0 + 1] = px[y0 + 3][x0 + 2] = (210, 170, 84, 255)
    return px


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
write_png(f"{root}/block/arc_furnace_side.png", arc_side())
write_png(f"{root}/block/arc_furnace_top.png", arc_top())
write_png(f"{root}/block/arc_furnace_front.png", arc_front(False))
write_png(f"{root}/block/arc_furnace_front_on.png", arc_front(True))
import make_bronze_gear_textures as gear
COIL = {"O": (30, 22, 18, 255), "D": (126, 66, 40, 255), "M": (176, 96, 58, 255), "H": (232, 172, 128, 255),
        "W": (120, 86, 48, 255), "w": (84, 58, 32, 255)}
# Now made by tools/recolor_vanilla.py: for piece in ("helmet", "chestplate", "leggings", "boots"):
#     write_png(f"{root}/item/coil_{piece}.png", from_map(gear.ICONS[piece], COIL))
write_png(f"{root}/item/electric_drill.png", from_map(ELECTRIC_DRILL, ELEC))
write_png(f"{root}/item/chainsaw.png", from_map(CHAINSAW, ELEC))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/coil_layer_1.png", coil_layer(True))
# Now made by tools/recolor_vanilla.py: write_png(f"{root}/models/armor/coil_layer_2.png", coil_layer(False))
write_png(f"{root}/block/sorting_cabinet_side.png", cabinet_side())
write_png(f"{root}/block/sorting_cabinet_front.png", cabinet_front())
print("electrical textures written")
