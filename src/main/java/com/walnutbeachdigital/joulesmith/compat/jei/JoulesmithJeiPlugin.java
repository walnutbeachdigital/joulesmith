package com.walnutbeachdigital.joulesmith.compat.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.client.MachineScreen;
import com.walnutbeachdigital.joulesmith.material.Metal;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * JEI support: a page for every Joulesmith machine's recipes, the machines as catalysts, a clickable
 * progress arrow on each machine screen, and where each ore generates. JEI only loads this class when JEI
 * is installed, so Joulesmith never requires it.
 */
@JeiPlugin
public class JoulesmithJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Joulesmith.id("jei_plugin");

    /** Which block runs each kind of recipe. */
    private static Map<MachineRecipeKind, ItemLike> machines() {
        return Map.ofEntries(Map.entry(MachineRecipeKind.BIO_REACTING, JSBlocks.BIO_REACTOR.get()),

                Map.entry(MachineRecipeKind.CRUSHING, JSBlocks.CRUSHER.get()),
                Map.entry(MachineRecipeKind.ALLOY_SMELTING, JSBlocks.ALLOY_SMELTER.get()),
                Map.entry(MachineRecipeKind.COMPRESSING, JSBlocks.COMPRESSOR.get()),
                Map.entry(MachineRecipeKind.WIRE_DRAWING, JSBlocks.WIRE_DRAWER.get()),
                Map.entry(MachineRecipeKind.SAWING, JSBlocks.SAWMILL.get()),
                Map.entry(MachineRecipeKind.WASHING, JSBlocks.ORE_WASHER.get()),
                Map.entry(MachineRecipeKind.MIXING, JSBlocks.FLUID_MIXER.get()),
                Map.entry(MachineRecipeKind.CENTRIFUGING, JSBlocks.CENTRIFUGE.get()),
                Map.entry(MachineRecipeKind.CIRCUIT_ASSEMBLING, JSBlocks.CIRCUIT_ASSEMBLER.get()),
                Map.entry(MachineRecipeKind.DISSOLVING, JSBlocks.CHEMICAL_DISSOLVER.get()),
                Map.entry(MachineRecipeKind.CRYSTALLIZING, JSBlocks.CRYSTALLIZER.get()),
                Map.entry(MachineRecipeKind.FERTILIZER_MIXING, JSBlocks.FERTILIZER_MIXER.get()),
                Map.entry(MachineRecipeKind.ENGRAVING, JSBlocks.LASER_ENGRAVER.get()),
                Map.entry(MachineRecipeKind.REFINING, JSBlocks.PLASMA_REFINERY.get()));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        machines().forEach((kind, block) -> registration.addRecipeCategories(new MachineRecipeCategory(gui, kind, block)));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            var recipes = Minecraft.getInstance().level.getRecipeManager();
            for (MachineRecipeKind kind : machines().keySet()) {
                registration.addRecipes(MachineRecipeCategory.typeOf(kind), recipes.getAllRecipesFor(kind.type()));
            }
        }
        // Where each ore generates, shown on the ore blocks and the raw ore.
        for (Metal metal : Metal.values()) {
            if (!metal.hasOre()) continue;
            Component info = Component.translatable("jei." + Joulesmith.MODID + ".info.ore." + metal.id());
            registration.addIngredientInfo(JSBlocks.ORE.get(metal).get(), info);
            registration.addIngredientInfo(JSBlocks.DEEPSLATE_ORE.get(metal).get(), info);
            registration.addIngredientInfo(JSItems.RAW.get(metal).get(), info);
        }
        // What each block and part does, for the ones with an info text in the language file.
        var language = net.minecraft.locale.Language.getInstance();
        for (var holder : JSItems.ITEMS.getEntries()) {
            var id = holder.getId();
            String key = "jei." + Joulesmith.MODID + ".info." + id.getPath();
            if (language.has(key)) registration.addIngredientInfo(holder.get(), Component.translatable(key));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(JSBlocks.ELECTRIC_FURNACE.get()), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(JSBlocks.COMBUSTION_GENERATOR.get()), RecipeTypes.FUELING);
        machines().forEach((kind, block) -> registration.addRecipeCatalyst(new ItemStack(block), MachineRecipeCategory.typeOf(kind)));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(MachineScreen.class, new IGuiContainerHandler<MachineScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(MachineScreen screen) {
                return screen.extraAreas();
            }

            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(MachineScreen screen, double mouseX, double mouseY) {
                RecipeType<?> type = typeFor(screen.getMenu().recipeCategory());
                List<IGuiClickableArea> areas = new ArrayList<>();
                if (type != null) areas.add(IGuiClickableArea.createBasic(MachineScreen.arrowX(), MachineScreen.arrowY(), 24, 17, type));
                return areas;
            }
        });
    }

    private static RecipeType<?> typeFor(ResourceLocation category) {
        if (category == null) return null;
        if (category.equals(RecipeTypes.SMELTING.getUid())) return RecipeTypes.SMELTING;
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            if (category.equals(Joulesmith.id(kind.id()))) return MachineRecipeCategory.typeOf(kind);
        }
        return null;
    }
}
