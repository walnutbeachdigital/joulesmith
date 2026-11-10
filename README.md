# Joulesmith

An all-in-one tech mod for Minecraft 1.21.1 on NeoForge: power, item and fluid transport, processing, automation, farming, storage, and powered armor, tools and weapons. Version 1.0 covers tiers 1 to 3, from a coal generator to steam turbines, quarries and the Exo-Suit.

Everything speaks NeoForge's standard energy (FE), item and fluid capabilities, so Joulesmith plugs into Mekanism, Thermal, Create, AE2 and other tech mods in a pack.

## What's in it

- **Power:** Combustion, Biofuel, Geothermal, Solar (three tiers), Wind and Steam generators; batteries and cables in three tiers.
- **Transport:** item ducts, fluid pipes and energy cables set per side with the Wrench; filter cards; pumps, tanks and conveyor belts.
- **Processing:** twelve machines that raise ore yield tier by tier, up to four ingots per raw ore.
- **Automation:** placers, breakers, an autocrafter, quarry, mob fighter, sensors and a Logic Controller.
- **Farming:** planters, harvesters, tree farms, sprinklers, hydroponics, an animal rancher, a mob grinder and an auto enchanter.
- **Gear:** copper, bronze and steel tools and armor, then FE-powered drills, chainsaws, an energy sword and the Exo-Suit, customised with modules.

See [CHANGELOG.md](CHANGELOG.md) for the full list.

## Optional mods

JEI (recipes), Jade or The One Probe (block info), CC: Tweaked (Lua methods on every machine) and Curios (a belt slot for Energy Cells). None are required.

## Configuration

Balance settings (generator output, machine speed and power use, powered gear costs) are in `config/joulesmith-common.toml`. Recipes, loot tables and ore generation are ordinary data, so data packs can change them.

## Building

```
./gradlew build
```

The jar is in `build/libs/`. Run a test client with `./gradlew runClient`.

## License

MIT. See [LICENSE](LICENSE).
