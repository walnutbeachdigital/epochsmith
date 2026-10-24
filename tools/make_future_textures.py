"""Draws placeholder art for the Future era. Run from the project root:
    python3 tools/make_future_textures.py"""
from pixelart import write_png, from_map

root = "src/main/resources/assets/epochsmith/textures"

GRAPHENE_PAL = {"O": (10, 10, 14, 255), "D": (34, 36, 44, 255), "M": (70, 76, 92, 255), "H": (150, 160, 190, 255)}
GRAPHENE = [
    "................",
    "....OOOOOOOO....",
    "...OMHMDDMHMO...",
    "..OMDMHMMHMDMO..",
    ".OHMDDMHHMDDMHO.",
    ".OMHMDDMMDDMHMO.",
    ".ODMHMDDDDMHMDO.",
    ".ODDMHMDDMHMDDO.",
    ".ODDMHMDDMHMDDO.",
    ".ODMHMDDDDMHMDO.",
    ".OMHMDDMMDDMHMO.",
    ".OHMDDMHHMDDMHO.",
    "..OMDMHMMHMDMO..",
    "...OMHMDDMHMO...",
    "....OOOOOOOO....",
]

NANO_PAL = {"O": (20, 24, 30, 255), "S": (200, 210, 224, 255), "s": (120, 132, 150, 255), "G": (120, 255, 200, 255)}
NANOBOTS = [
    "................",
    "..Ss......sS....",
    "..sG.......Gs...",
    "......Ss........",
    "......sG...Ss...",
    "...Ss......sG...",
    "...sG...........",
    "........Ss......",
    ".Ss.....sG..Ss..",
    ".sG.........sG..",
    ".....Ss.........",
    ".....sG...Ss....",
    "..........sG....",
    "..Ss............",
    "..sG.....Ss.....",
    ".........sG.....",
]

COIL_PAL = {"O": (14, 18, 30, 255), "B": (80, 200, 255, 255), "b": (40, 120, 220, 255), "G": (60, 64, 76, 255),
            "H": (200, 240, 255, 255)}
SUPERCONDUCTOR = [
    "................",
    "....OOOOOOOO....",
    "...OGGGGGGGGO...",
    "...OBHBBBBHBO...",
    "...ObbbbbbbbO...",
    "...OBHBBBBHBO...",
    "...ObbbbbbbbO...",
    "...OBHBBBBHBO...",
    "...ObbbbbbbbO...",
    "...OBHBBBBHBO...",
    "...ObbbbbbbbO...",
    "...OGGGGGGGGO...",
    "....OOOOOOOO....",
]

CELL_PAL = {"O": (16, 16, 22, 255), "G": (70, 76, 92, 255), "g": (110, 118, 136, 255), "P": (230, 90, 255, 255),
            "p": (150, 40, 220, 255), "W": (255, 220, 255, 255)}
PLASMA_CELL = [
    "......OOOO......",
    ".....OggggO.....",
    ".....OGGGGO.....",
    "....OpppppPO....",
    "....OpPWPppO....",
    "....OpWWPppO....",
    "....OpPPpPpO....",
    "....OppPWPpO....",
    "....OpPWWPpO....",
    "....OppPPppO....",
    "....OpPppPpO....",
    "....OpppppPO....",
    ".....OGGGGO.....",
    ".....OggggO.....",
    "......OOOO......",
]


def nanofab_side():
    """Pearl-white composite with a glowing violet seam."""
    px = [[(232, 230, 240, 255) if (x + 2 * y) % 7 else (218, 216, 230, 255) for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (250, 250, 255, 255)
        px[15][i] = px[i][15] = (170, 166, 186, 255)
        px[11][i] = (170, 80, 240, 255)
    return px


def nanofab_front(on):
    px = nanofab_side()
    for y in range(3, 10):
        for x in range(3, 13):
            edge = y in (3, 9) or x in (3, 12)
            if edge:
                px[y][x] = (120, 110, 140, 255)
            elif on:
                px[y][x] = (220, 130, 255, 255) if (x * y) % 3 else (255, 210, 255, 255)
            else:
                px[y][x] = (40, 30, 56, 255)
    return px


write_png(f"{root}/item/graphene.png", from_map(GRAPHENE, GRAPHENE_PAL))
write_png(f"{root}/item/nanobots.png", from_map(NANOBOTS, NANO_PAL))
write_png(f"{root}/item/superconductor_coil.png", from_map(SUPERCONDUCTOR, COIL_PAL))
write_png(f"{root}/item/plasma_cell.png", from_map(PLASMA_CELL, CELL_PAL))
write_png(f"{root}/block/nanofabricator_side.png", nanofab_side())
write_png(f"{root}/block/nanofabricator_front.png", nanofab_front(False))
write_png(f"{root}/block/nanofabricator_front_on.png", nanofab_front(True))
print("future textures written")


def reactor_side(lit):
    """A ring of superconducting magnets around a viewport into the plasma."""
    px = [[(60, 64, 78, 255) if (x + y) % 3 else (52, 56, 70, 255) for x in range(16)] for y in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (120, 126, 144, 255)
        px[15][i] = px[i][15] = (30, 32, 40, 255)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.0 <= d < 6.5:
                px[y][x] = (70, 170, 240, 255) if (x + y) % 2 else (40, 110, 200, 255)
            elif d < 5.0:
                if lit:
                    px[y][x] = (255, 240, 255, 255) if d < 2 else (240, 140, 255, 255) if d < 3.5 else (170, 70, 240, 255)
                else:
                    px[y][x] = (24, 20, 34, 255)
    return px


def reactor_top():
    px = [[(70, 74, 90, 255) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = px[15][i] = px[i][15] = (36, 38, 48, 255)
    for y in range(3, 13):
        for x in range(3, 13):
            px[y][x] = (40, 110, 200, 255) if x in (3, 12) or y in (3, 12) else (46, 50, 64, 255)
    return px


write_png(f"{root}/block/fusion_reactor_side.png", reactor_side(False))
write_png(f"{root}/block/fusion_reactor_side_on.png", reactor_side(True))
write_png(f"{root}/block/fusion_reactor_top.png", reactor_top())
print("fusion reactor textures written")


def smelter_side():
    """Pearl-white shell with a band of violet plasma vents."""
    px = nanofab_side()
    for x in range(1, 15):
        px[11][x] = (220, 216, 232, 255)
        if x % 3 == 1:
            for y in (12, 13):
                px[y][x] = (150, 60, 220, 255)
    return px


def smelter_front(lit):
    px = smelter_side()
    for y in range(2, 10):
        for x in range(4, 12):
            d = abs(x - 7.5) + abs(y - 5.5)
            if y in (2, 9) or x in (4, 11):
                px[y][x] = (110, 100, 130, 255)
            elif lit:
                px[y][x] = (255, 240, 255, 255) if d < 1.5 else (230, 120, 255, 255) if d < 3 else (140, 50, 220, 255)
            else:
                px[y][x] = (30, 24, 40, 255)
    return px


write_png(f"{root}/block/plasma_smelter_side.png", smelter_side())
write_png(f"{root}/block/plasma_smelter_front.png", smelter_front(False))
write_png(f"{root}/block/plasma_smelter_front_on.png", smelter_front(True))
print("plasma smelter textures written")


def quantum_side(lit):
    """A dark crystal lattice with a shimmering violet-and-cyan core."""
    px = [[(28, 24, 40, 255) for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = (120, 110, 150, 255)
        px[15][i] = px[i][15] = (16, 14, 24, 255)
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[y][x] = (70, 60, 110, 255)
    for y in range(5, 11):
        for x in range(5, 11):
            if lit:
                px[y][x] = (140, 240, 255, 255) if (x + y) % 2 else (210, 120, 255, 255)
            else:
                px[y][x] = (60, 50, 90, 255)
    return px


def quantum_top():
    px = quantum_side(False)
    for y in range(6, 10):
        for x in range(6, 10):
            px[y][x] = (120, 110, 150, 255)
    return px


write_png(f"{root}/block/quantum_cell_side.png", quantum_side(False))
write_png(f"{root}/block/quantum_cell_side_on.png", quantum_side(True))
write_png(f"{root}/block/quantum_cell_top.png", quantum_top())
print("quantum cell textures written")
