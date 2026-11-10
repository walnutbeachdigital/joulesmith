#!/usr/bin/env python3
"""Draws every metal texture for Joulesmith: original 16x16 pixel art in a bright,
clean, Thermal-inspired style. Nothing here copies another mod's or Mojang's textures.

Each shape is drawn once as a map of shade levels (0 = outline .. 4 = highlight),
then coloured with a five-tone palette per metal.

    python3 tools/make_textures.py            # write textures
    python3 tools/make_textures.py --sheet    # also write build/texture_sheet.png
"""
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "joulesmith", "textures")

# outline, dark, mid, light, highlight
PALETTES = {
    "iron":     ["#3b3b3f", "#77777e", "#a6a6ad", "#cfcfd5", "#f4f4f8"],
    "copper":   ["#5a2814", "#9c4a26", "#c96c3b", "#e9925a", "#fbc39a"],
    "gold":     ["#6b4805", "#ad7a0c", "#dfad20", "#f5d548", "#fff4a6"],
    "tin":      ["#34445a", "#6c8199", "#9cb2c9", "#c6d7e9", "#eef6ff"],
    "lead":     ["#1f2240", "#3d4372", "#5b6398", "#7d86bb", "#a9b1df"],
    "silver":   ["#3b5263", "#7c9bb0", "#b0cadb", "#d6e8f4", "#ffffff"],
    "nickel":   ["#545137", "#979165", "#c7c192", "#e2deb2", "#fbf8de"],
    "uranium":  ["#203a17", "#3d6c28", "#5ea13a", "#8ad656", "#c8f991"],
    "steel":    ["#25292e", "#4b525a", "#717a83", "#99a2ac", "#c7cfd8"],
    "bronze":   ["#58320f", "#93571d", "#c8812c", "#e8a84e", "#fcd48c"],
    "invar":    ["#3c4843", "#6b7a74", "#96a59e", "#bbc8c1", "#e4eee9"],
    "electrum": ["#5c4a0d", "#a5871c", "#d8b836", "#f1d965", "#fff6b4"],
}


def hex_rgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def blank():
    return [[None] * 16 for _ in range(16)]


def paint(levels, metal):
    pal = [hex_rgba(c) for c in PALETTES[metal]]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = levels[y][x]
            if v is not None:
                img.putpixel((x, y), pal[max(0, min(4, v))])
    return img


def outline(levels, mask):
    """Edge pixels of a shape: lit edges (top/left) dark, shadow edges (bottom/right) outline."""
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            out = lambda dx, dy: not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx])
            if out(1, 0) or out(0, 1):
                levels[y][x] = 0
            elif out(-1, 0) or out(0, -1):
                levels[y][x] = min(levels[y][x], 1) if levels[y][x] is not None else 1
    return levels


def noise(seed):
    r = random.Random(seed)
    return [[r.random() for _ in range(16)] for _ in range(16)]


# ---------------------------------------------------------------------- items
def ingot():
    """A bar lying diagonally, bottom-left to top-right: bright top face, shaded front."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            a, b = x - y, x + y          # a runs along the bar, b across it
            if not (10 <= b <= 20):
                continue
            top = b <= 15
            span = 9 if top else 10    # top face is shorter: the ingot tapers
            if -span <= a <= span:
                mask[y][x] = True
                if top:
                    lv[y][x] = 4 if b <= 11 or (b == 13 and -4 <= a <= 2) else 3
                else:
                    lv[y][x] = 2 if b <= 18 else 1
                # end caps read as their own small faces
                if a == -span and not top:
                    lv[y][x] = 1
    return outline(lv, mask)


def nugget():
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    blobs = [(6.0, 9.0, 3.4), (10.0, 7.5, 2.6), (9.6, 11.4, 2.3)]
    for y in range(16):
        for x in range(16):
            for cx, cy, r in blobs:
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    mask[y][x] = True
                    shade = 3 if (x - cx) + (y - cy) < -0.6 else 2
                    if (x - cx) + (y - cy) < -1.6:
                        shade = 4
                    lv[y][x] = max(lv[y][x] or 0, shade)
    return outline(lv, mask)


def dust():
    """A soft mound with speckles, lit from the top-left."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    n = noise(7)
    for y in range(16):
        for x in range(16):
            t = (y - 4.5) / 9.0                     # 0 at the peak, 1 at the base
            if 0 <= t <= 1 and y <= 13:
                half = 1.2 + 6.4 * math.sin(t * math.pi / 2)
                dx = x - 7.5
                if abs(dx) <= half:
                    mask[y][x] = True
                    side = dx / max(half, 1)
                    shade = 3 if side < -0.25 else (2 if side < 0.45 else 1)
                    if n[y][x] > 0.82:
                        shade = min(4, shade + 1)
                    elif n[y][x] < 0.14:
                        shade = max(1, shade - 1)
                    lv[y][x] = shade
    return outline(lv, mask)


def plate():
    """A square plate face on: bevelled edges and a diagonal sheen."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            mask[y][x] = True
            if y == 2 or x == 2:
                lv[y][x] = 4
            elif y == 13 or x == 13:
                lv[y][x] = 1
            elif y == 3 or x == 3:
                lv[y][x] = 3
            elif y == 12 or x == 12:
                lv[y][x] = 2
            else:
                d = x + y
                lv[y][x] = 4 if d in (11, 12) or d == 15 else 3 if d < 17 else 2
    return outline(lv, mask)


def gear():
    """Eight-tooth gear, face on, with a bevelled rim and a round hub hole."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = math.cos(ang * 8) > 0.15
            if r <= (7.6 if tooth else 5.6) and r > 1.6:
                mask[y][x] = True
                lit = -(dx + dy) / max(r, 0.01)    # +1 facing the light (top-left)
                if r > 5.6:
                    shade = 3 if lit > 0 else 2
                elif r > 4.4:
                    shade = 4 if lit > 0.3 else (2 if lit < -0.3 else 3)
                elif r > 2.8:
                    shade = 2
                else:
                    shade = 1 if lit > 0 else 3    # hub hole edge is lit from inside
                lv[y][x] = shade
    return outline(lv, mask)


def wire():
    """A coil of wire: a thick wound ring with dark lines between the turns, and a loose end."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    cx, cy = 7.0, 7.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if 2.4 <= r <= 6.8:
                mask[y][x] = True
                ang = math.degrees(math.atan2(dy, dx)) % 30
                lit = -(dx + dy) / max(r, 0.01)
                shade = 4 if lit > 0.55 else 3 if lit > -0.2 else 2
                if 12 <= ang < 18:                 # gap between two turns
                    shade = max(1, shade - 2)
                lv[y][x] = shade
    for x, y in ((12, 11), (13, 12), (14, 12), (15, 13)):   # loose end trailing down-right
        mask[y][x] = True
        lv[y][x] = 3
    return outline(lv, mask)


def raw(seed):
    """A lumpy chunk of raw ore: metal-coloured with rocky pits."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    n = noise(seed)
    blobs = [(6.5, 8.5, 4.4), (10.2, 7.0, 3.2), (9.0, 11.2, 2.8), (4.8, 11.0, 2.4)]
    for y in range(16):
        for x in range(16):
            best = None
            for cx, cy, r in blobs:
                d = math.hypot(x - cx, y - cy) / r
                if d <= 1 and (best is None or d < best[0]):
                    best = (d, x - cx, y - cy)
            if best:
                mask[y][x] = True
                d, dx, dy = best
                lit = -(dx + dy)
                shade = 3 if lit > 1.2 else (2 if lit > -1.2 else 1)
                if lit > 2.6 and d > 0.4:
                    shade = 4
                if n[y][x] < 0.16:
                    shade = max(1, shade - 1)
                lv[y][x] = shade
    return outline(lv, mask)


# --------------------------------------------------------------------- blocks
def storage_block():
    """Bevelled frame around four riveted panels: reads as a solid metal block and tiles cleanly."""
    lv = [[2] * 16 for _ in range(16)]
    for i in range(16):
        lv[0][i] = lv[i][0] = 3
        lv[15][i] = lv[i][15] = 1
    lv[0][15] = lv[15][0] = 2
    for py in (1, 8):
        for px in (1, 8):
            for y in range(py, py + 7):
                for x in range(px, px + 7):
                    if y == py or x == px:
                        lv[y][x] = 4
                    elif y == py + 6 or x == px + 6:
                        lv[y][x] = 1
                    else:
                        lv[y][x] = 3 if (x - px) + (y - py) < 6 else 2
            lv[py + 1][px + 1] = 4        # rivet
            lv[py + 2][px + 2] = 1
    return lv


def raw_block(seed):
    n = noise(seed)
    lv = [[None] * 16 for _ in range(16)]
    r = random.Random(seed)
    centers = [(r.uniform(0, 16), r.uniform(0, 16)) for _ in range(9)]
    for y in range(16):
        for x in range(16):
            # tile-safe distance to the nearest lump
            ds = sorted(
                math.hypot(min(abs(x - cx), 16 - abs(x - cx)), min(abs(y - cy), 16 - abs(y - cy)))
                for cx, cy in centers)
            edge = ds[1] - ds[0]
            shade = 3 if edge > 2.2 else (2 if edge > 0.9 else 1)
            if n[y][x] > 0.86:
                shade = min(4, shade + 1)
            if edge < 0.45:
                shade = 0
            lv[y][x] = shade
    return lv


def ore_overlay(seed):
    """Small metal clusters on a transparent background, laid over vanilla stone in the model."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    r = random.Random(seed)
    spots = []
    tries = 0
    while len(spots) < 6 and tries < 500:
        tries += 1
        c = (r.randint(1, 13), r.randint(1, 13))
        if all(abs(c[0] - s[0]) + abs(c[1] - s[1]) > 5 for s in spots):
            spots.append(c)
    shape = [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1), (2, 1), (1, 2), (-1, 1), (2, 0)]
    for sx, sy in spots:
        k = r.randint(6, len(shape))
        for dx, dy in shape[:k]:
            x, y = sx + dx, sy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                mask[y][x] = True
                lv[y][x] = 4 if (dx, dy) == (0, 0) else (3 if dx + dy <= 1 else 2)
    return outline(lv, mask)


# ----------------------------------------------------------------------- main
def metal_of(name):
    for m in sorted(PALETTES, key=len, reverse=True):
        if f"_{m}_" in f"_{name}_":
            return m
    raise ValueError(name)


def draw(kind, name):
    m = metal_of(name)
    seed = sum(map(ord, name))
    if kind == "item":
        if name.startswith("raw_"):
            return paint(raw(seed), m)
        form = name.rsplit("_", 1)[1]
        return paint({"ingot": ingot, "nugget": nugget, "dust": dust, "plate": plate,
                      "gear": gear, "wire": wire}[form](), m)
    if name.endswith("_overlay"):
        return paint(ore_overlay(seed), m)
    if name.startswith("raw_"):
        return paint(raw_block(seed), m)
    return paint(storage_block(), m)


def main():
    manifest = json.load(open(os.path.join(ROOT, "tools", "texture_manifest.json")))
    drawn = []
    for kind in ("block", "item"):
        os.makedirs(os.path.join(TEX, kind), exist_ok=True)
        for name in manifest[kind]:
            img = draw(kind, name)
            img.save(os.path.join(TEX, kind, f"{name}.png"))
            drawn.append((kind, name, img))
    print(f"{len(drawn)} textures written")
    if "--sheet" in sys.argv:
        sheet(drawn)


def sheet(drawn):
    """Contact sheet for review: items at 6x, and each ore overlay composited on grey stone."""
    scale, cols = 6, 12
    cell = 16 * scale + 8
    rows = (len(drawn) + cols - 1) // cols
    out = Image.new("RGBA", (cols * cell, rows * cell), (198, 198, 198, 255))
    stone = Image.new("RGBA", (16, 16), (125, 125, 125, 255))
    for i, (kind, name, img) in enumerate(drawn):
        tile = img
        if name.endswith("_overlay"):
            tile = stone.copy() if "deepslate" not in name else Image.new("RGBA", (16, 16), (72, 72, 78, 255))
            tile.alpha_composite(img)
        big = tile.resize((16 * scale, 16 * scale), Image.NEAREST)
        out.alpha_composite(big, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    path = os.path.join(ROOT, "build", "texture_sheet.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path)
    print("sheet:", path)


if __name__ == "__main__":
    main()
