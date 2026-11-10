package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import java.util.List;

import com.walnutbeachdigital.joulesmith.gear.powered.Module;
import com.walnutbeachdigital.joulesmith.item.filter.FilterContents;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.component.DataComponentType;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data stored on Joulesmith items: a filter card's list, a tank's fluid, a tool's charge and modules. */
public final class JSDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Joulesmith.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterContents>> FILTER =
            COMPONENTS.registerComponentType("filter", b -> b.persistent(FilterContents.CODEC).networkSynchronized(FilterContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID =
            COMPONENTS.registerComponentType("fluid", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BinContents>> BIN =
            COMPONENTS.registerComponentType("bin", b -> b.persistent(BinContents.CODEC).networkSynchronized(BinContents.STREAM_CODEC));

    /** Forge Energy held by a powered tool, weapon, armor piece or Energy Cell. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** The modules fitted to a powered item at the Modification Table. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<Module>>> MODULES =
            COMPONENTS.registerComponentType("modules", b -> b.persistent(Module.CODEC.listOf()).networkSynchronized(Module.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** What an Item Drive holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<com.walnutbeachdigital.joulesmith.storage.bin.BinContents>>> DRIVE =
            COMPONENTS.registerComponentType("drive", b -> b.persistent(com.walnutbeachdigital.joulesmith.storage.bin.BinContents.CODEC.listOf())
                    .networkSynchronized(com.walnutbeachdigital.joulesmith.storage.bin.BinContents.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** The recipe recorded on a Pattern. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.walnutbeachdigital.joulesmith.network.PatternContents>> PATTERN =
            COMPONENTS.registerComponentType("pattern", b -> b.persistent(com.walnutbeachdigital.joulesmith.network.PatternContents.CODEC)
                    .networkSynchronized(com.walnutbeachdigital.joulesmith.network.PatternContents.STREAM_CODEC));

    private JSDataComponents() {}
}
