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


def battery_front(on, trim=None):
    """Four charge bars in a window: dark when empty, glowing green when charged."""
    img = casing(STEEL, trim or COPPER)
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


def tank_side(trim=None):
    """A glass panel in a frame: tier-coloured corners, clear glass with streaks (cut out so the fluid shows)."""
    trim = trim or COPPER
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
        px[cx, cy] = rgba(trim[4])
    for x in range(5, 11):
        px[x, 0] = rgba(trim[3])
        px[x, 15] = rgba(trim[1])
    return img


def tank_top(trim=None):
    img = tank_side(trim)
    px = img.load()
    for x in range(4, 12):                 # a filler cap across the top
        for y in (7, 8):
            px[x, y] = rgba(STEEL[5] if y == 7 else STEEL[2])
    return img


def pump_front(on, base=None):
    """An impeller behind a round grille; spinning blades and blue water when running."""
    import math
    img = base() if base else casing()
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


BARK = ["#2a2018", "#3d3024", "#524233", "#6a5844", "#82705a", "#9c8a72"]
RUBBER_WOOD = ["#5c4630", "#7a6044", "#967a58", "#b0946c", "#c8ae84", "#dcc59c"]
LEAF = ["#16351a", "#1f4a24", "#2c6430", "#3b7f3c", "#4f9a48", "#6cb85a", "#8fd070"]
AMBER = ["#5a3608", "#8a5410", "#c27d1a", "#e8a838", "#f8d070"]


RUBBER_BARK = ["#2b231c", "#3a3027", "#4a3f33", "#5b4f41", "#6c5f4f", "#7e705e", "#91836f", "#a69882"]


def rubber_log_side(spot=None):
    """Rubber tree bark: smooth, pale grey-brown, in long vertical streaks of many tones, with darker
    furrows and the short horizontal pores (lenticels) rubber trees have. Tiles top to bottom. With a tap
    spot cut in the middle: full of amber latex ("wet") or empty ("dry")."""
    import random
    r = random.Random(41)
    lv = [[4] * 16 for _ in range(16)]
    # each column has its own base tone, smoothed so neighbours belong together
    base = [r.choice([3, 4, 4, 5, 5, 6]) for _ in range(16)]
    base = [round((base[x - 1] + 2 * base[x] + base[(x + 1) % 16]) / 4) for x in range(16)]
    for x in range(16):
        for y in range(16):
            lv[y][x] = base[x]
    # vertical strokes: lighter ridges and darker furrows of varying length (wrapping so it tiles)
    for _ in range(26):
        x, y0, n = r.randrange(16), r.randrange(16), r.randint(2, 7)
        d = r.choice([-2, -1, -1, 1, 1, 2])
        for i in range(n):
            y = (y0 + i) % 16
            lv[y][x] = max(1, min(7, lv[y][x] + d))
    # two deep furrows running most of the length
    for x0 in (3, 11):
        for y in range(16):
            x = x0 + (1 if (y // 5) % 2 else 0)
            lv[y][x] = 1 if y % 7 else 2
            lv[y][(x + 1) % 16] = max(lv[y][(x + 1) % 16], 5)          # its lit edge
    # lenticels: short dark dashes with a light lip under them
    for _ in range(7):
        x, y = r.randrange(14), r.randrange(15)
        for i in range(r.choice([2, 3])):
            lv[y][x + i] = 0
            lv[(y + 1) % 16][x + i] = min(7, lv[(y + 1) % 16][x + i] + 2)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgba(RUBBER_BARK[lv[y][x]])
    if spot:
        # a V-shaped tapping cut with a little spout under it
        cut = [(6, 6), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8)]
        for x, y in cut:
            if spot == "wet":
                px[x, y] = rgba(AMBER[4] if (x, y) in ((7, 6), (6, 6)) else AMBER[3] if y < 8 else AMBER[2])
            else:
                px[x, y] = rgba("#16100a" if y < 8 else "#24190f")
        for x, y in ((5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5)):
            px[x, y] = rgba(RUBBER_BARK[1])                       # the cut's upper lip in shadow
        for x, y in ((5, 6), (10, 6), (6, 8), (9, 8), (7, 9), (8, 9)):
            px[x, y] = rgba(RUBBER_BARK[7])                       # its lower edge catching light
        if spot == "wet":
            for y, c in ((10, AMBER[3]), (11, AMBER[2]), (12, AMBER[1])):
                px[8, y] = rgba(c)                                # latex running down
            px[7, 10] = rgba(AMBER[1])
    return img


RUBBER_RINGS = ["#6e5638", "#86693f", "#9c7c4a", "#b08f58", "#c2a166", "#d3b477", "#e2c78b"]


def rubber_log_top():
    """The cut end: pale wood with uneven, slightly squared growth rings, a darker heartwood, a dot of
    latex in the middle, and a ring of bark round the edge."""
    import math
    import random
    r = random.Random(17)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            edge = max(abs(dx), abs(dy))
            if edge > 6.5:                                        # bark rim
                c = RUBBER_BARK[2] if (x * 5 + y * 3) % 4 else RUBBER_BARK[4]
                if (x == 0 or y == 0) and 0 < x < 15 and 0 < y < 15:
                    c = RUBBER_BARK[5]
            elif edge > 5.6:
                c = RUBBER_BARK[1]                                # the dark line just inside the bark
            else:
                # rings: a blend of square and round distance, with a wobble
                d = 0.55 * edge + 0.45 * math.hypot(dx, dy) + 0.35 * math.sin(x * 1.3 + y * 0.7)
                ring = int(d * 1.15)
                t = 5 - ring % 3 * 2 + (1 if (d * 1.15) % 1 < 0.35 else 0)
                if d < 2.0:
                    t -= 2                                        # darker heartwood
                if r.random() < 0.07:
                    t -= 1
                c = RUBBER_RINGS[max(0, min(6, t))]
                if d < 0.9:
                    c = AMBER[2] if d < 0.6 else AMBER[1]         # latex at the heart
            px[x, y] = rgba(c)
    # a couple of drying cracks running in from the edge
    for x, y in ((11, 4), (12, 3), (4, 11), (3, 12)):
        px[x, y] = rgba(RUBBER_RINGS[0])
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


# ------------------------------------------------------------------ gear and storage
STEEL_METAL = ["#22262e", "#30353f", "#3e4450", "#4d5461", "#5f6774", "#767e8b", "#939aa6", "#b8bec8"]
HANDLE = ["#28180a", "#493012", "#6a4a20", "#896630"]


def _outline(px, filled):
    for (x, y) in list(filled):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in filled:
                px[q] = rgba("#14161a")


def _handle(px, filled, x0, y0, n):
    """A diagonal wooden handle running down-left from (x0, y0) for n pixels."""
    for i in range(n):
        x, y = x0 - i, y0 + i
        px[x, y] = rgba(HANDLE[3] if i % 3 else HANDLE[2])
        filled.add((x, y))
        if x + 1 < 16:
            px[x + 1, y] = rgba(HANDLE[1])
            filled.add((x + 1, y))


def hammer_item():
    """A heavy steel sledge head set square across the top of a long handle."""
    import math
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    filled = set()
    _handle(px, filled, 9, 6, 9)
    cx, cy = 11.0, 4.5
    for y in range(16):
        for x in range(16):
            along = ((x - cx) - (y - cy)) / math.sqrt(2)     # along the handle
            across = ((x - cx) + (y - cy)) / math.sqrt(2)    # the head's length
            if abs(along) <= 2.0 and abs(across) <= 4.6:
                face = abs(across) > 3.6                     # the striking faces at each end
                c = STEEL_METAL[7] if along < -0.8 else STEEL_METAL[5] if along < 0.8 else STEEL_METAL[3]
                if face:
                    c = STEEL_METAL[6] if along < 0 else STEEL_METAL[4]
                px[x, y] = rgba(c)
                filled.add((x, y))
    _outline(px, filled)
    return img


def excavator_item():
    """A broad steel spade on a long handle."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    filled = set()
    _handle(px, filled, 8, 7, 8)
    for y in range(0, 9):
        for x in range(7, 16):
            # a rounded spade, lying corner to corner, top-right
            dx, dy = x - 11.5, y - 3.5
            if (dx - dy) ** 2 / 30 + (dx + dy) ** 2 / 12 <= 1:
                lit = -(dx + dy)
                px[x, y] = rgba(STEEL_METAL[7] if lit > 2 else STEEL_METAL[6] if lit > 0 else STEEL_METAL[5] if lit > -2 else STEEL_METAL[3])
                filled.add((x, y))
    _outline(px, filled)
    return img


PLANK = ["#4a3218", "#6a4824", "#8a6232", "#a87c42", "#c49656"]


def crate_side(trim):
    """Planks in a metal frame with corner plates in the tier colour."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            board = y // 4
            c = PLANK[1] if y % 4 == 3 else PLANK[3] if (x + board * 3) % 7 else PLANK[2]
            px[x, y] = rgba(c)
    for i in range(16):
        for a, b in ((i, 0), (i, 15), (0, i), (15, i)):
            px[a, b] = rgba(STEEL[5] if a == 0 or b == 0 else STEEL[2])
    for i in range(1, 15):                                     # cross brace
        px[i, i] = rgba(PLANK[4])
        if i + 1 < 15:
            px[i + 1, i] = rgba(PLANK[1])
    for cx, cy in ((1, 1), (13, 1), (1, 13), (13, 13)):
        for dx in range(2):
            for dy in range(2):
                px[cx + dx, cy + dy] = rgba(trim[3] if dx == 0 and dy == 0 else trim[1])
    return img


def crate_top(trim):
    img = crate_side(trim)
    px = img.load()
    for x in range(5, 11):                                    # a handle
        px[x, 7] = rgba(STEEL[5])
        px[x, 8] = rgba(STEEL[2])
    return img


def bin_side(trim):
    img = casing(STEEL, trim)
    px = img.load()
    for y in (5, 10):
        for x in range(3, 13):
            px[x, y] = rgba(STEEL[2])
    return img


def bin_front(trim):
    """A recessed label panel where the bin shows its item and count."""
    img = casing(STEEL, trim)
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            edge_dark = x == 2 or y == 2
            edge_light = x == 13 or y == 13
            px[x, y] = rgba(STEEL[1] if edge_dark else STEEL[6] if edge_light else "#2a2d33")
    return img


def crate_gui(cols=9):
    """The Crate screen sheet (256x512): a panel top with nine rows of slots at the top, and the
    player-inventory part of the panel starting at v=180, to draw under however many rows a crate has."""
    img = Image.new("RGBA", (256 if cols == 9 else 512, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W = 16 + cols * 18
    BG, EDGE, DARK = (198, 198, 198, 255), (0, 0, 0, 255), (85, 85, 85, 255)

    def panel(y0, y1, top, bottom):
        d.rectangle([1, y0, W - 2, y1], fill=BG)
        d.line([0, y0 + (2 if top else 0), 0, y1 - (2 if bottom else 0)], fill=EDGE)
        d.line([W - 1, y0 + (3 if top else 0), W - 1, y1 - (3 if bottom else 0)], fill=EDGE)
        d.line([1, y0 + (2 if top else 0), 1, y1], fill=LIGHT)
        d.line([2, y0 + (3 if top else 0), 2, y1 - (5 if bottom else 0)], fill=LIGHT)
        d.line([W - 2, y0 + (4 if top else 0), W - 2, y1 - (3 if bottom else 0)], fill=DARK)
        d.line([W - 3, y0 + (4 if top else 0), W - 3, y1 - (4 if bottom else 0)], fill=DARK)
        if top:
            d.line([2, y0, W - 4, y0], fill=EDGE)
            d.line([2, y0 + 1, W - 4, y0 + 1], fill=LIGHT)
            d.line([3, y0 + 2, W - 5, y0 + 2], fill=LIGHT)
            img.putpixel((1, y0 + 1), EDGE)
            img.putpixel((W - 3, y0 + 1), EDGE)
            img.putpixel((W - 2, y0 + 2), EDGE)
        if bottom:
            d.line([3, y1, W - 3, y1], fill=EDGE)
            d.line([4, y1 - 1, W - 3, y1 - 1], fill=DARK)
            d.line([4, y1 - 2, W - 3, y1 - 2], fill=DARK)
            img.putpixel((1, y1 - 2), EDGE)
            img.putpixel((2, y1 - 1), EDGE)
            img.putpixel((W - 2, y1 - 2), EDGE)

    panel(0, 179, True, False)
    for r in range(9):
        for c in range(cols):
            gui_slot(d, 7 + c * 18, 17 + r * 18)
    panel(180, 276, False, True)
    for r in range(3):
        for c in range(9):
            gui_slot(d, 7 + (cols - 9) * 9 + c * 18, 180 + 14 + r * 18)
    for c in range(9):
        gui_slot(d, 7 + (cols - 9) * 9 + c * 18, 180 + 72)
    return img


# ------------------------------------------------------------------ rubber items, in the metal items' style
# Same method as tools/make_textures.py: a shape lit from the top-left on an eight-step ramp, bright
# top-left edges, darkest bottom-right edges, no full outline.
AMBER8 = ["#4a2a06", "#6e3f0a", "#93560f", "#b46e16", "#d28a22", "#e8a838", "#f6c55a", "#fde08e"]
RUBBER8 = ["#101012", "#18181b", "#212125", "#2b2b30", "#36363c", "#43434a", "#55555d", "#6c6c75"]
WOOD8 = ["#3a2412", "#55361c", "#704926", "#8a5e32", "#a37542", "#b98c54", "#cca468", "#dcbb80"]
STEEL8 = ["#22262e", "#30353f", "#3e4450", "#4d5461", "#5f6774", "#767e8b", "#939aa6", "#b8bec8"]
LEAF8 = ["#12301a", "#1a4422", "#245a2c", "#2f7136", "#3d8a42", "#52a450", "#6cbd60", "#8fd47a"]


def _mt():
    import importlib.util
    spec = importlib.util.spec_from_file_location("mt", os.path.join(os.path.dirname(os.path.abspath(__file__)), "make_textures.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def _lit(mask, lo=2, hi=7):
    """Levels for a shape: brighter toward its top-left, then the reference-style edges."""
    mt = _mt()
    pts = [(x, y) for y in range(16) for x in range(16) if mask[y][x]]
    x0 = min(p[0] for p in pts); x1 = max(p[0] for p in pts)
    y0 = min(p[1] for p in pts); y1 = max(p[1] for p in pts)
    lv = mt.blank()
    for x, y in pts:
        t = 1 - (((x - x0) / max(1, x1 - x0)) + ((y - y0) / max(1, y1 - y0))) / 2
        lv[y][x] = max(lo, min(hi, int(lo + t * (hi - lo + 0.99))))
    return mt.edge_light(lv, mask)


def _paint(img, lv, ramp):
    px = img.load()
    for y in range(16):
        for x in range(16):
            if lv[y][x] is not None:
                px[x, y] = rgba(ramp[max(0, min(len(ramp) - 1, lv[y][x]))])


def _mask(rows):
    m = [[False] * 16 for _ in range(16)]
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            m[y][x] = True
    return m


def latex_item():
    """A glossy amber drop of latex with a bright glint."""
    mask = _mask({3: (7, 8), 4: (6, 9), 5: (6, 9), 6: (5, 10), 7: (4, 11), 8: (4, 11), 9: (3, 12), 10: (3, 12),
                  11: (3, 12), 12: (4, 11), 13: (5, 10)})
    lv = _lit(mask)
    for x, y in ((6, 6), (5, 8), (5, 9)):
        lv[y][x] = 7
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, lv, AMBER8)
    return img


def rubber_item():
    """A sheet of rubber: the plate's shape, in near-black rubber tones."""
    mt = _mt()
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, mt.plate(), RUBBER8)
    return img


def tree_tap_item():
    """A wooden spout with a steel tip, angled down to the right like it is tapped into a tree."""
    wood = _mask({4: (2, 6), 5: (2, 7), 6: (2, 8), 7: (3, 9), 8: (4, 10), 9: (5, 10), 10: (6, 10), 11: (8, 10)})
    tip = _mask({10: (11, 12), 11: (11, 13), 12: (12, 13), 13: (12, 13)})
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, _lit(wood), WOOD8)
    _paint(img, _lit(tip, 3, 7), STEEL8)
    lv = _mt().blank()
    for y in (5, 6):                                   # the spout's open channel
        lv[y][4] = 0
        lv[y][5] = 1
    _paint(img, lv, WOOD8)
    return img


def sapling_item():
    """A young rubber tree: a thin stem and a pointed crown of leaves."""
    stem = _mask({y: (7, 8) for y in range(9, 15)})
    crown = _mask({1: (7, 8), 2: (6, 9), 3: (5, 10), 4: (5, 10), 5: (4, 11), 6: (3, 12), 7: (3, 12), 8: (4, 11), 9: (5, 6)})
    crown[9][9] = crown[9][10] = True
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, _lit(stem, 2, 5), WOOD8)
    _paint(img, _lit(crown), LEAF8)
    return img


def rubber_spot(state):
    """The tapping cut on a rubber log, drawn alone (transparent elsewhere) to lay over the bark:
    a V-shaped notch, amber with latex when "wet", dark and empty when "dry"."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cut = [(6, 6), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (7, 8), (8, 8)]
    for x, y in cut:
        if state == "wet":
            px[x, y] = rgba(AMBER8[7] if (x, y) == (7, 6) else AMBER8[6] if y == 6 else AMBER8[5] if y == 7 else AMBER8[3])
        else:
            px[x, y] = rgba("#16100a" if y < 8 else "#24190f")
    for x in range(5, 11):
        px[x, 5] = rgba("#2e251d")                     # the cut's upper lip, in shadow
    for x, y in ((5, 6), (10, 6), (6, 8), (9, 8), (7, 9), (8, 9)):
        px[x, y] = rgba("#b8a994")                     # its lower edge, catching light
    if state == "wet":
        for y, c in ((10, AMBER8[5]), (11, AMBER8[4]), (12, AMBER8[2])):
            px[8, y] = rgba(c)                         # latex running down
    return img


# ------------------------------------------------------------------ tier 3 (electrum-gold trim)
ELECTRUM_TRIM = ["#5a4410", "#8a6c1c", "#b8932a", "#e0bc3e", "#f4d860", "#fff09a"]
STEAM = ["#9aa6ae", "#b8c2c8", "#d6dde0", "#eef2f4"]


def casing3():
    return casing(STEEL, ELECTRUM_TRIM)


def boiler_front(on):
    """A firebox door under a riveted pressure drum; the firebox glows and steam wisps while running."""
    img = casing3()
    px = img.load()
    for y in range(3, 8):                                   # the drum
        for x in range(3, 13):
            px[x, y] = rgba(COPPER[3] if y == 3 else COPPER[2] if y < 7 else COPPER[1])
    for x in (4, 7, 10):
        px[x, 5] = rgba(COPPER[5])
    window(img, 5, 9, 10, 12)                               # firebox
    for y in range(9, 13):
        for x in range(5, 11):
            px[x, y] = rgba((FIRE[5] if y == 12 else FIRE[4] if y > 10 else FIRE[3]) if on else FIRE[0])
    if on:
        for x, y in ((12, 2), (13, 1), (12, 1)):
            px[x, y] = rgba(STEAM[3])
    return img


def turbine_front(on):
    """A bladed rotor in a round housing; it blurs into a ring while spinning."""
    import math
    img = casing3()
    px = img.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if 4.4 < r <= 5.4:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])
            elif r <= 4.4:
                ang = math.atan2(y - cy, x - cx)
                if r < 1.3:
                    c = ELECTRUM_TRIM[3]
                elif on:
                    c = STEEL[5] if int(r * 2) % 2 else STEEL[4]
                else:
                    blade = math.cos(ang * 6 + r * 0.5) > 0.2
                    c = (STEEL[6] if x + y < 15 else STEEL[4]) if blade else "#16181c"
                px[x, y] = rgba(c)
    return img


def cable_texture(core, sheath, stripe):
    """An insulated cable: a sheath with a tier stripe running along it, the metal core showing at the ends."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = sheath[2]
            if (x + y) % 5 == 0:
                c = sheath[3]
            if x in (7, 8):
                c = stripe[3] if x == 7 else stripe[1]
            if y in (0, 15):
                c = sheath[0]
            px[x, y] = rgba(c)
    for y in range(6, 10):                                  # the core seen on the cut faces
        for x in range(6, 10):
            px[x, y] = rgba(core[4] if (x + y) % 3 else core[3])
    return img


def plastic_sheet_item():
    """A sheet of plastic: the plate's shape, in pale off-white."""
    plastic = ["#6e7378", "#8a8f94", "#a6abaf", "#bfc3c6", "#d2d5d7", "#e2e4e5", "#eef0f0", "#fafbfb"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, _mt().plate(), plastic)
    return img



def _dark_window(img, x0, y0, x1, y1):
    window(img, x0, y0, x1, y1)
    px = img.load()
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rgba("#16181c")
    return px


def block_placer_front(on):
    """A piston-style pusher plate in the window; it slides forward while placing."""
    img = casing()
    px = _dark_window(img, 3, 3, 12, 12)
    d = 1 if on else 0
    for y in range(4, 12):
        for x in range(4, 12):
            if x in (4, 11) or y in (4, 11):
                px[x, y] = rgba(PLANK[3] if y == 4 or x == 4 else PLANK[1])
            elif 6 <= x <= 9 and 6 <= y <= 9:
                px[x, y] = rgba(STEEL[6 - d] if (x + y) < 15 else STEEL[4 - d])
    return img


def block_breaker_front(on):
    """A pick head in the window, swung down while breaking."""
    img = casing()
    px = _dark_window(img, 3, 3, 12, 12)
    head = [(4, 5), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 5), (11, 6)] if not on else [(4, 7), (5, 6), (6, 5), (7, 5), (8, 5), (9, 6), (10, 7), (11, 8)]
    for i, (x, y) in enumerate(head):
        px[x, y] = rgba(STEEL[6] if i < 4 else STEEL[4])
        px[x, y + 1] = rgba(STEEL[3])
    for y in range(6 if not on else 7, 12):
        px[7, y] = rgba(PLANK[3])
        px[8, y] = rgba(PLANK[1])
    return img


def autocrafter_front(on):
    """A 3x3 crafting grid; the middle square glows while crafting."""
    img = casing2()
    px = _dark_window(img, 3, 3, 12, 12)
    for gy in range(3):
        for gx in range(3):
            lit = on and gx == 1 and gy == 1
            for y in range(4 + gy * 3, 6 + gy * 3):
                for x in range(4 + gx * 3, 6 + gx * 3):
                    px[x, y] = rgba((COPPER[4] if lit else PLANK[3]) if (x + y) % 3 else (COPPER[3] if lit else PLANK[2]))
    return img


def fluid_placer_front(on, drain=False):
    """A round nozzle (Placer) or a grated intake (Drain); blue while moving fluid."""
    import math
    img = casing2()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 3.4 < r <= 4.4:
                px[x, y] = rgba(STEEL[1] if (x + y) < 15 else STEEL[6])
            elif r <= 3.4:
                if drain and (x + y) % 2 == 0:
                    px[x, y] = rgba(STEEL[4])
                else:
                    px[x, y] = rgba((WATER[3] if r < 1.5 else WATER[2]) if on else "#16181c")
    return img


def mechanical_user_front(on):
    """A jointed arm reaching out of the window, its grip glowing while it works."""
    img = casing3()
    px = _dark_window(img, 3, 3, 12, 12)
    for i in range(6):
        px[4 + i, 10 - i // 2] = rgba(STEEL[5])
        px[4 + i, 11 - i // 2] = rgba(STEEL[3])
    for x, y in ((10, 6), (11, 5), (11, 7)):
        px[x, y] = rgba(ELECTRUM_TRIM[4] if on else STEEL[4])
    return img


def quarry_front(on):
    """A drill bit pointing down out of the window, its tip glowing while it mines."""
    img = casing3()
    px = _dark_window(img, 3, 3, 12, 12)
    for y in range(4, 12):
        half = max(0, 3 - (y - 4) // 2)
        for x in range(8 - half - 1, 8 + half + 1):
            spiral = (x + y + (1 if on else 0)) % 3 == 0
            px[x, y] = rgba(STEEL[6] if spiral else STEEL[4])
    px[7, 12] = rgba(FIRE[5] if on else STEEL[2])
    px[8, 12] = rgba(FIRE[4] if on else STEEL[1])
    return img


def mob_fighter_front(on):
    """A sword blade across the window, red-tinted while fighting."""
    img = casing3()
    px = _dark_window(img, 3, 3, 12, 12)
    for i in range(7):
        x, y = 4 + i, 11 - i
        px[x, y] = rgba(STEEL[7] if not on else "#ffb0a8")
        px[x + 1, y] = rgba(STEEL[5] if not on else "#e05a4a")
    for x, y in ((4, 9), (6, 11), (5, 10)):
        px[x, y] = rgba(ELECTRUM_TRIM[3])
    return img


# ------------------------------------------------------------------ logic
SENSOR_ICONS = {
    "inventory": ["#######", "#.....#", "#######", "#..#..#", "#.....#", "#######"],
    "tank": ["..###..", ".#...#.", "#.....#", "#~~~~~#", "#~~~~~#", ".#####."],
    "energy": ["....##.", "...##..", "..####.", "...##..", "..##...", ".##...."],
    "time": [".#####.", "#..#..#", "#..#..#", "#..###.", "#.....#", ".#####."],
    "proximity": ["..###..", ".#...#.", "#..#..#", ".#...#.", "..###..", "......."],
}


def sensor_side():
    img = casing3()
    return img


def sensor_front():
    """The sensing face, against the block it reads: a grille."""
    img = casing3()
    px = img.load()
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba(STEEL[1] if (x + y) % 2 else STEEL[3])
    return img


def sensor_back(kind, on):
    """The face you see: the sensor's symbol in a dark window, and a lamp that lights with the signal."""
    img = casing3()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#16181c")
    for j, row in enumerate(SENSOR_ICONS[kind]):
        for i, ch in enumerate(row):
            if ch == "#":
                px[4 + i, 4 + j] = rgba(STEEL[6])
            elif ch == "~":
                px[4 + i, 4 + j] = rgba(WATER[2])
    for x in range(10, 12):
        for y in range(10, 12):
            px[x, y] = rgba(("#ff5a48" if (x, y) == (10, 10) else "#d0281c") if on else "#3a1612")
    return img


def logic_controller_side():
    """Rows of indicator LEDs and a strip of circuitry."""
    img = casing3()
    px = img.load()
    window(img, 3, 3, 12, 12)
    board = ["#0d1f3a", "#163266", "#21498e"]
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba(board[1] if (x + y) % 4 else board[2])
    leds = ["#ff5a48", "#4fd14a", "#ffd24a", "#4fd14a", "#ff5a48", "#4f9ad1"]
    for i, c in enumerate(leds):
        px[4 + (i % 3) * 3, 4 + (i // 3) * 3] = rgba(c)
    for x in range(4, 12):
        px[x, 11] = rgba(ELECTRUM_TRIM[3] if x % 2 else ELECTRUM_TRIM[1])
    return img


def logic_controller_top():
    img = casing3()
    px = img.load()
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = rgba("#d0281c")
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

# ------------------------------------------------------------------ powered gear (tier 3)
# Same lit-shape method as the rubber items: each part a shape on an eight-step ramp, lit from the top-left.
EXO8 = ["#11161d", "#1a222c", "#24303c", "#2f3e4c", "#3c4e5e", "#4d6273", "#647a8b", "#8197a6"]
ENERGY8 = ["#0a3a52", "#0f5677", "#13749b", "#1a93bd", "#2fb3da", "#5fd0ef", "#9be6f8", "#dff8ff"]
ELECTRUM8 = ["#957c3d", "#a19144", "#bca54f", "#c9b255", "#d9c25f", "#e9da75", "#f8f796", "#fbfbbc"]


def _diag(test):
    """A mask from a test on (along, across): along runs bottom-left to top-right, across top-left to
    bottom-right, both centred on the icon."""
    import math
    m = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            if test((dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)):
                m[y][x] = True
    return m


def _parts(*parts):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for mask, ramp, lo, hi in parts:
        if any(any(r) for r in mask):
            _paint(img, _lit(mask, lo, hi), ramp)
    return img


def power_drill_item():
    """A pistol-grip drill: a dark body with a glowing power strip, a gold chuck and a spiralled steel bit."""
    body = _diag(lambda a, c: -6.4 <= a <= 0.6 and abs(c + 0.7) <= 2.2)
    grip = _diag(lambda a, c: -6.0 <= a <= -3.2 and 1.5 < c + 0.7 <= 6.6)
    chuck = _diag(lambda a, c: 0.6 < a <= 2.4 and abs(c + 0.7) <= 1.6)
    bit = _diag(lambda a, c: 2.4 < a <= 8.8 and abs(c + 0.7) <= max(0.4, (9.4 - a) * 0.32))
    img = _parts((grip, RUBBER8, 2, 6), (body, EXO8, 2, 7), (chuck, ELECTRUM8, 2, 7), (bit, STEEL8, 3, 7))
    px = img.load()
    strip = _diag(lambda a, c: -5.6 <= a <= -0.2 and abs(c + 0.7) <= 0.4)
    trigger = _diag(lambda a, c: -3.0 <= a <= -2.0 and 1.5 < c + 0.7 <= 3.4)
    for y in range(16):
        for x in range(16):
            if strip[y][x]:
                px[x, y] = rgba(ENERGY8[6] if (x + y) % 3 else ENERGY8[4])
            if trigger[y][x]:
                px[x, y] = rgba(ELECTRUM8[3])
            if bit[y][x] and ((x - y) // 2) % 2 == 0 and (x + y) % 2 == 0:   # the spiral
                px[x, y] = rgba(STEEL8[2])
    return img


def chainsaw_item():
    """A long guide bar with a toothed chain, a dark motor housing with a power strip and a top handle."""
    import math
    body = _diag(lambda a, c: -9.4 <= a <= -1.6 and abs(c) <= 3.0)
    handle = _diag(lambda a, c: (-8.6 <= a <= -2.6 and -5.6 <= c < -4.4)
                   or ((-8.6 <= a <= -7.4 or -3.8 <= a <= -2.6) and -4.4 <= c < -3.0))
    bar = _diag(lambda a, c: -1.6 < a <= 9.6 and abs(c) <= 2.2)
    img = _parts((handle, STEEL8, 2, 6), (body, EXO8, 2, 7), (bar, STEEL8, 3, 7))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            a, c = (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)
            if bar[y][x] and abs(c) > 1.2:              # the chain, light and dark links
                px[x, y] = rgba(STEEL8[3] if ((x - y) // 2) % 2 else STEEL8[7])
            if body[y][x] and -8.2 <= a <= -2.8 and abs(c) <= 0.6:
                px[x, y] = rgba(ENERGY8[6] if (x + y) % 3 else ENERGY8[4])
    return img


def energy_sword_item():
    """A blade of light: a glowing cyan edge around a white core, a gold guard and a rubber grip."""
    blade = _diag(lambda a, c: -1.4 <= a <= 7.6 and abs(c) <= min(1.3, (8.0 - a) * 0.55))
    guard = _diag(lambda a, c: -2.6 <= a < -1.4 and abs(c) <= 3.0)
    grip = _diag(lambda a, c: -6.4 <= a < -2.6 and abs(c) <= 0.8)
    pommel = _diag(lambda a, c: -7.6 <= a < -6.4 and abs(c) <= 1.3)
    img = _parts((grip, RUBBER8, 3, 7), (pommel, ELECTRUM8, 2, 7), (guard, ELECTRUM8, 2, 7), (blade, ENERGY8, 3, 7))
    px = img.load()
    core = _diag(lambda a, c: -1.0 <= a <= 6.6 and abs(c) <= 0.4)
    for y in range(16):
        for x in range(16):
            if core[y][x]:
                px[x, y] = rgba(ENERGY8[7])
    return img


def energy_cell_item(caps=None):
    """An upright power cell: gold caps, a dark body and a glowing charge window."""
    caps = caps or ELECTRUM8
    body = _mask({y: (5, 10) for y in range(3, 14)})
    cap = _mask({1: (7, 8), 2: (6, 9)})
    base = _mask({14: (5, 10)})
    img = _parts((body, EXO8, 2, 7), (cap, caps, 3, 7), (base, caps, 1, 4))
    px = img.load()
    for y in range(5, 12):
        for x in (7, 8):
            px[x, y] = rgba(ENERGY8[7 if y < 7 else 5 if y < 10 else 3] if x == 7 else ENERGY8[5 if y < 7 else 4 if y < 10 else 2])
    for x in range(5, 11):                             # gold bands
        px[x, 3] = rgba(caps[6] if x < 9 else caps[3])
        px[x, 13] = rgba(caps[4] if x < 9 else caps[1])
    return img


MODULE_COLOR = {"tool": "#ffb347", "weapon": "#ff6b5a", "tool_or_weapon": "#ffe066", "armor": "#7fe8ff"}
MODULE_ART = {
    "speed": ("tool", ["#..#...", ".#..#..", "..#..#.", ".#..#..", "#..#..."]),
    "range": ("tool", ["#.#.#.#", ".......", "#.###.#", ".......", "#.#.#.#"]),
    "silk_touch": ("tool", ["...#...", "..#.#..", ".#...#.", "..#.#..", "...#..."]),
    "fortune": ("tool", ["#.#.###", ".#....#", "#.#.###", "......#", "....###"]),
    "auto_smelt": ("tool", ["#######", "#.....#", "#..#..#", "#.###.#", "#######"]),
    "magnet": ("tool_or_weapon", ["##...##", "##...##", "##...##", ".#####.", "..###.."]),
    "damage": ("weapon", ["...#...", "..###..", ".#.#.#.", "...#...", "...#..."]),
    "looting": ("weapon", ["..###..", ".#...#.", "#..#..#", ".#...#.", "..###.."]),
    "fire": ("weapon", ["...#...", "..##.#.", ".#####.", "#######", ".#####."]),
    "night_vision": ("armor", [".#####.", "#..#..#", "#.###.#", "#..#..#", ".#####."]),
    "water_breathing": ("armor", [".#...#.", "#.#.#.#", ".#...#.", "...#...", "..#.#.."]),
    "solar": ("armor", ["#..#..#", ".#.#.#.", "..###..", ".#.#.#.", "#..#..#"]),
    "jetpack": ("armor", ["...#...", "..###..", ".#####.", "..#.#..", ".#...#."]),
    "flight": ("armor", ["#.....#", "##...##", ".#####.", "..###..", "...#..."]),
    "swiftness": ("armor", ["####...", ".......", "######.", ".......", "####..."]),
    "jump_boost": ("armor", ["...#...", "..###..", ".#.#.#.", "...#...", "#######"]),
    "step_assist": ("armor", ["....###", "....#..", "..###..", "..#....", "###...."]),
    "fall_protection": ("armor", ["...#...", "...#...", ".#.#.#.", "..###..", "#######"]),
    "chain_lightning": ("weapon", ["....##.", "...##..", "..####.", "...##..", "..##..."]),
    "lifesteal": ("weapon", [".##.##.", "#######", ".#####.", "..###..", "...#..."]),
}



def charging_pad_top(on):
    """The pad's face: a steel plate in a gold frame with a ring-shaped charging coil."""
    img = casing3()
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = rgba(EXO8[3] if (x + y) % 2 else EXO8[2])
    coil = ENERGY8 if on else ["#1a2630", "#22323e", "#2b3f4d", "#35505f", "#3f5f70", "#4b6f80", "#587e8e", "#668c9b"]
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.2 <= d <= 5.4 or d <= 1.6:
                px[x, y] = rgba(coil[6] if (x + y) < 15 else coil[4])
            elif 2.6 <= d <= 3.2:
                px[x, y] = rgba(coil[3])
    return img


def modification_table_top():
    """A work mat: a dark grid in a gold frame."""
    img = casing3()
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            px[x, y] = rgba(EXO8[4] if x % 4 == 1 or y % 4 == 1 else EXO8[2])
    return img


def modification_table_side(front):
    """The bench: a gold-trimmed top band over a dark column; the front has a small screen."""
    img = casing3()
    px = img.load()
    for y in range(4):
        for x in range(16):
            px[x, y] = rgba(ELECTRUM_TRIM[4 if y == 0 else 3 if y == 1 else 2 if y == 2 else 0])
    for y in range(4, 16):
        for x in range(2, 14):
            px[x, y] = rgba(EXO8[3] if x in (2, 13) else EXO8[2] if (x + y) % 5 else EXO8[1])
    if front:
        for y in range(6, 11):
            for x in range(4, 12):
                px[x, y] = rgba(ENERGY8[1] if x in (4, 11) or y in (6, 10) else ENERGY8[4] if (x + y) % 3 else ENERGY8[6])
    return img


def modification_table_gui():
    """The Modification Table screen: the gear slot, a gold link, and two module slots."""
    img = gui_panel()
    d = ImageDraw.Draw(img)
    gui_slot(d, 43, 34)
    gui_slot(d, 97, 34)
    gui_slot(d, 115, 34)
    for x in range(64, 94):                            # the link from gear to modules
        for y in (42, 43):
            img.putpixel((x, y), rgba(ELECTRUM8[4 if y == 42 else 1]))
    for x, y in ((90, 40), (91, 41), (90, 45), (91, 44)):
        img.putpixel((x, y), rgba(ELECTRUM8[3]))
    return img


COPPER8 = ["#5a2412", "#772c15", "#8a3a1c", "#b8622a", "#ce6e26", "#da7e2a", "#f0a030", "#fcbe2d"]


def upgrade_kit_item(trim):
    """A tier upgrade kit: a small case in the tier's trim colour with a steel handle and a glowing arrow."""
    case = _mask({y: (2, 13) for y in range(5, 14)})
    handle = _mask({2: (5, 10), 3: (5, 5), 4: (5, 5)})
    handle[3][10] = handle[4][10] = True
    img = _parts((handle, STEEL8, 3, 7), (case, trim, 2, 7))
    px = img.load()
    for x in range(2, 14):                             # the lid seam
        px[x, 7] = rgba(trim[1])
    for (x, y) in ((7, 9), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (7, 11), (8, 11), (7, 12), (8, 12)):
        px[x, y] = rgba(ENERGY8[6] if y < 11 else ENERGY8[4])
    return img


def geothermal_front(on):
    """A grated firebox showing lava: it glows and shimmers while making power."""
    img = casing2()
    px = img.load()
    window(img, 3, 3, 12, 12)
    LAVA = ["#3a0c04", "#7a1c06", "#c8420c", "#f07a18", "#ffb43a", "#ffe08a"]
    for y in range(3, 13):
        for x in range(3, 13):
            if on:
                c = LAVA[5] if (x * 7 + y * 3) % 11 == 0 else LAVA[4] if (x + y * 2) % 5 == 0 else LAVA[3] if y > 6 else LAVA[2]
            else:
                c = LAVA[1] if (x + y) % 4 == 0 else LAVA[0]
            px[x, y] = rgba(c)
    for x in range(3, 13):                               # the grate
        if x % 3 == 0:
            for y in range(3, 13):
                px[x, y] = rgba(STEEL[2])
    return img


def wind_turbine_front(on):
    """A three-bladed rotor on a hub; the blades blur into a disc while it spins."""
    import math
    img = casing3()
    px = img.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 6.2 or (x in (0, 15) or y in (0, 15)):
                continue
            ang = math.atan2(y - cy, x - cx)
            if r < 1.5:
                px[x, y] = rgba(ELECTRUM_TRIM[4] if x + y < 15 else ELECTRUM_TRIM[2])
            elif on:
                if 1.5 <= r <= 6.2:
                    px[x, y] = rgba(STEEL[5] if int(r * 1.5) % 2 else STEEL[4])
            else:
                blade = min(abs(((ang - k * 2.094 + math.pi) % (2 * math.pi)) - math.pi) for k in range(3))
                if r * math.sin(min(blade, 1.5)) < 1.1 - r * 0.06:
                    px[x, y] = rgba(STEEL[6] if x + y < 15 else STEEL[4])
                else:
                    px[x, y] = rgba("#1a1d22")
    return img


def solar_top(trim):
    """Photovoltaic cells: deep blue squares with silver grid lines in a frame of the tier's trim colour."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    CELL = ["#0c1a3a", "#13285a", "#1c3a7c", "#2a54a8", "#4a7ad0"]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = trim[4] if (x == 0 or y == 0) else trim[1]
            elif x in (5, 10) or y in (5, 10):
                c = STEEL[5]
            else:
                lx, ly = (x - 1) % 5, (y - 1) % 5
                c = CELL[4] if lx + ly == 0 else CELL[3] if lx + ly < 3 else CELL[2] if lx + ly < 6 else CELL[1]
            px[x, y] = rgba(c)
    return img


def fertilizer_mixer_front(on):
    """A hopper-fed drum of dark, earthy granules that tumbles while mixing."""
    img = casing2()
    px = img.load()
    window(img, 3, 3, 12, 12)
    SOIL = ["#2a1c10", "#3e2a16", "#5a3e20", "#6e5a24", "#7c7a2c", "#93a03a"]
    for y in range(3, 13):
        for x in range(3, 13):
            k = (x * 5 + y * 3 + (2 if on else 0)) % 7
            px[x, y] = rgba(SOIL[min(5, k)] if on else SOIL[k % 3])
    for x in range(3, 13):
        px[x, 7] = rgba(STEEL[3] if not on else STEEL[5] if x % 2 else STEEL[3])
    return img


def animal_rancher_front(on):
    """A little window on a grassy pen with a fence rail; a lamp glows while it works."""
    img = casing2()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#8ec5ff" if y < 8 else "#4f9a48" if (x + y) % 3 else "#3d8a42")
    for x in range(3, 13):
        px[x, 9] = rgba(WOOD[5])
    for x in (4, 8, 12):
        for y in range(8, 13):
            px[x, y] = rgba(WOOD[3])
    for x, y in ((6, 7), (7, 7), (6, 6), (7, 6), (8, 6), (9, 7)):      # a sheep's back over the rail
        px[x, y] = rgba("#f2f2f2" if y == 6 else "#d8d8d8")
    px[13, 2] = rgba("#4fd14a" if on else "#2c3a30")
    return img


def hydroponic_bed_front(on):
    """Rows of green shoots in a glass tray under a grow light that glows magenta while running."""
    img = casing3()
    px = img.load()
    window(img, 2, 3, 13, 12)
    for y in range(3, 13):
        for x in range(2, 14):
            px[x, y] = rgba("#14161c")
    for x in range(3, 13):
        px[x, 3] = rgba("#ff7af0" if on else "#4a2a48")                 # the grow light
        px[x, 4] = rgba("#c050c0" if on else "#2a1a2c")
    for x in range(3, 13, 3):
        for y in range(7, 11):
            px[x, y] = rgba("#6cbd60" if y < 9 else "#3d8a42")
        px[x - 1, 8] = rgba("#52a450")
        px[x + 1, 7] = rgba("#8fd47a")
    for x in range(2, 14):
        px[x, 11] = rgba(WATER[2])
        px[x, 12] = rgba(WATER[1])
    return img


def mob_grinder_front(on):
    """Two rows of serrated steel blades; they turn and the gap glows red while it runs."""
    img = casing3()
    px = img.load()
    window(img, 2, 3, 13, 12)
    for y in range(3, 13):
        for x in range(2, 14):
            px[x, y] = rgba("#5a0c0c" if on and y in (7, 8) else "#14161c")
    for x in range(2, 14):
        tooth = (x + (1 if on else 0)) % 2
        px[x, 4] = rgba(STEEL[5]); px[x, 5] = rgba(STEEL[4] if tooth else STEEL[2])
        px[x, 6] = rgba(STEEL[3] if tooth else "#14161c")
        px[x, 9] = rgba(STEEL[3] if not tooth else "#14161c")
        px[x, 10] = rgba(STEEL[4] if not tooth else STEEL[2]); px[x, 11] = rgba(STEEL[5])
    return img


def auto_enchanter_front(on):
    """An open book on a dark lectern; its pages glow violet and runes drift up while enchanting."""
    img = casing3()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#120c1c")
    for x in range(4, 12):
        for y in range(8, 11):
            px[x, y] = rgba((PURPLE[6] if on else "#c8c0b0") if x not in (7, 8) else (PURPLE[4] if on else "#8a8070"))
    for x in range(4, 12):
        px[x, 11] = rgba("#6a2a1a")
    if on:
        for x, y in ((5, 6), (9, 5), (7, 4), (10, 7)):
            px[x, y] = rgba(PURPLE[7])
    return img


def sprinkler_texture():
    """The sprinkler: a copper body with steel nozzle holes."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgba(COPPER[3] if (x + y) % 5 else COPPER[2])
    for y in range(16):
        px[0, y] = rgba(COPPER[5]); px[15, y] = rgba(COPPER[1])
    for x in range(16):
        px[x, 0] = rgba(COPPER[5]); px[x, 15] = rgba(COPPER[1])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11), (7, 7), (8, 8)):
        px[x, y] = rgba(STEEL[1])
    return img


FERT8 = ["#24180c", "#3a2814", "#523a1c", "#6a4e22", "#7a6a2a", "#8c8434", "#a0a040", "#bcc058"]


def fertilizer_item():
    """A heap of fertilizer granules in dark earthy greens and browns."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _paint(img, _mt().dust(), FERT8)
    return img


def conveyor_top():
    """The belt: dark rubber with raised ridges and two steel rails; chevrons point the way it runs (up)."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15):
                c = STEEL[5] if x == 0 else STEEL[2]
            elif x in (1, 14):
                c = STEEL[3]
            else:
                c = RUBBER8[3] if y % 4 == 0 else RUBBER8[2]
            px[x, y] = rgba(c)
    for base in (3, 11):                                 # chevrons pointing toward the top edge
        for i in range(4):
            for x in (7 - i, 8 + i):
                px[x, base + i] = rgba(COPPER[4] if i < 2 else COPPER[3])
    return img


def conveyor_side():
    """The belt's frame: steel with a dark belt edge along the top."""
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = rgba(RUBBER8[2] if y == 12 else STEEL[4] if y < 15 else STEEL[2])
    for x in (2, 7, 13):
        px[x, 13] = rgba(STEEL[6])
    return img


def chemical_dissolver_front(on):
    """A glass vat of yellow acid with a stirrer; it bubbles while dissolving."""
    img = casing3()
    px = img.load()
    window(img, 4, 3, 11, 12)
    ACID = ["#3a3510", "#6b6218", "#a89a22", "#d9d23a", "#f2ec7a"]
    for y in range(3, 13):
        for x in range(4, 12):
            if y < 6:
                px[x, y] = rgba("#16181c")
            else:
                px[x, y] = rgba((ACID[3] if (x * 3 + y) % 5 == 0 else ACID[2]) if on else ACID[0])
    for x in range(4, 12):
        px[x, 6] = rgba(ACID[4] if on else ACID[1])
    for y in range(3, 11):
        px[7, y] = rgba(STEEL[5])
    if on:
        for x, y in ((5, 9), (10, 8), (9, 11), (6, 7)):
            px[x, y] = rgba(ACID[4])
    return img


def crystallizer_front(on):
    """A dark growth chamber; crystals glow in it while they form."""
    img = casing3()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#14161c")
    GLOW = ["#1d2f3a", "#2c5a6e", "#4aa3c0", "#9be6f8", "#e6fbff"]
    for cx, top in ((5, 8), (8, 5), (10, 7)):
        for y in range(top, 12):
            for x in (cx, cx + 1):
                lit = x == cx
                px[x, y] = rgba((GLOW[4] if y == top else GLOW[3] if lit else GLOW[2]) if on else (GLOW[1] if lit else GLOW[0]))
    for x in range(3, 13):
        px[x, 12] = rgba(ELECTRUM_TRIM[2])
    return img


# ------------------------------------------------------------------ redrawn item art
import colorsys
import math

def ramp8(base):
    """Eight steps from deep shadow to highlight around a base colour (step 5 is the base)."""
    r, g, b = (int(base[i:i + 2], 16) / 255 for i in (1, 3, 5))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(8):
        li = max(0.05, min(0.93, l * (0.38 + 0.125 * i) + (0.06 * (i - 5) if i > 5 else 0)))
        si = min(1, s * (1.1 - 0.06 * max(0, i - 5)))
        hi = (h + (i - 5) * 0.008) % 1                 # warm the lights, cool the shadows a touch
        rr, gg, bb = colorsys.hls_to_rgb(hi, li, si)
        out.append("#%02x%02x%02x" % (int(rr * 255), int(gg * 255), int(bb * 255)))
    return out


def card_item(base, icon, frame=STEEL8):
    """An upgrade/filter card: a bevelled steel frame round a coloured panel, an embossed symbol, gold
    contacts along the bottom edge."""
    ramp = ramp8(base)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(1, 15):
        for x in range(1, 15):
            if (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
                continue
            if x in (1, 14) or y in (1, 14):
                c = frame[6] if (x == 1 or y == 1) and not (x == 14 or y == 14) else frame[2]
                if (x == 1 and y == 14) or (x == 14 and y == 1):
                    c = frame[4]
            elif x in (2, 13) or y in (2, 11):
                c = frame[5] if (x == 2 or y == 2) and x != 13 else frame[3]
            elif y >= 12:
                c = EXO8[1]
            else:
                t = (y - 3) / 8
                c = ramp[6 if t < 0.15 else 5 if t < 0.45 else 4 if t < 0.8 else 3]
                if x + y in (8, 9) and y < 7:
                    c = ramp[6] if x + y == 9 else ramp[7]       # a glossy sheen across the corner
            px[x, y] = rgba(c)
    for x in range(3, 13, 2):                               # gold contacts
        px[x, 12] = rgba(ELECTRUM8[6])
        px[x, 13] = rgba(ELECTRUM8[3])
    for j, row in enumerate(icon):                          # the symbol, raised: light face, dark lower edge
        for i, ch in enumerate(row):
            if ch == "#":
                x, y = 4 + i, 4 + j
                if 0 <= y + 1 < 16 and (j + 1 >= len(icon) or icon[j + 1][i] != "#"):
                    px[x, y + 1] = rgba(ramp[1])
        for i, ch in enumerate(row):
            if ch == "#":
                px[4 + i, 4 + j] = rgba("#fffaf0" if j < 2 else "#f0e6d2")
    return img


def circuit_item(board, chip_ramp, trace, advanced=False):
    """A circuit board seen from above: a bevelled board, metal traces, a chip with silver legs and small
    parts; the advanced one has a gold-lidded chip and more traces."""
    ramp = ramp8(board)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, x1, y0, y1 = 1, 14, 2, 13
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x, y) in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
                continue
            if x == x1 or y == y1:
                c = ramp[1]
            elif x == x0 or y == y0:
                c = ramp[6]
            elif x == x1 - 1 or y == y1 - 1:
                c = ramp[3]
            else:
                c = ramp[5] if (x + y) < 13 else ramp[4]
            px[x, y] = rgba(c)
    tr = ramp8(trace)
    lines = [(3, 5, 3, 12), (12, 4, 12, 11), (3, 5, 6, 5), (9, 10, 12, 10), (4, 11, 10, 11)]
    if advanced:
        lines += [(3, 8, 5, 8), (10, 6, 12, 6), (7, 3, 7, 4), (8, 3, 10, 3)]
    for (ax, ay, bx, by) in lines:
        for x in range(min(ax, bx), max(ax, bx) + 1):
            for y in range(min(ay, by), max(ay, by) + 1):
                px[x, y] = rgba(tr[5])
    for (x, y) in ((3, 5), (12, 4), (12, 11), (4, 11), (3, 12)):
        px[x, y] = rgba(tr[7])                             # solder pads
    cr = chip_ramp
    for y in range(5, 10):                                  # the chip
        for x in range(5, 11):
            c = cr[2] if (x == 10 or y == 9) else cr[5] if (x == 5 or y == 5) else cr[3]
            px[x, y] = rgba(c)
    px[6, 6] = rgba(cr[7] if advanced else cr[6])
    for y in (5, 7, 9):                                     # legs
        px[4, y] = rgba(STEEL8[6]); px[11, y] = rgba(STEEL8[4])
    for x in (6, 8, 10):
        px[x, 4] = rgba(STEEL8[6]); px[x, 10] = rgba(STEEL8[3])
    # small parts: a capacitor and a resistor
    px[10, 12] = rgba("#3a7ad0"); px[11, 12] = rgba("#245aa8")
    px[2, 9] = rgba("#c8a060"); px[2, 10] = rgba("#8a6a3a")
    return img


def wrench_item():
    """An open-ended spanner corner to corner: a forged steel head with a U jaw top-right, a fluted
    steel shaft, and a rubber grip with a copper collar bottom-left."""
    def at(x, y):
        dx, dy = x - 7.5, y - 7.5
        return (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)
    head, shaft, grip, collar = (_diag(f) for f in (
        lambda a, c: math.hypot(a - 6.0, c) <= 3.9 and not (a > 5.4 and abs(c) <= 1.3),
        lambda a, c: -4.0 <= a <= 3.0 and abs(c) <= 1.3,
        lambda a, c: -10.0 <= a < -4.6 and abs(c) <= 1.6,
        lambda a, c: -4.6 <= a < -3.6 and abs(c) <= 1.8))
    GRIP = ramp8("#b8322a")
    img = _parts((grip, GRIP, 2, 7), (collar, COPPER8, 2, 7), (shaft, STEEL8, 3, 7), (head, STEEL8, 3, 7))
    px = img.load()
    for y in range(16):
        for x in range(16):
            a, c = at(x, y)
            if shaft[y][x] and abs(c) < 0.4 and -3.5 < a < 3.5:
                px[x, y] = rgba(STEEL8[3])                  # the flute down the shaft
            if grip[y][x] and int(a) % 2 == 0 and abs(c) < 1.2:
                px[x, y] = rgba(GRIP[3])                    # grip ridges
    return img


def module_cartridge(kind_color, icon):
    """A module cartridge: a dark plated casing with a glowing window in the colour of what it fits,
    its symbol dark on the glow, and gold pins along the bottom."""
    glow = ramp8(kind_color)
    body = _mask({y: (2, 13) for y in range(2, 13)})
    body[2][2] = body[2][13] = False
    img = _parts((body, EXO8, 2, 7))
    px = img.load()
    for y in range(3, 11):                                  # the window
        for x in range(3, 13):
            edge_t = y == 3 or x == 3
            edge_b = y == 10 or x == 12
            px[x, y] = rgba(EXO8[0] if edge_t else EXO8[5] if edge_b else glow[7] if y == 4 and x < 8 else glow[6] if y < 6 else glow[5] if y < 8 else glow[4])
    if icon:
        for j, row in enumerate(icon):
            for i, ch in enumerate(row):
                if ch == "#" and 5 <= 5 + i <= 10 and 5 + j <= 8:
                    pass
        # the 7x5 symbol scaled into the 6x4 window: draw its pixels that fit, dark on the glow
        for j, row in enumerate(icon):
            for i, ch in enumerate(row):
                if ch == "#":
                    px[4 + i, 4 + j] = rgba(glow[1])
    for x in range(3, 13, 2):                               # pins
        px[x, 13] = rgba(ELECTRUM8[6]); px[x, 14] = rgba(ELECTRUM8[3])

    return img



def filter_card_item():
    """The Filter Card: the upgrade-card design in teal with a funnel."""
    return card_item("#1fa79a", ["#######", ".#####.", "..###..", "...#...", "...#..."])


def upgrade_card(board, icon, icon_color=None):
    """An upgrade card in its own colour with its own symbol."""
    return card_item(board[3], icon)


def basic_circuit_item():
    return circuit_item("#2e7a33", ramp8("#3a3a40"), "#d08a3a")


def advanced_circuit_item():
    return circuit_item("#244a8c", ramp8("#c9a640"), "#e0c060", True)


def module_item(art=None):
    """A module cartridge; its window glows in the colour of what it fits."""
    if art is None:
        return module_cartridge("#7fe8ff", None)
    kind, icon = art
    return module_cartridge(MODULE_COLOR[kind], icon)



def _cylinder_rows(lv, x0, x1, y0, y1, rows=True):
    """The wire item's shading for an upright cylinder: brightest a little left of centre, darker to the
    right, edges darker; with {@code rows}, alternate lit and shaded rows like turns of wire."""
    import math
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            u = (x - x0) / max(1, x1 - x0)
            light = math.cos((u - 0.3) * math.pi * 0.9)
            bump = ((0.8 if (y - y0) % 2 == 0 else -0.6) if rows else 0)
            lv[y][x] = max(1, min(7, int(round(2 + light * 4.2 + bump))))
        lv[y][x0] = max(1, lv[y][x0] - 2)
        lv[y][x1] = 1


def _ellipse(lv, cx, cy, rx, ry, top, rim):
    """An upright cylinder's flat top: an ellipse lit on its upper left, with a darker rim."""
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1:
                edge = d > 0.55
                lv[y][x] = (rim if edge and (y > cy or x > cx) else top - (1 if x > cx + rx * 0.4 else 0))


def coil_item(metal):
    """A coil, drawn like the wire spools: the metal's wire wound in rounded rows round a steel core, the
    core's end showing on top, and two leads trailing off the bottom."""
    mt = _mt()
    core, wound = mt.blank(), mt.blank()
    _cylinder_rows(wound, 3, 12, 4, 12)
    _ellipse(wound, 7.5, 3.6, 4.9, 1.5, 6, 3)               # the top turn, seen from above
    _ellipse(core, 7.5, 3.5, 1.7, 0.8, 7, 2)                # the steel core's end
    for y in (2, 3):
        core[y][7] = 6; core[y][8] = 4
    for x, y, v in ((12, 13, 4), (13, 14, 3), (5, 13, 5), (4, 14, 4)):   # leads
        wound[y][x] = v
    out = mt.paint8(wound, metal)
    out.alpha_composite(mt.paint8(core, "steel"))
    return out


def motor_item():
    """An electric motor, drawn like the wire spools: an upright steel can shaded as a cylinder with
    cooling ribs, a band of copper windings showing through its middle, a flat top with the shaft
    sticking up, and a steel foot."""
    mt = _mt()
    can, band, shaft = mt.blank(), mt.blank(), mt.blank()
    _cylinder_rows(can, 2, 13, 5, 13, rows=False)
    for y in (6, 12):                                       # cooling ribs
        for x in range(3, 13):
            can[y][x] = max(1, can[y][x] - 2)
    _cylinder_rows(band, 2, 13, 8, 10)                      # copper windings in a slot round the middle
    _ellipse(can, 7.5, 4.6, 5.9, 1.6, 6, 3)                 # the top face
    for y in range(1, 5):                                   # the shaft
        shaft[y][7] = 7 if y < 4 else 6
        shaft[y][8] = 4 if y < 4 else 3
    for x in range(1, 15):                                  # the foot
        can[14][x] = 2 if x > 3 else 3
    img = mt.paint8(can, "steel")
    img.alpha_composite(mt.paint8(band, "copper"))
    img.alpha_composite(mt.paint8(shaft, "steel"))
    return img


def casing_item(face):
    """A machine casing: the tier's machine frame with its middle open, showing the dark inside."""
    img = face()
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            px[x, y] = rgba(EXO8[0] if (x == 4 or y == 4) else EXO8[2] if (x + y) % 3 else EXO8[1])
    for x in range(4, 12):
        px[x, 12] = rgba(STEEL8[6])
    for y in range(4, 13):
        px[12, y] = rgba(STEEL8[6])
    return img


# ------------------------------------------------------------------ tiers 4 and 5 (signal blue, plasma violet)
SIGNAL_TRIM = ["#0d2a5a", "#16428a", "#1f5fbf", "#3a86e8", "#6fb0f6", "#b8dcff"]
PLASMA_TRIM = ["#2e0b4a", "#4d1478", "#7322ad", "#9a3ee0", "#c27cf4", "#ecc8ff"]
SIGNAL8 = ramp8("#2f74d6")
PLASMA8 = ramp8("#8a34d0")


def casing4():
    return casing(STEEL, SIGNAL_TRIM)


def casing5():
    return casing(EXO_STEEL, PLASMA_TRIM)


EXO_STEEL = ["#161a20", "#232830", "#313842", "#404854", "#505a67", "#66717f", "#86919e", "#aeb7c2"]


def laser_engraver_front(on):
    """A dark engraving bed under a gantry; a red beam and a hot spot on the wafer while it works."""
    img = casing4()
    px = img.load()
    window(img, 3, 3, 12, 12)
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#121419")
    for x in range(3, 13):                               # the gantry rail
        px[x, 4] = rgba(STEEL8[6] if x < 9 else STEEL8[4])
        px[x, 5] = rgba(STEEL8[2])
    for (x, y) in ((7, 5), (8, 5), (7, 6), (8, 6)):     # the laser head
        px[x, y] = rgba(SIGNAL_TRIM[4] if x == 7 else SIGNAL_TRIM[2])
    for x in range(4, 12):                               # the wafer
        px[x, 11] = rgba("#9aa6b4" if x < 8 else "#6c7884")
        px[x, 10] = rgba("#c8d2dc" if x < 8 else "#8c98a6")
    if on:
        for y in range(7, 10):
            px[7, y] = rgba("#ff5a4a" if y < 9 else "#ffb0a0")
        px[7, 10] = rgba("#fff2c8")
        px[6, 10] = px[8, 10] = rgba("#ff8a5a")
    for x in range(3, 13):
        px[x, 12] = rgba(SIGNAL_TRIM[2])
    return img


def processor_item():
    return circuit_item("#1a2a52", ramp8("#7aa8e0"), "#9ac8f0", True)


def quantum_processor_item():
    img = circuit_item("#2a1240", ramp8("#b070f0"), "#e0b0ff", True)
    px = img.load()
    for (x, y) in ((7, 6), (8, 7), (7, 8), (6, 7)):      # a glowing core in the chip
        px[x, y] = rgba(PLASMA8[7] if (x, y) == (7, 6) else PLASMA8[6])
    return img


def tier45_textures():
    duct_window = ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]
    out = {
        "block/laser_engraver_front": laser_engraver_front(False),
        "block/laser_engraver_front_on": laser_engraver_front(True),
        "item/processor": processor_item(),
        "item/quantum_processor": quantum_processor_item(),
    }
    for tier, trim, trim8, case, sheath in (
            ("elite", SIGNAL_TRIM, SIGNAL8, casing4, ["#0e1a30", "#162642", "#203658", "#2c4870"]),
            ("ultimate", PLASMA_TRIM, PLASMA8, casing5, ["#1a0c28", "#28123c", "#381a52", "#4a246a"])):
        out.update({
            f"block/{tier}_energy_cable": cable_texture(trim, sheath, trim),
            f"block/{tier}_item_duct": pipe_texture(LEAD, trim, duct_window),
            f"block/{tier}_fluid_pipe": pipe_texture(TIN, trim, GLASS),
            f"block/{tier}_battery_front": battery_front(False, trim),
            f"block/{tier}_battery_front_on": battery_front(True, trim),
            f"block/{tier}_crate_side": crate_side(trim),
            f"block/{tier}_crate_top": crate_top(trim),
            f"block/{tier}_bulk_bin_side": bin_side(trim),
            f"block/{tier}_bulk_bin_front": bin_front(trim),
            f"block/{tier}_fluid_tank_side": tank_side(trim),
            f"block/{tier}_fluid_tank_top": tank_top(trim),
            f"block/{tier}_solar_panel_top": solar_top(trim),
            f"block/{tier}_solar_panel_side": case(),
            f"item/{tier}_casing": casing_item(case),
            f"item/{tier}_energy_cell": energy_cell_item(trim8),
            f"item/{tier}_upgrade_kit": upgrade_kit_item(trim8),
        })
    return out


# ------------------------------------------------------------------ the Fission Reactor
URANIUM_GLOW = ["#0f2a0c", "#1d4a16", "#2f7a22", "#4fb832", "#8ef060", "#d8ffb8"]
HAZARD = ["#2a2206", "#5c4a0c", "#b8920f", "#e8c21c", "#f8dc4a"]


def reactor_casing():
    """Heavy lead-grey plating with a yellow-and-black hazard band near the bottom."""
    img = casing(LEAD, SIGNAL_TRIM)
    px = img.load()
    for x in range(2, 14):
        for y in (11, 12):
            px[x, y] = rgba(HAZARD[3] if (x + y) // 2 % 2 else "#1a1a1e")
    for x in range(2, 14):
        px[x, 10] = rgba(LEAD[1]); px[x, 13] = rgba(LEAD[6])
    return img


def reactor_glass():
    """A thick lead frame round a tinted window; the middle is see-through."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d == 0:
                px[x, y] = rgba(LEAD[6] if x + y < 15 else LEAD[1])
            elif d == 1:
                px[x, y] = rgba(LEAD[4] if x + y < 15 else LEAD[2])
            elif (x - y) in (0, 1) and 4 <= x <= 11:
                px[x, y] = (200, 255, 210, 110)                     # a glint
            else:
                px[x, y] = (60, 110, 80, 60)
    for cx, cy in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[cx, cy] = rgba(SIGNAL_TRIM[3])
    return img


def reactor_port():
    """Reactor casing with a round socket: a copper ring round a dark hole with three contacts."""
    img = casing(LEAD, SIGNAL_TRIM)
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 3.6 <= r < 5.2:
                px[x, y] = rgba(COPPER[4] if x + y < 15 else COPPER[1])
            elif r < 3.6:
                px[x, y] = rgba("#121418")
    for (x, y) in ((7, 5), (5, 9), (10, 9)):
        px[x, y] = rgba(STEEL8[7])
    return img


def fission_reactor_front(on):
    """The controller: a status screen above a row of control rods; the screen and core glow green while running."""
    img = casing(LEAD, SIGNAL_TRIM)
    px = img.load()
    window(img, 3, 3, 12, 8)
    for y in range(3, 9):
        for x in range(3, 13):
            px[x, y] = rgba("#0d1210")
    if on:
        for x in range(4, 12):                               # a power trace
            y = 6 - (1 if x % 3 == 0 else 0) + (1 if x % 4 == 1 else 0)
            px[x, y] = rgba(URANIUM_GLOW[4])
        px[11, 4] = rgba(URANIUM_GLOW[5])
    else:
        for x in range(4, 12):
            px[x, 6] = rgba(URANIUM_GLOW[1])
    for i, x in enumerate((4, 6, 8, 10)):                    # control rods
        top = 10 if on else 11
        for y in range(top, 13):
            px[x, y] = rgba(STEEL8[6]); px[x + 1, y] = rgba(STEEL8[3])
        px[x, top - 1] = rgba(URANIUM_GLOW[4] if on else HAZARD[3])
        px[x + 1, top - 1] = rgba(URANIUM_GLOW[3] if on else HAZARD[2])
    return img


def reactor_textures():
    return {
        "block/reactor_casing": reactor_casing(),
        "block/reactor_glass": reactor_glass(),
        "block/reactor_port": reactor_port(),
        "block/fission_reactor_front": fission_reactor_front(False),
        "block/fission_reactor_front_on": fission_reactor_front(True),
    }


# ------------------------------------------------------------------ wireless power
def wireless_side(transmitter):
    """Signal-blue casing with a dark panel: rising arcs for a Transmitter, falling arcs for a Receiver.
    The lamp strip at the top is a separate overlay, tinted to the channel colour."""
    img = casing4()
    px = img.load()
    for y in range(6, 13):
        for x in range(3, 13):
            px[x, y] = rgba("#10141c")
    for x in range(3, 13):
        px[x, 5] = rgba(STEEL8[2]); px[x, 13] = rgba(STEEL8[6])
    cx, cy = 7.5, (12.5 if transmitter else 6.0)
    for y in range(6, 13):
        for x in range(3, 13):
            r = math.hypot(x - cx, (y - cy) * 1.2)
            if any(abs(r - k) < 0.5 for k in (3.0, 5.5)) and ((y < cy - 1) if transmitter else (y > cy + 1)):
                px[x, y] = rgba(SIGNAL_TRIM[4] if r < 4 else SIGNAL_TRIM[2])
    sy = 11 if transmitter else 7                        # the emitter / pickup
    for x in (7, 8):
        px[x, sy] = rgba(SIGNAL_TRIM[5]); px[x, sy + (1 if transmitter else -1)] = rgba(STEEL8[5])
    return img


def wireless_lamp():
    """The channel lamp: a light-grey strip (tinted by the game) along the top of each side."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(4, 12):
        px[x, 3] = (255, 255, 255, 255) if x < 9 else (215, 215, 215, 255)
        px[x, 4] = (190, 190, 190, 255)
    return img


def wireless_top(transmitter):
    """A dish (Transmitter) or a grid of pickup pads (Receiver) on top."""
    img = casing4()
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if transmitter:
                if r < 4.6:
                    px[x, y] = rgba(STEEL8[6] if x + y < 14 else STEEL8[3])
                if r < 1.2:
                    px[x, y] = rgba(SIGNAL_TRIM[5])
            elif 4 <= x <= 11 and 4 <= y <= 11:
                px[x, y] = rgba(SIGNAL_TRIM[3] if (x // 2 + y // 2) % 2 else SIGNAL_TRIM[1])
    return img


def wireless_textures():
    return {
        "block/wireless_transmitter_side": wireless_side(True),
        "block/wireless_receiver_side": wireless_side(False),
        "block/wireless_transmitter_top": wireless_top(True),
        "block/wireless_receiver_top": wireless_top(False),
        "block/wireless_lamp": wireless_lamp(),
    }


# ------------------------------------------------------------------ tier 4 gear
def omni_tool_item():
    """A multitool corner to corner: a rubber grip, a signal-blue body with a glowing strip, a curved
    steel pick-and-axe head and a short gold drill tip."""
    grip = _diag(lambda a, c: -7.2 <= a <= -2.6 and abs(c) <= 1.1)
    body = _diag(lambda a, c: -2.6 < a <= 2.0 and abs(c) <= 1.6)
    axe = _diag(lambda a, c: -5.6 <= c <= 0 and 0.8 + max(0, -c - 3) * 0.6 <= a <= 3.6 - c * 0.5)   # a broad axe blade
    pick = _diag(lambda a, c: 0 < c <= 6.4 and 1.2 + c * 0.35 <= a <= 3.8 + c * 0.05)              # a curved pick spike
    head = [[axe[y][x] or pick[y][x] for x in range(16)] for y in range(16)]
    tip = _diag(lambda a, c: 3.4 < a <= 6.4 and abs(c) <= max(0.5, (6.8 - a) * 0.4) and abs(c) < 1.2)
    img = _parts((grip, RUBBER8, 2, 6), (body, SIGNAL8, 2, 7), (tip, ELECTRUM8, 3, 7), (head, STEEL8, 3, 7))
    px = img.load()
    strip = _diag(lambda a, c: -2.2 <= a <= 1.4 and abs(c) <= 0.4)
    for y in range(16):
        for x in range(16):
            if strip[y][x]:
                px[x, y] = rgba(ENERGY8[6] if (x + y) % 3 else ENERGY8[4])
    return img


def railgun_item():
    """A rail gun: two steel rails with copper coil bands and a glowing bore between them, on a
    signal-blue receiver with a grip and a short stock."""
    rails = _diag(lambda a, c: -0.8 <= a <= 7.0 and 0.9 <= abs(c + 0.4) <= 1.9)
    bore = _diag(lambda a, c: -0.8 <= a <= 6.6 and abs(c + 0.4) < 0.9)
    body = _diag(lambda a, c: -5.2 <= a < -0.8 and abs(c + 0.4) <= 2.4)
    stock = _diag(lambda a, c: -7.6 <= a < -5.2 and -1.4 <= c + 0.4 <= 1.8)
    grip = _diag(lambda a, c: -4.6 <= a <= -2.8 and 2.4 < c + 0.4 <= 5.6)
    img = _parts((grip, RUBBER8, 2, 6), (stock, RUBBER8, 2, 6), (body, SIGNAL8, 2, 7), (rails, STEEL8, 3, 7))
    px = img.load()
    bands = _diag(lambda a, c: -0.8 <= a <= 6.0 and 0.9 <= abs(c + 0.4) <= 1.9 and int(a + 10) % 3 == 0)
    for y in range(16):
        for x in range(16):
            if bore[y][x]:
                px[x, y] = rgba(ENERGY8[7] if (x + y) % 3 == 0 else ENERGY8[4])
            if bands[y][x]:
                px[x, y] = rgba(COPPER8[6] if x + y < 15 else COPPER8[3])
    return img


def tier4_gear_textures():
    return {"item/omni_tool": omni_tool_item(), "item/railgun": railgun_item()}


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
        "block/reinforced_energy_cable": cable_texture(COPPER, ["#141414", "#1f1f21", "#2c2c30", "#3c3c42"], STEEL_TRIM),
        "block/advanced_energy_cable": cable_texture(ELECTRUM_TRIM, ["#8a8f94", "#a6abaf", "#c6cacd", "#dcdfe1"], ELECTRUM_TRIM),
        "block/advanced_item_duct": pipe_texture(LEAD, ELECTRUM_TRIM, ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]),
        "block/advanced_fluid_pipe": pipe_texture(TIN, ELECTRUM_TRIM, GLASS),
        "block/reinforced_battery_front": battery_front(False, STEEL_TRIM),
        "block/reinforced_battery_front_on": battery_front(True, STEEL_TRIM),
        "block/advanced_battery_front": battery_front(False, ELECTRUM_TRIM),
        "block/advanced_battery_front_on": battery_front(True, ELECTRUM_TRIM),
        "block/boiler_front": boiler_front(False),
        "block/boiler_front_on": boiler_front(True),
        "block/steam_turbine_front": turbine_front(False),
        "block/steam_turbine_front_on": turbine_front(True),
        "item/plastic_sheet": plastic_sheet_item(),
        "item/advanced_circuit": advanced_circuit_item(),
        "block/sensor_side": sensor_side(),
        "block/sensor_front": sensor_front(),
        **{f"block/{k}_sensor_back{'_on' if on else ''}": sensor_back(k, on) for k in SENSOR_ICONS for on in (False, True)},
        "block/logic_controller": logic_controller_side(),
        "block/logic_controller_top": logic_controller_top(),
        **{f"block/{name}_front{'_on' if on else ''}": fn(on)
           for name, fn in (("block_placer", block_placer_front), ("block_breaker", block_breaker_front),
                            ("autocrafter", autocrafter_front), ("fluid_placer", fluid_placer_front),
                            ("fluid_drain", lambda on: fluid_placer_front(on, True)),
                            ("mechanical_user", mechanical_user_front), ("quarry", quarry_front),
                            ("mob_fighter", mob_fighter_front))
           for on in (False, True)},
        "block/item_duct": pipe_texture(LEAD, COPPER, ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]),
        "block/reinforced_item_duct": pipe_texture(LEAD, STEEL_TRIM, ["#0c0d12", "#1b1d26", "#2a2d3a", "#4a5066", "#9aa3c0"]),
        "block/fluid_pipe": pipe_texture(TIN, COPPER, GLASS),
        "block/reinforced_fluid_pipe": pipe_texture(TIN, STEEL_TRIM, GLASS),
        "block/pipe_attachment": pipe_attachment(),
        "block/fluid_tank_side": tank_side(),
        "block/fluid_tank_top": tank_top(),
        "block/pump_front": pump_front(False),
        "block/pump_front_on": pump_front(True),
        "block/advanced_pump_front": pump_front(False, casing3),
        "block/advanced_pump_front_on": pump_front(True, casing3),
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
        "item/copper_coil": coil_item("copper"),
        "item/electrum_coil": coil_item("electrum"),
        "item/motor": motor_item(),
        "item/basic_casing": casing_item(casing),
        "item/reinforced_casing": casing_item(casing2),
        "item/advanced_casing": casing_item(casing3),
        "block/rubber_log_spot_dry": rubber_spot("dry"),
        "block/rubber_log_spot_wet": rubber_spot("wet"),
        "item/latex": latex_item(),
        "item/rubber": rubber_item(),
        "item/tree_tap": tree_tap_item(),
        "item/steel_hammer": hammer_item(),
        "item/steel_excavator": excavator_item(),
        "block/basic_crate_side": crate_side(COPPER),
        "block/basic_crate_top": crate_top(COPPER),
        "block/reinforced_crate_side": crate_side(STEEL_TRIM),
        "block/reinforced_crate_top": crate_top(STEEL_TRIM),
        "block/basic_bulk_bin_side": bin_side(COPPER),
        "block/basic_bulk_bin_front": bin_front(COPPER),
        "block/reinforced_bulk_bin_side": bin_side(STEEL_TRIM),
        "block/reinforced_bulk_bin_front": bin_front(STEEL_TRIM),
        "gui/crate": crate_gui(),
        "block/conveyor_belt_top": conveyor_top(),
        "block/conveyor_belt_side": conveyor_side(),
        **{f"block/{n}_front{'_on' if on else ''}": fn(on)
           for n, fn in (("fertilizer_mixer", fertilizer_mixer_front), ("animal_rancher", animal_rancher_front),
                         ("hydroponic_bed", hydroponic_bed_front), ("mob_grinder", mob_grinder_front),
                         ("auto_enchanter", auto_enchanter_front)) for on in (False, True)},
        "block/sprinkler": sprinkler_texture(),
        "item/fertilizer": fertilizer_item(),
        **{f"block/{n}_front{'_on' if on else ''}": fn(on)
           for n, fn in (("geothermal_generator", geothermal_front), ("wind_turbine", wind_turbine_front)) for on in (False, True)},
        "block/solar_panel_top": solar_top(COPPER),
        "block/reinforced_solar_panel_top": solar_top(STEEL_TRIM),
        "block/advanced_solar_panel_top": solar_top(ELECTRUM_TRIM),
        "block/solar_panel_side": casing(),
        "block/reinforced_solar_panel_side": casing2(),
        "block/advanced_solar_panel_side": casing3(),
        **{f"gui/crate_{c}": crate_gui(c) for c in (12, 15, 18)},
        "block/advanced_crate_side": crate_side(ELECTRUM_TRIM),
        "block/advanced_crate_top": crate_top(ELECTRUM_TRIM),
        "block/advanced_bulk_bin_side": bin_side(ELECTRUM_TRIM),
        "block/advanced_bulk_bin_front": bin_front(ELECTRUM_TRIM),
        "block/reinforced_fluid_tank_side": tank_side(STEEL_TRIM),
        "block/reinforced_fluid_tank_top": tank_top(STEEL_TRIM),
        "block/advanced_fluid_tank_side": tank_side(ELECTRUM_TRIM),
        "block/advanced_fluid_tank_top": tank_top(ELECTRUM_TRIM),
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
        **{f"block/{n}_front{'_on' if on else ''}": fn(on)
           for n, fn in (("chemical_dissolver", chemical_dissolver_front), ("crystallizer", crystallizer_front)) for on in (False, True)},
        "item/power_drill": power_drill_item(),
        "item/chainsaw": chainsaw_item(),
        "item/energy_sword": energy_sword_item(),
        "item/energy_cell": energy_cell_item(),
        "item/basic_energy_cell": energy_cell_item(COPPER8),
        "item/reinforced_energy_cell": energy_cell_item(STEEL8),
        "item/reinforced_upgrade_kit": upgrade_kit_item(STEEL8),
        "item/advanced_upgrade_kit": upgrade_kit_item(ELECTRUM8),
        "item/blank_module": module_item(),
        **{f"item/{m}_module": module_item(art) for m, art in MODULE_ART.items()},
        "block/charging_pad_top": charging_pad_top(False),
        "block/charging_pad_top_on": charging_pad_top(True),
        "block/modification_table_top": modification_table_top(),
        "block/modification_table_side": modification_table_side(False),
        "block/modification_table_front": modification_table_side(True),
        "gui/modification_table": modification_table_gui(),
        **tier45_textures(),
        **reactor_textures(),
        **wireless_textures(),
        **tier4_gear_textures(),
    }
    for name, img in out.items():
        img.save(os.path.join(TEX, name + ".png"))
    print(f"{len(out)} machine textures written")


if __name__ == "__main__":
    main()
