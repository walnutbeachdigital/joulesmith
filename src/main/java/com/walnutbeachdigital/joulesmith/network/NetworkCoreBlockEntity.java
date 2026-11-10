package com.walnutbeachdigital.joulesmith.network;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The heart of a storage network: one per network. It runs on 10 FE/t plus 5 FE/t per Drive Bay; without
 * power the network goes offline (nothing is lost, it just can't be reached).
 */
public class NetworkCoreBlockEntity extends BlockEntity {
    public static final int CAPACITY = 200_000, MAX_INPUT = 5_000, BASE = 10, PER_BAY = 5;

    private final JSEnergyStorage energy = new JSEnergyStorage(CAPACITY, MAX_INPUT, 0, this::setChanged);
    private int upkeep = BASE;
    private boolean powered;

    public NetworkCoreBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.NETWORK_CORE.get(), pos, state);
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    public boolean powered() {
        return powered;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkCoreBlockEntity core) {
        if (level.getGameTime() % 40 == Math.floorMod(pos.hashCode(), 40)) {
            core.upkeep = BASE + PER_BAY * Network.scan(level, pos).bays.size();
        }
        core.powered = core.energy.consume(core.upkeep) >= core.upkeep;
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
