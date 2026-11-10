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
    public final List<CraftingCpuBlockEntity> cpus;

    private Network(List<BlockPos> parts, NetworkCoreBlockEntity core, List<DriveBayBlockEntity> bays, List<CraftingCpuBlockEntity> cpus) {
        this.parts = parts;
        this.core = core;
        this.bays = bays;
        this.cpus = cpus;
    }

    /** The network {@code start} is part of. */
    public static Network scan(Level level, BlockPos start) {
        List<BlockPos> parts = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<NetworkCoreBlockEntity> cores = new ArrayList<>();
        List<DriveBayBlockEntity> bays = new ArrayList<>();
        List<CraftingCpuBlockEntity> cpus = new ArrayList<>();
        if (NetworkPart.at(level, start)) {
            queue.add(start.immutable());
            seen.add(start.immutable());
        }
        while (!queue.isEmpty() && parts.size() < MAX_PARTS) {
            BlockPos p = queue.poll();
            parts.add(p);
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof NetworkCoreBlockEntity core) cores.add(core);
            if (be instanceof DriveBayBlockEntity bay) bays.add(bay);
            if (be instanceof CraftingCpuBlockEntity cpu) cpus.add(cpu);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.contains(n) || !level.isLoaded(n)) continue;
                if (NetworkPart.joins(level, p, d)) {
                    seen.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        return new Network(parts, cores.size() == 1 ? cores.get(0) : null, bays, cpus);
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

    /** Every pattern on the network's CPUs. */
    public List<PatternContents> patterns() {
        List<PatternContents> all = new ArrayList<>();
        if (online()) for (CraftingCpuBlockEntity cpu : cpus) all.addAll(cpu.patterns());
        return all;
    }

    /** What the network's patterns can make (one of each, for the terminal). */
    public List<ItemStack> craftable() {
        List<ItemStack> out = new ArrayList<>();
        for (PatternContents p : patterns()) {
            if (out.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, p.output()))) out.add(p.output().copyWithCount(1));
        }
        return out;
    }

    /**
     * Asks a free CPU to make {@code count} of {@code item}. Returns a message for the player: started, no
     * free CPU, or what's missing.
     */
    public net.minecraft.network.chat.Component request(ItemStack item, int count) {
        if (!online()) return net.minecraft.network.chat.Component.translatable("gui.joulesmith.terminal.offline");
        CraftingPlan plan = CraftingPlan.make(contents(), patterns(), item, count);
        if (!plan.ok()) {
            BinContents first = plan.missing.isEmpty() ? null : plan.missing.get(0);
            return first == null ? net.minecraft.network.chat.Component.translatable("message.joulesmith.craft.too_big")
                    : net.minecraft.network.chat.Component.translatable("message.joulesmith.craft.missing",
                            first.count(), first.item().getHoverName(), plan.missing.size());
        }
        for (CraftingCpuBlockEntity cpu : cpus) {
            if (!cpu.busy()) {
                return cpu.start(this, plan, item)
                        ? net.minecraft.network.chat.Component.translatable("message.joulesmith.craft.started", count, item.getHoverName())
                        : net.minecraft.network.chat.Component.translatable("message.joulesmith.craft.changed");
            }
        }
        return net.minecraft.network.chat.Component.translatable(cpus.isEmpty() ? "message.joulesmith.craft.no_cpu" : "message.joulesmith.craft.busy");
    }

    /** Every fluid on the network and how much, merged across drives. */
    public List<net.neoforged.neoforge.fluids.FluidStack> fluids() {
        List<net.neoforged.neoforge.fluids.FluidStack> all = new ArrayList<>();
        if (!online()) return all;
        for (DriveBayBlockEntity bay : bays) {
            for (ItemStack drive : bay.fluidDrives()) {
                for (var f : FluidDriveItem.contents(drive)) {
                    int i = FluidDriveItem.indexOf(all, f);
                    if (i < 0) all.add(f.copy());
                    else all.set(i, f.copyWithAmount((int) Math.min(Integer.MAX_VALUE, (long) all.get(i).getAmount() + f.getAmount())));
                }
            }
        }
        return all;
    }

    /** Stores up to {@code fluid}'s amount; returns how much went in. */
    public int insertFluid(net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate) {
        if (fluid.isEmpty() || !online()) return 0;
        int left = fluid.getAmount();
        for (int pass = 0; pass < 2 && left > 0; pass++) {
            for (DriveBayBlockEntity bay : bays) {
                for (ItemStack drive : bay.fluidDrives()) {
                    boolean has = FluidDriveItem.indexOf(FluidDriveItem.contents(drive), fluid) >= 0;
                    if (has != (pass == 0)) continue;
                    int moved = FluidDriveItem.insert(drive, fluid, left, simulate);
                    if (moved > 0) {
                        left -= moved;
                        if (!simulate) bay.changed();
                    }
                    if (left <= 0) break;
                }
                if (left <= 0) break;
            }
        }
        return fluid.getAmount() - left;
    }

    /** Takes up to {@code amount} mB of {@code like}; returns what came out. */
    public net.neoforged.neoforge.fluids.FluidStack extractFluid(net.neoforged.neoforge.fluids.FluidStack like, int amount, boolean simulate) {
        if (like.isEmpty() || amount <= 0 || !online()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        int got = 0;
        for (DriveBayBlockEntity bay : bays) {
            for (ItemStack drive : bay.fluidDrives()) {
                int moved = FluidDriveItem.extract(drive, like, amount - got, simulate);
                if (moved > 0) {
                    got += moved;
                    if (!simulate) bay.changed();
                }
                if (got >= amount) break;
            }
            if (got >= amount) break;
        }
        return got == 0 ? net.neoforged.neoforge.fluids.FluidStack.EMPTY : like.copyWithAmount(got);
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
