package com.walnutbeachdigital.joulesmith.machine.furnace;

import java.util.List;
import java.util.Optional;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

/** Smelts anything a furnace can, twice as fast, on power instead of fuel. */
public class ElectricFurnaceBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200, ENERGY_PER_TICK = 20;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.ELECTRIC_FURNACE.get(), pos, state,
                List.of(SlotDef.input(56, 35), SlotDef.output(116, 35)), CAPACITY, MAX_INPUT, 0, true);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
    }

    @Override
    protected Process findProcess() {
        ItemStack input = firstInput();
        return recipe(input).map(holder -> Process.simple(
                holder.value().assemble(new SingleRecipeInput(input), level.registryAccess()),
                Math.max(1, holder.value().getCookingTime() / 2), ENERGY_PER_TICK, slotDefs().size(), 0, 1)).orElse(null);
    }

    @Override
    public ResourceLocation recipeCategory() {
        return ResourceLocation.withDefaultNamespace("smelting");
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return level == null || recipe(stack).isPresent();
    }
}
