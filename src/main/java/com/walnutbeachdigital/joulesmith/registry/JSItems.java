package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.farming.rubber.TreeTapItem;
import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.item.WrenchItem;
import com.walnutbeachdigital.joulesmith.item.filter.FilterCardItem;
import com.walnutbeachdigital.joulesmith.material.Metal;
import com.walnutbeachdigital.joulesmith.storage.bin.BinItem;
import com.walnutbeachdigital.joulesmith.storage.crate.CrateItem;
import com.walnutbeachdigital.joulesmith.storage.tank.FluidTankItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JSItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Joulesmith.MODID);

    /** Every item in creative-tab order. */
    public static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final Map<Metal, DeferredItem<Item>> RAW = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> INGOT = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> NUGGET = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> DUST = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> PLATE = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> GEAR = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> WIRE = new EnumMap<>(Metal.class);

    public static final DeferredItem<WrenchItem> WRENCH = register("wrench", () -> new WrenchItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.automation.MobImprintItem> MOB_IMPRINT = register("mob_imprint",
            () -> new com.walnutbeachdigital.joulesmith.automation.MobImprintItem(new Item.Properties()));
    public static final Map<UpgradeCardItem.Type, DeferredItem<UpgradeCardItem>> UPGRADE = new EnumMap<>(UpgradeCardItem.Type.class);
    public static final DeferredItem<Item> SAWDUST = ITEMS.registerSimpleItem("sawdust");
    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.registerSimpleItem("basic_circuit");
    /** Crafting parts: coils for generators and heaters, motors for anything that turns. */
    public static final DeferredItem<Item> COPPER_COIL = ITEMS.registerSimpleItem("copper_coil");
    public static final DeferredItem<Item> ELECTRUM_COIL = ITEMS.registerSimpleItem("electrum_coil");
    public static final DeferredItem<Item> MOTOR = ITEMS.registerSimpleItem("motor");
    /** Machine casings: the frame every machine of a tier is built on. */
    public static final DeferredItem<Item> BASIC_CASING = ITEMS.registerSimpleItem("basic_casing");
    public static final DeferredItem<Item> REINFORCED_CASING = ITEMS.registerSimpleItem("reinforced_casing");
    public static final DeferredItem<Item> ADVANCED_CASING = ITEMS.registerSimpleItem("advanced_casing");
    public static final DeferredItem<Item> ELITE_CASING = ITEMS.registerSimpleItem("elite_casing");
    public static final DeferredItem<Item> ULTIMATE_CASING = ITEMS.registerSimpleItem("ultimate_casing");
    /** The tier 4 and 5 gate parts, from the Laser Engraver. */
    public static final DeferredItem<Item> PROCESSOR = ITEMS.registerSimpleItem("processor");
    public static final DeferredItem<Item> QUANTUM_PROCESSOR = ITEMS.registerSimpleItem("quantum_processor");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");
    public static final DeferredItem<Item> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");
    public static final DeferredItem<com.walnutbeachdigital.joulesmith.farming.FertilizerItem> FERTILIZER = ITEMS.register("fertilizer",
            () -> new com.walnutbeachdigital.joulesmith.farming.FertilizerItem(new Item.Properties()));
    public static final DeferredItem<Item> LATEX = ITEMS.registerSimpleItem("latex");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");
    public static final DeferredItem<TreeTapItem> TREE_TAP = ITEMS.register("tree_tap", () -> new TreeTapItem(new Item.Properties().durability(128)));
    public static final Map<com.walnutbeachdigital.joulesmith.gear.powered.Module, DeferredItem<com.walnutbeachdigital.joulesmith.gear.powered.ModuleItem>> MODULES =
            new EnumMap<>(com.walnutbeachdigital.joulesmith.gear.powered.Module.class);
    public static final Map<Metal, DeferredItem<Item>> CRUSHED = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredItem<Item>> CRYSTAL = new EnumMap<>(Metal.class);
    public static final DeferredItem<FilterCardItem> FILTER_CARD = register("filter_card", () -> new FilterCardItem(new Item.Properties().stacksTo(16)));

    static {
        blockItem(JSBlocks.COMBUSTION_GENERATOR);
        blockItem(JSBlocks.BASIC_BATTERY);
        blockItem(JSBlocks.REINFORCED_BATTERY);
        blockItem(JSBlocks.ADVANCED_BATTERY);
        blockItem(JSBlocks.ELITE_BATTERY);
        blockItem(JSBlocks.ULTIMATE_BATTERY);
        blockItem(JSBlocks.BOILER);
        blockItem(JSBlocks.STEAM_TURBINE);
        blockItem(JSBlocks.SOLAR_PANEL);
        blockItem(JSBlocks.REINFORCED_SOLAR_PANEL);
        blockItem(JSBlocks.ADVANCED_SOLAR_PANEL);
        blockItem(JSBlocks.ELITE_SOLAR_PANEL);
        blockItem(JSBlocks.ULTIMATE_SOLAR_PANEL);
        blockItem(JSBlocks.GEOTHERMAL_GENERATOR);
        blockItem(JSBlocks.FISSION_REACTOR);
        blockItem(JSBlocks.REACTOR_CASING);
        blockItem(JSBlocks.REACTOR_GLASS);
        blockItem(JSBlocks.REACTOR_PORT);
        blockItem(JSBlocks.WIRELESS_TRANSMITTER);
        blockItem(JSBlocks.WIRELESS_RECEIVER);
        blockItem(JSBlocks.MOB_DUPLICATOR);
        blockItem(JSBlocks.WIND_TURBINE);
        JSBlocks.SENSORS.values().forEach(JSItems::blockItem);
        blockItem(JSBlocks.LOGIC_CONTROLLER);
        blockItem(JSBlocks.BLOCK_PLACER);
        blockItem(JSBlocks.BLOCK_BREAKER);
        blockItem(JSBlocks.AUTOCRAFTER);
        blockItem(JSBlocks.FLUID_PLACER);
        blockItem(JSBlocks.FLUID_DRAIN);
        blockItem(JSBlocks.MECHANICAL_USER);
        blockItem(JSBlocks.QUARRY);
        blockItem(JSBlocks.MOB_FIGHTER);
        blockItem(JSBlocks.ENERGY_CABLE);
        blockItem(JSBlocks.REINFORCED_ENERGY_CABLE);
        blockItem(JSBlocks.ADVANCED_ENERGY_CABLE);
        blockItem(JSBlocks.ELITE_ENERGY_CABLE);
        blockItem(JSBlocks.ULTIMATE_ENERGY_CABLE);
        blockItem(JSBlocks.ELECTRIC_FURNACE);
        blockItem(JSBlocks.CRUSHER);
        blockItem(JSBlocks.ALLOY_SMELTER);
        blockItem(JSBlocks.COMPRESSOR);
        blockItem(JSBlocks.WIRE_DRAWER);
        blockItem(JSBlocks.SAWMILL);
        blockItem(JSBlocks.ORE_WASHER);
        blockItem(JSBlocks.FLUID_MIXER);
        blockItem(JSBlocks.CENTRIFUGE);
        blockItem(JSBlocks.CIRCUIT_ASSEMBLER);
        blockItem(JSBlocks.CHEMICAL_DISSOLVER);
        blockItem(JSBlocks.CRYSTALLIZER);
        blockItem(JSBlocks.LASER_ENGRAVER);
        blockItem(JSBlocks.PLANTER);
        blockItem(JSBlocks.HARVESTER);
        blockItem(JSBlocks.TREE_FARM);
        blockItem(JSBlocks.BIO_REACTOR);
        blockItem(JSBlocks.BIOFUEL_GENERATOR);
        blockItem(JSBlocks.SPRINKLER);
        blockItem(JSBlocks.FERTILIZER_MIXER);
        TAB_ORDER.add(FERTILIZER);
        blockItem(JSBlocks.ANIMAL_RANCHER);
        blockItem(JSBlocks.HYDROPONIC_BED);
        blockItem(JSBlocks.MOB_GRINDER);
        blockItem(JSBlocks.AUTO_ENCHANTER);
        blockItem(JSBlocks.RUBBER_LOG);
        blockItem(JSBlocks.RUBBER_LEAVES);
        blockItem(JSBlocks.RUBBER_SAPLING);
        blockItem(JSBlocks.RUBBER_PLANKS);
        TAB_ORDER.add(TREE_TAP);
        TAB_ORDER.add(LATEX);
        TAB_ORDER.add(RUBBER);
        for (UpgradeCardItem.Type type : UpgradeCardItem.Type.values()) {
            UPGRADE.put(type, register(type.id() + "_upgrade", () -> new UpgradeCardItem(type, new Item.Properties().stacksTo(16))));
        }
        TAB_ORDER.add(SAWDUST);
        TAB_ORDER.add(COPPER_COIL);
        TAB_ORDER.add(ELECTRUM_COIL);
        TAB_ORDER.add(MOTOR);
        TAB_ORDER.add(BASIC_CASING);
        TAB_ORDER.add(REINFORCED_CASING);
        TAB_ORDER.add(ADVANCED_CASING);
        TAB_ORDER.add(ELITE_CASING);
        TAB_ORDER.add(ULTIMATE_CASING);
        TAB_ORDER.add(PROCESSOR);
        TAB_ORDER.add(QUANTUM_PROCESSOR);
        TAB_ORDER.add(BASIC_CIRCUIT);
        TAB_ORDER.add(ADVANCED_CIRCUIT);
        TAB_ORDER.add(PLASTIC_SHEET);
        blockItem(JSBlocks.PUMP);
        blockItem(JSBlocks.ADVANCED_PUMP);
        blockItem(JSBlocks.ITEM_DUCT);
        blockItem(JSBlocks.REINFORCED_ITEM_DUCT);
        blockItem(JSBlocks.FLUID_PIPE);
        blockItem(JSBlocks.REINFORCED_FLUID_PIPE);
        blockItem(JSBlocks.ADVANCED_ITEM_DUCT);
        blockItem(JSBlocks.ADVANCED_FLUID_PIPE);
        blockItem(JSBlocks.ELITE_ITEM_DUCT);
        blockItem(JSBlocks.ELITE_FLUID_PIPE);
        blockItem(JSBlocks.ULTIMATE_ITEM_DUCT);
        blockItem(JSBlocks.ULTIMATE_FLUID_PIPE);
        blockItem(JSBlocks.CONVEYOR_BELT);
        register("fluid_tank", () -> new FluidTankItem(JSBlocks.FLUID_TANK.get(), new Item.Properties()));
        register("reinforced_fluid_tank", () -> new FluidTankItem(JSBlocks.REINFORCED_FLUID_TANK.get(), new Item.Properties()));
        register("advanced_fluid_tank", () -> new FluidTankItem(JSBlocks.ADVANCED_FLUID_TANK.get(), new Item.Properties()));
        register("elite_fluid_tank", () -> new FluidTankItem(JSBlocks.ELITE_FLUID_TANK.get(), new Item.Properties()));
        register("ultimate_fluid_tank", () -> new FluidTankItem(JSBlocks.ULTIMATE_FLUID_TANK.get(), new Item.Properties()));
        register("basic_crate", () -> new CrateItem(JSBlocks.BASIC_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("reinforced_crate", () -> new CrateItem(JSBlocks.REINFORCED_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("basic_bulk_bin", () -> new BinItem(JSBlocks.BASIC_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("reinforced_bulk_bin", () -> new BinItem(JSBlocks.REINFORCED_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("advanced_crate", () -> new CrateItem(JSBlocks.ADVANCED_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("advanced_bulk_bin", () -> new BinItem(JSBlocks.ADVANCED_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("elite_crate", () -> new CrateItem(JSBlocks.ELITE_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("elite_bulk_bin", () -> new BinItem(JSBlocks.ELITE_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("ultimate_crate", () -> new CrateItem(JSBlocks.ULTIMATE_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("ultimate_bulk_bin", () -> new BinItem(JSBlocks.ULTIMATE_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("elite_upgrade_kit", () -> new com.walnutbeachdigital.joulesmith.item.UpgradeKitItem(4, new Item.Properties()));
        register("ultimate_upgrade_kit", () -> new com.walnutbeachdigital.joulesmith.item.UpgradeKitItem(5, new Item.Properties()));
        register("reinforced_upgrade_kit", () -> new com.walnutbeachdigital.joulesmith.item.UpgradeKitItem(2, new Item.Properties()));
        register("advanced_upgrade_kit", () -> new com.walnutbeachdigital.joulesmith.item.UpgradeKitItem(3, new Item.Properties()));
        blockItem(JSBlocks.CHARGING_PAD);
        blockItem(JSBlocks.MODIFICATION_TABLE);
        item("blank_module");
        for (var module : com.walnutbeachdigital.joulesmith.gear.powered.Module.values()) {
            MODULES.put(module, register(module.id() + "_module",
                    () -> new com.walnutbeachdigital.joulesmith.gear.powered.ModuleItem(module, new Item.Properties().stacksTo(16))));
        }
        blockItem(JSBlocks.CREATIVE_POWER_CELL);
        for (Metal metal : Metal.values()) {
            String id = metal.id();
            if (metal.hasOre()) {
                blockItem(JSBlocks.ORE.get(metal));
                blockItem(JSBlocks.DEEPSLATE_ORE.get(metal));
                blockItem(JSBlocks.RAW_BLOCK.get(metal));
                RAW.put(metal, item("raw_" + id));
            }
            if (metal.hasOre() || !metal.hasIngot()) {         // ore metals and vanilla's iron, copper, gold
                CRUSHED.put(metal, item("crushed_" + id));
                CRYSTAL.put(metal, item(id + "_crystal"));
            }
            if (metal.hasIngot()) {
                blockItem(JSBlocks.STORAGE_BLOCK.get(metal));
                INGOT.put(metal, item(id + "_ingot"));
                NUGGET.put(metal, item(id + "_nugget"));
            }
            if (metal.hasDust()) DUST.put(metal, item(id + "_dust"));
            if (metal.hasPlate()) PLATE.put(metal, item(id + "_plate"));
            if (metal.hasGear()) GEAR.put(metal, item(id + "_gear"));
            if (metal.hasWire()) WIRE.put(metal, item(id + "_wire"));
        }
    }

    private static <I extends Item> DeferredItem<I> register(String name, Supplier<I> factory) {
        DeferredItem<I> item = ITEMS.register(name, factory);
        TAB_ORDER.add(item);
        return item;
    }

    private static DeferredItem<Item> item(String name) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(name);
        TAB_ORDER.add(item);
        return item;
    }

    private static void blockItem(DeferredBlock<? extends Block> block) {
        TAB_ORDER.add(ITEMS.registerSimpleBlockItem(block));
    }

    private JSItems() {}
}
