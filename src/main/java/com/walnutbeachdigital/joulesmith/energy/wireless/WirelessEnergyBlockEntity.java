package com.walnutbeachdigital.joulesmith.energy.wireless;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The store inside a Wireless Transmitter or Receiver. Transmitters take power from cables; each tick a
 * Receiver pulls up to {@link #RATE} FE from the Transmitters on its channel within {@link #RANGE} blocks
 * and pushes it into whatever touches it.
 */
public class WirelessEnergyBlockEntity extends BlockEntity {
    public static final int CAPACITY = 200_000, RATE = 10_000, RANGE = 256;

    /** Loaded transmitters, by dimension. Server side only; entries are checked before use. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRANSMITTERS = new HashMap<>();

    private final JSEnergyStorage energy;

    public WirelessEnergyBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.WIRELESS_ENERGY.get(), pos, state);
        boolean tx = transmitter(state);
        energy = new JSEnergyStorage(CAPACITY, tx ? RATE : 0, tx ? 0 : RATE, this::setChanged);
    }

    private static boolean transmitter(BlockState state) {
        return state.getBlock() instanceof WirelessEnergyBlock b && b.transmitter();
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    public static void clear() {
        TRANSMITTERS.clear();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && transmitter(getBlockState())) {
            TRANSMITTERS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        forget();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        forget();
    }

    private void forget() {
        if (level != null && !level.isClientSide) {
            Set<BlockPos> set = TRANSMITTERS.get(level.dimension());
            if (set != null) set.remove(worldPosition);
        }
    }

    public static void receiverTick(Level level, BlockPos pos, BlockState state, WirelessEnergyBlockEntity rx) {
        int room = Math.min(RATE, rx.energy.getMaxEnergyStored() - rx.energy.getEnergyStored());
        Set<BlockPos> set = TRANSMITTERS.get(level.dimension());
        if (room > 0 && set != null && !set.isEmpty()) {
            var channel = state.getValue(WirelessEnergyBlock.CHANNEL);
            for (BlockPos p : set.toArray(BlockPos[]::new)) {
                if (room <= 0) break;
                if (p.distSqr(pos) > (double) RANGE * RANGE || !level.isLoaded(p)) continue;
                if (!(level.getBlockEntity(p) instanceof WirelessEnergyBlockEntity tx) || !transmitter(tx.getBlockState())) {
                    set.remove(p);
                    continue;
                }
                if (tx.getBlockState().getValue(WirelessEnergyBlock.CHANNEL) != channel) continue;
                int taken = tx.energy.consume(Math.min(room, tx.energy.getEnergyStored()));
                rx.energy.generate(taken);
                room -= taken;
            }
        }
        for (Direction dir : Direction.values()) {
            if (rx.energy.getEnergyStored() == 0) break;
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive() && !(level.getBlockEntity(pos.relative(dir)) instanceof WirelessEnergyBlockEntity)) {
                int taken = target.receiveEnergy(Math.min(RATE, rx.energy.getEnergyStored()), false);
                rx.energy.consume(taken);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
