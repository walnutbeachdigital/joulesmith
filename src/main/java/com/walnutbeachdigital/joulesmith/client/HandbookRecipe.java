package com.walnutbeachdigital.joulesmith.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipe;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;
import com.walnutbeachdigital.joulesmith.recipe.MachineStations;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A recipe drawn in the Handbook's tooltips: a 3 x 3 grid for crafting, or the inputs, an arrow and the result
 * for smelting and Joulesmith's machines, with the machine under the arrow. Ingredients that accept several
 * items cycle through them once a second; an item with several recipes cycles through those every few seconds.
 */
public final class HandbookRecipe {
    private static final int SLOT = 18;
    private static final Map<Item, List<HandbookRecipe>> CACHE = new HashMap<>();
    private static RecipeManager cachedFor;

    private final boolean grid;
    private final Ingredient[] cells;
    private final int[] counts;
    private final ItemStack result;
    private final ItemStack station;

    private HandbookRecipe(boolean grid, Ingredient[] cells, int[] counts, ItemStack result, ItemStack station) {
        this.grid = grid;
        this.cells = cells;
        this.counts = counts;
        this.result = result;
        this.station = station;
    }

    /** Every recipe that makes the item (Joulesmith's own first); empty if none does. */
    public static List<HandbookRecipe> of(Item item) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        RecipeManager manager = level.getRecipeManager();
        if (manager != cachedFor) {
            CACHE.clear();
            cachedFor = manager;
        }
        return CACHE.computeIfAbsent(item, it -> find(level, manager, it));
    }

    private static List<HandbookRecipe> find(Level level, RecipeManager manager, Item item) {
        List<HandbookRecipe> ours = new ArrayList<>(), others = new ArrayList<>();
        List<RecipeHolder<?>> holders = new ArrayList<>();
        holders.addAll(manager.getAllRecipesFor(RecipeType.CRAFTING));
        holders.addAll(manager.getAllRecipesFor(RecipeType.SMELTING));
        holders.addAll(manager.getAllRecipesFor(RecipeType.BLASTING));
        for (RecipeHolder<?> holder : holders) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial() || recipe.getIngredients().isEmpty()) continue;
            ItemStack out = recipe.getResultItem(level.registryAccess());
            if (out.isEmpty() || out.getItem() != item) continue;
            (Joulesmith.MODID.equals(holder.id().getNamespace()) ? ours : others).add(build(recipe, out.copy()));
        }
        // Joulesmith's machines
        for (var entry : MachineStations.all().entrySet()) {
            MachineRecipeKind kind = entry.getKey();
            for (RecipeHolder<MachineRecipe> holder : manager.getAllRecipesFor(kind.type())) {
                MachineRecipe r = holder.value();
                if (r.results().isEmpty() || r.results().get(0).stack().getItem() != item || r.inputs().isEmpty()) continue;
                int n = Math.min(3, r.inputs().size());
                Ingredient[] cells = new Ingredient[n];
                int[] counts = new int[n];
                for (int i = 0; i < n; i++) {
                    cells[i] = r.inputs().get(i).ingredient();
                    counts[i] = r.inputs().get(i).count();
                }
                ours.add(new HandbookRecipe(false, cells, counts, r.results().get(0).stack().copy(), new ItemStack(entry.getValue())));
            }
        }
        ours.addAll(others);
        return List.copyOf(ours);
    }

    private static HandbookRecipe build(Recipe<?> recipe, ItemStack result) {
        ItemStack station = recipe.getToastSymbol();
        List<Ingredient> ingredients = recipe.getIngredients();
        if (recipe instanceof AbstractCookingRecipe) {
            return new HandbookRecipe(false, new Ingredient[] {ingredients.get(0)}, new int[] {1}, result, station);
        }
        Ingredient[] cells = new Ingredient[9];
        java.util.Arrays.fill(cells, Ingredient.EMPTY);
        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            for (int i = 0; i < ingredients.size() && i < 9; i++) cells[(i / w) * 3 + i % w] = ingredients.get(i);
        } else {
            for (int i = 0; i < ingredients.size() && i < 9; i++) cells[i] = ingredients.get(i);
        }
        return new HandbookRecipe(true, cells, null, result, station);
    }

    public ItemStack station() {
        return station;
    }

    public int width() {
        return (grid ? 3 : cells.length) * SLOT + 26 + SLOT;
    }

    public int height() {
        return grid ? 3 * SLOT : SLOT + 18;
    }

    /** Draws the recipe with its top-left corner at (x, y). */
    public void render(GuiGraphics g, Font font, int x, int y) {
        int rows = grid ? 3 : 1, cols = grid ? 3 : cells.length;
        long second = Util.getMillis() / 1000;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = x + c * SLOT, sy = y + r * SLOT, i = r * cols + c;
                slot(g, sx, sy);
                ItemStack[] options = cells[i].getItems();
                if (options.length > 0) {
                    ItemStack shown = options[(int) (second % options.length)].copyWithCount(counts == null ? 1 : counts[i]);
                    g.renderItem(shown, sx + 1, sy + 1);
                    g.renderItemDecorations(font, shown, sx + 1, sy + 1);
                }
            }
        }
        int arrowX = x + cols * SLOT + 5;
        int midY = grid ? y + SLOT : y;
        arrow(g, arrowX, midY + 1);
        if (!station.isEmpty()) g.renderItem(station, arrowX, midY + SLOT + 1);
        int rx = arrowX + 21;
        slot(g, rx, midY);
        g.renderItem(result, rx + 1, midY + 1);
        g.renderItemDecorations(font, result, rx + 1, midY + 1);
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
        g.fill(x, y, x + SLOT - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + SLOT - 1, 0xFF373737);
        g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
    }

    private static void arrow(GuiGraphics g, int x, int y) {
        g.fill(x, y + 6, x + 10, y + 10, 0xFFC6C6C6);
        for (int i = 0; i < 6; i++) g.fill(x + 10 + i, y + 2 + i, x + 11 + i, y + 14 - i, 0xFFC6C6C6);
    }
}
