"""Future-era textures, built on vanilla materials. Run from the project root:
    python3 tools/make_textures_future.py"""
import math

from pixelart import write_png
from recolor_vanilla import tool
from texture_kit import (base, bevel, blank, blend, darken, disc, fill, frame, from_rows, glow, inset, lighten, mix,
                         outline, put, ring, shade, tint)
from make_textures_mid import led, window

OUT = "src/main/resources/assets/epochsmith/textures"
PEARL = [(160, 156, 180), (208, 206, 222), (232, 230, 242), (250, 250, 255)]
VOID = [(10, 8, 18), (22, 18, 36), (36, 30, 58), (58, 50, 88)]
PLASMA = [(90, 20, 150), (170, 60, 240), (230, 140, 255), (255, 240, 255)]
CYAN = [(20, 90, 150), (40, 170, 230), (120, 230, 255), (230, 252, 255)]
VIOLET = (170, 80, 240)


def pearl_panel(seam_color=VIOLET):
    px = base("block/quartz_block_side", PEARL)
    bevel(px, 0, 0, 15, 15, light=0.4, dark=0.3)
    for x in range(1, 15):
        put(px, x, 12, seam_color)
        blend(px, x, 13, seam_color, 0.35)
        blend(px, x, 11, seam_color, 0.2)
    return px


def void_casing():
    px = base("block/polished_deepslate", VOID)
    bevel(px, 0, 0, 15, 15, light=0.3, dark=0.5)
    return px


def sparkle(px, x, y, color):
    put(px, x, y, (255, 255, 255))
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        blend(px, x + dx, y + dy, color, 0.7)


# ---------------- Blocks ----------------

def nanofabricator_side():
    return pearl_panel()


def nanofabricator_front(on):
    """A chamber where a cloud of nanomachines builds something atom by atom."""
    px = pearl_panel()
    window(px, 2, 2, 13, 10, (16, 12, 28), frame_palette=PEARL)
    if on:
        glow(px, 7.5, 6, 5, PLASMA[1], 0.55)
        for x, y in ((5, 4), (9, 3), (7, 6), (10, 7), (4, 8), (8, 8)):
            sparkle(px, x, y, PLASMA[2])
    else:
        for x, y in ((5, 5), (9, 4), (8, 8)):
            put(px, x, y, (70, 60, 90))
    led(px, 13, 12, (120, 255, 160) if on else (60, 60, 70), True)
    return px


def fusion_reactor_side(lit):
    """Superconducting magnets ring a viewport onto the plasma."""
    px = void_casing()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 5.0 <= d < 7.0:  # magnet ring, segmented
                seg = int((math.atan2(y + 0.5 - 8, x + 0.5 - 8) + math.pi) / (math.pi / 4))
                put(px, x, y, shade(CYAN, 0.75 if seg % 2 else 0.45) if lit else shade(VOID, 0.9 if seg % 2 else 0.7))
            elif d < 5.0:
                if lit:
                    put(px, x, y, shade(PLASMA, 1.0 - d / 5.0 * 0.85))
                else:
                    put(px, x, y, (14, 10, 22))
    if lit:
        glow(px, 8, 8, 8, PLASMA[2], 0.25)
        for x, y in ((6, 6), (7, 6)):
            put(px, x, y, (255, 255, 255))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(px, x, y, shade(CYAN, 0.6) if lit else VOID[3])
    return px


def fusion_reactor_top():
    px = void_casing()
    ring(px, 8, 8, 6.5, 5.0, CYAN)
    for y in range(4, 12):
        for x in range(4, 12):
            if math.hypot(x + 0.5 - 8, y + 0.5 - 8) < 4.0 and (x + y) % 2:
                darken(px, x, y, 0.5)
    disc(px, 8, 8, 1.5, CYAN)
    return px


def plasma_smelter_side():
    px = pearl_panel()
    for x in range(1, 15, 3):  # heat vents
        for y in (3, 4, 5):
            put(px, x, y, (60, 30, 90))
            put(px, x + 1, y, (40, 20, 60))
    return px


def plasma_smelter_front(lit):
    px = plasma_smelter_side()

    def inside(x, y):
        if not lit:
            return (20, 14, 30)
        d = abs(x - 7.5) + abs(y - 6.5) * 0.8
        return shade(PLASMA, max(0.1, 1.0 - d / 6))

    window(px, 3, 1, 12, 11, inside, frame_palette=PEARL)
    if lit:
        glow(px, 7.5, 6.5, 7, PLASMA[2], 0.3)
    return px


def quantum_cell_side(on):
    """Vanilla's amethyst, darkened, around a glowing entangled core."""
    px = base("block/amethyst_block", VOID)
    bevel(px, 0, 0, 15, 15, light=0.35, dark=0.5)
    fill(px, 4, 4, 11, 11, (8, 6, 16))
    if on:
        for y in range(5, 11):
            for x in range(5, 11):
                t = 1 - max(abs(x - 7.5), abs(y - 7.5)) / 3.2
                put(px, x, y, mix(PLASMA[1], CYAN[2], (x + y - 10) / 10) if t < 0.6 else (240, 230, 255))
        glow(px, 7.5, 7.5, 7, (190, 160, 255), 0.35)
    else:
        for y in range(5, 11):
            for x in range(5, 11):
                put(px, x, y, (40, 32, 64) if (x + y) % 2 else (32, 26, 52))
    inset(px, 4, 4, 11, 11, light=0.2, dark=0.5)
    return px


def quantum_cell_top():
    px = base("block/amethyst_block", VOID)
    bevel(px, 0, 0, 15, 15, light=0.35, dark=0.5)
    for i in range(16):  # lattice lines
        for x, y in ((i, i), (15 - i, i)):
            blend(px, x, y, CYAN[1], 0.35)
    disc(px, 8, 8, 2.2, CYAN)
    return px


def matter_printer_side():
    return pearl_panel(seam_color=CYAN[1])


def matter_printer_front(on):
    """A print bed under a nozzle; when printing, a cone of light builds the item from matter."""
    px = matter_printer_side()
    window(px, 2, 1, 13, 10, (12, 18, 28), frame_palette=PEARL)
    fill(px, 6, 2, 9, 2, (150, 156, 170))  # nozzle
    put(px, 7, 3, (110, 116, 130)); put(px, 8, 3, (110, 116, 130))
    fill(px, 3, 9, 12, 9, (90, 96, 110))   # print bed
    if on:
        for y in range(4, 9):
            half = (y - 3) // 2 + 1
            for x in range(8 - half, 8 + half):
                blend(px, x, y, CYAN[2], 0.45)
        for x, y in ((6, 7), (7, 7), (8, 7), (9, 7), (7, 6), (8, 6), (6, 8), (9, 8), (7, 8), (8, 8)):
            put(px, x, y, CYAN[3] if (x + y) % 2 else CYAN[2])  # the item taking shape
        glow(px, 7.5, 7, 4, CYAN[2], 0.4)
    return px


def hover_pad_top(on):
    """Concentric anti-gravity emitter rings in a pearl rim."""
    px = base("block/quartz_block_side", PEARL)
    ring(px, 8, 8, 8.0, 6.8, PEARL)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 6.8:
                put(px, x, y, (30, 34, 52))
                if int(d) in (1, 3, 5):
                    put(px, x, y, shade(CYAN, 0.9 - d * 0.08) if on else (70, 76, 100))
    if on:
        glow(px, 8, 8, 6, CYAN[2], 0.35)
        put(px, 7, 7, (255, 255, 255)); put(px, 8, 8, (255, 255, 255))
    return px


def hover_pad_side():
    px = base("block/quartz_block_side", PEARL)
    for x in range(16):
        put(px, x, 11, (250, 250, 255))
        put(px, x, 12, CYAN[2])
        blend(px, x, 13, CYAN[1], 0.5)
        put(px, x, 15, PEARL[0])
    return px


def wireless_node():
    """The emitter: a glowing crystal tip on a dark mast (the model uses the top rows for the tip)."""
    px = base("block/polished_deepslate", VOID)
    for y in range(0, 4):
        for x in range(16):
            put(px, x, y, shade(CYAN, 1.0 - y * 0.15 - ((x + y) % 3) * 0.05))
    for x in range(16):
        put(px, x, 4, PEARL[2])
        put(px, x, 9, PEARL[1])
    for y in range(5, 16):
        lighten(px, 6, y, 0.15)
        darken(px, 9, y, 0.2)
    return px


# ---------------- Items ----------------

def graphene():
    """A sheet of carbon one atom thick: a honeycomb lattice with a soft sheen."""
    px = blank()
    for y in range(2, 14):
        for x in range(2, 14):
            put(px, x, y, (34, 36, 46))
    hexes = [(4, 3), (8, 3), (12, 3), (6, 6), (10, 6), (4, 9), (8, 9), (12, 9), (6, 12), (10, 12)]
    for cx, cy in hexes:
        for dx, dy in ((-1, -1), (0, -1), (1, -1), (-2, 0), (2, 0), (-1, 1), (0, 1), (1, 1)):
            put(px, cx + dx, cy + dy, (120, 130, 160))
    for y in range(2, 14):  # diagonal sheen
        for x in range(2, 14):
            if (x + y) in (11, 12):
                blend(px, x, y, (220, 230, 255), 0.35)
    bevel(px, 2, 2, 13, 13, light=0.2, dark=0.4)
    outline(px, (12, 12, 18))
    return px


def nanobots():
    """A small swarm of silver nanomachines, each with a glowing eye and little legs."""
    px = blank()
    body = [(210, 220, 232), (150, 160, 176), (80, 86, 100)]
    for cx, cy in ((4, 4), (11, 3), (8, 8), (3, 11), (12, 11)):
        for dx, dy, shade_i in ((0, 0, 0), (1, 0, 1), (0, 1, 1), (1, 1, 2), (-1, 0, 1), (-1, 1, 2), (0, -1, 0)):
            put(px, cx + dx, cy + dy, body[shade_i])
        put(px, cx, cy, (120, 255, 200))                        # eye
        put(px, cx - 2, cy + 2, (90, 96, 110)); put(px, cx + 2, cy + 2, (90, 96, 110))  # legs
    return px


def superconductor_coil():
    """A frost-cold coil glowing with current."""
    px = blank()
    for y in range(2, 14):
        for x in range(3, 13):
            put(px, x, y, shade(CYAN, 0.85 if y % 2 == 0 else 0.35) if 4 <= x <= 11 else (70, 76, 92))
    for x in range(3, 13):
        put(px, x, 2, (110, 118, 136)); put(px, x, 13, (50, 54, 66))
    for y in range(2, 14):
        lighten(px, 5, y, 0.2)
        darken(px, 10, y, 0.25)
    for x, y in ((4, 1), (11, 1), (2, 6), (13, 9), (6, 14)):  # frost sparkles
        put(px, x, y, (230, 250, 255))
    outline(px, (16, 20, 30))
    return px


def plasma_cell():
    """A glass capsule holding a swirl of plasma, capped in dark metal."""
    px = blank()
    for y in range(3, 13):
        for x in range(5, 11):
            d = abs(x - 7.5) / 3
            swirl = math.sin((y + x * 0.7) * 1.3) * 0.2
            put(px, x, y, shade(PLASMA, 0.85 - d * 0.6 + swirl))
    for y in (1, 2, 13, 14):
        for x in range(5, 11):
            put(px, x, y, shade([(20, 20, 28), (50, 50, 62), (90, 90, 106), (140, 140, 160)], 0.7 if y in (1, 13) else 0.4))
    for y in range(4, 11):
        put(px, 6, y, mix(px[y][6][:3], (255, 255, 255), 0.45))
    outline(px, (16, 10, 26))
    return px


def nano_multitool():
    """Vanilla's pickaxe shape in nano-black, with glowing cyan edges."""
    nano = [(14, 12, 22), (40, 36, 60), (90, 80, 130), (190, 180, 240)]
    px = tool("item/iron_pickaxe", nano)
    for y in range(16):
        for x in range(16):
            p = px[y][x]
            if p[3] and max(p[:3]) - min(p[:3]) <= 70 and p[2] > 200:  # only the brightest edge pixels glow
                put(px, x, y, CYAN[2])
    for x, y in ((7, 7), (8, 8)):  # glowing core where head meets handle
        put(px, x, y, CYAN[2])
    put(px, 7, 8, (255, 255, 255))
    return px


def mining_laser():
    rows = ["................",
            "................",
            "................",
            "..........OOO...",
            ".OOOOOOOOOHBBO..",
            "OHHMMMMMMMMBWBO.",
            "OMMMMDDDDDMBBO..",
            "ODDDDOOOODDOOO..",
            ".OOODPO..OO.....",
            "...ODDO.........",
            "...ODDO.........",
            "...OHDO.........",
            "....OO.........."]
    px = from_rows(rows, {"O": (12, 10, 20), "D": (40, 36, 60), "M": (90, 80, 130), "H": (190, 180, 240),
                          "B": CYAN[1], "W": CYAN[3], "P": PLASMA[2]})
    glow(px, 12, 5, 3, CYAN[2], 0.3)
    return px


if __name__ == "__main__":
    write_png(f"{OUT}/block/nanofabricator_side.png", nanofabricator_side())
    write_png(f"{OUT}/block/nanofabricator_front.png", nanofabricator_front(False))
    write_png(f"{OUT}/block/nanofabricator_front_on.png", nanofabricator_front(True))
    write_png(f"{OUT}/block/fusion_reactor_side.png", fusion_reactor_side(False))
    write_png(f"{OUT}/block/fusion_reactor_side_on.png", fusion_reactor_side(True))
    write_png(f"{OUT}/block/fusion_reactor_top.png", fusion_reactor_top())
    write_png(f"{OUT}/block/plasma_smelter_side.png", plasma_smelter_side())
    write_png(f"{OUT}/block/plasma_smelter_front.png", plasma_smelter_front(False))
    write_png(f"{OUT}/block/plasma_smelter_front_on.png", plasma_smelter_front(True))
    write_png(f"{OUT}/block/quantum_cell_side.png", quantum_cell_side(False))
    write_png(f"{OUT}/block/quantum_cell_side_on.png", quantum_cell_side(True))
    write_png(f"{OUT}/block/quantum_cell_top.png", quantum_cell_top())
    write_png(f"{OUT}/block/matter_printer_side.png", matter_printer_side())
    write_png(f"{OUT}/block/matter_printer_front.png", matter_printer_front(False))
    write_png(f"{OUT}/block/matter_printer_front_on.png", matter_printer_front(True))
    write_png(f"{OUT}/block/hover_pad_top.png", hover_pad_top(False))
    write_png(f"{OUT}/block/hover_pad_top_on.png", hover_pad_top(True))
    write_png(f"{OUT}/block/hover_pad_side.png", hover_pad_side())
    write_png(f"{OUT}/block/wireless_node.png", wireless_node())
    write_png(f"{OUT}/item/graphene.png", graphene())
    write_png(f"{OUT}/item/nanobots.png", nanobots())
    write_png(f"{OUT}/item/superconductor_coil.png", superconductor_coil())
    write_png(f"{OUT}/item/plasma_cell.png", plasma_cell())
    write_png(f"{OUT}/item/nano_multitool.png", nano_multitool())
    write_png(f"{OUT}/item/mining_laser.png", mining_laser())
    print("future-era textures written")
