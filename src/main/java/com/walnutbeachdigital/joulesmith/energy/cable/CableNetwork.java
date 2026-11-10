package com.walnutbeachdigital.joulesmith.energy.cable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Every cable joined together, sharing one small buffer. Power pushed into any cable goes into the buffer;
 * each tick the buffer is shared out among the machines touching the network that can take power.
 * The network is rebuilt whenever a cable or a neighbour changes.
 */
public final class CableNetwork {
    /** FE per tick into or out of the network at each connection, by cable tier (1 Basic, 2 Reinforced, 3 Advanced). */
    public static int rateFor(int tier) {
        return tier >= 5 ? 250_000 : tier == 4 ? 50_000 : tier == 3 ? 10_000 : tier == 2 ? 2_000 : 500;
    }

    /** The network's rate: its weakest cable's. */
    private int rate = Integer.MAX_VALUE;
    private static final int MAX_CABLES = 4096;

    private final Set<BlockPos> cables = new HashSet<>();
    private final List<BlockPos> endPositions = new ArrayList<>();
    private final List<Direction> endFaces = new ArrayList<>();
    private int buffer;
    private long lastTick = -1;
    private int startIndex;
    private boolean valid = true;
    private Level level;

    private final IEnergyStorage intake = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (!valid) return 0;
            int accepted = Math.min(Math.min(amount, rate), capacity() - buffer);
            if (accepted > 0 && !simulate) buffer += accepted;
            return Math.max(0, accepted);
        }

        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return buffer; }
        @Override public int getMaxEnergyStored() { return capacity(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return valid; }
    };

    private int capacity() {
        return rate * 2;
    }

    public IEnergyStorage intake() {
        return intake;
    }

    public boolean valid() {
        return valid;
    }

    /** Finds every cable joined to {@code start}, and the machines touching them. */
    static CableNetwork build(Level level, BlockPos start) {
        CableNetwork net = new CableNetwork();
        net.level = level;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        net.cables.add(start);
        while (!queue.isEmpty() && net.cables.size() < MAX_CABLES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof EnergyCableBlock block)) continue;
            net.rate = Math.min(net.rate, rateFor(block.tier()));
            if (level.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) cable.network = net;
            for (Direction dir : Direction.values()) {
                if (!state.getValue(EnergyCableBlock.CONNECTED.get(dir))) continue;
                BlockPos other = pos.relative(dir);
                if (level.getBlockState(other).getBlock() instanceof EnergyCableBlock) {
                    if (net.cables.add(other)) queue.add(other);
                } else {
                    net.endPositions.add(other);
                    net.endFaces.add(dir.getOpposite());
                }
            }
        }
        if (net.rate == Integer.MAX_VALUE) net.rate = rateFor(1);
        return net;
    }

    void invalidate() {
        if (!valid) return;
        valid = false;
        for (BlockPos pos : cables) {
            if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable && cable.network == this) {
                cable.network = null;
            }
        }
    }

    /** Shares the buffer out among the machines that can take power. Runs once per game tick per network. */
    void tick(Level level) {
        long now = level.getGameTime();
        if (now == lastTick) return;
        lastTick = now;
        if (buffer <= 0 || endPositions.isEmpty()) return;
        int n = endPositions.size();
        startIndex = (startIndex + 1) % n;                          // rotate who goes first, for fairness
        for (int k = 0; k < n && buffer > 0; k++) {
            int i = (startIndex + k) % n;
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, endPositions.get(i), endFaces.get(i));
            if (target == null || !target.canReceive()) continue;
            int share = Math.max(1, buffer / (n - k));
            buffer -= target.receiveEnergy(Math.min(Math.min(share, buffer), rate), false);
        }
    }
}
