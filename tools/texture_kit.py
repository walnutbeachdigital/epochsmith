"""Shared drawing helpers for Epochsmith's textures. Textures are lists of rows of (r, g, b, a).
Most textures start from a real Minecraft texture (so they match vanilla's style) and add details."""
import math

from recolor_vanilla import vanilla, recolor, brightness, shade

CLEAR = (0, 0, 0, 0)


def blank(width=16, height=16):
    return [[CLEAR] * width for _ in range(height)]


def copy(px):
    return [list(row) for row in px]


def base(path, palette=None):
    """A vanilla texture, optionally recolored to a palette (darkest..brightest)."""
    px = copy(vanilla(path))
    return recolor(px, palette) if palette else px


def put(px, x, y, color):
    if 0 <= y < len(px) and 0 <= x < len(px[0]):
        px[y][x] = tuple(color[:3]) + ((color[3] if len(color) > 3 else 255),)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def blend(px, x, y, color, amount):
    """Mixes a color over a pixel (amount 0..1), keeping the pixel's transparency."""
    if 0 <= y < len(px) and 0 <= x < len(px[0]) and px[y][x][3] > 0:
        px[y][x] = mix(px[y][x], color, amount) + (px[y][x][3],)


def darken(px, x, y, amount):
    blend(px, x, y, (0, 0, 0), amount)


def lighten(px, x, y, amount):
    blend(px, x, y, (255, 255, 255), amount)


def fill(px, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, color)


def tint(px, x0, y0, x1, y1, color, amount):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            blend(px, x, y, color, amount)


def bevel(px, x0, y0, x1, y1, light=0.35, dark=0.45):
    """A raised edge: lighter top and left, darker bottom and right (light comes from the top-left)."""
    for x in range(x0, x1 + 1):
        lighten(px, x, y0, light)
        darken(px, x, y1, dark)
    for y in range(y0 + 1, y1):
        lighten(px, x0, y, light * 0.8)
        darken(px, x1, y, dark * 0.8)


def inset(px, x0, y0, x1, y1, light=0.3, dark=0.45):
    """A sunken edge: darker top and left, lighter bottom and right."""
    for x in range(x0, x1 + 1):
        darken(px, x, y0, dark)
        lighten(px, x, y1, light)
    for y in range(y0 + 1, y1):
        darken(px, x0, y, dark * 0.8)
        lighten(px, x1, y, light * 0.8)


def frame(px, color, width=1):
    """A solid border round the whole texture."""
    h, w = len(px), len(px[0])
    for i in range(width):
        for x in range(w):
            put(px, x, i, color)
            put(px, x, h - 1 - i, color)
        for y in range(h):
            put(px, i, y, color)
            put(px, w - 1 - i, y, color)


def rivet(px, x, y, metal):
    """A 2x2 rivet head: highlight top-left, shadow bottom-right. metal is a palette (dark..bright)."""
    put(px, x, y, metal[-1])
    put(px, x + 1, y, metal[-2])
    put(px, x, y + 1, metal[-2])
    put(px, x + 1, y + 1, metal[0])


def stud(px, x, y, metal):
    """A 1-pixel rivet with a shadow under it."""
    put(px, x, y, metal[-1])
    darken(px, x + 1, y + 1, 0.4)


def band(px, y0, y1, metal, rivets=(), rivet_metal=None):
    """A horizontal metal band (rows y0..y1), shaded like a curved strap, with optional rivet columns."""
    rows = y1 - y0 + 1
    for i, y in enumerate(range(y0, y1 + 1)):
        t = 1.0 - i / max(1, rows - 1) * 0.85 if rows > 1 else 0.6
        for x in range(len(px[0])):
            c = shade(metal, t * 0.9 + 0.05 * ((x * 7 + y * 3) % 3) / 2)
            put(px, x, y, c)
    darken_row(px, y1 + 1, 0.35)
    for x in rivets:
        stud(px, x, y0 + (rows - 1) // 2, rivet_metal or metal)


def darken_row(px, y, amount):
    for x in range(len(px[0])):
        darken(px, x, y, amount)


def glow(px, cx, cy, radius, color, strength=1.0):
    """A soft round glow, strongest in the middle (for lit windows, lights and screens)."""
    for y in range(len(px)):
        for x in range(len(px[0])):
            d = math.hypot(x - cx, y - cy)
            if d < radius:
                blend(px, x, y, color, strength * (1 - d / radius) ** 1.5)


def ring(px, cx, cy, r_out, r_in, palette, light_angle=-2.36):
    """A shaded metal ring (gear rims, bezels). Brighter on the side facing the light."""
    for y in range(len(px)):
        for x in range(len(px[0])):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if r_in <= d < r_out:
                a = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
                t = 0.5 + 0.45 * math.cos(a - light_angle)
                put(px, x, y, shade(palette, t))


def disc(px, cx, cy, r, palette, light_angle=-2.36, dome=True):
    """A shaded filled circle (domes, buttons, lenses)."""
    for y in range(len(px)):
        for x in range(len(px[0])):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d < r:
                t = 0.55 + 0.45 * (-(dx + dy) / (r * 1.6)) if dome else 0.5
                put(px, x, y, shade(palette, min(1, max(0, t))))


def gear(px, cx, cy, r_out, r_root, r_hole, teeth, palette, phase=0.0):
    """A shaded gear: teeth round the outside, a hub hole in the middle."""
    for y in range(len(px)):
        for x in range(len(px[0])):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            tooth = math.cos((a + phase) * teeth) > 0.1
            edge = r_out if tooth else r_root
            if r_hole <= d < edge:
                t = 0.5 + 0.4 * (-(dx + dy) / (r_out * 1.5))
                if d > edge - 1.0:
                    t -= 0.25  # dark outline on the rim
                if d < r_hole + 1.0:
                    t -= 0.2   # shadow inside the hub
                put(px, x, y, shade(palette, min(1, max(0, t))))


def outline(px, color=None, amount=0.55):
    """Darkens the outermost pixels of an item shape, like vanilla's item outlines."""
    h, w = len(px), len(px[0])
    edge = []
    for y in range(h):
        for x in range(w):
            if px[y][x][3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < w and 0 <= ny < h) or px[ny][nx][3] == 0:
                    edge.append((x, y))
                    break
    for x, y in edge:
        if color:
            put(px, x, y, color)
        else:
            darken(px, x, y, amount)


def from_rows(rows, palette):
    """Builds a texture from text rows: each character is a key in palette, '.' is empty."""
    rows = list(rows) + ["." * len(rows[0])] * max(0, 16 - len(rows))  # pad short drawings to 16 rows
    px = blank(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                put(px, x, y, palette[ch])
    return px


# Shared material palettes, darkest to brightest
IRON = [(44, 46, 52), (92, 96, 104), (150, 154, 162), (214, 218, 224)]
DARK_IRON = [(26, 27, 31), (54, 56, 62), (88, 92, 100), (130, 134, 142)]
STEEL = [(40, 46, 58), (104, 116, 134), (150, 164, 182), (206, 218, 232)]
BRASS = [(92, 62, 20), (156, 112, 40), (206, 162, 70), (248, 216, 128)]
BRONZE = [(90, 52, 24), (160, 98, 40), (205, 133, 63), (240, 190, 120)]
COPPER = [(84, 40, 26), (150, 74, 46), (208, 118, 78), (240, 170, 130)]
OAK = [(70, 50, 26), (112, 84, 48), (156, 122, 72), (188, 152, 98)]
DARK_WOOD = [(40, 26, 14), (66, 44, 24), (96, 66, 38), (128, 92, 56)]
FIREBRICK = [(48, 22, 16), (96, 42, 30), (138, 64, 44), (172, 92, 66)]
CLAY = [(90, 92, 104), (120, 126, 140), (158, 164, 178), (190, 196, 208)]
FIRE = [(120, 20, 4), (220, 80, 10), (255, 160, 40), (255, 236, 150)]
