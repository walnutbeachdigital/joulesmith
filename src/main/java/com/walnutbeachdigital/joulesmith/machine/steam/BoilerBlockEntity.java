package com.walnutbeachdigital.joulesmith.machine.steam;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Boils water into steam. Burning furnace fuel it makes 200 mB of steam a tick (fuel lasts a quarter of its
 * furnace time); sitting on lava, magma, fire or a lit campfire it makes 100 mB a tick with no fuel at all.
 * Pushes steam out of every face set to give out, straight into a Steam Turbine or a fluid pipe.
 */
public class BoilerBlockEntity extends MachineBlockEntity {
    public static final int TANK = 16_000, FUEL_RATE = 200, HEAT_RATE = 100, PUSH = 1_000, FUEL_DIVISOR = 4;

    private final FluidTank water = new FluidTank(TANK, stack -> stack.is(FluidTags.WATER)) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final FluidTank steam = new FluidTank(TANK, stack -> stack.is(JSFluids.STEAM.source.get())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BOILER.get(), pos, state, List.of(SlotDef.input(80, 53)), 0, 0, 0);
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(water, 30), new TankDef(steam, 130));
    }

    @Override
    protected FluidTank inputTank() {
        return water;
    }

    @Override
    protected FluidTank outputTank() {
        return steam;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.FLAME;
    }

    private static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return burnTime(stack) > 0;
    }

    /** Lava, magma, fire or a lit campfire right under the boiler. */
    private boolean heatBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getFluidState().is(FluidTags.LAVA) || below.is(Blocks.MAGMA_BLOCK) || below.is(BlockTags.FIRE)
                || (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT));
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        boolean room = water.getFluidAmount() > 0 && steam.getSpace() > 0;
        if (progress <= 0 && allowed && room) {
            ItemStack fuel = items.getStackInSlot(0);
            int time = burnTime(fuel) / FUEL_DIVISOR;
            if (time > 0) {
                progress = maxProgress = time;
                ItemStack remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) items.setStackInSlot(0, remainder);
                setChanged();
            }
        }
        boolean burning = progress > 0;
        if (burning) progress--;
        int rate = !allowed ? 0 : burning ? FUEL_RATE : heatBelow(level, pos) ? HEAT_RATE : 0;
        int boiled = Math.min(rate, Math.min(water.getFluidAmount(), steam.getSpace()));
        if (boiled > 0) {
            water.drain(boiled, IFluidHandler.FluidAction.EXECUTE);
            steam.fill(new FluidStack(JSFluids.STEAM.source.get(), boiled), IFluidHandler.FluidAction.EXECUTE);
        }
        if (allowed) pushSteam(level, pos);
        if (burning || boiled > 0) setChanged();
        return burning || boiled > 0;
    }

    private void pushSteam(Level level, BlockPos pos) {
        if (steam.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (!sideMode(MachineFace.of(facing(), dir)).gives()) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null) FluidUtil.tryFluidTransfer(target, steam, PUSH, true);
            if (steam.isEmpty()) return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTotal", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        maxProgress = tag.getInt("BurnTotal");
    }
}
