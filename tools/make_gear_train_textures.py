"""Draws the Gear Train Mechanism's case and its animated dial. Run from the project root:
    python3 tools/make_gear_train_textures.py"""
import math

from pixelart import write_png

CASE_DARK = (90, 52, 24, 255)
CASE = (176, 112, 52, 255)
CASE_LIGHT = (222, 168, 98, 255)
FACE = (34, 30, 28, 255)
FACE_RING = (60, 50, 44, 255)
MARK = (232, 196, 120, 255)
HAND = (246, 226, 170, 255)
HUB = (200, 140, 70, 255)
FRAMES = 8


def case():
    """Bronze plate with a rivet in each corner."""
    px = [[CASE for _ in range(16)] for _ in range(16)]
    for i in range(16):
        px[0][i] = px[i][0] = CASE_LIGHT
        px[15][i] = px[i][15] = CASE_DARK
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[y][x] = CASE_LIGHT
        px[y + 1][x + 1] = CASE_DARK
    return px


def dial(angle):
    """The dial face with eight marks and a pointer at the given angle (radians, 0 = up)."""
    px = case()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= 6.2:
                px[y][x] = FACE
            elif d <= 6.9:
                px[y][x] = FACE_RING
    for k in range(8):  # hour marks around the rim
        a = k * math.pi / 4
        x, y = round(cx + 5.3 * math.sin(a)), round(cy - 5.3 * math.cos(a))
        px[y][x] = MARK
    for step in range(1, 9):  # the pointer
        r = step * 0.55
        x, y = round(cx + r * math.sin(angle)), round(cy - r * math.cos(angle))
        px[y][x] = HAND
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[y][x] = HUB
    return px


root = "src/main/resources/assets/epochsmith/textures/block"
write_png(f"{root}/gear_train_case.png", case())
write_png(f"{root}/gear_train_dial.png", dial(0.0))
# The running dial is a tall strip of frames; Minecraft plays them as an animation
strip = []
for f in range(FRAMES):
    strip += dial(f * 2 * math.pi / FRAMES)
write_png(f"{root}/gear_train_dial_running.png", strip)
with open(f"{root}/gear_train_dial_running.png.mcmeta", "w") as f:
    f.write('{\n  "animation": {\n    "frametime": 4\n  }\n}\n')
print("gear train textures written")
