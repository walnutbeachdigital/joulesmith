package com.walnutbeachdigital.joulesmith.network;

import com.walnutbeachdigital.joulesmith.item.filter.FilterContents;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Twice a second, moves up to {@link #PER_OPERATION} items: an Import Bus pulls from the inventory it faces
 * into the network (only what its filter lets through, if it has one); an Export Bus sends what its filter
 * names from the network into that inventory (with no filter it sends nothing).
 */
public class BusBlockEntity extends BlockEntity {
    public static final int INTERVAL = 10, PER_OPERATION = 16, FLUID_PER_OPERATION = 1_000;
    private ItemStack filter = ItemStack.EMPTY;

    public BusBlockEntity(BlockPos pos, BlockState state) {
        super(com.walnutbeachdigital.joulesmith.registry.JSBlockEntities.BUS.get(), pos, state);
    }

    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack;
        setChanged();
    }

    private FilterContents contents() {
        return filter.getOrDefault(JSDataComponents.FILTER.get(), FilterContents.EMPTY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BusBlockEntity bus) {
        if (level.getGameTime() % INTERVAL != Math.floorMod(pos.hashCode(), INTERVAL)) return;
        Direction facing = state.getValue(BusBlock.FACING);
        if (state.getBlock() instanceof BusBlock b && b.fluid()) {
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(facing), facing.getOpposite());
            if (tank == null) return;
            Network net = Network.scan(level, pos);
            if (!net.online()) return;
            if (b.export()) bus.exportFluid(net, tank);
            else bus.importFluid(net, tank);
            return;
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(facing), facing.getOpposite());
        if (target == null) return;
        Network net = Network.scan(level, pos);
        if (!net.online()) return;
        boolean export = state.getBlock() instanceof BusBlock b && b.export();
        if (export) bus.export(net, target);
        else bus.importFrom(net, target);
    }

    private void importFrom(Network net, IItemHandler target) {
        FilterContents f = contents();
        int left = PER_OPERATION;
        for (int slot = 0; slot < target.getSlots() && left > 0; slot++) {
            ItemStack peek = target.extractItem(slot, left, true);
            if (peek.isEmpty() || (!filter.isEmpty() && !f.test(peek))) continue;
            ItemStack rest = net.insert(peek, true);
            int can = peek.getCount() - rest.getCount();
            if (can <= 0) continue;
            ItemStack taken = target.extractItem(slot, can, false);
            ItemStack back = net.insert(taken, false);
            if (!back.isEmpty()) ItemHandlerHelper.insertItem(target, back, false);
            left -= taken.getCount() - back.getCount();
        }
    }

    private void export(Network net, IItemHandler target) {
        if (filter.isEmpty()) return;
        FilterContents f = contents();
        int left = PER_OPERATION;
        for (BinContents c : net.contents()) {
            if (left <= 0) break;
            if (!f.test(c.item())) continue;
            int amount = Math.min(left, Math.min(c.count(), c.item().getMaxStackSize()));
            ItemStack rest = ItemHandlerHelper.insertItem(target, c.item().copyWithCount(amount), true);
            int can = amount - rest.getCount();
            if (can <= 0) continue;
            ItemStack got = net.extract(c.item(), can, false);
            ItemStack back = ItemHandlerHelper.insertItem(target, got, false);
            if (!back.isEmpty()) net.insert(back, false);
            left -= got.getCount() - back.getCount();
        }
    }

    /** The fluids the filter names: whatever its items (buckets, tanks) hold. */
    private java.util.List<FluidStack> filterFluids() {
        java.util.List<FluidStack> out = new java.util.ArrayList<>();
        for (ItemStack s : contents().items()) FluidUtil.getFluidContained(s).ifPresent(out::add);
        return out;
    }

    private boolean fluidAllowed(FluidStack fluid) {
        if (filter.isEmpty()) return true;
        boolean listed = filterFluids().stream().anyMatch(f -> FluidStack.isSameFluidSameComponents(f, fluid));
        return listed != contents().blacklist();
    }

    private void importFluid(Network net, IFluidHandler tank) {
        FluidStack peek = tank.drain(FLUID_PER_OPERATION, IFluidHandler.FluidAction.SIMULATE);
        if (peek.isEmpty() || !fluidAllowed(peek)) return;
        int can = net.insertFluid(peek, true);
        if (can <= 0) return;
        FluidStack got = tank.drain(peek.copyWithAmount(can), IFluidHandler.FluidAction.EXECUTE);
        net.insertFluid(got, false);
    }

    private void exportFluid(Network net, IFluidHandler tank) {
        if (filter.isEmpty()) return;
        for (FluidStack stored : net.fluids()) {
            if (!fluidAllowed(stored)) continue;
            int can = tank.fill(stored.copyWithAmount(Math.min(FLUID_PER_OPERATION, stored.getAmount())), IFluidHandler.FluidAction.SIMULATE);
            if (can <= 0) continue;
            FluidStack got = net.extractFluid(stored, can, false);
            int filled = tank.fill(got, IFluidHandler.FluidAction.EXECUTE);
            if (filled < got.getAmount()) net.insertFluid(got.copyWithAmount(got.getAmount() - filled), false);
            return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filter.isEmpty()) tag.put("Filter", filter.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        filter = tag.contains("Filter") ? ItemStack.parse(registries, tag.getCompound("Filter")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
