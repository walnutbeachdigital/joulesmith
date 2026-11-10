package com.walnutbeachdigital.joulesmith.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The Engineer's Handbook's quest tree: a chapter per tier and one for the storage network. Each quest is
 * "have this item"; a quest opens once every quest it links from is done. Each chapter has one key quest (the
 * part that opens the next chapter), drawn bigger with a gold frame. Positions are worked out here: each
 * quest's column is one right of the furthest quest it needs, main-path quests sit on the middle row, and
 * the rest go as close to the rows of what they need as the column has room.
 */
public final class HandbookQuests {
    public enum Kind { MAIN, OPTIONAL, KEY }

    public enum Chapter {
        BASIC(null), REINFORCED("basic_circuit"), ADVANCED("advanced_circuit"), ELITE("processor"),
        ULTIMATE("quantum_processor"), NETWORK("basic_circuit");

        /** The quest that opens this chapter (null: always open). */
        public final String opener;

        Chapter(String opener) {
            this.opener = opener;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public Component displayName() {
            return Component.translatable("handbook.joulesmith.chapter." + id());
        }
    }

    public record Quest(String id, Chapter chapter, ResourceLocation itemId, Kind kind, List<String> needs, float x, float y) {
        public Item item() {
            return BuiltInRegistries.ITEM.get(itemId);
        }

        public Component title() {
            return item().getDescription();
        }
    }

    private record Draft(String id, Kind kind, String item, List<String> needs) {}

    private static final Map<String, Quest> BY_ID = new LinkedHashMap<>();
    private static final Map<Chapter, List<Draft>> DRAFTS = new LinkedHashMap<>();
    private static Chapter current;

    public static Quest get(String id) {
        return BY_ID.get(id);
    }

    public static List<Quest> all() {
        return new ArrayList<>(BY_ID.values());
    }

    public static List<Quest> chapter(Chapter chapter) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : BY_ID.values()) if (q.chapter() == chapter) out.add(q);
        return out;
    }

    public static Quest key(Chapter chapter) {
        for (Quest q : chapter(chapter)) if (q.kind() == Kind.KEY) return q;
        return null;
    }

    private static void add(Kind kind, String id, String... needs) {
        DRAFTS.computeIfAbsent(current, c -> new ArrayList<>()).add(new Draft(id, kind, id, List.of(needs)));
    }

    private static void main(String id, String... needs) { add(Kind.MAIN, id, needs); }
    private static void opt(String id, String... needs) { add(Kind.OPTIONAL, id, needs); }
    private static void key(String id, String... needs) { add(Kind.KEY, id, needs); }

    static {
        current = Chapter.BASIC;
        main("raw_tin");
        main("tin_ingot", "raw_tin");
        main("combustion_generator", "tin_ingot");
        main("energy_cable", "combustion_generator");
        main("crusher", "energy_cable");
        main("alloy_smelter", "crusher");
        main("bronze_ingot", "alloy_smelter");
        main("circuit_assembler", "bronze_ingot");
        key("basic_circuit", "circuit_assembler");
        opt("wrench", "tin_ingot");
        opt("item_duct", "tin_ingot");
        opt("fluid_pipe", "tin_ingot");
        opt("tree_tap", "tin_ingot");
        opt("latex", "tree_tap");
        opt("rubber", "latex");
        opt("electric_furnace", "energy_cable");
        opt("basic_battery", "energy_cable");
        opt("solar_panel", "energy_cable");
        opt("fluid_tank", "fluid_pipe");
        opt("pump", "fluid_pipe");
        opt("basic_crate", "item_duct");
        opt("compressor", "crusher");
        opt("wire_drawer", "compressor");
        opt("sawmill", "electric_furnace");
        opt("planter", "electric_furnace");
        opt("harvester", "planter");
        opt("block_placer", "crusher");
        opt("block_breaker", "block_placer");
        opt("bronze_pickaxe", "bronze_ingot");
        opt("basic_energy_cell", "basic_battery");

        current = Chapter.REINFORCED;
        main("steel_ingot");
        main("reinforced_casing", "steel_ingot");
        main("fluid_mixer", "reinforced_casing");
        main("plastic_sheet", "fluid_mixer");
        main("electrum_ingot");
        key("advanced_circuit", "plastic_sheet", "electrum_ingot");
        opt("ore_washer", "reinforced_casing");
        opt("biofuel_generator", "reinforced_casing");
        opt("bio_reactor", "biofuel_generator");
        opt("geothermal_generator", "reinforced_casing");
        opt("centrifuge", "reinforced_casing");
        opt("tree_farm", "reinforced_casing");
        opt("autocrafter", "reinforced_casing");
        opt("fertilizer_mixer", "bio_reactor");
        opt("animal_rancher", "reinforced_casing");
        opt("fluid_placer", "reinforced_casing");
        opt("fluid_drain", "fluid_placer");
        opt("inventory_sensor", "reinforced_casing");
        opt("energy_sensor", "inventory_sensor");
        opt("reinforced_battery", "steel_ingot");
        opt("reinforced_upgrade_kit", "steel_ingot");
        opt("filter_card", "steel_ingot");
        opt("speed_upgrade", "reinforced_casing");
        opt("steel_pickaxe", "steel_ingot");
        opt("steel_hammer", "steel_pickaxe");
        opt("reinforced_solar_panel", "reinforced_casing");
        opt("conveyor_belt", "steel_ingot");

        current = Chapter.ADVANCED;
        main("advanced_casing");
        main("elite_casing", "advanced_casing");
        main("laser_engraver", "elite_casing");
        key("processor", "laser_engraver");
        opt("chemical_dissolver", "advanced_casing");
        opt("crystallizer", "chemical_dissolver");
        opt("boiler", "advanced_casing");
        opt("steam_turbine", "boiler");
        opt("wind_turbine", "advanced_casing");
        opt("quarry", "advanced_casing");
        opt("mob_grinder", "advanced_casing");
        opt("auto_enchanter", "mob_grinder");
        opt("logic_controller", "advanced_casing");
        opt("hydroponic_bed", "advanced_casing");
        opt("advanced_solar_panel", "advanced_casing");
        opt("advanced_battery", "advanced_casing");
        opt("energy_cell");
        opt("charging_pad", "energy_cell");
        opt("power_drill", "energy_cell");
        opt("chainsaw", "energy_cell");
        opt("energy_sword", "energy_cell");
        opt("exo_chestplate", "energy_cell");
        opt("modification_table", "advanced_casing");
        opt("blank_module", "modification_table");
        opt("jetpack_module", "blank_module");

        current = Chapter.ELITE;
        main("uranium_ingot");
        main("reactor_casing");
        main("reactor_port", "reactor_casing");
        main("fission_reactor", "reactor_port", "uranium_ingot");
        key("quantum_processor", "fission_reactor");
        opt("reactor_glass", "reactor_casing");
        opt("wireless_transmitter");
        opt("wireless_receiver", "wireless_transmitter");
        opt("omni_tool");
        opt("railgun");
        opt("mob_imprint");
        opt("mob_duplicator", "mob_imprint");
        opt("elite_exo_chestplate");
        opt("elite_battery");
        opt("elite_solar_panel");
        opt("drone");

        current = Chapter.ULTIMATE;
        main("ultimate_casing");
        key("fusion_core", "ultimate_casing");
        opt("plasma_refinery", "fusion_core");
        opt("drone_hub", "ultimate_casing");
        opt("plasma_cannon", "ultimate_casing");
        opt("ultimate_exo_chestplate", "ultimate_casing");
        opt("flight_module", "ultimate_exo_chestplate");
        opt("ultimate_battery", "ultimate_casing");
        opt("ultimate_solar_panel", "ultimate_casing");

        current = Chapter.NETWORK;
        main("network_cable");
        main("network_terminal", "network_cable");
        main("network_core", "network_cable");
        main("drive_bay", "network_core");
        main("basic_item_drive", "drive_bay");
        main("crafting_terminal", "network_terminal", "basic_item_drive");
        main("blank_pattern", "crafting_terminal");
        key("crafting_cpu", "blank_pattern");
        opt("import_bus", "network_terminal");
        opt("export_bus", "import_bus");
        opt("basic_fluid_drive", "drive_bay");
        opt("fluid_import_bus", "basic_fluid_drive");
        opt("advanced_item_drive", "basic_item_drive");
        opt("wireless_terminal", "network_terminal");

        layout();
    }

    /** Places each chapter's quests on the grid (see the class comment). */
    private static void layout() {
        for (var entry : DRAFTS.entrySet()) {
            Map<String, Draft> byId = new LinkedHashMap<>();
            for (Draft d : entry.getValue()) byId.put(d.id(), d);
            Map<String, Integer> depth = new HashMap<>();
            for (Draft d : entry.getValue()) depthOf(d.id(), byId, depth);
            int maxDepth = depth.values().stream().max(Integer::compare).orElse(0);
            Map<String, Float> rows = new HashMap<>();
            for (int col = 0; col <= maxDepth; col++) {
                List<Float> taken = new ArrayList<>();
                for (int pass = 0; pass < 2; pass++) {               // main path and key first, then the rest
                    for (Draft d : entry.getValue()) {
                        if (depth.get(d.id()) != col || (d.kind() == Kind.OPTIONAL) != (pass == 1)) continue;
                        float want = 0;
                        if (d.kind() == Kind.OPTIONAL) {
                            float sum = 0;
                            int n = 0;
                            for (String need : d.needs()) {
                                if (rows.containsKey(need)) { sum += rows.get(need); n++; }
                            }
                            want = n == 0 ? 1.5f : sum / n;
                        }
                        float y = want;
                        for (int k = 0; k < 64; k++) {
                            float up = want - k * 1.5f, down = want + k * 1.5f;
                            if (!near(taken, down)) { y = down; break; }
                            if (!near(taken, up)) { y = up; break; }
                        }
                        taken.add(y);
                        rows.put(d.id(), y);
                        BY_ID.put(d.id(), new Quest(d.id(), entry.getKey(), ResourceLocation.fromNamespaceAndPath(Joulesmith.MODID, d.item()),
                                d.kind(), d.needs(), col * 2, y));
                    }
                }
            }
        }
    }

    private static boolean near(List<Float> taken, float y) {
        for (float t : taken) if (Math.abs(t - y) < 1.4f) return true;
        return false;
    }

    private static int depthOf(String id, Map<String, Draft> byId, Map<String, Integer> depth) {
        Integer known = depth.get(id);
        if (known != null) return known;
        int d = 0;
        Draft draft = byId.get(id);
        if (draft != null) for (String need : draft.needs()) if (byId.containsKey(need)) d = Math.max(d, depthOf(need, byId, depth) + 1);
        depth.put(id, d);
        return d;
    }

    private HandbookQuests() {}
}
