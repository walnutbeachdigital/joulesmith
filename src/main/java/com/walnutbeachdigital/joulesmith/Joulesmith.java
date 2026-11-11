package com.walnutbeachdigital.joulesmith;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSCapabilities;
import com.walnutbeachdigital.joulesmith.registry.JSCreativeTabs;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;
import com.walnutbeachdigital.joulesmith.registry.JSGear;
import com.walnutbeachdigital.joulesmith.registry.JSItems;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;
import com.walnutbeachdigital.joulesmith.registry.JSRecipes;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Joulesmith.MODID)
public class Joulesmith {
    public static final String MODID = "joulesmith";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Joulesmith(IEventBus modEventBus, ModContainer modContainer) {
        JSFluids.TYPES.register(modEventBus);          // also adds the fluid blocks and buckets
        JSFluids.FLUIDS.register(modEventBus);
        JSGear.ARMOR_MATERIALS.register(modEventBus);  // also adds the tools and armor
        JSBlocks.BLOCKS.register(modEventBus);
        JSItems.ITEMS.register(modEventBus);
        JSCreativeTabs.TABS.register(modEventBus);
        JSBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JSMenus.MENUS.register(modEventBus);
        JSDataComponents.COMPONENTS.register(modEventBus);
        JSRecipes.TYPES.register(modEventBus);
        JSRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(JSCapabilities::register);
        modEventBus.addListener(com.walnutbeachdigital.joulesmith.network.TerminalPayloads::register);
        modEventBus.addListener(com.walnutbeachdigital.joulesmith.transport.item.ItemTransits::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) ->
                com.walnutbeachdigital.joulesmith.energy.wireless.WirelessEnergyBlockEntity.clear());
        com.walnutbeachdigital.joulesmith.registry.JSSounds.SOUNDS.register(modEventBus);
        com.walnutbeachdigital.joulesmith.guide.HandbookProgress.register(modEventBus);
        com.walnutbeachdigital.joulesmith.gear.powered.PoweredGearEvents.register();
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, JSConfig.SPEC);
        modEventBus.addListener(com.walnutbeachdigital.joulesmith.compat.top.TopCompat::enqueue);
        // ComputerCraft: Lua methods for every machine (the compat class only loads when CC: Tweaked is present)
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            if (net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
                event.enqueueWork(com.walnutbeachdigital.joulesmith.compat.cc.CCCompat::register);
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
