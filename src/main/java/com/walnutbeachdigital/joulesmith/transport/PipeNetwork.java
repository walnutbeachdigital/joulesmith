package com.walnutbeachdigital.joulesmith.transport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every pipe of one family joined together, and the blocks they touch. Item and fluid networks build on
 * this; it is rebuilt whenever a pipe, a neighbour or a wrench setting changes.
 */
public abstract class PipeNetwork {
    private static final int MAX_PIPES = 4096;

    /** One side of one pipe touching a block: where the pipe is, which way it faces, and its settings. */
    public record Endpoint(BlockPos pipe, Direction dir, PipeMode mode, int tier) {
        public BlockPos target() { return pipe.relative(dir); }
        public Direction face() { return dir.getOpposite(); }
    }

    protected final Set<BlockPos> pipes = new HashSet<>();
    protected final List<Endpoint> endpoints = new ArrayList<>();
    private final Map<BlockPos, Map<BlockPos, Integer>> distances = new HashMap<>();
    protected Level level;
    private boolean valid = true;
    private long lastTick = -1;

    public boolean valid() {
        return valid;
    }

    /** Finds every pipe joined to {@code start} and the blocks they touch. */
    protected void build(Level level, BlockPos start) {
        this.level = level;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        pipes.add(start);
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PipeBlock block) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe)) continue;
            pipe.network = this;
            joined(pipe);
            for (Direction dir : Direction.values()) {
                PipeConnection conn = state.getValue(PipeBlock.CONNECTION.get(dir));
                if (conn == PipeConnection.PIPE) {
                    BlockPos other = pos.relative(dir);
                    if (level.isLoaded(other) && pipes.add(other)) queue.add(other);
                } else if (conn != PipeConnection.NONE) {
                    endpoints.add(new Endpoint(pos, dir, pipe.mode(dir), block.tier()));
                }
            }
        }
    }

    /** Called for each pipe as it joins, so a network can collect what the pipe was holding. */
    protected void joined(PipeBlockEntity pipe) {
    }

    /** Called just before the network breaks up, while every pipe still points at it. */
    protected void leaving() {
    }

    void invalidate() {
        if (!valid) return;
        leaving();
        valid = false;
        for (BlockPos pos : pipes) {
            if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe && pipe.network == this) {
                pipe.network = null;
            }
        }
    }

    /** Runs {@link #update} once per game tick, however many pipes ask. */
    void tick(Level level) {
        long now = level.getGameTime();
        if (now == lastTick || !valid) return;
        lastTick = now;
        update(level, now);
    }

    protected abstract void update(Level level, long gameTime);

    /** How many pipes away every pipe in the network is from {@code from}. Worked out once and kept. */
    protected Map<BlockPos, Integer> distancesFrom(BlockPos from) {
        return distances.computeIfAbsent(from, start -> {
            Map<BlockPos, Integer> dist = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            dist.put(start, 0);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos pos = queue.poll();
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof PipeBlock)) continue;
                for (Direction dir : Direction.values()) {
                    if (state.getValue(PipeBlock.CONNECTION.get(dir)) != PipeConnection.PIPE) continue;
                    BlockPos other = pos.relative(dir);
                    if (pipes.contains(other) && !dist.containsKey(other)) {
                        dist.put(other, dist.get(pos) + 1);
                        queue.add(other);
                    }
                }
            }
            return dist;
        });
    }

    /** The pipes from {@code from} to {@code to}, both included (empty if they aren't joined). */
    protected java.util.List<BlockPos> path(BlockPos from, BlockPos to) {
        Map<BlockPos, Integer> dist = distancesFrom(to);
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        Integer d = dist.get(from);
        if (d == null) return out;
        BlockPos at = from;
        out.add(at);
        while (d > 0 && out.size() < 256) {
            BlockPos next = null;
            for (Direction dir : Direction.values()) {
                BlockPos n = at.relative(dir);
                if (dist.getOrDefault(n, Integer.MAX_VALUE) == d - 1) { next = n; break; }
            }
            if (next == null) break;
            at = next;
            d--;
            out.add(at);
        }
        return out;
    }

    public int size() {
        return pipes.size();
    }
}
