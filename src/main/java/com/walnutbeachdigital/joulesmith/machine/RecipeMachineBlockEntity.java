package com.walnutbeachdigital.joulesmith.machine;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.recipe.CountedIngredient;
import com.walnutbeachdigital.joulesmith.recipe.MachineInput;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipe;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine that runs one kind of {@link MachineRecipe}: it looks up what its input slots make, and the
 * shared base does the rest. Takes upgrade cards. Each machine is then just its slots and its recipe kind.
 */
public abstract class RecipeMachineBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200;

    private final MachineRecipeKind kind;
    private RecipeHolder<MachineRecipe> last;          // checked first, so the usual case skips the search

    protected RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots, MachineRecipeKind kind) {
        super(type, pos, state, slots, CAPACITY, MAX_INPUT, 0, true);
        this.kind = kind;
    }

    public MachineRecipeKind kind() {
        return kind;
    }

    @Override
    public ResourceLocation recipeCategory() {
        return Joulesmith.id(kind.id());
    }

    private List<Integer> inputSlots() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < slotDefs().size(); i++) {
            if (slotDefs().get(i).role() == SlotRole.INPUT) list.add(i);
        }
        return list;
    }

    @Override
    protected Process findProcess() {
        if (level == null) return null;
        List<Integer> inputSlots = inputSlots();
        List<ItemStack> stacks = inputSlots.stream().map(items::getStackInSlot).toList();
        MachineInput input = new MachineInput(stacks);
        if (input.isEmpty()) return null;
        RecipeHolder<MachineRecipe> holder = last != null && last.value().matches(input, level) ? last
                : level.getRecipeManager().getRecipeFor(kind.type(), input, level).orElse(null);
        if (holder == null) return null;
        last = holder;
        MachineRecipe recipe = holder.value();
        int[] chosen = recipe.assign(stacks);
        if (chosen == null) return null;
        int[] consume = new int[slotDefs().size()];
        for (int i = 0; i < chosen.length; i++) {
            CountedIngredient in = recipe.inputs().get(i);
            consume[inputSlots.get(chosen[i])] += in.count();
        }
        int time = Math.max(1, recipe.time());
        return new Process(recipe.results(), time, Math.max(1, recipe.energy() / time), consume);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (level == null) return true;
        for (RecipeHolder<MachineRecipe> holder : level.getRecipeManager().getAllRecipesFor(kind.type())) {
            if (holder.value().accepts(stack)) return true;
        }
        return false;
    }
}
