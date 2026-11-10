package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

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

    static {
        blockItem(JSBlocks.ELECTRIC_FURNACE);
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
