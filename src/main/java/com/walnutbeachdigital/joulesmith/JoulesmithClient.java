package com.walnutbeachdigital.joulesmith;

import com.walnutbeachdigital.joulesmith.client.MetalTexturePack;

import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;

// Client-only entry point. Screens, renderers and models are hooked up here as they are added.
@Mod(value = Joulesmith.MODID, dist = Dist.CLIENT)
public class JoulesmithClient {
    public JoulesmithClient(IEventBus modEventBus) {
        modEventBus.addListener(JoulesmithClient::addPacks);
    }

    /** Adds the built-in pack that colours vanilla-shaped metal textures through each metal's gradient. */
    private static void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(MetalTexturePack.source());
        }
    }
}
