"""Checks Epochsmith's data files for mistakes Minecraft only reports when the game loads.
Run from the project root:  python3 tools/check_data.py"""
import glob
import json
import os
import re

JAVA = "src/main/java"
RES = "src/main/resources"
problems = []

# Every epochsmith item and block id registered in the Java code
ids = set()
for path in glob.glob(f"{JAVA}/**/*.java", recursive=True):
    src = open(path).read()
    ids.update(re.findall(r'(?:ITEMS|BLOCKS)\.register\w*\(\s*"([a-z0-9_/]+)"', src))


def check_item(where, ident):
    if ident.startswith("epochsmith:") and ident.split(":", 1)[1] not in ids:
        problems.append(f"{where}: unknown item {ident}")


def walk_items(where, node):
    if isinstance(node, dict):
        for key in ("item", "id", "name"):
            if isinstance(node.get(key), str):
                check_item(where, node[key])
        for value in node.values():
            walk_items(where, value)
    elif isinstance(node, list):
        for value in node:
            walk_items(where, value)


for path in sorted(glob.glob(f"{RES}/**/*.json", recursive=True)):
    try:
        data = json.load(open(path))
    except ValueError as error:
        problems.append(f"{path}: not valid JSON ({error})")
        continue
    if "/recipe/" in path:
        name = os.path.basename(path)
        walk_items(name, data)
        if data.get("type") == "minecraft:crafting_shaped":
            used = set("".join(data["pattern"])) - {" "}
            keys = set(data["key"])
            if used - keys:
                problems.append(f"{name}: pattern uses symbols with no key: {sorted(used - keys)}")
            if keys - used:
                problems.append(f"{name}: key defines symbols the pattern doesn't use: {sorted(keys - used)}")
            if len({len(row) for row in data["pattern"]}) != 1:
                problems.append(f"{name}: pattern rows are different lengths")
    elif "/loot_table/" in path:
        walk_items(os.path.basename(path), data)

# Models and textures that are referenced but missing
assets = f"{RES}/assets/epochsmith"
for path in glob.glob(f"{assets}/**/*.json", recursive=True):
    text = open(path).read()
    for kind, ref in re.findall(r'"epochsmith:(block|item)/([a-z0-9_/]+)"', text):
        is_model_ref = "/blockstates/" in path or '"parent"' in text and f'"parent": "epochsmith:{kind}/{ref}"' in text
        if is_model_ref and not os.path.exists(f"{assets}/models/{kind}/{ref}.json"):
            problems.append(f"{os.path.relpath(path, assets)}: missing model {kind}/{ref}")
        elif "/models/" in path and not is_model_ref and not os.path.exists(f"{assets}/textures/{kind}/{ref}.png"):
            problems.append(f"{os.path.relpath(path, assets)}: missing texture {kind}/{ref}")

# Every registered block or item should have an item model and a name
lang = json.load(open(f"{assets}/lang/en_us.json"))
for ident in sorted(ids):
    if "/" in ident:
        continue
    if not os.path.exists(f"{assets}/models/item/{ident}.json"):
        problems.append(f"missing item model for {ident}")
    if f"item.epochsmith.{ident}" not in lang and f"block.epochsmith.{ident}" not in lang:
        problems.append(f"missing English name for {ident}")

# Model boxes must stay within one block of their own (-16 to 32), or Minecraft refuses the whole model
for path in sorted(glob.glob(f"{RES}/assets/*/models/**/*.json", recursive=True)):
    try:
        data = json.load(open(path))
    except ValueError:
        continue
    for element in data.get("elements", []):
        for corner in ("from", "to"):
            if any(v < -16 or v > 32 for v in element.get(corner, [])):
                problems.append(f"{path}: a box's '{corner}' {element[corner]} is outside -16..32")

print("\n".join(problems) if problems else "No problems found.")
print(f"({len(ids)} registered ids checked)")
