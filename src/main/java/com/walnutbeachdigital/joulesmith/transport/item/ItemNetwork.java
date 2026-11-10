package com.walnutbeachdigital.joulesmith.transport.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;
import com.walnutbeachdigital.joulesmith.transport.RoutingMode;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitType;
import com.walnutbeachdigital.joulesmith.transport.conduit.ItemConduit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Item ducts joined together. Every half second each pulling side takes one stack's worth (1 item on
 * tier 1, 4 on tier 2) from its block and hands it to the pushing sides, in the order its routing mode
 * sets. Items only leave a block when somewhere can take them, so nothing is ever dropped.
 */
public final class ItemNetwork extends PipeNetwork {
    public static final int INTERVAL = 10;

    private final Map<Endpoint, Integer> nextTarget = new HashMap<>();
    private int nextIntake;

    public static ItemNetwork create(Level level, BlockPos start) {
        ItemNetwork net = new ItemNetwork();
        net.build(level, start, ConduitType.ITEM);
        return net;
    }

    public static int itemsPerMove(int tier) {
        return tier >= 5 ? 64 : tier == 4 ? 32 : tier == 3 ? 16 : tier == 2 ? 4 : 1;
    }

    @Override
    protected void update(Level level, long gameTime) {
        if (gameTime % INTERVAL != 0) return;
        for (Endpoint source : endpoints) {
            if (source.mode().pulls()) pull(level, source);
        }
    }

    private ItemConduit duct(BlockPos pos) {
        return pipe(pos) instanceof ItemConduit duct ? duct : null;
    }

    /** Takes up to one move's worth of one item from {@code source}'s block and sends it on. */
    private void pull(Level level, Endpoint source) {
        IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, source.target(), source.face());
        ItemConduit duct = duct(source.pipe());
        if (from == null || duct == null) return;
        List<Endpoint> targets = targetsFor(source, duct);
        if (targets.isEmpty()) return;
        int amount = itemsPerMove(source.tier());
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack offered = from.extractItem(slot, amount, true);
            if (offered.isEmpty() || !duct.passes(source.dir(), offered)) continue;
            ItemStack left = send(targets, offered, true);
            int fits = offered.getCount() - left.getCount();
            if (fits <= 0) continue;
            ItemStack taken = from.extractItem(slot, fits, false);
            ItemStack rest = send(targets, taken, false);
            if (!rest.isEmpty()) rest = ItemHandlerHelper.insertItem(from, rest, false);   // put back what did not fit
            if (!rest.isEmpty()) Block.popResource(level, source.pipe(), rest);
            nextTarget.merge(source, 1, Integer::sum);
            return;
        }
    }

    /** The pushing sides {@code source} may send to, ordered by its routing mode. */
    private List<Endpoint> targetsFor(Endpoint source, ItemConduit duct) {
        List<Endpoint> targets = new ArrayList<>();
        for (Endpoint e : endpoints) {
            if (e.mode().pushes() && !e.target().equals(source.target())) targets.add(e);
        }
        if (targets.size() < 2) return targets;
        RoutingMode routing = duct.routing(source.dir());
        if (routing == RoutingMode.ROUND_ROBIN) {
            int start = Math.floorMod(nextTarget.getOrDefault(source, 0), targets.size());
            List<Endpoint> rotated = new ArrayList<>(targets.subList(start, targets.size()));
            rotated.addAll(targets.subList(0, start));
            return rotated;
        }
        Map<BlockPos, Integer> dist = distancesFrom(source.pipe());
        Comparator<Endpoint> byDistance = Comparator.comparingInt(e -> dist.getOrDefault(e.pipe(), Integer.MAX_VALUE));
        targets.sort(routing == RoutingMode.NEAREST ? byDistance : byDistance.reversed());
        return targets;
    }

    /** Offers {@code stack} to each target in turn; returns what none of them could take. */
    private ItemStack send(List<Endpoint> targets, ItemStack stack, boolean simulate) {
        ItemStack left = stack.copy();
        for (Endpoint target : targets) {
            if (left.isEmpty()) break;
            ItemConduit duct = duct(target.pipe());
            if (duct != null && !duct.passes(target.dir(), left)) continue;
            IItemHandler to = level.getCapability(Capabilities.ItemHandler.BLOCK, target.target(), target.face());
            if (to != null) left = ItemHandlerHelper.insertItem(to, left, simulate);
        }
        return left;
    }

    /** Items pushed into the duct at {@code pipe} from outside (a hopper, another mod's pipe). */
    public ItemStack deliver(BlockPos pipe, Direction side, ItemStack stack, boolean simulate) {
        List<Endpoint> targets = new ArrayList<>();
        for (Endpoint e : endpoints) {
            boolean isSender = side != null && e.pipe().equals(pipe) && e.dir() == side;
            if (e.mode().pushes() && !isSender) targets.add(e);
        }
        if (targets.isEmpty()) return stack;
        int start = Math.floorMod(nextIntake, targets.size());
        List<Endpoint> rotated = new ArrayList<>(targets.subList(start, targets.size()));
        rotated.addAll(targets.subList(0, start));
        ItemStack left = send(rotated, stack, simulate);
        if (!simulate && left.getCount() < stack.getCount()) nextIntake++;
        return left;
    }
}
