package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JSCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Joulesmith.MODID);

    /** Materials keeps the old "main" id so it stays where players had it. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = tab("main", "itemGroup.joulesmith",
            CreativeLayout.Tab.MATERIALS, () -> JSItems.INGOT.get(Metal.TIN).get().getDefaultInstance(), CreativeModeTabs.SPAWN_EGGS);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINES = tab("machines", "itemGroup.joulesmith.machines",
            CreativeLayout.Tab.MACHINES, () -> JSBlocks.CRUSHER.get().asItem().getDefaultInstance(), MAIN.getKey());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LOGISTICS = tab("logistics", "itemGroup.joulesmith.logistics",
            CreativeLayout.Tab.LOGISTICS, () -> JSBlocks.ITEM_DUCT.get().asItem().getDefaultInstance(), MACHINES.getKey());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR = tab("gear", "itemGroup.joulesmith.gear",
            CreativeLayout.Tab.GEAR, () -> JSGear.POWER_DRILL.get().getDefaultInstance(), LOGISTICS.getKey());

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, String title, CreativeLayout.Tab layout,
            java.util.function.Supplier<net.minecraft.world.item.ItemStack> icon, net.minecraft.resources.ResourceKey<CreativeModeTab> after) {
        return TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.translatable(title))
                .withTabsBefore(after)
                .icon(icon)
                .displayItems((params, output) -> CreativeLayout.items(layout).forEach(output::accept))
                .build());
    }

    private JSCreativeTabs() {}
}
