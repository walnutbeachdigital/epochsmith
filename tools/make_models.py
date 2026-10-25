"""Builds Epochsmith's 3D block models out of boxes, like vanilla's own models.
Run from the project root:  python3 tools/make_models.py

Each model is a list of boxes in block pixels (0-16). The front of a machine faces north (z = 0);
blockstates turn it to face the player. Faces pick their textures from a small dictionary per box:
"all", "side", "top", "bottom", or a face name ("north", "south", "east", "west", "up", "down").
"""
import json
import math
import os

OUT = "src/main/resources/assets/epochsmith/models/block"
FACES = ("north", "south", "east", "west", "up", "down")
SIDES = ("north", "south", "east", "west")


def box(frm, to, tex, glow=False, uv=None, rotation=None, skip=()):
    """A box. tex: a texture key ("#body") or a dict of face/group -> key. uv: optional {face: [u0,v0,u1,v1]}.
    glow: make it light itself (for fire, plasma, screens). skip: faces not to draw."""
    if isinstance(tex, str):
        tex = {"all": tex}
    faces = {}
    for face in FACES:
        if face in skip:
            continue
        key = tex.get(face) or (tex.get("top") if face == "up" else None) or (tex.get("bottom") if face == "down" else None) \
            or (tex.get("side") if face in SIDES else None) or tex.get("all")
        if not key:
            continue
        f = {"texture": key}
        if uv and face in uv:
            f["uv"] = uv[face]
        if cull(frm, to, face):
            f["cullface"] = face
        faces[face] = f
    element = {"from": list(frm), "to": list(to), "faces": faces}
    if rotation:
        element["rotation"] = rotation
    if glow:
        element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        element["shade"] = False
    return element


def cull(frm, to, face):
    """Only faces that fill a whole block side are hidden against neighbours."""
    full_xy = frm[0] == 0 and to[0] == 16 and frm[1] == 0 and to[1] == 16
    full_xz = frm[0] == 0 and to[0] == 16 and frm[2] == 0 and to[2] == 16
    full_yz = frm[1] == 0 and to[1] == 16 and frm[2] == 0 and to[2] == 16
    return {"north": frm[2] == 0 and full_xy, "south": to[2] == 16 and full_xy,
            "west": frm[0] == 0 and full_yz, "east": to[0] == 16 and full_yz,
            "down": frm[1] == 0 and full_xz, "up": to[1] == 16 and full_xz}[face]


def frame(lo, hi, y0, y1, t, key):
    """Four corner posts from y0 to y1, t pixels thick, between lo and hi."""
    return [box((x, y0, z), (x + t, y1, z + t), key) for x in (lo, hi - t) for z in (lo, hi - t)]


def edges(lo, hi, t, key):
    """The twelve edges of a cube frame (corner posts plus top and bottom rims)."""
    parts = frame(lo, hi, lo, hi, t, key)
    for y in (lo, hi - t):
        parts += [box((lo + t, y, lo), (hi - t, y + t, lo + t), key), box((lo + t, y, hi - t), (hi - t, y + t, hi), key),
                  box((lo, y, lo + t), (lo + t, y + t, hi - t), key), box((hi - t, y, lo + t), (hi, y + t, hi - t), key)]
    return parts


def ring(y0, y1, lo, hi, t, key, glow=False):
    """A square ring (four walls) between lo and hi, t thick."""
    return [box((lo, y0, lo), (hi, y1, lo + t), key, glow), box((lo, y0, hi - t), (hi, y1, hi), key, glow),
            box((lo, y0, lo + t), (lo + t, y1, hi - t), key, glow), box((hi - t, y0, lo + t), (hi, y1, hi - t), key, glow)]


def model(name, textures, elements, cutout=False, translucent=False, ao=True, display=None):
    data = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if not ao:
        data["ambientocclusion"] = False
    if display:
        data["display"] = display
    if cutout:
        data["render_type"] = "minecraft:cutout"
    if translucent:
        data["render_type"] = "minecraft:translucent"
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/{name}.json", "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


FULL = [0, 0, 16, 16]
E = "epochsmith:block/"
M = "minecraft:block/"


# ---------------- Primitive and Bronze & Iron ----------------

def knapping_stone():
    t = {"particle": M + "cobblestone", "rock": M + "cobblestone", "face": M + "smooth_stone", "flint": "minecraft:item/flint",
         "moss": M + "mossy_cobblestone"}
    els = [box((1, 0, 1), (15, 4, 15), {"side": "#rock", "top": "#moss", "bottom": "#rock"}),
           box((2, 4, 3), (13, 6, 13), {"side": "#rock", "top": "#face"}),
           box((3, 6, 4), (12, 7, 12), {"side": "#face", "top": "#face"}),
           # flint flakes lying on the stone
           box((10, 7, 5), (12, 7.5, 7), "#flint", rotation={"origin": [11, 7, 6], "axis": "y", "angle": 22.5}),
           box((4, 7, 9), (6, 7.5, 10.5), "#flint", rotation={"origin": [5, 7, 10], "axis": "y", "angle": -22.5}),
           box((7, 7, 7), (8.5, 7.5, 8.5), "#flint"),
           box((13, 0, 12), (15.5, 2, 14.5), {"side": "#rock", "top": "#face"})]
    model("knapping_stone", t, els)


def pit_kiln(lit):
    t = {"particle": M + "packed_mud", "mud": M + "packed_mud", "brick": M + "mud_bricks", "bed": M + ("magma" if lit else "coarse_dirt"),
         "log": M + ("campfire_log_lit" if lit else "oak_log"), "log_end": M + "oak_log_top", "clay": M + "terracotta"}
    els = [box((0, 0, 0), (16, 2, 16), "#mud")]
    els += ring(2, 7, 0, 16, 3, {"side": "#brick", "top": "#brick"})
    els += [box((3, 2, 3), (13, 4, 13), {"top": "#bed", "side": "#mud"}, glow=lit),
            box((4, 4, 5), (12, 6, 7), {"side": "#log", "north": "#log_end", "south": "#log_end"},
                rotation={"origin": [8, 5, 6], "axis": "y", "angle": 22.5}),
            box((5, 4, 9), (13, 6, 11), {"side": "#log", "east": "#log_end", "west": "#log_end"},
                rotation={"origin": [9, 5, 10], "axis": "y", "angle": -22.5}),
            box((6, 6, 6), (9, 8, 9), {"all": "#clay"})]          # a clay pot being fired
    model("pit_kiln_lit" if lit else "pit_kiln", t, els)


def woven_basket():
    t = {"particle": M + "hay_block_side", "weave": M + "hay_block_side", "top": M + "hay_block_top",
         "rim": M + "stripped_bamboo_block", "fiber": M + "bamboo_block"}
    els = [box((2, 0, 2), (14, 9, 14), {"side": "#weave", "top": "#top", "bottom": "#top"}),
           box((3, 1, 3), (13, 8.9, 13), "#weave", skip=("down",)),
           box((1.5, 8, 1.5), (14.5, 10, 14.5), {"side": "#rim", "top": "#rim"}, skip=("down",)),
           box((3, 10, 7.5), (4, 14, 8.5), "#fiber"), box((12, 10, 7.5), (13, 14, 8.5), "#fiber"),
           box((4, 13, 7.5), (12, 14, 8.5), "#fiber")]
    # the inside: hollow, so model the floor separately
    els[0] = box((2, 0, 2), (14, 9, 14), {"side": "#weave", "bottom": "#top"}, skip=("up",))
    els.insert(1, box((3, 1, 3), (13, 1.1, 13), {"up": "#top"}))
    els.pop(2)
    model("woven_basket", t, els)


def stone_anvil():
    t = {"particle": M + "stone", "stone": M + "stone", "rough": M + "cobblestone", "face": M + "smooth_stone",
         "wood": M + "stripped_oak_log", "wood_top": M + "stripped_oak_log_top"}
    els = [box((1, 0, 1), (15, 5, 15), {"side": "#wood", "top": "#wood_top", "bottom": "#wood_top"}),   # stump
           box((3, 5, 4), (13, 10, 12), {"all": "#rough"}),
           box((1, 10, 2), (15, 16, 14), {"side": "#stone", "top": "#face"}),
           box((0, 12, 6), (1, 15, 10), {"all": "#stone"})]                                       # a worked edge
    model("stone_anvil", t, els)


def bloomery(lit):
    t = {"particle": M + "mud_bricks", "brick": M + "mud_bricks", "base": M + "cobblestone", "clay": M + "terracotta",
         "mouth": M + ("magma" if lit else "coal_block"), "top_hole": M + ("magma" if lit else "coal_block")}
    els = [box((0, 0, 0), (16, 3, 16), "#base"),
           box((1, 3, 1), (15, 11, 15), "#brick"),
           box((2, 11, 2), (14, 14, 14), "#brick"),
           box((4, 14, 4), (12, 16, 12), {"side": "#brick", "top": "#top_hole"}, glow=False),
           box((5.5, 15.9, 5.5), (10.5, 16, 10.5), {"up": "#top_hole"}, glow=lit),
           box((5, 3, 0.5), (11, 8, 1), {"north": "#mouth"}, glow=lit),                       # the door, glowing when lit
           box((4, 3, 0), (5, 9, 1), "#clay"), box((11, 3, 0), (12, 9, 1), "#clay"), box((4, 8, 0), (12, 9, 1), "#clay"),
           box((15, 5, 7), (16, 7, 9), "#clay")]                                               # the bellows pipe
    model("bloomery_lit" if lit else "bloomery", t, els)


# ---------------- Mechanical ----------------

def millstone(active):
    t = {"particle": M + "stone", "base": M + "stone_bricks", "lower": M + "smooth_stone", "runner": M + "stone",
         "axle": M + "stripped_oak_log", "axle_top": M + "stripped_oak_log_top", "wood": M + "spruce_planks"}
    els = [box((0, 0, 0), (16, 6, 16), "#base"),
           box((1, 6, 1), (15, 9, 15), {"side": "#lower", "top": "#lower"}),
           box((2, 9, 2), (14, 13, 14), {"side": "#runner", "top": "#lower"}),
           box((7, 9, 7), (9, 16, 9), {"side": "#axle", "top": "#axle_top"}),
           box((3, 13, 7.5), (13, 14, 8.5), "#wood"),                                              # turning bar
           box((14, 3, 6), (16, 6, 10), "#wood")]                                                  # flour chute
    model("millstone_active" if active else "millstone", t, els)


def joiners_bench():
    t = {"particle": M + "oak_planks", "top": M + "stripped_oak_log", "wood": M + "oak_planks", "leg": M + "stripped_spruce_log",
         "iron": M + "iron_block", "dark": M + "dark_oak_planks"}
    els = [box((0, 13, 0), (16, 16, 16), {"side": "#wood", "top": "#top"})]
    els += frame(1, 15, 0, 13, 3, "#leg")
    els += [box((2, 4, 2), (14, 5, 14), "#dark"),                                                  # lower shelf
            box((11, 11, -0.5), (15, 14, 1.5), "#iron"),                                           # vise jaw
            box((12.5, 11.5, -1.5), (13.5, 12.5, -0.5), "#iron"),                                  # vise screw
            box((2, 5, 3), (6, 6, 9), "#wood")]                                                     # offcuts on the shelf
    model("joiners_bench", t, els)


def forge(lit):
    t = {"particle": M + "bricks", "brick": M + "bricks", "hearth": M + ("magma" if lit else "coal_block"),
         "bellows": M + "brown_wool", "wood": M + "oak_planks", "iron": M + "iron_block", "dark": M + "smooth_basalt"}
    els = [box((0, 0, 0), (16, 9, 16), "#brick"),
           box((2, 9, 1), (14, 10, 9), {"top": "#hearth", "side": "#dark"}, glow=lit),
           box((1, 9, 0), (15, 11, 1), "#brick"), box((1, 9, 1), (2, 11, 9), "#brick"), box((14, 9, 1), (15, 11, 9), "#brick"),
           box((4, 9, 10), (11, 16, 15), {"side": "#brick", "top": "#dark"}),                    # chimney
           box((11, 9, 10), (15, 12, 15), {"side": "#bellows", "top": "#wood"}),                  # bellows
           box((11, 12, 10), (15, 13, 15), "#wood"),
           box((5, 4, -0.5), (11, 8, 0), {"north": "#hearth"}, glow=lit),                         # firebox opening
           box((0, 2, 0), (16, 3, 16), "#iron", skip=("up", "down"))]                             # iron band
    model("forge_lit" if lit else "forge", t, els)


def gearbox():
    t = {"particle": E + "gearbox", "panel": E + "gearbox", "frame": M + "stripped_dark_oak_log", "iron": M + "iron_block"}
    els = [box((1, 1, 1), (15, 15, 15), "#panel")] + edges(0, 16, 2, "#frame")
    for frm, to in (((6, 6, 0), (10, 10, 1)), ((6, 6, 15), (10, 10, 16)), ((0, 6, 6), (1, 10, 10)),
                    ((15, 6, 6), (16, 10, 10)), ((6, 0, 6), (10, 1, 10)), ((6, 15, 6), (10, 16, 10))):
        els.append(box(frm, to, "#iron"))                                                         # shaft collars
    model("gearbox", t, els)


def gear_train(running):
    t = {"particle": E + "gear_train_case", "case": E + "gear_train_case", "dial": E + ("gear_train_dial_running" if running else "gear_train_dial"),
         "wood": M + "dark_oak_planks", "brass": M + "gold_block"}
    els = [box((0, 0, 0), (16, 2, 16), "#wood"),
           box((1, 2, 1), (15, 14, 15), "#case"),
           box((0, 14, 0), (16, 16, 16), {"side": "#wood", "top": "#wood"}),
           box((2, 3, 0.5), (14, 13, 1), {"north": "#dial"}, uv={"north": FULL}),
           box((7, 16, 7), (9, 16.5, 9), "#brass")]
    model("gear_train_running" if running else "gear_train", t, els)


# ---------------- Industrial ----------------

def banded_crate():
    t = {"particle": E + "banded_crate", "crate": E + "banded_crate", "frame": M + "stripped_dark_oak_log"}
    model("banded_crate", t, [box((1, 1, 1), (15, 15, 15), "#crate")] + edges(0, 16, 2, "#frame"))


def coke_oven(lit):
    t = {"particle": E + "coke_oven_side", "brick": E + "coke_oven_side", "front": E + ("coke_oven_front_lit" if lit else "coke_oven_front"),
         "vent": M + ("magma" if lit else "coal_block"), "iron": M + "iron_block"}
    els = [box((0, 0, 0), (16, 11, 16), {"side": "#brick", "top": "#brick", "north": "#front"}, uv={"north": FULL}, glow=False),
           box((1, 11, 1), (15, 13, 15), "#brick"),
           box((3, 13, 3), (13, 15, 13), "#brick"),
           box((5, 15, 5), (11, 16, 11), {"side": "#brick", "top": "#vent"}),
           box((6, 15.9, 6), (10, 16, 10), {"up": "#vent"}, glow=lit),
           box((0, 0, 0), (2, 12, 2), "#iron"), box((14, 0, 0), (16, 12, 2), "#iron")]            # iron corner braces
    if lit:
        els.append(box((4, 2, -0.01), (12, 9, 0), {"north": "#front"}, uv={"north": [4, 6, 12, 13]}, glow=True))
    model("coke_oven_lit" if lit else "coke_oven", t, els)


def boiler(lit):
    t = {"particle": E + "boiler_side", "drum": E + "boiler_side", "cap": E + "boiler_top", "brick": E + "coke_oven_side",
         "fire": M + ("furnace_front_on" if lit else "furnace_front"), "brass": M + "gold_block", "iron": M + "iron_block"}
    els = [box((0, 0, 0), (16, 6, 16), {"side": "#brick", "top": "#brick", "north": "#fire"}, uv={"north": [0, 6, 16, 16]}),
           box((1, 6, 1), (15, 14, 15), {"side": "#drum", "top": "#cap"}),
           box((2, 14, 2), (14, 15, 14), {"side": "#drum", "top": "#cap"}),
           box((6, 15, 6), (10, 16, 10), "#brass"),                                                 # steam dome
           box((12, 14, 3), (14, 16, 5), "#iron")]                                                  # safety valve
    if lit:
        els.append(box((3, 1, -0.01), (13, 5, 0), {"north": "#fire"}, uv={"north": [3, 9, 13, 13]}, glow=True))
    model("boiler_lit" if lit else "boiler", t, els)


def steam_engine():
    t = {"particle": E + "boiler_side", "bed": M + "polished_blackstone", "cylinder": E + "boiler_side", "wheel": E + "flywheel",
         "axle": M + "stripped_oak_log", "pipe": E + "steam_pipe", "iron": M + "iron_block", "brass": M + "gold_block"}
    els = [box((0, 0, 0), (16, 2, 16), "#bed"),
           box((1, 2, 4), (9, 9, 12), "#cylinder"),
           box((2, 9, 5), (8, 11, 11), "#iron"),                                                   # valve chest
           box((0.5, 3, 3.5), (1, 8, 12.5), "#brass"),                                             # cylinder cover
           box((9, 5, 7.5), (12, 6, 8.5), "#iron"),                                                # piston rod
           box((12, 1, 1), (14, 15, 15), {"east": "#wheel", "west": "#wheel", "side": "#iron"}),
           box((9, 7, 7), (16, 9, 9), "#axle"),
           box((4, 11, 7), (6, 16, 9), "#pipe"),
           box((14, 2, 6), (16, 5, 10), "#bed")]                                                   # bearing block
    model("steam_engine", t, els)


def lathe():
    t = {"particle": E + "boiler_side", "bed": M + "polished_blackstone", "body": E + "boiler_side", "gauge": E + "steam_pipe",
         "iron": M + "iron_block", "brass": M + "gold_block"}
    els = [box((1, 0, 2), (3, 9, 14), "#bed"), box((13, 0, 2), (15, 9, 14), "#bed"),
           box((0, 9, 3), (16, 11, 13), {"side": "#iron", "top": "#bed"}),                        # bed rails
           box((0, 11, 4), (5, 16, 12), "#body"),                                                  # headstock
           box((5, 12.5, 7), (6.5, 13.5, 9), "#brass"),                                            # chuck
           box((12, 11, 5), (16, 15, 11), "#body"),                                                # tailstock
           box((10.5, 12.5, 7.5), (12, 13.5, 8.5), "#iron"),
           box((6, 11, 10), (10, 12.5, 13), "#iron"),                                              # tool rest
           box((1, 13, 3), (4, 16, 4), "#gauge")]
    model("lathe", t, els)


# ---------------- Electrical ----------------

def battery(level):
    t = {"particle": E + f"battery_side_{level}", "side": E + f"battery_side_{level}", "top": E + "battery_top",
         "base": M + "polished_blackstone", "pos": M + "red_concrete", "neg": M + "black_concrete", "copper": M + "copper_block"}
    els = [box((1, 0, 1), (15, 2, 15), "#base"),
           box((2, 2, 2), (14, 14, 14), {"side": "#side", "top": "#top"}, uv={s: [2, 2, 14, 14] for s in SIDES}),
           box((4, 14, 6), (6, 16, 10), {"side": "#copper", "top": "#pos"}),
           box((10, 14, 6), (12, 16, 10), {"side": "#copper", "top": "#neg"})]
    model(f"battery_{level}", t, els)


def sorting_cabinet():
    t = {"particle": E + "sorting_cabinet_side", "front": E + "sorting_cabinet_front", "side": E + "sorting_cabinet_side",
         "brass": M + "gold_block", "top": M + "polished_deepslate"}
    els = [box((0, 0, 0), (16, 16, 16), {"north": "#front", "side": "#side", "top": "#side", "bottom": "#side"})]
    for cx in (1, 6, 11):
        for cy in (1, 6, 11):
            y = 16 - (cy + 4)
            els.append(box((cx + 1, y, -0.5), (cx + 3, y + 1, 0), "#brass"))                      # drawer pulls
    els.append(box((0, 15, 0), (16, 16, 16), {"top": "#top", "side": "#top"}, skip=("down",)))   # dark top
    els[0] = box((0, 0, 0), (16, 15, 16), {"north": "#front", "side": "#side", "bottom": "#side"},
                 uv={"north": [0, 1, 16, 16], "east": [0, 1, 16, 16], "west": [0, 1, 16, 16], "south": [0, 1, 16, 16]}, skip=("up",))
    model("sorting_cabinet", t, els)


# ---------------- Information ----------------

def circuit_fabricator(on):
    t = {"particle": E + "fabricator_side", "panel": E + "fabricator_side", "glass": M + "glass", "stage": M + "polished_deepslate",
         "wafer": M + "light_blue_stained_glass", "beam": M + "purple_stained_glass",
         "head": M + "iron_block", "front": E + ("fabricator_front_on" if on else "fabricator_front")}
    els = [box((0, 0, 0), (16, 9, 16), {"side": "#panel", "north": "#front", "top": "#panel"}, uv={"north": [0, 1, 16, 10]}),
           box((1, 9, 1), (15, 15, 15), "#glass", skip=("down",)),
           box((0, 15, 0), (16, 16, 16), {"side": "#panel", "top": "#panel", "bottom": "#panel"}),
           box((5, 9, 5), (11, 10, 11), {"side": "#stage", "top": "#wafer"}),
           box((7, 13, 7), (9, 15, 9), "#head")]
    if on:
        els.append(box((7.5, 10, 7.5), (8.5, 13, 8.5), "#beam", glow=True))
    model("circuit_fabricator_on" if on else "circuit_fabricator", t, els, cutout=True)


def computer_core(on):
    t = {"particle": E + "computer_core_side", "rack": E + ("computer_core_side_on" if on else "computer_core_side"),
         "top": E + "computer_core_top", "post": M + "polished_deepslate", "foot": M + "black_concrete"}
    els = [box((1, 1, 1), (15, 15, 15), {"side": "#rack", "top": "#top", "bottom": "#top"}, glow=on)]
    els += frame(0, 16, 1, 15, 2, "#post")
    els += [box((0, 15, 0), (16, 16, 16), {"side": "#post", "top": "#top"}),
            box((0, 0, 0), (16, 1, 16), "#foot")]
    model("computer_core_on" if on else "computer_core", t, els)


def data_vault(on):
    t = {"particle": E + "data_vault_side", "bays": E + ("data_vault_side_on" if on else "data_vault_side"),
         "top": E + "data_vault_top", "post": M + "polished_deepslate"}
    els = [box((1, 1, 1), (15, 15, 15), {"side": "#bays", "top": "#top", "bottom": "#top"}, glow=on)]
    els += edges(0, 16, 1, "#post")
    els += [box((3, 15.5, 3), (13, 16, 13), {"up": "#top", "side": "#post"})]
    model("data_vault_on" if on else "data_vault", t, els)


def automated_assembler(on):
    t = {"particle": E + "assembler_side", "panel": E + "assembler_side", "glass": M + "glass",
         "arm": M + ("orange_concrete" if on else "light_gray_concrete"), "joint": M + "iron_block", "bed": M + "polished_deepslate",
         "spark": M + "light_blue_stained_glass", "front": E + ("assembler_front_on" if on else "assembler_front")}
    els = [box((0, 0, 0), (16, 4, 16), {"side": "#panel", "top": "#bed"}, uv={s: [0, 10, 16, 14] for s in SIDES}),
           box((1, 4, 1), (15, 15, 15), "#glass", skip=("down",)),
           box((0, 15, 0), (16, 16, 16), {"side": "#panel", "top": "#panel", "bottom": "#panel"}),
           box((5, 4, 4), (11, 5, 10), "#bed"),                                                     # work platform
           box((10, 4, 10), (13, 6, 13), "#joint"),                                                 # arm base
           box((11, 6, 11), (12, 11, 12), "#arm"),
           box((7, 10, 11), (11, 11, 12), "#arm"),
           box((7, 7, 6), (8, 11, 7), "#arm", rotation={"origin": [7.5, 10, 6.5], "axis": "x", "angle": 22.5}),
           box((7, 10, 6), (8, 11, 12), "#arm"),
           box((6.5, 6, 5.5), (8.5, 7, 7.5), "#joint")]                                             # gripper
    if on:
        els.append(box((7, 5, 6), (8, 6, 7), "#spark", glow=True))
    model("automated_assembler_on" if on else "automated_assembler", t, els, cutout=True)


# ---------------- Future ----------------

def nanofabricator(on):
    t = {"particle": E + "nanofabricator_side", "pearl": E + "nanofabricator_side", "glass": M + "glass",
         "core": M + ("amethyst_block" if on else "purple_concrete"), "front": E + ("nanofabricator_front_on" if on else "nanofabricator_front")}
    els = [box((0, 0, 0), (16, 6, 16), {"side": "#pearl", "north": "#front", "top": "#pearl"}, uv={"north": [0, 2, 16, 8]}),
           box((1, 6, 1), (15, 12, 15), "#glass", skip=("down",)),
           box((3, 12, 3), (13, 15, 13), "#glass", skip=("down",)),
           box((2, 15, 2), (14, 16, 14), "#pearl"),
           box((6, 6, 6), (10, 10, 10), "#core", glow=on,
               rotation={"origin": [8, 8, 8], "axis": "y", "angle": 45} if on else None),
           box((7, 12, 7), (9, 15, 9), "#pearl")]
    model("nanofabricator_on" if on else "nanofabricator", t, els, cutout=True)


def fusion_reactor(lit):
    t = {"particle": E + "fusion_reactor_side", "core": E + ("fusion_reactor_side_on" if lit else "fusion_reactor_side"),
         "top": E + "fusion_reactor_top", "base": M + "polished_deepslate",
         "magnet": M + ("sea_lantern" if lit else "light_blue_concrete"), "strut": M + "iron_block"}
    els = [box((0, 0, 0), (16, 2, 16), {"side": "#base", "top": "#top"}),
           box((3, 2, 3), (13, 14, 13), {"side": "#core", "top": "#top"}, uv={s: [1, 1, 15, 15] for s in SIDES}, glow=lit),
           box((2, 14, 2), (14, 16, 14), {"side": "#base", "top": "#top"})]
    els += ring(4, 6, 1, 15, 2, "#magnet", glow=lit) + ring(10, 12, 1, 15, 2, "#magnet", glow=lit)
    els += frame(0, 16, 2, 14, 1, "#strut")
    model("fusion_reactor_on" if lit else "fusion_reactor", t, els)


def plasma_smelter(lit):
    t = {"particle": E + "plasma_smelter_side", "side": E + "plasma_smelter_side", "front": E + ("plasma_smelter_front_on" if lit else "plasma_smelter_front"),
         "top": E + "nanofabricator_side", "vent": M + ("amethyst_block" if lit else "purple_concrete"), "foot": M + "polished_deepslate"}
    els = [box((0, 0, 0), (16, 1, 16), "#foot"),
           box((1, 1, 1), (15, 13, 15), {"side": "#side", "north": "#front", "top": "#top"}, uv={"north": [1, 0, 15, 12]}),
           box((3, 13, 3), (13, 15, 13), "#top"),
           box((5, 15, 5), (11, 16, 11), {"side": "#top", "top": "#vent"}, glow=lit)]
    for x in (2, 12):
        els.append(box((x, 13, 12), (x + 2, 16, 14), "#foot"))                                    # exhaust stacks
    model("plasma_smelter_on" if lit else "plasma_smelter", t, els)


def quantum_cell(on):
    t = {"particle": E + "quantum_cell_side", "cell": E + ("quantum_cell_side_on" if on else "quantum_cell_side"),
         "top": E + "quantum_cell_top", "frame": M + "polished_deepslate", "node": M + ("amethyst_block" if on else "purple_concrete")}
    els = [box((3, 3, 3), (13, 13, 13), {"side": "#cell", "top": "#top", "bottom": "#top"}, glow=on)]
    els += edges(0, 16, 2, "#frame")
    for x in (0, 14):
        for y in (0, 14):
            for z in (0, 14):
                els.append(box((x, y, z), (x + 2, y + 2, z + 2), "#node", glow=on))           # glowing corner nodes
    model("quantum_cell_on" if on else "quantum_cell", t, els)


def matter_printer(on):
    t = {"particle": E + "matter_printer_side", "pearl": E + "matter_printer_side", "glass": M + "glass",
         "bed": M + "polished_deepslate", "head": M + "iron_block", "item": M + "light_blue_concrete", "beam": M + "light_blue_stained_glass"}
    els = [box((0, 0, 0), (16, 3, 16), "#pearl")]
    els += frame(0, 16, 3, 15, 2, "#pearl")
    els += [box((0, 15, 0), (16, 16, 16), "#pearl"),
            box((2, 3, 0.5), (14, 15, 1), "#glass"), box((2, 3, 15), (14, 15, 15.5), "#glass"),
            box((0.5, 3, 2), (1, 15, 14), "#glass"), box((15, 3, 2), (15.5, 15, 14), "#glass"),
            box((3, 3, 3), (13, 4, 13), "#bed"),
            box((2, 13, 7), (14, 14, 9), "#head"),                                                   # gantry
            box((7, 10, 7), (9, 13, 9), "#head")]                                                    # nozzle
    if on:
        els += [box((6, 4, 6), (10, 7, 10), "#item", glow=True),
                box((7.5, 7, 7.5), (8.5, 10, 8.5), "#beam", glow=True)]
    model("matter_printer_on" if on else "matter_printer", t, els, cutout=True)


def wireless_node():
    t = {"particle": E + "wireless_node", "mast": E + "wireless_node", "base": M + "polished_deepslate",
         "dish": E + "nanofabricator_side", "crystal": M + "light_blue_stained_glass", "glow": M + "sea_lantern"}
    els = [box((3, 0, 3), (13, 2, 13), "#base"),
           box((7, 2, 7), (9, 11, 9), "#mast", uv={s: [6, 5, 8, 14] for s in SIDES}),
           box((5, 10, 5), (11, 11, 11), "#dish"),
           box((6.5, 11, 6.5), (9.5, 14, 9.5), "#glow", glow=True, rotation={"origin": [8, 12, 8], "axis": "y", "angle": 45}),
           box((7.25, 14, 7.25), (8.75, 15, 8.75), "#glow", glow=True)]
    model("wireless_node", t, els)


def hover_pad(on):
    t = {"particle": E + "hover_pad_side", "side": E + "hover_pad_side", "top": E + ("hover_pad_top_on" if on else "hover_pad_top"),
         "rim": E + "nanofabricator_side", "tip": M + ("sea_lantern" if on else "light_blue_concrete")}
    els = [box((0, 0, 0), (16, 2, 16), {"side": "#side", "top": "#rim"}, uv={s: [0, 12, 16, 14] for s in SIDES}),
           box((2, 2, 2), (14, 3, 14), {"side": "#rim", "top": "#top"}, glow=on)]
    for x in (0, 13):
        for z in (0, 13):
            els += [box((x, 2, z), (x + 3, 4, z + 3), "#rim"), box((x + 1, 4, z + 1), (x + 2, 5, z + 2), "#tip", glow=on)]
    model("hover_pad_on" if on else "hover_pad", t, els)


# ---------------- Added: blast furnace, arc furnace, strongbox, dynamo, induction furnace ----------------

def steam_blast_furnace(lit):
    t = {"particle": E + "boiler_side", "brick": E + "coke_oven_side", "shell": E + "boiler_side", "cap": E + "boiler_top",
         "front": M + ("blast_furnace_front_on" if lit else "blast_furnace_front"), "pipe": E + "steam_pipe",
         "brass": M + "gold_block", "hot": M + ("magma" if lit else "coal_block")}
    els = [box((0, 0, 0), (16, 7, 16), {"side": "#brick", "top": "#brick", "north": "#front"}, uv={"north": [0, 4, 16, 11]}),
           box((1, 7, 1), (15, 13, 15), {"side": "#shell", "top": "#cap"}),
           box((0.5, 8, 0.5), (15.5, 9, 15.5), "#brass", skip=("up", "down")),                    # brass bands
           box((0.5, 11, 0.5), (15.5, 12, 15.5), "#brass", skip=("up", "down")),
           box((3, 13, 3), (13, 15, 13), {"side": "#shell", "top": "#cap"}),
           box((5, 15, 5), (11, 16, 11), {"side": "#shell", "top": "#hot"}),
           box((6, 15.95, 6), (10, 16, 10), {"up": "#hot"}, glow=lit),
           box((12, 7, 14), (14, 16, 16), "#pipe"),                                                   # steam feed
           box((2, 7, 14), (4, 13, 16), "#pipe")]
    if lit:
        els.append(box((4, 1, -0.01), (12, 6, 0), {"north": "#front"}, uv={"north": [4, 8, 12, 13]}, glow=True))
    model("steam_blast_furnace_lit" if lit else "steam_blast_furnace", t, els)


def arc_furnace(on):
    t = {"particle": E + "arc_furnace_side", "side": E + "arc_furnace_side", "front": E + ("arc_furnace_front_on" if on else "arc_furnace_front"),
         "top": E + "arc_furnace_top", "graphite": M + "polished_basalt_side", "graphite_top": M + "polished_basalt_top",
         "tip": M + ("white_concrete" if on else "gray_concrete"), "copper": M + "copper_block", "leg": M + "polished_blackstone"}
    els = frame(1, 15, 0, 2, 2, "#leg")
    els += [box((1, 2, 1), (15, 11, 15), {"side": "#side", "north": "#front", "top": "#top"}, uv={"north": [1, 3, 15, 12]}),
            box((2, 11, 2), (14, 13, 14), {"side": "#side", "top": "#top"})]
    for x, z in ((4, 4), (10, 5), (6, 10)):                                                         # graphite electrodes
        els += [box((x, 13, z), (x + 2, 16, z + 2), {"side": "#graphite", "top": "#graphite_top"}),
                box((x + 0.5, 15.5, z + 0.5), (x + 1.5, 16, z + 1.5), {"up": "#tip"}, glow=on)]
    for z in (5, 10):                                                                               # copper busbars
        els.append(box((0, 3, z), (1, 12, z + 1), "#copper"))
        els.append(box((15, 3, z), (16, 12, z + 1), "#copper"))
    model("arc_furnace_on" if on else "arc_furnace", t, els)


def strongbox():
    t = {"particle": E + "strongbox_side", "side": E + "strongbox_side", "front": E + "strongbox_front",
         "dark": M + "polished_blackstone", "brass": M + "gold_block", "iron": M + "iron_block"}
    els = frame(1, 15, 0, 1, 2, "#dark")                                                            # little feet
    els += [box((1, 1, 1), (15, 15, 15), {"side": "#side", "north": "#front", "top": "#side"}, uv={"north": [1, 1, 15, 15]}),
            box((0.5, 14, 0.5), (15.5, 15.5, 15.5), "#dark"),                                       # heavy lid rim
            box((9, 6, 0.5), (12, 9, 1), "#brass"),                                                 # combination dial
            box((10, 7, 0), (11, 8, 0.5), "#iron"),
            box((3, 7, 0.5), (7, 8, 1), "#iron"),                                                   # handle
            box((1.5, 3, 0.5), (2.5, 6, 1), "#dark"), box((1.5, 10, 0.5), (2.5, 13, 1), "#dark")]   # hinges
    model("strongbox", t, els)


def dynamo():
    t = {"particle": E + "dynamo_side", "side": E + "dynamo_side", "top": E + "dynamo_top", "base": M + "polished_blackstone",
         "copper": M + "copper_block", "iron": M + "iron_block", "brass": M + "gold_block"}
    els = [box((0, 0, 0), (16, 2, 16), "#base"),
           box((2, 2, 2), (14, 13, 14), {"side": "#side", "top": "#top"}, uv={s: [2, 2, 14, 14] for s in SIDES}),
           box((1.5, 4, 1.5), (14.5, 6, 14.5), "#copper", skip=("up", "down")),                    # field windings
           box((1.5, 9, 1.5), (14.5, 11, 14.5), "#copper", skip=("up", "down")),
           box((7, 7, 0), (9, 9, 2), "#iron"), box((7, 7, 14), (9, 9, 16), "#iron"),               # shaft ends on every side
           box((0, 7, 7), (2, 9, 9), "#iron"), box((14, 7, 7), (16, 9, 9), "#iron"),
           box((4, 13, 6), (5, 15, 7), "#brass"), box((11, 13, 9), (12, 15, 10), "#brass")]       # output terminals
    model("dynamo", t, els)


def induction_furnace(on):
    t = {"particle": E + "induction_furnace_side", "side": E + "induction_furnace_side", "front": E + ("induction_furnace_front_on" if on else "induction_furnace_front"),
         "steel": M + "iron_block", "coil": M + "copper_block", "crucible": M + ("magma" if on else "terracotta"),
         "base": M + "polished_deepslate"}
    els = [box((0, 0, 0), (16, 3, 16), {"side": "#base", "top": "#steel"}),
           box((0, 13, 0), (16, 16, 16), {"side": "#side", "top": "#steel"}, uv={s: [0, 0, 16, 3] for s in SIDES})]
    els += frame(0, 16, 3, 13, 2, "#steel")
    for y in (4, 6.5, 9, 11.5):                                                                     # copper induction coil
        els += ring(y, y + 1.5, 3, 13, 1, "#coil")
    els.append(box((5, 3, 5), (11, 12, 11), {"side": "#crucible", "top": "#crucible"}, glow=on))   # glowing crucible inside
    els.append(box((5, 13.5, -0.3), (11, 15.5, 0), {"north": "#front"}, uv={"north": [4, 3, 12, 6]}))          # control panel
    model("induction_furnace_on" if on else "induction_furnace", t, els)


# ---------------- Big models (multiblocks and two-block machines) ----------------
# Models can reach one block out on every side (-16 to 32). Big boxes are cut on the block grid so each
# piece shows its texture once per block, the way a wall of real blocks would. No faces are culled.

def pick(tex, face):
    if isinstance(tex, str):
        return tex
    return tex.get(face) or (tex.get("top") if face == "up" else None) or (tex.get("bottom") if face == "down" else None) \
        or (tex.get("side") if face in SIDES else None) or tex.get("all")


def local_uv(face, f, t):
    """Vanilla's automatic UVs, for a piece whose corners f..t are given inside its own block (0-16)."""
    return {"north": [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]], "south": [f[0], 16 - t[1], t[0], 16 - f[1]],
            "west": [f[2], 16 - t[1], t[2], 16 - f[1]], "east": [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]],
            "up": [f[0], f[2], t[0], t[2]], "down": [f[0], 16 - t[2], t[0], 16 - f[2]]}[face]


def tiled(frm, to, tex, glow=False, skip=()):
    """A box of any size up to three blocks, textured block by block."""
    def cuts(a, b):
        points = [a]
        k = (math.floor(a / 16) + 1) * 16
        while k < b:
            points.append(k)
            k += 16
        points.append(b)
        return list(zip(points, points[1:]))
    out = []
    for x0, x1 in cuts(frm[0], to[0]):
        for y0, y1 in cuts(frm[1], to[1]):
            for z0, z1 in cuts(frm[2], to[2]):
                origin = [math.floor((a + b) / 32) * 16 for a, b in ((x0, x1), (y0, y1), (z0, z1))]
                f = [x0 - origin[0], y0 - origin[1], z0 - origin[2]]
                t = [x1 - origin[0], y1 - origin[1], z1 - origin[2]]
                outer = {"north": z0 == frm[2], "south": z1 == to[2], "west": x0 == frm[0], "east": x1 == to[0],
                         "down": y0 == frm[1], "up": y1 == to[1]}
                faces = {}
                for face in FACES:
                    key = pick(tex, face)
                    if outer[face] and face not in skip and key:
                        faces[face] = {"texture": key, "uv": local_uv(face, f, t)}
                if faces:
                    element = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
                    if glow:
                        element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
                        element["shade"] = False
                    out.append(element)
    return out


def part(frm, to, tex, glow=False, rotation=None, skip=()):
    """One piece (any size) with its texture squeezed to fit, for small or rotated parts of big models."""
    dims = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}
    faces = {}
    for face in FACES:
        key = pick(tex, face)
        if key and face not in skip:
            a, b = dims[face]
            w, h = min(16, to[a] - frm[a]), min(16, to[b] - frm[b])
            faces[face] = {"texture": key, "uv": [0, 0, round(w, 3), round(h, 3)]}
    element = {"from": list(frm), "to": list(to), "faces": faces}
    if rotation:
        element["rotation"] = rotation
    if glow:
        element["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        element["shade"] = False
    return element


def hidden(name, particle):
    """Draws nothing: the invisible parts of a built multiblock (the core part draws the whole machine)."""
    os.makedirs(OUT, exist_ok=True)
    with open(f"{OUT}/{name}.json", "w") as f:
        json.dump({"textures": {"particle": particle}, "elements": []}, f, indent=2)
        f.write("\n")


def cube(name, textures):
    """A plain full block (the parts of a multiblock before it's built)."""
    model(name, dict(textures, particle=textures.get("particle") or textures.get("side") or textures.get("all")),
          [box((0, 0, 0), (16, 16, 16), {k: "#" + k for k in textures if k != "particle"})])


def coke_oven_formed(lit):
    """The built 3x3x3 coke oven, drawn by its centre brick (the door is in the middle of the front)."""
    t = {"particle": E + "coke_oven_side", "brick": E + "coke_oven_side", "base": M + "polished_blackstone_bricks",
         "iron": M + "anvil", "dark": M + "polished_basalt_side", "front": E + ("coke_oven_front_lit" if lit else "coke_oven_front"),
         "vent": M + ("magma" if lit else "coal_block"), "cap": M + "polished_blackstone"}
    els = tiled((-15.5, -16, -15), (31.5, -12, 31.5), "#base")                                 # plinth (posts stand proud of it)
    els += tiled((-15.5, -12, -14), (31.5, 26, 31.5), "#brick")                                # oven body
    els += tiled((-12, 26, -10), (28, 30, 28), "#brick")                                       # domed roof
    els += tiled((-15.75, 26, -14.5), (31.75, 27, 31.75), {"side": "#cap", "top": "#cap"})    # cap course
    els += [part((2, 30, 2), (14, 32, 14), {"side": "#cap", "top": "#vent"}),                  # chimney
            part((4, 31.9, 4), (12, 32, 12), {"up": "#vent"}, glow=lit)]
    for x in (-16, 28):                                                                        # iron corner posts
        for z in (-16, 28):
            els += tiled((x, -16, z), (x + 4, 27.5, z + 4), "#dark")
    els += tiled((-12, 22, -15.5), (28, 24, -14), "#iron")   # (bolts below sit just in front of these bars)                                       # front tie bar
    els += tiled((-12, -12, -15.5), (28, -10, -14), "#iron")
    els += [part((-3, -12, -16), (0, 19, -14), "#iron"), part((16, -12, -16), (19, 19, -14), "#iron"),   # door frame
            part((-3, 16, -16), (19, 19, -13.5), "#iron"),
            part((0, -10, -14.05), (16, 16, -14), {"north": "#front"}, glow=lit)]                # the oven door
    for x in (-8, 24):                                                                         # tie-rod bolts
        for y in (-11, 23):
            els.append(part((x - 1, y - 1, -16), (x + 1, y + 1, -15.4), "#iron"))
    model("coke_oven_formed_lit" if lit else "coke_oven_formed", t, els, ao=False)


def steam_engine_formed():
    """The built steam engine, drawn by its middle cylinder. Front slab: the flywheel round the output axle.
    Back slab: the cylinder on its bed, with the valve chest and exhaust stack on top."""
    t = {"particle": E + "boiler_side", "bed": M + "polished_blackstone_bricks", "cyl": E + "steam_cylinder",
         "end": E + "steam_cylinder_end", "iron": M + "anvil", "dark": E + "engine_casing", "brass": M + "gold_block",
         "axle": M + "stripped_oak_log", "pipe": E + "steam_pipe", "glass": M + "white_stained_glass"}
    els = tiled((-16, -16, -16), (32, -11, 16), {"side": "#bed", "top": "#bed"})               # engine bed
    # Back slab: the cylinder lying along the engine
    els += tiled((-12, -6, 1), (28, 14, 15), {"side": "#cyl", "east": "#end", "west": "#end"})
    els += [part((-14, -7, 1), (-12, 15, 15), {"east": "#end", "west": "#end", "side": "#dark"}),        # end covers
            part((28, -7, 1), (30, 15, 15), {"east": "#end", "west": "#end", "side": "#dark"}),
            part((-11, -11, 3), (-5, -6, 13), "#dark"), part((21, -11, 3), (27, -6, 13), "#dark")]   # feet
    els += tiled((-6, 14, 3), (22, 20, 13), "#dark")                                           # valve chest
    els += [part((4, 20, 4), (12, 32, 12), {"side": "#pipe", "top": "#iron"}),                 # exhaust stack
            part((3, 29, 3), (13, 31, 13), "#brass"),
            part((-4, 17, 1), (2, 23, 3), {"north": "#glass", "side": "#brass"}),             # pressure gauge
            part((-2.5, 18.5, 0.9), (0.5, 21.5, 1), {"north": "#iron"}, glow=True)]
    # Front slab: bearing pedestal, axle and flywheel
    els += [part((2, -11, -6), (14, 2, 0), "#dark"), part((4, 2, -5), (12, 4, -1), "#brass")]
    els += [part((5, 5, -16), (11, 11, -1), "#axle"), part((3, 3, -13), (13, 13, -5), "#brass")]
    a, w = 22, 4                                     # flywheel: an octagon rim 4 px thick
    side = 2 * a * math.tan(math.radians(22.5))
    z0, z1 = -12, -7
    els += [part((8 - side / 2, 8 + a - w, z0), (8 + side / 2, 8 + a, z1), "#iron"),
            part((8 - side / 2, 8 - a, z0), (8 + side / 2, 8 - a + w, z1), "#iron"),
            part((8 - a, 8 - side / 2, z0), (8 - a + w, 8 + side / 2, z1), "#iron"),
            part((8 + a - w, 8 - side / 2, z0), (8 + a, 8 + side / 2, z1), "#iron")]
    r = (a - w / 2) * math.cos(math.radians(45))
    for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        cx, cy = 8 + sx * r, 8 + sy * r
        els.append(part((cx - side / 2 - 0.6, cy - w / 2, z0), (cx + side / 2 + 0.6, cy + w / 2, z1), "#iron",
                        rotation={"origin": [cx, cy, (z0 + z1) / 2], "axis": "z", "angle": -45 if sx == sy else 45}))
    els += [part((8 - a + w, 6.5, -11), (8 + a - w, 9.5, -8), "#dark"), part((6.5, 8 - a + w, -11), (9.5, 8 + a - w, -8), "#dark")]
    for angle in (45, -45):                                                                    # diagonal spokes
        els.append(part((8 - a + w + 2, 6.5, -11), (8 + a - w - 2, 9.5, -8), "#dark",
                        rotation={"origin": [8, 8, -9.5], "axis": "z", "angle": angle}))
    model("steam_engine_formed", t, els, ao=False)


def joiners_bench_wide():
    """The joiner's bench, two blocks wide: the main half (with the vise) is 0-16, the rest reaches to -16."""
    t = {"particle": M + "oak_planks", "top": M + "stripped_oak_log", "wood": M + "oak_planks", "leg": M + "stripped_spruce_log",
         "iron": M + "iron_block", "dark": M + "dark_oak_planks", "handle": M + "stripped_birch_log"}
    els = tiled((-16, 13, 0), (16, 16, 16), {"side": "#wood", "top": "#top", "bottom": "#wood"})
    for x in (-15, 12):
        for z in (1, 12):
            els.append(part((x, 0, z), (x + 3, 13, z + 3), "#leg"))
    els += tiled((-14, 4, 2), (14, 5, 14), "#dark")                                           # lower shelf
    els += [part((-15, 10, 1), (15, 13, 2), "#dark"),                                         # front apron
            part((-15, 2, 13), (15, 4, 14), "#leg"), part((-15, 2, 2), (15, 4, 3), "#leg"),     # stretchers
            part((11, 11, -0.5), (15, 14, 1.5), "#iron"),                                      # front vise
            part((12.5, 11.5, -1.5), (13.5, 12.5, -0.5), "#iron"),
            part((10, 12, -2), (16, 13, -1.5), "#handle"),
            part((-15, 11, -0.5), (-11, 14, 1.5), "#iron"),                                    # tail vise
            part((-13.5, 11.5, -1.5), (-12.5, 12.5, -0.5), "#iron"),
            part((-14, 16, 13), (-1, 19, 15), "#dark"),                                        # tool rack at the back
            part((-12, 16, 4), (-8, 18.5, 7), "#dark"),                                        # mallet head
            part((-11, 16.5, 7), (-9, 17.5, 12), "#handle"),
            part((-5, 16, 5), (-4, 16.5, 11), "#iron"),                                        # chisel
            part((-5.5, 16, 10), (-3.5, 17, 12.5), "#handle"),
            part((-12, 5, 4), (-4, 7, 10), "#wood"), part((-11, 7, 5), (-6, 8, 9), "#top")]    # offcuts on the shelf
    for x in (-10, -4, 4):                                                                     # bench dog holes
        els.append(part((x, 15.95, 3), (x + 1, 16, 4), {"up": "#dark"}))
    model("joiners_bench", t, els)
    # The item: the same bench moved to the middle of the slot and shrunk to fit
    shifted = json.loads(json.dumps(els))
    for e in shifted:
        e["from"][0] += 8; e["to"][0] += 8
        if "rotation" in e:
            e["rotation"]["origin"][0] += 8
    display = {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.42, 0.42, 0.42]},
               "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.18, 0.18, 0.18]},
               "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
               "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
               "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
               "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}
    model("joiners_bench_item", t, shifted, display=display)
    hidden("joiners_bench_side", M + "oak_planks")


def multiblock_parts():
    cube("coke_oven", {"side": E + "coke_oven_side", "top": E + "coke_oven_side", "bottom": E + "coke_oven_side",
                            "north": E + "coke_oven_front", "particle": E + "coke_oven_side"})
    cube("coke_oven_bricks", {"all": E + "coke_oven_side"})
    cube("engine_casing", {"all": E + "engine_casing"})
    cube("steam_cylinder", {"side": E + "steam_cylinder", "top": E + "steam_cylinder", "bottom": E + "steam_cylinder",
                            "east": E + "steam_cylinder_end", "west": E + "steam_cylinder_end"})
    hidden("coke_oven_hidden", E + "coke_oven_side")
    hidden("engine_casing_hidden", E + "engine_casing")
    hidden("steam_cylinder_hidden", E + "steam_cylinder")
    hidden("steam_engine_hidden", E + "boiler_side")


if __name__ == "__main__":
    knapping_stone()
    for state in (False, True):
        pit_kiln(state); bloomery(state); millstone(state); forge(state); gear_train(state); boiler(state)
        circuit_fabricator(state); computer_core(state); data_vault(state); automated_assembler(state); nanofabricator(state)
        fusion_reactor(state); plasma_smelter(state); quantum_cell(state); matter_printer(state); hover_pad(state)
    woven_basket(); stone_anvil(); joiners_bench_wide(); gearbox(); banded_crate(); steam_engine(); lathe()
    for level in range(5):
        battery(level)
    sorting_cabinet(); wireless_node()
    for state in (False, True):
        steam_blast_furnace(state); arc_furnace(state); induction_furnace(state)
    strongbox(); dynamo()
    for state in (False, True):
        coke_oven_formed(state)
    steam_engine_formed(); multiblock_parts()
    import fix_flicker
    fix_flicker.run()
    print("3D block models written")
