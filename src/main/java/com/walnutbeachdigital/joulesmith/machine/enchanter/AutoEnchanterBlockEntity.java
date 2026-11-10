package com.walnutbeachdigital.joulesmith.machine.enchanter;

import java.util.List;
import java.util.Optional;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.recipe.ChanceResult;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Enchants books and unenchanted tools, weapons and armor as a level 30 enchanting table would, using
 * {@link #XP_COST} mB of Liquid XP (and power) for each one. Feed it Liquid XP from a Mob Grinder by pipe.
 */
public class AutoEnchanterBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000, MAX_INPUT = 400, TIME = 200, ENERGY_PER_TICK = 50, XP_COST = 1_000, TANK = 16_000, LEVEL = 30;

    private final FluidTank tank = new FluidTank(TANK, stack -> stack.is(JSFluids.LIQUID_XP.source.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    /** The roll for the item now being enchanted, kept so the result doesn't change every tick. */
    private ItemStack rolled = ItemStack.EMPTY;
    private ItemStack rolledFor = ItemStack.EMPTY;

    public AutoEnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.AUTO_ENCHANTER.get(), pos, state, List.of(SlotDef.input(56, 35), SlotDef.output(116, 35)),
                CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(tank, 30));
    }

    @Override
    protected FluidTank inputTank() {
        return tank;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.is(Items.BOOK) || (stack.isEnchantable() && !stack.isEnchanted());
    }

    @Override
    protected Process findProcess() {
        if (level == null || tank.getFluidAmount() < XP_COST) return null;
        ItemStack input = items.getStackInSlot(0);
        if (input.isEmpty() || !isValidInput(input)) return null;
        if (rolled.isEmpty() || !ItemStack.isSameItemSameComponents(rolledFor, input)) {
            rolledFor = input.copyWithCount(1);
            rolled = EnchantmentHelper.enchantItem(level.random, input.copyWithCount(1), LEVEL, level.registryAccess(), Optional.empty());
        }
        if (!rolled.isEnchanted() && !rolled.is(Items.ENCHANTED_BOOK)) return null;
        int[] consume = new int[slotDefs().size()];
        consume[0] = 1;
        return new Process(List.of(ChanceResult.always(rolled.copy())), TIME, ENERGY_PER_TICK, consume,
                new FluidStack(JSFluids.LIQUID_XP.source.get(), XP_COST), FluidStack.EMPTY);
    }

    @Override
    protected void finish(Process process) {
        super.finish(process);
        rolled = ItemStack.EMPTY;                          // the next item gets a fresh roll
    }
}
