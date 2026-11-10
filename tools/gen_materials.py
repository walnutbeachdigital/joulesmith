#!/usr/bin/env python3
"""Writes every JSON file for Joulesmith's metals: blockstates, models, loot tables,
tags, recipes, worldgen and English names.

Keep METALS in step with material/Metal.java. Run from the repo root:
    python3 tools/gen_materials.py
Files it owns are rewritten each run; it never touches anything else.
"""
import json
import os
import shutil

MOD = "joulesmith"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MOD)
DATA = os.path.join(RES, "data")

# name: (source, dust, plate, gear, wire)
METALS = {
    "iron":     ("vanilla", True, True, True, False),
    "copper":   ("vanilla", True, True, True, True),
    "gold":     ("vanilla", True, True, True, True),
    "tin":      ("ore",     True, True, True, False),
    "lead":     ("ore",     True, True, True, False),
    "silver":   ("ore",     True, True, True, True),
    "nickel":   ("ore",     True, True, True, False),
    "uranium":  ("ore",     True, False, False, False),
    "steel":    ("alloy",   True, True, True, True),
    "bronze":   ("alloy",   True, True, True, False),
    "invar":    ("alloy",   True, True, True, False),
    "electrum": ("alloy",   True, True, True, True),
}

# Ore generation: vein size, veins per chunk, min y, max y, tool tier, smelt xp.
ORES = {
    "tin":     dict(size=9, count=10, min_y=-16, max_y=96, tool="stone", xp=0.7),
    "lead":    dict(size=8, count=6,  min_y=-48, max_y=32, tool="iron",  xp=0.7),
    "silver":  dict(size=7, count=5,  min_y=-64, max_y=32, tool="iron",  xp=1.0),
    "nickel":  dict(size=7, count=5,  min_y=-32, max_y=48, tool="iron",  xp=1.0),
    "uranium": dict(size=4, count=3,  min_y=-64, max_y=-8, tool="iron",  xp=1.0),
}

VANILLA_INGOT = {"iron": "minecraft:iron_ingot", "copper": "minecraft:copper_ingot", "gold": "minecraft:gold_ingot"}

OWNED_DIRS = [
    os.path.join(ASSETS, "blockstates"),
    os.path.join(ASSETS, "models"),
    os.path.join(DATA, MOD, "loot_table"),
    os.path.join(DATA, MOD, "recipe"),
    os.path.join(DATA, MOD, "worldgen"),
    os.path.join(DATA, MOD, "neoforge"),
    os.path.join(DATA, "c", "tags"),
    os.path.join(DATA, "minecraft", "tags"),
]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def title(s):
    return " ".join(w.capitalize() for w in s.split("_"))


def ingot_of(m):
    return VANILLA_INGOT.get(m, f"{MOD}:{m}_ingot")


# ---------------------------------------------------------------- collect ids
blocks = []        # (id, kind, metal)
items = []         # (id, form, metal)
for m, (src, dust, plate, gear, wire) in METALS.items():
    if src == "ore":
        blocks += [(f"{m}_ore", "ore", m), (f"deepslate_{m}_ore", "deepslate_ore", m), (f"raw_{m}_block", "raw_block", m)]
        items.append((f"raw_{m}", "raw", m))
    if src != "vanilla":
        blocks.append((f"{m}_block", "storage", m))
        items += [(f"{m}_ingot", "ingot", m), (f"{m}_nugget", "nugget", m)]
    if dust:
        items.append((f"{m}_dust", "dust", m))
    if plate:
        items.append((f"{m}_plate", "plate", m))
    if gear:
        items.append((f"{m}_gear", "gear", m))
    if wire:
        items.append((f"{m}_wire", "wire", m))

for d in OWNED_DIRS:
    shutil.rmtree(d, ignore_errors=True)

# ---------------------------------------------------------- blockstates/models
SIDES = ["down", "up", "north", "south", "west", "east"]


def layered(base):
    faces = lambda tex: {s: {"uv": [0, 0, 16, 16], "texture": tex, "cullface": s} for s in SIDES}
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"particle": base, "base": base},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#base")},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#overlay")},
        ],
    }


# Forms whose textures are made while the game loads, by MetalTexturePack: Minecraft's own iron
# ingot (from copper, for its shiny top), iron nugget, raw iron and raw iron block, palette-swapped onto each
# metal's gradient. They are referenced here but never stored in the repo.
GENERATED_ITEM_FORMS = {"ingot", "nugget", "raw"}
GENERATED_BLOCK_KINDS = {"raw_block"}


for bid, kind, m in blocks:
    write(os.path.join(ASSETS, "blockstates", f"{bid}.json"), {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})
    if kind in ("ore", "deepslate_ore"):
        model = layered("minecraft:block/stone" if kind == "ore" else "minecraft:block/deepslate")
        model["textures"]["overlay"] = f"{MOD}:block/{bid}_overlay"
    else:
        model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{bid}"}}
    write(os.path.join(ASSETS, "models", "block", f"{bid}.json"), model)
    write(os.path.join(ASSETS, "models", "item", f"{bid}.json"), {"parent": f"{MOD}:block/{bid}"})

for iid, form, m in items:
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{iid}"}})

# ----------------------------------------------------------------- loot tables
for bid, kind, m in blocks:
    if kind in ("ore", "deepslate_ore"):
        entry = {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:{bid}", "conditions": [{
                "condition": "minecraft:match_tool",
                "predicate": {"predicates": {"minecraft:enchantments": [
                    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
            {"type": "minecraft:item", "name": f"{MOD}:raw_{m}", "functions": [
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]},
        ]}
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
    else:
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{bid}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}
    write(os.path.join(DATA, MOD, "loot_table", "blocks", f"{bid}.json"),
          {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{bid}"})

# ------------------------------------------------------------------------ tags
tags = {}  # (namespace, registry, path) -> list


def tag(ns, reg, path, value):
    tags.setdefault((ns, reg, path), [])
    if value not in tags[(ns, reg, path)]:
        tags[(ns, reg, path)].append(value)


def both(path, value):
    tag("c", "block", path, value)
    tag("c", "item", path, value)


for bid, kind, m in blocks:
    tag("minecraft", "block", "mineable/pickaxe", f"{MOD}:{bid}")
    if kind in ("ore", "deepslate_ore"):
        tag("minecraft", "block", f"needs_{ORES[m]['tool']}_tool", f"{MOD}:{bid}")
        both(f"ores/{m}", f"{MOD}:{bid}")
        both("ores", f"#c:ores/{m}")
        both("ores_in_ground/stone" if kind == "ore" else "ores_in_ground/deepslate", f"{MOD}:{bid}")
        both("ore_rates/singular", f"{MOD}:{bid}")
    else:
        tag("minecraft", "block", "needs_stone_tool", f"{MOD}:{bid}")
        sub = f"raw_{m}" if kind == "raw_block" else m
        both(f"storage_blocks/{sub}", f"{MOD}:{bid}")
        both("storage_blocks", f"#c:storage_blocks/{sub}")

FORM_TAG = {"raw": "raw_materials", "ingot": "ingots", "nugget": "nuggets", "dust": "dusts",
            "plate": "plates", "gear": "gears", "wire": "wires"}
for iid, form, m in items:
    group = FORM_TAG[form]
    tag("c", "item", f"{group}/{m}", f"{MOD}:{iid}")
    tag("c", "item", group, f"#c:{group}/{m}")

for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})

# --------------------------------------------------------------------- recipes
recipes = {}


def ing(t):
    return {"tag": t[1:]} if t.startswith("#") else {"item": t}


def cook(name, ingredient, result, xp):
    for kind, time in (("smelting", 200), ("blasting", 100)):
        recipes[f"{name}_from_{kind}"] = {
            "type": f"minecraft:{kind}", "category": "misc", "cookingtime": time,
            "experience": xp, "ingredient": ing(ingredient), "result": {"id": result}}


def compress(name, ingredient, result):
    recipes[name] = {"type": "minecraft:crafting_shaped", "category": "misc",
                     "pattern": ["###", "###", "###"], "key": {"#": ing(ingredient)},
                     "result": {"id": result, "count": 1}}


def unpack(name, ingredient, result, count=9):
    recipes[name] = {"type": "minecraft:crafting_shapeless", "category": "misc",
                     "ingredients": [ing(ingredient)], "result": {"id": result, "count": count}}


for m, (src, dust, plate, gear, wire) in METALS.items():
    ingot = ingot_of(m)
    if src == "ore":
        xp = ORES[m]["xp"]
        cook(f"{m}_ingot_from_raw", f"#c:raw_materials/{m}", ingot, xp)
        cook(f"{m}_ingot_from_ore", f"#c:ores/{m}", ingot, xp)
        compress(f"raw_{m}_block", f"#c:raw_materials/{m}", f"{MOD}:raw_{m}_block")
        unpack(f"raw_{m}_from_block", f"#c:storage_blocks/raw_{m}", f"{MOD}:raw_{m}")
    if src != "vanilla":
        compress(f"{m}_block", f"#c:ingots/{m}", f"{MOD}:{m}_block")
        unpack(f"{m}_ingot_from_block", f"#c:storage_blocks/{m}", ingot)
        compress(f"{m}_ingot_from_nuggets", f"#c:nuggets/{m}", ingot)
        unpack(f"{m}_nugget", f"#c:ingots/{m}", f"{MOD}:{m}_nugget")
    if dust:
        cook(f"{m}_ingot_from_dust", f"#c:dusts/{m}", ingot, 0.1)
    if gear:
        recipes[f"{m}_gear"] = {"type": "minecraft:crafting_shaped", "category": "misc",
                                "pattern": [" # ", "#n#", " # "],
                                "key": {"#": ing(f"#c:ingots/{m}"), "n": ing("#c:nuggets/iron")},
                                "result": {"id": f"{MOD}:{m}_gear", "count": 1}}

for name, r in recipes.items():
    write(os.path.join(DATA, MOD, "recipe", f"{name}.json"), r)

# -------------------------------------------------------------------- worldgen
for m, o in ORES.items():
    write(os.path.join(DATA, MOD, "worldgen", "configured_feature", f"ore_{m}.json"), {
        "type": "minecraft:ore",
        "config": {"size": o["size"], "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
             "state": {"Name": f"{MOD}:{m}_ore"}},
            {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
             "state": {"Name": f"{MOD}:deepslate_{m}_ore"}}]}})
    write(os.path.join(DATA, MOD, "worldgen", "placed_feature", f"ore_{m}.json"), {
        "feature": f"{MOD}:ore_{m}",
        "placement": [
            {"type": "minecraft:count", "count": o["count"]},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:trapezoid",
                "min_inclusive": {"absolute": o["min_y"]}, "max_inclusive": {"absolute": o["max_y"]}}},
            {"type": "minecraft:biome"}]})
    write(os.path.join(DATA, MOD, "neoforge", "biome_modifier", f"add_{m}_ore.json"), {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": f"{MOD}:ore_{m}", "step": "underground_ores"})

# ------------------------------------------------------------------------ lang
lang = {"itemGroup.joulesmith": "Joulesmith"}
for bid, kind, m in blocks:
    lang[f"block.{MOD}.{bid}"] = {
        "ore": f"{title(m)} Ore", "deepslate_ore": f"Deepslate {title(m)} Ore",
        "raw_block": f"Block of Raw {title(m)}", "storage": f"Block of {title(m)}"}[kind]
for iid, form, m in items:
    lang[f"item.{MOD}.{iid}"] = f"Raw {title(m)}" if form == "raw" else title(iid)
TOOL_NAME = {"stone": "a stone", "iron": "an iron"}
for m, o in ORES.items():
    lang[f"jei.{MOD}.info.ore.{m}"] = (
        f"Found in the Overworld between Y {o['min_y']} and Y {o['max_y']}. "
        f"Mine it with {TOOL_NAME[o['tool']]} pickaxe or better, then smelt or blast the raw {m} into ingots.")
write(os.path.join(ASSETS, "lang", "en_us.json"), lang)

# -------------------------------------------------------------------- machines
# Machine blocks share one casing (machine_side/top/bottom) and differ by their front.
MACHINES = {
    "electric_furnace": {"name": "Electric Furnace", "front": "electric_furnace_front", "lit": True},
}
CREATIVE = {"creative_power_cell": "Creative Power Cell"}
ROTATION = {"north": 0, "east": 90, "south": 180, "west": 270}
for mid, info in MACHINES.items():
    variants = {}
    for facing, rot in ROTATION.items():
        for lit in (False, True):
            model = f"{MOD}:block/{mid}_on" if lit else f"{MOD}:block/{mid}"
            v = {"model": model}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},lit={'true' if lit else 'false'}"] = v
    write(os.path.join(ASSETS, "blockstates", f"{mid}.json"), {"variants": variants})
    for suffix, front in (("", info["front"]), ("_on", info["front"] + "_on")):
        write(os.path.join(ASSETS, "models", "block", f"{mid}{suffix}.json"), {
            "parent": "minecraft:block/orientable_with_bottom",
            "textures": {"top": f"{MOD}:block/machine_top", "bottom": f"{MOD}:block/machine_bottom",
                         "side": f"{MOD}:block/machine_side", "front": f"{MOD}:block/{front}"}})
    write(os.path.join(ASSETS, "models", "item", f"{mid}.json"), {"parent": f"{MOD}:block/{mid}"})
    write(os.path.join(DATA, MOD, "loot_table", "blocks", f"{mid}.json"), {
        "type": "minecraft:block", "random_sequence": f"{MOD}:blocks/{mid}",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{mid}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tag("minecraft", "block", "mineable/pickaxe", f"{MOD}:{mid}")
for cid in CREATIVE:
    write(os.path.join(ASSETS, "blockstates", f"{cid}.json"), {"variants": {"": {"model": f"{MOD}:block/{cid}"}}})
    write(os.path.join(ASSETS, "models", "block", f"{cid}.json"),
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{cid}"}})
    write(os.path.join(ASSETS, "models", "item", f"{cid}.json"), {"parent": f"{MOD}:block/{cid}"})
# rewrite the machine tags into their files (the tag files were written above)
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})
write(os.path.join(DATA, MOD, "recipe", "electric_furnace.json"), {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["IGI", "GFG", "IRI"],
    "key": {"I": {"tag": "c:ingots/iron"}, "G": {"tag": "c:gears/copper"},
            "F": {"item": "minecraft:furnace"}, "R": {"tag": "c:dusts/redstone"}},
    "result": {"id": f"{MOD}:electric_furnace", "count": 1}})
recipes["electric_furnace"] = True
lang.update({f"block.{MOD}.{k}": v["name"] for k, v in MACHINES.items()})
lang.update({f"block.{MOD}.{k}": v for k, v in CREATIVE.items()})
lang.update({
    "gui.joulesmith.energy": "%s / %s FE",
    "gui.joulesmith.progress": "%s%% done",
    "gui.joulesmith.click_to_change": "Click to change",
    "gui.joulesmith.redstone.title": "Redstone control",
    "gui.joulesmith.redstone.ignored": "Always runs",
    "gui.joulesmith.redstone.high": "Runs with a redstone signal",
    "gui.joulesmith.redstone.low": "Runs without a redstone signal",
    "gui.joulesmith.redstone.never": "Never runs",
    "gui.joulesmith.sides.title": "Side configuration",
    "gui.joulesmith.sides.hint": "Left-click: next   Right-click: back",
    "gui.joulesmith.face.front": "Front", "gui.joulesmith.face.back": "Back",
    "gui.joulesmith.face.left": "Left", "gui.joulesmith.face.right": "Right",
    "gui.joulesmith.face.top": "Top", "gui.joulesmith.face.bottom": "Bottom",
    "gui.joulesmith.side_mode.none": "Off",
    "gui.joulesmith.side_mode.input": "Input: items can be put in",
    "gui.joulesmith.side_mode.output": "Output: results can be taken out",
    "gui.joulesmith.side_mode.both": "Input and output",
})
write(os.path.join(ASSETS, "lang", "en_us.json"), lang)

print(f"{len(blocks)} blocks, {len(items)} items, {len(recipes)} recipes, "
      f"{len(tags)} tags, {len(ORES)} ores")
# Texture list for the art script and for checks.
write(os.path.join(ROOT, "tools", "texture_manifest.json"), {
    "block": [f"{b}_overlay" for b, k, m in blocks if k in ("ore", "deepslate_ore")] +
             [b for b, k, m in blocks if k == "storage"],
    "item": [i for i, f, m in items if f not in GENERATED_ITEM_FORMS],
    "generated": [f"block/{b}" for b, k, m in blocks if k in GENERATED_BLOCK_KINDS] +
                 [f"item/{i}" for i, f, m in items if f in GENERATED_ITEM_FORMS]})
