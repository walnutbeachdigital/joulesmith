package com.walnutbeachdigital.joulesmith.transport.conduit;

import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;

import net.minecraft.world.level.Level;

/** An item duct or fluid pipe: a conduit whose network moves things between the blocks it touches. */
public abstract class PipeConduit extends Conduit {
    public PipeNetwork network;

    protected PipeConduit(ConduitType type, int tier) {
        super(type, tier);
    }

    protected abstract PipeNetwork createNetwork();

    /** This conduit's network, found again if anything around it changed. */
    public PipeNetwork network() {
        if ((network == null || !network.valid()) && host != null && level() != null && !level().isClientSide) {
            network = createNetwork();
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
        PipeNetwork net = network();
        if (net != null) net.tick(level);
    }
}
