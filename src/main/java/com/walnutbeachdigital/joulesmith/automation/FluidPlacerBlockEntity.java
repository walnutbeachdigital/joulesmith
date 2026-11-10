package com.walnutbeachdigital.joulesmith.automation;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The Fluid Placer puts a source block of the fluid in its tank into the space in front of it; the Fluid
 * Drain does the opposite, taking a source block from in front of it into its tank and pushing it out of
 * faces set to give out. One bucket a second, 50 FE each.
 */
public class FluidPlacerBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 10_000, MAX_INPUT = 200, ENERGY = 50, INTERVAL = 20, TANK = 8_000;

    private final boolean drain;
    private final FluidTank tank = new FluidTank(TANK) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private int cooldown;

    public FluidPlacerBlockEntity(BlockPos pos, BlockState state, boolean drain) {
        super(drain ? JSBlockEntities.FLUID_DRAIN.get() : JSBlockEntities.FLUID_PLACER.get(), pos, state, List.of(),
                CAPACITY, MAX_INPUT, 0, true);
        this.drain = drain;
    }

    @Override
    public List<TankDef> tanks() {
        return List.of(new TankDef(tank, 80));
    }

    @Override
    protected FluidTank inputTank() {
        return drain ? null : tank;
    }

    @Override
    protected FluidTank outputTank() {
        return drain ? tank : null;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed) return false;
        if (drain) push(level, pos);
        if (++cooldown < INTERVAL || energy.getEnergyStored() < ENERGY) return false;
        cooldown = 0;
        BlockPos target = pos.relative(facing());
        boolean done = drain ? pickUp(level, target) : place(level, target);
        if (done) energy.consume(ENERGY);
        return done;
    }

    private boolean place(Level level, BlockPos target) {
        if (tank.getFluidAmount() < 1000 || !level.getBlockState(target).canBeReplaced()) return false;
        FluidStack bucket = tank.getFluid().copyWithAmount(1000);
        return FluidUtil.tryPlaceFluid(null, level, InteractionHand.MAIN_HAND, target, tank, bucket);
    }

    private boolean pickUp(Level level, BlockPos target) {
        FluidState fluid = level.getFluidState(target);
        BlockState state = level.getBlockState(target);
        if (!fluid.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) return false;
        FluidStack stack = new FluidStack(fluid.getType(), 1000);
        if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) return false;
        ItemStack bucket = pickup.pickupBlock(null, level, target, state);
        if (bucket.isEmpty()) return false;
        tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void push(Level level, BlockPos pos) {
        if (tank.isEmpty()) return;
        for (Direction dir : Direction.values()) {
            if (dir == facing() || !sideMode(MachineFace.of(facing(), dir)).gives()) continue;
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null) FluidUtil.tryFluidTransfer(target, tank, 1000, true);
        }
    }
}
