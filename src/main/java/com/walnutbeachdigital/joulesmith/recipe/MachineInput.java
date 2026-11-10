package com.walnutbeachdigital.joulesmith.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a machine offers a recipe: the contents of its input slots and, for fluid machines, its input tank. */
public record MachineInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput {
    public MachineInput(List<ItemStack> items) {
        this(items, FluidStack.EMPTY);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return fluid.isEmpty() && items.stream().allMatch(ItemStack::isEmpty);
    }
}
