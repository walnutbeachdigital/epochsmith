"""Early-era textures (Primitive to Industrial), built on vanilla materials. Run from the project root:
    python3 tools/make_textures_early.py"""
import math

from pixelart import write_png
from texture_kit import (BRASS, BRONZE, CLAY, COPPER, DARK_IRON, DARK_WOOD, FIRE, FIREBRICK, IRON, OAK, STEEL,
                         base, bevel, blank, blend, band, copy, darken, disc, fill, frame, from_rows, gear, glow,
                         inset, lighten, mix, outline, put, ring, rivet, shade, stud, tint)

OUT = "src/main/resources/assets/epochsmith/textures"
TERRACOTTA = [(92, 44, 24), (150, 74, 40), (186, 98, 56), (214, 130, 84)]
BLACK_GLAZE = (30, 22, 20)
CREAM = (232, 208, 160)


# ---------------- Blocks ----------------

def amphora():
    """Greek-style pot: terracotta with black-glaze bands and a key (meander) pattern."""
    px = base("block/orange_terracotta", TERRACOTTA)
    for y in (1, 14):
        for x in range(16):
            put(px, x, y, BLACK_GLAZE)
    for x in range(16):
        put(px, x, 3, CREAM if x % 2 == 0 else BLACK_GLAZE)  # dotted cream line
        put(px, x, 12, CREAM if x % 2 == 0 else BLACK_GLAZE)
    # Meander band, rows 5-10, repeating every 8 pixels
    key = ["########",
           "#......#",
           "#.####.#",
           "#.#..#.#",
           "#.#.##.#",
           "#.#....."]
    for y, row in enumerate(key):
        for x in range(16):
            if row[x % 8] == "#":
                put(px, x, 5 + y, BLACK_GLAZE)
    # A soft highlight down one side, like a round glazed pot
    for y in range(16):
        lighten(px, 4, y, 0.12)
        lighten(px, 5, y, 0.08)
        darken(px, 13, y, 0.12)
        darken(px, 14, y, 0.18)
    return px


def banded_crate():
    """Spruce planks held by riveted iron bands, with a dark frame."""
    px = base("block/spruce_planks")
    band(px, 2, 3, DARK_IRON, rivets=(1, 7, 14), rivet_metal=IRON)
    band(px, 12, 13, DARK_IRON, rivets=(1, 7, 14), rivet_metal=IRON)
    frame(px, (44, 30, 18))
    bevel(px, 1, 1, 14, 14, light=0.15, dark=0.3)
    return px


def strongbox_side():
    """Riveted iron plates."""
    px = base("block/iron_block", DARK_IRON)
    inset(px, 1, 1, 14, 14)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        rivet(px, x, y, IRON)
    for y in range(16):
        darken(px, 7, y, 0.25)  # plate seam
        lighten(px, 8, y, 0.12)
    return px


def strongbox_front():
    """Iron door with a brass lock plate and keyhole."""
    px = strongbox_side()
    for y in range(16):  # no seam on the door
        px[y][7] = px[y][6]
        px[y][8] = px[y][9]
    fill(px, 5, 5, 10, 10, BRASS[2])
    for x in range(5, 11):
        for y in range(5, 11):
            put(px, x, y, shade(BRASS, 0.85 - (x - 5 + y - 5) * 0.06))
    bevel(px, 5, 5, 10, 10, light=0.3, dark=0.5)
    put(px, 7, 6, (24, 16, 8)); put(px, 8, 6, (24, 16, 8))
    put(px, 7, 7, (24, 16, 8)); put(px, 8, 7, (24, 16, 8))
    put(px, 7, 8, (24, 16, 8)); put(px, 7, 9, (24, 16, 8))
    for y in (3, 12):  # hinges
        fill(px, 0, y, 2, y + 1, IRON[1])
        put(px, 0, y, IRON[3])
    return px


def gear_train_case():
    """Brass panel in a dark wood frame, with corner screws."""
    px = base("block/cut_copper", BRASS)
    frame(px, DARK_WOOD[1], 2)
    for i in range(16):
        put(px, i, 0, DARK_WOOD[2]); put(px, 0, i, DARK_WOOD[2])
    inset(px, 2, 2, 13, 13, light=0.25, dark=0.5)
    for x, y in ((3, 3), (11, 3), (3, 11), (11, 11)):
        rivet(px, x, y, IRON)
    return px


def dial(angle):
    """The gear train's clock face: brass bezel, cream face, hour ticks and a dark hand."""
    px = gear_train_case()
    cx = cy = 8
    ring(px, cx, cy, 7.0, 5.0, BRASS)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d < 5.0:
                put(px, x, y, mix((246, 236, 210), (210, 196, 160), d / 5))
    ring(px, cx, cy, 5.2, 4.6, [(40, 30, 20), (70, 54, 34), (90, 70, 46), (110, 88, 58)])
    for i in range(4):  # quarter-hour ticks
        a = i * math.pi / 2
        put(px, int(cx + math.cos(a) * 3.6 - 0.5 + 0.5), int(cy + math.sin(a) * 3.6 - 0.5 + 0.5), (40, 30, 20))
    for step in range(1, 5):
        x = cx + math.cos(angle) * step * 0.95
        y = cy + math.sin(angle) * step * 0.95
        put(px, int(math.floor(x)), int(math.floor(y)), (24, 18, 12))
    put(px, 7, 7, BRASS[3]); put(px, 8, 7, BRASS[2]); put(px, 7, 8, BRASS[2]); put(px, 8, 8, BRASS[1])
    for x, y in ((5, 5), (6, 5), (5, 6)):  # glass glint
        blend(px, x, y, (255, 255, 255), 0.45)
    return px


def gear_train_dial_frames():
    frames = [dial(-math.pi / 2 + i * math.pi / 4) for i in range(8)]
    return [row for f in frames for row in f]


def gearbox():
    """A dark wood housing with a brass gear meshing behind an iron-framed window."""
    px = base("block/dark_oak_planks")
    inset(px, 2, 2, 13, 13, light=0.2, dark=0.55)
    tint(px, 3, 3, 12, 12, (16, 10, 6), 0.65)
    gear(px, 8, 8, 5.6, 4.4, 1.6, 8, BRASS)
    frame(px, DARK_IRON[1])
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14)):
        rivet(px, x, y, IRON)
    return px


def flywheel():
    """The steam engine's flywheel: a heavy iron rim, spokes and a brass hub."""
    px = base("block/smooth_stone", DARK_IRON)
    tint(px, 0, 0, 15, 15, (10, 10, 12), 0.5)
    ring(px, 8, 8, 8.0, 6.0, IRON)
    for a in range(6):
        ang = a * math.pi / 3 + 0.3
        for r in range(2, 6):
            x, y = int(8 + math.cos(ang) * r), int(8 + math.sin(ang) * r)
            put(px, x, y, shade(IRON, 0.55 - 0.3 * math.sin(ang + 0.8)))
    disc(px, 8, 8, 2.4, BRASS)
    return px


def boiler_side():
    """Riveted steel boiler plates with a brass band."""
    px = base("block/iron_block", STEEL)
    for y in range(16):  # curved-cylinder shading
        for x in range(16):
            t = math.sin((x + 0.5) / 16 * math.pi)
            blend(px, x, y, (255, 255, 255) if t > 0.8 else (0, 0, 0), 0.1 if t > 0.8 else (1 - t) * 0.35)
    for y in (4, 11):
        for x in range(16):
            darken(px, x, y, 0.35)
            lighten(px, x, y + 1, 0.15)
        for x in range(1, 16, 3):
            stud(px, x, y - 1, STEEL)
    band(px, 13, 14, BRASS, rivets=(2, 8, 13), rivet_metal=BRASS)
    return px


def boiler_top():
    """Steel cap with a riveted flange and a central steam outlet."""
    px = base("block/iron_block", STEEL)
    inset(px, 0, 0, 15, 15, light=0.15, dark=0.35)
    for x in range(1, 15, 3):
        stud(px, x, 1, STEEL); stud(px, x, 13, STEEL)
        stud(px, 1, x, STEEL); stud(px, 13, x, STEEL)
    ring(px, 8, 8, 4.2, 2.4, BRASS)
    disc(px, 8, 8, 2.4, [(20, 20, 22), (36, 36, 40), (52, 52, 58), (70, 70, 78)], dome=False)
    return px


def coke_oven_side():
    """Dark fire bricks."""
    return base("block/bricks", FIREBRICK)


def coke_oven_front(lit):
    """Fire bricks with an iron-framed arched door; glowing coals when lit."""
    px = coke_oven_side()
    hole = [(x, y) for y in range(5, 15) for x in range(4, 12)
            if not (y == 5 and x in (4, 11)) and not (y == 6 and x in (4, 11) and False)]
    for x, y in hole:
        put(px, x, y, (18, 12, 10))
    if lit:
        for x, y in hole:
            heat = (y - 5) / 9
            put(px, x, y, shade(FIRE, 0.25 + heat * 0.6 + ((x * 5 + y * 3) % 4) * 0.04))
        glow(px, 8, 13, 7, FIRE[2], 0.35)
    # Iron frame round the opening
    for y in range(4, 15):
        put(px, 3, y, IRON[1]); put(px, 12, y, IRON[0])
    for x in range(3, 13):
        put(px, x, 4, IRON[2])
    put(px, 3, 4, IRON[3])
    for x in range(3, 13):
        put(px, x, 15, IRON[0])
    return px


def steam_pipe():
    """A copper pipe running sideways: rounded shading, a flange ring and rivets."""
    px = base("block/copper_block", COPPER)
    for y in range(16):
        t = math.sin((y + 0.5) / 16 * math.pi)  # round: bright in the middle, dark at the edges
        for x in range(16):
            blend(px, x, y, (0, 0, 0), (1 - t) * 0.55)
            if 5 <= y <= 6:
                blend(px, x, y, (255, 220, 190), 0.25)
    for x in (0, 1, 14, 15):  # flanges at both ends
        for y in range(16):
            put(px, x, y, shade(BRASS, 0.2 + 0.6 * math.sin((y + 0.5) / 16 * math.pi)))
    for y in (3, 8, 12):
        stud(px, 0, y, BRASS)
        stud(px, 14, y, BRASS)
    return px


# ---------------- Items ----------------

def gear_item(palette, teeth=8):
    px = blank()
    gear(px, 8, 8, 7.4, 5.6, 2.0, teeth, palette)
    outline(px, palette[0])
    return px


def bronze_plate():
    """A square of hammered bronze sheet with punched corner holes."""
    px = blank()
    for y in range(3, 13):
        for x in range(2, 14):
            t = 0.75 - (x - 2) * 0.025 - (y - 3) * 0.035
            put(px, x, y, shade(BRONZE, t))
    bevel(px, 2, 3, 13, 12, light=0.35, dark=0.4)
    for y in range(3, 13):  # diagonal metal sheen
        for x in range(2, 14):
            d = (x - 2) + (y - 3)
            if d in (6, 7):
                blend(px, x, y, (255, 236, 190), 0.45)
            elif d in (5, 8):
                blend(px, x, y, (255, 236, 190), 0.2)
    for x, y in ((3, 4), (12, 4), (3, 11), (12, 11)):  # rivet holes
        put(px, x, y, BRONZE[0])
    outline(px, BRONZE[0])
    return px


def iron_bloom():
    """A spongy lump of iron from the bloomery, still flecked with glowing slag."""
    px = base("item/raw_iron", [(30, 26, 26), (70, 62, 60), (104, 96, 92), (150, 140, 134)])
    pores = [(5, 5), (9, 6), (7, 9), (11, 9), (6, 11), (10, 4)]
    for x, y in pores:
        darken(px, x, y, 0.6)
    for x, y in ((8, 7), (4, 8), (12, 7)):
        blend(px, x, y, FIRE[2], 0.75)
    return px


def rivets():
    px = blank()
    for cx, cy in ((5, 5), (11, 6), (7, 11)):
        disc(px, cx, cy, 2.6, IRON)
        for y in range(cy + 2, cy + 5):  # shank
            put(px, cx - 1, y, IRON[1]); put(px, cx, y, IRON[0])
    outline(px, IRON[0])
    return px


def pressure_gauge():
    px = blank()
    fill(px, 7, 12, 8, 15, BRASS[1])  # pipe stem
    put(px, 7, 12, BRASS[3]); put(px, 7, 13, BRASS[2])
    ring(px, 8, 7, 6.8, 4.6, BRASS)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 7)
            if d < 4.6:
                put(px, x, y, mix((246, 236, 210), (210, 196, 160), d / 4.6))
    for i in range(5):  # scale ticks over the top half
        a = math.pi * (1 + i / 4)
        put(px, int(8 + math.cos(a) * 3.4), int(7 + math.sin(a) * 3.4), (40, 30, 20))
    put(px, 11, 5, (150, 30, 24))  # red line at the top of the scale
    for x, y in ((8, 7), (9, 6), (10, 5)):  # needle, nearly in the red
        put(px, x, y, (24, 18, 12))
    put(px, 7, 7, (30, 22, 16))
    for x, y in ((5, 4), (5, 5), (6, 4)):
        blend(px, x, y, (255, 255, 255), 0.5)
    outline(px, BRASS[0])
    return px


def steam_piston():
    rows = ["................",
            "....OOOOOOOO....",
            "...OBHHHMMMDO...",
            "...OBHMMMMMDO...",
            "...OODDDDDDOO...",
            "....OSHHMMDO....",
            "....OSHMMMDO....",
            "....OSHMMMDO....",
            "....OOOOOOOO....",
            "......OHDO......",
            "......OHDO......",
            "......OHDO......",
            ".....OOHDOO.....",
            ".....OBBBBO.....",
            ".....OOOOOO.....",
            "................"]
    pal = {"O": (30, 30, 34), "D": STEEL[1], "M": STEEL[2], "H": STEEL[3], "S": STEEL[1], "B": BRASS[2]}
    return from_rows(rows, pal)


def unfired_amphora():
    rows = ["................",
            "......OOOO......",
            "......OHMO......",
            ".....OOHMOO.....",
            "....OMOHMDOMO...",
            "....O.OHMDO.O...",
            ".....OHMMMDO....",
            "....OHHMMMDDO...",
            "....OHMMMMMDO...",
            "....OHMMMMMDO...",
            ".....OHMMMDO....",
            ".....OMMMMDO....",
            "......OMMDO.....",
            ".......ODO......",
            "......OOOOO.....",
            "................"]
    return from_rows(rows, {"O": CLAY[0], "D": CLAY[1], "M": CLAY[2], "H": CLAY[3]})


def with_stick(px, x0=0, y0=0):
    """Pastes vanilla's stick handle under a tool head (handle runs bottom-left to the middle)."""
    stick = base("item/stick")
    out = copy(stick)
    for y in range(16):
        for x in range(16):
            if px[y][x][3] > 0:
                out[y][x] = px[y][x]
    return out


def smiths_hammer():
    """A heavy bronze hammer head across the top of a wooden handle."""
    head = blank()
    cx, cy = 11.5, 3.5
    for y in range(16):
        for x in range(16):
            length = (x - cx) + (y - cy)      # runs top-left to bottom-right, across the handle
            thick = (x - cx) - (y - cy)
            if abs(length) <= 5.4 and abs(thick) <= 2.6:
                t = 0.68 - thick * 0.14 - length * 0.04
                put(head, x, y, shade(BRONZE, t))
    outline(head, BRONZE[0])
    for x, y in ((8, 1), (14, 8)):  # bright striking faces
        blend(head, x, y, (255, 230, 170), 0.3)
    return with_stick(head)


def shift(px, dx, dy):
    out = blank()
    for y in range(16):
        for x in range(16):
            if 0 <= x - dx < 16 and 0 <= y - dy < 16:
                out[y][x] = px[y - dy][x - dx]
    return out


def hand_drill():
    """A brace drill: crank handle, steel bit."""
    rows = ["................",
            "............OO..",
            "...........OHO..",
            "..........OHDO..",
            "...OOOO..OHDO...",
            "..OWWwO.OHDO....",
            "..OWwOOOHDO.....",
            "...OOOOHDO......",
            "......OHDO......",
            ".....OHDO.......",
            "....OSsO........",
            "...OSsO.........",
            "..OSsO..........",
            "..OsO...........",
            "...O............",
            "................"]
    return from_rows(rows, {"O": (30, 26, 22), "H": IRON[3], "D": IRON[1], "W": OAK[3], "w": OAK[1],
                            "S": STEEL[3], "s": STEEL[1]})


def steam_drill():
    rows = ["................",
            "..OOOOOO........",
            "..OBBbbbO.......",
            "..OBbGgbOOO.....",
            "..ObbggbOSsO....",
            "..ObbbbbOsSsO...",
            "...OOObOOOsSsO..",
            ".....ObO...OsSO.",
            ".....OwO....OsO.",
            ".....OwO.....O..",
            "....OOBOO.......",
            "....OwwwO.......",
            ".....OOO........"]
    rows += ["................"] * (16 - len(rows))
    return from_rows(rows, {"O": (30, 24, 18), "B": BRASS[3], "b": BRASS[2], "G": (240, 236, 220), "g": (150, 30, 24),
                            "S": STEEL[3], "s": STEEL[1], "w": DARK_WOOD[2]})


def pneumatic_axe():
    """Vanilla's axe shape in steel, with a brass air cylinder clamped behind the head."""
    from recolor_vanilla import tool
    px = tool("item/iron_axe", STEEL)
    for x, y in ((8, 7), (9, 8), (7, 8), (8, 9)):
        put(px, x, y, BRASS[2])
    put(px, 8, 7, BRASS[3]); put(px, 8, 9, BRASS[0])
    return px


def engine_casing():
    """Heavy riveted iron plate with a cross brace: the steam engine's frame."""
    px = base("block/iron_block", DARK_IRON)
    bevel(px, 0, 0, 15, 15, light=0.3, dark=0.5)
    for i in range(2, 14):  # diagonal braces, raised
        for x, y in ((i, i), (15 - i, i)):
            put(px, x, y, IRON[1])
            lighten(px, x, y - 1, 0.18)
            darken(px, x, y + 1, 0.3)
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13), (7, 7)):
        rivet(px, x, y, IRON)
    for x in range(4, 12, 3):
        stud(px, x, 1, IRON); stud(px, x, 14, IRON)
    return px


def steam_cylinder():
    """A steel cylinder lying sideways, with brass flange bands and a steam port."""
    px = base("block/iron_block", STEEL)
    for y in range(16):  # round: bright in the middle
        t = math.sin((y + 0.5) / 16 * math.pi)
        for x in range(16):
            blend(px, x, y, (0, 0, 0), (1 - t) * 0.5)
            if 4 <= y <= 5:
                blend(px, x, y, (255, 255, 255), 0.15)
    for x in (0, 1, 14, 15):
        for y in range(16):
            put(px, x, y, shade(BRASS, 0.15 + 0.65 * math.sin((y + 0.5) / 16 * math.pi)))
    for y in (3, 8, 12):
        stud(px, 0, y, BRASS); stud(px, 14, y, BRASS)
    for x in range(3, 13, 3):
        stud(px, x, 2, STEEL); stud(px, x, 13, STEEL)
    return px


def steam_cylinder_end():
    """The end of a steam cylinder: a riveted brass flange round a steam port."""
    px = base("block/iron_block", STEEL)
    inset(px, 0, 0, 15, 15, light=0.15, dark=0.4)
    ring(px, 8, 8, 7.4, 5.0, BRASS)
    for a in range(8):
        ang = a * math.pi / 4
        stud(px, int(8 + math.cos(ang) * 6.2), int(8 + math.sin(ang) * 6.2), BRASS)
    disc(px, 8, 8, 3.0, COPPER)
    disc(px, 8, 8, 1.6, [(14, 12, 12), (24, 22, 22), (34, 32, 32), (44, 42, 42)], dome=False)
    return px


if __name__ == "__main__":
    write_png(f"{OUT}/block/amphora.png", amphora())
    write_png(f"{OUT}/block/banded_crate.png", banded_crate())
    write_png(f"{OUT}/block/strongbox_side.png", strongbox_side())
    write_png(f"{OUT}/block/strongbox_front.png", strongbox_front())
    write_png(f"{OUT}/block/gear_train_case.png", gear_train_case())
    write_png(f"{OUT}/block/gear_train_dial.png", dial(-math.pi / 2))
    write_png(f"{OUT}/block/gear_train_dial_running.png", gear_train_dial_frames())
    write_png(f"{OUT}/block/gearbox.png", gearbox())
    write_png(f"{OUT}/block/flywheel.png", flywheel())
    write_png(f"{OUT}/block/boiler_side.png", boiler_side())
    write_png(f"{OUT}/block/boiler_top.png", boiler_top())
    write_png(f"{OUT}/block/coke_oven_side.png", coke_oven_side())
    write_png(f"{OUT}/block/coke_oven_front.png", coke_oven_front(False))
    write_png(f"{OUT}/block/coke_oven_front_lit.png", coke_oven_front(True))
    write_png(f"{OUT}/block/steam_pipe.png", steam_pipe())
    write_png(f"{OUT}/block/engine_casing.png", engine_casing())
    write_png(f"{OUT}/block/steam_cylinder.png", steam_cylinder())
    write_png(f"{OUT}/block/steam_cylinder_end.png", steam_cylinder_end())
    write_png(f"{OUT}/item/bronze_plate.png", bronze_plate())
    write_png(f"{OUT}/item/iron_bloom.png", iron_bloom())
    write_png(f"{OUT}/item/iron_gear.png", gear_item(IRON))
    write_png(f"{OUT}/item/wooden_gear.png", gear_item(OAK, teeth=6))
    write_png(f"{OUT}/item/rivets.png", rivets())
    write_png(f"{OUT}/item/pressure_gauge.png", pressure_gauge())
    write_png(f"{OUT}/item/steam_piston.png", steam_piston())
    write_png(f"{OUT}/item/unfired_amphora.png", unfired_amphora())
    write_png(f"{OUT}/item/smiths_hammer.png", smiths_hammer())
    write_png(f"{OUT}/item/hand_drill.png", hand_drill())
    write_png(f"{OUT}/item/steam_drill.png", steam_drill())
    write_png(f"{OUT}/item/pneumatic_axe.png", pneumatic_axe())
    print("early-era textures written")
