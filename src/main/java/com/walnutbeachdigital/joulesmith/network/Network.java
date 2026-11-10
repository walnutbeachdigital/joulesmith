package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A storage network, found by walking every connected {@link NetworkPart} from a starting block. It works
 * only with exactly one powered Network Core. Its storage is every drive in every Drive Bay on it.
 */
public final class Network {
    public static final int MAX_PARTS = 4096;

    public final List<BlockPos> parts;
    public final NetworkCoreBlockEntity core;
    public final List<DriveBayBlockEntity> bays;

    private Network(List<BlockPos> parts, NetworkCoreBlockEntity core, List<DriveBayBlockEntity> bays) {
        this.parts = parts;
        this.core = core;
        this.bays = bays;
    }

    /** The network {@code start} is part of. */
    public static Network scan(Level level, BlockPos start) {
        List<BlockPos> parts = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<NetworkCoreBlockEntity> cores = new ArrayList<>();
        List<DriveBayBlockEntity> bays = new ArrayList<>();
        if (level.getBlockState(start).getBlock() instanceof NetworkPart) {
            queue.add(start.immutable());
            seen.add(start.immutable());
        }
        while (!queue.isEmpty() && parts.size() < MAX_PARTS) {
            BlockPos p = queue.poll();
            parts.add(p);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof NetworkCoreBlockEntity core) cores.add(core);
            if (be instanceof DriveBayBlockEntity bay) bays.add(bay);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.contains(n) || !level.isLoaded(n)) continue;
                if (level.getBlockState(n).getBlock() instanceof NetworkPart) {
                    seen.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        return new Network(parts, cores.size() == 1 ? cores.get(0) : null, bays);
    }

    /** Working: one Network Core, with power. */
    public boolean online() {
        return core != null && core.powered();
    }

    /** Every kind of item on the network and how many, merged across drives. */
    public List<BinContents> contents() {
        List<BinContents> all = new ArrayList<>();
        if (!online()) return all;
        for (DriveBayBlockEntity bay : bays) {
            for (ItemStack drive : bay.drives()) {
                for (BinContents c : DriveItem.contents(drive)) {
                    int i = DriveItem.indexOf(all, c.item());
                    if (i < 0) all.add(c);
                    else all.set(i, new BinContents(c.item(), (int) Math.min(Integer.MAX_VALUE, (long) all.get(i).count() + c.count())));
                }
            }
        }
        return all;
    }

    /** Stores what it can of {@code stack}; returns what's left over. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !online()) return stack;
        int left = stack.getCount();
        // drives that already hold this item first, then any drive with room
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (DriveBayBlockEntity bay : bays) {
                for (ItemStack drive : bay.drives()) {
                    boolean has = DriveItem.indexOf(DriveItem.contents(drive), stack) >= 0;
                    if (has != (pass == 0)) continue;
                    int moved = DriveItem.insert(drive, stack, left, simulate);
                    if (moved > 0) {
                        left -= moved;
                        if (!simulate) bay.changed();
                    }
                    if (left <= 0) break;
                }
                if (left <= 0) break;
            }
        }
        return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    /** Takes up to {@code amount} of {@code like}; returns what came out. */
    public ItemStack extract(ItemStack like, int amount, boolean simulate) {
        if (like.isEmpty() || amount <= 0 || !online()) return ItemStack.EMPTY;
        int got = 0;
        for (DriveBayBlockEntity bay : bays) {
            for (ItemStack drive : bay.drives()) {
                int moved = DriveItem.extract(drive, like, amount - got, simulate);
                if (moved > 0) {
                    got += moved;
                    if (!simulate) bay.changed();
                }
                if (got >= amount) break;
            }
            if (got >= amount) break;
        }
        return got == 0 ? ItemStack.EMPTY : like.copyWithCount(got);
    }
}
