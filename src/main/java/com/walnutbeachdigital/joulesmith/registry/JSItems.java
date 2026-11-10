package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.item.WrenchItem;
import com.walnutbeachdigital.joulesmith.item.filter.FilterCardItem;
import com.walnutbeachdigital.joulesmith.material.Metal;
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
        for (UpgradeCardItem.Type type : UpgradeCardItem.Type.values()) {
            UPGRADE.put(type, register(type.id() + "_upgrade", () -> new UpgradeCardItem(type, new Item.Properties().stacksTo(16))));
        }
        TAB_ORDER.add(SAWDUST);
        blockItem(JSBlocks.PUMP);
        blockItem(JSBlocks.ITEM_DUCT);
        blockItem(JSBlocks.REINFORCED_ITEM_DUCT);
        blockItem(JSBlocks.FLUID_PIPE);
        blockItem(JSBlocks.REINFORCED_FLUID_PIPE);
        register("fluid_tank", () -> new FluidTankItem(JSBlocks.FLUID_TANK.get(), new Item.Properties()));
        blockItem(JSBlocks.CREATIVE_POWER_CELL);
        for (Metal metal : Metal.values()) {
            String id = metal.id();
            if (metal.hasOre()) {
                blockItem(JSBlocks.ORE.get(metal));
                blockItem(JSBlocks.DEEPSLATE_ORE.get(metal));
                blockItem(JSBlocks.RAW_BLOCK.get(metal));
                RAW.put(metal, item("raw_" + id));
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
