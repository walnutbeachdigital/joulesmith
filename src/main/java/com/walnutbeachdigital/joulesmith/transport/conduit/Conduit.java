package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.Arrays;
import java.util.List;

import com.walnutbeachdigital.joulesmith.transport.PipeConnection;
import com.walnutbeachdigital.joulesmith.transport.PipeMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One conduit inside a bundle: its kind and tier, the wrench setting on each side, how each side is joined
 * (worked out on the server and sent to the game for drawing), and the network it belongs to.
 */
public abstract class Conduit {
    public final ConduitType type;
    protected int tier;
    private final PipeMode[] modes = new PipeMode[6];
    private final PipeConnection[] connections = new PipeConnection[6];
    BundleBlockEntity host;

    protected Conduit(ConduitType type, int tier) {
        this.type = type;
        this.tier = Math.max(1, Math.min(type.maxTier, tier));
        Arrays.fill(modes, PipeMode.PUSH);
        Arrays.fill(connections, PipeConnection.NONE);
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
        invalidateNetwork();
        if (host != null) host.changed(true);
    }

    public BlockPos pos() {
        return host.getBlockPos();
    }

    public Level level() {
        return host.getLevel();
    }

    public BundleBlockEntity host() {
        return host;
    }

    public PipeMode mode(Direction dir) {
        return modes[dir.get3DDataValue()];
    }

    public void setMode(Direction dir, PipeMode mode) {
        modes[dir.get3DDataValue()] = mode;
        invalidateNetwork();
        if (host != null) {
            host.refreshConnections();
            host.changed(true);
            if (host.getLevel() != null) host.getLevel().invalidateCapabilities(host.getBlockPos());
        }
    }

    public PipeConnection connection(Direction dir) {
        return connections[dir.get3DDataValue()];
    }

    PipeConnection[] connections() {
        return connections;
    }

    // ------------------------------------------------------------------ joining

    /** Whether the block at {@code other} holds what this conduit carries, on its face {@code face}. */
    protected abstract boolean hasTarget(Level level, BlockPos other, Direction face);

    /** How a side touching a block looks in a mode. */
    protected PipeConnection endpointLook(PipeMode mode) {
        return type.plates ? mode.look() : PipeConnection.PUSH;
    }

    /** How this conduit should look on side {@code dir}, from its wrench setting and what is next to it. */
    PipeConnection compute(Direction dir) {
        PipeMode mode = mode(dir);
        if (mode == PipeMode.DISABLED) return PipeConnection.NONE;
        Level level = level();
        BlockPos other = pos().relative(dir);
        if (!level.isLoaded(other)) return connection(dir);
        Conduit neighbour = BundleBlockEntity.conduit(level, other, type);
        if (neighbour != null) return neighbour.mode(dir.getOpposite()) == PipeMode.DISABLED ? PipeConnection.NONE : PipeConnection.PIPE;
        if (level.getBlockState(other).getBlock() instanceof BundleBlock) return PipeConnection.NONE;
        return hasTarget(level, other, dir.getOpposite()) ? endpointLook(mode) : PipeConnection.NONE;
    }

    /** Works out every side again; true if any changed. */
    boolean recompute() {
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            PipeConnection now = compute(dir);
            if (now != connections[dir.get3DDataValue()]) {
                connections[dir.get3DDataValue()] = now;
                changed = true;
            }
        }
        if (changed) invalidateNetwork();
        return changed;
    }

    // ------------------------------------------------------------------ network

    /** Forgets the network (it is found again on the next tick). */
    public abstract void invalidateNetwork();

    /** Runs the network (once per tick however many of its conduits ask). */
    public abstract void tick(Level level);

    // ------------------------------------------------------------------ wrench

    /** What the next wrench click on a side touching a block sets. */
    protected PipeMode nextMode(PipeMode mode) {
        return type.plates ? mode.next() : mode == PipeMode.DISABLED ? PipeMode.PUSH : PipeMode.DISABLED;
    }

    protected Component modeMessage(PipeMode mode) {
        if (type.plates) return Component.translatable(mode.key());
        return Component.translatable(mode == PipeMode.DISABLED ? "message.joulesmith.conduit.off" : "message.joulesmith.conduit.on");
    }

    /**
     * Wrench on side {@code dir}. Next to the same kind of conduit it joins or splits them. Next to a block it
     * steps push, pull, both, off (cables: on, off).
     */
    public Component wrench(Player player, Direction dir) {
        Level level = level();
        BlockPos other = pos().relative(dir);
        Conduit neighbour = BundleBlockEntity.conduit(level, other, type);
        if (neighbour != null) {
            boolean join = mode(dir) == PipeMode.DISABLED || neighbour.mode(dir.getOpposite()) == PipeMode.DISABLED;
            PipeMode mode = join ? PipeMode.PUSH : PipeMode.DISABLED;
            neighbour.modes[dir.getOpposite().get3DDataValue()] = mode;
            neighbour.invalidateNetwork();
            setMode(dir, mode);
            neighbour.host.refreshConnections();
            neighbour.host.changed(true);
            level.invalidateCapabilities(other);
            return Component.translatable(join ? "message.joulesmith.pipe.joined" : "message.joulesmith.pipe.split");
        }
        if (!(level.getBlockState(other).getBlock() instanceof BundleBlock) && hasTarget(level, other, dir.getOpposite())) {
            PipeMode mode = nextMode(mode(dir));
            setMode(dir, mode);
            return modeMessage(mode);
        }
        return Component.translatable("message.joulesmith.pipe.nothing");
    }

    /** Sneak + wrench on a side. Item ducts use it for filters and routing. */
    public Component sneakWrench(Player player, Direction dir) {
        return Component.translatable("message.joulesmith.pipe.no_options");
    }

    /** Anything fitted to the conduit that drops with it (an item duct's filter cards). */
    public List<ItemStack> extras() {
        return List.of();
    }

    // ------------------------------------------------------------------ saving

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Type", type.key());
        tag.putInt("Tier", tier);
        int[] m = new int[6], c = new int[6];
        for (int i = 0; i < 6; i++) {
            m[i] = modes[i].ordinal();
            c[i] = connections[i].ordinal();
        }
        tag.putIntArray("Modes", m);
        tag.putIntArray("Conns", c);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        tier = Math.max(1, Math.min(type.maxTier, tag.getInt("Tier")));
        int[] m = tag.getIntArray("Modes"), c = tag.getIntArray("Conns");
        for (int i = 0; i < 6; i++) {
            if (i < m.length) modes[i] = PipeMode.values()[Math.floorMod(m[i], PipeMode.values().length)];
            if (i < c.length) connections[i] = PipeConnection.values()[Math.floorMod(c[i], PipeConnection.values().length)];
        }
    }

    public static Conduit read(CompoundTag tag, HolderLookup.Provider registries) {
        ConduitType type;
        try {
            type = ConduitType.valueOf(tag.getString("Type").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        Conduit conduit = type.create(tag.getInt("Tier"));
        conduit.load(tag, registries);
        return conduit;
    }
}
