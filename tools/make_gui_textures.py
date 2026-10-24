"""Draws one machine-screen background per era (176x166, plus that era's slot and output-slot frames).
Run from the project root:  python3 tools/make_gui_textures.py

Layout of each 256x256 texture:
  (0, 0)    176x166  the panel
  (176, 0)  18x18    a slot
  (176, 18) 26x26    the frame around an output slot
  (176, 44) 24x17    an empty progress arrow (vanilla's, with its grey background removed)
  (176, 62) 14x14    an empty flame (likewise)
"""
import math
import random

from pixelart import write_png
from texture_kit import base, blank, mix, put, shade, BRASS, BRONZE, COPPER, DARK_IRON, IRON, OAK, STEEL

OUT = "src/main/resources/assets/epochsmith/textures/gui/era"
W, H = 176, 166


def tiled(tile, width=W, height=H):
    n = len(tile)
    return [[tile[y % n][x % n] for x in range(width)] for y in range(height)]


def soften(px, toward, amount):
    """Pulls a busy block texture toward one colour, so it works as a calm background."""
    for y in range(len(px)):
        for x in range(len(px[0])):
            p = px[y][x]
            px[y][x] = mix(p, toward, amount) + (255,)


def border(px, metal, width=4, rivets=None):
    """A bevelled metal (or wood) border, lit from the top-left, with optional rivets every so often."""
    h, w = len(px), len(px[0])
    for y in range(h):
        for x in range(w):
            d = min(x, y, w - 1 - x, h - 1 - y)
            if d >= width:
                continue
            if d == 0:
                c = (0, 0, 0)
            else:
                lit = x < width or y < width
                t = 0.8 - (d - 1) * 0.12 if lit else 0.35 - (d - 1) * 0.05
                if (x < width and y > h - width) or (y < width and x > w - width):
                    t = 0.5
                c = shade(metal, max(0, min(1, t)))
            px[y][x] = c + (255,)
    # inner edge: a thin dark line, then a highlight, like vanilla's sunken panel
    for x in range(width, w - width):
        px[width][x] = mix(px[width][x][:3], (0, 0, 0), 0.35) + (255,)
        px[h - width - 1][x] = mix(px[h - width - 1][x][:3], (255, 255, 255), 0.15) + (255,)
    for y in range(width, h - width):
        px[y][width] = mix(px[y][width][:3], (0, 0, 0), 0.35) + (255,)
        px[y][w - width - 1] = mix(px[y][w - width - 1][:3], (255, 255, 255), 0.15) + (255,)
    if rivets:
        for x in list(range(10, w - 6, 24)):
            for y in (1, h - 3):
                rivet(px, x, y, rivets)
        for y in list(range(12, h - 6, 24)):
            for x in (1, w - 3):
                rivet(px, x, y, rivets)


def rivet(px, x, y, metal):
    put(px, x, y, metal[-1]); put(px, x + 1, y, metal[-2]); put(px, x, y + 1, metal[-2]); put(px, x + 1, y + 1, metal[0])


def divider(px, y, color, highlight):
    """A line separating the machine from the player's inventory."""
    for x in range(7, W - 7):
        put(px, x, y, color)
        put(px, x, y + 1, highlight)


def slot(dark, light, fill):
    """An 18x18 sunken slot: dark top-left edge, light bottom-right, like vanilla."""
    px = blank(18, 18)
    for y in range(18):
        for x in range(18):
            if x == 17 or y == 17:
                c = light
            elif x == 0 or y == 0:
                c = dark
            else:
                c = fill
            if (x == 17 and y == 0) or (x == 0 and y == 17):
                c = mix(dark, light, 0.5)
            px[y][x] = c + (255,)
    return px


def output_frame(metal):
    """A raised 26x26 frame for output slots."""
    px = blank(26, 26)
    for y in range(26):
        for x in range(26):
            d = min(x, y, 25 - x, 25 - y)
            if d > 3:
                continue
            lit = x < 4 or y < 4
            px[y][x] = shade(metal, 0.8 - d * 0.1 if lit else 0.3 - d * 0.04) + (255,)
            if d == 0:
                px[y][x] = (0, 0, 0, 255)
    return px


def furnace_shapes():
    """Vanilla's empty arrow and flame, cut out of the furnace screen without its grey panel colour."""
    import zipfile
    from recolor_vanilla import GAME, read_png
    furnace = read_png(zipfile.ZipFile(GAME).read("assets/minecraft/textures/gui/container/furnace.png"))

    def cut(x0, y0, w, h):
        out = blank(w, h)
        for y in range(h):
            for x in range(w):
                p = furnace[y0 + y][x0 + x]
                out[y][x] = (0, 0, 0, 0) if p[:3] == (198, 198, 198) else p
        return out

    return cut(79, 34, 24, 17), cut(56, 36, 14, 14)


ARROW, FLAME = furnace_shapes()


def sheet(panel, slot_px, frame_px):
    out = blank(256, 256)
    for y in range(17):
        for x in range(24):
            out[44 + y][176 + x] = ARROW[y][x]
    for y in range(14):
        for x in range(14):
            out[62 + y][176 + x] = FLAME[y][x]
    for y in range(H):
        for x in range(W):
            out[y][x] = panel[y][x]
    for y in range(18):
        for x in range(18):
            out[y][176 + x] = slot_px[y][x]
    for y in range(26):
        for x in range(26):
            out[18 + y][176 + x] = frame_px[y][x]
    return out


FIBER = [(92, 70, 30), (150, 120, 60), (196, 166, 92), (226, 204, 136)]
MUD = [(60, 42, 30), (92, 66, 48), (118, 88, 64), (140, 108, 80)]
BAKELITE = [(14, 12, 12), (30, 26, 24), (46, 40, 36), (66, 58, 52)]
PANEL = [(150, 156, 168), (196, 202, 212), (222, 226, 234), (242, 244, 248)]
VOID = [(10, 8, 18), (22, 18, 36), (36, 30, 58), (58, 50, 88)]
CYAN = [(20, 90, 150), (40, 170, 230), (120, 230, 255), (230, 252, 255)]


def primitive():
    """Packed mud, framed with woven plant fibre."""
    px = tiled(base("block/packed_mud", MUD)); soften(px, (100, 74, 54), 0.35)
    border(px, FIBER, 5)
    for i in range(0, W, 4):  # weave pattern in the border
        for j in (1, 2, H - 3, H - 2):
            put(px, i, j, FIBER[1])
    divider(px, 71, (58, 40, 28), (138, 106, 78))
    return sheet(px, slot((40, 28, 20), (150, 118, 88), (74, 54, 40)), output_frame(FIBER))


def bronze_iron():
    """Terracotta bricks in a hammered bronze frame."""
    px = tiled(base("block/bricks", [(70, 36, 24), (120, 62, 40), (150, 84, 56), (176, 110, 80)])); soften(px, (130, 70, 46), 0.4)
    border(px, BRONZE, 5, rivets=BRONZE)
    divider(px, 71, (70, 34, 20), (186, 120, 86))
    return sheet(px, slot((54, 26, 16), (196, 140, 104), (96, 50, 32)), output_frame(BRONZE))


def mechanical():
    """Oak planks with an iron-strapped frame."""
    px = tiled(base("block/oak_planks", OAK)); soften(px, (150, 116, 70), 0.3)
    border(px, IRON, 5, rivets=IRON)
    divider(px, 71, (80, 58, 30), (196, 160, 106))
    return sheet(px, slot((70, 50, 26), (204, 172, 120), (110, 82, 48)), output_frame(DARK_IRON))


def industrial():
    """Dark riveted steel plate with brass trim."""
    px = tiled(base("block/iron_block", [(44, 46, 52), (70, 74, 82), (96, 100, 108), (120, 124, 134)])); soften(px, (78, 82, 90), 0.3)
    for y in range(8, H - 8, 32):  # plate seams with rivet rows
        for x in range(6, W - 6):
            put(px, x, y, (40, 42, 48))
            put(px, x, y + 1, (120, 124, 132))
        for x in range(12, W - 12, 8):
            rivet(px, x, y + 3, IRON)
    border(px, BRASS, 5, rivets=BRASS)
    divider(px, 71, BRASS[0], BRASS[3])
    return sheet(px, slot((26, 27, 31), (150, 154, 162), (52, 54, 60)), output_frame(BRASS))


def electrical():
    """Polished bakelite with copper trim and art-deco lines."""
    px = tiled(base("block/polished_blackstone", BAKELITE)); soften(px, (40, 34, 30), 0.3)
    border(px, COPPER, 5, rivets=BRASS)
    for x in range(8, W - 8):  # deco stripes under the title
        put(px, x, 17, COPPER[2]); put(px, x, 19, COPPER[1])
    for i in range(10):  # corner fans
        put(px, 8 + i, 22 + i, BRASS[1]); put(px, W - 9 - i, 22 + i, BRASS[1])
    divider(px, 71, COPPER[0], COPPER[3])
    return sheet(px, slot((6, 6, 6), (110, 84, 64), (22, 20, 18)), output_frame(COPPER))


def information():
    """A clean white composite panel with a blue status stripe."""
    px = tiled(base("block/quartz_block_side", PANEL)); soften(px, (226, 230, 236), 0.6)
    border(px, [(120, 126, 140), (170, 176, 188), (214, 218, 226), (250, 250, 252)], 4)
    for x in range(5, W - 5):
        put(px, x, H - 9, (70, 170, 255)); put(px, x, H - 8, (150, 210, 255))
    for (cx, cy) in ((8, 8), (W - 10, 8)):  # little status LEDs
        put(px, cx, cy, (90, 230, 120)); put(px, cx + 1, cy, (90, 230, 120))
    divider(px, 71, (150, 156, 168), (255, 255, 255))
    return sheet(px, slot((120, 126, 140), (255, 255, 255), (176, 182, 194)), output_frame([(70, 120, 200), (90, 160, 240), (150, 200, 255), (220, 240, 255)]))


def future():
    """Dark void plating edged with glowing cyan and violet energy lines."""
    px = tiled(base("block/polished_deepslate", VOID)); soften(px, (24, 20, 38), 0.35)
    rnd = random.Random(7)
    for _ in range(60):  # faint stars
        put(px, rnd.randrange(8, W - 8), rnd.randrange(8, H - 8), (90, 80, 140))
    border(px, [(30, 24, 52), (90, 50, 160), (170, 90, 240), (230, 180, 255)], 4)
    for x in range(4, W - 4):  # glowing edge lines
        put(px, x, 4, CYAN[2]); put(px, x, H - 5, CYAN[1])
    for y in range(4, H - 4):
        put(px, 4, y, CYAN[2]); put(px, W - 5, y, CYAN[1])
    divider(px, 71, (40, 30, 70), CYAN[1])
    return sheet(px, slot((8, 6, 16), (120, 90, 200), (26, 20, 44)), output_frame(CYAN))


if __name__ == "__main__":
    import os
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("primitive", primitive), ("bronze_iron", bronze_iron), ("mechanical", mechanical), ("industrial", industrial),
                     ("electrical", electrical), ("information", information), ("future", future)):
        write_png(f"{OUT}/{name}.png", fn())
    print("era GUI textures written")
