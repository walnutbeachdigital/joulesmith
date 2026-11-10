package com.walnutbeachdigital.joulesmith;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Balance settings, in config/joulesmith-common.toml. Pack makers can change power output, machine speed and
 * cost, and powered gear costs without touching code. Recipes, ore generation and loot are data packs.
 */
public final class JSConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MACHINE_SPEED, MACHINE_ENERGY, SOLAR;
    public static final ModConfigSpec.IntValue COMBUSTION, BIOFUEL, GEOTHERMAL, STEAM_FE_PER_MB, WIND_MIN, WIND_MAX;
    public static final ModConfigSpec.IntValue TOOL_COST, SWORD_COST, JETPACK_COST, GRINDER_DAMAGE, RANCHER_CAP;
    public static final ModConfigSpec.DoubleValue EXO_ABSORB;
    public static final ModConfigSpec.BooleanValue SOUNDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Processing machines").push("machines");
        MACHINE_SPEED = b.comment("Multiplies how fast every recipe machine works (2.0 = twice as fast)")
                .defineInRange("speed", 1.0, 0.1, 10.0);
        MACHINE_ENERGY = b.comment("Multiplies the power every recipe machine uses per job")
                .defineInRange("energy", 1.0, 0.0, 10.0);
        SOUNDS = b.comment("Whether machines make a quiet sound while they run").define("sounds", true);
        b.pop();

        b.comment("Generators, in FE per tick").push("generators");
        COMBUSTION = b.defineInRange("combustion", 40, 1, 100_000);
        BIOFUEL = b.defineInRange("biofuel", 160, 1, 100_000);
        GEOTHERMAL = b.comment("Most it makes; touching lava gives 20 each").defineInRange("geothermal", 120, 1, 100_000);
        STEAM_FE_PER_MB = b.comment("FE the Steam Turbine makes per mB of steam").defineInRange("steamFePerMb", 2, 1, 1_000);
        WIND_MIN = b.comment("Wind Turbine output at y 64").defineInRange("windMin", 50, 0, 100_000);
        WIND_MAX = b.comment("Wind Turbine output at y 192 and above").defineInRange("windMax", 300, 0, 100_000);
        SOLAR = b.comment("Multiplies every Solar Panel's output").defineInRange("solarMultiplier", 1.0, 0.0, 100.0);
        b.pop();

        b.comment("Powered gear").push("gear");
        TOOL_COST = b.comment("FE per block for the Power Drill and Chainsaw").defineInRange("toolCost", 200, 0, 100_000);
        SWORD_COST = b.comment("FE per Energy Sword hit").defineInRange("swordCost", 400, 0, 100_000);
        JETPACK_COST = b.comment("FE per tick while flying with the Jetpack module").defineInRange("jetpackCost", 60, 0, 100_000);
        EXO_ABSORB = b.comment("Share of damage each charged Exo-Suit piece soaks up").defineInRange("exoAbsorb", 0.2, 0.0, 0.25);
        b.pop();

        b.comment("Farming").push("farming");
        GRINDER_DAMAGE = b.comment("Damage the Mob Grinder deals per hit").defineInRange("grinderDamage", 8, 1, 1_000);
        RANCHER_CAP = b.comment("Animals the Animal Rancher breeds up to").defineInRange("rancherCap", 16, 2, 256);
        b.pop();
        SPEC = b.build();
    }

    /** A setting's value, or its default before the config has loaded. */
    public static int get(ModConfigSpec.IntValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }

    private JSConfig() {}
}
