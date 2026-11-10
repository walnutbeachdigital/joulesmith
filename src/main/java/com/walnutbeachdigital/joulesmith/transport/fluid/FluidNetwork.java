package com.walnutbeachdigital.joulesmith.transport.fluid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitType;
import com.walnutbeachdigital.joulesmith.transport.conduit.FluidConduit;
import com.walnutbeachdigital.joulesmith.transport.conduit.PipeConduit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fluid pipes joined together, holding one fluid at a time. Each tick, pulling sides drain their blocks
 * into the network (100 mB/t on tier 1, 400 on tier 2) and the network fills the pushing sides in turn.
 * What the pipes hold is shared out to them when the network breaks up, so no fluid is lost.
 */
public final class FluidNetwork extends PipeNetwork {
    private FluidStack buffer = FluidStack.EMPTY;
    private int capacity;
    private int nextTarget;

    private final IFluidHandler intake = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return buffer.copy(); }
        @Override public int getTankCapacity(int tank) { return capacity; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return buffer.isEmpty() || FluidStack.isSameFluidSameComponents(buffer, stack); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!valid() || resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int accepted = Math.min(resource.getAmount(), capacity - buffer.getAmount());
            if (accepted > 0 && action.execute()) add(resource.copyWithAmount(accepted));
            return Math.max(0, accepted);
        }
    };

    public static FluidNetwork create(Level level, BlockPos start) {
        FluidNetwork net = new FluidNetwork();
        net.build(level, start, ConduitType.FLUID);
        int maxRate = 0;
        for (Endpoint e : net.endpoints) maxRate = Math.max(maxRate, mbPerTick(e.tier()));
        net.capacity = Math.max(1000, Math.max(maxRate * 2, net.pipes.size() * 250));
        if (net.buffer.getAmount() > net.capacity) net.buffer.setAmount(net.capacity);
        return net;
    }

    public static int mbPerTick(int tier) {
        return tier >= 5 ? 25_600 : tier == 4 ? 6_400 : tier == 3 ? 1_600 : tier == 2 ? 400 : 100;
    }

    public IFluidHandler intake() {
        return intake;
    }

    public FluidStack contents() {
        return buffer;
    }

    private void add(FluidStack stack) {
        if (buffer.isEmpty()) buffer = stack.copy();
        else buffer.grow(stack.getAmount());
    }

    @Override
    protected void joined(PipeConduit pipe) {
        if (pipe instanceof FluidConduit fluidPipe) {
            FluidStack held = fluidPipe.takeHeld();
            if (!held.isEmpty() && (buffer.isEmpty() || FluidStack.isSameFluidSameComponents(buffer, held))) add(held);
        }
    }

    @Override
    protected void leaving() {
        shareOut(true);
    }

    /** Splits what the network holds evenly over its pipes, so it survives a rebuild or a save. */
    public FluidStack shareFor(FluidConduit pipe) {
        if (buffer.isEmpty() || pipes.isEmpty()) return FluidStack.EMPTY;
        int share = buffer.getAmount() / pipes.size();
        if (pipe.pos().equals(pipes.iterator().next())) share += buffer.getAmount() % pipes.size();
        return share <= 0 ? FluidStack.EMPTY : buffer.copyWithAmount(share);
    }

    private void shareOut(boolean clear) {
        if (buffer.isEmpty() || level == null) return;
        for (BlockPos pos : pipes) {
            if (level.isLoaded(pos) && pipe(pos) instanceof FluidConduit pipe) pipe.hold(shareFor(pipe));
        }
        if (clear) buffer = FluidStack.EMPTY;
    }

    @Override
    protected void update(Level level, long gameTime) {
        Set<BlockPos> drainedFrom = new HashSet<>();
        for (Endpoint source : endpoints) {
            if (!source.mode().pulls()) continue;
            int room = capacity - buffer.getAmount();
            if (room <= 0) break;
            IFluidHandler from = level.getCapability(Capabilities.FluidHandler.BLOCK, source.target(), source.face());
            if (from == null) continue;
            int max = Math.min(room, mbPerTick(source.tier()));
            FluidStack offered = buffer.isEmpty() ? from.drain(max, IFluidHandler.FluidAction.SIMULATE)
                    : from.drain(buffer.copyWithAmount(max), IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty()) continue;
            FluidStack drained = from.drain(offered, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) continue;
            add(drained);
            drainedFrom.add(source.target());
        }
        if (buffer.isEmpty()) return;
        List<Endpoint> all = endpoints;
        int n = all.size();
        if (n == 0) return;
        nextTarget = (nextTarget + 1) % n;                       // take turns going first
        for (int k = 0; k < n && !buffer.isEmpty(); k++) {
            Endpoint target = all.get((nextTarget + k) % n);
            if (!target.mode().pushes() || drainedFrom.contains(target.target())) continue;
            IFluidHandler to = level.getCapability(Capabilities.FluidHandler.BLOCK, target.target(), target.face());
            if (to == null) continue;
            int give = Math.min(buffer.getAmount(), mbPerTick(target.tier()));
            int filled = to.fill(buffer.copyWithAmount(give), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                buffer.shrink(filled);
                if (buffer.isEmpty()) buffer = FluidStack.EMPTY;
            }
        }
    }
}
