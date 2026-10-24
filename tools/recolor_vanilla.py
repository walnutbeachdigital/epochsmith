"""Makes Epochsmith's ore, raw ore, ingot and coke textures by recoloring Minecraft's own
iron and coal textures, so they match vanilla's style exactly. Run from the project root
after Gradle has set up the game files once:
    python3 tools/recolor_vanilla.py

Each recolored pixel keeps its brightness from the vanilla texture; the colors come from
each material's palette (darkest, dark, light, highlight)."""
import struct
import zipfile
import zlib

from pixelart import write_png

GAME = "build/moddev/artifacts/neoforge-21.1.264-client-extra-aka-minecraft-resources.jar"
OUT = "src/main/resources/assets/epochsmith/textures"

# Palettes, darkest to brightest (the same colors the mod's other textures use)
TIN = [(66, 76, 88), (134, 150, 164), (180, 196, 208), (224, 236, 244)]
TIN_ORE = [(70, 82, 96), (130, 148, 164), (172, 190, 206), (210, 226, 238)]
RAW_TIN = [(56, 64, 74), (112, 126, 140), (156, 170, 182), (200, 212, 222)]
BRONZE = [(90, 52, 24), (160, 98, 40), (205, 133, 63), (240, 190, 120)]
WROUGHT = [(35, 38, 44), (80, 86, 96), (110, 116, 126), (150, 156, 166)]
STEEL = [(40, 46, 58), (104, 116, 134), (150, 164, 182), (206, 218, 232)]
COKE = [(22, 22, 24), (54, 54, 60), (82, 82, 90), (124, 124, 134)]
# Armor and tool materials
HIDE = [(52, 34, 20), (98, 66, 40), (140, 98, 62), (182, 142, 98)]
FLINT = [(22, 22, 26), (52, 52, 60), (88, 88, 98), (146, 146, 156)]
STEAM = [(36, 36, 40), (88, 92, 100), (150, 118, 56), (214, 176, 92)]
COIL = [(30, 22, 18), (126, 66, 40), (176, 96, 58), (232, 172, 128)]
COMPOSITE = [(18, 20, 26), (44, 50, 60), (74, 84, 98), (132, 148, 166)]
EXOSUIT = [(40, 40, 56), (160, 162, 178), (222, 224, 234), (255, 255, 255)]

PIECES = ("helmet", "chestplate", "leggings", "boots")
TOOLS = ("sword", "pickaxe", "axe", "shovel", "hoe")


def read_png(data):
    """Decodes an 8-bit PNG (RGB, RGBA, grey, grey+alpha or palette) into rows of RGBA tuples."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, idat, palette, alpha = 8, b"", None, None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", body[:10])
        elif kind == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b"tRNS":
            alpha = list(body)
        elif kind == b"IDAT":
            idat += body
    assert depth in (1, 2, 4, 8), "16-bit PNGs aren't supported"
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    raw = zlib.decompress(idat)
    stride = (width * channels * depth + 7) // 8
    bpp = max(1, channels * depth // 8)  # bytes per pixel, for the filters
    rows, prev = [], bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        kind, line = raw[start], bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if kind == 1:
                line[i] = (line[i] + a) & 255
            elif kind == 2:
                line[i] = (line[i] + b) & 255
            elif kind == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        # Unpack samples (several per byte when depth < 8)
        if depth == 8:
            samples = list(line)
        else:
            per_byte = 8 // depth
            mask = (1 << depth) - 1
            samples = [(line[i // per_byte] >> (8 - depth * (i % per_byte + 1))) & mask
                       for i in range(width * channels)]
        row = []
        for x in range(width):
            v = samples[x * channels:(x + 1) * channels]
            if color == 6:
                row.append(tuple(v))
            elif color == 2:
                row.append((v[0], v[1], v[2], 255))
            elif color == 0:
                g = v[0] * 255 // ((1 << depth) - 1)
                row.append((g, g, g, 255))
            elif color == 4:
                row.append((v[0], v[0], v[0], v[1]))
            else:
                a_ = alpha[v[0]] if alpha and v[0] < len(alpha) else 255
                row.append(palette[v[0]] + (a_,))
        rows.append(row)
    return rows


game = zipfile.ZipFile(GAME)


def vanilla(path):
    return read_png(game.read(f"assets/minecraft/textures/{path}.png"))


def brightness(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def shade(palette, t):
    """A color part-way along the palette (t from 0 = darkest to 1 = brightest)."""
    t = min(max(t, 0.0), 1.0) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(round(palette[i][k] + (palette[i + 1][k] - palette[i][k]) * f) for k in range(3))


def recolor(px, palette, mask=None):
    """Recolors the pixels in mask (or every visible pixel), keeping their relative brightness."""
    picked = [(x, y) for y, row in enumerate(px) for x, p in enumerate(row)
              if p[3] > 0 and (mask is None or mask[y][x])]
    levels = [brightness(px[y][x]) for x, y in picked]
    low, high = min(levels), max(levels)
    out = [list(row) for row in px]
    for (x, y), level in zip(picked, levels):
        t = (level - low) / (high - low) if high > low else 0.5
        out[y][x] = shade(palette, t) + (px[y][x][3],)
    return out


def ore(ore_path, palette):
    """Keeps the grey stone pixels; recolors only the ore flecks (iron's flecks are the tan, colorful pixels)."""
    ore_px = vanilla(ore_path)
    mask = [[max(p[:3]) - min(p[:3]) > 14 for p in row] for row in ore_px]
    return recolor(ore_px, palette, mask)


def tool(path, palette):
    """Recolors a vanilla tool's head and keeps its wooden handle (the handle is the brown, colorful part)."""
    px = vanilla(path)
    mask = [[max(p[:3]) - min(p[:3]) <= 14 for p in row] for row in px]
    return recolor(px, palette, mask)


# ---- Details painted on top of the recolored armor (positions follow vanilla's iron armor layout)
GLOW = (200, 250, 255)
CYAN = (60, 210, 255)
CYAN_DIM = (34, 130, 180)
JOINT = (34, 36, 50)
SEAM = (196, 198, 212)


def paint(px, spots, color):
    """Paints the given (x, y) spots, but only where the armor is visible (never into empty space)."""
    for x, y in spots:
        if 0 <= y < len(px) and 0 <= x < len(px[0]) and px[y][x][3] > 0:
            px[y][x] = color + (px[y][x][3],)


def line(x0, x1, y):
    return [(x, y) for x in range(x0, x1 + 1)]


def column(x, y0, y1):
    return [(x, y) for y in range(y0, y1 + 1)]


def composite_icon(piece, px):
    if piece == "helmet":
        paint(px, line(5, 10, 7), CYAN)            # visor
        paint(px, line(6, 9, 8), CYAN_DIM)
        paint(px, [(7, 7), (8, 7)], GLOW)
    elif piece == "chestplate":
        paint(px, column(7, 9, 12) + column(8, 9, 12), CYAN_DIM)  # circuit trace
        paint(px, [(7, 7), (8, 7), (7, 8), (8, 8)], CYAN)       # chest light
        paint(px, [(7, 7)], GLOW)
        paint(px, [(3, 4), (12, 4)], CYAN)                        # shoulder lights
    elif piece == "leggings":
        paint(px, column(4, 5, 10) + column(11, 5, 10), CYAN_DIM)  # side stripes
        paint(px, [(5, 8), (10, 8)], CYAN)                          # knee lights
    elif piece == "boots":
        paint(px, line(2, 5, 11) + line(10, 13, 11), CYAN)          # toe lights
        paint(px, [(4, 5), (11, 5)], CYAN_DIM)


def composite_layer(layer, px):
    if layer == 1:
        paint(px, line(8, 15, 9), CYAN)                     # visor, front
        paint(px, line(8, 15, 10), CYAN_DIM)
        paint(px, [(11, 9), (12, 9)], GLOW)
        paint(px, line(0, 7, 9) + line(16, 23, 9), CYAN_DIM)  # visor wraps round the sides
        paint(px, column(21, 21, 30) + column(26, 21, 30) + line(21, 26, 26), CYAN_DIM)  # chest traces
        paint(px, [(23, 22), (24, 22), (23, 23), (24, 23)], CYAN)                      # chest light
        paint(px, [(23, 22)], GLOW)
        paint(px, column(35, 21, 30) + column(36, 21, 30), CYAN_DIM)                   # power spine
        paint(px, [(35, 23), (36, 26), (35, 29)], CYAN)
        paint(px, line(44, 47, 22) + line(40, 43, 22) + line(48, 51, 22), CYAN_DIM)    # arm stripes
        paint(px, line(4, 7, 30) + line(0, 3, 30) + line(8, 11, 30), CYAN)             # boot toe lights
    else:
        paint(px, column(7, 21, 27) + column(4, 21, 27), CYAN_DIM)  # leg stripes
        paint(px, [(5, 24), (6, 24)], CYAN)                          # knee lights
        paint(px, line(16, 39, 27), CYAN_DIM)                        # belt trace


def exosuit_icon(piece, px):
    if piece == "helmet":
        paint(px, line(4, 11, 7) + line(5, 10, 8) + line(5, 10, 9), JOINT)  # dark visor
        paint(px, line(6, 9, 8), CYAN)                                       # scan line
        paint(px, [(7, 8)], GLOW)
    elif piece == "chestplate":
        ring = [(7, 5), (8, 5), (6, 6), (9, 6), (6, 7), (9, 7), (7, 8), (8, 8)]
        paint(px, ring, JOINT)                                   # core housing
        paint(px, [(7, 6), (8, 6), (7, 7), (8, 7)], CYAN)        # power core
        paint(px, [(7, 6)], GLOW)
        paint(px, line(5, 10, 10) + line(5, 10, 12), SEAM)       # ab plates
        paint(px, [(2, 4), (3, 4), (12, 4), (13, 4)], JOINT)     # shoulder joints
    elif piece == "leggings":
        paint(px, line(4, 11, 3), JOINT)                         # belt
        paint(px, [(7, 3), (8, 3)], CYAN)
        paint(px, [(4, 8), (5, 8), (10, 8), (11, 8)], JOINT)     # knee joints
        paint(px, [(5, 8), (10, 8)], CYAN)
    elif piece == "boots":
        paint(px, [(4, 7), (5, 7), (10, 7), (11, 7)], JOINT)     # ankle joints
        paint(px, line(2, 5, 11) + line(10, 13, 11), CYAN)       # thrusters
        paint(px, [(3, 11), (12, 11)], GLOW)


def exosuit_layer(layer, px):
    if layer == 1:
        paint(px, line(8, 15, 9) + line(8, 15, 10) + line(0, 7, 9) + line(16, 23, 9), JOINT)  # visor
        paint(px, line(9, 14, 10), CYAN)
        paint(px, [(11, 10), (12, 10)], GLOW)
        paint(px, column(11, 1, 6) + column(12, 1, 6), SEAM)                                # crest
        ring = [(23, 21), (24, 21), (22, 22), (25, 22), (22, 23), (25, 23), (23, 24), (24, 24)]
        paint(px, ring, JOINT)                                                               # core housing
        paint(px, [(23, 22), (24, 22), (23, 23), (24, 23)], CYAN)                            # power core
        paint(px, [(23, 22)], GLOW)
        paint(px, line(20, 27, 26) + line(21, 26, 28), SEAM)                                 # ab plates
        paint(px, column(35, 21, 30) + column(36, 21, 30), JOINT)                            # powered spine
        paint(px, [(35, 22), (36, 25), (35, 28)], CYAN)
        paint(px, line(40, 55, 25), JOINT)                                                   # elbow joints
        paint(px, [(45, 25), (41, 25), (49, 25)], CYAN)
        paint(px, line(0, 15, 28), JOINT)                                                    # ankle joints
        paint(px, line(0, 15, 31), CYAN)                                                     # thrusters
        paint(px, [(5, 31), (6, 31)], GLOW)
    else:
        paint(px, line(16, 39, 27), JOINT)                                                   # belt
        paint(px, [(23, 27), (24, 27)], CYAN)
        paint(px, line(0, 15, 24), JOINT)                                                    # knee joints
        paint(px, [(5, 24), (6, 24), (1, 24), (9, 24), (13, 24)], CYAN)


DETAILS = {"composite": (composite_icon, composite_layer), "exosuit": (exosuit_icon, exosuit_layer)}


def armor_set(name, base, palette, material=None):
    """Item icons and the worn-armor textures for one armor set, recolored from a vanilla set.
    The worn textures are named after the armor material, which is usually the same as the set's name."""
    icon_details, layer_details = DETAILS.get(name, (None, None))
    for piece in PIECES:
        px = recolor(vanilla(f"item/{base}_{piece}"), palette)
        if icon_details:
            icon_details(piece, px)
        write_png(f"{OUT}/item/{name}_{piece}.png", px)
    for layer in (1, 2):
        px = recolor(vanilla(f"models/armor/{base}_layer_{layer}"), palette)
        if layer_details:
            layer_details(layer, px)
        write_png(f"{OUT}/models/armor/{material or name}_layer_{layer}.png", px)


if __name__ == "__main__":
    write_png(f"{OUT}/block/tin_ore.png", ore("block/iron_ore", TIN_ORE))
    write_png(f"{OUT}/block/deepslate_tin_ore.png", ore("block/deepslate_iron_ore", TIN_ORE))
    write_png(f"{OUT}/item/raw_tin.png", recolor(vanilla("item/raw_iron"), RAW_TIN))
    write_png(f"{OUT}/item/tin_ingot.png", recolor(vanilla("item/iron_ingot"), TIN))
    write_png(f"{OUT}/item/bronze_ingot.png", recolor(vanilla("item/iron_ingot"), BRONZE))
    write_png(f"{OUT}/item/wrought_iron_ingot.png", recolor(vanilla("item/iron_ingot"), WROUGHT))
    write_png(f"{OUT}/item/steel_ingot.png", recolor(vanilla("item/iron_ingot"), STEEL))
    write_png(f"{OUT}/item/coke.png", recolor(vanilla("item/coal"), COKE))

    # Armor: shaped like vanilla iron armor (hide armor like leather)
    armor_set("hide", "leather", HIDE)
    armor_set("bronze", "iron", BRONZE)
    armor_set("steel", "iron", STEEL, material="knight")
    armor_set("steam", "iron", STEAM)
    armor_set("coil", "iron", COIL)
    armor_set("composite", "iron", COMPOSITE)
    armor_set("exosuit", "iron", EXOSUIT)

    # Tools: shaped like vanilla iron tools (flint like stone tools; the flint knife like a stone sword)
    for kind in TOOLS:
        write_png(f"{OUT}/item/bronze_{kind}.png", tool(f"item/iron_{kind}", BRONZE))
        write_png(f"{OUT}/item/steel_{kind}.png", tool(f"item/iron_{kind}", STEEL))
    for kind in ("pickaxe", "axe", "shovel"):
        write_png(f"{OUT}/item/flint_{kind}.png", tool(f"item/stone_{kind}", FLINT))
    write_png(f"{OUT}/item/flint_knife.png", tool("item/stone_sword", FLINT))
    print("recolored textures written")
