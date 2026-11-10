package com.walnutbeachdigital.joulesmith.compat.jei;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI support. Crafting, smelting and blasting recipes show up in JEI on their own; this plugin adds
 * what JEI can't work out by itself. Machine recipe pages are added here as the machines are built.
 * JEI only loads this class when JEI is installed, so Joulesmith never requires it.
 */
@JeiPlugin
public class JoulesmithJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Joulesmith.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Where each ore generates, shown on the ore blocks and the raw ore.
        for (Metal metal : Metal.values()) {
            if (!metal.hasOre()) continue;
            Component info = Component.translatable("jei." + Joulesmith.MODID + ".info.ore." + metal.id());
            registration.addIngredientInfo(JSBlocks.ORE.get(metal).get(), info);
            registration.addIngredientInfo(JSBlocks.DEEPSLATE_ORE.get(metal).get(), info);
            registration.addIngredientInfo(JSItems.RAW.get(metal).get(), info);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // the Electric Furnace runs every smelting recipe
        registration.addRecipeCatalyst(new ItemStack(JSBlocks.ELECTRIC_FURNACE.get()), RecipeTypes.SMELTING);
    }
}
