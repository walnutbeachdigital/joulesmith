package com.walnutbeachdigital.joulesmith.energy.cable;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyCableBlockEntity extends BlockEntity {
    CableNetwork network;

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    /** This cable's network, found again if the cables around it changed. */
    public CableNetwork network() {
        if ((network == null || !network.valid()) && level != null) {
            network = CableNetwork.build(level, worldPosition);
        }
        return network;
    }

    public void invalidateNetwork() {
        if (network != null) network.invalidate();
        network = null;
    }

    /** What machines see on any face: the network's intake. */
    public IEnergyStorage energyHandler() {
        CableNetwork net = network();
        return net == null ? null : net.intake();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity cable) {
        CableNetwork net = cable.network();
        if (net != null) net.tick(level);
    }

    @Override
    public void setRemoved() {
        invalidateNetwork();
        super.setRemoved();
    }
}
