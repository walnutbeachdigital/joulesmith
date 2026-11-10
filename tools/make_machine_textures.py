#!/usr/bin/env python3
"""Draws Joulesmith's machine textures and the shared machine screen. Original art in a
clean, Thermal-inspired style: a steel casing shared by every machine, tier trim on the
corners, and a distinct front per machine.

    python3 tools/make_machine_textures.py
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "joulesmith", "textures")

STEEL = ["#24272d", "#3b3f47", "#545a63", "#6c727c", "#868d97", "#a2a9b3", "#c0c6ce", "#dde2e8"]
COPPER = ["#5a2412", "#8a3a1c", "#b8622a", "#da7e2a", "#f0a030", "#fcbe2d"]       # tier 1 trim
FIRE = ["#2a0d06", "#6b1c08", "#b8360c", "#e8641a", "#f9a33a", "#ffd86a", "#fff4c2"]
PURPLE = ["#1d0f2e", "#33184f", "#4f2378", "#6d33a3", "#8d4dc9", "#b07ae0", "#d4adf2", "#f2e2ff"]


def rgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def casing(ramp=STEEL, trim=COPPER):
    """Bevelled panel with corner bolts and tier-coloured corner brackets."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            lit = x <= y and x + y <= 15 or y <= x and x + y < 15   # top/left half of the frame
            top_left = (y == d and x + y <= 15) or (x == d and x + y <= 15)
            if d == 0:
                c = ramp[5] if top_left else ramp[0]
            elif d == 1:
                c = ramp[6] if top_left else ramp[1]
            else:
                # inner panel: soft diagonal light from the top-left
                t = 1 - (x + y - 4) / 22
                c = ramp[4] if t > 0.62 else ramp[3] if t > 0.28 else ramp[2]
                if (x + y) % 7 == 0 and t > 0.4:
                    c = ramp[5]
            px[x, y] = rgba(c)
    # corner brackets in the tier colour
    for cx, cy, sx, sy in ((2, 2, 1, 1), (13, 2, -1, 1), (2, 13, 1, -1), (13, 13, -1, -1)):
        for i in range(3):
            px[cx + sx * i, cy] = rgba(trim[3] if sy > 0 else trim[1])
            px[cx, cy + sy * i] = rgba(trim[3] if sx > 0 else trim[1])
        px[cx, cy] = rgba(trim[4])
    return img


def bolt(img, x, y, ramp=STEEL):
    px = img.load()
    px[x, y] = rgba(ramp[7])
    px[x + 1, y] = rgba(ramp[5])
    px[x, y + 1] = rgba(ramp[5])
    px[x + 1, y + 1] = rgba(ramp[1])


def machine_side():
    img = casing()
    for x, y in ((4, 4), (10, 4), (4, 10), (10, 10)):
        bolt(img, x, y)
    return img


def machine_top():
    img = casing()
    px = img.load()
    # vent grille
    for y in (5, 7, 9):
        for x in range(4, 12):
            px[x, y] = rgba(STEEL[0])
            px[x, y + 1] = rgba(STEEL[5])
    return img


def machine_bottom():
    img = casing()
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * 0.8), int(g * 0.8), int(b * 0.8), a)
    return img


def furnace_front(on):
    """Casing with a barred window; behind the bars, dark when idle and glowing fire when running."""
    img = casing()
    px = img.load()
    x0, y0, x1, y1 = 4, 4, 11, 10
    for y in range(y0 - 1, y1 + 2):
        for x in range(x0 - 1, x1 + 2):
            edge = y in (y0 - 1, y1 + 1) or x in (x0 - 1, x1 + 1)
            if edge:
                px[x, y] = rgba(STEEL[1] if (y == y0 - 1 or x == x0 - 1) else STEEL[6])
            elif on:
                # brightest low in the middle, cooling toward the top and sides
                heat = (y - y0) / (y1 - y0) * 0.7 + (1 - abs(x - 7.5) / 4) * 0.4
                px[x, y] = rgba(FIRE[min(6, max(1, int(heat * 6.2)))])
            else:
                px[x, y] = rgba(FIRE[0] if (x + y) % 3 else "#3a1a12")
    for x in (5, 7, 9):                                       # grate bars
        for y in range(y0, y1 + 1):
            px[x, y] = rgba(STEEL[2] if on else STEEL[1])
            if y == y0:
                px[x, y] = rgba(STEEL[5])
    # accent stripe under the window
    for x in range(5, 11):
        px[x, 12] = rgba(FIRE[4] if on else FIRE[2])
    return img


def creative_cell():
    img = casing(PURPLE, [PURPLE[1], PURPLE[3], PURPLE[4], PURPLE[6], PURPLE[7], PURPLE[7]])
    px = img.load()
    bolt_shape = ["....###",
                  "...###.",
                  "..###..",
                  ".######",
                  "....##.",
                  "...##..",
                  "..##...",
                  ".#....."]
    for j, row in enumerate(bolt_shape):
        for i, ch in enumerate(row):
            if ch == "#":
                px[4 + i, 4 + j] = rgba("#fff6c4" if i <= 3 else "#ffd23f")
                if 4 + i + 1 < 16 and bolt_shape[j][i + 1:i + 2] != "#":
                    px[5 + i, 4 + j] = rgba("#b8860b") if px[5 + i, 4 + j][0] > 60 else px[5 + i, 4 + j]
    return img


# ------------------------------------------------------------------ screen
def gui():
    W, H = 176, 166
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    BG, LIGHT, DARK, EDGE = (198, 198, 198, 255), (255, 255, 255, 255), (85, 85, 85, 255), (0, 0, 0, 255)
    # panel with rounded black edge and vanilla-style bevel
    d.rectangle([1, 1, W - 2, H - 2], fill=BG)
    d.line([2, 0, W - 4, 0], fill=EDGE)
    d.line([0, 2, 0, H - 4], fill=EDGE)
    d.line([3, H - 1, W - 3, H - 1], fill=EDGE)
    d.line([W - 1, 3, W - 1, H - 3], fill=EDGE)
    for p in ((1, 1), (W - 3, 1), (W - 2, 2), (1, H - 3), (2, H - 2), (W - 2, H - 3), (W - 3, H - 2)):
        img.putpixel(p, EDGE)
    d.line([2, 1, W - 4, 1], fill=LIGHT)
    d.line([1, 2, 1, H - 4], fill=LIGHT)
    d.line([3, 2, W - 5, 2], fill=LIGHT)
    d.line([2, 3, 2, H - 5], fill=LIGHT)
    d.line([4, H - 2, W - 3, H - 2], fill=DARK)
    d.line([W - 2, 4, W - 2, H - 3], fill=DARK)
    d.line([4, H - 3, W - 3, H - 3], fill=DARK)
    d.line([W - 3, 4, W - 3, H - 4], fill=DARK)

    def slot(x, y, size=18):
        d.rectangle([x, y, x + size - 1, y + size - 1], fill=(139, 139, 139, 255))
        d.line([x, y, x + size - 2, y], fill=(55, 55, 55, 255))
        d.line([x, y, x, y + size - 2], fill=(55, 55, 55, 255))
        d.line([x + 1, y + size - 1, x + size - 1, y + size - 1], fill=LIGHT)
        d.line([x + size - 1, y + 1, x + size - 1, y + size - 1], fill=LIGHT)

    for row in range(3):
        for col in range(9):
            slot(7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(7 + col * 18, 141)
    # sprites
    slot(176, 0)                         # 18x18 slot
    slot(176, 18, 26)                    # 26x26 output slot
    # progress arrow, empty and full
    arrow = ["..........#...........", "..........##..........", "###########.#.........",
             "#..........#.#........", "#............#.......", "#.............#......",
             "#..............#.....", "#.............#......", "#............#.......",
             "#..........#.#........", "###########.#.........", "..........##..........",
             "..........#..........."]
    for k, color in enumerate(((139, 139, 139, 255), (255, 255, 255, 255))):
        oy = 44 + k * 17 + 2
        for j, row in enumerate(arrow):
            for i, ch in enumerate(row):
                inside = 0 < i < 16 and 2 < j < 10 or (i >= 11 and abs(j - 6) < (16 - i + 1) and i < 17)
                if ch == "#":
                    img.putpixel((176 + i, oy + j), (85, 85, 85, 255))
                elif inside and i < 17 and 2 <= j <= 10:
                    img.putpixel((176 + i, oy + j), color)
    # energy bar frame (16x50) and fill (14x48, red, lighter at the left)
    bx, by = 202, 0
    d.rectangle([bx, by, bx + 15, by + 49], fill=(139, 139, 139, 255))
    d.line([bx, by, bx + 14, by], fill=(55, 55, 55, 255))
    d.line([bx, by, bx, by + 48], fill=(55, 55, 55, 255))
    d.line([bx + 1, by + 49, bx + 15, by + 49], fill=LIGHT)
    d.line([bx + 15, by + 1, bx + 15, by + 49], fill=LIGHT)
    d.rectangle([bx + 1, by + 1, bx + 14, by + 48], fill=(43, 43, 43, 255))
    reds = [(255, 120, 98), (233, 66, 48), (200, 38, 26), (160, 24, 16)]
    for x in range(14):
        c = reds[0] if x < 3 else reds[1] if x < 8 else reds[2] if x < 12 else reds[3]
        for y in range(48):
            stripe = (y % 4 == 0)
            img.putpixel((bx + 17 + x, by + 1 + y), (*(tuple(max(0, v - 25) for v in c) if stripe else c), 255))
    return img


def main():
    os.makedirs(os.path.join(TEX, "block"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "gui"), exist_ok=True)
    out = {
        "block/machine_side": machine_side(),
        "block/machine_top": machine_top(),
        "block/machine_bottom": machine_bottom(),
        "block/electric_furnace_front": furnace_front(False),
        "block/electric_furnace_front_on": furnace_front(True),
        "block/creative_power_cell": creative_cell(),
        "gui/machine": gui(),
    }
    for name, img in out.items():
        img.save(os.path.join(TEX, name + ".png"))
    print(f"{len(out)} machine textures written")


if __name__ == "__main__":
    main()
