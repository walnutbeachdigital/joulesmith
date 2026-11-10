package com.walnutbeachdigital.joulesmith.transport.conduit;

import com.walnutbeachdigital.joulesmith.energy.cable.CableNetwork;
import com.walnutbeachdigital.joulesmith.transport.PipeMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** An energy cable (500 FE/t at tier 1, then 2,000, 10,000, 50,000 and 250,000). Joins anything that holds power. */
public class EnergyConduit extends Conduit {
    public CableNetwork network;

    public EnergyConduit(int tier) {
        super(ConduitType.ENERGY, tier);
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, other, face) != null;
    }

    public CableNetwork network() {
        if ((network == null || !network.valid()) && host != null && level() != null && !level().isClientSide) {
            network = CableNetwork.build(level(), pos());
        }
        return network;
    }

    @Override
    public void invalidateNetwork() {
        if (network != null) network.invalidate();
        network = null;
    }

    @Override
    public void tick(Level level) {
        CableNetwork net = network();
        if (net != null) net.tick(level);
    }

    /** What machines see: the network's intake (nothing on a side switched off). */
    public IEnergyStorage energyHandler(Direction side) {
        if (side != null && mode(side) == PipeMode.DISABLED) return null;
        CableNetwork net = network();
        return net == null ? null : net.intake();
    }
}
