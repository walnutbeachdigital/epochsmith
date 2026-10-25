"""Stops textures flickering ("z-fighting") in block models. Run from the project root:  python3 tools/fix_flicker.py

When two boxes in a model have faces in exactly the same spot, facing the same way, the game can't decide which to
draw and flickers between them as you move. This lifts the smaller face a hair (0.02 px) off the bigger one.
make_models.py runs it automatically; it can also be run on its own after editing a model by hand."""
import glob
import json

MODELS = "src/main/resources/assets/epochsmith/models/block"
# face: (axis, 1 if it's the box's "to" side else 0, which way is outward)
FACES = {"north": (2, 0, -1), "south": (2, 1, 1), "west": (0, 0, -1), "east": (0, 1, 1), "down": (1, 0, -1), "up": (1, 1, 1)}
STEP = 0.02


def plane(e, face):
    axis, side, _ = FACES[face]
    return e["to"][axis] if side else e["from"][axis]


def overlap(a, b, axis):
    others = [i for i in range(3) if i != axis]
    return all(min(a["to"][i], b["to"][i]) - max(a["from"][i], b["from"][i]) > 0.05 for i in others)


def area(e, axis):
    w, h = [e["to"][i] - e["from"][i] for i in range(3) if i != axis]
    return w * h


def move(e, face, amount):
    """Moves one face of a box outward (positive) or inward; False if that would leave the allowed space."""
    axis, side, out = FACES[face]
    corner = "to" if side else "from"
    value = round(e[corner][axis] + out * amount, 4)
    if not -16 <= value <= 32 or e["to"][axis] - e["from"][axis] - (0 if amount > 0 else STEP) <= 0:
        return False
    e[corner][axis] = value
    return True


def fix(elements):
    """Returns how many flickering spots were fixed."""
    fixed = 0
    for _ in range(6):
        changed = False
        for i, a in enumerate(elements):
            for b in elements[i + 1:]:
                if a.get("rotation") or b.get("rotation"):
                    continue
                for face, (axis, _, _) in FACES.items():
                    if face not in a["faces"] or face not in b["faces"]:
                        continue
                    if abs(plane(a, face) - plane(b, face)) > 1e-6 or not overlap(a, b, axis):
                        continue
                    small, big = (a, b) if area(a, axis) <= area(b, axis) else (b, a)
                    if move(small, face, STEP) or move(big, face, -STEP):
                        fixed += 1
                        changed = True
        if not changed:
            break
    return fixed


def run(paths=None):
    total = 0
    for path in paths or sorted(glob.glob(f"{MODELS}/*.json")):
        data = json.load(open(path))
        if not data.get("elements"):
            continue
        n = fix(data["elements"])
        if n:
            total += n
            with open(path, "w") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
    return total


if __name__ == "__main__":
    print(f"{run()} flickering faces fixed")
