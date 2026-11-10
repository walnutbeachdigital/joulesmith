package com.walnutbeachdigital.joulesmith;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSCapabilities;
import com.walnutbeachdigital.joulesmith.registry.JSCreativeTabs;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
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
        JSBlocks.BLOCKS.register(modEventBus);
        JSItems.ITEMS.register(modEventBus);
        JSCreativeTabs.TABS.register(modEventBus);
        JSBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JSMenus.MENUS.register(modEventBus);
        JSDataComponents.COMPONENTS.register(modEventBus);
        JSRecipes.TYPES.register(modEventBus);
        JSRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(JSCapabilities::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
