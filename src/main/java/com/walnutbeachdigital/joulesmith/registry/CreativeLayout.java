package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Where every Joulesmith item goes in the creative tabs, and in what order. JEI lists items in creative-tab
 * order too, so this also orders JEI. Patterns: {metal} runs through the metals, {tier} through the tiers
 * (no prefix or basic, then reinforced, advanced, elite, ultimate). Anything not listed lands at the end of
 * the Materials tab, so a new item is never missing.
 */
public final class CreativeLayout {
    public enum Tab { MATERIALS, MACHINES, LOGISTICS, GEAR }

    private static final String[] METALS = {"tin", "lead", "silver", "nickel", "uranium", "copper", "iron", "gold",
            "bronze", "steel", "invar", "electrum"};
    private static final String[] TIERS = {"", "basic_", "reinforced_", "advanced_", "elite_", "ultimate_"};

    private static final Map<Tab, List<String>> ORDER = Map.of(
            Tab.MATERIALS, List.of(
                    "{metal}_ore", "deepslate_{metal}_ore", "raw_{metal}", "crushed_{metal}", "{metal}_crystal", "{metal}_dust",
                    "{metal}_ingot", "{metal}_nugget", "{metal}_plate", "{metal}_gear", "{metal}_wire", "{metal}_coil",
                    "{metal}_block", "raw_{metal}_block",
                    "rubber_log", "rubber_planks", "rubber_leaves", "rubber_sapling", "latex", "rubber", "plastic_sheet",
                    "sawdust", "fertilizer",
                    "basic_circuit", "advanced_circuit", "processor", "quantum_processor", "motor", "{tier}casing",
                    "sulfuric_acid_bucket", "coolant_bucket", "biofuel_bucket", "oil_bucket", "liquid_latex_bucket"),
            Tab.MACHINES, List.of(
                    "combustion_generator", "{tier}solar_panel", "biofuel_generator", "geothermal_generator", "wind_turbine",
                    "boiler", "steam_turbine", "fission_reactor", "reactor_casing", "reactor_glass", "reactor_port",
                    "fusion_core", "creative_power_cell",
                    "electric_furnace", "crusher", "alloy_smelter", "compressor", "wire_drawer", "sawmill", "ore_washer",
                    "fluid_mixer", "centrifuge", "circuit_assembler", "chemical_dissolver", "crystallizer", "laser_engraver",
                    "plasma_refinery",
                    "planter", "harvester", "tree_farm", "bio_reactor", "sprinkler", "fertilizer_mixer", "animal_rancher",
                    "hydroponic_bed", "mob_grinder", "mob_duplicator", "auto_enchanter",
                    "block_placer", "block_breaker", "autocrafter", "fluid_placer", "fluid_drain", "mechanical_user", "quarry",
                    "mob_fighter", "drone_hub",
                    "inventory_sensor", "tank_sensor", "energy_sensor", "time_sensor", "proximity_sensor", "logic_controller",
                    "speed_upgrade", "efficiency_upgrade", "bonus_upgrade", "eject_upgrade", "range_upgrade"),
            Tab.LOGISTICS, List.of(
                    "wrench", "{tier}upgrade_kit",
                    "{tier}energy_cable", "{tier}battery", "wireless_transmitter", "wireless_receiver",
                    "{tier}item_duct", "filter_card", "conveyor_belt",
                    "{tier}fluid_pipe", "pump", "advanced_pump", "{tier}fluid_tank",
                    "{tier}crate", "{tier}bulk_bin",
                    "network_cable", "network_core", "drive_bay", "{tier}item_drive", "{tier}fluid_drive",
                    "network_terminal", "crafting_terminal", "wireless_terminal",
                    "import_bus", "export_bus", "fluid_import_bus", "fluid_export_bus",
                    "crafting_cpu", "blank_pattern"),
            Tab.GEAR, List.of(
                    "copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe",
                    "copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots",
                    "bronze_sword", "bronze_pickaxe", "bronze_axe", "bronze_shovel", "bronze_hoe",
                    "bronze_helmet", "bronze_chestplate", "bronze_leggings", "bronze_boots",
                    "steel_sword", "steel_pickaxe", "steel_axe", "steel_shovel", "steel_hoe",
                    "steel_helmet", "steel_chestplate", "steel_leggings", "steel_boots",
                    "steel_hammer", "steel_excavator", "tree_tap",
                    "basic_energy_cell", "reinforced_energy_cell", "energy_cell", "elite_energy_cell", "ultimate_energy_cell",
                    "charging_pad", "power_drill", "chainsaw", "omni_tool", "energy_sword", "railgun", "plasma_cannon",
                    "exo_helmet", "exo_chestplate", "exo_leggings", "exo_boots",
                    "elite_exo_helmet", "elite_exo_chestplate", "elite_exo_leggings", "elite_exo_boots",
                    "ultimate_exo_helmet", "ultimate_exo_chestplate", "ultimate_exo_leggings", "ultimate_exo_boots",
                    "modification_table", "blank_module", "{module}",
                    "mob_imprint", "drone"));

    /** Not shown: an empty Pattern only exists by encoding one. */
    private static final java.util.Set<String> HIDDEN = java.util.Set.of("pattern");

    private record Place(Tab tab, int index) {}

    private static Map<String, Place> places;

    private static Map<String, Place> places() {
        if (places != null) return places;
        Map<String, Place> map = new HashMap<>();
        for (Tab tab : Tab.values()) {
            int i = 0;
            for (String pattern : ORDER.get(tab)) {
                for (String id : expand(pattern)) map.putIfAbsent(id, new Place(tab, i++));
            }
        }
        return places = map;
    }

    private static List<String> expand(String pattern) {
        List<String> out = new ArrayList<>();
        if (pattern.contains("{metal}")) {
            for (String m : METALS) out.add(pattern.replace("{metal}", m));
        } else if (pattern.contains("{tier}")) {
            for (String t : TIERS) out.add(pattern.replace("{tier}", t));
        } else if (pattern.equals("{module}")) {
            for (var m : com.walnutbeachdigital.joulesmith.gear.powered.Module.values()) out.add(m.id() + "_module");
        } else {
            out.add(pattern);
        }
        return out;
    }

    /** The items in a tab, in order. */
    public static List<Item> items(Tab tab) {
        Map<String, Place> map = places();
        List<Item> out = new ArrayList<>();
        List<Item> unlisted = new ArrayList<>();
        for (DeferredHolder<Item, ? extends Item> holder : JSItems.ITEMS.getEntries()) {
            Item item = holder.get();
            Place place = map.get(BuiltInRegistries.ITEM.getKey(item).getPath());
            if (HIDDEN.contains(BuiltInRegistries.ITEM.getKey(item).getPath())) continue;
            if (place == null) {
                if (tab == Tab.MATERIALS) unlisted.add(item);
            } else if (place.tab() == tab) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparingInt(item -> map.get(BuiltInRegistries.ITEM.getKey(item).getPath()).index()));
        out.addAll(unlisted);
        return out;
    }

    private CreativeLayout() {}
}
