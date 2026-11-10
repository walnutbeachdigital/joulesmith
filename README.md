# Joulesmith

An all-in-one tech mod for Minecraft 1.21.1 on NeoForge: power, item and fluid transport, processing, automation, farming, storage, a digital storage network with auto-crafting, and powered armor, tools and weapons. Version 1.0 covers all five tiers, from a coal generator to fission and fusion reactors, drones and a plasma cannon.

Everything speaks NeoForge's standard energy (FE), item and fluid capabilities, so Joulesmith plugs into Mekanism, Thermal, Create, AE2 and other tech mods in a pack.

## What's in it

- **Power:** Combustion, Biofuel, Geothermal, Solar (five tiers), Wind and Steam generators, a Fission Reactor (a 3x3x3 multiblock that shuts down safely, never explodes) and a Fusion Core; batteries and cables in five tiers; wireless power on dye-coloured channels.
- **Transport:** item ducts, fluid pipes and energy cables set per side with the Wrench; filter cards; pumps, tanks and conveyor belts.
- **Processing:** fourteen machines that raise ore yield tier by tier, up to five ingots per raw ore with the Plasma Refinery; the Laser Engraver makes the Processors that open tiers 4 and 5.
- **Automation:** placers, breakers, an autocrafter, quarry, mob fighter, Drone Hub, sensors and a Logic Controller.
- **Farming:** planters, harvesters, tree farms, sprinklers, hydroponics, an animal rancher, a mob grinder, a Mob Duplicator and an auto enchanter.
- **Storage:** crates, bulk bins and tanks in five tiers, and a storage network: Network Core, Drive Bays with Item and Fluid Drives, Network, Crafting and Wireless Terminals, Import and Export Buses, and an Auto-Crafting CPU that crafts from recorded Patterns.
- **Gear:** copper, bronze and steel tools and armor, then FE-powered drills, chainsaws, an energy sword, the Omni-Tool, Railgun and Plasma Cannon, and the Exo-Suit in three tiers (up to six modules), customised with modules including flight.

See [CHANGELOG.md](CHANGELOG.md) for the full list.

## Optional mods

JEI (recipes), Jade or The One Probe (block info), CC: Tweaked (Lua methods on every machine) and Curios (a belt slot for Energy Cells). None are required.

## Configuration

Balance settings (generator, fission and fusion output, machine speed and power use, powered gear costs) are in `config/joulesmith-common.toml`. Recipes, loot tables and ore generation are ordinary data, so data packs can change them.

## Languages

English, plus Simplified Chinese, Russian, German, Spanish (Spain and Mexico) and Brazilian Portuguese. Block, item and screen text is translated; the longer JEI info pages and tooltip descriptions are still English in those languages. Corrections are welcome.

## Building

```
./gradlew build
```

The jar is in `build/libs/`. Run a test client with `./gradlew runClient`.

## License

MIT. See [LICENSE](LICENSE).
