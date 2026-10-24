"""Electrical- and Information-era textures, built on vanilla materials. Run from the project root:
    python3 tools/make_textures_mid.py"""
import math

from pixelart import write_png
from recolor_vanilla import tool
from texture_kit import (BRASS, COPPER, DARK_IRON, FIRE, FIREBRICK, IRON, STEEL, base, bevel, blank, blend, band,
                         copy, darken, disc, fill, frame, from_rows, gear, glow, inset, lighten, mix, outline, put,
                         ring, rivet, shade, stud, tint)

OUT = "src/main/resources/assets/epochsmith/textures"
BAKELITE = [(14, 12, 12), (30, 26, 24), (46, 40, 36), (66, 58, 52)]
PANEL = [(150, 156, 168), (196, 202, 212), (222, 226, 234), (242, 244, 248)]  # Information-era white panels
SLATE = [(18, 20, 26), (32, 36, 44), (48, 54, 64), (70, 78, 92)]               # dark rack metal
BLUE = (70, 170, 255)
BLUE_GLOW = (190, 235, 255)
GREEN = (90, 230, 120)
AMBER = (255, 184, 60)
RED = (230, 60, 50)


# ---------------- Shared pieces ----------------

def casing(palette=DARK_IRON, rivets=True):
    """An electrical-era machine casing: dark steel with a raised edge and corner rivets."""
    px = base("block/iron_block", palette)
    bevel(px, 0, 0, 15, 15, light=0.25, dark=0.5)
    if rivets:
        for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
            rivet(px, x, y, IRON)
    return px


def white_panel(seams=True):
    """An Information-era panel: clean white composite with a soft bevel."""
    px = base("block/quartz_block_side", PANEL)
    bevel(px, 0, 0, 15, 15, light=0.4, dark=0.3)
    if seams:
        for x in range(1, 15):
            darken(px, x, 7, 0.12)
            lighten(px, x, 8, 0.25)
    return px


def window(px, x0, y0, x1, y1, inside, frame_palette=IRON):
    """A glass window with a metal frame and a glint. inside is a color or a function(x, y)."""
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            put(px, x, y, inside(x, y) if callable(inside) else inside)
    for x in range(x0, x1 + 1):
        put(px, x, y0, frame_palette[1]); put(px, x, y1, frame_palette[2])
    for y in range(y0, y1 + 1):
        put(px, x0, y, frame_palette[1]); put(px, x1, y, frame_palette[2])
    put(px, x0, y0, frame_palette[0]); put(px, x1, y1, frame_palette[3])
    blend(px, x0 + 1, y0 + 1, (255, 255, 255), 0.35)
    blend(px, x0 + 2, y0 + 1, (255, 255, 255), 0.2)
    blend(px, x0 + 1, y0 + 2, (255, 255, 255), 0.2)


def led(px, x, y, color, on=True):
    if on:
        put(px, x, y, color)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            blend(px, x + dx, y + dy, color, 0.25)
    else:
        put(px, x, y, mix(color, (20, 20, 24), 0.75))


def vents(px, x0, x1, ys, depth=0.55):
    for y in ys:
        for x in range(x0, x1 + 1):
            darken(px, x, y, depth)
            lighten(px, x, y + 1, 0.15)


def hazard(px, y0, y1):
    for y in range(y0, y1 + 1):
        for x in range(16):
            put(px, x, y, (232, 186, 30) if ((x + y) // 2) % 2 else (28, 26, 30))


# ---------------- Electrical era ----------------

def arc_furnace_side():
    px = casing()
    for x in (4, 11):  # copper busbars
        for y in range(2, 14):
            put(px, x, y, shade(COPPER, 0.75 - y * 0.02))
            put(px, x + 1, y, shade(COPPER, 0.4 - y * 0.015))
        stud(px, x, 3, BRASS); stud(px, x, 11, BRASS)
    vents(px, 7, 8, (4, 6, 8, 10), 0.5)
    return px


def arc_furnace_top():
    """Three graphite electrodes poking up through the roof."""
    px = casing()
    for cx, cy in ((5, 5), (11, 6), (7, 11)):
        ring(px, cx, cy, 2.8, 2.0, BRASS)
        disc(px, cx, cy, 2.0, [(20, 20, 24), (40, 40, 46), (64, 64, 72), (100, 100, 110)])
    return px


def arc_furnace_front(on):
    px = arc_furnace_side()
    for x in (4, 5, 10, 11):
        for y in range(4, 13):
            px[y][x] = casing()[y][x]

    def inside(x, y):
        if not on:
            return shade(FIREBRICK, 0.15 + ((x * 3 + y) % 3) * 0.05)
        if y >= 10:  # molten metal pool
            return shade(FIRE, 0.55 + ((x + y) % 3) * 0.1)
        return mix((40, 30, 50), (150, 190, 255), 0.25 if (x + y) % 4 else 0.5)

    window(px, 3, 3, 12, 12, inside)
    if on:  # a jagged arc between two electrodes
        for x, y in ((5, 4), (6, 5), (6, 6), (7, 7), (8, 6), (9, 7), (9, 8), (10, 9)):
            put(px, x, y, BLUE_GLOW)
        glow(px, 7.5, 7, 6, (200, 220, 255), 0.35)
    return px


def battery_side(level):
    """Bakelite case with brass corners and a 4-segment charge meter."""
    px = base("block/polished_blackstone", BAKELITE)
    bevel(px, 0, 0, 15, 15, light=0.2, dark=0.5)
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(px, x, y, BRASS[2])
    fill(px, 5, 2, 10, 13, (8, 8, 8))
    for i in range(4):
        y0 = 11 - i * 3
        lit = i < level
        color = (GREEN if level > 1 else AMBER) if lit else (26, 40, 30)
        fill(px, 6, y0, 9, y0 + 1, color)
        if lit:
            put(px, 6, y0, mix(color, (255, 255, 255), 0.5))
    for x in (2, 13):  # copper side straps
        for y in range(2, 14):
            put(px, x, y, shade(COPPER, 0.6 - y * 0.02))
    return px


def battery_top():
    px = base("block/polished_blackstone", BAKELITE)
    bevel(px, 0, 0, 15, 15, light=0.2, dark=0.5)
    disc(px, 5, 8, 2.6, [(80, 14, 12), (150, 30, 24), (210, 60, 50), (250, 140, 120)])   # + terminal
    disc(px, 11, 8, 2.6, [(10, 10, 10), (40, 40, 44), (80, 80, 86), (130, 130, 140)])    # - terminal
    put(px, 4, 8, (255, 230, 220)); put(px, 5, 7, (255, 230, 220)); put(px, 5, 9, (255, 230, 220)); put(px, 6, 8, (255, 230, 220))
    put(px, 10, 8, (200, 200, 210)); put(px, 12, 8, (200, 200, 210))
    return px


def copper_wire():
    """Twisted copper strands (the wire uses the middle of this texture)."""
    px = base("block/copper_block", COPPER)
    for y in range(16):
        for x in range(16):
            twist = (x + y * 2) % 6
            if twist < 2:
                blend(px, x, y, (255, 214, 180), 0.35)
            elif twist == 5:
                blend(px, x, y, (0, 0, 0), 0.35)
    return px


def dynamo_side():
    """A steel housing with a window onto the copper windings."""
    px = casing()

    def coil(x, y):
        t = 0.8 if y % 2 == 0 else 0.35
        return shade(COPPER, t - abs(x - 7.5) * 0.04)

    window(px, 3, 3, 12, 12, coil, frame_palette=BRASS)
    return px


def dynamo_top():
    px = casing()
    ring(px, 8, 8, 6.0, 4.0, BRASS)
    disc(px, 8, 8, 4.0, DARK_IRON)
    gear(px, 8, 8, 3.6, 2.6, 1.0, 6, STEEL)
    return px


def electric_lamp(on):
    """Vanilla's redstone lamp, warmed up, in a brass frame."""
    if on:
        px = base("block/redstone_lamp_on", [(150, 96, 30), (230, 170, 70), (255, 222, 140), (255, 250, 220)])
    else:
        px = base("block/redstone_lamp", [(40, 34, 28), (74, 64, 52), (110, 98, 82), (150, 138, 120)])
    for i in range(16):
        for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
            put(px, x, y, BRASS[1] if (x + y) % 2 else BRASS[2])
    for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
        put(px, x, y, BRASS[3])
    return px


def sorting_cabinet_side():
    px = casing(STEEL, rivets=False)
    for y in (5, 10):
        for x in range(1, 15):
            darken(px, x, y, 0.3)
            lighten(px, x, y + 1, 0.2)
    return px


def sorting_cabinet_front():
    """Nine steel drawers with brass pulls and paper label slots (items show on the drawers in game)."""
    px = casing(STEEL, rivets=False)
    for cy in (1, 6, 11):
        for cx in (1, 6, 11):
            bevel(px, cx - 1, cy - 1, cx + 4, cy + 4, light=0.3, dark=0.55)
            fill(px, cx + 1, cy + 3, cx + 2, cy + 3, BRASS[2])   # pull
            put(px, cx + 1, cy + 3, BRASS[3])
    return px


# ---------------- Information era ----------------

def solar_panel_top():
    """Deep blue cells on a silver grid, with a sweep of reflected sky."""
    px = blank()
    for y in range(16):
        for x in range(16):
            grid = x % 5 == 0 or y % 5 == 0
            if grid:
                put(px, x, y, (196, 204, 216))
            else:
                cell = (24, 44, 104) if (x + y) % 2 else (30, 54, 122)
                put(px, x, y, cell)
            sweep = (x + y) - 12
            if -1 <= sweep <= 1 and not grid:
                blend(px, x, y, (170, 210, 255), 0.35 if sweep == 0 else 0.18)
    frame(px, (150, 158, 170))
    return px


def fabricator_side():
    px = white_panel()
    for x in range(1, 15):
        put(px, x, 12, BLUE)
        blend(px, x, 13, BLUE, 0.3)
    return px


def fabricator_front(on):
    """A clean-room window: a silicon wafer on a stage, etched by a laser when running."""
    px = fabricator_side()

    def inside(x, y):
        c = (26, 30, 42)
        if y >= 8 and 4 <= x <= 11:  # the wafer
            c = (110, 120, 150) if (x + y) % 2 else (90, 100, 130)
        return c

    window(px, 2, 2, 13, 10, inside, frame_palette=PANEL)
    if on:
        for y in range(3, 8):
            put(px, 8, y, (200, 120, 255))
        glow(px, 8, 8, 4, (200, 120, 255), 0.5)
        put(px, 8, 8, (255, 230, 255))
        led(px, 12, 12, GREEN)
    else:
        led(px, 12, 12, GREEN, on=False)
    return px


def computer_core_side(on):
    """A server rack: three blade servers with status LEDs and vents."""
    px = base("block/polished_deepslate", SLATE)
    bevel(px, 0, 0, 15, 15, light=0.25, dark=0.5)
    for i, y in enumerate((2, 7, 12)):
        fill(px, 2, y - 1, 13, y + 2, SLATE[2])
        bevel(px, 2, y - 1, 13, y + 2, light=0.3, dark=0.5)
        vents(px, 7, 12, (y,), 0.6)
        colors = (GREEN, BLUE, AMBER)
        for j, x in enumerate((3, 5)):
            blink = on and (i + j) % 3 != 2
            led(px, x, y, colors[(i + j) % 3], blink)
    return px


def computer_core_top():
    px = base("block/polished_deepslate", SLATE)
    bevel(px, 0, 0, 15, 15, light=0.25, dark=0.5)
    ring(px, 8, 8, 6.5, 5.5, SLATE)
    for y in range(3, 13):  # fan grille
        for x in range(3, 13):
            if math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 5.0 and (x + y) % 2 == 0:
                darken(px, x, y, 0.6)
    disc(px, 8, 8, 1.6, SLATE)
    return px


def data_vault_side(on):
    """Drive bays, each with an activity light, behind a dark front."""
    px = base("block/polished_deepslate", SLATE)
    bevel(px, 0, 0, 15, 15, light=0.25, dark=0.5)
    for i, y in enumerate((2, 6, 10)):
        fill(px, 2, y, 13, y + 2, SLATE[2])
        bevel(px, 2, y, 13, y + 2, light=0.3, dark=0.5)
        fill(px, 4, y + 1, 9, y + 1, SLATE[0])
        led(px, 12, y + 1, GREEN if i == 1 else BLUE, on)
    for x in range(2, 14):
        put(px, x, 14, BLUE if on else SLATE[1])
    return px


def data_vault_top():
    px = base("block/polished_deepslate", SLATE)
    bevel(px, 0, 0, 15, 15, light=0.25, dark=0.5)
    window(px, 4, 4, 11, 11, lambda x, y: (40, 90, 170) if (x + y) % 2 else (50, 110, 200), frame_palette=SLATE)
    return px


def induction_furnace_side():
    """Brushed steel with a band of copper induction coil."""
    px = base("block/iron_block", STEEL)
    bevel(px, 0, 0, 15, 15, light=0.3, dark=0.45)
    for y in range(5, 11):
        for x in range(1, 15):
            put(px, x, y, shade(COPPER, 0.85 if y % 2 else 0.35))
    for x in range(1, 15):
        darken(px, x, 4, 0.4); darken(px, x, 11, 0.2)
    return px


def induction_furnace_front(on):
    """A crucible glowing inside the coil."""
    px = induction_furnace_side()

    def inside(x, y):
        if not on:
            return (34, 28, 28) if y < 10 else (70, 60, 56)
        heat = 1 - math.hypot(x - 7.5, y - 9) / 6
        return shade(FIRE, 0.35 + max(0, heat) * 0.65)

    window(px, 3, 2, 12, 13, inside, frame_palette=STEEL)
    if on:
        glow(px, 7.5, 9, 7, FIRE[2], 0.3)
    return px


def assembler_side():
    px = white_panel()
    hazard(px, 12, 13)
    return px


def assembler_front(on):
    """A robotic arm inside a lit work cell; it welds when running."""
    px = assembler_side()
    window(px, 2, 1, 13, 10, (28, 34, 46), frame_palette=PANEL)
    arm = (255, 170, 60) if on else (160, 164, 174)
    for x, y in ((4, 3), (5, 3), (6, 3), (7, 3), (7, 4), (8, 5), (8, 6), (8, 7)):
        put(px, x, y, arm)
    put(px, 4, 3, (90, 94, 104)); put(px, 4, 2, (90, 94, 104))  # mount
    put(px, 7, 8, arm); put(px, 9, 8, arm)                          # gripper
    if on:
        put(px, 8, 8, (255, 255, 230))
        glow(px, 8, 8, 3.5, (140, 220, 255), 0.6)
    return px


# ---------------- Items ----------------

def light_bulb():
    px = blank()
    disc(px, 8, 6, 5.0, [(150, 160, 170), (200, 214, 222), (232, 240, 246), (255, 255, 255)])
    for x, y in ((7, 5), (8, 4), (9, 5), (8, 6), (7, 7), (9, 7)):  # filament
        put(px, x, y, (255, 190, 80))
    for y in range(10, 14):  # brass screw base
        for x in range(6, 11):
            put(px, x, y, shade(BRASS, 0.8 if y % 2 == 0 else 0.35))
    fill(px, 7, 14, 9, 14, (40, 34, 28))
    blend(px, 6, 3, (255, 255, 255), 0.7)
    outline(px, (60, 64, 70))
    return px


def electric_drill():
    """A copper motor housing, bakelite grip and a steel bit."""
    rows = ["................",
            "................",
            "..OOOOOOO.......",
            ".OCHHHCCCO......",
            ".OCHCCCCcOOOO...",
            ".OCCCCCCcOSssO..",
            ".OcccccccOsSSsO.",
            "..OOOObbOOOOsSO.",
            "......ObbO..OsO.",
            ".....ObbbO...O..",
            ".....ObbO.......",
            ".....OBbO.......",
            "....ObbbbO......",
            "....OOOOOO......"]
    return from_rows(rows, {"O": (20, 16, 14), "C": COPPER[2], "H": COPPER[3], "c": COPPER[1],
                            "S": STEEL[3], "s": STEEL[1], "b": BAKELITE[2], "B": BAKELITE[3]})


def chainsaw():
    rows = ["................",
            "................",
            "......OOOOOOOOO.",
            "....OOsSsSsSsSsO",
            "..OORRO........O",
            ".ORHRRO.sSsSsSsO",
            ".ORRRRrOOOOOOOO.",
            ".OrrrrrO........",
            "..OOObbO........",
            "....ObbO........",
            "....OOO........."]
    px = from_rows(rows, {"O": (20, 16, 16), "R": (200, 50, 40), "H": (250, 120, 100), "r": (130, 28, 24),
                          "S": STEEL[3], "s": STEEL[1], "b": BAKELITE[2]})
    for x in range(8, 15):  # the bar inside the chain
        put(px, x, 4, (90, 96, 104))
    return px


def silicon():
    """A crystal of silicon: vanilla's quartz shape in metallic blue-grey."""
    return base("item/quartz", [(30, 34, 44), (70, 78, 96), (118, 130, 156), (190, 204, 230)])


def transistor():
    rows = ["................",
            "................",
            ".....OOOOOO.....",
            "....OHMMMMDO....",
            "....OMMMMMDO....",
            "....OMWMMMDO....",
            "....ODDDDDDO....",
            ".....OOOOOO.....",
            ".....L..L..L....",
            ".....L..L..L....",
            ".....L..L..L....",
            ".....l..l..l....",
            ".....l..l..l...."]
    return from_rows(rows, {"O": (10, 10, 12), "H": (80, 80, 90), "M": (46, 46, 52), "D": (30, 30, 34),
                            "W": (180, 180, 190), "L": (220, 224, 230), "l": (150, 154, 162)})


def circuit_board():
    """Green board, gold traces, a black chip and two capacitors."""
    px = blank()
    for y in range(2, 14):
        for x in range(1, 15):
            put(px, x, y, (36, 120, 62) if (x + y) % 2 else (32, 110, 56))
    bevel(px, 1, 2, 14, 13, light=0.25, dark=0.45)
    gold = (232, 190, 80)
    for x in range(2, 14):
        put(px, x, 4, gold) if x not in (6, 7) else None
    for y in range(4, 12):
        put(px, 3, y, gold)
    for x in range(3, 13):
        put(px, x, 11, gold) if x % 3 else None
    fill(px, 6, 5, 10, 9, (24, 24, 28))       # chip
    for x in range(6, 11):
        put(px, x, 4, (200, 204, 210)); put(px, x, 10, (200, 204, 210))
    put(px, 6, 5, (90, 90, 100))
    disc(px, 12.5, 7.5, 1.4, [(20, 40, 90), (40, 80, 170), (80, 130, 220), (160, 200, 255)])
    disc(px, 12.5, 10.5, 1.0, [(80, 60, 10), (150, 110, 30), (210, 170, 60), (250, 220, 120)])
    outline(px, (16, 50, 28))
    return px


def laser_cutter():
    """A drill-shaped tool with a glowing blue lens."""
    rows = ["................",
            "................",
            "...........OO...",
            "..........ObBO..",
            ".........ObWBO..",
            "...OOOOOOOMBO...",
            "..OHHMMMMMDO....",
            "..OHMMMMMDDO....",
            "..ODDDOODDO.....",
            "...OOOODRDO.....",
            "......ODDO......",
            "......ODDO......",
            "......OHDO......",
            ".......OO......."]
    px = from_rows(rows, {"O": (14, 16, 22), "D": (44, 50, 62), "M": (82, 92, 110), "H": (150, 166, 190),
                          "B": (60, 200, 255), "b": (30, 120, 200), "W": (220, 250, 255), "R": RED})
    return px


def multitool():
    """Vanilla's pickaxe shape with a steel head and a glowing core."""
    px = tool("item/iron_pickaxe", STEEL)
    for x, y in ((7, 7), (8, 8)):
        put(px, x, y, BLUE)
    put(px, 7, 8, BLUE_GLOW)
    return px


if __name__ == "__main__":
    write_png(f"{OUT}/block/arc_furnace_side.png", arc_furnace_side())
    write_png(f"{OUT}/block/arc_furnace_top.png", arc_furnace_top())
    write_png(f"{OUT}/block/arc_furnace_front.png", arc_furnace_front(False))
    write_png(f"{OUT}/block/arc_furnace_front_on.png", arc_furnace_front(True))
    for level in range(5):
        write_png(f"{OUT}/block/battery_side_{level}.png", battery_side(level))
    write_png(f"{OUT}/block/battery_top.png", battery_top())
    write_png(f"{OUT}/block/copper_wire.png", copper_wire())
    write_png(f"{OUT}/block/dynamo_side.png", dynamo_side())
    write_png(f"{OUT}/block/dynamo_top.png", dynamo_top())
    write_png(f"{OUT}/block/electric_lamp.png", electric_lamp(False))
    write_png(f"{OUT}/block/electric_lamp_on.png", electric_lamp(True))
    write_png(f"{OUT}/block/sorting_cabinet_side.png", sorting_cabinet_side())
    write_png(f"{OUT}/block/sorting_cabinet_front.png", sorting_cabinet_front())
    write_png(f"{OUT}/block/solar_panel_top.png", solar_panel_top())
    write_png(f"{OUT}/block/fabricator_side.png", fabricator_side())
    write_png(f"{OUT}/block/fabricator_front.png", fabricator_front(False))
    write_png(f"{OUT}/block/fabricator_front_on.png", fabricator_front(True))
    write_png(f"{OUT}/block/computer_core_side.png", computer_core_side(False))
    write_png(f"{OUT}/block/computer_core_side_on.png", computer_core_side(True))
    write_png(f"{OUT}/block/computer_core_top.png", computer_core_top())
    write_png(f"{OUT}/block/data_vault_side.png", data_vault_side(False))
    write_png(f"{OUT}/block/data_vault_side_on.png", data_vault_side(True))
    write_png(f"{OUT}/block/data_vault_top.png", data_vault_top())
    write_png(f"{OUT}/block/induction_furnace_side.png", induction_furnace_side())
    write_png(f"{OUT}/block/induction_furnace_front.png", induction_furnace_front(False))
    write_png(f"{OUT}/block/induction_furnace_front_on.png", induction_furnace_front(True))
    write_png(f"{OUT}/block/assembler_side.png", assembler_side())
    write_png(f"{OUT}/block/assembler_front.png", assembler_front(False))
    write_png(f"{OUT}/block/assembler_front_on.png", assembler_front(True))
    write_png(f"{OUT}/item/light_bulb.png", light_bulb())
    write_png(f"{OUT}/item/electric_drill.png", electric_drill())
    write_png(f"{OUT}/item/chainsaw.png", chainsaw())
    write_png(f"{OUT}/item/silicon.png", silicon())
    write_png(f"{OUT}/item/transistor.png", transistor())
    write_png(f"{OUT}/item/circuit_board.png", circuit_board())
    write_png(f"{OUT}/item/laser_cutter.png", laser_cutter())
    write_png(f"{OUT}/item/multitool.png", multitool())
    print("mid-era textures written")
