package com.walnutbeachdigital.joulesmith;

import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.material.Metal;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

// Client-only entry point. Screens, renderers and models are hooked up here as they are added.
@Mod(value = Joulesmith.MODID, dist = Dist.CLIENT)
public class JoulesmithClient {
    public JoulesmithClient(IEventBus modEventBus) {
        modEventBus.addListener(JoulesmithClient::itemColors);
        modEventBus.addListener(JoulesmithClient::blockColors);
    }

    /** Ingots, nuggets, raw ore and dusts reuse vanilla's textures, tinted to each metal's colour. */
    private static void itemColors(RegisterColorHandlersEvent.Item event) {
        for (Map<Metal, DeferredItem<Item>> form : List.of(JSItems.INGOT, JSItems.NUGGET, JSItems.DUST)) {
            form.forEach((metal, item) -> event.register((stack, layer) -> layer == 0 ? metal.tint() : -1, item.get()));
        }
        JSItems.RAW.forEach((metal, item) -> event.register((stack, layer) -> layer == 0 ? metal.rawTint() : -1, item.get()));
        JSBlocks.STORAGE_BLOCK.forEach((metal, block) -> event.register((stack, layer) -> layer == 0 ? metal.tint() : -1, block.get()));
        JSBlocks.RAW_BLOCK.forEach((metal, block) -> event.register((stack, layer) -> layer == 0 ? metal.rawTint() : -1, block.get()));
    }

    /** Storage blocks and raw blocks reuse vanilla's iron block textures, tinted the same way. */
    private static void blockColors(RegisterColorHandlersEvent.Block event) {
        JSBlocks.STORAGE_BLOCK.forEach((metal, block) -> event.register((state, level, pos, layer) -> layer == 0 ? metal.tint() : -1, block.get()));
        JSBlocks.RAW_BLOCK.forEach((metal, block) -> event.register((state, level, pos, layer) -> layer == 0 ? metal.rawTint() : -1, block.get()));
    }
}
