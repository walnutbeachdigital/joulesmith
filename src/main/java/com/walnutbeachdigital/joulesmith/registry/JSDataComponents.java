package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.item.filter.FilterContents;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data stored on Joulesmith items: a filter card's list, a tank's fluid. */
public final class JSDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Joulesmith.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilterContents>> FILTER =
            COMPONENTS.registerComponentType("filter", b -> b.persistent(FilterContents.CODEC).networkSynchronized(FilterContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID =
            COMPONENTS.registerComponentType("fluid", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BinContents>> BIN =
            COMPONENTS.registerComponentType("bin", b -> b.persistent(BinContents.CODEC).networkSynchronized(BinContents.STREAM_CODEC));

    private JSDataComponents() {}
}
