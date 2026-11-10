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


def paint_ramp(levels, ramp_hex):
    ramp = [hex_rgba(c) for c in ramp_hex]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = levels[y][x]
            if v is not None:
                img.putpixel((x, y), ramp[max(0, min(len(ramp) - 1, v))])
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


GEAR_TEETH = [(8, 2), (12, 6), (11, 11), (6, 12), (2, 8), (3, 3)]   # top-left cell of each 2x2 tooth


def gear():
    """A compact, rounded six-tooth gear: a full round body, six square 2x2 teeth turned slightly
    off-axis, and a small axle hole. Patchy shading lit from the top-left, darkest along the
    bottom-right edges."""
    lv, mask = blank(), [[False] * 16 for _ in range(16)]
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            if math.hypot(x - cx, y - cy) <= 5.1:
                mask[y][x] = True
    for tx, ty in GEAR_TEETH:
        for dx in (0, 1):
            for dy in (0, 1):
                mask[ty + dy][tx + dx] = True
    hole = [(7, 7), (8, 7), (7, 8), (8, 8)]
    for x, y in hole:
        mask[y][x] = False
    n = noise(9)
    for y in range(16):
        for x in range(16):
            if not mask[y][x]:
                continue
            light = 1 - ((x - 1) + (y - 1)) / 24.0
            patch = 1 if ((x // 2) + (y // 2)) % 2 == 0 else 0
            if n[y][x] > 0.85:
                patch += 1
            lv[y][x] = max(2, min(7, int(2.2 + light * 4.2) + patch))
    lv = edge_light(lv, mask)
    for x, y in hole:                         # rim of the axle hole: shadow top-left, light bottom-right
        for nx, ny, val in ((x - 1, y, 2), (x, y - 1, 2), (x + 1, y, 6), (x, y + 1, 6)):
            if (nx, ny) not in hole:
                lv[ny][nx] = val
    return lv


SPOOL_RAMP = ["#2b2620", "#3d342a", "#54473a", "#6b5a48", "#857059", "#9e876c", "#b9a183", "#d4bd9e"]  # dark wood


def wire():
    """A spool of wire seen from slightly above: wood flanges top and bottom, the metal's wire wound
    between them in rounded rows (lit on the left, shaded to the right), and a loose end trailing off.
    Returns (spool levels, wire levels); both on eight-step ramps."""
    spool, wire_lv = blank(), blank()
    cx = 7.5
    # flanges: flat ellipses, top one fully visible, bottom one showing its front rim
    for y in range(16):
        for x in range(16):
            dt = ((x - cx) / 6.6) ** 2 + ((y - 3.5) / 2.1) ** 2
            db = ((x - cx) / 6.6) ** 2 + ((y - 12.0) / 2.1) ** 2
            if dt <= 1:
                inner = ((x - cx) / 1.6) ** 2 + ((y - 3.5) / 0.6) ** 2 <= 1      # spindle hole
                spool[y][x] = 1 if inner else (6 if y <= 3 else 4) - (1 if x > cx + 3 else 0)
            elif db <= 1 and y >= 12:
                spool[y][x] = 3 if x < cx + 2 else 2
    # darker rim around each flange
    for y in range(16):
        for x in range(16):
            if spool[y][x] is not None and spool[y][x] > 1:
                edge = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or spool[y + dy][x + dx] is None
                           for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if edge:
                    spool[y][x] = 2 if (y > 3 or x > cx) else 4
    # wound wire: a cylinder between the flanges, rows of wire lit on the left
    for y in range(5, 12):
        for x in range(2, 14):
            u = (x - 2) / 11.0                                  # 0 left .. 1 right
            light = math.cos((u - 0.3) * math.pi * 0.9)          # brightest a little left of centre
            row_top = (y - 5) % 2 == 0                           # each winding: lit top half, shaded bottom
            v = 2 + light * 4.2 + (0.8 if row_top else -0.6)
            wire_lv[y][x] = max(1, min(7, int(round(v))))
    for y in range(5, 12):                                       # darker edges of the cylinder
        wire_lv[y][2] = max(1, wire_lv[y][2] - 2)
        wire_lv[y][13] = 1
    # loose end leaving the bottom-right of the coil
    for x, y, v in ((14, 9, 5), (14, 10, 4), (15, 11, 4), (15, 12, 2)):
        wire_lv[y][x] = v
    return spool, wire_lv


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
# ---------------------------------------------------------------- metal blocks
# Each metal's block has its own pattern; all use the metal's eight-step ramp (0 darkest .. 7 brightest)
# and tile cleanly.

def _edge(x, y):
    """Distance to the nearest edge and which edge it is."""
    d = min(x, y, 15 - x, 15 - y)
    side = "top" if y == d else "left" if x == d else "bottom" if 15 - y == d else "right"
    return d, side


def block_inset():
    """Wide bevelled frame around a sunken square."""
    lv = blank()
    for y in range(16):
        for x in range(16):
            d, side = _edge(x, y)
            lit = side in ("top", "left")
            if d == 0:
                v = 7 if lit else 0
            elif d < 3:
                v = (6 if side == "top" else 5) if lit else (2 if side == "bottom" else 3)
            elif d == 3:
                v = 2 if lit else 6                      # inner bevel faces the other way
            else:
                v = 4 if (x + y) < 15 else 3
            lv[y][x] = v
    return lv


def block_panels():
    """Four raised panels with dark seams between them."""
    lv = blank()
    for y in range(16):
        for x in range(16):
            lx, ly = x % 8, y % 8
            d = min(lx, ly, 7 - lx, 7 - ly)
            if d == 0:
                v = 1
            elif d == 1:
                v = 6 if (ly == 1 or lx == 1) and not (ly == 6 or lx == 6) else 2
            else:
                v = 5 if lx + ly < 7 else 4
            lv[y][x] = v
    return lv


def block_ribs():
    """Vertical ribs inside a frame."""
    lv = blank()
    for y in range(16):
        for x in range(16):
            d, side = _edge(x, y)
            lit = side in ("top", "left")
            if d == 0:
                v = 6 if lit else 0
            elif d == 1:
                v = 5 if lit else 1
            else:
                col = (x - 2) % 3
                v = (6, 4, 2)[col]
                if y == 2:
                    v = min(7, v + 1)
                elif y == 13:
                    v = max(1, v - 2)
            lv[y][x] = v
    return lv


def block_brushed():
    """Narrow frame around diagonally brushed metal."""
    lv = blank()
    n = noise(21)
    for y in range(16):
        for x in range(16):
            d, side = _edge(x, y)
            lit = side in ("top", "left")
            if d == 0:
                v = 6 if lit else 1
            elif d == 1:
                v = 5 if lit else 2
            else:
                streak = (x + y) % 6
                v = 6 if streak == 0 else 5 if streak in (1, 5) else 4
                if (x + y) > 18:
                    v -= 1
                if n[y][x] > 0.9:
                    v = min(7, v + 1)
            lv[y][x] = v
    return lv


def block_pyramid():
    """Four sloped faces meeting at a small flat centre."""
    lv = blank()
    for y in range(16):
        for x in range(16):
            d, side = _edge(x, y)
            if d == 0:
                v = 6 if side in ("top", "left") else 1
            elif 6 <= x <= 9 and 6 <= y <= 9:
                v = 3                                    # flat top
            else:
                v = {"top": 6, "left": 5, "right": 3, "bottom": 2}[side]
                if x == y or x == 15 - y:                # ridges where faces meet
                    v = 7 if (x < 8 and y < 8) else 4 if y < 8 or x < 8 else 1
            lv[y][x] = v
    return lv


def block_smooth():
    """Thin frame around a soft diagonal gradient."""
    bayer = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
    lv = blank()
    for y in range(16):
        for x in range(16):
            d, side = _edge(x, y)
            lit = side in ("top", "left")
            if d == 0:
                v = 7 if lit else 1
            elif d == 1:
                v = 5 if lit else 2
            else:
                t = 1 - (x + y - 4) / 22.0
                v = 3 + int(t * 3 + bayer[y % 4][x % 4] / 16.0)
            lv[y][x] = max(0, min(7, v))
    return lv


def block_studs():
    """A grid of small square studs."""
    lv = blank()
    for y in range(16):
        for x in range(16):
            lx, ly = x % 4, y % 4
            if lx == 3 or ly == 3:
                v = 0
            elif lx == 0 and ly == 0:
                v = 7
            elif lx == 0 or ly == 0:
                v = 5
            elif lx == 2 and ly == 2:
                v = 2
            else:
                v = 4 if (lx, ly) == (1, 1) else 3
            lv[y][x] = v
    return lv


# How much of the ramp an ingot uses, darkest step first (percent). Blocks are rebalanced to match it so
# they read as the same material as the ingot beside them.
INGOT_TONE_MIX = [10, 6, 15, 19, 22, 17, 10, 1]


def with_gear_shading(lv, spread=6):
    """Lay the gears' and plates' shading under a block pattern: a soft top-left to bottom-right
    sweep and blocky 2x2 light patches. The pattern's own shades are spread apart so they stay in
    charge; a smaller spread lets the sweep cross them, for small repeating patterns."""
    out = blank()
    for y in range(16):
        for x in range(16):
            v = lv[y][x]
            if v is None:
                continue
            sweep = round((1 - (x + y) / 30.0) * 4) - 2            # +2 top-left .. -2 bottom-right
            patch = 1 if ((x // 2) + (y // 2)) % 2 == 0 else 0
            out[y][x] = v * spread + sweep + patch
    return out


def match_ingot_tones(lv):
    """Remap a block's shades so its mix of dark, mid and bright matches an ingot's. Each shade keeps its
    place in the order (darker stays darker), so the pattern is untouched; only the tones shift."""
    counts = {}
    for row in lv:
        for v in row:
            if v is not None:
                counts[v] = counts.get(v, 0) + 1
    total = sum(counts.values())
    target_edges, acc = [], 0
    for pct in INGOT_TONE_MIX:
        acc += pct
        target_edges.append(acc / sum(INGOT_TONE_MIX))
    mapping, seen = {}, 0
    for v in sorted(counts):
        mid = (seen + counts[v] / 2) / total          # where this shade sits in the block's own tone range
        seen += counts[v]
        mapping[v] = next(i for i, edge in enumerate(target_edges) if mid <= edge + 1e-9)
    return [[None if v is None else mapping[v] for v in row] for row in lv]


BLOCK_DESIGN = {
    "tin": block_brushed, "lead": block_studs, "silver": block_pyramid, "nickel": block_smooth,
    "uranium": block_inset, "steel": block_panels, "bronze": block_ribs, "invar": block_inset,
    "electrum": block_brushed,
}


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


ORE_CLUSTERS = [  # shade steps 0..7 on the eight-step ramp; None = empty
    [[None, 6, 6, None], [6, 7, 6, 4], [5, 6, 4, 2], [None, 3, 2, None]],                 # 0 big
    [[None, None, 6, 5, None], [None, 6, 7, 6, 4], [6, 7, 5, 3, 1], [4, 3, 2, 1, None]],  # 1 big
    [[None, 6, 5], [6, 7, 4], [4, 3, 1]],                                                # 2 mid
    [[6, 7, 4], [4, 3, 1]],                                                              # 3 mid
    [[6, 5, None], [5, 7, 4], [None, 3, 1]],                                             # 4 mid
    [[6, 4], [4, 1]],                                                                    # 5 small
    [[7]],                                                                               # 6 speck
]


def ore_overlay(seed):
    """Chunky ore clusters on a transparent background (the model lays them over vanilla stone or
    deepslate): each lit top-left, shaded bottom-right, plus a few single-pixel specks."""
    r = random.Random(seed)
    lv = blank()
    taken = set()
    placed = tries = 0
    target = r.randint(7, 9)
    while placed < target and tries < 400:
        tries += 1
        shape = ORE_CLUSTERS[r.choice([0, 1, 0, 1, 2, 3, 4, 2, 5, 6])]
        h, w = len(shape), len(shape[0])
        ox, oy = r.randint(0, 16 - w), r.randint(0, 16 - h)
        cells = [(ox + x, oy + y) for y in range(h) for x in range(w) if shape[y][x] is not None]
        # clusters may touch corner to corner, never side by side
        if any((cx + dx, cy + dy) in taken for cx, cy in cells for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1))):
            continue
        for y in range(h):
            for x in range(w):
                if shape[y][x] is not None:
                    lv[oy + y][ox + x] = shape[y][x]
                    taken.add((ox + x, oy + y))
        placed += 1
    # centre the whole layout on the block
    pts = [(x, y) for y in range(16) for x in range(16) if lv[y][x] is not None]
    if pts:
        mx = sum(p[0] for p in pts) / len(pts)
        my = sum(p[1] for p in pts) / len(pts)
        sx, sy = round(7.5 - mx), round(7.5 - my)
        x_lo, x_hi = min(p[0] for p in pts), max(p[0] for p in pts)
        y_lo, y_hi = min(p[1] for p in pts), max(p[1] for p in pts)
        sx = max(-x_lo, min(15 - x_hi, sx))
        sy = max(-y_lo, min(15 - y_hi, sy))
        moved = blank()
        for x, y in pts:
            moved[y + sy][x + sx] = lv[y][x]
        lv = moved
    return lv


# Stone around a chunk, drawn on the overlay so the chunk looks set into the rock: the rock's lip throws
# a shadow over the chunk's top-left edge and catches light along its bottom-right edge (like vanilla ores).
ORE_LIP = {False: ("#4a4a4a", "#5e5e5e", "#9c9c9c"), True: ("#1f1f25", "#2b2b33", "#5d5d66")}


def recess(lv):
    """Reshades clusters as sunk in: dark where the stone above or to the left overhangs them,
    bright along the lower-right where light falls into the hole, mid tones between."""
    filled = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and lv[y][x] is not None
    out = blank()
    for y in range(16):
        for x in range(16):
            if lv[y][x] is None:
                continue
            under_lip = not filled(x, y - 1) or not filled(x - 1, y)
            lit_wall = not filled(x, y + 1) or not filled(x + 1, y)
            if under_lip and lit_wall:
                v = 3                                    # a small chunk: mostly in shadow
            elif under_lip:
                v = 1 if (not filled(x, y - 1) and not filled(x - 1, y)) else 2
            elif lit_wall:
                v = 7 if (not filled(x, y + 1) and not filled(x + 1, y)) else 6
            else:
                v = 5 if lv[y][x] >= 5 else 4
            out[y][x] = v
    return out


def ore_image(lv, metal, deepslate):
    deep, shadow, light = (hex_rgba(c) for c in ORE_LIP[deepslate])
    img = paint8(recess(lv), metal)
    filled = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and lv[y][x] is not None
    for y in range(16):
        for x in range(16):
            if filled(x, y):
                continue
            below, right = filled(x, y + 1), filled(x + 1, y)
            above, left = filled(x, y - 1), filled(x - 1, y)
            if below and right:
                img.putpixel((x, y), deep)               # the rock's lip right over the chunk's corner
            elif below or right:
                img.putpixel((x, y), shadow)
            elif above and left:
                img.putpixel((x, y), light)              # light catching the far rim
    return img


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
        if form == "wire":
            spool, wound = wire()
            img = paint_ramp(spool, SPOOL_RAMP)
            img.alpha_composite(paint8(wound, m))
            return img
        if form in ("plate", "gear", "dust"):
            return paint8({"plate": plate, "gear": gear, "dust": dust}[form](), m)
        return paint({"ingot": ingot, "nugget": nugget, "dust": dust, "wire": wire}[form](), m)
    if name.endswith("_overlay"):
        # stone and deepslate ores share one layout per metal; only the stone behind them differs
        return ore_image(ore_overlay(sum(map(ord, m)) * 7), m, "deepslate" in name)
    if name.startswith("raw_"):
        return paint(raw_block(seed), m)
    # small repeating patterns (studs, panels) need the sweep to cross their shades to read as lit
    spread = 2 if BLOCK_DESIGN[m] in (block_studs, block_panels) else 6
    return paint8(match_ingot_tones(with_gear_shading(BLOCK_DESIGN[m](), spread)), m)


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
