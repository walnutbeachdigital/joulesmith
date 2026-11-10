# Changelog

## 1.0.0

The first release: all five tiers of Joulesmith for Minecraft 1.21.1 on NeoForge.

### Materials
- Tin, lead, silver, nickel and uranium ores, with raw ores, ingots, nuggets, dusts, plates, gears, wires and storage blocks.
- Alloys: bronze, steel, invar and electrum.
- Rubber trees in forests; tap them for latex, then rubber.

### Tiers
- Five tiers: Basic, Reinforced, Advanced, Elite and Ultimate, each with its own machine casing and trim colour.
- Basic and Advanced Circuits (Circuit Assembler) open tiers 2 and 3; Processors and Quantum Processors (Laser Engraver) open tiers 4 and 5.
- Upgrade Kits raise cables, pipes, batteries, crates, bins, tanks and solar panels a tier where they stand, up to Ultimate.

### Power
- Combustion, Biofuel and Geothermal Generators, Solar Panels (five tiers), Wind Turbine, Boiler and Steam Turbine.
- Fission Reactor: a 3x3x3 multiblock of Reactor Casing, Glass and Ports that burns uranium for 1,000 FE/t, cooled by water; it shuts itself down when it overheats and never damages the world.
- Fusion Core: lit with 500,000 FE, then burns deuterium and tritium for 20,000 FE/t and makes plasma.
- Batteries and Energy Cables in five tiers, up to 1,000,000,000 FE and 250,000 FE/t.
- Wireless Transmitter and Receiver: power over 16 dye-coloured channels within 256 blocks.
- Every energy bar shows the FE/t a block makes or uses; hot machines show their heat and a status line.

### Transport
- Item Ducts, Fluid Pipes and Energy Cables in five tiers, set per side with the Wrench.
- Filter Cards (exact item, tag or mod; whitelist or blacklist) and routing modes.
- Pump and Advanced Pump, Fluid Tanks in five sizes, Conveyor Belt.

### Processing
- Electric Furnace, Crusher, Alloy Smelter, Compressor, Wire Drawer, Sawmill, Ore Washer, Fluid Mixer, Centrifuge, Circuit Assembler, Chemical Dissolver, Crystallizer, Laser Engraver and Plasma Refinery.
- Ore yield climbs with each tier: 2, 3, 4, then 5 ingots per raw ore.
- Speed, Efficiency, Bonus, Eject and Range upgrade cards.
- Fluids: sulfuric acid, coolant, biofuel, oil, liquid latex, steam, liquid XP, ore slurries, nutrient paste, deuterium, tritium and plasma.

### Automation and logic
- Block Placer, Block Breaker, Autocrafter, Fluid Placer and Drain, Mechanical User, Quarry and Mob Fighter.
- Drone Hub: up to four drones fetch dropped items and harvest and replant crops within 24 blocks.
- Sensors (inventory, tank, energy, time, proximity) and the Logic Controller.
- Redstone modes on every machine: ignored, on with signal, on without signal, never, pulse.

### Farming
- Planter, Harvester, Tree Farm, Bio Reactor, Sprinkler, Fertilizer Mixer, Animal Rancher, Hydroponic Bed, Mob Grinder and Auto Enchanter.
- Mob Duplicator: grows copies of the mob on a Mob Imprint from Nutrient Paste and power.

### Storage
- Crates, Bulk Bins and Fluid Tanks in five tiers.
- Storage network: Network Cable, Network Core, Drive Bays, Item Drives (up to 2,097,152 items) and Fluid Drives (up to 32,768,000 mB) in four sizes.
- Network Terminal (searchable, @mod to search by mod), Crafting Terminal (refills its grid from the network) and Wireless Terminal (64 blocks).
- Import and Export Buses for items and fluids, with filters.
- Auto-Crafting CPU: encode recipes on Patterns at a Crafting Terminal, then middle-click an item in any terminal to have it crafted, ingredients and all.

### Gear
- Copper, bronze and steel tools and armor; steel Hammer and Excavator (3x3).
- Powered gear: Power Drill, Chainsaw, Energy Sword, Omni-Tool, Railgun and Plasma Cannon; Energy Cells in five tiers and the Charging Pad.
- Exo-Suit, Elite Exo-Suit (four module slots) and Ultimate Exo-Suit (six), upgraded at a Smithing Table with modules and charge kept.
- The Modification Table and 24 modules, including the Jetpack (hover and climb) and the tier 5 Flight module.

### Compatibility
- JEI, Jade, The One Probe, ComputerCraft (CC: Tweaked) and Curios, all optional.
- Works with any mod that uses NeoForge energy, item and fluid capabilities.
- Balance settings in `config/joulesmith-common.toml`; recipes, loot and ore generation are data packs.
- Translations: Simplified Chinese, Russian, German, Spanish (Spain and Mexico) and Brazilian Portuguese.
