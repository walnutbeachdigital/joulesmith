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
PALETTES = {  # eight-step ramps, darkest to brightest; same values as Metal.java
    "iron":      ["#4a4a52", "#5e5f68", "#76777f", "#8d8e96", "#a8a9b0", "#c4c5cb", "#dcdde1", "#f4f4f6"],
    "copper":    ["#772c15", "#a0511f", "#b8622a", "#ce6e26", "#da7e2a", "#f0a030", "#fcbe2d", "#ffe08a"],
    "gold":      ["#8a5a12", "#a06d14", "#b8841a", "#cf9c22", "#e3b52e", "#f1cd45", "#fbe36e", "#fff4a8"],
    "tin":       ["#314f6f", "#476f81", "#517c88", "#6c8998", "#88a2a7", "#90afac", "#a1c6c2", "#bfd8dd"],
    "lead":      ["#13142f", "#232457", "#2f3061", "#393c61", "#454967", "#575e79", "#727989", "#7d97a6"],
    "silver":    ["#2a3345", "#3c4859", "#4d5c6a", "#647380", "#758392", "#9fadb4", "#beccd2", "#ffffff"],
    "nickel":    ["#734c3b", "#866444", "#997857", "#a28960", "#b0a075", "#c7b784", "#e7d498", "#f9f5ab"],
    "uranium":   ["#1f3d17", "#2c5520", "#3b6e28", "#4c8a31", "#62a63c", "#7fc24c", "#a2db66", "#c8f08e"],
    "steel":     ["#22262e", "#30353f", "#3e4450", "#4d5461", "#5f6774", "#767e8b", "#939aa6", "#b8bec8"],
    "bronze":    ["#6a3412", "#82441a", "#9b5622", "#b46a2c", "#ca7f37", "#de9a48", "#eeb764", "#f9d58d"],
    "invar":     ["#4a666a", "#5c7c7b", "#768885", "#859792", "#a3b1a8", "#c5ccc3", "#d7ded9", "#e1e7e5"],
    "electrum":  ["#957c3d", "#a19144", "#bca54f", "#c9b255", "#d9c25f", "#e9da75", "#f8f796", "#fbfbbc"],
}


def hex_rgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def blank():
    return [[None] * 16 for _ in range(16)]


def paint(levels, metal):
    ramp = [hex_rgba(c) for c in PALETTES[metal]]
    pal = [ramp[i] for i in (0, 1, 3, 5, 7)]  # shade levels 0..4 -> ramp steps
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = levels[y][x]
            if v is not None:
                img.putpixel((x, y), pal[max(0, min(4, v))])
    return img


def paint8(levels, metal):
    ramp = [hex_rgba(c) for c in PALETTES[metal]]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = levels[y][x]
            if v is not None:
                img.putpixel((x, y), ramp[max(0, min(7, v))])
    return img


def edge_light(levels, mask):
    """Reference-style edges: top-left edges catch the light, bottom-right edges fall to the darkest tones.
    No full outline."""
    inside = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and mask[y][x]
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            if not inside(x + 1, y) or not inside(x, y + 1):
                levels[y][x] = 0 if (not inside(x + 1, y + 1)) else 1
            elif not inside(x - 1, y) or not inside(x, y - 1):
                levels[y][x] = min(7, levels[y][x] + 2)
    return levels


def outline(levels, mask, all_dark=False):
    """Edge pixels of a shape: lit edges (top/left) dark, shadow edges (bottom/right) outline."""
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            out = lambda dx, dy: not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mask[y + dy][x + dx])
            if out(1, 0) or out(0, 1) or (all_dark and (out(-1, 0) or out(0, -1))):
                levels[y][x] = 0
            elif out(-1, 0) or out(0, -1):
                levels[y][x] = min(levels[y][x], 1) if levels[y][x] is not None else 1
    return levels


def noise(seed):
    r = random.Random(seed)
    return [[r.random() for _ in range(16)] for _ in range(16)]


# ---------------------------------------------------------------------- items
def ingot():
    """A chunky bar seen at three-quarters: the top face leans back to the right, a bright edge where
    it meets the front, a mid-tone front, a darker right end and a dark outline all round."""
    # Shape row by row (outline included): y -> (first x, last x)
    rows = {3: (7, 12), 4: (4, 14), 5: (2, 14), 6: (1, 14), 7: (1, 14),
            8: (1, 14), 9: (1, 14), 10: (1, 14), 11: (2, 13)}
    top = {4: (5, 13), 5: (3, 13), 6: (2, 12)}
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            mask[y][x] = True
            if y in top and top[y][0] <= x <= top[y][1]:
                lv[y][x] = 3
            elif x >= 13:
                lv[y][x] = 1                       # right end in shade
            elif y == 7:
                lv[y][x] = 3 if x <= 9 else 2      # bevel under the top edge
            elif y == 10:
                lv[y][x] = 1
            else:
                lv[y][x] = 2
    for x in range(2, 9):
        lv[6][x] = 4                               # bright front edge of the top face
    for x in range(5, 9):
        lv[4][x] = 4                               # glint near the back
    lv[5][13] = 2
    return outline(lv, mask, all_dark=True)


def nugget():
    """A small rounded drop."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    rows = {5: (7, 8), 6: (6, 9), 7: (5, 10), 8: (5, 10), 9: (5, 10), 10: (6, 9)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            mask[y][x] = True
            lv[y][x] = 4 if (x == 6 and y in (7, 8)) else (3 if x <= 7 else 2)
    return outline(lv, mask, all_dark=True)

def dust():
    """A rounded pile with a pointed top: speckled grains, lit from the top-left, dark bottom rim."""
    rows = {3: (7, 8), 4: (6, 9), 5: (5, 10), 6: (4, 11), 7: (3, 12), 8: (2, 13),
            9: (2, 13), 10: (2, 13), 11: (3, 12), 12: (4, 11), 13: (6, 9)}
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    n = noise(11)
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            mask[y][x] = True
            light = 1 - ((x - 2) / 11 + (y - 3) / 10) / 2
            grain = 2 if n[y][x] > 0.8 else (-2 if n[y][x] < 0.2 else 0)
            lv[y][x] = max(2, min(7, int(2.5 + light * 4.2) + grain))
    return edge_light(lv, mask)


def plate(band_axis="v"):
    """A square sheet turned about 30 degrees clockwise: light streaks running parallel to its
    top-left edge, lit from the top-left, darkest along the bottom-right edges."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    cx, cy, half = 7.6, 7.6, 4.85
    c, s_ = math.cos(math.radians(30)), math.sin(math.radians(30))
    n = noise(5)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            u, v = dx * c + dy * s_, -dx * s_ + dy * c
            if abs(u) <= half and abs(v) <= half:
                mask[y][x] = True
                light = 1 - ((u + half) + (v + half)) / (4 * half)
                k = u if band_axis == "u" else v
                band = 1 if (int(k + half) // 2) % 2 == 0 else -1
                if n[y][x] > 0.85:
                    band = 0
                lv[y][x] = max(2, min(7, int(2.5 + light * 4.5) + band))
    return edge_light(lv, mask)


def gear():
    """A six-tooth gear turned slightly off-axis: full round body, short wide teeth, small axle hole,
    lit from the top-left with the darkest tones only along the bottom-right edges."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    cx = cy = 7.5
    off = math.radians(15)
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            tooth = math.cos(6 * (math.atan2(dy, dx) - off)) > -0.15
            if 1.2 < r <= (6.9 if tooth else 5.3):
                mask[y][x] = True
    hole = [(x, y) for y in range(16) for x in range(16) if 0 < math.hypot(x - cx, y - cy) <= 1.2] + [(7, 7), (8, 7), (7, 8), (8, 8)]
    n = noise(9)
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            light = 1 - ((x - 1) + (y - 1)) / 26.0
            speck = 1 if n[y][x] > 0.75 else (-1 if n[y][x] < 0.2 else 0)
            lv[y][x] = max(2, min(7, int(2.5 + light * 4.3) + speck))
    lv = edge_light(lv, mask)
    for x, y in set(hole):                                         # rim around the hole
        for nx, ny, val in ((x - 1, y, 1), (x, y - 1, 1), (x + 1, y, 6), (x, y + 1, 6)):
            if 0 <= nx < 16 and 0 <= ny < 16 and mask[ny][nx] and math.hypot(nx - cx, ny - cy) < 2.6:
                lv[ny][nx] = val
    return lv


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
    """Smooth polished metal: soft blotches and a gentle top-left light, tiles without seams."""
    r = random.Random(42)
    g = [[r.random() for _ in range(4)] for _ in range(4)]   # 4x4 tileable value noise

    def smooth(x, y):
        fx, fy = x / 4.0, y / 4.0
        x0, y0 = int(fx) % 4, int(fy) % 4
        x1, y1 = (x0 + 1) % 4, (y0 + 1) % 4
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        top = g[y0][x0] * (1 - tx) + g[y0][x1] * tx
        bot = g[y1][x0] * (1 - tx) + g[y1][x1] * tx
        return top * (1 - ty) + bot * ty

    bayer = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
    lv = [[2] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            # broad diagonal sheen plus soft blotches, dithered between three tones
            sheen = 0.5 + 0.5 * math.cos((x - y) * math.pi / 8)
            v = 0.45 * sheen + 0.35 * smooth(x, y) + 0.2 * (1 - (x + y) / 30)
            t = v * 2.2 + (bayer[y % 4][x % 4] / 16 - 0.5) * 0.55
            lv[y][x] = 2 + max(0, min(2, int(t)))
    for i in range(16):
        lv[0][i] = max(lv[0][i], 3)
        lv[i][0] = max(lv[i][0], 3)
        lv[15][i] = min(lv[15][i], 2)
        lv[i][15] = min(lv[i][15], 2)
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
        if form in ("plate", "gear", "dust"):
            return paint8({"plate": plate, "gear": gear, "dust": dust}[form](), m)
        return paint({"ingot": ingot, "nugget": nugget, "dust": dust, "wire": wire}[form](), m)
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
