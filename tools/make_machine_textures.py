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


GREEN = ["#0f2a12", "#1d4d22", "#2e7a33", "#43a547", "#6bd06a", "#a8f29a", "#e2ffd8"]


def window(img, x0, y0, x1, y1):
    """A recessed window frame: dark top-left lip, light bottom-right lip."""
    px = img.load()
    for y in range(y0 - 1, y1 + 2):
        for x in range(x0 - 1, x1 + 2):
            if y in (y0 - 1, y1 + 1) or x in (x0 - 1, x1 + 1):
                px[x, y] = rgba(STEEL[1] if (y == y0 - 1 or x == x0 - 1) else STEEL[6])


def generator_front(on):
    """A round combustion chamber with a porthole: embers when idle, fire when burning."""
    img = casing()
    px = img.load()
    cx, cy = 7.5, 7.0
    for y in range(16):
        for x in range(16):
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 3.6 < r <= 4.6:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])      # rim
            elif r <= 3.6:
                if on:
                    heat = 1 - r / 3.6 * 0.6 + (y - cy) / 12
                    px[x, y] = rgba(FIRE[min(6, max(2, int(heat * 6.5)))])
                else:
                    px[x, y] = rgba(FIRE[1] if (x * 3 + y) % 5 == 0 else FIRE[0])
    for x in range(5, 11):                                        # vent slot under the chamber
        px[x, 13] = rgba(STEEL[0])
    return img


def battery_front(on):
    """Four charge bars in a window: dark when empty, glowing green when charged."""
    img = casing()
    px = img.load()
    window(img, 4, 3, 11, 12)
    for y in range(3, 13):
        for x in range(4, 12):
            px[x, y] = rgba("#14161a")
    for i, bx in enumerate((5, 7, 9)):
        for y in range(4, 12):
            lit = on
            c = GREEN[5] if (lit and y < 6) else GREEN[4] if lit else STEEL[1]
            if lit and x == bx:
                c = GREEN[6] if y == 4 else c
            px[bx, y] = rgba(c)
            px[bx + 1, y] = rgba(GREEN[3] if lit else STEEL[0])
    # terminals on top
    for x in (5, 10):
        px[x, 2] = rgba("#e8a03a")
    return img


def crusher_front(on):
    """Two toothed grinding wheels behind a window; brighter, with sparks, while running."""
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    import math
    for wx, spin in ((5.5, 1), (9.5, -1)):
        for y in range(4, 12):
            for x in range(3, 13):
                dx, dy = x - wx, y - 7.5
                r = math.hypot(dx, dy)
                ang = math.atan2(dy, dx) * spin + (0.4 if on else 0)
                tooth = math.cos(ang * 6) > 0.1
                if r <= (2.9 if tooth else 2.1):
                    lit = -(dx + dy) / max(r, 0.1)
                    c = STEEL[1] if r < 0.8 else STEEL[6] if lit > 0.5 else STEEL[4] if lit > -0.3 else STEEL[2]
                    px[x, y] = rgba(c)
    if on:
        for x, y in ((7, 5), (8, 10), (7, 9)):
            px[x, y] = rgba(FIRE[5])
    for x in range(5, 11):
        px[x, 13] = rgba(COPPER[3] if on else COPPER[1])
    return img


def energy_cable():
    """Braided bare copper, the tier 1 conductor."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            braid = (x + y) % 4 < 2
            other = (x - y) % 4 < 2
            c = COPPER[4] if braid and other else COPPER[3] if braid or other else COPPER[2]
            if (x + 2 * y) % 7 == 0:
                c = COPPER[5]
            px[x, y] = rgba(c)
    return img


# ------------------------------------------------------------------ pipes, tank, pump
LEAD = ["#13142f", "#232457", "#2f3061", "#393c61", "#454967", "#575e79", "#727989", "#7d97a6"]
TIN = ["#314f6f", "#476f81", "#517c88", "#6c8998", "#88a2a7", "#90afac", "#a1c6c2", "#bfd8dd"]
STEEL_TRIM = ["#22262e", "#3e4450", "#5f6774", "#939aa6", "#b8bec8", "#dde2e8"]          # tier 2 trim
WATER = ["#1d3f8a", "#2a58b8", "#3f76e4", "#6b98f0", "#a9c6fa"]
GLASS = ["#2b3a44", "#3d5361", "#5d7c8c", "#9fc4d2", "#e6f6fb"]


def pipe_texture(body, trim, window):
    """One pipe tile. Rows 0-5: the pipe's side, running along u (rim, metal, a window, metal, rim)
    with a tier-coloured ring at the joint. The 6x6 square at (0, 6): the end cap seen on the core."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(16):
        ring = x in (0, 1)
        for y in range(6):
            if ring:
                c = trim[1] if y in (0, 5) else trim[4] if y == 1 else trim[3]
            elif y == 0:
                c = body[2]
            elif y == 5:
                c = body[0]
            elif y == 1:
                c = body[6] if x % 5 == 3 else body[5]
            elif y == 4:
                c = body[3]
            else:
                # the window: what the pipe carries shows through, with a glint on the top row
                c = window[4] if (y == 2 and x % 6 == 4) else window[3] if y == 2 else window[1]
            px[x, y] = rgba(c)
    # end cap: tier ring around a metal face with a dark bore
    for y in range(6):
        for x in range(6):
            edge = x in (0, 5) or y in (0, 5)
            if edge:
                c = trim[4] if (x == 0 or y == 0) and not (x == 5 or y == 5) else trim[1]
            elif x in (2, 3) and y in (2, 3):
                c = window[0] if (x, y) != (2, 2) else window[2]
            else:
                c = body[5] if (x + y) < 5 else body[3]
            px[x, 6 + y] = rgba(c)
    return img


def pipe_attachment():
    """Plates where a pipe meets a block. (0,0) 8x8: the push plate (steel, orange output dot);
    (8,0) 8x8: the pull servo (copper ring, blue input core); below each, its edge."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(8):
        for x in range(8):
            edge = x in (0, 7) or y in (0, 7)
            lit = x + y < 7
            c = (STEEL[6] if lit else STEEL[2]) if edge else (STEEL[5] if lit else STEEL[4])
            if 3 <= x <= 4 and 3 <= y <= 4:
                c = COPPER[4] if (x, y) == (3, 3) else COPPER[3]
            if (x, y) in ((1, 1), (6, 1), (1, 6), (6, 6)):
                c = STEEL[1]
            px[x, y] = rgba(c)
            # servo
            r = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** 0.5
            if r > 3.3:
                c = COPPER[1] if not lit else COPPER[3]
            elif r > 2.2:
                c = COPPER[4] if lit else COPPER[2]
            elif r > 1.2:
                c = STEEL[1]
            else:
                c = WATER[3] if lit else WATER[2]
            px[8 + x, y] = rgba(c)
    for y in range(8, 16):
        for x in range(8):
            px[x, y] = rgba(STEEL[3] if (y % 2) else STEEL[4])
            px[8 + x, y] = rgba(COPPER[2] if (y % 2) else COPPER[3])
    return img


def tank_side():
    """A glass panel in a frame: tier-coloured corners, clear glass with streaks (cut out so the fluid shows)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d == 0:
                c = STEEL[5] if (x == 0 or y == 0) and x != 15 and y != 15 else STEEL[1]
            elif d == 1:
                c = STEEL[4] if (x == 1 or y == 1) and x < 14 and y < 14 else STEEL[2]
            else:
                c = None
                if (x - y) in (5, 6) and 3 <= x <= 12:
                    c = GLASS[4] if (x - y) == 5 else GLASS[3]
                elif (x - y) == -3 and 3 <= x <= 9:
                    c = GLASS[3]
            if c:
                px[x, y] = rgba(c)
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[cx, cy] = rgba(COPPER[4])
    for x in range(5, 11):
        px[x, 0] = rgba(COPPER[3])
        px[x, 15] = rgba(COPPER[1])
    return img


def tank_top():
    img = tank_side()
    px = img.load()
    for x in range(4, 12):                 # a filler cap across the top
        for y in (7, 8):
            px[x, y] = rgba(STEEL[5] if y == 7 else STEEL[2])
    return img


def pump_front(on):
    """An impeller behind a round grille; spinning blades and blue water when running."""
    import math
    img = casing()
    px = img.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if 4.4 < r <= 5.4:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])
            elif r <= 4.4:
                ang = math.atan2(dy, dx) + (0.5 if on else 0)
                blade = math.cos(ang * 4 + r * 0.6) > 0.35
                if r < 1.2:
                    c = STEEL[6]
                elif blade:
                    c = STEEL[5] if dx + dy < 0 else STEEL[3]
                else:
                    c = (WATER[2] if r < 3 else WATER[1]) if on else "#16181c"
                px[x, y] = rgba(c)
    for x in range(5, 11):
        px[x, 14] = rgba(WATER[3] if on else WATER[0])
    return img


def wrench_item():
    """A steel open-ended wrench lying corner to corner: a jawed head top-right, a copper grip bottom-left."""
    import math
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    fill = {}
    gap = set()
    hx, hy = 11.0, 4.5
    for y in range(16):
        for x in range(16):
            # head: a disc with a jaw cut along the handle's line, opening to the top-right
            dx, dy = x - hx, y - hy
            r = math.hypot(dx, dy)
            along = (dx - dy) / math.sqrt(2)          # toward the top-right
            across = (dx + dy) / math.sqrt(2)          # negative: the upper-left side
            jaw = along > 0.0 and abs(across) < 1.0
            if r <= 3.4 and jaw:
                gap.add((x, y))
            elif r <= 3.1:
                fill[(x, y)] = STEEL[6] if across < -1.4 else STEEL[3] if across > 1.4 else STEEL[5]
            # shaft: a two-pixel diagonal from the head down to the grip
            if abs(x + y - 15) <= 0.5 or abs(x + y - 16) <= 0.5:
                if 2 <= x <= 9:
                    grip = x <= 5
                    upper = (x + y) == 15
                    fill[(x, y)] = (COPPER[4] if upper else COPPER[2]) if grip else (STEEL[5] if upper else STEEL[3])
    for (x, y), c in fill.items():
        px[x, y] = rgba(c)
    # dark outline around the shape
    for y in range(16):
        for x in range(16):
            if (x, y) in fill or (x, y) in gap:
                continue
            if any((x + ox, y + oy) in fill for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = rgba("#1a1c20")
    return img


def filter_card_item():
    """A small circuit card: green board, copper contacts, and a white funnel on it."""
    board = ["#0f2a12", "#1d4d22", "#2e7a33", "#43a547", "#6bd06a"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 14):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (2, 13)
            c = board[0] if edge else board[3] if (x + y) < 12 else board[2]
            px[x, y] = rgba(c)
    for x in range(3, 13, 2):                 # contacts along the bottom edge
        px[x, 12] = rgba(COPPER[4])
        px[x, 13] = rgba(COPPER[2])
    funnel = ["#######", ".#####.", "..###..", "...#...", "...#..."]
    for j, row in enumerate(funnel):
        for i, ch in enumerate(row):
            if ch == "#":
                px[4 + i, 4 + j] = rgba("#f2f6f2" if j < 2 else "#cfd8cf")
    return img


def gauge_marks(img):
    """Tick marks drawn over a fluid gauge (14x48 at 176,96 in the machine screen sheet)."""
    for y in range(0, 48):
        if y % 6 == 5:
            w = 5 if y % 12 == 11 else 3
            for x in range(14 - w, 14):
                img.putpixel((176 + x, 96 + y), (40, 40, 40, 200))


def filter_gui():
    """The Filter Card screen: the shared panel, a 3x3 grid, a toggle button and its two icons."""
    img = gui_panel()
    d = ImageDraw.Draw(img)
    for row in range(3):
        for col in range(3):
            gui_slot(d, 43 + col * 18, 16 + row * 18)
    # button, normal and hovered (20x20)
    for k, (face, hi, lo) in enumerate((((198, 198, 198), (255, 255, 255), (85, 85, 85)),
                                         ((170, 190, 230), (230, 240, 255), (60, 70, 110)))):
        bx = 176 + k * 20
        d.rectangle([bx, 0, bx + 19, 19], fill=(0, 0, 0, 255))
        d.rectangle([bx + 1, 1, bx + 18, 18], fill=(*face, 255))
        d.line([bx + 1, 1, bx + 17, 1], fill=(*hi, 255))
        d.line([bx + 1, 1, bx + 1, 17], fill=(*hi, 255))
        d.line([bx + 2, 18, bx + 18, 18], fill=(*lo, 255))
        d.line([bx + 18, 2, bx + 18, 18], fill=(*lo, 255))
    # icons (16x16): whitelist = white sheet with a green tick; blacklist = dark sheet with a red cross
    for k, (sheet, mark) in enumerate((((240, 240, 240), (60, 170, 70)), ((60, 60, 66), (220, 60, 50)))):
        ix = 176 + k * 16
        d.rectangle([ix + 3, 22, ix + 12, 33], fill=(*sheet, 255), outline=(30, 30, 30, 255))
        for x in range(ix + 5, ix + 11):
            for y in (25, 27):
                img.putpixel((x, y), (150, 150, 150, 255) if k == 0 else (110, 110, 118, 255))
        if k == 0:
            for i, (x, y) in enumerate(((5, 30), (6, 31), (7, 32), (8, 31), (9, 30), (10, 29), (11, 28), (12, 27))):
                img.putpixel((ix + x, y), (*mark, 255))
                img.putpixel((ix + x, y + 1), (*mark, 255))
        else:
            for i in range(6):
                img.putpixel((ix + 6 + i, 27 + i), (*mark, 255))
                img.putpixel((ix + 11 - i, 27 + i), (*mark, 255))
                img.putpixel((ix + 7 + i, 27 + i), (*mark, 255))
                img.putpixel((ix + 12 - i, 27 + i), (*mark, 255))
    return img


# ------------------------------------------------------------------ tier 1 processing machines
WOOD = ["#3b2410", "#5a3a1c", "#7a5428", "#9a6e38", "#b88a4c", "#d0a564", "#e4c185", "#f2dcaa"]


def alloy_smelter_front(on):
    """Two crucibles side by side under a hood; molten metal glows in them while running."""
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for cx in (5, 10):
        for y in range(7, 12):
            half = 2 if y < 11 else 1
            for x in range(cx - half, cx + half + 1):
                rim = y == 7 or abs(x - cx) == half
                if rim:
                    px[x, y] = rgba(STEEL[5] if x < cx else STEEL[3])
                else:
                    px[x, y] = rgba((FIRE[5] if y == 8 else FIRE[4]) if on else FIRE[1])
        if on:
            px[cx, 6] = rgba(FIRE[3])
    for x in range(4, 12):                       # hood
        px[x, 5] = rgba(STEEL[2])
    for x in range(5, 11):
        px[x, 13] = rgba(FIRE[4] if on else FIRE[2])
    return img


def compressor_front(on):
    """A press: a heavy head on two rails over an anvil plate; the head is down while running."""
    img = casing()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for y in range(3, 13):                       # rails
        px[4, y] = rgba(STEEL[4])
        px[11, y] = rgba(STEEL[2])
    top = 7 if on else 4
    for y in range(top, top + 3):                # head
        for x in range(5, 11):
            px[x, y] = rgba(STEEL[6] if y == top else STEEL[4] if y == top + 1 else STEEL[2])
    for x in range(5, 11):                        # plate on the anvil
        px[x, 11] = rgba(COPPER[3] if on else COPPER[2])
        px[x, 12] = rgba(STEEL[1])
    if on:
        for x in (5, 10):
            px[x, 10] = rgba(COPPER[5])
    return img


def wire_drawer_front(on):
    """A wire spool beside a die: copper wire runs from the die onto the spool."""
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for y in range(5, 11):                        # spool, wound with copper
        for x in range(4, 9):
            flange = x in (4, 8)
            px[x, y] = rgba(STEEL[4] if flange else (COPPER[4] if (y + (1 if on else 0)) % 2 else COPPER[2]))
    for y in (6, 7, 8, 9):                         # die block
        for x in (10, 11):
            px[x, y] = rgba(STEEL[5] if y == 6 else STEEL[3])
    px[10, 7] = rgba("#0a0a0c")
    for x in (9,):                                 # wire between them
        px[x, 7] = rgba(COPPER[5] if on else COPPER[3])
    for x in range(5, 11):
        px[x, 13] = rgba(COPPER[3] if on else COPPER[1])
    return img


def sawmill_front(on):
    """A toothed saw blade in a dark window under a copper guard; it blurs into a ring while running."""
    import math
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 12)
    for y in range(4, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    cx, cy = 7.5, 8.5
    for y in range(4, 13):
        for x in range(3, 13):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            if on:
                if 2.8 < r <= 4.3:
                    px[x, y] = rgba(STEEL[6] if (x + y) % 2 else STEEL[4])
                elif 1.0 < r <= 2.8:
                    px[x, y] = rgba(STEEL[3])
                elif r <= 1.0:
                    px[x, y] = rgba(COPPER[3])
                continue
            tooth = math.cos(ang * 6) > 0.2
            if r <= (4.3 if tooth else 3.3):
                if r <= 1.0:
                    c = COPPER[3]
                elif r > 3.3:
                    c = STEEL[6]
                else:
                    c = STEEL[5] if dx + dy < -0.5 else STEEL[4] if dx + dy < 1.5 else STEEL[3]
                px[x, y] = rgba(c)
    for x in range(3, 13):                         # guard across the top
        px[x, 3] = rgba(COPPER[4])
        px[x, 4] = rgba(COPPER[2])
    if on:
        for x, y in ((11, 12), (12, 11), (4, 12)):
            px[x, y] = rgba(WOOD[6])
    return img


def upgrade_card(board, icon, icon_color):
    """An upgrade card: the same card as the Filter Card, in its own colour, with its own symbol."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 14):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (2, 13)
            px[x, y] = rgba(board[0] if edge else board[3] if (x + y) < 12 else board[2])
    for x in range(3, 13, 2):
        px[x, 12] = rgba(COPPER[4])
        px[x, 13] = rgba(COPPER[2])
    for j, row in enumerate(icon):
        for i, ch in enumerate(row):
            if ch == "#":
                px[4 + i, 4 + j] = rgba(icon_color)
    return img


UPGRADE_ART = {
    "speed": (["#3a0d0d", "#6b1818", "#a82a2a", "#d04040"],
              ["#..#...", ".#..#..", "..#..#.", ".#..#..", "#..#..."], "#ffe9a8"),
    "efficiency": (["#0d2a3a", "#184d6b", "#2a78a8", "#3f9ad0"],
                   ["....##.", "...##..", "..####.", "...##..", "..##..."], "#fff4a0"),
    "bonus": (["#3a2a0a", "#6b4f14", "#a87f22", "#d0a634"],
              ["...#...", "...#...", "#######", "...#...", "...#..."], "#ffffff"),
    "eject": (["#1a1a3a", "#2a2a6b", "#3f3fa8", "#5a5ad0"],
              ["...#...", "....#..", "#######", "....#..", "...#..."], "#ffffff"),
    "range": (["#0f2a12", "#1d4d22", "#2e7a33", "#43a547"],
              ["#.#.#.#", ".......", "#.###.#", ".......", "#.#.#.#"], "#e8ffd8"),
}


def sawdust_item():
    """A pile of sawdust, drawn the same way as the metal dusts but in wood tones."""
    import importlib.util
    spec = importlib.util.spec_from_file_location("mt", os.path.join(os.path.dirname(os.path.abspath(__file__)), "make_textures.py"))
    mt = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mt)
    return mt.paint_ramp(mt.dust(), WOOD)


# ------------------------------------------------------------------ tier 2 machines (steel trim)
def casing2():
    return casing(STEEL, STEEL_TRIM)


def ore_washer_front(on):
    """Spray nozzles over a grate; water falls in streaks while running."""
    img = casing2()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for x in range(4, 12):                               # spray bar
        px[x, 4] = rgba(STEEL[5])
    for x in (5, 8, 11):
        px[x, 5] = rgba(STEEL[3])
        if on:
            for y in range(6, 10):
                if (y + x) % 2:
                    px[x, y] = rgba(WATER[3] if y < 8 else WATER[2])
    for x in range(3, 13):                               # grate
        px[x, 10] = rgba(STEEL[4] if x % 2 else STEEL[2])
        px[x, 11] = rgba(WATER[1] if on else STEEL[1])
    for x in range(5, 11):
        px[x, 13] = rgba(WATER[3] if on else WATER[0])
    return img


def fluid_mixer_front(on):
    """A glass vat with a paddle; the liquid swirls green while running."""
    img = casing2()
    px = img.load()
    window(img, 4, 3, 11, 12)
    for y in range(3, 13):
        for x in range(4, 12):
            if y < 6:
                px[x, y] = rgba("#16181c")
            else:
                px[x, y] = rgba(GREEN[3] if on and (x + y) % 3 == 0 else GREEN[2] if on else "#1d2a20")
    for y in range(3, 11):                               # shaft
        px[7, y] = rgba(STEEL[5])
    for x in range(5, 10) if on else range(6, 9):       # paddle (wider when turning)
        px[x, 10] = rgba(STEEL[4])
    for x in range(4, 12):
        px[x, 6] = rgba(GREEN[4] if on else "#2c3a30")    # surface
    return img


def centrifuge_front(on):
    """A drum with four sample tubes; it blurs while spinning."""
    import math
    img = casing2()
    px = img.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if 4.4 < r <= 5.4:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])
            elif r <= 4.4:
                px[x, y] = rgba("#16181c")
    tubes = [(7.5, 4.5), (10.5, 7.5), (7.5, 10.5), (4.5, 7.5)] if not on else [(9.6, 5.4), (9.6, 9.6), (5.4, 9.6), (5.4, 5.4)]
    colors = [PURPLE[5], WATER[3], GREEN[4], FIRE[4]]
    for (tx, ty), c in zip(tubes, colors):
        px[int(tx), int(ty)] = rgba(c)
        px[int(tx) + (1 if tx > cx else -1 if tx < cx else 0), int(ty) + (1 if ty > cy else -1 if ty < cy else 0)] = rgba(STEEL[4])
    px[7, 7] = rgba(STEEL[5])
    px[8, 8] = rgba(STEEL[3])
    if on:
        for a in range(0, 360, 30):
            x = int(round(cx + 3.6 * math.cos(math.radians(a))))
            y = int(round(cy + 3.6 * math.sin(math.radians(a))))
            if px[x, y][:3] == rgba("#16181c")[:3]:
                px[x, y] = rgba(STEEL[2])
    return img


def circuit_assembler_front(on):
    """A green board under a soldering arm; the tip glows while running."""
    img = casing2()
    px = img.load()
    window(img, 3, 4, 12, 11)
    board = ["#0f2a12", "#1d4d22", "#2e7a33", "#43a547"]
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba(board[2] if y >= 8 else "#16181c")
    for x in range(4, 12, 2):                              # traces on the board
        px[x, 9] = rgba(COPPER[3])
    px[6, 10] = rgba("#121212")
    px[7, 10] = rgba("#121212")
    for x in range(4, 10):                                 # arm
        px[x, 5] = rgba(STEEL[5])
    px[9, 6] = rgba(STEEL[4])
    px[9, 7] = rgba(FIRE[5] if on else STEEL[2])
    if on:
        px[10, 8] = rgba(FIRE[6])
    return img


def basic_circuit_item():
    """A small green board with copper traces and one black chip."""
    board = ["#0f2a12", "#1d4d22", "#2e7a33", "#43a547", "#6bd06a"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(3, 13):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (3, 12)
            px[x, y] = rgba(board[0] if edge else board[3] if (x + y) < 13 else board[2])
    for x in range(3, 13):
        if x not in (6, 7, 8, 9):
            px[x, 7] = rgba(COPPER[3])
    for y in (4, 5, 9, 10, 11):
        px[4, y] = rgba(COPPER[3])
        px[11, y] = rgba(COPPER[2])
    for y in range(5, 10):                                 # chip
        for x in range(6, 10):
            px[x, y] = rgba("#222226" if (x, y) != (6, 5) else "#55555c")
    for y in (5, 7, 9):
        px[5, y] = rgba(STEEL[6])
        px[10, y] = rgba(STEEL[6])
    return img


# ------------------------------------------------------------------ farming
BARK = ["#2a2018", "#3d3024", "#524233", "#6a5844", "#82705a", "#9c8a72"]
RUBBER_WOOD = ["#5c4630", "#7a6044", "#967a58", "#b0946c", "#c8ae84", "#dcc59c"]
LEAF = ["#16351a", "#1f4a24", "#2c6430", "#3b7f3c", "#4f9a48", "#6cb85a", "#8fd070"]
AMBER = ["#5a3608", "#8a5410", "#c27d1a", "#e8a838", "#f8d070"]


def rubber_log_side(spot=None):
    """Pale grey-brown bark in long vertical strips; with a tap spot cut in the middle, full of amber
    latex ("wet") or empty ("dry")."""
    import random
    r = random.Random(41)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    cols = [r.choice([2, 3, 3, 4]) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            v = cols[x]
            if (x * 7 + y * 3) % 11 == 0:
                v -= 1
            if x % 4 == 0:
                v = 1
            if r.random() < 0.08:
                v += 1
            px[x, y] = rgba(BARK[max(0, min(5, v))])
    if spot:
        for y in range(6, 11):
            for x in range(6, 10):
                edge = x in (6, 9) or y in (6, 10)
                if edge:
                    px[x, y] = rgba(BARK[0] if (x == 6 or y == 6) else BARK[4])
                elif spot == "wet":
                    px[x, y] = rgba(AMBER[4] if (x, y) == (7, 7) else AMBER[3] if y < 9 else AMBER[2])
                else:
                    px[x, y] = rgba("#1a130c")
        if spot == "wet":
            px[8, 11] = rgba(AMBER[3])                  # a drip running down
            px[8, 12] = rgba(AMBER[2])
    return img


def rubber_log_top():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                c = BARK[2]
            elif d > 5.5:
                c = BARK[4]
            else:
                ring = int(d) % 2
                c = RUBBER_WOOD[3] if ring else RUBBER_WOOD[2]
                if d < 1:
                    c = AMBER[2]                        # a little latex in the heart
            px[x, y] = rgba(c)
    return img


def rubber_leaves():
    import random
    r = random.Random(7)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = r.random()
            if n < 0.12:
                continue                                  # gaps, like vanilla leaves
            v = 2 + int(n * 4) - (1 if (x + y) % 5 == 0 else 0)
            px[x, y] = rgba(LEAF[max(0, min(6, v))])
    return img


def rubber_sapling():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(9, 16):                                # stem
        px[7, y] = rgba(BARK[3])
        px[8, y] = rgba(BARK[2])
    leaves = ["....##....", "...####...", "..######..", ".########.", "..######..", "...####...", "..##..##.."]
    for j, row in enumerate(leaves):
        for i, ch in enumerate(row):
            if ch == "#":
                px[3 + i, 3 + j] = rgba(LEAF[5] if i + j < 7 else LEAF[3])
    return img


def rubber_planks():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            board = y // 4
            seam = y % 4 == 3 or (x == (5 + board * 7) % 16)
            c = RUBBER_WOOD[1] if seam else RUBBER_WOOD[3] if (x + board) % 6 else RUBBER_WOOD[2]
            if y % 4 == 0 and not seam:
                c = RUBBER_WOOD[4]
            px[x, y] = rgba(c)
    return img


def latex_item():
    """A sticky amber blob."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    import math
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - 7.5) / 5.0, (y - 8.5) / 4.2)
            if d <= 1:
                lit = -(x - 7.5) - (y - 8.5)
                c = AMBER[4] if lit > 4 else AMBER[3] if lit > -1 else AMBER[2] if d < 0.85 else AMBER[1]
                px[x, y] = rgba(c)
    px[5, 6] = rgba("#fff4d0")
    return img


def rubber_item():
    """A dark rubber bar."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    dark = ["#141414", "#1f1f21", "#2c2c30", "#3c3c42", "#55555c"]
    for y in range(5, 12):
        for x in range(2, 14):
            edge_b = y == 11 or x == 13
            edge_t = y == 5 or x == 2
            px[x, y] = rgba(dark[0] if edge_b else dark[4] if edge_t else dark[2] if (x + y) % 5 else dark[3])
    return img


def tree_tap_item():
    """A wooden spout with an iron tip."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shape = ["............###.",
             "...........#ii#.",
             "..........#ii#..",
             ".........#ww#...",
             "........#ww#....",
             ".......#ww#.....",
             "..####.#w#......",
             ".#wwww#ww#......",
             ".#wwwwwww#......",
             ".#wwwwww#.......",
             "..#wwww#........",
             "...####.........",
             ".....#a#........",
             "......a.........", "", ""]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == "#":
                px[x, y] = rgba("#2a1c10")
            elif ch == "w":
                px[x, y] = rgba(RUBBER_WOOD[3] if (x + y) % 3 else RUBBER_WOOD[2])
            elif ch == "i":
                px[x, y] = rgba(STEEL[5])
            elif ch == "a":
                px[x, y] = rgba(AMBER[3])
    return img


def planter_front(on):
    """Seed rows over soil, with a sprout; the seed chute glows when planting."""
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#4a3020" if y >= 9 else "#16181c")
    for x in range(3, 13):
        px[x, 9] = rgba("#6a4630")
    for x in (5, 8, 11):
        px[x, 8] = rgba(LEAF[5])
        px[x, 7] = rgba(LEAF[4] if on else LEAF[2])
    for x in range(4, 12):
        px[x, 4] = rgba(STEEL[4])
    px[8, 5] = rgba(COPPER[4] if on else STEEL[2])
    return img


def harvester_front(on):
    """A cutter bar with blades over ripe wheat."""
    img = casing()
    px = img.load()
    window(img, 3, 4, 12, 11)
    for y in range(4, 12):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for x in range(3, 13):
        top = 7 if x % 2 else 8
        for y in range(top, 12):
            px[x, y] = rgba("#d8b04a" if y == top else "#b08a30")
    for x in range(3, 13):                                  # cutter bar
        px[x, 5] = rgba(STEEL[5])
        if (x + (1 if on else 0)) % 2:
            px[x, 6] = rgba(STEEL[6])
    return img


def tree_farm_front(on):
    """A small tree in a window beside a saw arm."""
    img = casing2()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for y in range(8, 13):
        px[7, y] = rgba(BARK[3])
    for y in range(3, 9):
        for x in range(4, 11):
            if abs(x - 7) + abs(y - 5.5) < 4:
                px[x, y] = rgba(LEAF[4] if (x + y) % 3 else LEAF[2])
    for y in range(9, 12):                                   # saw arm
        px[11, y] = rgba(STEEL[5] if on else STEEL[3])
    px[10, 10] = rgba(STEEL[6] if on else STEEL[4])
    return img


def bio_reactor_front(on):
    """A glass digester full of green mash that bubbles while running."""
    img = casing2()
    px = img.load()
    window(img, 4, 3, 11, 12)
    for y in range(3, 13):
        for x in range(4, 12):
            px[x, y] = rgba("#16181c" if y < 5 else (GREEN[3] if on and (x * 3 + y) % 5 == 0 else GREEN[2] if on else "#1e2c1a"))
    if on:
        for x, y in ((6, 7), (9, 9), (7, 11)):
            px[x, y] = rgba(GREEN[5])
    for x in range(4, 12):
        px[x, 5] = rgba(GREEN[4] if on else "#2a3a24")
    return img


def biofuel_generator_front(on):
    """A green-flamed burner behind a round window."""
    import math
    img = casing2()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.0)
            if 3.6 < r <= 4.6:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])
            elif r <= 3.6:
                if on:
                    heat = 1 - r / 3.6 * 0.6 + (y - 7) / 12
                    px[x, y] = rgba(GREEN[min(6, max(2, int(heat * 6.5)))])
                else:
                    px[x, y] = rgba(GREEN[1] if (x * 3 + y) % 5 == 0 else GREEN[0])
    for x in range(5, 11):
        px[x, 13] = rgba(STEEL[0])
    return img


# ------------------------------------------------------------------ screen
LIGHT = (255, 255, 255, 255)


def gui_slot(d, x, y, size=18):
    d.rectangle([x, y, x + size - 1, y + size - 1], fill=(139, 139, 139, 255))
    d.line([x, y, x + size - 2, y], fill=(55, 55, 55, 255))
    d.line([x, y, x, y + size - 2], fill=(55, 55, 55, 255))
    d.line([x + 1, y + size - 1, x + size - 1, y + size - 1], fill=LIGHT)
    d.line([x + size - 1, y + 1, x + size - 1, y + size - 1], fill=LIGHT)


def gui_panel():
    """The vanilla-style 176x166 panel with the player's inventory slots, on a 256x256 sheet."""
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
    for row in range(3):
        for col in range(9):
            gui_slot(d, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        gui_slot(d, 7 + col * 18, 141)
    return img


def gui():
    img = gui_panel()
    d = ImageDraw.Draw(img)

    def slot(x, y, size=18):
        gui_slot(d, x, y, size)
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
    # flame, unlit and lit (14x14 each)
    flame = ["......##......", ".....####.....", "....######....", "...#######....", "...########...",
             "..##########..", "..##########..", ".############.", ".############.", "##############",
             "##############", ".############.", "..##########..", "...########..."]
    fire = [(255, 236, 140), (255, 196, 64), (247, 130, 30), (214, 70, 20)]
    for j, row in enumerate(flame):
        for i, ch in enumerate(row):
            if ch == "#":
                img.putpixel((176 + i, 78 + j), (120, 120, 120, 255))
                c = fire[0] if j > 9 and 4 <= i <= 9 else fire[1] if j > 6 else fire[2] if j > 3 else fire[3]
                img.putpixel((190 + i, 78 + j), (*c, 255))
    gauge_marks(img)
    return img


def main():
    os.makedirs(os.path.join(TEX, "block"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "gui"), exist_ok=True)
    os.makedirs(os.path.join(TEX, "item"), exist_ok=True)
    out = {
        "block/machine_side": machine_side(),
        "block/machine_top": machine_top(),
        "block/machine_bottom": machine_bottom(),
        "block/electric_furnace_front": furnace_front(False),
        "block/electric_furnace_front_on": furnace_front(True),
        "block/creative_power_cell": creative_cell(),
        "block/combustion_generator_front": generator_front(False),
        "block/combustion_generator_front_on": generator_front(True),
        "block/basic_battery_front": battery_front(False),
        "block/basic_battery_front_on": battery_front(True),
        "block/crusher_front": crusher_front(False),
        "block/crusher_front_on": crusher_front(True),
        "block/energy_cable": energy_cable(),
        "block/item_duct": pipe_texture(LEAD, COPPER, ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]),
        "block/reinforced_item_duct": pipe_texture(LEAD, STEEL_TRIM, ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]),
        "block/fluid_pipe": pipe_texture(TIN, COPPER, GLASS),
        "block/reinforced_fluid_pipe": pipe_texture(TIN, STEEL_TRIM, GLASS),
        "block/pipe_attachment": pipe_attachment(),
        "block/fluid_tank_side": tank_side(),
        "block/fluid_tank_top": tank_top(),
        "block/pump_front": pump_front(False),
        "block/pump_front_on": pump_front(True),
        "item/wrench": wrench_item(),
        "item/filter_card": filter_card_item(),
        "gui/filter": filter_gui(),
        "block/alloy_smelter_front": alloy_smelter_front(False),
        "block/alloy_smelter_front_on": alloy_smelter_front(True),
        "block/compressor_front": compressor_front(False),
        "block/compressor_front_on": compressor_front(True),
        "block/wire_drawer_front": wire_drawer_front(False),
        "block/wire_drawer_front_on": wire_drawer_front(True),
        "block/sawmill_front": sawmill_front(False),
        "block/sawmill_front_on": sawmill_front(True),
        "item/sawdust": sawdust_item(),
        "item/basic_circuit": basic_circuit_item(),
        "block/rubber_log": rubber_log_side(),
        "block/rubber_log_dry": rubber_log_side("dry"),
        "block/rubber_log_wet": rubber_log_side("wet"),
        "block/rubber_log_top": rubber_log_top(),
        "block/rubber_leaves": rubber_leaves(),
        "block/rubber_sapling": rubber_sapling(),
        "block/rubber_planks": rubber_planks(),
        "item/latex": latex_item(),
        "item/rubber": rubber_item(),
        "item/tree_tap": tree_tap_item(),
        **{f"block/{name}_front{'_on' if on else ''}": fn(on)
           for name, fn in (("planter", planter_front), ("harvester", harvester_front), ("tree_farm", tree_farm_front),
                            ("bio_reactor", bio_reactor_front), ("biofuel_generator", biofuel_generator_front))
           for on in (False, True)},
        "block/ore_washer_front": ore_washer_front(False),
        "block/ore_washer_front_on": ore_washer_front(True),
        "block/fluid_mixer_front": fluid_mixer_front(False),
        "block/fluid_mixer_front_on": fluid_mixer_front(True),
        "block/centrifuge_front": centrifuge_front(False),
        "block/centrifuge_front_on": centrifuge_front(True),
        "block/circuit_assembler_front": circuit_assembler_front(False),
        "block/circuit_assembler_front_on": circuit_assembler_front(True),
        **{f"item/{u}_upgrade": upgrade_card(*art) for u, art in UPGRADE_ART.items()},
        "gui/machine": gui(),
    }
    for name, img in out.items():
        img.save(os.path.join(TEX, name + ".png"))
    print(f"{len(out)} machine textures written")


if __name__ == "__main__":
    main()
