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
    public static final Map<UpgradeCardItem.Type, DeferredItem<UpgradeCardItem>> UPGRADE = new EnumMap<>(UpgradeCardItem.Type.class);
    public static final DeferredItem<Item> SAWDUST = ITEMS.registerSimpleItem("sawdust");
    public static final DeferredItem<Item> BASIC_CIRCUIT = ITEMS.registerSimpleItem("basic_circuit");
    public static final DeferredItem<Item> LATEX = ITEMS.registerSimpleItem("latex");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");
    public static final DeferredItem<TreeTapItem> TREE_TAP = ITEMS.register("tree_tap", () -> new TreeTapItem(new Item.Properties().durability(128)));
    public static final Map<Metal, DeferredItem<Item>> CRUSHED = new EnumMap<>(Metal.class);
    public static final DeferredItem<FilterCardItem> FILTER_CARD = register("filter_card", () -> new FilterCardItem(new Item.Properties().stacksTo(16)));

    static {
        blockItem(JSBlocks.COMBUSTION_GENERATOR);
        blockItem(JSBlocks.BASIC_BATTERY);
        blockItem(JSBlocks.ENERGY_CABLE);
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
        blockItem(JSBlocks.PLANTER);
        blockItem(JSBlocks.HARVESTER);
        blockItem(JSBlocks.TREE_FARM);
        blockItem(JSBlocks.BIO_REACTOR);
        blockItem(JSBlocks.BIOFUEL_GENERATOR);
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
        TAB_ORDER.add(BASIC_CIRCUIT);
        blockItem(JSBlocks.PUMP);
        blockItem(JSBlocks.ITEM_DUCT);
        blockItem(JSBlocks.REINFORCED_ITEM_DUCT);
        blockItem(JSBlocks.FLUID_PIPE);
        blockItem(JSBlocks.REINFORCED_FLUID_PIPE);
        register("fluid_tank", () -> new FluidTankItem(JSBlocks.FLUID_TANK.get(), new Item.Properties()));
        register("basic_crate", () -> new CrateItem(JSBlocks.BASIC_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("reinforced_crate", () -> new CrateItem(JSBlocks.REINFORCED_CRATE.get(), new Item.Properties().stacksTo(1)));
        register("basic_bulk_bin", () -> new BinItem(JSBlocks.BASIC_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
        register("reinforced_bulk_bin", () -> new BinItem(JSBlocks.REINFORCED_BULK_BIN.get(), new Item.Properties().stacksTo(1)));
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
