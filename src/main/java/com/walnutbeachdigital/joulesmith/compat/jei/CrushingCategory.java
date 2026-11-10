package com.walnutbeachdigital.joulesmith.compat.jei;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.recipe.CrushingRecipe;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI page for the Crusher: input, arrow, output, and the energy each run takes. */
public class CrushingCategory implements IRecipeCategory<RecipeHolder<CrushingRecipe>> {
    public static final RecipeType<RecipeHolder<CrushingRecipe>> TYPE =
            RecipeType.createRecipeHolderType(Joulesmith.id("crushing"));

    private final IDrawable icon;
    private final IDrawableStatic slot;
    private final IDrawableStatic outputSlot;
    private final IDrawableAnimated arrow;

    public CrushingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(JSBlocks.CRUSHER.get()));
        this.slot = gui.getSlotDrawable();
        this.outputSlot = gui.getOutputSlot();
        this.arrow = gui.createAnimatedRecipeArrow(100);
    }

    @Override public RecipeType<RecipeHolder<CrushingRecipe>> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.joulesmith.crushing"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 116; }
    @Override public int getHeight() { return 40; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrushingRecipe> recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addIngredients(recipe.value().ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 69, 5).addItemStack(recipe.value().result());
    }

    @Override
    public void draw(RecipeHolder<CrushingRecipe> recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        slot.draw(g, 4, 4);
        outputSlot.draw(g, 64, 0);
        arrow.draw(g, 32, 4);
        g.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.joulesmith.energy", String.format("%,d", recipe.value().energy())), 4, 30, 0xFF606060, false);
    }
}
