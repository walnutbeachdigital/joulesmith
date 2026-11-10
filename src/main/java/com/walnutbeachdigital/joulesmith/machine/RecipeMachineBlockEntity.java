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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A machine that runs one kind of {@link MachineRecipe}: it looks up what its input slots make, and the
 * shared base does the rest. Takes upgrade cards. Each machine is then just its slots and its recipe kind.
 */
public abstract class RecipeMachineBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200;

    public static final int TANK = 8_000;

    private final MachineRecipeKind kind;
    private final FluidTank inputTank;
    private final FluidTank outputTank;
    private final List<TankDef> tankDefs = new ArrayList<>();
    private RecipeHolder<MachineRecipe> last;          // checked first, so the usual case skips the search

    protected RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots, MachineRecipeKind kind) {
        this(type, pos, state, slots, kind, -1, -1);
    }

    /** {@code inputTankX} / {@code outputTankX}: where each tank's gauge goes, or -1 for no such tank. */
    protected RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots,
                                       MachineRecipeKind kind, int inputTankX, int outputTankX) {
        super(type, pos, state, slots, CAPACITY, MAX_INPUT, 0, true);
        this.kind = kind;
        this.inputTank = inputTankX < 0 ? null : new FluidTank(TANK, this::usesFluid) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
        this.outputTank = outputTankX < 0 ? null : new FluidTank(TANK) {
            @Override protected void onContentsChanged() { setChanged(); }
        };
        if (inputTank != null) tankDefs.add(new TankDef(inputTank, inputTankX));
        if (outputTank != null) tankDefs.add(new TankDef(outputTank, outputTankX));
    }

    @Override
    public List<TankDef> tanks() {
        return tankDefs;
    }

    @Override
    protected FluidTank inputTank() {
        return inputTank;
    }

    @Override
    protected FluidTank outputTank() {
        return outputTank;
    }

    /** Whether any of this machine's recipes takes {@code fluid} (so pipes only fill it with fluids it can use). */
    private boolean usesFluid(FluidStack fluid) {
        if (level == null) return true;
        for (RecipeHolder<MachineRecipe> holder : level.getRecipeManager().getAllRecipesFor(kind.type())) {
            if (FluidStack.isSameFluidSameComponents(holder.value().fluid(), fluid)) return true;
        }
        return false;
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
        MachineInput input = new MachineInput(stacks, inputTank == null ? FluidStack.EMPTY : inputTank.getFluid());
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
        return new Process(recipe.results(), time, Math.max(1, recipe.energy() / time), consume, recipe.fluid(), recipe.fluidResult());
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
