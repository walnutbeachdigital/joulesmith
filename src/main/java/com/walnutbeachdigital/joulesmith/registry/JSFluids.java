package com.walnutbeachdigital.joulesmith.registry;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Joulesmith's fluids. Each has a source and flowing fluid, a block (so it can be placed in the world) and a
 * bucket. They are drawn with vanilla's water texture tinted to their own colour.
 */
public final class JSFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Joulesmith.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Joulesmith.MODID);

    public static final List<Entry> ALL = new ArrayList<>();

    /** One fluid and everything that goes with it. {@code color} is ARGB, used to tint the water texture. */
    public static final class Entry {
        public final String name;
        public final int color;
        public DeferredHolder<FluidType, FluidType> type;
        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public DeferredBlock<LiquidBlock> block;
        public DeferredItem<BucketItem> bucket;
        private BaseFlowingFluid.Properties properties;

        Entry(String name, int color) {
            this.name = name;
            this.color = color;
        }

        BaseFlowingFluid.Properties properties() {
            if (properties == null) {
                properties = new BaseFlowingFluid.Properties(type, source, flowing);
                if (bucket != null) properties.bucket(bucket).block(block);
            }
            return properties;
        }

        /** Whether it has a bucket and can be placed in the world (steam can't). */
        public boolean placeable() {
            return bucket != null;
        }
    }

    public static final Entry SULFURIC_ACID = register("sulfuric_acid", 0xE0D9D23A, 1100, 1100, 300);
    public static final Entry COOLANT = register("coolant", 0xE03ED6E0, 1000, 1000, 250);
    public static final Entry BIOFUEL = register("biofuel", 0xE07FA331, 900, 1200, 300);
    /** Steam only lives in tanks and pipes: no bucket, no block. */
    public static final Entry STEAM = register("steam", 0xC0E6ECF0, -1000, 200, 400, false);
    /** Experience from the Mob Grinder, 20 mB a point, for the Auto Enchanter or bottling. Tanks and pipes only. */
    public static final Entry LIQUID_XP = register("liquid_xp", 0xE09CE84A, 800, 800, 300, false);

    /** Ore slurry from the Chemical Dissolver, one per ore metal, for the Crystallizer. Tanks and pipes only. */
    public static final java.util.Map<com.walnutbeachdigital.joulesmith.material.Metal, Entry> SLURRY =
            new java.util.EnumMap<>(com.walnutbeachdigital.joulesmith.material.Metal.class);

    static {
        for (var metal : com.walnutbeachdigital.joulesmith.material.Metal.values()) {
            if (metal.hasOre() || !metal.hasIngot()) {
                SLURRY.put(metal, register(metal.id() + "_slurry", 0xE0000000 | metal.rampStep(4), 1400, 2000, 320, false));
            }
        }
    }

    private static Entry register(String name, int color, int density, int viscosity, int temperature) {
        return register(name, color, density, viscosity, temperature, true);
    }

    private static Entry register(String name, int color, int density, int viscosity, int temperature, boolean placeable) {
        Entry e = new Entry(name, color);
        e.type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + Joulesmith.MODID + "." + name)
                .density(density).viscosity(viscosity).temperature(temperature)
                .canConvertToSource(false).canExtinguish(name.equals("coolant"))));
        e.source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(e.properties()));
        e.flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(e.properties()));
        if (placeable) {
            e.block = JSBlocks.BLOCKS.register(name, () -> new LiquidBlock(e.source.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
            e.bucket = JSItems.ITEMS.register(name + "_bucket", () -> new BucketItem(e.source.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            JSItems.TAB_ORDER.add(e.bucket);
        }
        ALL.add(e);
        return e;
    }

    private JSFluids() {}
}
