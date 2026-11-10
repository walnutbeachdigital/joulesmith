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
    if src in ("ore", "vanilla"):
        items.append((f"crushed_{m}", "crushed", m))
        items.append((f"{m}_crystal", "crystal", m))

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
            "plate": "plates", "gear": "gears", "wire": "wires", "crushed": "crushed_ores",
            "crystal": "crystals"}
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
    lang[f"item.{MOD}.{iid}"] = (f"Raw {title(m)}" if form == "raw" else
                                 f"Crushed {title(m)} Ore" if form == "crushed" else title(iid))
TOOL_NAME = {"stone": "a stone", "iron": "an iron"}
for m, o in ORES.items():
    lang[f"jei.{MOD}.info.ore.{m}"] = (
        f"Found in the Overworld between Y {o['min_y']} and Y {o['max_y']}. "
        f"Mine it with {TOOL_NAME[o['tool']]} pickaxe or better, then smelt or blast the raw {m} into ingots.")
write(os.path.join(ASSETS, "lang", "en_us.json"), lang)

# -------------------------------------------------------------------- machines
# Machine blocks share one casing (machine_side/top/bottom) and differ by their front.
MACHINES = {
    "combustion_generator": {"name": "Combustion Generator", "front": "combustion_generator_front"},
    "basic_battery": {"name": "Basic Battery", "front": "basic_battery_front"},
    "reinforced_battery": {"name": "Reinforced Battery", "front": "reinforced_battery_front"},
    "advanced_battery": {"name": "Advanced Battery", "front": "advanced_battery_front"},
    "boiler": {"name": "Boiler", "front": "boiler_front"},
    "block_placer": {"name": "Block Placer", "front": "block_placer_front"},
    "block_breaker": {"name": "Block Breaker", "front": "block_breaker_front"},
    "autocrafter": {"name": "Autocrafter", "front": "autocrafter_front"},
    "fluid_placer": {"name": "Fluid Placer", "front": "fluid_placer_front"},
    "fluid_drain": {"name": "Fluid Drain", "front": "fluid_drain_front"},
    "mechanical_user": {"name": "Mechanical User", "front": "mechanical_user_front"},
    "quarry": {"name": "Quarry", "front": "quarry_front"},
    "mob_fighter": {"name": "Mob Fighter", "front": "mob_fighter_front"},
    "steam_turbine": {"name": "Steam Turbine", "front": "steam_turbine_front"},
    "electric_furnace": {"name": "Electric Furnace", "front": "electric_furnace_front"},
    "crusher": {"name": "Crusher", "front": "crusher_front"},
    "pump": {"name": "Pump", "front": "pump_front"},
    "alloy_smelter": {"name": "Alloy Smelter", "front": "alloy_smelter_front"},
    "compressor": {"name": "Compressor", "front": "compressor_front"},
    "wire_drawer": {"name": "Wire Drawer", "front": "wire_drawer_front"},
    "sawmill": {"name": "Sawmill", "front": "sawmill_front"},
    "ore_washer": {"name": "Ore Washer", "front": "ore_washer_front"},
    "fluid_mixer": {"name": "Fluid Mixer", "front": "fluid_mixer_front"},
    "centrifuge": {"name": "Centrifuge", "front": "centrifuge_front"},
    "circuit_assembler": {"name": "Circuit Assembler", "front": "circuit_assembler_front"},
    "planter": {"name": "Planter", "front": "planter_front"},
    "harvester": {"name": "Harvester", "front": "harvester_front"},
    "tree_farm": {"name": "Tree Farm", "front": "tree_farm_front"},
    "bio_reactor": {"name": "Bio Reactor", "front": "bio_reactor_front"},
    "biofuel_generator": {"name": "Biofuel Generator", "front": "biofuel_generator_front"},
    "chemical_dissolver": {"name": "Chemical Dissolver", "front": "chemical_dissolver_front"},
    "crystallizer": {"name": "Crystallizer", "front": "crystallizer_front"},
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
# ---- energy cable: a core plus an arm per connected side (multipart)
SIDE_ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}
CABLES = {"energy_cable": "Energy Cable", "reinforced_energy_cable": "Reinforced Energy Cable",
          "advanced_energy_cable": "Advanced Energy Cable"}
for cid in CABLES:
    cable_tex = f"{MOD}:block/{cid}"
    write(os.path.join(ASSETS, "models", "block", f"{cid}_core.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": cable_tex, "cable": cable_tex},
        "elements": [{"from": [6, 6, 6], "to": [10, 10, 10],
                      "faces": {f: {"texture": "#cable"} for f in SIDES}}]})
    write(os.path.join(ASSETS, "models", "block", f"{cid}_arm.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": cable_tex, "cable": cable_tex},
        "elements": [{"from": [6, 6, 0], "to": [10, 10, 6],
                      "faces": {f: {"texture": "#cable"} for f in ("east", "west", "up", "down", "north")}}]})
    write(os.path.join(ASSETS, "blockstates", f"{cid}.json"), {"multipart":
        [{"apply": {"model": f"{MOD}:block/{cid}_core"}}] +
        [{"when": {side: "true"}, "apply": {"model": f"{MOD}:block/{cid}_arm", **rot}} for side, rot in SIDE_ROT.items()]})
    write(os.path.join(ASSETS, "models", "item", f"{cid}.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": cable_tex, "cable": cable_tex},
        "elements": [{"from": [0, 6, 6], "to": [16, 10, 10], "faces": {f: {"texture": "#cable"} for f in SIDES}}]})
    write(os.path.join(DATA, MOD, "loot_table", "blocks", f"{cid}.json"), {
        "type": "minecraft:block", "random_sequence": f"{MOD}:blocks/{cid}",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{cid}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tags[("minecraft", "block", "mineable/pickaxe")].append(f"{MOD}:{cid}")
write(os.path.join(DATA, "minecraft", "tags", "block", "mineable/pickaxe.json"),
      {"replace": False, "values": tags[("minecraft", "block", "mineable/pickaxe")]})

# ---- item ducts and fluid pipes: shared parent models (built facing north), one texture per pipe
def model(name, obj):
    write(os.path.join(ASSETS, "models", "block", f"{name}.json"), obj)

def face(uv, rotation=0):
    f = {"uv": uv, "texture": "#pipe"}
    if rotation:
        f["rotation"] = rotation
    return f

def attach_faces(u0, depth):
    """The plate where a pipe meets a block: front and back from the attachment sheet, thin edges below."""
    return {"north": {"uv": [u0, 0, u0 + 8, 8], "texture": "#attach"},
            "south": {"uv": [u0, 0, u0 + 8, 8], "texture": "#attach"},
            "up": {"uv": [u0, 8, u0 + 8, 8 + depth], "texture": "#attach"},
            "down": {"uv": [u0, 8, u0 + 8, 8 + depth], "texture": "#attach"},
            "east": {"uv": [u0, 8, u0 + depth, 16], "texture": "#attach"},
            "west": {"uv": [u0, 8, u0 + depth, 16], "texture": "#attach"}}

def arm(z0, z1):
    length = z1 - z0
    return {"from": [5, 5, z0], "to": [11, 11, z1], "faces": {
        "east": face([0, 0, length, 6]), "west": face([0, 0, length, 6]),
        "up": face([0, 0, length, 6], 90), "down": face([0, 0, length, 6], 90)}}

ATTACH = f"{MOD}:block/pipe_attachment"
model("pipe_core", {"parent": "minecraft:block/block", "textures": {"particle": "#pipe"},
      "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "faces": {f: face([0, 6, 6, 12]) for f in SIDES}}]})
model("pipe_arm", {"parent": "minecraft:block/block", "textures": {"particle": "#pipe"}, "elements": [arm(0, 5)]})
model("pipe_push", {"parent": "minecraft:block/block", "textures": {"particle": "#pipe", "attach": ATTACH},
      "elements": [arm(2, 5), {"from": [4, 4, 0], "to": [12, 12, 2], "faces": attach_faces(0, 2)}]})
model("pipe_pull", {"parent": "minecraft:block/block", "textures": {"particle": "#pipe", "attach": ATTACH},
      "elements": [arm(3, 5), {"from": [4, 4, 0], "to": [12, 12, 3], "faces": attach_faces(8, 3)}]})

PIPES = {"item_duct": "Item Duct", "reinforced_item_duct": "Reinforced Item Duct", "advanced_item_duct": "Advanced Item Duct",
         "fluid_pipe": "Fluid Pipe", "reinforced_fluid_pipe": "Reinforced Fluid Pipe", "advanced_fluid_pipe": "Advanced Fluid Pipe"}
SIDE_ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270},
            "up": {"x": 270}, "down": {"x": 90}}

def simple_loot(bid, functions=None):
    entry = {"type": "minecraft:item", "name": f"{MOD}:{bid}"}
    if functions:
        entry["functions"] = functions
    write(os.path.join(DATA, MOD, "loot_table", "blocks", f"{bid}.json"), {
        "type": "minecraft:block", "random_sequence": f"{MOD}:blocks/{bid}",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [entry],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

for pid in PIPES:
    tex = f"{MOD}:block/{pid}"
    for part in ("core", "arm", "push", "pull"):
        model(f"{pid}_{part}", {"parent": f"{MOD}:block/pipe_{part}", "textures": {"pipe": tex}})
    parts = [{"apply": {"model": f"{MOD}:block/{pid}_core"}}]
    for side, rot in SIDE_ROT.items():
        for look in ("pipe", "push", "pull"):
            part = "arm" if look == "pipe" else look
            parts.append({"when": {side: look}, "apply": {"model": f"{MOD}:block/{pid}_{part}", **rot}})
    write(os.path.join(ASSETS, "blockstates", f"{pid}.json"), {"multipart": parts})
    write(os.path.join(ASSETS, "models", "item", f"{pid}.json"), {
        "parent": "minecraft:block/block", "textures": {"particle": tex, "pipe": tex},
        "elements": [{"from": [0, 5, 5], "to": [16, 11, 11], "faces": {
            "east": face([0, 6, 6, 12]), "west": face([0, 6, 6, 12]),
            "north": face([0, 0, 16, 6]), "south": face([0, 0, 16, 6]),
            "up": face([0, 0, 16, 6]), "down": face([0, 0, 16, 6])}}],
        "display": {"gui": {"rotation": [30, 45, 0], "scale": [0.8, 0.8, 0.8]}}})
    simple_loot(pid)
    tags[("minecraft", "block", "mineable/pickaxe")].append(f"{MOD}:{pid}")

# ---- fluid tank: frame and glass (cut out so the fluid drawn inside shows); keeps its fluid when broken
write(os.path.join(ASSETS, "blockstates", "fluid_tank.json"), {"variants": {"": {"model": f"{MOD}:block/fluid_tank"}}})
model("fluid_tank", {"parent": "minecraft:block/cube_column", "render_type": "minecraft:cutout",
      "textures": {"side": f"{MOD}:block/fluid_tank_side", "end": f"{MOD}:block/fluid_tank_top"}})
write(os.path.join(ASSETS, "models", "item", "fluid_tank.json"), {"parent": f"{MOD}:block/fluid_tank"})
simple_loot("fluid_tank", [{"function": "minecraft:copy_components", "source": "block_entity",
                            "include": [f"{MOD}:fluid"]}])
tags[("minecraft", "block", "mineable/pickaxe")].append(f"{MOD}:fluid_tank")

# ---- plain items
for iid in ("wrench", "filter_card"):
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/handheld" if iid == "wrench" else "minecraft:item/generated",
           "textures": {"layer0": f"{MOD}:item/{iid}"}})

write(os.path.join(DATA, "minecraft", "tags", "block", "mineable/pickaxe.json"),
      {"replace": False, "values": tags[("minecraft", "block", "mineable/pickaxe")]})

# ---- crafting recipes for the machines
def shaped(name, pattern, key, count=1):
    write(os.path.join(DATA, MOD, "recipe", f"{name}.json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {k: ({"item": v} if ":" in v and not v.startswith("#") else {"tag": v[1:]}) for k, v in key.items()},
        "result": {"id": f"{MOD}:{name}", "count": count}})
    recipes[name] = True

shaped("electric_furnace", ["IGI", "GFG", "IRI"],
       {"I": "#c:ingots/iron", "G": "#c:gears/copper", "F": "minecraft:furnace", "R": "#c:dusts/redstone"})
shaped("combustion_generator", ["III", "GFG", "IRI"],
       {"I": "#c:ingots/iron", "G": "#c:gears/copper", "F": "minecraft:furnace", "R": "#c:dusts/redstone"})
shaped("basic_battery", ["ILI", "LRL", "ILI"],
       {"I": "#c:ingots/iron", "L": "#c:ingots/lead", "R": "#c:storage_blocks/redstone"})
shaped("crusher", ["IGI", "FPF", "IRI"],
       {"I": "#c:ingots/iron", "G": "#c:gears/copper", "F": "minecraft:flint", "P": "minecraft:piston", "R": "#c:dusts/redstone"})
shaped("energy_cable", ["CRC"], {"C": "#c:ingots/copper", "R": "#c:dusts/redstone"}, count=6)
shaped("pump", ["IBI", "PGP", "IRI"],
       {"I": "#c:ingots/iron", "B": "minecraft:bucket", "P": f"{MOD}:fluid_pipe", "G": "#c:gears/copper", "R": "#c:dusts/redstone"})
shaped("item_duct", ["LGL"], {"L": "#c:ingots/lead", "G": "#c:glass_blocks"}, count=6)
shaped("fluid_pipe", ["TGT"], {"T": "#c:ingots/tin", "G": "#c:glass_blocks"}, count=6)
# Tier 2: eight tier 1 pipes around a steel (ducts) or invar (pipes) plate from the Compressor.
shaped("reinforced_item_duct", ["DDD", "DPD", "DDD"], {"D": f"{MOD}:item_duct", "P": "#c:plates/steel"}, count=8)
shaped("reinforced_fluid_pipe", ["DDD", "DPD", "DDD"], {"D": f"{MOD}:fluid_pipe", "P": "#c:plates/invar"}, count=8)
shaped("fluid_tank", ["IGI", "G G", "IGI"], {"I": "#c:ingots/copper", "G": "#c:glass_blocks"})
shaped("wrench", ["I I", " T ", " I "], {"I": "#c:ingots/iron", "T": "#c:ingots/tin"})
shaped("filter_card", ["NPN", "PRP", "NPN"], {"N": "#c:nuggets/tin", "P": "minecraft:paper", "R": "#c:dusts/redstone"})

# ---- machine recipes: one JSON format for every machine (see MachineRecipe.java)
def machine(kind, name, inputs, results, energy=None, time=None):
    """inputs: [(ingredient, count)]; results: [(item id, count, chance)]."""
    r = {"type": f"{MOD}:{kind}",
         "inputs": [dict(ing(i), **({"count": c} if c != 1 else {})) for i, c in inputs],
         "results": [dict({"id": rid}, **({"count": c} if c != 1 else {}), **({"chance": ch} if ch < 1 else {}))
                     for rid, c, ch in results]}
    if energy is not None:
        r["energy"] = energy
    if time is not None:
        r["time"] = time
    write(os.path.join(DATA, MOD, "recipe", kind, f"{name}.json"), r)
    recipes[f"{kind}/{name}"] = True

DUST = lambda m: f"{MOD}:{m}_dust"
INGOT = lambda m: ingot_of(m)

# Crusher: ores and raw metal give two dusts (ores sometimes a bonus dust of a related metal), ingots one.
ORE_BONUS = {"iron": "nickel", "gold": "silver", "copper": "gold", "tin": "iron", "lead": "silver",
             "silver": "lead", "nickel": "iron", "uranium": "lead"}
for m, (src, dust, plate, gear, wire) in METALS.items():
    if not dust:
        continue
    machine("crushing", f"{m}_dust_from_ingot", [(f"#c:ingots/{m}", 1)], [(DUST(m), 1, 1)], 1600, 80)
    if src == "ore" or m in ("iron", "copper", "gold"):
        machine("crushing", f"{m}_dust_from_raw", [(f"#c:raw_materials/{m}", 1)], [(DUST(m), 2, 1)])
        machine("crushing", f"{m}_dust_from_ore", [(f"#c:ores/{m}", 1)],
                [(DUST(m), 2, 1), (DUST(ORE_BONUS[m]), 1, 0.1)], 3000, 150)
machine("crushing", "gravel_from_cobblestone", [("#c:cobblestones/normal", 1)], [("minecraft:gravel", 1, 1)], 1200, 60)
machine("crushing", "sand_from_gravel", [("#c:gravels", 1)], [("minecraft:sand", 1, 1), ("minecraft:flint", 1, 0.1)], 1200, 60)
machine("crushing", "bone_meal_from_bone", [("minecraft:bone", 1)], [("minecraft:bone_meal", 6, 1)], 800, 40)
machine("crushing", "blaze_powder_from_rod", [("minecraft:blaze_rod", 1)], [("minecraft:blaze_powder", 4, 1)], 1600, 80)

# Alloy Smelter: ingots or dusts, mixed.
ALLOYS = {"steel": ([("iron", 1)], "#minecraft:coals", 2, 1),
          "bronze": ([("copper", 3), ("tin", 1)], None, 0, 4),
          "invar": ([("iron", 2), ("nickel", 1)], None, 0, 3),
          "electrum": ([("gold", 1), ("silver", 1)], None, 0, 2)}

for alloy, (parts, extra, extra_count, out) in ALLOYS.items():
    r = {"type": f"{MOD}:alloy_smelting", "inputs": [], "results": [{"id": INGOT(alloy), "count": out}]}
    for m, c in parts:
        entry = {"tag": f"c:ingots/{m}"}
        r["inputs"].append(dict(entry, **({"count": c} if c != 1 else {})))
    if extra:
        r["inputs"].append(dict(ing(extra), count=extra_count))
    write(os.path.join(DATA, MOD, "recipe", "alloy_smelting", f"{alloy}_ingot.json"), r)
    recipes[f"alloy_smelting/{alloy}_ingot"] = True
    # the same from dusts
    d = {"type": f"{MOD}:alloy_smelting", "inputs": [], "results": [{"id": INGOT(alloy), "count": out}], "energy": 3200}
    for m, c in parts:
        d["inputs"].append(dict({"tag": f"c:dusts/{m}"}, **({"count": c} if c != 1 else {})))
    if extra:
        d["inputs"].append(dict(ing(extra), count=extra_count))
    write(os.path.join(DATA, MOD, "recipe", "alloy_smelting", f"{alloy}_ingot_from_dust.json"), d)
    recipes[f"alloy_smelting/{alloy}_ingot_from_dust"] = True
machine("alloy_smelting", "netherite_scrap_ingot", [("minecraft:netherite_scrap", 4), ("#c:ingots/gold", 4)],
        [("minecraft:netherite_ingot", 1, 1)], 16000, 400)

# Compressor: ingots to plates.
for m, (src, dust, plate, gear, wire) in METALS.items():
    if plate:
        machine("compressing", f"{m}_plate", [(f"#c:ingots/{m}", 1)], [(f"{MOD}:{m}_plate", 1, 1)])
machine("compressing", "snow_block", [("minecraft:snowball", 4)], [("minecraft:snow_block", 1, 1)], 400, 20)

# Wire Drawer: one ingot into two wires.
for m, (src, dust, plate, gear, wire) in METALS.items():
    if wire:
        machine("wire_drawing", f"{m}_wire", [(f"#c:ingots/{m}", 1)], [(f"{MOD}:{m}_wire", 2, 1)])

# Sawmill: a log into six planks and some sawdust.
WOODS = {"oak": "oak_logs", "spruce": "spruce_logs", "birch": "birch_logs", "jungle": "jungle_logs",
         "acacia": "acacia_logs", "dark_oak": "dark_oak_logs", "mangrove": "mangrove_logs",
         "cherry": "cherry_logs", "crimson": "crimson_stems", "warped": "warped_stems"}
for wood, log_tag in WOODS.items():
    machine("sawing", f"{wood}_planks", [(f"#minecraft:{log_tag}", 1)],
            [(f"minecraft:{wood}_planks", 6, 1), (f"{MOD}:sawdust", 1, 1)])
machine("sawing", "bamboo_planks", [("#minecraft:bamboo_blocks", 1)], [("minecraft:bamboo_planks", 3, 1), (f"{MOD}:sawdust", 1, 0.5)])
machine("sawing", "sticks", [("#minecraft:planks", 1)], [("minecraft:stick", 4, 1), (f"{MOD}:sawdust", 1, 0.25)], 600, 30)

# Ore Washer: raw ore or ore and water into three crushed ores (then the Crusher makes each a dust).
def machine_fluid(kind, name, fluid, inputs, results, fluid_result=None, energy=None, time=None):
    r = {"type": f"{MOD}:{kind}"}
    if fluid:
        r["fluid"] = {"id": fluid[0], "amount": fluid[1]}
    if inputs:
        r["inputs"] = [dict(ing(i), **({"count": c} if c != 1 else {})) for i, c in inputs]
    if results:
        r["results"] = [dict({"id": rid}, **({"count": c} if c != 1 else {}), **({"chance": ch} if ch < 1 else {}))
                        for rid, c, ch in results]
    if fluid_result:
        r["fluid_result"] = {"id": fluid_result[0], "amount": fluid_result[1]}
    if energy is not None:
        r["energy"] = energy
    if time is not None:
        r["time"] = time
    write(os.path.join(DATA, MOD, "recipe", kind, f"{name}.json"), r)
    recipes[f"{kind}/{name}"] = True

CRUSHED = lambda m: f"{MOD}:crushed_{m}"
for m, (src, dust, plate, gear, wire) in METALS.items():
    if src not in ("ore", "vanilla"):
        continue
    machine_fluid("washing", f"crushed_{m}_from_raw", ("minecraft:water", 500), [(f"#c:raw_materials/{m}", 1)],
                  [(CRUSHED(m), 3, 1), ("minecraft:gravel", 1, 0.1)])
    machine_fluid("washing", f"crushed_{m}_from_ore", ("minecraft:water", 500), [(f"#c:ores/{m}", 1)],
                  [(CRUSHED(m), 3, 1), (CRUSHED(ORE_BONUS[m]), 1, 0.15)], energy=3600, time=160)
    machine("crushing", f"{m}_dust_from_crushed", [(CRUSHED(m), 1)], [(DUST(m), 1, 1)], 1200, 60)
machine_fluid("washing", "clay_from_dirt", ("minecraft:water", 250), [("minecraft:dirt", 1)],
              [("minecraft:clay_ball", 2, 1), ("minecraft:flint", 1, 0.05)], energy=1200, time=60)

# Fluid Mixer: water and an item into Joulesmith's fluids.
ACID, COOLANT, BIOFUEL = f"{MOD}:sulfuric_acid", f"{MOD}:coolant", f"{MOD}:biofuel"
machine_fluid("mixing", "sulfuric_acid", ("minecraft:water", 1000), [("#c:gunpowders", 1)], [], (ACID, 1000))
machine_fluid("mixing", "coolant", ("minecraft:water", 1000), [("minecraft:ice", 1)], [], (COOLANT, 1000))
machine_fluid("mixing", "coolant_from_packed_ice", ("minecraft:water", 1000), [("minecraft:packed_ice", 1)], [], (COOLANT, 4000), time=200)
machine_fluid("mixing", "biofuel_from_sawdust", ("minecraft:water", 500), [(f"{MOD}:sawdust", 4)], [], (BIOFUEL, 500))
machine_fluid("mixing", "biofuel_from_sugar", ("minecraft:water", 500), [("minecraft:sugar", 2)], [], (BIOFUEL, 500))

# Centrifuge: alloys back apart, and a few useful splits.
machine("centrifuging", "electrum_dust", [("#c:dusts/electrum", 2)], [(DUST("gold"), 1, 1), (DUST("silver"), 1, 1)])
machine("centrifuging", "bronze_dust", [("#c:dusts/bronze", 4)], [(DUST("copper"), 3, 1), (DUST("tin"), 1, 1)])
machine("centrifuging", "invar_dust", [("#c:dusts/invar", 3)], [(DUST("iron"), 2, 1), (DUST("nickel"), 1, 1)])
machine("centrifuging", "steel_dust", [("#c:dusts/steel", 1)], [(DUST("iron"), 1, 1), ("minecraft:coal", 1, 0.5)])
machine("centrifuging", "magma_cream", [("minecraft:magma_cream", 1)], [("minecraft:slime_ball", 1, 1), ("minecraft:blaze_powder", 1, 1)])
machine("centrifuging", "glowstone_dust", [("#c:dusts/glowstone", 2)], [("minecraft:redstone", 1, 1), ("minecraft:gold_nugget", 2, 0.5)])
machine("centrifuging", "gravel", [("#c:gravels", 1)],
        [("minecraft:sand", 1, 1), ("minecraft:flint", 1, 0.25), ("minecraft:iron_nugget", 1, 0.05), ("minecraft:gold_nugget", 1, 0.02)])
machine("centrifuging", "rotten_flesh", [("minecraft:rotten_flesh", 2)], [("minecraft:leather", 1, 0.5), ("minecraft:bone_meal", 1, 0.5)])

# Circuit Assembler: the Basic Circuit, the part every tier 2 machine needs.
machine("circuit_assembling", "basic_circuit", [("#c:wires/copper", 2), ("#c:dusts/redstone", 2), ("#c:plates/tin", 1)],
        [(f"{MOD}:basic_circuit", 1, 1)])

# ---- tier 2 machines: steel plates and a Basic Circuit; the Circuit Assembler itself is made without one
shaped("circuit_assembler", ["PWP", "GMG", "PRP"],
       {"P": "#c:plates/bronze", "W": "#c:wires/copper", "G": "#c:gears/copper", "M": "minecraft:crafter", "R": "#c:dusts/redstone"})
shaped("ore_washer", ["PCP", "GMG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "M": "minecraft:cauldron", "R": "#c:dusts/redstone"})
shaped("fluid_mixer", ["PCP", "GMG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "M": f"{MOD}:fluid_tank", "R": "#c:dusts/redstone"})
shaped("centrifuge", ["PCP", "GMG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/invar", "M": "#c:ingots/electrum", "R": "#c:dusts/redstone"})

# ---- fluids: a block to place, a bucket (NeoForge's tinted bucket model) and names
FLUIDS = {"sulfuric_acid": "Sulfuric Acid", "coolant": "Coolant", "biofuel": "Biofuel"}
for fid, fname in FLUIDS.items():
    write(os.path.join(ASSETS, "blockstates", f"{fid}.json"), {"variants": {"": {"model": f"{MOD}:block/{fid}"}}})
    write(os.path.join(ASSETS, "models", "block", f"{fid}.json"), {"textures": {"particle": "minecraft:block/water_still"}})
    write(os.path.join(ASSETS, "models", "item", f"{fid}_bucket.json"), {
        "parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": f"{MOD}:{fid}"})

# ---- farming: Bio Reactor recipes, farm machines, rubber trees
machine_fluid("bio_reacting", "seeds", None, [("#c:seeds", 2)], [], (BIOFUEL, 50))
machine_fluid("bio_reacting", "crops", None, [("#c:crops", 1)], [], (BIOFUEL, 50))
machine_fluid("bio_reacting", "saplings", None, [("#minecraft:saplings", 1)], [], (BIOFUEL, 50))
machine_fluid("bio_reacting", "leaves", None, [("#minecraft:leaves", 2)], [], (BIOFUEL, 25))
machine_fluid("bio_reacting", "sawdust", None, [(f"{MOD}:sawdust", 2)], [], (BIOFUEL, 25))
machine_fluid("bio_reacting", "sugar_cane", None, [("minecraft:sugar_cane", 1)], [], (BIOFUEL, 50))
machine_fluid("bio_reacting", "kelp", None, [("minecraft:kelp", 2)], [], (BIOFUEL, 25))
machine_fluid("bio_reacting", "flowers", None, [("#minecraft:flowers", 1)], [], (BIOFUEL, 25))
machine_fluid("bio_reacting", "melon", None, [("minecraft:melon_slice", 2)], [], (BIOFUEL, 50))
machine_fluid("bio_reacting", "pumpkin", None, [("minecraft:pumpkin", 1)], [], (BIOFUEL, 100))

shaped("planter", ["IHI", "GDG", "IRI"],
       {"I": "#c:ingots/iron", "H": "minecraft:iron_hoe", "G": "#c:gears/copper", "D": "minecraft:dirt", "R": "#c:dusts/redstone"})
shaped("harvester", ["IHI", "GSG", "IRI"],
       {"I": "#c:ingots/iron", "H": "minecraft:iron_hoe", "G": "#c:gears/copper", "S": "minecraft:shears", "R": "#c:dusts/redstone"})
shaped("tree_farm", ["PCP", "GAG", "PSP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "A": "minecraft:iron_axe", "S": "#minecraft:saplings"})
shaped("bio_reactor", ["PCP", "GTG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "T": "minecraft:composter", "R": "#c:dusts/redstone"})
shaped("biofuel_generator", ["PCP", "GTG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "T": f"{MOD}:combustion_generator", "R": "#c:dusts/redstone"})
shaped("range_upgrade", ["RER", "EPE", "RER"], {"R": "#c:dusts/redstone", "E": "#c:ender_pearls", "P": "#c:plates/tin"})
shaped("tree_tap", [" I ", "PPP", "P  "], {"I": "#c:ingots/iron", "P": "#minecraft:planks"})

# rubber trees: a log with tap spots, leaves, a sapling and planks
RUBBER = {"rubber_log": "Rubber Log", "rubber_leaves": "Rubber Leaves", "rubber_sapling": "Rubber Sapling",
          "rubber_planks": "Rubber Planks"}
log_variants = {}
for axis, rot in (("y", {}), ("x", {"x": 90, "y": 90}), ("z", {"x": 90})):
    for latex in ("none", "dry", "wet"):
        log_model = f"{MOD}:block/rubber_log" + ("" if latex == "none" else f"_{latex}")
        log_variants[f"axis={axis},latex={latex}"] = dict({"model": log_model}, **rot)
write(os.path.join(ASSETS, "blockstates", "rubber_log.json"), {"variants": log_variants})
# bark, log ends, planks and leaves are made in game from vanilla wood (see MetalTexturePack); the tap
# spot is a separate overlay laid on the bark, like grass on a grass block
model("rubber_log", {"parent": "minecraft:block/cube_column",
      "textures": {"side": f"{MOD}:block/rubber_log", "end": f"{MOD}:block/rubber_log_top"}})
CUBE_FACES = {"down": "#end", "up": "#end", "north": "#side", "south": "#side", "west": "#side", "east": "#side"}
for state in ("dry", "wet"):
    model(f"rubber_log_{state}", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "#side", "side": f"{MOD}:block/rubber_log", "end": f"{MOD}:block/rubber_log_top",
                     "spot": f"{MOD}:block/rubber_log_spot_{state}"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {f: {"texture": t, "cullface": f} for f, t in CUBE_FACES.items()}},
            {"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {f: {"texture": "#spot", "cullface": f} for f in ("north", "south", "west", "east")}}]})
write(os.path.join(ASSETS, "models", "item", "rubber_log.json"), {"parent": f"{MOD}:block/rubber_log"})
for bid in ("rubber_leaves", "rubber_planks"):
    write(os.path.join(ASSETS, "blockstates", f"{bid}.json"), {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{bid}"}}
    if bid == "rubber_leaves":
        m["render_type"] = "minecraft:cutout_mipped"
    model(bid, m)
    write(os.path.join(ASSETS, "models", "item", f"{bid}.json"), {"parent": f"{MOD}:block/{bid}"})
write(os.path.join(ASSETS, "blockstates", "rubber_sapling.json"), {"variants": {"stage=0": {"model": f"{MOD}:block/rubber_sapling"},
                                                                              "stage=1": {"model": f"{MOD}:block/rubber_sapling"}}})
model("rubber_sapling", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                         "textures": {"cross": f"{MOD}:block/rubber_sapling"}})
write(os.path.join(ASSETS, "models", "item", "rubber_sapling.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:block/rubber_sapling"}})
for bid in ("rubber_log", "rubber_planks", "rubber_sapling"):
    simple_loot(bid)
SILK_OR_SHEARS = {"condition": "minecraft:any_of", "terms": [
    {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
    {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
write(os.path.join(DATA, MOD, "loot_table", "blocks", "rubber_leaves.json"), {
    "type": "minecraft:block", "random_sequence": f"{MOD}:blocks/rubber_leaves",
    "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:rubber_leaves", "conditions": [SILK_OR_SHEARS]},
            {"type": "minecraft:item", "name": f"{MOD}:rubber_sapling", "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                 "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}]}]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:inverted", "term": SILK_OR_SHEARS}],
         "entries": [{"type": "minecraft:item", "name": "minecraft:stick",
                      "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}],
                      "conditions": [{"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                                      "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}]}]})
for reg in ("block", "item"):
    tag("minecraft", reg, "logs", f"{MOD}:rubber_log")
    tag("minecraft", reg, "logs_that_burn", f"{MOD}:rubber_log")
    tag("minecraft", reg, "leaves", f"{MOD}:rubber_leaves")
    tag("minecraft", reg, "saplings", f"{MOD}:rubber_sapling")
    tag("minecraft", reg, "planks", f"{MOD}:rubber_planks")
    tag(MOD, reg, "rubber_logs", f"{MOD}:rubber_log")
tag("minecraft", "block", "mineable/axe", f"{MOD}:rubber_log")
tag("minecraft", "block", "mineable/axe", f"{MOD}:rubber_planks")
tag("minecraft", "block", "mineable/hoe", f"{MOD}:rubber_leaves")
recipes["rubber_planks"] = {"type": "minecraft:crafting_shapeless", "category": "building",
                            "ingredients": [{"tag": f"{MOD}:rubber_logs"}], "result": {"id": f"{MOD}:rubber_planks", "count": 4}}
write(os.path.join(DATA, MOD, "recipe", "rubber_planks.json"), recipes["rubber_planks"])
write(os.path.join(DATA, MOD, "recipe", "rubber_from_latex.json"), {
    "type": "minecraft:smelting", "category": "misc", "cookingtime": 200, "experience": 0.2,
    "ingredient": {"item": f"{MOD}:latex"}, "result": {"id": f"{MOD}:rubber"}})
recipes["rubber_from_latex"] = True
machine("sawing", "rubber_planks", [(f"#{MOD}:rubber_logs", 1)], [(f"{MOD}:rubber_planks", 6, 1), (f"{MOD}:sawdust", 1, 1)])

# rubber tree worldgen: a tall trunk (one log in six with a full tap spot) under a spruce-like crown
LOG = lambda latex: {"Name": f"{MOD}:rubber_log", "Properties": {"axis": "y", "latex": latex}}
write(os.path.join(DATA, MOD, "worldgen", "configured_feature", "rubber_tree.json"), {
    "type": "minecraft:tree", "config": {
        "trunk_provider": {"type": "minecraft:weighted_state_provider", "entries": [
            {"weight": 5, "data": LOG("none")}, {"weight": 1, "data": LOG("wet")}]},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 5, "height_rand_a": 3, "height_rand_b": 1},
        "foliage_provider": {"type": "minecraft:simple_state_provider",
                             "state": {"Name": f"{MOD}:rubber_leaves", "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}},
        "foliage_placer": {"type": "minecraft:spruce_foliage_placer", "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 3},
                           "offset": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 1},
                           "trunk_height": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}},
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}},
        "decorators": [], "ignore_vines": True, "force_dirt": False}})
write(os.path.join(DATA, MOD, "worldgen", "placed_feature", "rubber_tree.json"), {
    "feature": f"{MOD}:rubber_tree", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive",
                                                                   "state": {"Name": f"{MOD}:rubber_sapling", "Properties": {"stage": "0"}}}}]})
for biomes in ("is_forest", "is_jungle"):
    write(os.path.join(DATA, MOD, "neoforge", "biome_modifier", f"rubber_trees_{biomes}.json"), {
        "type": "neoforge:add_features", "biomes": f"#minecraft:{biomes}", "features": f"{MOD}:rubber_tree",
        "step": "vegetal_decoration"})

for iid in ("latex", "rubber", "tree_tap", "range_upgrade"):
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/handheld" if iid == "tree_tap" else "minecraft:item/generated",
           "textures": {"layer0": f"{MOD}:item/{iid}"}})
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})

# ---- material gear: copper, bronze and steel tools and armor (textures made in game from vanilla's iron
# ones), the steel Hammer and Excavator, and enchanting / repair tags
GEAR_METALS = {"copper": "Copper", "bronze": "Bronze", "steel": "Steel"}
TOOLS = {"sword": ("swords", ["X", "X", "S"]), "pickaxe": ("pickaxes", ["XXX", " S ", " S "]),
         "axe": ("axes", ["XX", "XS", " S"]), "shovel": ("shovels", ["X", "S", "S"]), "hoe": ("hoes", ["XX", " S", " S"])}
ARMOR = {"helmet": ("head_armor", ["XXX", "X X"]), "chestplate": ("chest_armor", ["X X", "XXX", "XXX"]),
         "leggings": ("leg_armor", ["XXX", "X X", "X X"]), "boots": ("foot_armor", ["X X", "X X"])}
gear_generated = []
for metal, mname in GEAR_METALS.items():
    for piece, (item_tag, pattern) in {**TOOLS, **ARMOR}.items():
        iid = f"{metal}_{piece}"
        handheld = piece in TOOLS
        write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
              {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
               "textures": {"layer0": f"{MOD}:item/{iid}"}})
        gear_generated.append(f"item/{iid}")
        key = {"X": f"#c:ingots/{metal}"}
        if handheld:
            key["S"] = "#c:rods/wooden"
        shaped(iid, pattern, key)
        tag("minecraft", "item", item_tag, f"{MOD}:{iid}")
        lang[f"item.{MOD}.{iid}"] = f"{mname} {piece.title()}"
    gear_generated += [f"models/armor/{metal}_layer_1", f"models/armor/{metal}_layer_2"]
for iid, name, item_tag, pattern, key in (
        ("steel_hammer", "Steel Hammer", "pickaxes", ["SBS", "SRS", " R "],
         {"S": "#c:ingots/steel", "B": "#c:storage_blocks/steel", "R": "#c:rods/wooden"}),
        ("steel_excavator", "Steel Excavator", "shovels", [" B ", "SRS", " R "],
         {"S": "#c:ingots/steel", "B": "#c:storage_blocks/steel", "R": "#c:rods/wooden"})):
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{iid}"}})
    shaped(iid, pattern, key)
    tag("minecraft", "item", item_tag, f"{MOD}:{iid}")
    lang[f"item.{MOD}.{iid}"] = name

# ---- storage: Crates and Bulk Bins keep what they hold when broken
STORAGE = {"basic_crate": "Crate", "reinforced_crate": "Reinforced Crate",
           "basic_bulk_bin": "Bulk Bin", "reinforced_bulk_bin": "Reinforced Bulk Bin"}
for bid in ("basic_crate", "reinforced_crate"):
    write(os.path.join(ASSETS, "blockstates", f"{bid}.json"), {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})
    model(bid, {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": f"{MOD}:block/{bid}_side", "top": f"{MOD}:block/{bid}_top", "bottom": f"{MOD}:block/{bid}_top"}})
    simple_loot(bid, [{"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:container"]}])
for bid in ("basic_bulk_bin", "reinforced_bulk_bin"):
    write(os.path.join(ASSETS, "blockstates", f"{bid}.json"), {"variants": {
        f"facing={f}": dict({"model": f"{MOD}:block/{bid}"}, **({"y": r} if r else {})) for f, r in ROTATION.items()}})
    model(bid, {"parent": "minecraft:block/orientable", "textures": {
        "front": f"{MOD}:block/{bid}_front", "side": f"{MOD}:block/{bid}_side", "top": f"{MOD}:block/{bid}_side"}})
    simple_loot(bid, [{"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:bin"]}])
for bid, name in STORAGE.items():
    write(os.path.join(ASSETS, "models", "item", f"{bid}.json"), {"parent": f"{MOD}:block/{bid}"})
    tag("minecraft", "block", "mineable/axe", f"{MOD}:{bid}")
    lang[f"block.{MOD}.{bid}"] = name
shaped("basic_crate", ["TPT", "PCP", "TPT"], {"T": "#c:ingots/tin", "P": "#minecraft:planks", "C": "#c:chests/wooden"})
shaped("reinforced_crate", ["SPS", "PCP", "SPS"], {"S": "#c:ingots/steel", "P": "#c:plates/steel", "C": "#c:chests/wooden"})
shaped("basic_bulk_bin", ["TPT", "PBP", "TPT"], {"T": "#c:ingots/tin", "P": "#minecraft:planks", "B": "#c:barrels/wooden"})
shaped("reinforced_bulk_bin", ["SPS", "PBP", "SPS"], {"S": "#c:ingots/steel", "P": "#c:plates/steel", "B": "#c:barrels/wooden"})
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})

# ---- tier 3: steam, plastic and the Advanced Circuit, higher-tier cables, ducts, pipes and batteries
machine_fluid("mixing", "plastic_sheet", (BIOFUEL, 500), [(f"{MOD}:latex", 1)], [(f"{MOD}:plastic_sheet", 2, 1)])
machine("circuit_assembling", "advanced_circuit",
        [("#c:wires/electrum", 2), (f"{MOD}:basic_circuit", 1), (f"{MOD}:plastic_sheet", 1)], [(f"{MOD}:advanced_circuit", 1, 1)], 6000, 160)
shaped("reinforced_energy_cable", ["RRR", "WWW", "RRR"], {"R": f"{MOD}:rubber", "W": "#c:wires/copper"}, count=6)
shaped("advanced_energy_cable", ["RRR", "WWW", "RRR"], {"R": f"{MOD}:plastic_sheet", "W": "#c:wires/electrum"}, count=6)
shaped("advanced_item_duct", ["DDD", "DPD", "DDD"], {"D": f"{MOD}:reinforced_item_duct", "P": "#c:plates/electrum"}, count=8)
shaped("advanced_fluid_pipe", ["DDD", "DPD", "DDD"], {"D": f"{MOD}:reinforced_fluid_pipe", "P": f"{MOD}:plastic_sheet"}, count=8)
shaped("reinforced_battery", ["PCP", "BRB", "PCP"],
       {"P": "#c:plates/invar", "C": f"{MOD}:basic_circuit", "B": f"{MOD}:basic_battery", "R": "#c:storage_blocks/redstone"})
shaped("advanced_battery", ["PCP", "BRB", "PCP"],
       {"P": "#c:plates/electrum", "C": f"{MOD}:advanced_circuit", "B": f"{MOD}:reinforced_battery", "R": "#c:storage_blocks/redstone"})
shaped("boiler", ["PCP", "GFG", "PTP"],
       {"P": "#c:plates/invar", "C": f"{MOD}:advanced_circuit", "G": "#c:gears/steel", "F": "minecraft:blast_furnace", "T": f"{MOD}:fluid_tank"})
shaped("steam_turbine", ["PCP", "GBG", "PRP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:advanced_circuit", "G": "#c:gears/steel", "B": "#c:storage_blocks/copper", "R": "#c:storage_blocks/redstone"})
for iid in ("advanced_circuit", "plastic_sheet"):
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{iid}"}})
lang.update({f"item.{MOD}.advanced_circuit": "Advanced Circuit", f"item.{MOD}.plastic_sheet": "Plastic Sheet"})
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})

# ---- automation (tiers 1 to 3)
IRON, RED, CU_GEAR = "#c:ingots/iron", "#c:dusts/redstone", "#c:gears/copper"
shaped("block_placer", ["IDI", "GPG", "IRI"], {"I": IRON, "D": "minecraft:dispenser", "G": CU_GEAR, "P": "minecraft:piston", "R": RED})
shaped("block_breaker", ["IAI", "GPG", "IRI"], {"I": IRON, "A": "minecraft:iron_pickaxe", "G": CU_GEAR, "P": "minecraft:piston", "R": RED})
T2 = {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "G": "#c:gears/bronze", "R": RED}
shaped("autocrafter", ["PCP", "GTG", "PRP"], dict(T2, T="minecraft:crafter"))
shaped("fluid_placer", ["PCP", "GDG", "PBP"], dict(T2, D="minecraft:dispenser", B="minecraft:bucket"))
shaped("fluid_drain", ["PCP", "GMG", "PBP"], dict(T2, M=f"{MOD}:pump", B="minecraft:bucket"))
T3 = {"P": "#c:plates/steel", "C": f"{MOD}:advanced_circuit", "G": "#c:gears/steel", "R": RED}
shaped("mechanical_user", ["PCP", "GDG", "PRP"], dict(T3, P="#c:plates/invar", D="minecraft:dispenser"))
shaped("quarry", ["PCP", "GAG", "PBP"], dict(T3, A="minecraft:diamond_pickaxe", B="#c:storage_blocks/steel"))
shaped("mob_fighter", ["PCP", "GSG", "PRP"], dict(T3, S="minecraft:diamond_sword"))

# ---- logic: sensors (their sensing face points at the block they read) and the Logic Controller
SENSORS = {"inventory": ("Inventory Sensor", "minecraft:chest"), "tank": ("Tank Sensor", "minecraft:bucket"),
           "energy": ("Energy Sensor", "#c:storage_blocks/redstone"), "time": ("Time Sensor", "minecraft:clock"),
           "proximity": ("Proximity Sensor", "minecraft:observer")}
FACING6 = {"north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
for kind, (name, part) in SENSORS.items():
    sid = f"{kind}_sensor"
    variants = {}
    for facing, rot in FACING6.items():
        for powered in ("false", "true"):
            variants[f"facing={facing},powered={powered}"] = dict({"model": f"{MOD}:block/{sid}" + ("_on" if powered == "true" else "")}, **rot)
    write(os.path.join(ASSETS, "blockstates", f"{sid}.json"), {"variants": variants})
    for suffix in ("", "_on"):
        model(f"{sid}{suffix}", {"parent": "minecraft:block/cube", "textures": {
            "particle": f"{MOD}:block/sensor_side", "north": f"{MOD}:block/sensor_front", "south": f"{MOD}:block/{sid}_back{suffix}",
            "east": f"{MOD}:block/sensor_side", "west": f"{MOD}:block/sensor_side", "up": f"{MOD}:block/sensor_side", "down": f"{MOD}:block/sensor_side"}})
    write(os.path.join(ASSETS, "models", "item", f"{sid}.json"), {"parent": f"{MOD}:block/{sid}"})
    simple_loot(sid)
    tag("minecraft", "block", "mineable/pickaxe", f"{MOD}:{sid}")
    lang[f"block.{MOD}.{sid}"] = name
    shaped(sid, ["PCP", "RXR", "PRP"], {"P": "#c:plates/steel", "C": f"{MOD}:basic_circuit", "R": "#c:dusts/redstone", "X": part})
write(os.path.join(ASSETS, "blockstates", "logic_controller.json"), {"variants": {"": {"model": f"{MOD}:block/logic_controller"}}})
model("logic_controller", {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "side": f"{MOD}:block/logic_controller", "top": f"{MOD}:block/logic_controller_top", "bottom": f"{MOD}:block/machine_bottom"}})
write(os.path.join(ASSETS, "models", "item", "logic_controller.json"), {"parent": f"{MOD}:block/logic_controller"})
simple_loot("logic_controller")
tag("minecraft", "block", "mineable/pickaxe", f"{MOD}:logic_controller")
shaped("logic_controller", ["PCP", "RMR", "PCP"],
       {"P": "#c:plates/steel", "C": f"{MOD}:advanced_circuit", "R": "minecraft:comparator", "M": "#c:storage_blocks/redstone"})
lang.update({
    f"block.{MOD}.logic_controller": "Logic Controller",
    "message.joulesmith.sensor": "%s: %s (signal %s)",
    "gui.joulesmith.measure.energy": "Energy", "gui.joulesmith.measure.items": "Items",
    "gui.joulesmith.measure.fluid": "Fluid", "gui.joulesmith.measure.redstone": "Redstone",
    "gui.joulesmith.measure.time": "Time", "gui.joulesmith.measure.players": "Players",
    "gui.joulesmith.measure.hostiles": "Hostile mobs", "gui.joulesmith.measure.animals": "Animals",
    "gui.joulesmith.dir.north": "North", "gui.joulesmith.dir.south": "South", "gui.joulesmith.dir.east": "East",
    "gui.joulesmith.dir.west": "West", "gui.joulesmith.dir.up": "Up", "gui.joulesmith.dir.down": "Down",
    "gui.joulesmith.logic.col.reading": "Reading", "gui.joulesmith.logic.col.side": "Side",
    "gui.joulesmith.logic.col.if": "If", "gui.joulesmith.logic.col.value": "Value", "gui.joulesmith.logic.col.output": "Power",
    "gui.joulesmith.logic.outputs": "Powered:",
    "gui.joulesmith.logic.on": "Rule on (lamp: green while it holds, red while it doesn't)",
    "gui.joulesmith.logic.off": "Rule off",
    "gui.joulesmith.logic.reading": "%s, now %s",
    "gui.joulesmith.logic.side": "The side of the controller to read from",
    "gui.joulesmith.logic.no_side": "This reading doesn't use a side",
    "gui.joulesmith.logic.above": "Holds when the reading is above the value",
    "gui.joulesmith.logic.below": "Holds when the reading is below the value",
    "gui.joulesmith.logic.value": "The value to compare with",
    "gui.joulesmith.logic.output": "The side to power while the rule holds",
})
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})

# ---- tier 3 ore processing: Chemical Dissolver (ore + acid -> 1000 mB slurry) and Crystallizer
# (250 mB slurry -> one crystal); the Crusher turns each crystal into a dust: four per raw ore.
for m, (src, dust, plate, gear, wire) in METALS.items():
    if src not in ("ore", "vanilla"):
        continue
    slurry = f"{MOD}:{m}_slurry"
    machine_fluid("dissolving", f"{m}_slurry_from_raw", (ACID, 250), [(f"#c:raw_materials/{m}", 1)], [], (slurry, 1000))
    machine_fluid("dissolving", f"{m}_slurry_from_ore", (ACID, 250), [(f"#c:ores/{m}", 1)],
                  [(CRUSHED(ORE_BONUS[m]), 1, 0.15)], (slurry, 1000), energy=5000, time=200)
    machine_fluid("crystallizing", f"{m}_crystal", (slurry, 250), [], [(f"{MOD}:{m}_crystal", 1, 1)])
    machine("crushing", f"{m}_dust_from_crystal", [(f"{MOD}:{m}_crystal", 1)], [(DUST(m), 1, 1)], 1200, 60)
    lang[f"fluid_type.{MOD}.{m}_slurry"] = f"{title(m)} Slurry"
shaped("chemical_dissolver", ["PCP", "GMG", "PRP"],
       {"P": "#c:plates/electrum", "C": f"{MOD}:advanced_circuit", "G": "#c:gears/steel", "M": f"{MOD}:fluid_mixer", "R": "#c:dusts/redstone"})
shaped("crystallizer", ["PCP", "QTQ", "PRP"],
       {"P": "#c:plates/electrum", "C": f"{MOD}:advanced_circuit", "Q": "#c:gems/quartz", "T": f"{MOD}:fluid_tank", "R": "#c:dusts/redstone"})
lang.update({"jei.joulesmith.dissolving": "Dissolving", "jei.joulesmith.crystallizing": "Crystallizing"})

# ---- powered gear (tier 3): tools, weapon and Exo-Suit that run on FE, the Energy Cell, modules,
# the Charging Pad and the Modification Table
POWERED = {"energy_cell": "Energy Cell", "power_drill": "Power Drill", "chainsaw": "Chainsaw", "energy_sword": "Energy Sword"}
for iid, name in POWERED.items():
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated" if iid == "energy_cell" else "minecraft:item/handheld",
           "textures": {"layer0": f"{MOD}:item/{iid}"}})
    lang[f"item.{MOD}.{iid}"] = name
EXO = {"helmet": ("Exo-Suit Helmet", "head_armor", ["SCS", "SES"]),
       "chestplate": ("Exo-Suit Chestplate", "chest_armor", ["SES", "SCS", "SSS"]),
       "leggings": ("Exo-Suit Leggings", "leg_armor", ["SCS", "SES", "S S"]),
       "boots": ("Exo-Suit Boots", "foot_armor", ["SES", "S S"])}
for piece, (name, item_tag, pattern) in EXO.items():
    iid = f"exo_{piece}"
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{iid}"}})
    shaped(iid, pattern, {"S": "#c:plates/steel", "C": f"{MOD}:advanced_circuit", "E": f"{MOD}:energy_cell"})
    lang[f"item.{MOD}.{iid}"] = name
    gear_generated.append(f"item/{iid}")
gear_generated += ["models/armor/exo_layer_1", "models/armor/exo_layer_2"]
shaped("energy_cell", ["LWL", "RBR", "LWL"], {"L": "#c:plates/lead", "W": "#c:wires/gold",
                                              "R": "#c:dusts/redstone", "B": f"{MOD}:basic_battery"})
shaped("power_drill", [" D ", "SCS", "PEP"], {"D": "#c:gems/diamond", "S": "#c:plates/steel", "C": f"{MOD}:advanced_circuit",
                                              "P": "#c:plates/invar", "E": f"{MOD}:energy_cell"})
shaped("chainsaw", ["SAS", "SCS", "PEP"], {"A": "minecraft:iron_axe", "S": "#c:plates/steel", "C": f"{MOD}:advanced_circuit",
                                           "P": "#c:plates/invar", "E": f"{MOD}:energy_cell"})
shaped("energy_sword", [" D ", " D ", "CEC"], {"D": "#c:gems/diamond", "C": f"{MOD}:advanced_circuit", "E": f"{MOD}:energy_cell"})

MODULES = {
    "speed": ("Speed Module", "Mines 75% faster, using 50% more power per block", "tool", ["#c:dusts/redstone", "#c:dusts/glowstone"]),
    "range": ("Range Module", "Power Drill: digs 3x3 (sneak for one block)", "tool", ["minecraft:piston"]),
    "silk_touch": ("Silk Touch Module", "Silk Touch", "tool", ["minecraft:white_wool", "#c:gems/emerald"]),
    "fortune": ("Fortune Module", "Fortune III", "tool", ["#c:gems/diamond", "#c:gems/lapis"]),
    "auto_smelt": ("Auto-Smelt Module", "Smelts what you mine as it drops", "tool", ["minecraft:blast_furnace"]),
    "magnet": ("Magnet Module", "Drops land right at your feet", "tool_or_weapon", ["minecraft:ender_pearl", "#c:ingots/iron"]),
    "damage": ("Damage Module", "Sharpness V", "weapon", ["#c:gems/quartz", "#c:gems/diamond"]),
    "looting": ("Looting Module", "Looting III", "weapon", ["#c:gems/emerald", "#c:gems/lapis"]),
    "fire": ("Fire Module", "Fire Aspect II", "weapon", ["minecraft:blaze_powder", "minecraft:blaze_powder"]),
    "night_vision": ("Night Vision Module", "See in the dark", "helmet", ["minecraft:golden_carrot"]),
    "water_breathing": ("Water Breathing Module", "Breathe underwater", "helmet", ["minecraft:prismarine_crystals", "minecraft:pufferfish"]),
    "solar": ("Solar Module", "Charges the whole suit in daylight", "helmet", ["minecraft:daylight_detector"]),
    "jetpack": ("Jetpack Module", "Fly as in creative, using power while flying", "chestplate",
                ["minecraft:phantom_membrane", "minecraft:phantom_membrane", "minecraft:firework_rocket"]),
    "swiftness": ("Swiftness Module", "Run 35% faster", "leggings", ["minecraft:sugar", "minecraft:sugar"]),
    "jump_boost": ("Jump Boost Module", "Jump about two blocks high", "leggings", ["minecraft:rabbit_foot"]),
    "step_assist": ("Step Assist Module", "Walk straight up full blocks", "boots", ["#minecraft:stairs", "minecraft:piston"]),
    "fall_protection": ("Fall Protection Module", "No fall damage while charged", "boots", ["minecraft:slime_ball", "minecraft:feather"]),
}
FITS = {"tool": "Fits: Power Drill, Chainsaw", "weapon": "Fits: Energy Sword", "tool_or_weapon": "Fits: Power Drill, Chainsaw, Energy Sword",
        "helmet": "Fits: Exo-Suit Helmet", "chestplate": "Fits: Exo-Suit Chestplate", "leggings": "Fits: Exo-Suit Leggings",
        "boots": "Fits: Exo-Suit Boots"}
shaped("blank_module", ["EWE", "WCW", "EWE"], {"E": "#c:plates/electrum", "W": "#c:wires/gold", "C": f"{MOD}:advanced_circuit"})
for mid, (name, desc, kind, extras) in MODULES.items():
    iid = f"{mid}_module"
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{iid}"}})
    write(os.path.join(DATA, MOD, "recipe", f"{iid}.json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [ing(f"{MOD}:blank_module")] + [ing(e) for e in extras], "result": {"id": f"{MOD}:{iid}"}})
    recipes[iid] = True
    lang[f"item.{MOD}.{iid}"] = name
    lang[f"tooltip.{MOD}.module.{mid}"] = desc
write(os.path.join(ASSETS, "models", "item", "blank_module.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/blank_module"}})
lang[f"item.{MOD}.blank_module"] = "Blank Module"
lang.update({f"tooltip.{MOD}.module.fits.{k}": v for k, v in FITS.items()})

# Charging Pad: a 3-pixel-high plate, its coil glowing while it charges
for on in (False, True):
    suffix = "_on" if on else ""
    model(f"charging_pad{suffix}", {"parent": "minecraft:block/block", "textures": {
        "particle": f"{MOD}:block/machine_side", "top": f"{MOD}:block/charging_pad_top{suffix}",
        "side": f"{MOD}:block/machine_side", "bottom": f"{MOD}:block/machine_bottom"},
        "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": {
            "up": {"texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"},
            **{f: {"uv": [0, 13, 16, 16], "texture": "#side", "cullface": f} for f in ("north", "south", "east", "west")}}}]})
write(os.path.join(ASSETS, "blockstates", "charging_pad.json"), {"variants": {
    "lit=false": {"model": f"{MOD}:block/charging_pad"}, "lit=true": {"model": f"{MOD}:block/charging_pad_on"}}})
write(os.path.join(ASSETS, "models", "item", "charging_pad.json"), {"parent": f"{MOD}:block/charging_pad"})
simple_loot("charging_pad")
shaped("charging_pad", ["WPW", "SBS"], {"W": "#c:wires/gold", "P": "minecraft:heavy_weighted_pressure_plate",
                                       "S": "#c:plates/steel", "B": f"{MOD}:basic_battery"})

# Modification Table: a bench top over a column, with a small screen on the front
def table_faces(tex_front, box, side_uv):
    return {f: {"uv": side_uv, "texture": tex_front if f == "north" else "#side"} for f in ("north", "south", "east", "west")}
model("modification_table", {"parent": "minecraft:block/block", "textures": {
    "particle": f"{MOD}:block/modification_table_side", "top": f"{MOD}:block/modification_table_top",
    "side": f"{MOD}:block/modification_table_side", "front": f"{MOD}:block/modification_table_front",
    "bottom": f"{MOD}:block/machine_bottom"},
    "elements": [
        {"from": [0, 12, 0], "to": [16, 16, 16], "faces": {
            "up": {"texture": "#top", "cullface": "up"}, "down": {"texture": "#bottom"},
            **table_faces("#front", None, [0, 0, 16, 4])}},
        {"from": [2, 0, 2], "to": [14, 12, 14], "faces": {
            "down": {"uv": [2, 2, 14, 14], "texture": "#bottom", "cullface": "down"},
            **table_faces("#front", None, [2, 4, 14, 16])}}]})
write(os.path.join(ASSETS, "blockstates", "modification_table.json"), {"variants": {
    f"facing={f}": dict({"model": f"{MOD}:block/modification_table"}, **({"y": r} if r else {})) for f, r in ROTATION.items()}})
write(os.path.join(ASSETS, "models", "item", "modification_table.json"), {"parent": f"{MOD}:block/modification_table"})
simple_loot("modification_table")
shaped("modification_table", ["PCP", "SWS", "S S"], {"P": "#c:plates/electrum", "C": f"{MOD}:advanced_circuit",
                                                    "W": "minecraft:crafting_table", "S": "#c:plates/steel"})
for bid in ("charging_pad", "modification_table"):
    tags[("minecraft", "block", "mineable/pickaxe")].append(f"{MOD}:{bid}")
lang.update({
    f"block.{MOD}.charging_pad": "Charging Pad",
    f"block.{MOD}.modification_table": "Modification Table",
    f"container.{MOD}.modification_table": "Modification Table",
    "gui.joulesmith.modification_table.gear": "Powered gear: tools, Energy Sword or Exo-Suit",
    "gui.joulesmith.modification_table.module": "Modules for the gear on the left (two at most)",
    "tooltip.joulesmith.energy": "Energy: %s / %s FE",
    "tooltip.joulesmith.power_drill": "Pickaxe and shovel in one",
    "tooltip.joulesmith.chainsaw": "Fells a whole tree; sneak to cut one log",
    "tooltip.joulesmith.energy_sword": "Hits harder the fuller its charge",
    "tooltip.joulesmith.exo_suit": "While charged, each piece soaks up 20% of damage",
    "tooltip.joulesmith.energy_cell": "Charges the powered gear you carry (use to switch off)",
    "tooltip.joulesmith.energy_cell.off": "Switched off (use to switch on)",
    "message.joulesmith.energy_cell.on": "Energy Cell: charging your gear",
    "message.joulesmith.energy_cell.off": "Energy Cell: switched off",
})
for (ns, reg, path), values in tags.items():
    write(os.path.join(DATA, ns, "tags", reg, f"{path}.json"), {"replace": False, "values": values})
# ---- the new machines, upgrade cards and sawdust
shaped("alloy_smelter", ["IGI", "BFB", "IRI"],
       {"I": "#c:ingots/iron", "G": "#c:gears/copper", "B": "minecraft:bricks", "F": "minecraft:furnace", "R": "#c:dusts/redstone"})
shaped("compressor", ["IPI", "GBG", "IRI"],
       {"I": "#c:ingots/iron", "P": "minecraft:piston", "G": "#c:gears/copper", "B": "#c:storage_blocks/iron", "R": "#c:dusts/redstone"})
shaped("wire_drawer", ["IGI", "TST", "IRI"],
       {"I": "#c:ingots/iron", "G": "#c:gears/copper", "T": "#c:ingots/tin", "S": "minecraft:shears", "R": "#c:dusts/redstone"})
shaped("sawmill", ["ISI", "GAG", "IRI"],
       {"I": "#c:ingots/iron", "S": "minecraft:stonecutter", "G": "#c:gears/copper", "A": "minecraft:iron_axe", "R": "#c:dusts/redstone"})
shaped("speed_upgrade", ["RGR", "GPG", "RGR"], {"R": "#c:dusts/redstone", "G": "#c:ingots/gold", "P": "#c:plates/bronze"})
shaped("efficiency_upgrade", ["RLR", "LPL", "RLR"], {"R": "#c:dusts/redstone", "L": "#c:gems/lapis", "P": "#c:plates/invar"})
shaped("bonus_upgrade", ["RQR", "QPQ", "RQR"], {"R": "#c:dusts/redstone", "Q": "#c:gems/quartz", "P": "#c:plates/electrum"})
shaped("eject_upgrade", ["R R", "HPH", "R R"], {"R": "#c:dusts/redstone", "H": "minecraft:hopper", "P": "#c:plates/tin"})
UPGRADES = {"speed": "Speed Upgrade", "efficiency": "Efficiency Upgrade", "bonus": "Output Bonus Upgrade", "eject": "Auto-Eject Upgrade",
            "range": "Range Upgrade"}
for iid in [f"{u}_upgrade" for u in UPGRADES if u != "range"] + ["sawdust", "basic_circuit"]:
    write(os.path.join(ASSETS, "models", "item", f"{iid}.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{iid}"}})
lang.update({f"block.{MOD}.{k}": v["name"] for k, v in MACHINES.items()})
lang.update({f"block.{MOD}.{k}": v for k, v in CREATIVE.items()})
lang.update({f"block.{MOD}.{c}": n for c, n in CABLES.items()})
lang[f"fluid_type.{MOD}.steam"] = "Steam"
lang.update({f"item.{MOD}.{u}_upgrade": name for u, name in UPGRADES.items()})
lang.update({
    f"item.{MOD}.sawdust": "Sawdust",
    f"item.{MOD}.basic_circuit": "Basic Circuit",
    f"item.{MOD}.latex": "Latex",
    f"item.{MOD}.rubber": "Rubber",
    f"item.{MOD}.tree_tap": "Tree Tap",
    **{f"block.{MOD}.{b}": n for b, n in RUBBER.items()},
    "jei.joulesmith.bio_reacting": "Bio Reacting",
    "tooltip.joulesmith.tree_tap": "Use on a rubber log's latex spot",
    "tooltip.joulesmith.area_tool": "Breaks 3x3; sneak to break one block",
    "tooltip.joulesmith.crate.slots": "%s of %s slots used",
    "tooltip.joulesmith.crate.more": "...and %s more",
    "tooltip.joulesmith.bin.empty": "Empty (holds %s of one item)",
    "tooltip.joulesmith.bin.holds": "%s: %s / %s",
    "tooltip.joulesmith.bin.use": "Right-click: put in. Left-click: take a stack (sneak: one)",
    "tooltip.joulesmith.upgrade.range": "Farm machines: each card widens the area (5x5, 9x9, 13x13)",
    **{f"block.{MOD}.{f}": n for f, n in FLUIDS.items()},
    **{f"fluid_type.{MOD}.{f}": n for f, n in FLUIDS.items()},
    **{f"item.{MOD}.{f}_bucket": f"{n} Bucket" for f, n in FLUIDS.items()},
    "jei.joulesmith.washing": "Ore Washing",
    "jei.joulesmith.mixing": "Fluid Mixing",
    "jei.joulesmith.centrifuging": "Centrifuging",
    "jei.joulesmith.circuit_assembling": "Circuit Assembling",
    "jei.joulesmith.alloy_smelting": "Alloy Smelting",
    "jei.joulesmith.compressing": "Compressing",
    "jei.joulesmith.wire_drawing": "Wire Drawing",
    "jei.joulesmith.sawing": "Sawing",
    "jei.joulesmith.chance": "%s%% chance",
    "jei.joulesmith.time": "%s s",
    "gui.joulesmith.upgrade_slot": "Upgrade slot",
    "gui.joulesmith.upgrade_slot.hint": "Takes Speed, Efficiency, Output Bonus or Auto-Eject cards",
    "tooltip.joulesmith.upgrade.speed": "Each card: runs 50% faster, using 25% more power per run",
    "tooltip.joulesmith.upgrade.efficiency": "Each card: 20% less power per run",
    "tooltip.joulesmith.upgrade.bonus": "Each card: +10% byproduct chance and 10% chance of an extra output",
    "tooltip.joulesmith.upgrade.eject": "Pushes finished items out of the machine's output faces",
})
lang.update({f"block.{MOD}.{k}": v for k, v in PIPES.items()})
lang.update({
    f"block.{MOD}.fluid_tank": "Fluid Tank",
    f"item.{MOD}.wrench": "Wrench",
    f"item.{MOD}.filter_card": "Filter Card",
    "message.joulesmith.pipe.mode.push": "Push: sends into this block",
    "message.joulesmith.pipe.mode.pull": "Pull: takes out of this block",
    "message.joulesmith.pipe.mode.both": "Push and pull",
    "message.joulesmith.pipe.mode.disabled": "Off",
    "message.joulesmith.pipe.joined": "Pipes joined",
    "message.joulesmith.pipe.split": "Pipes split apart",
    "message.joulesmith.pipe.nothing": "Nothing to connect to on that side",
    "message.joulesmith.pipe.no_options": "Nothing else to set on this pipe",
    "message.joulesmith.routing": "Routing: %s",
    "message.joulesmith.routing.round_robin": "Round robin (takes turns)",
    "message.joulesmith.routing.nearest": "Nearest first",
    "message.joulesmith.routing.furthest": "Furthest first",
    "message.joulesmith.routing.pull_only": "Routing only applies to a side set to pull",
    "message.joulesmith.filter.added": "Filter Card fitted",
    "message.joulesmith.filter.removed": "Filter Card removed",
    "message.joulesmith.filter.no_inventory": "Fit Filter Cards on a side that touches a block",
    "message.joulesmith.fluid": "%s: %s / %s mB",
    "message.joulesmith.fluid.empty": "Empty: 0 / %s mB",
    "gui.joulesmith.filter.whitelist": "Whitelist: only these items pass",
    "gui.joulesmith.filter.blacklist": "Blacklist: everything but these items passes",
    "gui.joulesmith.filter.hint": "Click with an item to add it; click with an empty hand to clear",
    "tooltip.joulesmith.filter.whitelist": "Whitelist",
    "tooltip.joulesmith.filter.blacklist": "Blacklist",
    "tooltip.joulesmith.filter.use": "Use to edit; right-click a duct's side to fit",
    "tooltip.joulesmith.wrench.pipe": "Pipe side: push, pull, both or off",
    "tooltip.joulesmith.wrench.sneak": "Sneak on a duct side: remove filter or change routing",
    "tooltip.joulesmith.wrench.machine": "Machine: turn it to face the next way",
})
lang.update({
    "jei.joulesmith.crushing": "Crushing",
    "jei.joulesmith.energy": "%s FE",
    "gui.joulesmith.burn_time": "Burning: %s s left",
})
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
    "generated": gear_generated + ["block/rubber_log", "block/rubber_log_top", "block/rubber_planks", "block/rubber_leaves", "block/rubber_sapling"] + [f"block/{b}" for b, k, m in blocks if k in GENERATED_BLOCK_KINDS] +
                 [f"item/{i}" for i, f, m in items if f in GENERATED_ITEM_FORMS]})
