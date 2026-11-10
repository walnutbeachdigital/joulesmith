package com.walnutbeachdigital.joulesmith;

import com.walnutbeachdigital.joulesmith.client.BinRenderer;
import com.walnutbeachdigital.joulesmith.client.CrateScreen;
import com.walnutbeachdigital.joulesmith.client.FilterScreen;
import com.walnutbeachdigital.joulesmith.client.LogicScreen;
import com.walnutbeachdigital.joulesmith.client.FluidTankRenderer;
import com.walnutbeachdigital.joulesmith.client.MachineScreen;
import com.walnutbeachdigital.joulesmith.client.MetalTexturePack;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

// Client-only entry point. Screens, renderers and models are hooked up here as they are added.
@Mod(value = Joulesmith.MODID, dist = Dist.CLIENT)
public class JoulesmithClient {
    public JoulesmithClient(IEventBus modEventBus) {
        modEventBus.addListener(JoulesmithClient::addPacks);
        modEventBus.addListener(JoulesmithClient::screens);
        modEventBus.addListener(JoulesmithClient::renderers);
        modEventBus.addListener(JoulesmithClient::fluids);
        modEventBus.addListener(JoulesmithClient::setup);
    }

    /** Joulesmith fluids reuse vanilla's water textures, tinted to each fluid's colour. */
    private static void fluids(RegisterClientExtensionsEvent event) {
        for (JSFluids.Entry fluid : JSFluids.ALL) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
                @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
                @Override public ResourceLocation getOverlayTexture() { return ResourceLocation.withDefaultNamespace("block/water_overlay"); }
                @Override public int getTintColor() { return fluid.color; }
            }, fluid.type.get());
        }
    }

    @SuppressWarnings("deprecation")
    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (JSFluids.Entry fluid : JSFluids.ALL) {        // see-through, like water
                ItemBlockRenderTypes.setRenderLayer(fluid.source.get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(fluid.flowing.get(), RenderType.translucent());
            }
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(JSBlockEntities.FLUID_TANK.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(JSBlockEntities.BULK_BIN.get(), BinRenderer::new);
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(JSMenus.MACHINE.get(), MachineScreen::new);
        event.register(JSMenus.FILTER.get(), FilterScreen::new);
        event.register(JSMenus.CRATE.get(), CrateScreen::new);
        event.register(JSMenus.LOGIC.get(), LogicScreen::new);
        event.register(JSMenus.MODIFICATION_TABLE.get(), com.walnutbeachdigital.joulesmith.client.ModificationTableScreen::new);
    }

    /** Adds the built-in pack that colours vanilla-shaped metal textures through each metal's gradient. */
    private static void addPacks(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(MetalTexturePack.source());
        }
    }
}
