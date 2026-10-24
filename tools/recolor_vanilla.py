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


def armor_set(name, base, palette, material=None):
    """Item icons and the worn-armor textures for one armor set, recolored from a vanilla set.
    The worn textures are named after the armor material, which is usually the same as the set's name."""
    for piece in PIECES:
        write_png(f"{OUT}/item/{name}_{piece}.png", recolor(vanilla(f"item/{base}_{piece}"), palette))
    for layer in (1, 2):
        write_png(f"{OUT}/models/armor/{material or name}_layer_{layer}.png",
                  recolor(vanilla(f"models/armor/{base}_layer_{layer}"), palette))


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
