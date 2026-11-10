package com.walnutbeachdigital.joulesmith.machine.crusher;

import java.util.List;
import java.util.Optional;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.recipe.CrushingRecipe;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/** Grinds ores into dust (two dusts per ore), ingots back into dust, cobblestone into gravel and gravel into sand. */
public class CrusherBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200;

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CRUSHER.get(), pos, state,
                List.of(SlotDef.input(56, 35), SlotDef.output(116, 35)), CAPACITY, MAX_INPUT);
    }

    private Optional<RecipeHolder<CrushingRecipe>> recipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(JSRecipes.CRUSHING.get(), new SingleRecipeInput(stack), level);
    }

    @Override
    protected Process findProcess() {
        return recipe(firstInput()).map(holder -> {
            CrushingRecipe r = holder.value();
            int time = Math.max(1, r.time());
            return new Process(r.result().copy(), time, Math.max(1, r.energy() / time), 1);
        }).orElse(null);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return level == null || recipe(stack).isPresent();
    }
}
