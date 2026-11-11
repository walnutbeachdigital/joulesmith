package com.walnutbeachdigital.joulesmith.transport;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared base for item ducts and fluid pipes: the wrench setting on each side, and the network it belongs to. */
public abstract class PipeBlockEntity extends BlockEntity {
    private final PipeMode[] modes = new PipeMode[6];
    protected PipeNetwork network;

    protected PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(modes, PipeMode.PUSH);
    }

    protected abstract PipeNetwork createNetwork();

    /** This pipe's network, found again if anything around it changed. */
    public PipeNetwork network() {
        if ((network == null || !network.valid()) && level != null && !level.isClientSide) {
            network = createNetwork();
        }
        return network;
    }

    public void invalidateNetwork() {
        if (network != null) network.invalidate();
        network = null;
    }

    public PipeMode mode(Direction dir) {
        return modes[dir.get3DDataValue()];
    }

    public void setMode(Direction dir, PipeMode mode) {
        modes[dir.get3DDataValue()] = mode;
        setChanged();
        invalidateNetwork();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Sneak + wrench on a side. Item ducts use it for filters and routing; others say there is nothing to change. */
    public Component sneakWrench(Player player, Direction dir) {
        return Component.translatable("message.joulesmith.pipe.no_options");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeBlockEntity pipe) {
        PipeNetwork net = pipe.network();
        if (net != null) net.tick(level);
    }

    @Override
    public void setRemoved() {
        invalidateNetwork();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] values = new int[6];
        for (int i = 0; i < 6; i++) values[i] = modes[i].ordinal();
        tag.putIntArray("Modes", values);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] values = tag.getIntArray("Modes");
        for (int i = 0; i < Math.min(6, values.length); i++) {
            modes[i] = PipeMode.values()[Math.floorMod(values[i], PipeMode.values().length)];
        }
    }

    // The client keeps a copy of the side settings so its own shape updates draw the same arms as the server.
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
