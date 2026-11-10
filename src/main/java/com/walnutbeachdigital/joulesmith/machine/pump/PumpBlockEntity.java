package com.walnutbeachdigital.joulesmith.machine.pump;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.machine.SideMode;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Drains the pool of fluid under it, one bucket a second for 100 FE, starting from the far edge so the
 * pool shrinks inwards. Pushes the fluid out of every face set to give out (all but the front at first).
 */
public class PumpBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200, ENERGY_PER_BUCKET = 100, INTERVAL = 20;
    public static final int TANK = 8_000, PUSH_RATE = 1_000, RADIUS = 32, DEPTH = 64, MAX_SCAN = 8_192;

    private final FluidTank tank = new FluidTank(TANK) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final Deque<BlockPos> sources = new ArrayDeque<>();
    private Fluid sourceFluid;
    private int cooldown;
    private int active;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.PUMP.get(), pos, state, List.of(), CAPACITY, MAX_INPUT);
    }

    @Override
    public FluidTank fluidTank() {
        return tank;
    }

    @Override
    public int tankX() {
        return 80;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (allowed && ++cooldown >= INTERVAL) {
            cooldown = 0;
            if (energy.getEnergyStored() >= ENERGY_PER_BUCKET && tank.getSpace() >= 1000 && pumpOne(level, pos)) {
                energy.consume(ENERGY_PER_BUCKET);
                active = INTERVAL * 2;
            }
        }
        if (allowed) pushFluid(level, pos);
        if (active > 0) active--;
        return active > 0;
    }

    /** Takes one source block of fluid from the pool below. */
    private boolean pumpOne(Level level, BlockPos pos) {
        for (int tries = 0; tries < 2; tries++) {
            if (sources.isEmpty()) scan(level, pos);
            while (!sources.isEmpty()) {
                BlockPos p = sources.pollFirst();
                FluidState fluid = level.getFluidState(p);
                if (!fluid.isSource() || !fluid.getType().isSame(sourceFluid)) continue;
                BlockState state = level.getBlockState(p);
                if (!(state.getBlock() instanceof BucketPickup pickup)) continue;
                FluidStack stack = new FluidStack(fluid.getType(), 1000);
                if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) return false;
                ItemStack bucket = pickup.pickupBlock(null, level, p, state);
                if (bucket.isEmpty()) continue;
                tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }

    /** Finds every source block in the pool below, furthest first. */
    private void scan(Level level, BlockPos pos) {
        sources.clear();
        BlockPos start = pos.below();
        FluidState first = level.getFluidState(start);
        if (first.isEmpty()) return;
        sourceFluid = first.getType();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < MAX_SCAN) {
            BlockPos p = queue.poll();
            FluidState f = level.getFluidState(p);
            if (f.isSource()) found.add(p);
            for (Direction dir : Direction.values()) {
                BlockPos next = p.relative(dir);
                if (next.getY() >= pos.getY() || next.getY() < pos.getY() - DEPTH) continue;
                if (Math.abs(next.getX() - pos.getX()) > RADIUS || Math.abs(next.getZ() - pos.getZ()) > RADIUS) continue;
                if (!level.isLoaded(next) || !seen.add(next)) continue;
                if (level.getFluidState(next).getType().isSame(sourceFluid)) queue.add(next);
            }
        }
        found.sort(Comparator.comparingDouble((BlockPos p) -> p.distSqr(pos)).reversed());
        sources.addAll(found);
    }

    private void pushFluid(Level level, BlockPos pos) {
        if (tank.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (!sideMode(MachineFace.of(facing(), dir)).gives()) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null) FluidUtil.tryFluidTransfer(target, tank, PUSH_RATE, true);
            if (tank.isEmpty()) return;
        }
    }

    /** What pipes see: fluid can be taken out of faces set to give out; nothing can be put in. */
    @Override
    public IFluidHandler fluidHandler(Direction side) {
        if (side == null) return tank;
        SideMode mode = sideMode(MachineFace.of(facing(), side));
        if (!mode.gives()) return null;
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int i) { return tank.getFluid(); }
            @Override public int getTankCapacity(int i) { return tank.getCapacity(); }
            @Override public boolean isFluidValid(int i, FluidStack stack) { return false; }
            @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) { return tank.drain(resource, action); }
            @Override public FluidStack drain(int maxDrain, FluidAction action) { return tank.drain(maxDrain, action); }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
