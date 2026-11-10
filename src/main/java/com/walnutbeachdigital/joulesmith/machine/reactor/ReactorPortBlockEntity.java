package com.walnutbeachdigital.joulesmith.machine.reactor;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A Reactor Port in the reactor's shell: pipes put water in, ducts and hoppers put uranium in, and cables
 * take power out, all straight to and from the controller. Unlinked (no complete reactor), it does nothing.
 */
public class ReactorPortBlockEntity extends BlockEntity {
    private BlockPos controller;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.REACTOR_PORT.get(), pos, state);
    }

    void setController(BlockPos pos) {
        if (pos == null ? controller == null : pos.equals(controller)) return;
        controller = pos;
        setChanged();
    }

    /** The reactor this port belongs to, if it's complete. */
    public FissionReactorBlockEntity reactor() {
        if (controller == null || level == null || !level.isLoaded(controller)) return null;
        return level.getBlockEntity(controller) instanceof FissionReactorBlockEntity r && r.hasPort(worldPosition) ? r : null;
    }

    public final IEnergyStorage energy = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean simulate) { return 0; }
        @Override public int extractEnergy(int max, boolean simulate) { var r = reactor(); return r == null ? 0 : r.energy().extractEnergy(max, simulate); }
        @Override public int getEnergyStored() { var r = reactor(); return r == null ? 0 : r.energy().getEnergyStored(); }
        @Override public int getMaxEnergyStored() { var r = reactor(); return r == null ? 0 : r.energy().getMaxEnergyStored(); }
        @Override public boolean canExtract() { return reactor() != null; }
        @Override public boolean canReceive() { return false; }
    };

    public final IFluidHandler fluids = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { var r = reactor(); return r == null ? FluidStack.EMPTY : r.water().getFluid(); }
        @Override public int getTankCapacity(int tank) { var r = reactor(); return r == null ? 0 : r.water().getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { var r = reactor(); return r != null && r.water().isFluidValid(stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { var r = reactor(); return r == null ? 0 : r.water().fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    public final IItemHandler items = new IItemHandler() {
        private IItemHandler inner() { var r = reactor(); return r == null ? null : r.itemHandler(null); }
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { var h = inner(); return h == null ? ItemStack.EMPTY : h.getStackInSlot(0); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var r = reactor();
            if (r == null || !stack.is(FissionReactorBlockEntity.FUEL)) return stack;
            return r.itemHandler(null).insertItem(0, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.is(FissionReactorBlockEntity.FUEL); }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) tag.put("Controller", NbtUtils.writeBlockPos(controller));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
    }
}
