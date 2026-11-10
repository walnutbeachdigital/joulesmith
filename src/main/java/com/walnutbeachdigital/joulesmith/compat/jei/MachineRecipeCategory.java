package com.walnutbeachdigital.joulesmith.compat.jei;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.recipe.ChanceResult;
import com.walnutbeachdigital.joulesmith.recipe.CountedIngredient;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipe;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

/**
 * One JEI page layout for every Joulesmith recipe machine: up to two inputs, an arrow, a main output and
 * byproducts with their chance, plus the energy and time each run takes.
 */
public class MachineRecipeCategory implements IRecipeCategory<RecipeHolder<MachineRecipe>> {
    private static final int OUT_X = 82, BYPRODUCT_X = 110;

    private final MachineRecipeKind kind;
    private final RecipeType<RecipeHolder<MachineRecipe>> type;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public MachineRecipeCategory(IGuiHelper gui, MachineRecipeKind kind, ItemLike machine) {
        this.kind = kind;
        this.type = typeOf(kind);
        this.icon = gui.createDrawableItemStack(new ItemStack(machine));
        this.arrow = gui.createAnimatedRecipeArrow(Math.max(20, kind.defaultTime()));
    }

    public static RecipeType<RecipeHolder<MachineRecipe>> typeOf(MachineRecipeKind kind) {
        return RecipeType.createRecipeHolderType(Joulesmith.id(kind.id()));
    }

    @Override public RecipeType<RecipeHolder<MachineRecipe>> getRecipeType() { return type; }
    @Override public Component getTitle() { return Component.translatable(kind.titleKey()); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 140; }
    @Override public int getHeight() { return 44; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
        MachineRecipe recipe = holder.value();
        int x = 2;
        for (CountedIngredient input : recipe.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, x, 6).setStandardSlotBackground().addItemStacks(input.displayStacks());
            x += 19;
        }
        int bx = BYPRODUCT_X;
        boolean first = true;
        for (ChanceResult result : recipe.results()) {
            IRecipeSlotBuilder slot;
            if (first) {
                slot = builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 5, 6).setOutputSlotBackground();
            } else {
                slot = builder.addSlot(RecipeIngredientRole.OUTPUT, bx, 6).setStandardSlotBackground();
                bx += 19;
            }
            slot.addItemStack(result.stack());
            if (!result.certain()) {
                int percent = Math.round(result.chance() * 100);
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.joulesmith.chance", percent).withStyle(ChatFormatting.GOLD)));
            }
            first = false;
        }
    }

    @Override
    public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        MachineRecipe recipe = holder.value();
        int inputsEnd = 2 + recipe.inputs().size() * 19;
        arrow.draw(g, Math.max(inputsEnd + 4, 50), 6);
        var font = Minecraft.getInstance().font;
        int bx = BYPRODUCT_X;
        for (int i = 1; i < recipe.results().size(); i++) {
            ChanceResult r = recipe.results().get(i);
            if (!r.certain()) g.drawString(font, Math.round(r.chance() * 100) + "%", bx + 1, 26, 0xFF8A6A20, false);
            bx += 19;
        }
        g.drawString(font, Component.translatable("jei.joulesmith.energy", String.format("%,d", recipe.energy())), 2, 34, 0xFF606060, false);
        Component time = Component.translatable("jei.joulesmith.time", String.format("%.1f", recipe.time() / 20.0));
        g.drawString(font, time, 140 - font.width(time) - 2, 34, 0xFF606060, false);
    }
}
